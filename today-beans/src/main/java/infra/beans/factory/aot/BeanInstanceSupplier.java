/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.beans.factory.aot;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import infra.aot.hint.ExecutableMode;
import infra.beans.BeanInstantiationException;
import infra.beans.BeanUtils;
import infra.beans.BeansException;
import infra.beans.TypeConverter;
import infra.beans.factory.UnsatisfiedDependencyException;
import infra.beans.factory.config.ConstructorArgumentValues;
import infra.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import infra.beans.factory.config.DependencyDescriptor;
import infra.beans.factory.support.AbstractAutowireCapableBeanFactory;
import infra.beans.factory.support.BeanDefinitionValueResolver;
import infra.beans.factory.support.InstanceSupplier;
import infra.beans.factory.support.InstantiationStrategy;
import infra.beans.factory.support.RegisteredBean;
import infra.beans.factory.support.RootBeanDefinition;
import infra.core.MethodParameter;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.ClassUtils;
import infra.util.CollectionUtils;
import infra.util.ReflectionUtils;
import infra.util.function.ThrowingBiFunction;
import infra.util.function.ThrowingFunction;
import infra.util.function.ThrowingSupplier;

/**
 * Specialized {@link InstanceSupplier} that provides the factory {@link Method}
 * used to instantiate the underlying bean instance, if any. Transparently
 * handles resolution of {@link AutowiredArguments} if necessary. Typically used
 * in AOT-processed applications as a targeted alternative to the reflection
 * based injection.
 *
 * <p>If no {@code generator} is provided, reflection is used to instantiate the
 * bean instance, and full {@link ExecutableMode#INVOKE invocation} hints are
 * contributed. Multiple generator callback styles are supported:
 * <ul>
 * <li>A function with the {@code registeredBean} and resolved {@code arguments}
 * for executables that require arguments resolution. An
 * {@link ExecutableMode#INTROSPECT introspection} hint is added so that
 * parameter annotations can be read </li>
 * <li>A function with only the {@code registeredBean} for simpler cases that
 * do not require resolution of arguments</li>
 * <li>A supplier when a method reference can be used</li>
 * </ul>
 * Generator callbacks handle checked exceptions so that the caller does not
 * have to deal with them.
 *
 * @param <T> the type of instance supplied by this supplier
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see AutowiredArguments
 * @since 4.0
 */
public final class BeanInstanceSupplier<T> extends AutowiredElementResolver implements InstanceSupplier<T> {

  private final ExecutableLookup lookup;

  @Nullable
  private final ThrowingFunction<RegisteredBean, T> generatorWithoutArguments;

  @Nullable
  private final ThrowingBiFunction<RegisteredBean, AutowiredArguments, T> generatorWithArguments;

  @Nullable
  private final String[] shortcutBeanNames;

  private BeanInstanceSupplier(ExecutableLookup lookup,
          @Nullable ThrowingFunction<RegisteredBean, T> generatorWithoutArguments,
          @Nullable ThrowingBiFunction<RegisteredBean, AutowiredArguments, T> generatorWithArguments,
          @Nullable String[] shortcutBeanNames) {

    this.lookup = lookup;
    this.generatorWithoutArguments = generatorWithoutArguments;
    this.generatorWithArguments = generatorWithArguments;
    this.shortcutBeanNames = shortcutBeanNames;
  }

  /**
   * Create a {@link BeanInstanceSupplier} that resolves
   * arguments for the specified bean constructor.
   *
   * @param <T> the type of instance supplied
   * @param parameterTypes the constructor parameter types
   * @return a new {@link BeanInstanceSupplier} instance
   */
  public static <T> BeanInstanceSupplier<T> forConstructor(Class<?>... parameterTypes) {
    Assert.notNull(parameterTypes, "'parameterTypes' is required");
    Assert.noNullElements(parameterTypes, "'parameterTypes' must not contain null elements");
    return new BeanInstanceSupplier<>(new ConstructorLookup(parameterTypes),
            null, null, null);
  }

  /**
   * Create a new {@link BeanInstanceSupplier} that
   * resolves arguments for the specified factory method.
   *
   * @param <T> the type of instance supplied
   * @param declaringClass the class that declares the factory method
   * @param methodName the factory method name
   * @param parameterTypes the factory method parameter types
   * @return a new {@link BeanInstanceSupplier} instance
   */
  public static <T> BeanInstanceSupplier<T> forFactoryMethod(
          Class<?> declaringClass, String methodName, Class<?>... parameterTypes) {

    Assert.notNull(declaringClass, "'declaringClass' is required");
    Assert.hasText(methodName, "'methodName' must not be empty");
    Assert.notNull(parameterTypes, "'parameterTypes' is required");
    Assert.noNullElements(parameterTypes, "'parameterTypes' must not contain null elements");
    return new BeanInstanceSupplier<>(
            new FactoryMethodLookup(declaringClass, methodName, parameterTypes),
            null, null, null);
  }

