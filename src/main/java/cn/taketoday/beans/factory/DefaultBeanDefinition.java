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
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import cn.taketoday.beans.ArgumentsResolver;
import cn.taketoday.beans.FactoryBean;
import cn.taketoday.beans.InitializingBean;
import cn.taketoday.beans.NoSuchPropertyException;
import cn.taketoday.beans.support.BeanInstantiator;
import cn.taketoday.beans.support.BeanUtils;
import cn.taketoday.core.AttributeAccessorSupport;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.core.annotation.OrderUtils;
import cn.taketoday.core.reflect.MethodInvoker;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
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
  private Object beanClass;
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

  /** source @since 4.0 source */
  @Nullable
  private Object source;

  @Nullable
  private LinkedHashSet<PropertyValue> propertyValues;

  public DefaultBeanDefinition() { }

  public DefaultBeanDefinition(Class<?> beanClass) {
    setBeanClass(beanClass);
  }

  public DefaultBeanDefinition(String name, String beanClassName) {
    this.name = name;
    this.beanClass = beanClassName;
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
  public PropertyValue getPropertyValue(String name) {
    if (propertyValues != null) {
      for (PropertyValue propertySetter : propertyValues) {
        if (propertySetter.getName().equals(name)) {
          return propertySetter;
        }
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

  /**
   * Return the specified class of the bean definition (assuming it is resolved already).
   * <p><b>NOTE:</b> This is an initial class reference as declared in the bean metadata
   * definition, potentially combined with a declared factory method or a
   * {@link cn.taketoday.beans.FactoryBean} which may lead to a different
   * runtime type of the bean, or not being set at all in case of an instance-level
   * factory method (which is resolved via {@link FactoryMethodBeanDefinition} instead).
   * <b>Do not use this for runtime type introspection of arbitrary bean definitions.</b>
   * The recommended way to find out about the actual runtime type of a particular bean
   * is a {@link cn.taketoday.beans.factory.BeanFactory#getType} call for the
   * specified bean name; this takes all of the above cases into account and returns the
   * type of object that a {@link cn.taketoday.beans.factory.BeanFactory#getBean}
   * call is going to return for the same bean name.
   *
   * @return the resolved bean class (never {@code null})
   * @throws IllegalStateException if the bean definition does not define a bean class,
   * or a specified bean class name has not been resolved into an actual Class yet
   * @see #getBeanClassName()
   * @see #hasBeanClass()
   * @see #setBeanClass(Class)
   * @see #resolveBeanClass(ClassLoader)
   */
  @Override
  public Class<?> getBeanClass() throws IllegalStateException {
    Object beanClassObject = this.beanClass;
    if (beanClassObject == null) {
      throw new IllegalStateException("No bean class specified on bean definition");
    }
    if (!(beanClassObject instanceof Class)) {
      throw new IllegalStateException(
              "Bean class name [" + beanClassObject + "] has not been resolved into an actual Class");
    }
    return (Class<?>) beanClassObject;
  }

  /**
   * Specify the bean class name of this bean definition.
   */
  @Override
  public void setBeanClassName(@Nullable String beanClassName) {
    this.beanClass = beanClassName;
  }

  /**
   * Return the current bean class name of this bean definition.
   */
  @Override
  @Nullable
  public String getBeanClassName() {
    Object beanClassObject = this.beanClass;
    if (beanClassObject instanceof Class) {
      return ((Class<?>) beanClassObject).getName();
    }
    else {
      return (String) beanClassObject;
    }
  }

  /**
   * Return whether this definition specifies a bean class.
   *
   * @see #getBeanClass()
   * @see #setBeanClass(Class)
   * @see #resolveBeanClass(ClassLoader)
   * @since 4.0
   */
  public boolean hasBeanClass() {
    return beanClass instanceof Class;
  }

  /**
   * Determine the class of the wrapped bean, resolving it from a
   * specified class name if necessary. Will also reload a specified
   * Class from its name when called with the bean class already resolved.
   *
   * @param classLoader the ClassLoader to use for resolving a (potential) class name
   * @return the resolved bean class
   * @throws ClassNotFoundException if the class name could be resolved
   */
  @Nullable
  public Class<?> resolveBeanClass(@Nullable ClassLoader classLoader) throws ClassNotFoundException {
    String className = getBeanClassName();
    if (className == null) {
      return null;
    }
    Class<?> resolvedClass = ClassUtils.forName(className, classLoader);
    this.beanClass = resolvedClass;
    return resolvedClass;
  }

  /**
   * Return a description of the resource that this bean definition
   * came from (for the purpose of showing context in case of errors).
   */
  @Nullable
  public String getResourceDescription() {
    return this.source != null ? this.source.toString() : null;
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

  @Nullable
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

  // -----------------------

  @Override
  public void setInitialized(boolean initialized) {
    this.initialized = initialized;
  }

  @Override
  public void setFactoryBean(boolean factoryBean) {
    this.factoryBean = factoryBean;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public void setScope(String scope) {
    this.scope = scope;
  }

  @Override
  public void setInitMethods(Method... initMethods) {
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
  }

  @Override
  public void setDestroyMethods(String... destroyMethods) {
    this.destroyMethods = destroyMethods;
  }

  @Override
  public void addPropertyValue(String name, Object value) {
    Assert.notNull(name, "property name must not be null");
    addPropertyValues(new PropertyValue(name, value));
  }

  @Override
  public void addPropertyValues(PropertyValue... propertyValue) {
    if (propertyValues == null) {
      propertyValues = new LinkedHashSet<>();
    }
    CollectionUtils.addAll(propertyValues, propertyValue);
  }

  @Override
  public void setPropertyValues(PropertyValue... propertyValues) {
    if (this.propertyValues == null) {
      if (ObjectUtils.isNotEmpty(propertyValues)) {
        this.propertyValues = new LinkedHashSet<>();
        CollectionUtils.addAll(this.propertyValues, propertyValues);
      }
    }
    else {
      this.propertyValues.clear();
      CollectionUtils.addAll(this.propertyValues, propertyValues);
    }
  }

  @Override
  public void setPropertyValues(Collection<PropertyValue> propertyValues) {
    if (this.propertyValues == null) {
      if (CollectionUtils.isNotEmpty(propertyValues)) {
        this.propertyValues = new LinkedHashSet<>();
        CollectionUtils.addAll(this.propertyValues, propertyValues);
      }
    }
    else {
      this.propertyValues.clear();
      CollectionUtils.addAll(this.propertyValues, propertyValues);
    }
  }

  @Override
  public Set<PropertyValue> getPropertyValues() {
    return propertyValues;
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
    return AnnotatedElementUtils.isAnnotated(getBeanClass(), annotation);
  }

  @Override
  public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
    return AnnotationUtils.getAnnotation(getBeanClass(), annotationClass);
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
    setPropertyValues(newDef.getPropertyValues());

    setLazyInit(newDef.isLazyInit());
    setInitialized(newDef.isInitialized());

    setRole(newDef.getRole());
    setSynthetic(newDef.isSynthetic());
    setPrimary(newDef.isPrimary());

    if (newDef instanceof DefaultBeanDefinition) {
      DefaultBeanDefinition defaultBeanDefinition = (DefaultBeanDefinition) newDef;

      this.source = defaultBeanDefinition.source;
      this.beanClass = defaultBeanDefinition.beanClass;
      this.executable = defaultBeanDefinition.executable;
      this.initMethods = defaultBeanDefinition.initMethods;
      this.constructor = defaultBeanDefinition.constructor;
      this.methodInvokers = defaultBeanDefinition.methodInvokers;
      this.instanceSupplier = defaultBeanDefinition.instanceSupplier;
    }
    else {
      setBeanClassName(newDef.getBeanClassName());
      setInitMethods(newDef.getInitMethods());
    }

    copyAttributesFrom(newDef);
  }

  @Override
  public <T> void setSupplier(Supplier<T> instanceSupplier) {
    this.instanceSupplier = instanceSupplier;
  }

  protected Class<?> obtainBeanClass() {
    return getBeanClass();
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
  public void setSource(@Nullable Object source) {
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
              && Objects.equals(propertyValues, other.propertyValues);
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
    sb.append(getBeanClassName()).append(']');
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
