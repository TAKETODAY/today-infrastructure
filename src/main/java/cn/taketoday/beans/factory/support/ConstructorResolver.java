/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.beans.factory.support;

import java.beans.ConstructorProperties;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.beans.BeanMetadataElement;
import cn.taketoday.beans.BeanWrapper;
import cn.taketoday.beans.BeanWrapperImpl;
import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.TypeConverter;
import cn.taketoday.beans.TypeMismatchException;
import cn.taketoday.beans.factory.AutowireCapableBeanFactory;
import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.InjectionPoint;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.beans.factory.NoUniqueBeanDefinitionException;
import cn.taketoday.beans.factory.UnsatisfiedDependencyException;
import cn.taketoday.beans.factory.support.ConstructorArgumentValues.ValueHolder;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.NamedThreadLocal;
import cn.taketoday.core.ParameterNameDiscoverer;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.ReflectiveMethodInvoker;
import cn.taketoday.util.StringUtils;

/**
 * Delegate for resolving constructors and factory methods.
 *
 * <p>Performs constructor resolution through argument matching.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Mark Fisher
 * @author Costin Leau
 * @author Sebastien Deleuze
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #autowireConstructor
 * @see #instantiateUsingFactoryMethod
 * @see AbstractAutowireCapableBeanFactory
 * @since 4.0 2022/1/6 21:16
 */
final class ConstructorResolver {

  private static final Object[] EMPTY_ARGS = new Object[0];

  /**
   * Marker for autowired arguments in a cached argument array, to be replaced
   * by a {@linkplain #resolveAutowiredArgument resolved autowired argument}.
   */
  private static final Object autowiredArgumentMarker = new Object();

  private static final NamedThreadLocal<InjectionPoint> currentInjectionPoint =
          new NamedThreadLocal<>("Current injection point");

  private final DependencyInjector injector;
  private final AbstractAutowireCapableBeanFactory beanFactory;

  private final Logger log = AbstractAutowireCapableBeanFactory.log;

  /**
   * Create a new ConstructorResolver for the given factory and instantiation strategy.
   *
   * @param beanFactory the BeanFactory to work with
   */
  public ConstructorResolver(AbstractAutowireCapableBeanFactory beanFactory) {
    this.beanFactory = beanFactory;
    this.injector = beanFactory.getInjector();
  }