  ExecutableLookup getLookup() {
    return this.lookup;
  }

  /**
   * Return a new {@link BeanInstanceSupplier} instance that uses the specified
   * {@code generator} bi-function to instantiate the underlying bean.
   *
   * @param generator a {@link ThrowingBiFunction} that uses the
   * {@link RegisteredBean} and resolved {@link AutowiredArguments} to
   * instantiate the underlying bean
   * @return a new {@link BeanInstanceSupplier} instance with the specified generator
   */
  public BeanInstanceSupplier<T> withGenerator(ThrowingBiFunction<RegisteredBean, AutowiredArguments, T> generator) {
    Assert.notNull(generator, "'generator' is required");
    return new BeanInstanceSupplier<>(this.lookup, null, generator, this.shortcutBeanNames);
  }

  /**
   * Return a new {@link BeanInstanceSupplier} instance that uses the specified
   * {@code generator} function to instantiate the underlying bean.
   *
   * @param generator a {@link ThrowingFunction} that uses the
   * {@link RegisteredBean} to instantiate the underlying bean
   * @return a new {@link BeanInstanceSupplier} instance with the specified generator
   */
  public BeanInstanceSupplier<T> withGenerator(ThrowingFunction<RegisteredBean, T> generator) {
    Assert.notNull(generator, "'generator' is required");
    return new BeanInstanceSupplier<>(this.lookup, generator, null, this.shortcutBeanNames);
  }

  /**
   * Return a new {@link BeanInstanceSupplier} instance that uses
   * direct bean name injection shortcuts for specific parameters.
   *
   * @param beanNames the bean names to use as shortcut (aligned with the
   * constructor or factory method parameters)
   * @return a new {@link BeanInstanceSupplier} instance that uses the
   * given shortcut bean names
   */
  public BeanInstanceSupplier<T> withShortcut(String... beanNames) {
    return new BeanInstanceSupplier<>(
            this.lookup, this.generatorWithoutArguments, this.generatorWithArguments, beanNames);
  }

  @SuppressWarnings("unchecked")
  @Override
  public T get(RegisteredBean registeredBean) {
    Assert.notNull(registeredBean, "'registeredBean' is required");
    if (this.generatorWithoutArguments != null) {
      Executable executable = getFactoryMethodForGenerator();
      return invokeBeanSupplier(executable, () -> this.generatorWithoutArguments.apply(registeredBean));
    }
    else if (this.generatorWithArguments != null) {
      Executable executable = getFactoryMethodForGenerator();
      AutowiredArguments arguments = resolveArguments(registeredBean,
              executable != null ? executable : this.lookup.get(registeredBean));
      return invokeBeanSupplier(executable, () -> this.generatorWithArguments.apply(registeredBean, arguments));
    }
    else {
      Executable executable = this.lookup.get(registeredBean);
      Object[] arguments = resolveArguments(registeredBean, executable).toArray();
      return invokeBeanSupplier(executable, () -> (T) instantiate(registeredBean, executable, arguments));
    }
  }

  @Override
  @Nullable
  public Method getFactoryMethod() {
    // Cached factory method retrieval for qualifier introspection etc.
    if (this.lookup instanceof FactoryMethodLookup factoryMethodLookup) {
      return factoryMethodLookup.get();
    }
    return null;
  }

  @Nullable
  private Method getFactoryMethodForGenerator() {
    // Avoid unnecessary currentlyInvokedFactoryMethod exposure outside of full configuration classes.
    if (this.lookup instanceof FactoryMethodLookup factoryMethodLookup &&
            factoryMethodLookup.declaringClass.getName().contains(ClassUtils.CGLIB_CLASS_SEPARATOR)) {
      return factoryMethodLookup.get();
    }
    return null;
  }

  private T invokeBeanSupplier(@Nullable Executable executable, ThrowingSupplier<T> beanSupplier) {
    if (executable instanceof Method method) {
      return InstantiationStrategy.instantiateWithFactoryMethod(method, beanSupplier);
    }
    return beanSupplier.get();
  }

  /**
   * Resolve arguments for the specified registered bean.
   *
   * @param registeredBean the registered bean
   * @return the resolved constructor or factory method arguments
   */
  AutowiredArguments resolveArguments(RegisteredBean registeredBean) {
    Assert.notNull(registeredBean, "'registeredBean' is required");
    return resolveArguments(registeredBean, this.lookup.get(registeredBean));
  }

