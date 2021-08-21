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
package cn.taketoday.beans.factory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import cn.taketoday.beans.FactoryBean;
import cn.taketoday.beans.InitializingBean;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.Scope;
import cn.taketoday.context.loader.NoSuchPropertyException;
import cn.taketoday.core.Assert;
import cn.taketoday.core.AttributeAccessorSupport;
import cn.taketoday.core.Constant;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.reflect.BeanConstructor;
import cn.taketoday.core.reflect.MethodInvoker;
import cn.taketoday.util.AnnotationUtils;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ContextUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.OrderUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;

import static cn.taketoday.util.ContextUtils.resolveParameter;

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
  private Class<?> beanClass;
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
  private PropertySetter[] propertySetters = EMPTY_PROPERTY_SETTER;

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
  /** @since 3.0 fast invoke init methods */
  private MethodInvoker[] methodInvokers;
  /** @since 3.0 bean instance supplier */
  private Supplier<?> instanceSupplier;

  public DefaultBeanDefinition(String name, Class<?> beanClass) {
    setName(name);
    setBeanClass(beanClass);
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
    copy(childDef);
    setName(beanName);
    setChild(childDef);
  }

  @Override
  public PropertySetter getPropertyValue(String name) {
    for (PropertySetter propertySetter : propertySetters) {
      if (propertySetter.getName().equals(name)) {
        return propertySetter;
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

  public void setBeanClass(Class<?> beanClass) {
    this.beanClass = beanClass;
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
  public PropertySetter[] getPropertySetters() {
    return propertySetters;
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
    if (ObjectUtils.isNotEmpty(initMethods)) {
      this.initMethods = initMethods;
      this.methodInvokers = new MethodInvoker[initMethods.length];
      int i = 0;
      for (final Method initMethod : initMethods) {
        methodInvokers[i++] = MethodInvoker.create(initMethod);
      }
    }
    else {
      this.initMethods = EMPTY_METHOD;
      this.methodInvokers = null;
    }
    return this;
  }

  @Override
  public BeanDefinition setInitMethods(String... initMethods) {
    return setInitMethods(ContextUtils.resolveInitMethod(initMethods, obtainBeanClass()));
  }

  @Override
  public BeanDefinition setDestroyMethods(String... destroyMethods) {
    this.destroyMethods = destroyMethods;
    return this;
  }

  @Override
  public BeanDefinition setPropertyValues(PropertySetter... propertySetters) {
    this.propertySetters = propertySetters;
    return this;
  }

  @Override
  public void addPropertyValue(final String name, final Object value) {
    Assert.notNull(name, "property name must not be null");

    final Field field = ReflectionUtils.findField(obtainBeanClass(), name);
    if (field == null) {
      throw new IllegalArgumentException("property '" + name + "' not found");
    }
    final DefaultPropertySetter propertyValue = new DefaultPropertySetter(value, field);
    addPropertySetter(propertyValue);
  }

  @Override
  public void addPropertySetter(PropertySetter... setters) {
    if (ObjectUtils.isNotEmpty(setters)) {
      final PropertySetter[] propertySetters = getPropertySetters();
      if (ObjectUtils.isEmpty(propertySetters)) {
        setPropertyValues(setters);
      }
      else {
        List<PropertySetter> pool = new ArrayList<>(setters.length + propertySetters.length);

        Collections.addAll(pool, setters);
        Collections.addAll(pool, propertySetters);

        setPropertyValues(pool.toArray(new PropertySetter[pool.size()]));
      }
    }
  }

  @Override
  public void addPropertySetter(Collection<PropertySetter> newValues) {
    if (CollectionUtils.isEmpty(newValues)) {
      return;
    }
    final PropertySetter[] propertySetters = getPropertySetters();
    if (ObjectUtils.isNotEmpty(propertySetters)) {
      Collections.addAll(newValues, propertySetters);
    }
    setPropertyValues(newValues.toArray(new PropertySetter[newValues.size()]));
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
    return AnnotationUtils.isPresent(getBeanClass(), annotation);
  }

  @Override
  public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
    return AnnotationUtils.getAnnotation(annotationClass, getBeanClass());
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
    BeanConstructor<?> constructor = this.constructor;
    if (constructor == null) {
      constructor = createConstructor(factory);
      this.constructor = constructor;
    }
    return constructor;
  }

  protected BeanConstructor<?> createConstructor(BeanFactory factory) {
    return ReflectionUtils.newConstructor(getBeanClass());
  }

  public Executable getExecutable() {
    Executable executable = this.executable;
    if (executable == null) {
      executable = ClassUtils.getSuitableConstructor(getBeanClass());
      this.executable = executable;
    }
    return executable;
  }

  /** @since 3.0 */
  @Override
  public Object newInstance(final BeanFactory factory) {
    final Supplier<?> instanceSupplier = this.instanceSupplier;
    if (instanceSupplier != null) {
      return instanceSupplier.get();
    }
    final BeanConstructor<?> target = getConstructor(factory);
    final Object[] args = resolveParameter(getExecutable(), factory);
    return target.newInstance(args);
  }

  /**
   * @param factory
   *         input bean factory
   * @param args
   *         arguments to use when creating a corresponding instance
   *
   * @since 3.0
   */
  @Override
  public Object newInstance(BeanFactory factory, Object... args) {
    final BeanConstructor<?> target = getConstructor(factory);
    return target.newInstance(args);
  }

  /**
   * use {@link MethodInvoker} fast invoke init methods
   *
   * @param bean
   *         target bean
   * @param beanFactory
   *         target factory
   */
  public final void fastInvokeInitMethods(Object bean, BeanFactory beanFactory) {
    final MethodInvoker[] methodInvokers = this.methodInvokers;
    if (ObjectUtils.isNotEmpty(methodInvokers)) {
      for (final MethodInvoker methodInvoker : methodInvokers) {
        final Object[] args = resolveParameter(methodInvoker.getMethod(), beanFactory);
        methodInvoker.invoke(bean, args);
      }
    }
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

  @Override
  public void copy(BeanDefinition newDef) {
    setName(newDef.getName());
    setChild(newDef.getChild());
    setScope(newDef.getScope());

    setBeanClass(newDef.getBeanClass());
    setFactoryBean(newDef.isFactoryBean());
    setInitMethods(newDef.getInitMethods());
    setDestroyMethods(newDef.getDestroyMethods());
    setPropertyValues(newDef.getPropertySetters());

    setLazyInit(newDef.isLazyInit());
    setInitialized(newDef.isInitialized());

    copyAttributesFrom(newDef);
  }

  @Override
  public <T> void setSupplier(Supplier<T> instanceSupplier) {
    this.instanceSupplier = instanceSupplier;
  }

  protected Class<?> obtainBeanClass() {
    final Class<?> beanClass = getBeanClass();
    Assert.state(beanClass != null, "Bean Class is Null");
    return beanClass;
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
              && lazyInit == other.lazyInit
              && beanClass == other.beanClass
              && Objects.equals(scope, other.scope)
              && Objects.equals(childDef, other.childDef)
              && Objects.deepEquals(initMethods, other.initMethods)
              && Objects.deepEquals(destroyMethods, other.destroyMethods)
              && Objects.deepEquals(propertySetters, other.propertySetters);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, beanClass, lazyInit, scope);
  }

  @Override
  public String toString() {

    return new StringBuilder()//
            .append("{\n\t\"name\":\"").append(name)//
            .append("\",\n\t\"scope\":\"").append(scope)//
            .append("\",\n\t\"beanClass\":\"").append(beanClass)//
            .append("\",\n\t\"initMethods\":\"").append(Arrays.toString(initMethods))//
            .append("\",\n\t\"destroyMethods\":\"").append(Arrays.toString(destroyMethods))//
            .append("\",\n\t\"propertyValues\":\"").append(Arrays.toString(propertySetters))//
            .append("\",\n\t\"initialized\":\"").append(initialized)//
            .append("\",\n\t\"factoryBean\":\"").append(factoryBean)//
            .append("\",\n\t\"child\":\"").append(childDef)//
            .append("\"\n}")//
            .toString();
  }
}