  /**
   * "autowire constructor" (with constructor arguments by type) behavior.
   * Also applied if explicit constructor argument values are specified,
   * matching all remaining arguments with beans from the bean factory.
   * <p>This corresponds to constructor injection: In this mode, a Spring
   * bean factory is able to host components that expect constructor-based
   * dependency resolution.
   *
   * @param beanName the name of the bean
   * @param merged the merged bean definition for the bean
   * @param chosenCtors chosen candidate constructors (or {@code null} if none)
   * @param explicitArgs argument values passed in programmatically via the getBean method,
   * or {@code null} if none (-> use constructor argument values from bean definition)
   * @return a BeanWrapper for the new instance
   */
  public Object autowireConstructor(String beanName, RootBeanDefinition merged,
          @Nullable Constructor<?>[] chosenCtors, @Nullable Object[] explicitArgs) {

    BeanWrapperImpl wrapper = new BeanWrapperImpl();
    this.beanFactory.initBeanWrapper(wrapper);

    Constructor<?> constructorToUse = null;
    ArgumentsHolder argsHolderToUse = null;
    Object[] argsToUse = null;

    if (explicitArgs != null) {
      argsToUse = explicitArgs;
    }
    else {
      Object[] argsToResolve = null;
      synchronized(merged.constructorArgumentLock) {
        constructorToUse = (Constructor<?>) merged.executable;
        if (constructorToUse != null && merged.constructorArgumentsResolved) {
          // Found a cached constructor...
          argsToUse = merged.resolvedConstructorArguments;
          if (argsToUse == null) {
            argsToResolve = merged.preparedConstructorArguments;
          }
        }
      }
      if (argsToResolve != null) {
        argsToUse = resolvePreparedArguments(beanName, merged, constructorToUse, argsToResolve, wrapper);
      }
    }

    if (constructorToUse == null || argsToUse == null) {
      // Take specified constructors, if any.
      Constructor<?>[] candidates = chosenCtors;
      if (candidates == null) {
        Class<?> beanClass = merged.getBeanClass();
        try {
          candidates = merged.isNonPublicAccessAllowed()
                       ? beanClass.getDeclaredConstructors()
                       : beanClass.getConstructors();
        }
        catch (Throwable ex) {
          throw new BeanCreationException(merged,
                  "Resolution of declared constructors on bean Class [" + beanClass.getName() +
                          "] from ClassLoader [" + beanClass.getClassLoader() + "] failed", ex);
        }
      }

      if (candidates.length == 1 && explicitArgs == null && !merged.hasConstructorArgumentValues()) {
        Constructor<?> uniqueCandidate = candidates[0];
        if (uniqueCandidate.getParameterCount() == 0) {
          synchronized(merged.constructorArgumentLock) {
            merged.executable = uniqueCandidate;
            merged.constructorArgumentsResolved = true;
            merged.resolvedConstructorArguments = EMPTY_ARGS;
          }
          return instantiate(beanName, merged, uniqueCandidate, EMPTY_ARGS);
        }
      }

      // Need to resolve the constructor.
      boolean autowiring = chosenCtors != null
              || merged.getResolvedAutowireMode() == AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR;
      ConstructorArgumentValues resolvedValues = null;

      int minNrOfArgs;
      if (explicitArgs != null) {
        minNrOfArgs = explicitArgs.length;
      }
      else {
        ConstructorArgumentValues cargs = merged.getConstructorArgumentValues();
        resolvedValues = new ConstructorArgumentValues();
        minNrOfArgs = resolveConstructorArguments(merged, wrapper, cargs, resolvedValues);
      }

      AutowireUtils.sortConstructors(candidates);
      int minTypeDiffWeight = Integer.MAX_VALUE;
      Set<Constructor<?>> ambiguousConstructors = null;
      Deque<UnsatisfiedDependencyException> causes = null;

      for (Constructor<?> candidate : candidates) {
        int parameterCount = candidate.getParameterCount();

        if (constructorToUse != null && argsToUse != null && argsToUse.length > parameterCount) {
          // Already found greedy constructor that can be satisfied ->
          // do not look any further, there are only less greedy constructors left.
          break;
        }
        if (parameterCount < minNrOfArgs) {
          continue;
        }

        ArgumentsHolder argsHolder;
        Class<?>[] paramTypes = candidate.getParameterTypes();
        if (resolvedValues != null) {
          try {
            String[] paramNames = ConstructorPropertiesChecker.evaluate(candidate, parameterCount);
            if (paramNames == null) {
              ParameterNameDiscoverer pnd = beanFactory.getParameterNameDiscoverer();
              if (pnd != null) {
                paramNames = pnd.getParameterNames(candidate);
              }
            }
            argsHolder = createArgumentArray(merged, resolvedValues, paramTypes, paramNames,
                    getUserDeclaredConstructor(candidate), wrapper, autowiring, candidates.length == 1);
          }
          catch (UnsatisfiedDependencyException ex) {
            if (log.isTraceEnabled()) {
              log.trace("Ignoring constructor [{}] of bean '{}': {}", candidate, beanName, ex);
            }
            // Swallow and try next constructor.
            if (causes == null) {
              causes = new ArrayDeque<>(1);
            }
            causes.add(ex);
            continue;
          }
        }
        else {
          // Explicit arguments given -> arguments length must match exactly.
          if (parameterCount != explicitArgs.length) {
            continue;
          }
          argsHolder = new ArgumentsHolder(explicitArgs);
        }

        int typeDiffWeight = merged.isLenientConstructorResolution()
                             ? argsHolder.getTypeDifferenceWeight(paramTypes)
                             : argsHolder.getAssignabilityWeight(paramTypes);

        // Choose this constructor if it represents the closest match.
        if (typeDiffWeight < minTypeDiffWeight) {
          constructorToUse = candidate;
          argsHolderToUse = argsHolder;
          argsToUse = argsHolder.arguments;
          minTypeDiffWeight = typeDiffWeight;
          ambiguousConstructors = null;
        }
        else if (constructorToUse != null && typeDiffWeight == minTypeDiffWeight) {
          if (ambiguousConstructors == null) {
            ambiguousConstructors = new LinkedHashSet<>();
            ambiguousConstructors.add(constructorToUse);
          }
          ambiguousConstructors.add(candidate);
        }
      }

      if (constructorToUse == null) {
        if (causes != null) {
          UnsatisfiedDependencyException ex = causes.removeLast();
          for (Exception cause : causes) {
            beanFactory.onSuppressedException(cause);
          }
          throw ex;
        }
        throw new BeanCreationException(merged.getResourceDescription(), beanName,
                "Could not resolve matching constructor on bean class [" + merged.getBeanClassName() + "] " +
                        "(hint: specify index/type/name arguments for simple parameters to avoid type ambiguities)");
      }
      else if (ambiguousConstructors != null && !merged.isLenientConstructorResolution()) {
        throw new BeanCreationException(merged.getResourceDescription(), beanName,
                "Ambiguous constructor matches found on bean class [" + merged.getBeanClassName() + "] " +
                        "(hint: specify index/type/name arguments for simple parameters to avoid type ambiguities): " +
                        ambiguousConstructors);
      }

      if (explicitArgs == null && argsHolderToUse != null) {
        argsHolderToUse.storeCache(merged, constructorToUse);
      }
    }

    Assert.state(argsToUse != null, "Unresolved constructor arguments");
    return instantiate(beanName, merged, constructorToUse, argsToUse);
  }