  private AutowiredArguments resolveArguments(RegisteredBean registeredBean, Executable executable) {
    int parameterCount = executable.getParameterCount();
    Object[] resolved = new Object[parameterCount];
    Assert.isTrue(this.shortcutBeanNames == null || this.shortcutBeanNames.length == resolved.length,
            () -> "'shortcuts' must contain " + resolved.length + " elements");

    ValueHolder[] argumentValues = resolveArgumentValues(registeredBean, executable);
    Set<String> autowiredBeanNames = new LinkedHashSet<>(resolved.length * 2);
    int startIndex = (executable instanceof Constructor<?> constructor &&
            ClassUtils.isInnerClass(constructor.getDeclaringClass())) ? 1 : 0;
    for (int i = startIndex; i < parameterCount; i++) {
      MethodParameter parameter = getMethodParameter(executable, i);
      DependencyDescriptor descriptor = new DependencyDescriptor(parameter, true);
      String shortcut = (this.shortcutBeanNames != null ? this.shortcutBeanNames[i] : null);
      if (shortcut != null) {
        descriptor = new ShortcutDependencyDescriptor(descriptor, shortcut);
      }
      ValueHolder argumentValue = argumentValues[i];
      resolved[i] = resolveAutowiredArgument(
              registeredBean, descriptor, argumentValue, autowiredBeanNames);
    }
    registerDependentBeans(registeredBean.getBeanFactory(), registeredBean.getBeanName(), autowiredBeanNames);

    return AutowiredArguments.of(resolved);
  }

  private MethodParameter getMethodParameter(Executable executable, int index) {
    if (executable instanceof Constructor<?> constructor) {
      return new MethodParameter(constructor, index);
    }
    if (executable instanceof Method method) {
      return new MethodParameter(method, index);
    }
    throw new IllegalStateException("Unsupported executable: " + executable.getClass().getName());
  }

  private ValueHolder[] resolveArgumentValues(RegisteredBean registeredBean, Executable executable) {
    Parameter[] parameters = executable.getParameters();
    ValueHolder[] resolved = new ValueHolder[parameters.length];
    RootBeanDefinition beanDefinition = registeredBean.getMergedBeanDefinition();
    if (beanDefinition.hasConstructorArgumentValues() &&
            registeredBean.getBeanFactory() instanceof AbstractAutowireCapableBeanFactory beanFactory) {
      BeanDefinitionValueResolver valueResolver = new BeanDefinitionValueResolver(
              beanFactory, registeredBean.getBeanName(), beanDefinition, beanFactory.getTypeConverter());
      ConstructorArgumentValues values = resolveConstructorArguments(
              valueResolver, beanDefinition.getConstructorArgumentValues());
      var usedValueHolders = CollectionUtils.<ValueHolder>newHashSet(parameters.length);
      for (int i = 0; i < parameters.length; i++) {
        Class<?> parameterType = parameters[i].getType();
        String parameterName = (parameters[i].isNamePresent() ? parameters[i].getName() : null);
        ValueHolder valueHolder = values.getArgumentValue(
                i, parameterType, parameterName, usedValueHolders);
        if (valueHolder != null) {
          resolved[i] = valueHolder;
          usedValueHolders.add(valueHolder);
        }
      }
    }
    return resolved;
  }

  private ConstructorArgumentValues resolveConstructorArguments(
          BeanDefinitionValueResolver valueResolver, ConstructorArgumentValues constructorArguments) {

    ConstructorArgumentValues resolvedConstructorArguments = new ConstructorArgumentValues();
    for (Map.Entry<Integer, ConstructorArgumentValues.ValueHolder> entry : constructorArguments.getIndexedArgumentValues().entrySet()) {
      resolvedConstructorArguments.addIndexedArgumentValue(entry.getKey(), resolveArgumentValue(valueResolver, entry.getValue()));
    }
    for (ConstructorArgumentValues.ValueHolder valueHolder : constructorArguments.getGenericArgumentValues()) {
      resolvedConstructorArguments.addGenericArgumentValue(resolveArgumentValue(valueResolver, valueHolder));
    }
    return resolvedConstructorArguments;
  }

  private ValueHolder resolveArgumentValue(BeanDefinitionValueResolver resolver, ValueHolder valueHolder) {
    if (valueHolder.isConverted()) {
      return valueHolder;
    }
    Object value = resolver.resolveValueIfNecessary("constructor argument", valueHolder.getValue());
    ValueHolder resolvedHolder = new ValueHolder(value, valueHolder.getType(), valueHolder.getName());
    resolvedHolder.setSource(valueHolder);
    return resolvedHolder;
  }

