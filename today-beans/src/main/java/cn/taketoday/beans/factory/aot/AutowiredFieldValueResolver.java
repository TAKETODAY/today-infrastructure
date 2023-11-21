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

import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.aot.hint.ExecutableMode;
import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.TypeConverter;
import cn.taketoday.beans.factory.InjectionPoint;
import cn.taketoday.beans.factory.UnsatisfiedDependencyException;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.config.DependencyDescriptor;
import cn.taketoday.beans.factory.support.RegisteredBean;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.function.ThrowingConsumer;

/**
 * Resolver used to support the autowiring of fields. Typically used in
 * AOT-processed applications as a targeted alternative to the
 * {@link cn.taketoday.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor
 * AutowiredAnnotationBeanPostProcessor}.
 *
 * <p>When resolving arguments in a native image, the {@link Field} being used must
 * be marked with an {@link ExecutableMode#INTROSPECT introspection} hint so
 * that field annotations can be read. Full {@link ExecutableMode#INVOKE
 * invocation} hints are only required if the
 * {@link #resolveAndSet(RegisteredBean, Object)} method of this class is being
 * used (typically to support private fields).
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class AutowiredFieldValueResolver extends AutowiredElementResolver {

  private final String fieldName;

  private final boolean required;

  @Nullable
  private final String shortcut;

  private AutowiredFieldValueResolver(String fieldName, boolean required,
          @Nullable String shortcut) {

    Assert.hasText(fieldName, "'fieldName' must not be empty");
    this.fieldName = fieldName;
    this.required = required;
    this.shortcut = shortcut;
  }

  /**
   * Create a new {@link AutowiredFieldValueResolver} for the specified field
   * where injection is optional.
   *
   * @param fieldName the field name
   * @return a new {@link AutowiredFieldValueResolver} instance
   */
  public static AutowiredFieldValueResolver forField(String fieldName) {
    return new AutowiredFieldValueResolver(fieldName, false, null);
  }

  /**
   * Create a new {@link AutowiredFieldValueResolver} for the specified field
   * where injection is required.
   *
   * @param fieldName the field name
   * @return a new {@link AutowiredFieldValueResolver} instance
   */
  public static AutowiredFieldValueResolver forRequiredField(String fieldName) {
    return new AutowiredFieldValueResolver(fieldName, true, null);
  }

  /**
   * Return a new {@link AutowiredFieldValueResolver} instance that uses a
   * direct bean name injection shortcut.
   *
   * @param beanName the bean name to use as a shortcut
   * @return a new {@link AutowiredFieldValueResolver} instance that uses the
   * shortcuts
   */
  public AutowiredFieldValueResolver withShortcut(String beanName) {
    return new AutowiredFieldValueResolver(this.fieldName, this.required, beanName);
  }

  /**
   * Resolve the field for the specified registered bean and provide it to the
   * given action.
   *
   * @param registeredBean the registered bean
   * @param action the action to execute with the resolved field value
   */
  public <T> void resolve(RegisteredBean registeredBean, ThrowingConsumer<T> action) {
    Assert.notNull(registeredBean, "'registeredBean' is required");
    Assert.notNull(action, "'action' is required");
    T resolved = resolve(registeredBean);
    if (resolved != null) {
      action.accept(resolved);
    }
  }

  /**
   * Resolve the field value for the specified registered bean.
   *
   * @param registeredBean the registered bean
   * @param requiredType the required type
   * @return the resolved field value
   */
  @Nullable
  @SuppressWarnings("unchecked")
  public <T> T resolve(RegisteredBean registeredBean, Class<T> requiredType) {
    Object value = resolveObject(registeredBean);
    Assert.isInstanceOf(requiredType, value);
    return (T) value;
  }

  /**
   * Resolve the field value for the specified registered bean.
   *
   * @param registeredBean the registered bean
   * @return the resolved field value
   */
  @Nullable
  @SuppressWarnings("unchecked")
  public <T> T resolve(RegisteredBean registeredBean) {
    return (T) resolveObject(registeredBean);
  }

  /**
   * Resolve the field value for the specified registered bean.
   *
   * @param registeredBean the registered bean
   * @return the resolved field value
   */
  @Nullable
  public Object resolveObject(RegisteredBean registeredBean) {
    Assert.notNull(registeredBean, "'registeredBean' is required");
    return resolveValue(registeredBean, getField(registeredBean));
  }

  /**
   * Resolve the field value for the specified registered bean and set it
   * using reflection.
   *
   * @param registeredBean the registered bean
   * @param instance the bean instance
   */
  public void resolveAndSet(RegisteredBean registeredBean, Object instance) {
    Assert.notNull(registeredBean, "'registeredBean' is required");
    Assert.notNull(instance, "'instance' is required");
    Field field = getField(registeredBean);
    Object resolved = resolveValue(registeredBean, field);
    if (resolved != null) {
      ReflectionUtils.makeAccessible(field);
      ReflectionUtils.setField(field, instance, resolved);
    }
  }

  @Nullable
  private Object resolveValue(RegisteredBean registeredBean, Field field) {
    String beanName = registeredBean.getBeanName();
    Class<?> beanClass = registeredBean.getBeanClass();
    ConfigurableBeanFactory beanFactory = registeredBean.getBeanFactory();
    DependencyDescriptor descriptor = new DependencyDescriptor(field, this.required);
    descriptor.setContainingClass(beanClass);
    if (this.shortcut != null) {
      descriptor = new ShortcutDependencyDescriptor(descriptor, this.shortcut);
    }
    Set<String> autowiredBeanNames = new LinkedHashSet<>(1);
    TypeConverter typeConverter = beanFactory.getTypeConverter();
    try {
      Object value = beanFactory.resolveDependency(
              descriptor, beanName, autowiredBeanNames, typeConverter);
      registerDependentBeans(beanFactory, beanName, autowiredBeanNames);
      return value;
    }
    catch (BeansException ex) {
      throw new UnsatisfiedDependencyException(
              null, beanName, new InjectionPoint(field), ex);
    }
  }

  private Field getField(RegisteredBean registeredBean) {
    Field field = ReflectionUtils.findField(registeredBean.getBeanClass(),
            this.fieldName);
    Assert.notNull(field, () -> "No field '" + this.fieldName + "' found on "
            + registeredBean.getBeanClass().getName());
    return field;
  }

}