  private Object instantiate(String beanName, RootBeanDefinition mbd,
          Constructor<?> constructorToUse, Object[] argsToUse) {
    try {
      return beanFactory.getInstantiationStrategy().instantiate(
              mbd, beanName, beanFactory, constructorToUse, argsToUse);
    }
    catch (Throwable ex) {
      throw new BeanCreationException(mbd,
              "Bean instantiation via constructor failed", ex);
    }
  }

  /**
   * Resolve the factory method in the specified bean definition, if possible.
   * {@link BeanDefinition#getResolvedFactoryMethod()} can be checked for the result.
   *
   * @param mbd the bean definition to check
   */
  public void resolveFactoryMethodIfPossible(BeanDefinition mbd) {
    Class<?> factoryClass;
    boolean isStatic;
    if (mbd.getFactoryBeanName() != null) {
      factoryClass = beanFactory.getType(mbd.getFactoryBeanName());
      isStatic = false;
    }
    else {
      factoryClass = mbd.getBeanClass();
      isStatic = true;
    }
    Assert.state(factoryClass != null, "Unresolvable factory class");
    factoryClass = ClassUtils.getUserClass(factoryClass);

    Method[] candidates = getCandidateMethods(factoryClass, mbd);
    Method uniqueCandidate = null;
    for (Method candidate : candidates) {
      if (Modifier.isStatic(candidate.getModifiers()) == isStatic && mbd.isFactoryMethod(candidate)) {
        if (uniqueCandidate == null) {
          uniqueCandidate = candidate;
        }
        else if (isParamMismatch(uniqueCandidate, candidate)) {
          uniqueCandidate = null;
          break;
        }
      }
    }
    mbd.factoryMethodToIntrospect = uniqueCandidate;
  }

  private static boolean isParamMismatch(Method uniqueCandidate, Method candidate) {
    int uniqueCandidateParameterCount = uniqueCandidate.getParameterCount();
    int candidateParameterCount = candidate.getParameterCount();
    return uniqueCandidateParameterCount != candidateParameterCount
            || !Arrays.equals(uniqueCandidate.getParameterTypes(), candidate.getParameterTypes());
  }

  /**
   * Retrieve all candidate methods for the given class, considering
   * the {@link BeanDefinition#isNonPublicAccessAllowed()} flag.
   * Called as the starting point for factory method determination.
   */
  private Method[] getCandidateMethods(Class<?> factoryClass, BeanDefinition mbd) {
    return mbd.isNonPublicAccessAllowed()
           ? ReflectionUtils.getAllDeclaredMethods(factoryClass)
           : factoryClass.getMethods();
  }