  @Nullable
  private Object resolveAutowiredArgument(RegisteredBean registeredBean, DependencyDescriptor descriptor,
          @Nullable ValueHolder argumentValue, Set<String> autowiredBeanNames) {

    TypeConverter typeConverter = registeredBean.getBeanFactory().getTypeConverter();
    if (argumentValue != null) {
      return argumentValue.isConverted() ?
              argumentValue.getConvertedValue() :
              typeConverter.convertIfNecessary(argumentValue.getValue(),
                      descriptor.getDependencyType(), descriptor.getMethodParameter());
    }
    try {
      return registeredBean.resolveAutowiredArgument(descriptor, typeConverter, autowiredBeanNames);
    }
    catch (BeansException ex) {
      throw new UnsatisfiedDependencyException(null, registeredBean.getBeanName(), descriptor, ex);
    }
  }

  private Object instantiate(RegisteredBean registeredBean, Executable executable, Object[] args) {
    if (executable instanceof Constructor<?> constructor) {
      if (registeredBean.getBeanFactory() instanceof AbstractAutowireCapableBeanFactory aacb
              && registeredBean.getMergedBeanDefinition().hasMethodOverrides()) {
        return aacb.getInstantiationStrategy().instantiate(registeredBean.getMergedBeanDefinition(),
                registeredBean.getBeanName(), registeredBean.getBeanFactory());
      }
      return BeanUtils.newInstance(constructor, args);
    }
    if (executable instanceof Method method) {
      Object target = null;
      String factoryBeanName = registeredBean.getMergedBeanDefinition().getFactoryBeanName();
      if (factoryBeanName != null) {
        target = registeredBean.getBeanFactory().getBean(factoryBeanName, method.getDeclaringClass());
      }
      else if (!Modifier.isStatic(method.getModifiers())) {
        throw new IllegalStateException("Cannot invoke instance method without factoryBeanName: " + method);
      }
      try {
        ReflectionUtils.makeAccessible(method);
        return method.invoke(target, args);
      }
      catch (Throwable ex) {
        throw new BeanInstantiationException(method, ex.getMessage(), ex);
      }
    }
    throw new IllegalStateException("Unsupported executable " + executable.getClass().getName());
  }

  private static String toCommaSeparatedNames(Class<?>... parameterTypes) {
    return Arrays.stream(parameterTypes).map(Class::getName).collect(Collectors.joining(", "));
  }

  /**
   * Performs lookup of the {@link Executable}.
   */
  abstract static class ExecutableLookup {

    abstract Executable get(RegisteredBean registeredBean);
  }

  /**
   * Performs lookup of the {@link Constructor}.
   */
  private static class ConstructorLookup extends ExecutableLookup {

    private final Class<?>[] parameterTypes;

    ConstructorLookup(Class<?>[] parameterTypes) {
      this.parameterTypes = parameterTypes;
    }

    @Override
    public Executable get(RegisteredBean registeredBean) {
      Class<?> beanClass = registeredBean.getMergedBeanDefinition().getBeanClass();
      try {
        return beanClass.getDeclaredConstructor(this.parameterTypes);
      }
      catch (NoSuchMethodException ex) {
        throw new IllegalArgumentException(
                "%s cannot be found on %s".formatted(this, beanClass.getName()), ex);
      }
    }

    @Override
    public String toString() {
      return "Constructor with parameter types [%s]".formatted(toCommaSeparatedNames(this.parameterTypes));
    }
  }

  /**
   * Performs lookup of the factory {@link Method}.
   */
  private static class FactoryMethodLookup extends ExecutableLookup {

    private final Class<?> declaringClass;

    private final String methodName;

    private final Class<?>[] parameterTypes;

    @Nullable
    private volatile Method resolvedMethod;

    FactoryMethodLookup(Class<?> declaringClass, String methodName, Class<?>[] parameterTypes) {
      this.declaringClass = declaringClass;
      this.methodName = methodName;
      this.parameterTypes = parameterTypes;
    }

    @Override
    public Executable get(RegisteredBean registeredBean) {
      return get();
    }

    Method get() {
      Method method = this.resolvedMethod;
      if (method == null) {
        method = ReflectionUtils.findMethod(
                ClassUtils.getUserClass(this.declaringClass), this.methodName, this.parameterTypes);
        Assert.notNull(method, () -> "%s cannot be found".formatted(this));
        this.resolvedMethod = method;
      }
      return method;
    }

    @Override
    public String toString() {
      return "Factory method '%s' with parameter types [%s] declared on %s".formatted(
              this.methodName, toCommaSeparatedNames(this.parameterTypes),
              this.declaringClass);
    }
  }

}
