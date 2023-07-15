/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.beans.factory.aot;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import cn.taketoday.aot.hint.ExecutableMode;
import cn.taketoday.beans.BeanInstantiationException;
import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.TypeConverter;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.UnsatisfiedDependencyException;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.config.ConstructorArgumentValues;
import cn.taketoday.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import cn.taketoday.beans.factory.config.DependencyDescriptor;
import cn.taketoday.beans.factory.support.AbstractAutowireCapableBeanFactory;
import cn.taketoday.beans.factory.support.BeanDefinitionValueResolver;
import cn.taketoday.beans.factory.support.InstanceSupplier;
import cn.taketoday.beans.factory.support.InstantiationStrategy;
import cn.taketoday.beans.factory.support.RegisteredBean;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.function.ThrowingBiFunction;
import cn.taketoday.util.function.ThrowingFunction;
import cn.taketoday.util.function.ThrowingSupplier;

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

  final ExecutableLookup lookup;

  @Nullable
  private final ThrowingBiFunction<RegisteredBean, AutowiredArguments, T> generator;

  @Nullable
  private final String[] shortcuts;

  private BeanInstanceSupplier(ExecutableLookup lookup,
          @Nullable ThrowingBiFunction<RegisteredBean, AutowiredArguments, T> generator,
          @Nullable String[] shortcuts) {

    this.lookup = lookup;
    this.generator = generator;
    this.shortcuts = shortcuts;
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
    Assert.notNull(parameterTypes, "'parameterTypes' must not be null");
    Assert.noNullElements(parameterTypes, "'parameterTypes' must not contain null elements");
    return new BeanInstanceSupplier<>(new ConstructorLookup(parameterTypes), null, null);
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

    Assert.notNull(declaringClass, "'declaringClass' must not be null");
    Assert.hasText(methodName, "'methodName' must not be empty");
    Assert.notNull(parameterTypes, "'parameterTypes' must not be null");
    Assert.noNullElements(parameterTypes, "'parameterTypes' must not contain null elements");
    return new BeanInstanceSupplier<>(
            new FactoryMethodLookup(declaringClass, methodName, parameterTypes),
            null, null);
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
  public BeanInstanceSupplier<T> withGenerator(
          ThrowingBiFunction<RegisteredBean, AutowiredArguments, T> generator) {

    Assert.notNull(generator, "'generator' must not be null");
    return new BeanInstanceSupplier<>(this.lookup, generator, this.shortcuts);
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
    Assert.notNull(generator, "'generator' must not be null");
    return new BeanInstanceSupplier<>(this.lookup,
            (registeredBean, args) -> generator.apply(registeredBean), this.shortcuts);
  }

  /**
   * Return a new {@link BeanInstanceSupplier} instance
   * that uses direct bean name injection shortcuts for specific parameters.
   *
   * @param beanNames the bean names to use as shortcuts (aligned with the
   * constructor or factory method parameters)
   * @return a new {@link BeanInstanceSupplier} instance
   * that uses the shortcuts
   */
  public BeanInstanceSupplier<T> withShortcuts(String... beanNames) {
    return new BeanInstanceSupplier<>(this.lookup, this.generator, beanNames);
  }

  @Override
  public T get(RegisteredBean registeredBean) throws Exception {
    Assert.notNull(registeredBean, "'registeredBean' must not be null");
    Executable executable = this.lookup.get(registeredBean);
    AutowiredArguments arguments = resolveArguments(registeredBean, executable);
    if (this.generator != null) {
      return invokeBeanSupplier(executable, () -> this.generator.apply(registeredBean, arguments));
    }
    return invokeBeanSupplier(executable,
            () -> instantiate(registeredBean.getBeanFactory(), executable, arguments.toArray()));
  }

  private T invokeBeanSupplier(Executable executable, ThrowingSupplier<T> beanSupplier) {
    if (!(executable instanceof Method method)) {
      return beanSupplier.get();
    }
    try {
      InstantiationStrategy.setCurrentlyInvokedFactoryMethod(method);
      return beanSupplier.get();
    }
    finally {
      InstantiationStrategy.setCurrentlyInvokedFactoryMethod(null);
    }
  }

  @Nullable
  @Override
  public Method getFactoryMethod() {
    if (this.lookup instanceof FactoryMethodLookup factoryMethodLookup) {
      return factoryMethodLookup.get();
    }
    return null;
  }

  /**
   * Resolve arguments for the specified registered bean.
   *
   * @param registeredBean the registered bean
   * @return the resolved constructor or factory method arguments
   */
  AutowiredArguments resolveArguments(RegisteredBean registeredBean) {
    Assert.notNull(registeredBean, "'registeredBean' must not be null");
    return resolveArguments(registeredBean, this.lookup.get(registeredBean));
  }

  private AutowiredArguments resolveArguments(RegisteredBean registeredBean, Executable executable) {
    Assert.isInstanceOf(AbstractAutowireCapableBeanFactory.class, registeredBean.getBeanFactory());

    int startIndex = (executable instanceof Constructor<?> constructor &&
            ClassUtils.isInnerClass(constructor.getDeclaringClass())) ? 1 : 0;
    int parameterCount = executable.getParameterCount();
    Object[] resolved = new Object[parameterCount - startIndex];
    Assert.isTrue(this.shortcuts == null || this.shortcuts.length == resolved.length,
            () -> "'shortcuts' must contain " + resolved.length + " elements");

    ConstructorArgumentValues argumentValues = resolveArgumentValues(registeredBean);
    Set<String> autowiredBeans = new LinkedHashSet<>(resolved.length);
    for (int i = startIndex; i < parameterCount; i++) {
      MethodParameter parameter = getMethodParameter(executable, i);
      DependencyDescriptor descriptor = new DependencyDescriptor(parameter, true);
      String shortcut = (this.shortcuts != null ? this.shortcuts[i - startIndex] : null);
      if (shortcut != null) {
        descriptor = new ShortcutDependencyDescriptor(descriptor, shortcut, registeredBean.getBeanClass());
      }
      ValueHolder argumentValue = argumentValues.getIndexedArgumentValue(i, null);
      resolved[i - startIndex] = resolveArgument(registeredBean, descriptor, argumentValue, autowiredBeans);
    }
    registerDependentBeans(registeredBean.getBeanFactory(), registeredBean.getBeanName(), autowiredBeans);

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

  private ConstructorArgumentValues resolveArgumentValues(RegisteredBean registeredBean) {
    ConstructorArgumentValues resolved = new ConstructorArgumentValues();
    RootBeanDefinition beanDefinition = registeredBean.getMergedBeanDefinition();
    if (beanDefinition.hasConstructorArgumentValues() &&
            registeredBean.getBeanFactory() instanceof AbstractAutowireCapableBeanFactory beanFactory) {
      BeanDefinitionValueResolver valueResolver = new BeanDefinitionValueResolver(
              beanFactory, registeredBean.getBeanName(), beanDefinition, beanFactory.getTypeConverter());
      ConstructorArgumentValues values = beanDefinition.getConstructorArgumentValues();
      values.getIndexedArgumentValues().forEach((index, valueHolder) -> {
        ValueHolder resolvedValue = resolveArgumentValue(valueResolver, valueHolder);
        resolved.addIndexedArgumentValue(index, resolvedValue);
      });
    }
    return resolved;
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
  private Object resolveArgument(RegisteredBean registeredBean, DependencyDescriptor descriptor,
          @Nullable ValueHolder argumentValue, Set<String> autowiredBeans) {

    TypeConverter typeConverter = registeredBean.getBeanFactory().getTypeConverter();
    if (argumentValue != null) {
      return argumentValue.isConverted() ?
             argumentValue.getConvertedValue() :
             typeConverter.convertIfNecessary(argumentValue.getValue(),
                     descriptor.getDependencyType(), descriptor.getMethodParameter());
    }
    try {
      return registeredBean.resolveAutowiredArgument(descriptor, typeConverter, autowiredBeans);
    }
    catch (BeansException ex) {
      throw new UnsatisfiedDependencyException(null, registeredBean.getBeanName(), descriptor, ex);
    }
  }

  @SuppressWarnings("unchecked")
  private T instantiate(ConfigurableBeanFactory beanFactory, Executable executable, Object[] args) {
    if (executable instanceof Constructor<?> constructor) {
      try {
        return (T) instantiate(constructor, args);
      }
      catch (Exception ex) {
        throw new BeanInstantiationException(constructor, ex.getMessage(), ex);
      }
    }
    if (executable instanceof Method method) {
      try {
        return (T) instantiate(beanFactory, method, args);
      }
      catch (Exception ex) {
        throw new BeanInstantiationException(method, ex.getMessage(), ex);
      }
    }
    throw new IllegalStateException("Unsupported executable " + executable.getClass().getName());
  }

  private Object instantiate(Constructor<?> constructor, Object[] args) throws Exception {
    Class<?> declaringClass = constructor.getDeclaringClass();
    if (ClassUtils.isInnerClass(declaringClass)) {
      Object enclosingInstance = createInstance(declaringClass.getEnclosingClass());
      args = ObjectUtils.addObjectToArray(args, enclosingInstance, 0);
    }
    ReflectionUtils.makeAccessible(constructor);
    return constructor.newInstance(args);
  }

  private Object instantiate(ConfigurableBeanFactory beanFactory, Method method, Object[] args) throws Exception {
    Object target = getFactoryMethodTarget(beanFactory, method);
    ReflectionUtils.makeAccessible(method);
    return method.invoke(target, args);
  }

  @Nullable
  private Object getFactoryMethodTarget(BeanFactory beanFactory, Method method) {
    if (Modifier.isStatic(method.getModifiers())) {
      return null;
    }
    Class<?> declaringClass = method.getDeclaringClass();
    return beanFactory.getBean(declaringClass);
  }

  private Object createInstance(Class<?> clazz) throws Exception {
    if (!ClassUtils.isInnerClass(clazz)) {
      Constructor<?> constructor = clazz.getDeclaredConstructor();
      ReflectionUtils.makeAccessible(constructor);
      return constructor.newInstance();
    }
    Class<?> enclosingClass = clazz.getEnclosingClass();
    Constructor<?> constructor = clazz.getDeclaredConstructor(enclosingClass);
    return constructor.newInstance(createInstance(enclosingClass));
  }

  private static String toCommaSeparatedNames(Class<?>... parameterTypes) {
    return Arrays.stream(parameterTypes).map(Class::getName).collect(Collectors.joining(", "));
  }

  /**
   * Performs lookup of the {@link Executable}.
   */
  static abstract class ExecutableLookup {

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
      Class<?> beanClass = registeredBean.getBeanClass();
      try {
        Class<?>[] actualParameterTypes = (!ClassUtils.isInnerClass(beanClass)) ?
                                          this.parameterTypes : ObjectUtils.addObjectToArray(
                this.parameterTypes, beanClass.getEnclosingClass(), 0);
        return beanClass.getDeclaredConstructor(actualParameterTypes);
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
      Method method = ReflectionUtils.findMethod(this.declaringClass, this.methodName, this.parameterTypes);
      Assert.notNull(method, () -> "%s cannot be found".formatted(this));
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
