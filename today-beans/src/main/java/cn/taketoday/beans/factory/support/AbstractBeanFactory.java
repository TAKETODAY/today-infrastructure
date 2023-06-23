/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

import java.beans.PropertyEditor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.beans.BeanWrapper;
import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.PropertyEditorRegistrar;
import cn.taketoday.beans.PropertyEditorRegistry;
import cn.taketoday.beans.PropertyEditorRegistrySupport;
import cn.taketoday.beans.SimpleTypeConverter;
import cn.taketoday.beans.TypeConverter;
import cn.taketoday.beans.TypeMismatchException;
import cn.taketoday.beans.factory.BeanClassLoadFailedException;
import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.beans.factory.BeanCurrentlyInCreationException;
import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.BeanIsAbstractException;
import cn.taketoday.beans.factory.BeanIsNotAFactoryException;
import cn.taketoday.beans.factory.BeanNotOfRequiredTypeException;
import cn.taketoday.beans.factory.DependenciesBeanPostProcessor;
import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.HierarchicalBeanFactory;
import cn.taketoday.beans.factory.InitializationBeanPostProcessor;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.beans.factory.SmartFactoryBean;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.BeanDefinitionHolder;
import cn.taketoday.beans.factory.config.BeanExpressionContext;
import cn.taketoday.beans.factory.config.BeanExpressionResolver;
import cn.taketoday.beans.factory.config.BeanPostProcessor;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.config.DestructionAwareBeanPostProcessor;
import cn.taketoday.beans.factory.config.InstantiationAwareBeanPostProcessor;
import cn.taketoday.beans.factory.config.Scope;
import cn.taketoday.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import cn.taketoday.core.AttributeAccessor;
import cn.taketoday.core.DecoratingClassLoader;
import cn.taketoday.core.NamedThreadLocal;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.StringValueResolver;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.NullValue;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * Abstract base class for {@link BeanFactory} implementations,
 * providing the full capabilities of the {@link ConfigurableBeanFactory} SPI.
 * Does <i>not</i> assume a listable bean factory: can therefore also be used
 * as base class for bean factory implementations which obtain bean definitions
 * from some backend resource (where bean definition access is an expensive operation).
 *
 * <p>This class provides a singleton cache (through its base class
 * {@link DefaultSingletonBeanRegistry}, singleton/prototype determination,
 * {@link FactoryBean} handling, aliases, and bean destruction ({@link DisposableBean}
 * interface, custom destroy methods). Furthermore, it can manage a bean factory
 * hierarchy (delegating to the parent in case of an unknown bean), through implementing
 * the {@link HierarchicalBeanFactory} interface.
 *
 * <p>The main template methods to be implemented by subclasses are
 * {@link #getBeanDefinition} and {@link #createBean}, retrieving a bean definition
 * for a given bean name and creating a bean instance for a given bean definition,
 * respectively. Default implementations of those operations can be found in
 * {@link StandardBeanFactory} and {@link AbstractAutowireCapableBeanFactory}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Costin Leau
 * @author Chris Beams
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #getBeanDefinition
 * @see #createBean
 * @see AbstractAutowireCapableBeanFactory#createBean
 * @see StandardBeanFactory#getBeanDefinition
 * @since 2018-06-23 11:20:58
 */
public abstract class AbstractBeanFactory
        extends DefaultSingletonBeanRegistry implements ConfigurableBeanFactory {

  /** object factories */
  protected final ConcurrentHashMap<Class<?>, Object> objectFactories = new ConcurrentHashMap<>(16);
  private final HashMap<String, Scope> scopes = new HashMap<>();

  /** @since 4.0 */
  private DependencyInjector dependencyInjector;

  /** Parent bean factory, for bean inheritance support. @since 4.0 */
  @Nullable
  private BeanFactory parentBeanFactory;

  /** ClassLoader to resolve bean class names with, if necessary. @since 4.0 */
  @Nullable
  private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

  /** ClassLoader to temporarily resolve bean class names with, if necessary. */
  @Nullable
  private ClassLoader tempClassLoader;

  // @since 4.0 for bean-property conversion
  @Nullable
  private ConversionService conversionService;

  /** Resolution strategy for expressions in bean definition values. */
  @Nullable
  private BeanExpressionResolver beanExpressionResolver;

  // @since 4.0
  private volatile BeanPostProcessors postProcessorCache;

  /** Bean Post Processors */
  private final ArrayList<BeanPostProcessor> postProcessors = new ArrayList<>();

  /** object from a factory-bean map @since 4.0 */
  private final ConcurrentHashMap<String, Object> objectFromFactoryBeanCache = new ConcurrentHashMap<>(16);

  /** Names of beans that have already been created at least once. */
  private final Set<String> alreadyCreated = Collections.newSetFromMap(new ConcurrentHashMap<>(256));

  /** Names of beans that are currently in creation. */
  private final ThreadLocal<Object> prototypesCurrentlyInCreation =
          new NamedThreadLocal<>("Prototype beans currently in creation");

  /** String resolvers to apply e.g. to annotation attribute values. */
  private final ArrayList<StringValueResolver> embeddedValueResolvers = new ArrayList<>();

  /** A custom TypeConverter to use, overriding the default PropertyEditor mechanism. */
  @Nullable
  private TypeConverter typeConverter;

  /** Custom PropertyEditorRegistrars to apply to the beans of this factory. @since 4.0 */
  private final LinkedHashSet<PropertyEditorRegistrar> propertyEditorRegistrars = new LinkedHashSet<>(4);

  /** Custom PropertyEditors to apply to the beans of this factory. @since 4.0 */
  private final HashMap<Class<?>, Class<? extends PropertyEditor>> customEditors = new HashMap<>(4);

  /** Map from bean name to merged BeanDefinition. @since 4.0 */
  private final ConcurrentHashMap<String, RootBeanDefinition> mergedBeanDefinitions = new ConcurrentHashMap<>(256);

  /** Whether to cache bean metadata or rather reobtain it for every access. @since 4.0 */
  private boolean cacheBeanMetadata = true;

  //---------------------------------------------------------------------
  // Implementation of BeanFactory interface
  //---------------------------------------------------------------------

  @Override
  public Object getBean(String name) {
    return doGetBean(name, null, null, false);
  }

  @Override
  public <T> T getBean(String name, Class<T> requiredType) {
    return doGetBean(name, requiredType, null, false);
  }

  @Override
  public Object getBean(String name, Object... args) throws BeansException {
    return doGetBean(name, null, args, false);
  }

  /**
   * Return an instance, which may be shared or independent, of the specified bean.
   *
   * @param name the name of the bean to retrieve
   * @param requiredType the required type of the bean to retrieve
   * @param args arguments to use when creating a bean instance using explicit arguments
   * (only applied when creating a new instance as opposed to retrieving an existing one)
   * @param typeCheckOnly whether the instance is obtained for a type check,
   * not for actual use
   * @return an instance of the bean
   * @throws BeansException if the bean could not be created
   */
  @SuppressWarnings("unchecked")
  protected final <T> T doGetBean(
          String name, Class<?> requiredType, Object[] args, boolean typeCheckOnly) throws BeansException {
    // delete $
    String beanName = transformedBeanName(name);
    // 1. check singleton cache
    Object beanInstance = getSingleton(beanName);
    if (beanInstance == null || args != null) {
      // Fail if we're already creating this bean instance:
      // We're assumably within a circular reference.
      if (isPrototypeCurrentlyInCreation(beanName)) {
        throw new BeanCurrentlyInCreationException(beanName);
      }

      if (!containsBeanDefinition(beanName)) {
        // 2. Check if bean definition not exists in this factory.
        BeanFactory parentBeanFactory = getParentBeanFactory();
        if (parentBeanFactory != null) {
          // Not found -> check parent.
          String nameToLookup = originalBeanName(name);
          if (parentBeanFactory instanceof AbstractBeanFactory) {
            return ((AbstractBeanFactory) parentBeanFactory).doGetBean(
                    nameToLookup, requiredType, args, typeCheckOnly);
          }
          else if (args != null) {
            // Delegation to parent with explicit args.
            return (T) parentBeanFactory.getBean(nameToLookup, args);
          }
          else if (requiredType != null) {
            // No args -> delegate to standard getBean method.
            return (T) parentBeanFactory.getBean(nameToLookup, requiredType);
          }
          else {
            return (T) parentBeanFactory.getBean(nameToLookup);
          }
        }
        // no such bean
      }

      if (!typeCheckOnly) {
        markBeanAsCreated(beanName);
      }
      try {
        RootBeanDefinition merged = getMergedLocalBeanDefinition(beanName);
        checkMergedBeanDefinition(merged, beanName, args);

        // Guarantee initialization of beans that the current bean depends on.
        String[] dependsOn = merged.getDependsOn();
        if (dependsOn != null) {
          for (String dep : dependsOn) {
            if (isDependent(beanName, dep)) {
              throw new BeanCreationException(merged.getResourceDescription(), beanName,
                      "Circular depends-on relationship between '" + beanName + "' and '" + dep + "'");
            }
            registerDependentBean(dep, beanName);
            try {
              getBean(dep);
            }
            catch (NoSuchBeanDefinitionException ex) {
              throw new BeanCreationException(merged.getResourceDescription(), beanName,
                      "'" + beanName + "' depends on missing bean '" + dep + "'", ex);
            }
          }
        }

        // 3. Create bean instance.
        if (merged.isSingleton()) {
          beanInstance = getSingleton(beanName, () -> {
            try {
              Object bean = createBean(beanName, merged, args);
              // cache value
              return Objects.requireNonNullElse(bean, NullValue.INSTANCE);
            }
            catch (BeansException ex) {
              // Explicitly remove instance from singleton cache: It might have been put there
              // eagerly by the creation process, to allow for circular reference resolution.
              // Also remove any beans that received a temporary reference to the bean.
              destroySingleton(beanName);
              throw ex;
            }
          });
          // unwrap cache value (represent a null bean)
          if (beanInstance == NullValue.INSTANCE) {
            return null;
          }
        }
        else if (merged.isPrototype()) {
          // It's a prototype -> just create a new instance.
          try {
            beforePrototypeCreation(beanName);
            beanInstance = createBean(beanName, merged, args);
          }
          finally {
            afterPrototypeCreation(beanName);
          }
        }
        else {
          // other scope
          String scopeName = merged.getScope();
          if (StringUtils.isEmpty(scopeName)) {
            throw new IllegalStateException("No scope name defined for bean '" + beanName + "'");
          }

          Scope scope = this.scopes.get(scopeName);
          if (scope == null) {
            throw new IllegalStateException("No Scope registered for scope name '" + scopeName + "'");
          }

          try {
            beanInstance = scope.get(beanName, () -> {
              beforePrototypeCreation(beanName);
              try {
                return createBean(beanName, merged, args);
              }
              finally {
                afterPrototypeCreation(beanName);
              }
            });
          }
          catch (IllegalStateException ex) {
            throw new ScopeNotActiveException(beanName, scopeName, ex);
          }
        }
        // null (a null bean)
        if (beanInstance == null) {
          return null;
        }
        beanInstance = handleFactoryBean(name, beanName, merged, beanInstance);
      }
      catch (BeansException e) {
        cleanupAfterBeanCreationFailure(beanName);
        throw e;
      }
    }
    else if (beanInstance == NullValue.INSTANCE) {
      // unwrap cache value (represent a null bean)
      return null;
    }
    else {
      // is a singleton bean
      if (log.isDebugEnabled()) {
        if (isSingletonCurrentlyInCreation(beanName)) {
          log.trace("Returning eagerly cached instance of singleton bean '{}' " +
                  "that is not fully initialized yet - a consequence of a circular reference", beanName);
        }
        else {
          log.trace("Returning cached instance of singleton bean '{}'", beanName);
        }
      }
      beanInstance = handleFactoryBean(name, beanName, null, beanInstance);
    }
    return adaptBeanInstance(beanName, beanInstance, requiredType);
  }

  /**
   * Create a bean instance for the given merged bean definition (and arguments).
   * The bean definition will already have been merged with the parent definition
   * in case of a child definition.
   * <p>All bean retrieval methods delegate to this method for actual bean creation.
   *
   * @param beanName the name of the bean
   * @param merged the merged bean definition for the bean
   * @param args explicit arguments to use for constructor or factory method invocation
   * @return a new instance of the bean
   * @throws BeanCreationException if the bean could not be created
   */
  @Nullable
  protected abstract Object createBean(
          String beanName, RootBeanDefinition merged, @Nullable Object[] args) throws BeanCreationException;

  @Nullable
  @SuppressWarnings("unchecked")
  protected final <T> T adaptBeanInstance(String name, @Nullable Object bean, @Nullable Class<?> requiredType) {
    // Check if required type matches the type of the actual bean instance.
    if (bean != null && requiredType != null && !requiredType.isInstance(bean)) {
      try {
        Object convertedBean = getTypeConverter().convertIfNecessary(bean, requiredType);
        if (convertedBean == null) {
          throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
        }
        return (T) convertedBean;
      }
      catch (TypeMismatchException ex) {
        if (log.isDebugEnabled()) {
          log.trace("Failed to convert bean '{}' to required type '{}'",
                  name, ClassUtils.getQualifiedName(requiredType), ex);
        }
        throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
      }
    }
    return (T) bean;
  }

  @Override
  public boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
    return isTypeMatch(name, ResolvableType.forRawClass(typeToMatch));
  }

  @Override
  public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
    return isTypeMatch(name, typeToMatch, true);
  }

  protected boolean isTypeMatch(String name, ResolvableType typeToMatch, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {
    String beanName = transformedBeanName(name);

    // Check manually registered singletons.
    Object beanInstance = getSingleton(beanName, false);
    boolean isFactoryDereference = BeanFactoryUtils.isFactoryDereference(name);

    if (beanInstance != null && beanInstance != NullValue.INSTANCE) {
      if (beanInstance instanceof FactoryBean) {
        if (!isFactoryDereference) {
          Class<?> type = getTypeForFactoryBean((FactoryBean<?>) beanInstance);
          return type != null && typeToMatch.isAssignableFrom(type);
        }
        else {
          return typeToMatch.isInstance(beanInstance);
        }
      }
      else if (!isFactoryDereference) {
        if (typeToMatch.isInstance(beanInstance)) {
          // Direct match for exposed instance?
          return true;
        }
        else if (typeToMatch.hasGenerics() && containsBeanDefinition(beanName)) {
          // Generics potentially only match on the target class, not on the proxy...
          RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
          Class<?> targetType = mbd.getTargetType();
          if (targetType != null && targetType != ClassUtils.getUserClass(beanInstance)) {
            // Check raw class match as well, making sure it's exposed on the proxy.
            Class<?> classToMatch = typeToMatch.resolve();
            if (classToMatch != null && !classToMatch.isInstance(beanInstance)) {
              return false;
            }
            if (typeToMatch.isAssignableFrom(targetType)) {
              return true;
            }
          }
          ResolvableType resolvableType = mbd.targetType;
          if (resolvableType == null) {
            resolvableType = mbd.factoryMethodReturnType;
          }
          return (resolvableType != null && typeToMatch.isAssignableFrom(resolvableType));
        }
      }
      return false;
    }
    else if (containsSingleton(beanName) && !containsBeanDefinition(beanName)) {
      // null instance registered
      return false;
    }

    // No singleton instance found -> check bean definition.
    BeanFactory parentBeanFactory = getParentBeanFactory();
    if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
      // No bean definition found in this factory -> delegate to parent.
      return parentBeanFactory.isTypeMatch(originalBeanName(name), typeToMatch);
    }

    // Retrieve corresponding bean definition.
    RootBeanDefinition merged = getMergedLocalBeanDefinition(beanName);
    BeanDefinitionHolder decorated = merged.getDecoratedDefinition();

    // Setup the types that we want to match against
    Class<?> classToMatch = typeToMatch.resolve();
    if (classToMatch == null) {
      classToMatch = FactoryBean.class;
    }
    Class<?>[] typesToMatch = FactoryBean.class == classToMatch
                              ? new Class<?>[] { classToMatch }
                              : new Class<?>[] { FactoryBean.class, classToMatch };

    // Attempt to predict the bean type
    Class<?> predictedType = null;

    // We're looking for a regular reference but we're a factory bean that has
    // a decorated bean definition. The target bean should be the same type
    // as FactoryBean would ultimately return.
    if (!isFactoryDereference && decorated != null && isFactoryBean(beanName, merged)) {
      // We should only attempt if the user explicitly set lazy-init to true
      // and we know the merged bean definition is for a factory bean.
      if (!merged.isLazyInit() || allowFactoryBeanInit) {
        RootBeanDefinition tbd = getMergedBeanDefinition(decorated.getBeanName(), decorated.getBeanDefinition(), merged);
        Class<?> targetType = predictBeanType(decorated.getBeanName(), tbd, typesToMatch);
        if (targetType != null && !FactoryBean.class.isAssignableFrom(targetType)) {
          predictedType = targetType;
        }
      }
    }

    // If we couldn't use the target type, try regular prediction.
    if (predictedType == null) {
      predictedType = predictBeanType(beanName, merged, typesToMatch);
      if (predictedType == null) {
        return false;
      }
    }

    // Attempt to get the actual ResolvableType for the bean.
    ResolvableType beanType = null;

    // If it's a FactoryBean, we want to look at what it creates, not the factory class.
    if (FactoryBean.class.isAssignableFrom(predictedType)) {
      if (beanInstance == null && !isFactoryDereference) {
        beanType = getTypeForFactoryBean(beanName, merged, allowFactoryBeanInit);
        predictedType = beanType.resolve();
        if (predictedType == null) {
          return false;
        }
      }
    }
    else if (isFactoryDereference) {
      // Special case: A SmartInstantiationAwareBeanPostProcessor returned a non-FactoryBean
      // type but we nevertheless are being asked to dereference a FactoryBean...
      // Let's check the original bean class and proceed with it if it is a FactoryBean.
      predictedType = predictBeanType(beanName, merged, FactoryBean.class);
      if (predictedType == null || !FactoryBean.class.isAssignableFrom(predictedType)) {
        return false;
      }
    }

    // We don't have an exact type but if bean definition target type or the factory
    // method return type matches the predicted type then we can use that.
    if (beanType == null) {
      ResolvableType definedType = merged.targetType;
      if (definedType == null) {
        definedType = merged.factoryMethodReturnType;
      }
      if (definedType != null && definedType.resolve() == predictedType) {
        beanType = definedType;
      }
    }

    // If we have a bean type use it so that generics are considered
    if (beanType != null) {
      return typeToMatch.isAssignableFrom(beanType);
    }

    // If we don't have a bean type, fallback to the predicted type
    return typeToMatch.isAssignableFrom(predictedType);
  }

  /**
   * Resolve the bean class for the specified bean definition,
   * resolving a bean class name into a Class reference (if necessary)
   * and storing the resolved Class in the bean definition for further use.
   *
   * @param beanName beanName the name of the bean (for error handling purposes)
   * @param merged the merged bean definition to determine the class for
   * @param typesToMatch the types to match in case of internal type matching purposes
   * (also signals that the returned {@code Class} will never be exposed to application code)
   * @return the resolved bean class (or {@code null} if none)
   * @throws BeanClassLoadFailedException if we failed to load the class
   */
  @Nullable
  protected Class<?> resolveBeanClass(String beanName, RootBeanDefinition merged, Class<?>... typesToMatch) throws BeanClassLoadFailedException {
    if (merged.hasBeanClass()) {
      return merged.getBeanClass();
    }
    String beanClassName = merged.getBeanClassName();
    try {

      ClassLoader beanClassLoader = getBeanClassLoader();
      ClassLoader dynamicLoader = beanClassLoader;
      boolean freshResolve = false;

      if (ObjectUtils.isNotEmpty(typesToMatch)) {
        // When just doing type checks (i.e. not creating an actual instance yet),
        // use the specified temporary class loader (e.g. in a weaving scenario).
        ClassLoader tempClassLoader = getTempClassLoader();
        if (tempClassLoader != null) {
          dynamicLoader = tempClassLoader;
          freshResolve = true;
          if (tempClassLoader instanceof DecoratingClassLoader dcl) {
            for (Class<?> typeToMatch : typesToMatch) {
              dcl.excludeClass(typeToMatch.getName());
            }
          }
        }
      }

      String className = merged.getBeanClassName();
      if (className != null) {
        Object evaluated = evaluateBeanDefinitionString(className, merged);
        if (!className.equals(evaluated)) {
          // A dynamically resolved expression, supported as of 4.0...
          if (evaluated instanceof Class<?> clazz) {
            return clazz;
          }
          else if (evaluated instanceof String str) {
            className = str;
            freshResolve = true;
          }
          else {
            throw new IllegalStateException("Invalid class name expression result: " + evaluated);
          }
        }
        if (freshResolve) {
          // When resolving against a temporary class loader, exit early in order
          // to avoid storing the resolved Class in the bean definition.
          if (dynamicLoader != null) {
            try {
              return dynamicLoader.loadClass(className);
            }
            catch (ClassNotFoundException ex) {
              log.trace("Could not load class [{}] from {}: {}", className, dynamicLoader, ex.toString());
            }
          }
          return ClassUtils.forName(className, dynamicLoader);
        }
      }

      return merged.resolveBeanClass(beanClassLoader);
    }
    catch (ClassNotFoundException ex) {
      throw new BeanClassLoadFailedException(merged.getResourceDescription(), beanName, beanClassName, ex);
    }
    catch (LinkageError err) {
      throw new BeanClassLoadFailedException(merged.getResourceDescription(), beanName, beanClassName, err);
    }
  }

  /**
   * Determine whether the given bean name is already in use within this factory,
   * i.e. whether there is a local bean or alias registered under this name or
   * an inner bean created with this name.
   *
   * @param beanName the name to check
   */
  public boolean isBeanNameInUse(String beanName) {
    return isAlias(beanName) || containsLocalBean(beanName) || hasDependentBean(beanName);
  }

  /**
   * Determine whether the given bean requires destruction on shutdown.
   * <p>The default implementation checks the DisposableBean interface as well as
   * a specified destroy method and registered DestructionAwareBeanPostProcessors.
   *
   * @param bean the bean instance to check
   * @param mbd the corresponding bean definition
   * @see DisposableBean
   * @see DestructionAwareBeanPostProcessor
   */
  protected boolean requiresDestruction(Object bean, RootBeanDefinition mbd) {
    return bean != null && (DisposableBeanAdapter.hasDestroyMethod(bean, mbd)
            || DisposableBeanAdapter.hasApplicableProcessors(bean, postProcessors().destruction));
  }

  /**
   * Add the given bean to the list of disposable beans in this factory,
   * registering its DisposableBean interface and/or the given destroy method
   * to be called on factory shutdown (if applicable). Only applies to singletons.
   *
   * @param beanName the name of the bean
   * @param bean the bean instance
   * @param mbd the bean definition for the bean
   * @see BeanDefinition#isSingleton
   * @see #registerDisposableBean
   */
  protected void registerDisposableBeanIfNecessary(String beanName, Object bean, RootBeanDefinition mbd) {
    if (!mbd.isPrototype() && requiresDestruction(bean, mbd)) {
      if (mbd.isSingleton()) {
        // Register a DisposableBean implementation that performs all destruction
        // work for the given bean: DestructionAwareBeanPostProcessors,
        // DisposableBean interface, custom destroy method.
        registerDisposableBean(beanName, new DisposableBeanAdapter(
                beanName, bean, mbd, postProcessors().destruction));
      }
      else {
        // A bean with a custom scope...
        Scope scope = this.scopes.get(mbd.getScope());
        if (scope == null) {
          throw new IllegalStateException("No Scope registered for scope name '" + mbd.getScope() + "'");
        }
        scope.registerDestructionCallback(
                beanName, new DisposableBeanAdapter(beanName, bean, mbd, postProcessors().destruction));
      }
    }
  }

  @Override
  public abstract Map<String, BeanDefinition> getBeanDefinitions();

  /**
   * register bean-def for
   */
  protected abstract void registerBeanDefinition(String beanName, BeanDefinition def);

  @Override
  public void registerDependency(Class<?> dependencyType, @Nullable Object autowiredValue) {
    Assert.notNull(dependencyType, "Dependency type must not be null");
    if (autowiredValue != null) {
      if (!(autowiredValue instanceof Supplier<?> || dependencyType.isInstance(autowiredValue))) {
        throw new IllegalArgumentException("Value [" + autowiredValue +
                "] does not implement specified dependency type [" + dependencyType.getName() + "]");
      }
      objectFactories.put(dependencyType, autowiredValue);
    }
  }

  // ---------------------------------------

  @Override
  public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
    String beanName = transformedBeanName(name);
    Object beanInstance = getSingleton(beanName, false);
    if (beanInstance != null) {
      if (beanInstance instanceof FactoryBean) {
        return (BeanFactoryUtils.isFactoryDereference(name) || ((FactoryBean<?>) beanInstance).isSingleton());
      }
      else {
        return !BeanFactoryUtils.isFactoryDereference(name);
      }
    }

    // No singleton instance found -> check bean definition.
    BeanFactory parentBeanFactory = getParentBeanFactory();
    if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
      // No bean definition found in this factory -> delegate to parent.
      return parentBeanFactory.isSingleton(originalBeanName(name));
    }

    RootBeanDefinition definition = getMergedLocalBeanDefinition(beanName);
    // In case of FactoryBean, return singleton status of created object if not a dereference.
    if (definition.isSingleton()) {
      if (isFactoryBean(beanName, definition)) {
        if (BeanFactoryUtils.isFactoryDereference(name)) {
          return true;
        }
        FactoryBean<?> factoryBean = (FactoryBean<?>) getBean(FACTORY_BEAN_PREFIX + beanName);
        Assert.state(factoryBean != null, "Never get here");
        return factoryBean.isSingleton();
      }
      else {
        return !BeanFactoryUtils.isFactoryDereference(name);
      }
    }
    else {
      return false;
    }
  }

  @Override
  public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
    String beanName = transformedBeanName(name);

    BeanFactory parentBeanFactory = getParentBeanFactory();
    if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
      // No bean definition found in this factory -> delegate to parent.
      return parentBeanFactory.isPrototype(originalBeanName(name));
    }

    RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
    if (mbd.isPrototype()) {
      // In case of FactoryBean, return singleton status of created object if not a dereference.
      return !BeanFactoryUtils.isFactoryDereference(name) || isFactoryBean(beanName, mbd);
    }

    // Singleton or scoped - not a prototype.
    // However, FactoryBean may still produce a prototype object...
    if (BeanFactoryUtils.isFactoryDereference(name)) {
      return false;
    }
    if (isFactoryBean(beanName, mbd)) {
      FactoryBean<?> fb = (FactoryBean<?>) getBean(FACTORY_BEAN_PREFIX + beanName);
      if (fb instanceof SmartFactoryBean<?> smart) {
        return smart.isPrototype() || !fb.isSingleton();
      }
      return fb != null && !fb.isSingleton();
    }
    else {
      return false;
    }
  }

  @Override
  public Class<?> getType(String beanName) {
    return getType(beanName, true);
  }

  @Nullable
  @Override
  public Class<?> getType(String name, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {
    String beanName = transformedBeanName(name);
    // Check manually registered singletons.
    Object beanInstance = getSingleton(beanName, false);
    if (beanInstance != null && beanInstance != NullValue.INSTANCE) {
      if (beanInstance instanceof FactoryBean && !BeanFactoryUtils.isFactoryDereference(name)) {
        // If it's a FactoryBean, we want to look at what it creates, not at the factory class.
        return getTypeForFactoryBean((FactoryBean<?>) beanInstance);
      }
      else {
        return beanInstance.getClass();
      }
    }

    // No singleton instance found -> check bean definition.
    BeanFactory parentBeanFactory = getParentBeanFactory();
    if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
      // No bean definition found in this factory -> delegate to parent.
      return parentBeanFactory.getType(originalBeanName(name), allowFactoryBeanInit);
    }

    RootBeanDefinition mergedDef = getMergedLocalBeanDefinition(beanName);
    Class<?> beanClass = predictBeanType(beanName, mergedDef);

    if (beanClass != null) {
      // Check bean class whether we're dealing with a FactoryBean.
      if (FactoryBean.class.isAssignableFrom(beanClass)) {
        if (!BeanFactoryUtils.isFactoryDereference(name)) {
          // If it's a FactoryBean, we want to look at what it creates, not at the factory class.
          beanClass = getTypeForFactoryBean(beanName, mergedDef, allowFactoryBeanInit).resolve();
        }
      }
      else if (BeanFactoryUtils.isFactoryDereference(name)) {
        return null;
      }
    }

    if (beanClass == null) {
      // Check decorated bean definition, if any: We assume it'll be easier
      // to determine the decorated bean's type than the proxy's type.
      BeanDefinitionHolder decorated = mergedDef.getDecoratedDefinition();
      if (decorated != null && !BeanFactoryUtils.isFactoryDereference(name)) {
        RootBeanDefinition tbd = getMergedBeanDefinition(
                decorated.getBeanName(), decorated.getBeanDefinition(), mergedDef);
        Class<?> targetClass = predictBeanType(decorated.getBeanName(), tbd);
        if (targetClass != null && !FactoryBean.class.isAssignableFrom(targetClass)) {
          return targetClass;
        }
      }
    }

    return beanClass;
  }

  // FactoryBean

  /**
   * Determine the bean type for the given FactoryBean definition, as far as possible.
   * Only called if there is no singleton instance registered for the target bean
   * already. The implementation is allowed to instantiate the target factory bean if
   * {@code allowInit} is {@code true} and the type cannot be determined another way;
   * otherwise it is restricted to introspecting signatures and related metadata.
   * <p>If no {@link FactoryBean#OBJECT_TYPE_ATTRIBUTE} if set on the bean definition
   * and {@code allowInit} is {@code true}, the default implementation will create
   * the FactoryBean via {@code getBean} to call its {@code getObjectType} method.
   * Subclasses are encouraged to optimize this, typically by inspecting the generic
   * signature of the factory bean class or the factory method that creates it.
   * If subclasses do instantiate the FactoryBean, they should consider trying the
   * {@code getObjectType} method without fully populating the bean. If this fails,
   * a full FactoryBean creation as performed by this implementation should be used
   * as fallback.
   *
   * @param beanName the name of the bean
   * @param merged the merged bean definition for the bean
   * @param allowInit if initialization of the FactoryBean is permitted if the type
   * cannot be determined another way
   * @return the type for the bean if determinable, otherwise {@code ResolvableType.NONE}
   * @see FactoryBean#getObjectType()
   * @see #getBean(String)
   * @since 4.0
   */
  protected ResolvableType getTypeForFactoryBean(String beanName, RootBeanDefinition merged, boolean allowInit) {
    ResolvableType result = getTypeForFactoryBeanFromAttributes(merged);
    if (result != ResolvableType.NONE) {
      return result;
    }

    if (allowInit && merged.isSingleton()) {
      try {
        FactoryBean<?> factoryBean = doGetBean(
                FACTORY_BEAN_PREFIX + beanName, FactoryBean.class, null, true);
        Class<?> objectType = getTypeForFactoryBean(factoryBean);
        return objectType != null ? ResolvableType.forClass(objectType) : ResolvableType.NONE;
      }
      catch (BeanCreationException ex) {
        if (ex.contains(BeanCurrentlyInCreationException.class)) {
          log.trace("Bean currently in creation on FactoryBean type check: {}", ex.toString());
        }
        else if (merged.isLazyInit()) {
          log.trace("Bean creation exception on lazy FactoryBean type check: {}", ex.toString());
        }
        else {
          log.debug("Bean creation exception on eager FactoryBean type check: {}", ex.toString());
        }
        onSuppressedException(ex);
      }
    }
    return ResolvableType.NONE;
  }

  /**
   * Determine the type for the given FactoryBean.
   *
   * @param factoryBean the FactoryBean instance to check
   * @return the FactoryBean's object type,
   * or {@code null} if the type cannot be determined yet
   */
  @Nullable
  protected Class<?> getTypeForFactoryBean(FactoryBean<?> factoryBean) {
    try {
      return factoryBean.getObjectType();
    }
    catch (Throwable ex) {
      // Thrown from the FactoryBean's getObjectType implementation.
      log.info("FactoryBean threw exception from getObjectType, despite the contract saying " +
              "that it should return null if the type of its object cannot be determined yet", ex);
      return null;
    }
  }

  /**
   * Predict the eventual bean type (of the processed bean instance) for the
   * specified bean. Called by {@link #getType} and {@link #isTypeMatch}.
   * Does not need to handle FactoryBeans specifically, since it is only
   * supposed to operate on the raw bean type.
   * <p>This implementation is simplistic in that it is not able to
   * handle factory methods and InstantiationAwareBeanPostProcessors.
   * It only predicts the bean type correctly for a standard bean.
   * To be overridden in subclasses, applying more sophisticated type detection.
   *
   * @param beanName the name of the bean
   * @param merged the merged bean definition to determine the type for
   * @param typesToMatch the types to match in case of internal type matching purposes
   * (also signals that the returned {@code Class} will never be exposed to application code)
   * @return the type of the bean, or {@code null} if not predictable
   */
  @Nullable
  protected Class<?> predictBeanType(String beanName, RootBeanDefinition merged, Class<?>... typesToMatch) {
    Class<?> targetType = merged.getTargetType();
    if (targetType != null) {
      return targetType;
    }
    if (merged.getFactoryMethodName() != null) {
      return null;
    }
    return resolveBeanClass(beanName, merged, typesToMatch);
  }

  @Override
  public boolean isFactoryBean(String name) throws NoSuchBeanDefinitionException {
    String beanName = transformedBeanName(name);
    Object beanInstance = getSingleton(beanName, false);
    if (beanInstance != null) {
      return beanInstance instanceof FactoryBean;
    }
    // No singleton instance found -> check bean definition.
    if (!containsBeanDefinition(beanName)
            && getParentBeanFactory() instanceof ConfigurableBeanFactory parent) {
      // No bean definition found in this factory -> delegate to parent.
      return parent.isFactoryBean(name);
    }
    return isFactoryBean(beanName, getMergedLocalBeanDefinition(beanName));
  }

  /**
   * Check whether the given bean is defined as a {@link FactoryBean}.
   *
   * @param beanName the name of the bean
   * @param def the corresponding bean definition
   */
  protected boolean isFactoryBean(String beanName, RootBeanDefinition def) {
    Boolean result = def.isFactoryBean;
    if (result == null) {
      Class<?> beanType = predictBeanType(beanName, def, FactoryBean.class);
      result = beanType != null && FactoryBean.class.isAssignableFrom(beanType);
      def.isFactoryBean = result;
    }
    return result;
  }

  @Override
  public boolean containsBean(String name) {
    String beanName = transformedBeanName(name);
    if (containsSingleton(beanName) || containsBeanDefinition(beanName)) {
      return !BeanFactoryUtils.isFactoryDereference(name) || isFactoryBean(name);
    }
    // Not found -> check parent.
    BeanFactory parentBeanFactory = getParentBeanFactory();
    return parentBeanFactory != null && parentBeanFactory.containsBean(originalBeanName(beanName));
  }

  @Override
  public String[] getAliases(String name) {
    String beanName = transformedBeanName(name);
    ArrayList<String> aliases = new ArrayList<>();
    boolean factoryPrefix = name.startsWith(FACTORY_BEAN_PREFIX);
    String fullBeanName = beanName;
    if (factoryPrefix) {
      fullBeanName = FACTORY_BEAN_PREFIX + beanName;
    }
    if (!fullBeanName.equals(name)) {
      aliases.add(fullBeanName);
    }
    String[] retrievedAliases = super.getAliases(beanName);
    String prefix = factoryPrefix ? FACTORY_BEAN_PREFIX : Constant.BLANK;
    for (String retrievedAlias : retrievedAliases) {
      String alias = prefix + retrievedAlias;
      if (!alias.equals(name)) {
        aliases.add(alias);
      }
    }
    if (!containsSingleton(beanName) && !containsBeanDefinition(beanName)) {
      BeanFactory parentBeanFactory = getParentBeanFactory();
      if (parentBeanFactory != null) {
        CollectionUtils.addAll(aliases, parentBeanFactory.getAliases(fullBeanName));
      }
    }
    return StringUtils.toStringArray(aliases);
  }

  /**
   * Return the bean name, stripping out the factory dereference prefix if necessary,
   * and resolving aliases to canonical names.
   *
   * @param name the user-specified name
   * @return the transformed bean name
   */
  protected String transformedBeanName(String name) {
    return canonicalName(BeanFactoryUtils.transformedBeanName(name));
  }

  /**
   * Determine the original bean name, resolving locally defined aliases to canonical names.
   *
   * @param name the user-specified name
   * @return the original bean name
   */
  protected String originalBeanName(String name) {
    String beanName = transformedBeanName(name);
    if (name.startsWith(FACTORY_BEAN_PREFIX)) {
      beanName = FACTORY_BEAN_PREFIX + beanName;
    }
    return beanName;
  }

  //---------------------------------------------------------------------
  // Implementation of ArgumentsResolverProvider interface
  //---------------------------------------------------------------------

  /** @since 4.0 */
  @Override
  public DependencyInjector getInjector() {
    if (dependencyInjector == null) {
      this.dependencyInjector = new DependencyInjector(this);
    }
    return dependencyInjector;
  }

  //---------------------------------------------------------------------
  // Implementation of HierarchicalBeanFactory interface
  //---------------------------------------------------------------------

  @Override
  @Nullable
  public BeanFactory getParentBeanFactory() {
    return this.parentBeanFactory;
  }

  @Override
  public boolean containsLocalBean(String name) {
    String beanName = transformedBeanName(name);
    return (containsSingleton(beanName) || containsBeanDefinition(beanName))
            && (!BeanFactoryUtils.isFactoryDereference(name) || isFactoryBean(beanName));
  }

  //---------------------------------------------------------------------
  // Implementation of ConfigurableBeanFactory interface
  //---------------------------------------------------------------------

  @Override
  public void destroyBean(String beanName, Object beanInstance) {
    destroyBean(beanName, beanInstance, getMergedLocalBeanDefinition(beanName));
  }

  /**
   * Destroy the given bean instance (usually a prototype instance
   * obtained from this factory) according to the given bean definition.
   *
   * @param beanName the name of the bean definition
   * @param bean the bean instance to destroy
   * @param mbd the merged bean definition
   */
  protected void destroyBean(String beanName, Object bean, RootBeanDefinition mbd) {
    new DisposableBeanAdapter(
            beanName, bean, mbd, postProcessors().destruction).destroy();
  }

  // Scope

  @Override
  public void registerScope(String scopeName, Scope scope) {
    Assert.notNull(scope, "scope object must not be null");
    Assert.notNull(scopeName, "scope name must not be null");
    if (Scope.SINGLETON.equals(scopeName) || Scope.PROTOTYPE.equals(scopeName)) {
      throw new IllegalArgumentException("Cannot replace existing scopes 'singleton' and 'prototype'");
    }
    Scope previous = this.scopes.put(scopeName, scope);
    if (previous != null && previous != scope) {
      log.debug("Replacing scope '{}' from [{}] to [{}]", scopeName, previous, scope);
    }
    else if (log.isDebugEnabled()) {
      log.trace("Registering scope '{}' with implementation [{}]", scopeName, scope);
    }
  }

  @Override
  public String[] getRegisteredScopeNames() {
    return StringUtils.toStringArray(this.scopes.keySet());
  }

  @Override
  @Nullable
  public Scope getRegisteredScope(String scopeName) {
    Assert.notNull(scopeName, "Scope identifier must not be null");
    return this.scopes.get(scopeName);
  }

  @Override
  public void destroyScopedBean(String beanName) {
    RootBeanDefinition merged = getMergedLocalBeanDefinition(beanName);
    if (merged.isSingleton() || merged.isPrototype()) {
      throw new IllegalArgumentException(
              "Bean name '" + beanName + "' does not correspond to an object in a mutable scope");
    }
    Scope scope = scopes.get(merged.getScope());
    if (scope == null) {
      throw new IllegalStateException("No Scope SPI registered for scope name '" + merged.getScope() + "'");
    }
    Object bean = scope.remove(beanName);
    if (bean != null) {
      destroyBean(beanName, bean, merged);
    }
  }

  @Override
  public void setParentBeanFactory(@Nullable BeanFactory parentBeanFactory) {
    if (this.parentBeanFactory != null && this.parentBeanFactory != parentBeanFactory) {
      throw new IllegalStateException("Already associated with parent BeanFactory: " + this.parentBeanFactory);
    }
    if (this == parentBeanFactory) {
      throw new IllegalStateException("Cannot set parent bean factory to self");
    }
    this.parentBeanFactory = parentBeanFactory;
  }

  @Override
  public void setConversionService(@Nullable ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  //  @since 4.0 for bean-property conversion
  @Override
  @Nullable
  public ConversionService getConversionService() {
    return conversionService;
  }

  @Override
  public void setBeanExpressionResolver(@Nullable BeanExpressionResolver resolver) {
    this.beanExpressionResolver = resolver;
  }

  @Override
  @Nullable
  public BeanExpressionResolver getBeanExpressionResolver() {
    return this.beanExpressionResolver;
  }

  //

  @Override
  public void setBeanClassLoader(@Nullable ClassLoader beanClassLoader) {
    this.beanClassLoader = (beanClassLoader != null ? beanClassLoader : ClassUtils.getDefaultClassLoader());
  }

  @Nullable
  @Override
  public ClassLoader getBeanClassLoader() {
    return beanClassLoader;
  }

  @Override
  public void setTempClassLoader(@Nullable ClassLoader tempClassLoader) {
    this.tempClassLoader = tempClassLoader;
  }

  @Override
  @Nullable
  public ClassLoader getTempClassLoader() {
    return this.tempClassLoader;
  }

  @Override
  public void copyConfigurationFrom(ConfigurableBeanFactory otherFactory) {
    Assert.notNull(otherFactory, "BeanFactory must not be null");
    setBeanClassLoader(otherFactory.getBeanClassLoader());
    setCacheBeanMetadata(otherFactory.isCacheBeanMetadata());
    setConversionService(otherFactory.getConversionService());
    setBeanExpressionResolver(otherFactory.getBeanExpressionResolver());

    if (otherFactory instanceof AbstractBeanFactory beanFactory) {
      this.scopes.putAll(beanFactory.scopes);
      this.objectFactories.putAll(beanFactory.objectFactories);
      this.dependencyInjector = beanFactory.dependencyInjector;
      this.postProcessors.addAll(beanFactory.postProcessors);

      this.typeConverter = beanFactory.typeConverter;
      this.customEditors.putAll(beanFactory.customEditors);
      this.propertyEditorRegistrars.addAll(beanFactory.propertyEditorRegistrars);
    }
    else {
      String[] otherScopeNames = otherFactory.getRegisteredScopeNames();
      for (String scopeName : otherScopeNames) {
        this.scopes.put(scopeName, otherFactory.getRegisteredScope(scopeName));
      }
    }
  }

  //---------------------------------------------------------------------
  // BeanPostProcessor
  //---------------------------------------------------------------------

  public final List<BeanPostProcessor> getBeanPostProcessors() {
    return postProcessors;
  }

  protected final BeanPostProcessors postProcessors() {
    BeanPostProcessors postProcessors = postProcessorCache;
    if (postProcessors == null) {
      synchronized(this) {
        postProcessors = postProcessorCache;
        if (postProcessors == null) {
          postProcessors = new BeanPostProcessors(this.postProcessors);
          this.postProcessorCache = postProcessors;
        }
      }
    }
    return postProcessors;
  }

  @Override
  public void addBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
    Assert.notNull(beanPostProcessor, "BeanPostProcessor must not be null");
    invalidatePostProcessorsCache();

    postProcessors.remove(beanPostProcessor);
    postProcessors.add(beanPostProcessor);
  }

  /**
   * Add new BeanPostProcessors that will get applied to beans created
   * by this factory. To be invoked during factory configuration.
   *
   * @see #addBeanPostProcessor
   * @since 4.0
   */
  public void addBeanPostProcessors(Collection<? extends BeanPostProcessor> beanPostProcessors) {
    invalidatePostProcessorsCache();

    postProcessors.removeAll(beanPostProcessors);
    postProcessors.addAll(beanPostProcessors);
  }

  @Override
  public void removeBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
    invalidatePostProcessorsCache();
    postProcessors.remove(beanPostProcessor);
  }

  @Override
  public int getBeanPostProcessorCount() {
    return postProcessors.size();
  }

  // @since 4.0
  private void invalidatePostProcessorsCache() {
    postProcessorCache = null;
  }

  //---------------------------------------------------------------------
  // alreadyCreated
  //---------------------------------------------------------------------

  /**
   * Mark the specified bean as already created (or about to be created).
   * <p>This allows the bean factory to optimize its caching for repeated
   * creation of the specified bean.
   *
   * @param beanName the name of the bean
   * @since 4.0
   */
  protected void markBeanAsCreated(String beanName) {
    if (!alreadyCreated.contains(beanName)) {
      synchronized(mergedBeanDefinitions) {
        if (!alreadyCreated.contains(beanName)) {
          // Let the bean definition get re-merged now that we're actually creating
          // the bean... just in case some of its metadata changed in the meantime.
          clearMergedBeanDefinition(beanName);
          alreadyCreated.add(beanName);
        }
      }
    }
  }

  /**
   * Perform appropriate cleanup of cached metadata after bean creation failed.
   *
   * @param beanName the name of the bean
   * @since 4.0
   */
  protected void cleanupAfterBeanCreationFailure(String beanName) {
    this.alreadyCreated.remove(beanName);
  }

  /**
   * Determine whether the specified bean is eligible for having
   * its bean definition metadata cached.
   *
   * @param beanName the name of the bean
   * @return {@code true} if the bean's metadata may be cached
   * at this point already
   * @since 4.0
   */
  protected boolean isBeanEligibleForMetadataCaching(String beanName) {
    return this.alreadyCreated.contains(beanName);
  }

  /**
   * Remove the singleton instance (if any) for the given bean name,
   * but only if it hasn't been used for other purposes than type checking.
   *
   * @param beanName the name of the bean
   * @return {@code true} if actually removed, {@code false} otherwise
   * @since 4.0
   */
  protected boolean removeSingletonIfCreatedForTypeCheckOnly(String beanName) {
    if (!this.alreadyCreated.contains(beanName)) {
      removeSingleton(beanName);
      return true;
    }
    else {
      return false;
    }
  }

  /**
   * Check whether this factory's bean creation phase already started,
   * i.e. whether any bean has been marked as created in the meantime.
   *
   * @see #markBeanAsCreated
   * @since 4.0
   */
  protected boolean hasBeanCreationStarted() {
    return !this.alreadyCreated.isEmpty();
  }

  @Override
  public boolean isActuallyInCreation(String beanName) {
    return isSingletonCurrentlyInCreation(beanName) || isPrototypeCurrentlyInCreation(beanName);
  }

  /**
   * Return whether the specified prototype bean is currently in creation
   * (within the current thread).
   *
   * @param beanName the name of the bean
   * @since 4.0
   */
  protected boolean isPrototypeCurrentlyInCreation(String beanName) {
    Object curVal = this.prototypesCurrentlyInCreation.get();
    return (curVal != null &&
            (curVal.equals(beanName) || (curVal instanceof Set && ((Set<?>) curVal).contains(beanName))));
  }

  /**
   * Callback before prototype creation.
   * <p>The default implementation register the prototype as currently in creation.
   *
   * @param beanName the name of the prototype about to be created
   * @see #isPrototypeCurrentlyInCreation
   * @since 4.0
   */
  @SuppressWarnings("unchecked")
  protected void beforePrototypeCreation(String beanName) {
    Object curVal = this.prototypesCurrentlyInCreation.get();
    if (curVal == null) {
      this.prototypesCurrentlyInCreation.set(beanName);
    }
    else if (curVal instanceof String) {
      Set<String> beanNameSet = new HashSet<>(2);
      beanNameSet.add((String) curVal);
      beanNameSet.add(beanName);
      this.prototypesCurrentlyInCreation.set(beanNameSet);
    }
    else {
      Set<String> beanNameSet = (Set<String>) curVal;
      beanNameSet.add(beanName);
    }
  }

  /**
   * Callback after prototype creation.
   * <p>The default implementation marks the prototype as not in creation anymore.
   *
   * @param beanName the name of the prototype that has been created
   * @see #isPrototypeCurrentlyInCreation
   */
  @SuppressWarnings("unchecked")
  protected void afterPrototypeCreation(String beanName) {
    Object curVal = this.prototypesCurrentlyInCreation.get();
    if (curVal instanceof String) {
      this.prototypesCurrentlyInCreation.remove();
    }
    else if (curVal instanceof Set) {
      Set<String> beanNameSet = (Set<String>) curVal;
      beanNameSet.remove(beanName);
      if (beanNameSet.isEmpty()) {
        this.prototypesCurrentlyInCreation.remove();
      }
    }
  }

  //---------------------------------------------------------------------
  // FactoryBean
  //---------------------------------------------------------------------

  /**
   * Determine the bean type for a FactoryBean by inspecting its attributes for a
   * {@link FactoryBean#OBJECT_TYPE_ATTRIBUTE} value.
   *
   * @param attributes the attributes to inspect
   * @return a {@link ResolvableType} extracted from the attributes or
   * {@code ResolvableType.NONE}
   * @since 4.0
   */
  ResolvableType getTypeForFactoryBeanFromAttributes(AttributeAccessor attributes) {
    Object attribute = attributes.getAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE);
    if (attribute instanceof ResolvableType) {
      return (ResolvableType) attribute;
    }
    if (attribute instanceof Class) {
      return ResolvableType.forClass((Class<?>) attribute);
    }
    return ResolvableType.NONE;
  }

  /**
   * Get a FactoryBean for the given bean if possible.
   *
   * @param beanName the name of the bean
   * @param beanInstance the corresponding bean instance
   * @return the bean instance as FactoryBean
   * @throws BeansException if the given bean cannot be exposed as a FactoryBean
   */
  protected FactoryBean<?> getFactoryBean(String beanName, Object beanInstance) throws BeansException {
    if (beanInstance instanceof FactoryBean) {
      return (FactoryBean<?>) beanInstance;
    }
    throw new BeanCreationException(beanName,
            "Bean instance of type [" + beanInstance.getClass() + "] is not a FactoryBean");
  }

  /**
   * Get the object for the given bean instance, either the bean
   * instance itself or its created object in case of a FactoryBean.
   *
   * @param name the name that may include factory dereference prefix
   * @param beanName the canonical bean name
   * @param definition bean def
   * @param beanInstance the shared bean instance
   * @return the object to expose for the bean
   */
  @Nullable
  protected Object handleFactoryBean(String name, String beanName,
          @Nullable RootBeanDefinition definition, Object beanInstance) throws BeansException {
    // Don't let calling code try to dereference the factory if the bean isn't a factory.
    if (BeanFactoryUtils.isFactoryDereference(name)) {
      if (!(beanInstance instanceof FactoryBean)) {
        throw new BeanIsNotAFactoryException(beanName, beanInstance.getClass());
      }
      if (definition != null) {
        definition.isFactoryBean = true;
      }
      // get FactoryBean instance
      return beanInstance;
    }

    // Now we have the bean instance, which may be a normal bean or a FactoryBean.
    // If it's a FactoryBean, we use it to create a bean instance, unless the
    // caller actually wants a reference to the factory.
    if (beanInstance instanceof FactoryBean<?> factory) {
      if (log.isDebugEnabled()) {
        log.debug("Bean with name '{}' is a factory bean", beanName);
      }
      beanInstance = objectFromFactoryBeanCache.get(beanName);
      if (beanInstance == null) {
        if (definition != null) {
          definition.isFactoryBean = true;
        }
        // get bean from FactoryBean
        boolean synthetic = (definition != null && definition.isSynthetic());
        beanInstance = getObjectFromFactoryBean(factory, beanName, !synthetic);
      }
      // unwrap cache value
      if (beanInstance == NullValue.INSTANCE) {
        return null;
      }
    }
    return beanInstance;
  }

  /**
   * Obtain an object to expose from the given FactoryBean.
   *
   * @param factory the FactoryBean instance
   * @param beanName the name of the bean
   * @param shouldPostProcess whether the bean is subject to post-processing
   * @return the object obtained from the FactoryBean
   * @throws BeanCreationException if FactoryBean object creation failed
   * @see FactoryBean#getObject()
   */
  protected Object getObjectFromFactoryBean(FactoryBean<?> factory, String beanName, boolean shouldPostProcess) {
    if (factory.isSingleton() && containsSingleton(beanName)) {
      synchronized(getSingletonMutex()) {
        Object object = this.objectFromFactoryBeanCache.get(beanName);
        if (object == null) {
          object = doGetObjectFromFactoryBean(factory, beanName);
          // Only post-process and store if not put there already during getObject() call above
          // (e.g. because of circular reference processing triggered by custom getBean calls)
          Object alreadyThere = objectFromFactoryBeanCache.get(beanName);
          if (alreadyThere != null) {
            object = alreadyThere;
          }
          else {
            if (shouldPostProcess) {
              if (isSingletonCurrentlyInCreation(beanName)) {
                // Temporarily return non-post-processed object, not storing it yet..
                return object;
              }
              beforeSingletonCreation(beanName);
              try {
                object = postProcessObjectFromFactoryBean(object, beanName);
              }
              catch (Throwable ex) {
                throw new BeanCreationException(beanName,
                        "Post-processing of FactoryBean's singleton object failed", ex);
              }
              finally {
                afterSingletonCreation(beanName);
              }
            }
            if (containsSingleton(beanName)) {
              objectFromFactoryBeanCache.put(beanName, object);
            }
          }
        }
        return object;
      }
    }
    else {
      Object object = doGetObjectFromFactoryBean(factory, beanName);
      if (shouldPostProcess) {
        try {
          object = postProcessObjectFromFactoryBean(object, beanName);
        }
        catch (Throwable ex) {
          throw new BeanCreationException(beanName, "Post-processing of FactoryBean's object failed", ex);
        }
      }
      return object;
    }
  }

  /**
   * Obtain an object to expose from the given FactoryBean.
   *
   * @param factory the FactoryBean instance
   * @param beanName the name of the bean
   * @return the object obtained from the FactoryBean
   * @throws BeanCreationException if FactoryBean object creation failed
   * @see FactoryBean#getObject()
   */
  private Object doGetObjectFromFactoryBean(FactoryBean<?> factory, String beanName) throws BeanCreationException {
    Object object;
    try {
      object = factory.getObject();
    }
    catch (Throwable ex) {
      throw new BeanCreationException(beanName, "FactoryBean threw exception on object creation", ex);
    }

    // Do not accept a null value for a FactoryBean that's not fully
    // initialized yet: Many FactoryBeans just return null then.
    if (object == null) {
      if (isSingletonCurrentlyInCreation(beanName)) {
        throw new BeanCurrentlyInCreationException(
                beanName, "FactoryBean which is currently in creation returned null from getObject");
      }
      object = NullValue.INSTANCE;
    }
    return object;
  }

  /**
   * Post-process the given object that has been obtained from the FactoryBean.
   * The resulting object will get exposed for bean references.
   * <p>The default implementation simply returns the given object as-is.
   * Subclasses may override this, for example, to apply post-processors.
   *
   * @param object the object obtained from the FactoryBean.
   * @param beanName the name of the bean
   * @return the object to expose
   * @throws BeansException if any post-processing failed
   */
  protected Object postProcessObjectFromFactoryBean(Object object, String beanName) throws BeansException {
    return object;
  }

  /**
   * Overridden to clear the FactoryBean object cache as well.
   */
  @Override
  public void removeSingleton(String beanName) {
    synchronized(getSingletonMutex()) {
      super.removeSingleton(beanName);
      this.objectFromFactoryBeanCache.remove(beanName);
    }
  }

  /**
   * Overridden to clear the FactoryBean object cache as well.
   */
  @Override
  protected void clearSingletonCache() {
    synchronized(getSingletonMutex()) {
      super.clearSingletonCache();
      this.objectFromFactoryBeanCache.clear();
    }
  }

  @Override
  public void addEmbeddedValueResolver(StringValueResolver valueResolver) {
    Assert.notNull(valueResolver, "StringValueResolver must not be null");
    this.embeddedValueResolvers.add(valueResolver);
  }

  @Override
  public boolean hasEmbeddedValueResolver() {
    return !this.embeddedValueResolvers.isEmpty();
  }

  @Override
  @Nullable
  public String resolveEmbeddedValue(@Nullable String value) {
    if (value == null) {
      return null;
    }
    String result = value;
    for (StringValueResolver resolver : this.embeddedValueResolvers) {
      result = resolver.resolveStringValue(result);
      if (result == null) {
        return null;
      }
    }
    return result;
  }

  // @since 4.0
  @Override
  public void addPropertyEditorRegistrar(PropertyEditorRegistrar registrar) {
    Assert.notNull(registrar, "PropertyEditorRegistrar must not be null");
    this.propertyEditorRegistrars.add(registrar);
  }

  /**
   * Return the set of PropertyEditorRegistrars.
   *
   * @since 4.0
   */
  public Set<PropertyEditorRegistrar> getPropertyEditorRegistrars() {
    return this.propertyEditorRegistrars;
  }

  /**
   * Initialize the given BeanWrapper with the custom editors registered
   * with this factory. To be called for BeanWrappers that will create
   * and populate bean instances.
   * <p>The default implementation delegates to {@link #registerCustomEditors}.
   * Can be overridden in subclasses.
   *
   * @param bw the BeanWrapper to initialize
   * @since 4.0
   */
  protected void initBeanWrapper(BeanWrapper bw) {
    bw.setConversionService(getConversionService());
    registerCustomEditors(bw);
  }

  @Override
  public void registerCustomEditor(Class<?> requiredType, Class<? extends PropertyEditor> propertyEditorClass) {
    Assert.notNull(requiredType, "Required type must not be null");
    Assert.notNull(propertyEditorClass, "PropertyEditor class must not be null");
    this.customEditors.put(requiredType, propertyEditorClass);
  }

  @Override
  public void copyRegisteredEditorsTo(PropertyEditorRegistry registry) {
    registerCustomEditors(registry);
  }

  /**
   * Return the map of custom editors, with Classes as keys and PropertyEditor classes as values.
   */
  public Map<Class<?>, Class<? extends PropertyEditor>> getCustomEditors() {
    return this.customEditors;
  }

  @Override
  public void setTypeConverter(@Nullable TypeConverter typeConverter) {
    this.typeConverter = typeConverter;
  }

  /**
   * Return the custom TypeConverter to use, if any.
   *
   * @return the custom TypeConverter, or {@code null} if none specified
   */
  @Nullable
  protected TypeConverter getCustomTypeConverter() {
    return this.typeConverter;
  }

  @Override
  public TypeConverter getTypeConverter() {
    TypeConverter customConverter = getCustomTypeConverter();
    if (customConverter != null) {
      return customConverter;
    }
    else {
      // Build default TypeConverter, registering custom editors.
      SimpleTypeConverter typeConverter = new SimpleTypeConverter();
      typeConverter.setConversionService(getConversionService());
      registerCustomEditors(typeConverter);
      return typeConverter;
    }
  }

  /**
   * Initialize the given PropertyEditorRegistry with the custom editors
   * that have been registered with this BeanFactory.
   * <p>To be called for BeanWrappers that will create and populate bean
   * instances, and for SimpleTypeConverter used for constructor argument
   * and factory method type conversion.
   *
   * @param registry the PropertyEditorRegistry to initialize
   * @since 4.0
   */
  protected void registerCustomEditors(PropertyEditorRegistry registry) {
    if (registry instanceof PropertyEditorRegistrySupport registrySupport) {
      registrySupport.useConfigValueEditors();
    }
    if (!propertyEditorRegistrars.isEmpty()) {
      for (PropertyEditorRegistrar registrar : propertyEditorRegistrars) {
        try {
          registrar.registerCustomEditors(registry);
        }
        catch (BeanCreationException ex) {
          Throwable rootCause = ex.getMostSpecificCause();
          if (rootCause instanceof BeanCurrentlyInCreationException bce) {
            String bceBeanName = bce.getBeanName();
            if (bceBeanName != null && isCurrentlyInCreation(bceBeanName)) {
              if (log.isDebugEnabled()) {
                log.debug("PropertyEditorRegistrar [{}] failed because it tried to obtain currently created bean '{}': ",
                        registrar.getClass().getName(), ex.getBeanName(), ex.getMessage());
              }
              onSuppressedException(ex);
              continue;
            }
          }
          throw ex;
        }
      }
    }

    if (!customEditors.isEmpty()) {
      for (Map.Entry<Class<?>, Class<? extends PropertyEditor>> entry : customEditors.entrySet()) {
        Class<?> requiredType = entry.getKey();
        Class<? extends PropertyEditor> editorClass = entry.getValue();
        registry.registerCustomEditor(requiredType, BeanUtils.newInstance(editorClass));
      }
    }
  }

  @Override
  public void setCacheBeanMetadata(boolean cacheBeanMetadata) {
    this.cacheBeanMetadata = cacheBeanMetadata;
  }

  @Override
  public boolean isCacheBeanMetadata() {
    return this.cacheBeanMetadata;
  }

  /**
   * Return a 'merged' BeanDefinition for the given bean name,
   * merging a child bean definition with its parent if necessary.
   * <p>This {@code getMergedBeanDefinition} considers bean definition
   * in ancestors as well.
   *
   * @param name the name of the bean to retrieve the merged definition for
   * (may be an alias)
   * @return a (potentially merged) RootBeanDefinition for the given bean
   * @throws NoSuchBeanDefinitionException if there is no bean with the given name
   * @throws BeanDefinitionStoreException in case of an invalid bean definition
   * @since 4.0
   */
  @Override
  public BeanDefinition getMergedBeanDefinition(String name) throws BeansException {
    String beanName = transformedBeanName(name);
    // Efficiently check whether bean definition exists in this factory.
    if (!containsBeanDefinition(beanName) && getParentBeanFactory() instanceof ConfigurableBeanFactory parent) {
      return parent.getMergedBeanDefinition(beanName);
    }
    // Resolve merged bean definition locally.
    return getMergedLocalBeanDefinition(beanName);
  }

  /**
   * Return a merged BeanDefinition, traversing the parent bean definition
   * if the specified bean corresponds to a child bean definition.
   *
   * @param beanName the name of the bean to retrieve the merged definition for
   * @return a (potentially merged) BeanDefinition for the given bean
   * @throws NoSuchBeanDefinitionException if there is no bean with the given name
   * @throws BeanDefinitionStoreException in case of an invalid bean definition
   */
  protected RootBeanDefinition getMergedLocalBeanDefinition(String beanName) throws BeansException {
    // Quick check on the concurrent map first, with minimal locking.
    RootBeanDefinition mbd = mergedBeanDefinitions.get(beanName);
    if (mbd != null && !mbd.stale) {
      return mbd;
    }
    return getMergedBeanDefinition(beanName, getBeanDefinition(beanName));
  }

  /**
   * Return a BeanDefinition for the given top-level bean, by merging with
   * the parent if the given bean's definition is a child bean definition.
   *
   * @param beanName the name of the bean definition
   * @param bd the original bean definition (Root/ChildBeanDefinition)
   * @return a (potentially merged) BeanDefinition for the given bean
   * @throws BeanDefinitionStoreException in case of an invalid bean definition
   */
  protected RootBeanDefinition getMergedBeanDefinition(String beanName, BeanDefinition bd) throws BeanDefinitionStoreException {
    return getMergedBeanDefinition(beanName, bd, null);
  }

  /**
   * Return a BeanDefinition for the given bean, by merging with the
   * parent if the given bean's definition is a child bean definition.
   *
   * @param beanName the name of the bean definition
   * @param bd the original bean definition (Root/ChildBeanDefinition)
   * @param containingBd the containing bean definition in case of inner bean,
   * or {@code null} in case of a top-level bean
   * @return a (potentially merged) BeanDefinition for the given bean
   * @throws BeanDefinitionStoreException in case of an invalid bean definition
   */
  protected RootBeanDefinition getMergedBeanDefinition(
          String beanName, BeanDefinition bd, @Nullable BeanDefinition containingBd)
          throws BeanDefinitionStoreException {

    synchronized(this.mergedBeanDefinitions) {
      RootBeanDefinition mbd = null;
      RootBeanDefinition previous = null;

      // Check with full lock now in order to enforce the same merged instance.
      if (containingBd == null) {
        mbd = this.mergedBeanDefinitions.get(beanName);
      }

      if (mbd == null || mbd.stale) {
        previous = mbd;
        if (bd.getParentName() == null) {
          // Use copy of given root bean definition.
          if (bd instanceof RootBeanDefinition rootBeanDef) {
            mbd = rootBeanDef.cloneBeanDefinition();
          }
          else {
            mbd = new RootBeanDefinition(bd);
          }
        }
        else {
          // Child bean definition: needs to be merged with parent.
          BeanDefinition pbd;
          try {
            String parentBeanName = transformedBeanName(bd.getParentName());
            if (!beanName.equals(parentBeanName)) {
              pbd = getMergedBeanDefinition(parentBeanName);
            }
            else {
              if (getParentBeanFactory() instanceof ConfigurableBeanFactory parent) {
                pbd = parent.getMergedBeanDefinition(parentBeanName);
              }
              else {
                throw new NoSuchBeanDefinitionException(parentBeanName,
                        "Parent name '" + parentBeanName + "' is equal to bean name '" + beanName +
                                "': cannot be resolved without a ConfigurableBeanFactory parent");
              }
            }
          }
          catch (NoSuchBeanDefinitionException ex) {
            throw new BeanDefinitionStoreException(bd.getResourceDescription(), beanName,
                    "Could not resolve parent bean definition '" + bd.getParentName() + "'", ex);
          }
          // Deep copy with overridden values.
          mbd = new RootBeanDefinition(pbd);
          mbd.overrideFrom(bd);
        }

        // Set default singleton scope, if not configured before.
        if (StringUtils.isEmpty(mbd.getScope())) {
          mbd.setScope(BeanDefinition.SCOPE_SINGLETON);
        }

        // A bean contained in a non-singleton bean cannot be a singleton itself.
        // Let's correct this on the fly here, since this might be the result of
        // parent-child merging for the outer bean, in which case the original inner bean
        // definition will not have inherited the merged outer bean's singleton status.
        if (containingBd != null && !containingBd.isSingleton() && mbd.isSingleton()) {
          mbd.setScope(containingBd.getScope());
        }

        // Cache the merged bean definition for the time being
        // (it might still get re-merged later on in order to pick up metadata changes)
        if (containingBd == null && isCacheBeanMetadata()) {
          this.mergedBeanDefinitions.put(beanName, mbd);
        }
      }
      if (previous != null) {
        copyRelevantMergedBeanDefinitionCaches(previous, mbd);
      }
      return mbd;
    }
  }

  private void copyRelevantMergedBeanDefinitionCaches(RootBeanDefinition previous, RootBeanDefinition mbd) {
    if (Objects.equals(mbd.getBeanClassName(), previous.getBeanClassName())
            && Objects.equals(mbd.getFactoryBeanName(), previous.getFactoryBeanName())
            && Objects.equals(mbd.getFactoryMethodName(), previous.getFactoryMethodName())) {
      ResolvableType targetType = mbd.targetType;
      ResolvableType previousTargetType = previous.targetType;
      if (targetType == null || targetType.equals(previousTargetType)) {
        mbd.targetType = previousTargetType;
        mbd.isFactoryBean = previous.isFactoryBean;
        mbd.resolvedTargetType = previous.resolvedTargetType;
        mbd.factoryMethodReturnType = previous.factoryMethodReturnType;
        mbd.factoryMethodToIntrospect = previous.factoryMethodToIntrospect;
      }
    }
  }

  /**
   * Check the given merged bean definition,
   * potentially throwing validation exceptions.
   *
   * @param mbd the merged bean definition to check
   * @param beanName the name of the bean
   * @param args the arguments for bean creation, if any
   * @throws BeanDefinitionStoreException in case of validation failure
   */
  protected void checkMergedBeanDefinition(RootBeanDefinition mbd, String beanName, @Nullable Object[] args)
          throws BeanDefinitionStoreException {

    if (mbd.isAbstract()) {
      throw new BeanIsAbstractException(beanName);
    }
  }

  /**
   * Remove the merged bean definition for the specified bean,
   * recreating it on next access.
   *
   * @param beanName the bean name to clear the merged definition for
   */
  protected void clearMergedBeanDefinition(String beanName) {
    RootBeanDefinition bd = this.mergedBeanDefinitions.get(beanName);
    if (bd != null) {
      bd.stale = true;
    }
  }

  /**
   * Clear the merged bean definition cache, removing entries for beans
   * which are not considered eligible for full metadata caching yet.
   * <p>Typically triggered after changes to the original bean definitions,
   * e.g. after applying a {@code BeanFactoryPostProcessor}. Note that metadata
   * for beans which have already been created at this point will be kept around.
   *
   * @since 4.0
   */
  public void clearMetadataCache() {
    for (var entry : mergedBeanDefinitions.entrySet()) {
      if (!isBeanEligibleForMetadataCaching(entry.getKey())) {
        entry.getValue().stale = true;
      }
    }
  }

  /**
   * Evaluate the given String as contained in a bean definition,
   * potentially resolving it as an expression.
   *
   * @param value the value to check
   * @param beanDefinition the bean definition that the value comes from
   * @return the resolved value
   * @see #setBeanExpressionResolver
   */
  @Nullable
  protected Object evaluateBeanDefinitionString(
          @Nullable String value, @Nullable BeanDefinition beanDefinition) {
    if (this.beanExpressionResolver == null) {
      return value;
    }

    Scope scope = null;
    if (beanDefinition != null) {
      String scopeName = beanDefinition.getScope();
      if (scopeName != null) {
        scope = getRegisteredScope(scopeName);
      }
    }
    return this.beanExpressionResolver.evaluate(value, new BeanExpressionContext(this, scope));
  }

  protected final static class BeanPostProcessors {
    public final ArrayList<MergedBeanDefinitionPostProcessor> definitions = new ArrayList<>();
    public final ArrayList<DestructionAwareBeanPostProcessor> destruction = new ArrayList<>();
    public final ArrayList<DependenciesBeanPostProcessor> dependencies = new ArrayList<>();
    public final ArrayList<InitializationBeanPostProcessor> initialization = new ArrayList<>();
    public final ArrayList<InstantiationAwareBeanPostProcessor> instantiation = new ArrayList<>();
    public final ArrayList<SmartInstantiationAwareBeanPostProcessor> smartInstantiation = new ArrayList<>();

    BeanPostProcessors(ArrayList<BeanPostProcessor> postProcessors) {
      for (BeanPostProcessor postProcessor : postProcessors) {
        if (postProcessor instanceof DestructionAwareBeanPostProcessor destruction) {
          this.destruction.add(destruction);
        }
        if (postProcessor instanceof DependenciesBeanPostProcessor dependencies) {
          this.dependencies.add(dependencies);
        }
        if (postProcessor instanceof InitializationBeanPostProcessor initialization) {
          this.initialization.add(initialization);
        }
        if (postProcessor instanceof InstantiationAwareBeanPostProcessor instantiation) {
          this.instantiation.add(instantiation);
        }
        if (postProcessor instanceof MergedBeanDefinitionPostProcessor definition) {
          this.definitions.add(definition);
        }
        if (postProcessor instanceof SmartInstantiationAwareBeanPostProcessor smartInstantiation) {
          this.smartInstantiation.add(smartInstantiation);
        }
      }

      this.definitions.trimToSize();
      this.destruction.trimToSize();
      this.dependencies.trimToSize();
      this.instantiation.trimToSize();
      this.initialization.trimToSize();
      this.smartInstantiation.trimToSize();
    }

  }

}
