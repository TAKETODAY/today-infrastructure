/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.context.factory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.AttributeAccessorSupport;
import cn.taketoday.context.Constant;
import cn.taketoday.context.Ordered;
import cn.taketoday.context.Scope;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.exception.NoSuchPropertyException;
import cn.taketoday.context.reflect.BeanConstructor;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.CollectionUtils;
import cn.taketoday.context.utils.ContextUtils;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.context.utils.OrderUtils;
import cn.taketoday.context.utils.ReflectionUtils;
import cn.taketoday.context.utils.StringUtils;

import static cn.taketoday.context.utils.ContextUtils.resolveParameter;

/**
 * Default implementation of {@link BeanDefinition}
 *
 * @author TODAY <br>
 * 2019-02-01 12:23
 */
public class DefaultBeanDefinition
        extends AttributeAccessorSupport implements BeanDefinition, Ordered {

  /** bean name. */
  private String name;
  /** bean class. */
  private final Class<?> beanClass;
  /** bean scope. */
  private String scope;

  /**
   * Invoke before {@link InitializingBean#afterPropertiesSet}
   *
   * @since 2.3.3
   */
  private Method[] initMethods = EMPTY_METHOD;

  /**
   * Invoke after when publish
   * {@link ApplicationContext#publishEvent(Object)}
   *
   * @since 2.3.3
   */
  private String[] destroyMethods = Constant.EMPTY_STRING_ARRAY;

  /** property values */
  private PropertyValue[] propertyValues = EMPTY_PROPERTY_VALUE;

  /**
   * <p>
   * This is a very important property.
   * <p>
   * If registry contains it's singleton instance, we don't know the instance has
   * initialized or not, so must be have a flag to prove it has initialized
   *
   * @since 2.0.0
   */
  private boolean initialized = false;

  /**
   * Mark as a {@link FactoryBean}.
   *
   * @since 2.0.0
   */
  private boolean factoryBean = false;

  /** Child implementation */
  private BeanDefinition childDef;

  /** @since 3.0 */
  private Executable executable;
  /** @since 3.0 */
  private BeanConstructor<?> constructor;
  /** lazy init flag @since 3.0 */
  private Boolean lazyInit;

  public DefaultBeanDefinition(String name, Class<?> beanClass) {
    this.name = name;
    this.beanClass = beanClass;
  }

  /**
   * Build a {@link BeanDefinition} with given child {@link BeanDefinition}
   *
   * @param beanName
   *         Bean name
   * @param childDef
   *         Child {@link BeanDefinition}
   */
  public DefaultBeanDefinition(String beanName, BeanDefinition childDef) {
    this(beanName, childDef.getBeanClass());
    setChild(childDef);
    setScope(childDef.getScope());
    setInitMethods(childDef.getInitMethods());
    setDestroyMethods(childDef.getDestroyMethods());
    setPropertyValues(childDef.getPropertyValues());
  }

  @Override
  public PropertyValue getPropertyValue(String name) {
    for (PropertyValue propertyValue : propertyValues) {
      if (propertyValue.getName().equals(name)) {
        return propertyValue;
      }
    }
    throw new NoSuchPropertyException("No such property named: [" + name + "]");
  }

  @Override
  public boolean isSingleton() {
    final String scope = getScope();
    return StringUtils.isEmpty(scope) || Scope.SINGLETON.equals(scope);
  }

  @Override
  public boolean isPrototype() {
    return Scope.PROTOTYPE.equals(scope);
  }

  @Override
  public Class<?> getBeanClass() {
    return beanClass;
  }

  @Override
  public Method[] getInitMethods() {
    return initMethods;
  }

  @Override
  public String[] getDestroyMethods() {
    return destroyMethods;
  }

  @Override
  public String getScope() {
    return scope;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public boolean isFactoryBean() {
    return factoryBean;
  }

  @Override
  public boolean isInitialized() {
    return initialized;
  }

  @Override
  public boolean isAbstract() {
    return childDef != null;
  }

  @Override
  public PropertyValue[] getPropertyValues() {
    return propertyValues;
  }

  // -----------------------

  @Override
  public BeanDefinition setInitialized(boolean initialized) {
    this.initialized = initialized;
    return this;
  }

  @Override
  public BeanDefinition setFactoryBean(boolean factoryBean) {
    this.factoryBean = factoryBean;
    return this;
  }

  @Override
  public BeanDefinition setName(String name) {
    this.name = name;
    return this;
  }

  @Override
  public BeanDefinition setScope(String scope) {
    this.scope = scope;
    return this;
  }

  @Override
  public BeanDefinition setInitMethods(Method... initMethods) {
    this.initMethods = initMethods;
    return this;
  }

  @Override
  public BeanDefinition setInitMethods(String... initMethods) {
    ConfigurationException.nonNull(beanClass, "Bean Class must applied before invoke this method");
    return setInitMethods(ContextUtils.resolveInitMethod(initMethods, beanClass));
  }

  @Override
  public BeanDefinition setDestroyMethods(String... destroyMethods) {
    this.destroyMethods = destroyMethods;
    return this;
  }

  @Override
  public BeanDefinition setPropertyValues(PropertyValue... propertyValues) {
    this.propertyValues = propertyValues;
    return this;
  }

  @Override
  public void addPropertyValue(PropertyValue... newValues) {

    if (ObjectUtils.isNotEmpty(newValues)) { // fix

      final PropertyValue[] propertyValues = this.propertyValues;
      if (propertyValues == null) {
        this.propertyValues = newValues;
      }
      else {
        List<PropertyValue> pool = new ArrayList<>(newValues.length + propertyValues.length);

        Collections.addAll(pool, newValues);
        Collections.addAll(pool, propertyValues);

        this.propertyValues = pool.toArray(new PropertyValue[pool.size()]);
      }
    }
  }

  @Override
  public void addPropertyValue(Collection<PropertyValue> propertyValues) {

    if (CollectionUtils.isEmpty(propertyValues)) {
      return;
    }

    if (this.propertyValues != null) {
      Collections.addAll(propertyValues, this.propertyValues);
    }
    this.propertyValues = propertyValues.toArray(new PropertyValue[propertyValues.size()]);
  }

  /**
   * {@link BeanDefinition}'s Order
   *
   * @since 2.1.7
   */
  @Override
  public int getOrder() {
    return OrderUtils.getOrder(getBeanClass());
  }

  @Override
  public BeanDefinition getChild() {
    return childDef;
  }

  /**
   * Apply the child bean name
   *
   * @param childDef
   *         Child BeanDefinition
   *
   * @return {@link DefaultBeanDefinition}
   */
  public DefaultBeanDefinition setChild(BeanDefinition childDef) {
    this.childDef = childDef;
    return this;
  }

  // AnnotatedElement
  // -----------------------------

  @Override
  public boolean isAnnotationPresent(Class<? extends Annotation> annotation) {
    return ClassUtils.isAnnotationPresent(getBeanClass(), annotation);
  }

  @Override
  public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
    return ClassUtils.getAnnotation(annotationClass, getBeanClass());
  }

  @Override
  public Annotation[] getAnnotations() {
    return getBeanClass().getAnnotations();
  }

  @Override
  public Annotation[] getDeclaredAnnotations() {
    return getBeanClass().getDeclaredAnnotations();
  }

  public BeanConstructor<?> getConstructor(BeanFactory factory) {
    if (constructor == null) {
      constructor = createConstructor(factory);
    }
    return constructor;
  }

  protected BeanConstructor<?> createConstructor(BeanFactory factory) {
    return ReflectionUtils.newConstructor(getBeanClass());
  }

  public Executable getExecutable() {
    if (executable == null) {
      executable = ClassUtils.getSuitableConstructor(getBeanClass());
    }
    return executable;
  }

  @Override
  public Object newInstance(final BeanFactory factory) {
    final BeanConstructor<?> target = getConstructor(factory);
    return target.newInstance(resolveParameter(getExecutable(), factory));
  }

  @Override
  public Object newInstance(BeanFactory factory, Object... args) {
    final BeanConstructor<?> target = getConstructor(factory);
    return target.newInstance(args);
  }

  /**
   * Set whether this bean should be lazily initialized.
   * <p>If {@code false}, the bean will get instantiated on startup by bean
   * factories that perform eager initialization of singletons.
   *
   * @since 3.0
   */
  @Override
  public void setLazyInit(boolean lazyInit) {
    this.lazyInit = lazyInit;
  }

  /**
   * Return whether this bean should be lazily initialized, i.e. not
   * eagerly instantiated on startup. Only applicable to a singleton bean.
   *
   * @return whether to apply lazy-init semantics ({@code false} by default)
   *
   * @since 3.0
   */
  @Override
  public boolean isLazyInit() {
    return (this.lazyInit != null && this.lazyInit);
  }

  /**
   * Return whether this bean should be lazily initialized, i.e. not
   * eagerly instantiated on startup. Only applicable to a singleton bean.
   *
   * @return the lazy-init flag if explicitly set, or {@code null} otherwise
   *
   * @since 3.0
   */
  public Boolean getLazyInit() {
    return this.lazyInit;
  }

  // Object

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof DefaultBeanDefinition) {
      final DefaultBeanDefinition other = (DefaultBeanDefinition) obj;
      return Objects.equals(name, other.name)
              && Objects.equals(scope, other.scope)
              && beanClass == other.beanClass
              && Objects.equals(childDef, other.childDef)
              && Objects.deepEquals(initMethods, other.initMethods)
              && Objects.deepEquals(destroyMethods, other.destroyMethods)
              && Objects.deepEquals(propertyValues, other.propertyValues);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, beanClass);
  }

  @Override
  public String toString() {

    return new StringBuilder()//
            .append("{\n\t\"name\":\"").append(name)//
            .append("\",\n\t\"scope\":\"").append(scope)//
            .append("\",\n\t\"beanClass\":\"").append(beanClass)//
            .append("\",\n\t\"initMethods\":\"").append(Arrays.toString(initMethods))//
            .append("\",\n\t\"destroyMethods\":\"").append(Arrays.toString(destroyMethods))//
            .append("\",\n\t\"propertyValues\":\"").append(Arrays.toString(propertyValues))//
            .append("\",\n\t\"initialized\":\"").append(initialized)//
            .append("\",\n\t\"factoryBean\":\"").append(factoryBean)//
            .append("\",\n\t\"child\":\"").append(childDef)//
            .append("\"\n}")//
            .toString();
  }
}