  /**
   * Instantiate the bean using a named factory method. The method may be static, if the
   * bean definition parameter specifies a class, rather than a "factory-bean", or
   * an instance variable on a factory object itself configured using Dependency Injection.
   * <p>Implementation requires iterating over the static or instance methods with the
   * name specified in the BeanDefinition (the method may be overloaded) and trying
   * to match with the parameters. We don't have the types attached to constructor args,
   * so trial and error is the only way to go here. The explicitArgs array may contain
   * argument values passed in programmatically via the corresponding getBean method.
   *
   * @param merged the merged bean definition for the bean
   * @param explicitArgs argument values passed in programmatically via the getBean
   * method, or {@code null} if none (-> use constructor argument values from bean definition)
   * @return a BeanWrapper for the new instance
   */
  public Object instantiateUsingFactoryMethod(String beanName, RootBeanDefinition merged, @Nullable Object[] explicitArgs) {
    BeanWrapperImpl wrapper = new BeanWrapperImpl();
    this.beanFactory.initBeanWrapper(wrapper);

    boolean isStatic;
    Object factoryBean;
    Class<?> factoryClass;

    String factoryBeanName = merged.getFactoryBeanName();
    if (factoryBeanName != null) {
      if (factoryBeanName.equals(beanName)) {
        throw new BeanDefinitionStoreException(merged,
                "factory-bean reference points back to the same bean definition");
      }
      factoryBean = BeanFactoryUtils.requiredBean(beanFactory, factoryBeanName);
      if (merged.isSingleton() && beanFactory.containsSingleton(beanName)) {
        throw new ImplicitlyAppearedSingletonException();
      }
      beanFactory.registerDependentBean(factoryBeanName, beanName);
      factoryClass = factoryBean.getClass();
      isStatic = false;
    }
    else {
      // It's a static factory method on the bean class.
      if (!merged.hasBeanClass()) {
        throw new BeanDefinitionStoreException(merged,
                "bean definition declares neither a bean class nor a factory-bean reference");
      }
      factoryBean = null;
      factoryClass = merged.getBeanClass();
      isStatic = true;
    }

    Method factoryMethodToUse = null;
    ArgumentsHolder argsHolderToUse = null;
    Object[] argsToUse = null;

    if (explicitArgs != null) {
      argsToUse = explicitArgs;
    }
    else {
      Object[] argsToResolve = null;
      synchronized(merged.constructorArgumentLock) {
        factoryMethodToUse = (Method) merged.executable;
        if (factoryMethodToUse != null && merged.constructorArgumentsResolved) {
          // Found a cached factory method...
          argsToUse = merged.resolvedConstructorArguments;
          if (argsToUse == null) {
            argsToResolve = merged.preparedConstructorArguments;
          }
        }
      }
      if (argsToResolve != null) {
        argsToUse = resolvePreparedArguments(beanName, merged, factoryMethodToUse, argsToResolve, wrapper);
      }
    }

    if (factoryMethodToUse == null || argsToUse == null) {
      // Need to determine the factory method...
      // Try all methods with this name to see if they match the given arguments.
      factoryClass = ClassUtils.getUserClass(factoryClass);

      List<Method> candidates = null;
      if (merged.isFactoryMethodUnique) {
        if (factoryMethodToUse == null) {
          factoryMethodToUse = merged.getResolvedFactoryMethod();
        }
        if (factoryMethodToUse != null) {
          candidates = Collections.singletonList(factoryMethodToUse);
        }
      }
      if (candidates == null) {
        candidates = new ArrayList<>();
        Method[] rawCandidates = getCandidateMethods(factoryClass, merged);
        for (Method candidate : rawCandidates) {
          if (Modifier.isStatic(candidate.getModifiers()) == isStatic && merged.isFactoryMethod(candidate)) {
            candidates.add(candidate);
          }
        }
      }

      if (candidates.size() == 1 && explicitArgs == null && !merged.hasConstructorArgumentValues()) {
        Method uniqueCandidate = candidates.get(0);
        if (uniqueCandidate.getParameterCount() == 0) {
          merged.factoryMethodToIntrospect = uniqueCandidate;
          synchronized(merged.constructorArgumentLock) {
            merged.executable = uniqueCandidate;
            merged.constructorArgumentsResolved = true;
            merged.resolvedConstructorArguments = EMPTY_ARGS;
          }
          return instantiate(beanName, merged, factoryBean, uniqueCandidate, EMPTY_ARGS);
        }
      }

      if (candidates.size() > 1) {  // explicitly skip immutable singletonList
        candidates.sort(AutowireUtils.EXECUTABLE_COMPARATOR);
      }

      ConstructorArgumentValues resolvedValues = null;
      boolean autowiring = (merged.getResolvedAutowireMode() == AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR);
      int minTypeDiffWeight = Integer.MAX_VALUE;
      Set<Method> ambiguousFactoryMethods = null;

      int minNrOfArgs;
      if (explicitArgs != null) {
        minNrOfArgs = explicitArgs.length;
      }
      else {
        // We don't have arguments passed in programmatically, so we need to resolve the
        // arguments specified in the constructor arguments held in the bean definition.
        if (merged.hasConstructorArgumentValues()) {
          ConstructorArgumentValues cargs = merged.getConstructorArgumentValues();
          resolvedValues = new ConstructorArgumentValues();
          minNrOfArgs = resolveConstructorArguments(merged, wrapper, cargs, resolvedValues);
        }
        else {
          minNrOfArgs = 0;
        }
      }

      Deque<UnsatisfiedDependencyException> causes = null;

      for (Method candidate : candidates) {
        int parameterCount = candidate.getParameterCount();

        if (parameterCount >= minNrOfArgs) {
          ArgumentsHolder argsHolder;

          Class<?>[] paramTypes = candidate.getParameterTypes();
          if (explicitArgs != null) {
            // Explicit arguments given -> arguments length must match exactly.
            if (paramTypes.length != explicitArgs.length) {
              continue;
            }
            argsHolder = new ArgumentsHolder(explicitArgs);
          }
          else {
            // Resolved constructor arguments: type conversion and/or autowiring necessary.
            try {
              String[] paramNames = null;
              ParameterNameDiscoverer pnd = beanFactory.getParameterNameDiscoverer();
              if (pnd != null) {
                paramNames = pnd.getParameterNames(candidate);
              }
              argsHolder = createArgumentArray(merged, resolvedValues,
                      paramTypes, paramNames, candidate, wrapper, autowiring, candidates.size() == 1);
            }
            catch (UnsatisfiedDependencyException ex) {
              if (log.isTraceEnabled()) {
                log.trace("Ignoring factory method [{}] of bean '{}': {}", candidate, beanName, ex);
              }
              // Swallow and try next overloaded factory method.
              if (causes == null) {
                causes = new ArrayDeque<>(1);
              }
              causes.add(ex);
              continue;
            }
          }

          int typeDiffWeight = merged.isLenientConstructorResolution()
                               ? argsHolder.getTypeDifferenceWeight(paramTypes)
                               : argsHolder.getAssignabilityWeight(paramTypes);
          // Choose this factory method if it represents the closest match.
          if (typeDiffWeight < minTypeDiffWeight) {
            factoryMethodToUse = candidate;
            argsHolderToUse = argsHolder;
            argsToUse = argsHolder.arguments;
            minTypeDiffWeight = typeDiffWeight;
            ambiguousFactoryMethods = null;
          }
          // Find out about ambiguity: In case of the same type difference weight
          // for methods with the same number of parameters, collect such candidates
          // and eventually raise an ambiguity exception.
          // However, only perform that check in non-lenient constructor resolution mode,
          // and explicitly ignore overridden methods (with the same parameter signature).
          else if (factoryMethodToUse != null && typeDiffWeight == minTypeDiffWeight &&
                  !merged.isLenientConstructorResolution() &&
                  paramTypes.length == factoryMethodToUse.getParameterCount() &&
                  !Arrays.equals(paramTypes, factoryMethodToUse.getParameterTypes())) {
            if (ambiguousFactoryMethods == null) {
              ambiguousFactoryMethods = new LinkedHashSet<>();
              ambiguousFactoryMethods.add(factoryMethodToUse);
            }
            ambiguousFactoryMethods.add(candidate);
          }
        }
      }

      if (factoryMethodToUse == null || argsToUse == null) {
        if (causes != null) {
          UnsatisfiedDependencyException ex = causes.removeLast();
          for (Exception cause : causes) {
            beanFactory.onSuppressedException(cause);
          }
          throw ex;
        }
        ArrayList<String> argTypes = new ArrayList<>(minNrOfArgs);
        if (explicitArgs != null) {
          for (Object arg : explicitArgs) {
            argTypes.add(arg != null ? arg.getClass().getSimpleName() : "null");
          }
        }
        else if (resolvedValues != null) {
          LinkedHashSet<ValueHolder> valueHolders = new LinkedHashSet<>(resolvedValues.getArgumentCount());
          valueHolders.addAll(resolvedValues.getIndexedArgumentValues().values());
          valueHolders.addAll(resolvedValues.getGenericArgumentValues());
          for (ValueHolder value : valueHolders) {
            String argType = (value.getType() != null
                              ? ClassUtils.getShortName(value.getType())
                              : (value.getValue() != null
                                 ? value.getValue().getClass().getSimpleName()
                                 : "null"));
            argTypes.add(argType);
          }
        }
        String argDesc = StringUtils.collectionToCommaDelimitedString(argTypes);
        throw new BeanCreationException(merged.getResourceDescription(), beanName,
                "No matching factory method found on class [" + factoryClass.getName() + "]: " +
                        (merged.getFactoryBeanName() != null ?
                         "factory bean '" + merged.getFactoryBeanName() + "'; " : "") +
                        "factory method '" + merged.getFactoryMethodName() + "(" + argDesc + ")'. " +
                        "Check that a method with the specified name " +
                        (minNrOfArgs > 0 ? "and arguments " : "") +
                        "exists and that it is " +
                        (isStatic ? "static" : "non-static") + ".");
      }
      else if (void.class == factoryMethodToUse.getReturnType()) {
        throw new BeanCreationException(merged.getResourceDescription(), beanName,
                "Invalid factory method '" + merged.getFactoryMethodName() + "' on class [" +
                        factoryClass.getName() + "]: needs to have a non-void return type!");
      }
      else if (ambiguousFactoryMethods != null) {
        throw new BeanCreationException(merged.getResourceDescription(), beanName,
                "Ambiguous factory method matches found on class [" + factoryClass.getName() + "] " +
                        "(hint: specify index/type/name arguments for simple parameters to avoid type ambiguities): " +
                        ambiguousFactoryMethods);
      }

      if (explicitArgs == null && argsHolderToUse != null) {
        merged.factoryMethodToIntrospect = factoryMethodToUse;
        argsHolderToUse.storeCache(merged, factoryMethodToUse);
      }
    }

    return instantiate(beanName, merged, factoryBean, factoryMethodToUse, argsToUse);
  }

