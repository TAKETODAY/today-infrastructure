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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.beans.factory;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import cn.taketoday.beans.ArgumentsResolver;
import cn.taketoday.beans.FactoryBean;
import cn.taketoday.beans.InitializingBean;
import cn.taketoday.beans.NoSuchPropertyException;
import cn.taketoday.beans.support.BeanInstantiator;
import cn.taketoday.beans.support.BeanProperty;
import cn.taketoday.beans.support.BeanUtils;
import cn.taketoday.core.AttributeAccessorSupport;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.core.annotation.OrderUtils;
import cn.taketoday.core.reflect.MethodInvoker;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ExceptionUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

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
  private BeanInstantiator constructor;
  /** lazy init flag @since 3.0 */
  private Boolean lazyInit;
  /** @since 3.0 fast invoke init methods */
  private MethodInvoker[] methodInvokers;
  /** @since 3.0 bean instance supplier */
  private Supplier<?> instanceSupplier;

  /** @since 4.0 */
  private boolean synthetic = false;

  /** @since 4.0 */
  private int role = ROLE_APPLICATION;

  /** @since 4.0 */
  private boolean primary = false;

  /** @since 4.0  class name lazy load */
  private String className;

  @Nullable
  /** source @since 4.0 source */
  private Object source;

  public DefaultBeanDefinition() { }

  public DefaultBeanDefinition(Class<?> beanClass) {
    setBeanClass(beanClass);
  }

  public DefaultBeanDefinition(String name, String className) {
    this.name = name;
    this.className = className;
  }

  public DefaultBeanDefinition(String name, Class<?> beanClass) {
    setName(name);
    setBeanClass(beanClass);
  }

  /**
   * Build a {@link BeanDefinition} with given child {@link BeanDefinition}
   *
   * @param beanName Bean name
   * @param childDef Child {@link BeanDefinition}
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
    String scope = getScope();
    return StringUtils.isEmpty(scope) || Scope.SINGLETON.equals(scope);
  }

  @Override
  public boolean isPrototype() {
    return Scope.PROTOTYPE.equals(scope);
  }

  @Override
  public Class<?> getBeanClass() {
    if (beanClass == null) {
      if (className != null) {
        try {
          beanClass = ClassUtils.forName(className);
        }
        catch (ClassNotFoundException e) {
          throw ExceptionUtils.sneakyThrow(e);
        }
      }
    }
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
      AccessibleObject.setAccessible(initMethods, true);
      this.methodInvokers = new MethodInvoker[initMethods.length];
      int i = 0;
      for (Method initMethod : initMethods) {
        methodInvokers[i++] = MethodInvoker.fromMethod(initMethod);
      }
    }
    else {
      this.initMethods = EMPTY_METHOD;
      this.methodInvokers = null;
    }
    return this;
  }

  @Override
  public BeanDefinition setDestroyMethods(String... destroyMethods) {
    this.destroyMethods = destroyMethods;
    return this;
  }

  /**
   * @param propertySetters collection of PropertySetter
   * @since 4.0
   */
  public void setPropertyValues(@Nullable Collection<PropertySetter> propertySetters) {
    if (CollectionUtils.isNotEmpty(propertySetters)) {
      this.propertySetters = propertySetters.toArray(EMPTY_PROPERTY_SETTER);
    }
    else {
      this.propertySetters = EMPTY_PROPERTY_SETTER;
    }
  }

  public BeanDefinition setPropertyValues(PropertySetter... propertySetters) {
    this.propertySetters = propertySetters;
    return this;
  }

  @Override
  public void addPropertyValue(String name, Object value) {
    Assert.notNull(name, "property name must not be null");

    BeanProperty beanProperty = BeanProperty.valueOf(obtainBeanClass(), name);
    DefaultPropertySetter propertyValue = new DefaultPropertySetter(value, beanProperty);
    addPropertySetter(propertyValue);
  }

  @Override
  public void addPropertySetter(PropertySetter... setters) {
    if (ObjectUtils.isNotEmpty(setters)) {
      PropertySetter[] propertySetters = getPropertySetters();
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
    PropertySetter[] propertySetters = getPropertySetters();
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
    return OrderUtils.getOrderOrLowest(getBeanClass());
  }

  @Override
  public BeanDefinition getChild() {
    return childDef;
  }

  /**
   * Apply the child bean name
   *
   * @param childDef Child BeanDefinition
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

  public BeanInstantiator getConstructor(BeanFactory factory) {
    if (constructor == null) {
      this.constructor = createConstructor(factory);
    }
    return constructor;
  }

  protected BeanInstantiator createConstructor(BeanFactory factory) {
    return BeanInstantiator.fromClass(getBeanClass());
  }

  public Executable getExecutable() {
    if (executable == null) {
      this.executable = BeanUtils.getConstructor(getBeanClass());
    }
    return executable;
  }

  /** @since 3.0 */
  @Override
  public Object newInstance(BeanFactory factory) {
    if (instanceSupplier != null) {
      return instanceSupplier.get();
    }
    BeanInstantiator target = getConstructor(factory);
    Object[] args = factory.getArgumentsResolver().resolve(getExecutable(), factory);
    return target.instantiate(args);
  }

  /**
   * @param factory input bean factory
   * @param args arguments to use when creating a corresponding instance
   * @since 3.0
   */
  @Override
  public Object newInstance(BeanFactory factory, Object... args) {
    BeanInstantiator target = getConstructor(factory);
    return target.instantiate(args);
  }

  /**
   * use {@link MethodInvoker} fast invoke init methods
   *
   * @param bean target bean
   * @param beanFactory target factory
   */
  public final void fastInvokeInitMethods(Object bean, BeanFactory beanFactory) {
    if (ObjectUtils.isNotEmpty(methodInvokers)) {
      ArgumentsResolver resolver = beanFactory.getArgumentsResolver();
      for (MethodInvoker methodInvoker : methodInvokers) {
        Object[] args = resolver.resolve(methodInvoker.getMethod(), beanFactory);
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
    setDestroyMethods(newDef.getDestroyMethods());
    setPropertyValues(newDef.getPropertySetters());

    setLazyInit(newDef.isLazyInit());
    setInitialized(newDef.isInitialized());

    setRole(newDef.getRole());
    setSynthetic(newDef.isSynthetic());
    setPrimary(newDef.isPrimary());

    if (newDef instanceof DefaultBeanDefinition) {
      DefaultBeanDefinition defaultBeanDefinition = (DefaultBeanDefinition) newDef;
      setSupplier(defaultBeanDefinition.instanceSupplier);
      this.executable = defaultBeanDefinition.executable;
      this.initMethods = defaultBeanDefinition.initMethods;
      this.constructor = defaultBeanDefinition.constructor;
      this.methodInvokers = defaultBeanDefinition.methodInvokers;
    }
    else {
      setInitMethods(newDef.getInitMethods());
    }

    copyAttributesFrom(newDef);
  }

  @Override
  public <T> void setSupplier(Supplier<T> instanceSupplier) {
    this.instanceSupplier = instanceSupplier;
  }

  protected Class<?> obtainBeanClass() {
    Assert.state(beanClass != null, "Bean Class is Null");
    return beanClass;
  }

  /**
   * Set whether this bean definition is 'synthetic', that is, not defined
   * by the application itself (for example, an infrastructure bean such
   * as a helper for auto-proxying, created through {@code <aop:config>}).
   *
   * @since 4.0
   */
  @Override
  public void setSynthetic(boolean synthetic) {
    this.synthetic = synthetic;
  }

  /**
   * Return whether this bean definition is 'synthetic', that is,
   * not defined by the application itself.
   *
   * @since 4.0
   */
  @Override
  public boolean isSynthetic() {
    return this.synthetic;
  }

  /**
   * Set the role hint for this {@code BeanDefinition}.
   *
   * @since 4.0
   */
  @Override
  public void setRole(int role) {
    this.role = role;
  }

  /**
   * Return the role hint for this {@code BeanDefinition}.
   *
   * @since 4.0
   */
  @Override
  public int getRole() {
    return this.role;
  }

  /**
   * Set whether this bean is a primary autowire candidate.
   * <p>If this value is {@code true} for exactly one bean among multiple
   * matching candidates, it will serve as a tie-breaker.
   *
   * @since 4.0
   */
  @Override
  public void setPrimary(boolean primary) {
    this.primary = primary;
  }

  /**
   * Return whether this bean is a primary autowire candidate.
   *
   * @since 4.0
   */
  @Override
  public boolean isPrimary() {
    return this.primary;
  }

  /** @since 4.0 source */
  public void setSource(@Nullable Object source){
    this.source = source;
  }

  /** @since 4.0 source */
  @Nullable
  public Object getSource() {
    return this.source;
  }

  @Override
  public boolean isAssignableTo(ResolvableType typeToMatch) {
    BeanDefinition child = getChild();
    if (child != null) {
      Class<?> implementationClass = child.getBeanClass();
      return ResolvableType.fromClass(getBeanClass(), implementationClass)
              .isAssignableFrom(typeToMatch);
    }
    return ResolvableType.fromClass(getBeanClass())
            .isAssignableFrom(typeToMatch);
  }

  @Override
  public boolean isAssignableTo(Class<?> typeToMatch) {
    BeanDefinition child = getChild();
    if (child != null) {
      Class<?> implementationClass = child.getBeanClass();
      return typeToMatch.isAssignableFrom(implementationClass);
    }
    return typeToMatch.isAssignableFrom(getBeanClass());
  }

  // Object

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof DefaultBeanDefinition) {
      DefaultBeanDefinition other = (DefaultBeanDefinition) obj;
      return Objects.equals(name, other.name)
              && role == other.role
              && lazyInit == other.lazyInit
              && beanClass == other.beanClass
              && synthetic == other.synthetic
              && instanceSupplier == other.instanceSupplier
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
    return Objects.hash(name, beanClass, lazyInit, scope, synthetic, role);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("class [");
    sb.append(beanClass.getName()).append(']');
    sb.append("; scope=").append(this.scope);
    sb.append("; abstract=").append(isAbstract());
    sb.append("; lazyInit=").append(this.lazyInit);
    sb.append("; primary=").append(this.primary);
    sb.append("; initialized=").append(this.initialized);
    sb.append("; factoryBean=").append(this.factoryBean);
    sb.append("; initMethods=").append(Arrays.toString(initMethods));
    sb.append("; destroyMethods=").append(Arrays.toString(destroyMethods));
    sb.append("; child=").append(this.childDef);
    return sb.toString();
  }

}