  private Object instantiate(String beanName, RootBeanDefinition mbd,
          @Nullable Object factoryBean, Method factoryMethod, Object[] args) {

    try {
      return beanFactory.getInstantiationStrategy().instantiate(
              mbd, beanName, beanFactory, factoryBean, factoryMethod, args);
    }
    catch (Throwable ex) {
      throw new BeanCreationException(mbd, "Bean instantiation via factory method failed", ex);
    }
  }

  /**
   * Resolve the constructor arguments for this bean into the resolvedValues object.
   * This may involve looking up other beans.
   * <p>This method is also used for handling invocations of static factory methods.
   */
  private int resolveConstructorArguments(BeanDefinition mbd, BeanWrapper bw,
          ConstructorArgumentValues cargs, ConstructorArgumentValues resolvedValues) {

    TypeConverter customConverter = this.beanFactory.getCustomTypeConverter();
    TypeConverter converter = (customConverter != null ? customConverter : bw);

    BeanDefinitionValueResolver valueResolver = new BeanDefinitionValueResolver(beanFactory, mbd, converter);
    int minNrOfArgs = cargs.getArgumentCount();
    for (Map.Entry<Integer, ValueHolder> entry : cargs.getIndexedArgumentValues().entrySet()) {
      int index = entry.getKey();
      if (index < 0) {
        throw new BeanCreationException(
                mbd, "Invalid constructor argument index: " + index);
      }
      if (index + 1 > minNrOfArgs) {
        minNrOfArgs = index + 1;
      }
      ValueHolder valueHolder = entry.getValue();
      if (valueHolder.isConverted()) {
        resolvedValues.addIndexedArgumentValue(index, valueHolder);
      }
      else {
        Object resolvedValue = valueResolver.resolveValueIfNecessary(
                "constructor argument", valueHolder.getValue());
        ValueHolder resolvedValueHolder =
                new ValueHolder(resolvedValue, valueHolder.getType(), valueHolder.getName());
        resolvedValueHolder.setSource(valueHolder);
        resolvedValues.addIndexedArgumentValue(index, resolvedValueHolder);
      }
    }

    for (ValueHolder valueHolder : cargs.getGenericArgumentValues()) {
      if (valueHolder.isConverted()) {
        resolvedValues.addGenericArgumentValue(valueHolder);
      }
      else {
        Object resolvedValue = valueResolver.resolveValueIfNecessary(
                "constructor argument", valueHolder.getValue());
        ValueHolder resolvedValueHolder = new ValueHolder(
                resolvedValue, valueHolder.getType(), valueHolder.getName());
        resolvedValueHolder.setSource(valueHolder);
        resolvedValues.addGenericArgumentValue(resolvedValueHolder);
      }
    }

    return minNrOfArgs;
  }

  /**
   * Create an array of arguments to invoke a constructor or factory method,
   * given the resolved constructor argument values.
   */
  private ArgumentsHolder createArgumentArray(BeanDefinition definition,
          @Nullable ConstructorArgumentValues resolvedValues, Class<?>[] paramTypes,
          @Nullable String[] paramNames, Executable executable, BeanWrapper wrapper,
          boolean autowiring, boolean fallback) throws UnsatisfiedDependencyException {

    TypeConverter customConverter = this.beanFactory.getCustomTypeConverter();
    TypeConverter converter = customConverter != null ? customConverter : wrapper;

    String beanName = definition.getBeanName();

    ArgumentsHolder args = new ArgumentsHolder(paramTypes.length);
    HashSet<ValueHolder> usedValueHolders = new HashSet<>(paramTypes.length);
    LinkedHashSet<String> autowiredBeanNames = new LinkedHashSet<>(4);

    for (int paramIndex = 0; paramIndex < paramTypes.length; paramIndex++) {
      Class<?> paramType = paramTypes[paramIndex];
      String paramName = paramNames != null ? paramNames[paramIndex] : "";
      // Try to find matching constructor argument value, either indexed or generic.
      ValueHolder valueHolder = null;
      if (resolvedValues != null) {
        valueHolder = resolvedValues.getArgumentValue(paramIndex, paramType, paramName, usedValueHolders);
        // If we couldn't find a direct match and are not supposed to autowire,
        // let's try the next generic, untyped argument value as fallback:
        // it could match after type conversion (for example, String -> int).
        if (valueHolder == null && (!autowiring || paramTypes.length == resolvedValues.getArgumentCount())) {
          valueHolder = resolvedValues.getGenericArgumentValue(null, null, usedValueHolders);
        }
      }
      if (valueHolder != null) {
        // We found a potential match - let's give it a try.
        // Do not consider the same value definition multiple times!
        usedValueHolders.add(valueHolder);
        Object originalValue = valueHolder.getValue();
        Object convertedValue;
        if (valueHolder.isConverted()) {
          convertedValue = valueHolder.getConvertedValue();
          args.preparedArguments[paramIndex] = convertedValue;
        }
        else {
          MethodParameter methodParam = MethodParameter.forExecutable(executable, paramIndex);
          try {
            convertedValue = convertIfNecessary(originalValue, paramType, methodParam);
          }
          catch (TypeMismatchException ex) {
            throw new UnsatisfiedDependencyException(
                    definition.getResourceDescription(), beanName, new InjectionPoint(methodParam),
                    "Could not convert argument value of type [" +
                            ObjectUtils.nullSafeClassName(valueHolder.getValue()) +
                            "] to required type [" + paramType.getName() + "]: " + ex.getMessage());
          }
          Object sourceHolder = valueHolder.getSource();
          if (sourceHolder instanceof ValueHolder) {
            Object sourceValue = ((ValueHolder) sourceHolder).getValue();
            args.resolveNecessary = true;
            args.preparedArguments[paramIndex] = sourceValue;
          }
        }
        args.arguments[paramIndex] = convertedValue;
        args.rawArguments[paramIndex] = originalValue;
      }
      else {
        MethodParameter methodParam = MethodParameter.forExecutable(executable, paramIndex);
        // No explicit match found: we're either supposed to autowire or
        // have to fail creating an argument array for the given constructor.
//        if (!autowiring) { // FIXME delete this everything is ok
//          throw new UnsatisfiedDependencyException(
//                  definition.getResourceDescription(), beanName, new InjectionPoint(methodParam),
//                  "Ambiguous argument values for parameter of type [" + paramType.getName() +
//                          "] - did you specify the correct bean references as arguments?");
//        }
        try {
          Object autowiredArgument = resolveAutowiredArgument(
                  methodParam, beanName, autowiredBeanNames, converter, fallback);

          args.resolveNecessary = true;
          args.arguments[paramIndex] = autowiredArgument;
          args.rawArguments[paramIndex] = autowiredArgument;
          args.preparedArguments[paramIndex] = autowiredArgumentMarker;
        }
        catch (UnsatisfiedDependencyException e) {
          throw e;
        }
        catch (BeansException ex) {
          throw new UnsatisfiedDependencyException(
                  definition.getResourceDescription(), beanName, new InjectionPoint(methodParam), ex);
        }
      }
    }

    for (String autowiredBeanName : autowiredBeanNames) {
      beanFactory.registerDependentBean(autowiredBeanName, beanName);
      if (log.isDebugEnabled()) {
        log.debug("Autowiring by type from bean name '{}' via {} to bean named '{}'",
                beanName, (executable instanceof Constructor ? "constructor" : "factory method"), autowiredBeanName);
      }
    }

    return args;
  }

  private Object convertIfNecessary(Object originalValue, Class<?> paramType, MethodParameter methodParam) {
    if (paramType.isInstance(originalValue)) {
      return originalValue;
    }
    TypeConverter typeConverter = beanFactory.getTypeConverter();
    return typeConverter.convertIfNecessary(originalValue, paramType, methodParam);
  }

  /**
   * Resolve the prepared arguments stored in the given bean definition.
   */
  private Object[] resolvePreparedArguments(
          String beanName, BeanDefinition mbd, Executable executable, Object[] argsToResolve, BeanWrapper bw) {

    TypeConverter customConverter = this.beanFactory.getCustomTypeConverter();
    TypeConverter converter = customConverter != null ? customConverter : bw;

    BeanDefinitionValueResolver valueResolver =
            new BeanDefinitionValueResolver(beanFactory, mbd, converter);
    Class<?>[] paramTypes = executable.getParameterTypes();

    Object[] resolvedArgs = new Object[argsToResolve.length];
    for (int argIndex = 0; argIndex < argsToResolve.length; argIndex++) {
      Object argValue = argsToResolve[argIndex];
      MethodParameter methodParam = MethodParameter.forExecutable(executable, argIndex);
      if (argValue == autowiredArgumentMarker) {
        argValue = resolveAutowiredArgument(methodParam, beanName, null, converter, true);
      }
      else if (argValue instanceof BeanMetadataElement) {
        argValue = valueResolver.resolveValueIfNecessary("constructor argument", argValue);
      }
      else if (argValue instanceof String) {
        argValue = beanFactory.evaluateBeanDefinitionString((String) argValue, mbd);
      }
      Class<?> paramType = paramTypes[argIndex];
      try {
        resolvedArgs[argIndex] = convertIfNecessary(argValue, paramType, methodParam);
      }
      catch (TypeMismatchException ex) {
        throw new UnsatisfiedDependencyException(
                mbd.getResourceDescription(), beanName, new InjectionPoint(methodParam),
                "Could not convert argument value of type [" + ObjectUtils.nullSafeClassName(argValue) +
                        "] to required type [" + paramType.getName() + "]: " + ex.getMessage());
      }
    }
    return resolvedArgs;
  }

  private Constructor<?> getUserDeclaredConstructor(Constructor<?> constructor) {
    Class<?> declaringClass = constructor.getDeclaringClass();
    Class<?> userClass = ClassUtils.getUserClass(declaringClass);
    if (userClass != declaringClass) {
      try {
        return userClass.getDeclaredConstructor(constructor.getParameterTypes());
      }
      catch (NoSuchMethodException ex) {
        // No equivalent constructor on user class (superclass)...
        // Let's proceed with the given constructor as we usually would.
      }
    }
    return constructor;
  }

  /**
   * Template method for resolving the specified argument which is supposed to be autowired.
   */
  @Nullable
  private Object resolveAutowiredArgument(MethodParameter param, String beanName,
          @Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter, boolean fallback) {

    Class<?> paramType = param.getParameterType();
    if (InjectionPoint.class.isAssignableFrom(paramType)) {
      InjectionPoint injectionPoint = currentInjectionPoint.get();
      if (injectionPoint == null) {
        throw new IllegalStateException("No current InjectionPoint available for " + param);
      }
      return injectionPoint;
    }
    try {
      DependencyResolvingContext context = new DependencyResolvingContext(param.getExecutable(), beanFactory, beanName);
      context.setTypeConverter(typeConverter);
      context.setDependentBeans(autowiredBeanNames);
      return injector.resolveValue(new DependencyDescriptor(param, true), context);
    }
    catch (NoUniqueBeanDefinitionException ex) {
      throw ex;
    }
    catch (NoSuchBeanDefinitionException ex) {
      if (fallback) {
        // Single constructor or factory method -> let's return an empty array/collection
        // for e.g. a vararg or a non-null List/Set/Map parameter.
        if (paramType.isArray()) {
          return Array.newInstance(paramType.getComponentType(), 0);
        }
        else if (CollectionUtils.isApproximableCollectionType(paramType)) {
          return CollectionUtils.createCollection(paramType, 0);
        }
        else if (CollectionUtils.isApproximableMapType(paramType)) {
          return CollectionUtils.createMap(paramType, 0);
        }
      }
      throw ex;
    }
  }

  static InjectionPoint setCurrentInjectionPoint(@Nullable InjectionPoint injectionPoint) {
    InjectionPoint old = currentInjectionPoint.get();
    if (injectionPoint != null) {
      currentInjectionPoint.set(injectionPoint);
    }
    else {
      currentInjectionPoint.remove();
    }
    return old;
  }

  /**
   * Private inner class for holding argument combinations.
   */
  private static class ArgumentsHolder {

    public final Object[] arguments;
    public final Object[] rawArguments;
    public final Object[] preparedArguments;

    public boolean resolveNecessary = false;

    public ArgumentsHolder(int size) {
      this.arguments = new Object[size];
      this.rawArguments = new Object[size];
      this.preparedArguments = new Object[size];
    }

    public ArgumentsHolder(Object[] args) {
      this.arguments = args;
      this.rawArguments = args;
      this.preparedArguments = args;
    }

    public int getTypeDifferenceWeight(Class<?>[] paramTypes) {
      // If valid arguments found, determine type difference weight.
      // Try type difference weight on both the converted arguments and
      // the raw arguments. If the raw weight is better, use it.
      // Decrease raw weight by 1024 to prefer it over equal converted weight.
      int typeDiffWeight = ReflectiveMethodInvoker.getTypeDifferenceWeight(paramTypes, arguments);
      int rawTypeDiffWeight = ReflectiveMethodInvoker.getTypeDifferenceWeight(paramTypes, rawArguments) - 1024;
      return Math.min(rawTypeDiffWeight, typeDiffWeight);
    }

    public int getAssignabilityWeight(Class<?>[] paramTypes) {
      for (int i = 0; i < paramTypes.length; i++) {
        if (!ClassUtils.isAssignableValue(paramTypes[i], arguments[i])) {
          return Integer.MAX_VALUE;
        }
      }
      for (int i = 0; i < paramTypes.length; i++) {
        if (!ClassUtils.isAssignableValue(paramTypes[i], rawArguments[i])) {
          return Integer.MAX_VALUE - 512;
        }
      }
      return Integer.MAX_VALUE - 1024;
    }

    public void storeCache(BeanDefinition mbd, Executable constructorOrFactoryMethod) {
      synchronized(mbd.constructorArgumentLock) {
        mbd.executable = constructorOrFactoryMethod;
        mbd.constructorArgumentsResolved = true;
        if (resolveNecessary) {
          mbd.preparedConstructorArguments = preparedArguments;
        }
        else {
          mbd.resolvedConstructorArguments = arguments;
        }
      }
    }
  }

  /**
   * Delegate for checking Java 6's {@link ConstructorProperties} annotation.
   */
  private static class ConstructorPropertiesChecker {

    @Nullable
    public static String[] evaluate(Constructor<?> candidate, int paramCount) {
      ConstructorProperties cp = candidate.getAnnotation(ConstructorProperties.class);
      if (cp != null) {
        String[] names = cp.value();
        if (names.length != paramCount) {
          throw new IllegalStateException("Constructor annotated with @ConstructorProperties but not " +
                  "corresponding to actual number of parameters (" + paramCount + "): " + candidate);
        }
        return names;
      }
      else {
        return null;
      }
    }
  }

}

