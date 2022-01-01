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

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Executable;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import cn.taketoday.beans.NoSuchPropertyException;
import cn.taketoday.beans.PropertyValues;
import cn.taketoday.beans.factory.AutowireCandidateQualifier;
import cn.taketoday.beans.factory.BeanDefinitionValidationException;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.beans.factory.PropertyValue;
import cn.taketoday.beans.factory.Scope;
import cn.taketoday.beans.support.BeanInstantiator;
import cn.taketoday.core.AttributeAccessor;
import cn.taketoday.core.AttributeAccessorSupport;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.Prototype;
import cn.taketoday.lang.Singleton;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * @author TODAY 2019-02-01 12:23
 */
public class BeanDefinition
        extends AttributeAccessorSupport implements AttributeAccessor {

  public static final String INIT_METHODS = "initMethods";
  public static final String DESTROY_METHOD = "destroyMethod";
  public static final Method[] EMPTY_METHOD = Constant.EMPTY_METHOD_ARRAY;

  /**
   * Constant that indicates the container should attempt to infer the
   * {@link #setDestroyMethod destroy method name} for a bean as opposed to
   * explicit specification of a method name. The value {@value} is specifically
   * designed to include characters otherwise illegal in a method name, ensuring
   * no possibility of collisions with legitimately named methods having the same
   * name.
   * <p>Currently, the method names detected during destroy method inference
   * are "close" and "shutdown", if present on the specific bean class.
   */
  public static final String INFER_METHOD = "(inferred)";

  /**
   * Scope identifier for the standard singleton scope: {@value}.
   * <p>Note that extended bean factories might support further scopes.
   *
   * @see #setScope
   * @see Scope#SINGLETON
   */
  public static final String SCOPE_SINGLETON = Scope.SINGLETON;

  /**
   * Scope identifier for the standard prototype scope: {@value}.
   * <p>Note that extended bean factories might support further scopes.
   *
   * @see #setScope
   * @see Scope#PROTOTYPE
   */
  public static final String SCOPE_PROTOTYPE = Scope.PROTOTYPE;

  /**
   * Role hint indicating that a {@code BeanDefinition} is a major part
   * of the application. Typically， corresponds to a user-defined bean.
   */
  public static final int ROLE_APPLICATION = 0;

  /**
   * Role hint indicating that a {@code BeanDefinition} is a supporting
   * part of some larger configuration.
   */
  public static final int ROLE_SUPPORT = 1;

  /**
   * Role hint indicating that a {@code BeanDefinition} is providing an
   * entirely background role and has no relevance to the end-user. This hint is
   * used when registering beans that are completely part of the internal workings
   */
  public static final int ROLE_INFRASTRUCTURE = 2;

  /** bean name. */
  private String beanName;

  // @since 4.0
  private String[] aliases;

  /** bean class. */
  private Object beanClass;
  /** bean scope. */
  private String scope = SCOPE_SINGLETON;

  /**
   * Invoke before {@link InitializingBean#afterPropertiesSet}
   *
   * @since 2.3.3
   */
  @Nullable
  private String[] initMethods;

  /**
   * @since 2.3.3
   */
  @Nullable
  private String destroyMethod;

  /**
   * Mark as a {@link FactoryBean}.
   *
   * @since 2.0.0
   */
  @Nullable
  private Boolean factoryBean;

  /** lazy init flag @since 3.0 */
  @Nullable
  private Boolean lazyInit;
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
  private PropertyValues propertyValues;

  @Nullable
  private String factoryBeanName;

  @Nullable
  private String factoryMethodName;

  @Nullable
  private Object[] constructorArgs;

  /** enable DI @since 4.0 */
  private boolean enableDependencyInjection = true;

  /** autowire candidate @since 4.0 */
  private boolean autowireCandidate = true;

  // @since 4.0
  @Nullable
  private String description;

  @Nullable
  private String[] dependsOn;

  @Nullable
  private AnnotatedElement qualifiedElement;

  private Map<String, AutowireCandidateQualifier> qualifiers;

  // cache for fast access
  Executable executable;
  BeanInstantiator instantiator;

  ResolvableType factoryMethodReturnType;

  /** Package-visible field that indicates a before-instantiation post-processor having kicked in. */
  Boolean beforeInstantiationResolved;

  Method[] initMethodArray;

  /** Package-visible field that indicates BeanDefinitionPostProcessor having been applied. */
  boolean postProcessed = false;

  /** Common lock for the two post-processing fields below. */
  final Object postProcessingLock = new Object();

  @Nullable
  private Set<Member> externallyManagedConfigMembers;

  @Nullable
  volatile ResolvableType targetType;

  /** Package-visible field for caching the determined Class of a given bean definition. */
  @Nullable
  volatile Class<?> resolvedTargetType;

  /** Package-visible field for caching a unique factory method candidate for introspection. */
  @Nullable
  volatile Method factoryMethodToIntrospect;

  /** Package-visible field for caching a resolved destroy method name (also for inferred). */
  @Nullable
  volatile String resolvedDestroyMethodName;

  public BeanDefinition() { }

  public BeanDefinition(Class<?> beanClass) {
    setBeanClass(beanClass);
  }

  public BeanDefinition(String beanClassName) {
    this.beanClass = beanClassName;
  }

  public BeanDefinition(String beanName, String beanClassName) {
    this.beanName = beanName;
    this.beanClass = beanClassName;
  }

  public BeanDefinition(String beanName, Class<?> beanClass) {
    setBeanName(beanName);
    setBeanClass(beanClass);
  }

  /**
   * @since 4.0
   */
  public String[] getAliases() {
    return aliases;
  }

  /**
   * @since 4.0
   */
  public void setAliases(String... aliases) {
    this.aliases = aliases;
  }

  /**
   * @since 4.0
   */
  public boolean hasAliases() {
    return ObjectUtils.isNotEmpty(aliases);
  }

  /**
   * Determine whether the given candidate name matches the bean name
   * or the aliases stored in this bean definition.
   */
  public boolean matchesName(@Nullable String candidateName) {
    return (candidateName != null && (candidateName.equals(beanName)
            || candidateName.equals(BeanFactoryUtils.transformedBeanName(beanName))
            || ObjectUtils.containsElement(this.aliases, candidateName)));
  }

  /**
   * Indicates that If the bean is a {@link Singleton}.
   *
   * @return If the bean is a {@link Singleton}.
   */
  public boolean isSingleton() {
    String scope = getScope();
    return StringUtils.isEmpty(scope) || Scope.SINGLETON.equals(scope);
  }

  /**
   * Indicates that If the bean is a
   * {@link Prototype Prototype}.
   *
   * @return If the bean is a {@link Prototype
   * Prototype}.
   * @since 2.17
   */
  public boolean isPrototype() {
    return Scope.PROTOTYPE.equals(scope);
  }

  /**
   * Return the specified class of the bean definition (assuming it is resolved already).
   * <p><b>NOTE:</b> This is an initial class reference as declared in the bean metadata
   * definition, potentially combined with a declared factory method or a
   * {@link FactoryBean} which may lead to a different
   * runtime type of the bean, or not being set at all in case of an instance-level
   * factory method (which is resolved via {@link #getFactoryMethodName()} instead).
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
   * Specify the {@link AnnotatedElement} defining qualifiers,
   * to be used instead of the target class or factory method.
   *
   * @see #setTargetType(ResolvableType)
   * @see #getResolvedFactoryMethod()
   * @since 4.0
   */
  public void setQualifiedElement(@Nullable AnnotatedElement qualifiedElement) {
    this.qualifiedElement = qualifiedElement;
  }

  /**
   * Return the {@link AnnotatedElement} defining qualifiers, if any.
   * Otherwise, the factory method and target class will be checked.
   *
   * @since 4.0
   */
  @Nullable
  public AnnotatedElement getQualifiedElement() {
    return this.qualifiedElement;
  }

  /**
   * Specify a generics-containing target type of this bean definition, if known in advance.
   *
   * @since 4.0
   */
  public void setTargetType(@Nullable ResolvableType targetType) {
    this.targetType = targetType;
  }

  /**
   * Specify the target type of this bean definition, if known in advance.
   *
   * @since 4.0
   */
  public void setTargetType(@Nullable Class<?> targetType) {
    this.targetType = (targetType != null ? ResolvableType.fromClass(targetType) : null);
  }

  /**
   * Return the target type of this bean definition, if known
   * (either specified in advance or resolved on first instantiation).
   *
   * @since 4.0
   */
  @Nullable
  public Class<?> getTargetType() {
    if (this.resolvedTargetType != null) {
      return this.resolvedTargetType;
    }
    ResolvableType targetType = this.targetType;
    return targetType != null ? targetType.resolve() : null;
  }

  /**
   * Return a resolvable type for this bean definition.
   * <p>This implementation delegates to {@link #getBeanClass()}.
   *
   * @since 4.0
   */
  public ResolvableType getResolvableType() {
    ResolvableType targetType = this.targetType;
    if (targetType != null) {
      return targetType;
    }
    ResolvableType returnType = this.factoryMethodReturnType;
    if (returnType != null) {
      return returnType;
    }
    Method factoryMethod = this.factoryMethodToIntrospect;
    if (factoryMethod != null) {
      return ResolvableType.forReturnType(factoryMethod);
    }
    return hasBeanClass() ? ResolvableType.fromClass(getBeanClass()) : ResolvableType.NONE;
  }

  /**
   * Specify the bean class name of this bean definition.
   * <p>The class name can be modified during bean factory post-processing,
   * typically replacing the original class name with a parsed variant of it.
   *
   * @since 4.0
   */
  public void setBeanClassName(@Nullable String beanClassName) {
    this.beanClass = beanClassName;
  }

  /**
   * Return the current bean class name of this bean definition.
   * <p>Note that this does not have to be the actual class name used at runtime, in
   * case of a child definition overriding/inheriting the class name from its parent.
   * Also, this may just be the class that a factory method is called on, or it may
   * even be empty in case of a factory bean reference that a method is called on.
   * Hence, do <i>not</i> consider this to be the definitive bean type at runtime but
   * rather only use it for parsing purposes at the individual bean definition level.
   *
   * @since 4.0
   */
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

  /**
   * Get init methods
   *
   * @return Get all the init methods
   */
  @Nullable
  public String[] getInitMethods() {
    return initMethods;
  }

  /**
   * Get all the destroy methods name
   *
   * @return all the destroy methods name, never be null
   */
  @Nullable
  public String getDestroyMethod() {
    return destroyMethod;
  }

  /**
   * Get Bean {@link Scope}
   *
   * @return Bean {@link Scope}
   */
  @Nullable
  public String getScope() {
    return scope;
  }

  /**
   * Get bean name
   *
   * @return Bean name
   */
  public String getBeanName() {
    return beanName;
  }

  /**
   * If bean is a {@link FactoryBean}
   *
   * @return If Bean is a {@link FactoryBean}
   */
  @Nullable
  public Boolean isFactoryBean() {
    return factoryBean;
  }

  // -----------------------

  /**
   * Indicates that If the bean is a {@link FactoryBean}.
   *
   * @param factoryBean If its a {@link FactoryBean}
   */
  public void setFactoryBean(@Nullable Boolean factoryBean) {
    this.factoryBean = factoryBean;
  }

  /**
   * Apply bean' name
   *
   * @param beanName The bean's name
   */
  public void setBeanName(String beanName) {
    this.beanName = beanName;
  }

  /**
   * Apply bean' scope
   *
   * @param scope The scope of the bean
   * @see Scope#PROTOTYPE
   * @see Scope#SINGLETON
   */
  public void setScope(String scope) {
    this.scope = scope;
  }

  /**
   * Apply bean' initialize {@link Method}s
   *
   * @param initMethods The array of the bean's initialize {@link Method}s
   */
  public void setInitMethods(String... initMethods) {
    this.initMethods = initMethods;
  }

  /**
   * Apply bean' destroy {@link Method}s
   *
   * @param destroyMethod The bean's destroy {@link Method} names
   */
  public void setDestroyMethod(@Nullable String destroyMethod) {
    this.destroyMethod = destroyMethod;
  }

  /**
   * Add PropertyValue to list.
   *
   * @param name supports property-path like 'user.name'
   * @since 3.0
   */
  public BeanDefinition addPropertyValue(String name, Object value) {
    propertyValues().add(name, value);
    return this;
  }

  /** @since 4.0 */
  public BeanDefinition addPropertyValues(PropertyValue... propertyValues) {
    propertyValues().set(propertyValues);
    return this;
  }

  /** @since 4.0 */
  public BeanDefinition addPropertyValues(Map<String, Object> propertyValues) {
    propertyValues().set(propertyValues);
    return this;
  }

  /**
   * Apply bean' {@link PropertyValue}s
   *
   * @param propertyValues The array of the bean's PropertyValue s
   */
  public BeanDefinition setPropertyValues(PropertyValue... propertyValues) {
    propertyValues().set(propertyValues);
    return this;
  }

  public BeanDefinition setPropertyValues(Collection<PropertyValue> propertyValues) {
    propertyValues().set(propertyValues);
    return this;
  }

  public BeanDefinition setPropertyValues(PropertyValues propertyValues) {
    this.propertyValues = propertyValues;
    return this;
  }

  public PropertyValues propertyValues() {
    if (propertyValues == null) {
      propertyValues = new PropertyValues();
    }
    return propertyValues;
  }

  /** @since 4.0 */
  public BeanDefinition setPropertyValues(Map<String, Object> propertyValues) {
    propertyValues().set(propertyValues);
    return this;
  }

  /**
   * Get a property
   *
   * @param name The name of property
   * @return Property value object
   * @throws NoSuchPropertyException If there is no property with given name
   */
  public Object getRequiredPropertyValue(String name) {
    return propertyValues().getRequiredPropertyValue(name);
  }

  /**
   * get simple properties
   *
   * @since 4.0
   */
  @Nullable
  public PropertyValues getPropertyValues() {
    return propertyValues;
  }

  // @since 4.0
  public boolean hasPropertyValues() {
    return propertyValues != null;
  }

  /**
   * Set whether this bean should be lazily initialized.
   * <p>If {@code false}, the bean will get instantiated on startup by bean
   * factories that perform eager initialization of singletons.
   *
   * @since 3.0
   */
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
  public boolean isLazyInit() {
    return this.lazyInit != null && this.lazyInit;
  }

  /**
   * Return whether this bean should be lazily initialized, i.e. not
   * eagerly instantiated on startup. Only applicable to a singleton bean.
   *
   * @return the lazy-init flag if explicitly set, or {@code null} otherwise
   * @since 3.0
   */
  @Nullable
  public Boolean getLazyInit() {
    return this.lazyInit;
  }

  /**
   * @since 3.0
   */
  public void copyFrom(BeanDefinition from) {
    setBeanName(from.getBeanName());
    setScope(from.getScope());

    setBeanClass(from.getBeanClass());
    setFactoryBean(from.isFactoryBean());
    setDestroyMethod(from.getDestroyMethod());
    // copy

    setLazyInit(from.isLazyInit());

    setRole(from.getRole());
    setPrimary(from.isPrimary());
    setSynthetic(from.isSynthetic());
    setDependsOn(from.getDependsOn());

    this.source = from.source;
    this.beanClass = from.beanClass;
    this.initMethods = from.initMethods;
    this.factoryBeanName = from.factoryBeanName;
    this.factoryMethodName = from.factoryMethodName;
    this.instanceSupplier = from.instanceSupplier;
    this.enableDependencyInjection = from.enableDependencyInjection;
    this.factoryMethodToIntrospect = from.factoryMethodToIntrospect;

    this.executable = from.executable;
    this.instantiator = from.instantiator;
    this.initMethodArray = from.initMethodArray;
    this.factoryMethodReturnType = from.factoryMethodReturnType;
    this.resolvedTargetType = from.resolvedTargetType;
    this.targetType = from.targetType;
    this.beforeInstantiationResolved = from.beforeInstantiationResolved;

    if (from.getPropertyValues() != null) {
      propertyValues().add(from.getPropertyValues());
    }

    setBeanClassName(from.getBeanClassName());
    setInitMethods(from.getInitMethods());

    copyAttributesFrom(from);

  }

  /** @since 4.0 */
  public BeanDefinition cloneDefinition() {
    BeanDefinition definition = new BeanDefinition();
    definition.copyFrom(this);
    return definition;
  }

  /**
   * Apply the provided default values to this bean.
   *
   * @param defaults the default settings to apply
   * @since 4.0
   */
  public void applyDefaults(BeanDefinitionDefaults defaults) {
    Boolean lazyInit = defaults.getLazyInit();
    if (lazyInit != null) {
      setLazyInit(lazyInit);
    }
    setInitMethods(defaults.getInitMethodNames());
    setDestroyMethod(defaults.getDestroyMethodName());
  }

  /**
   * Set a bean instance supplier
   *
   * @param instanceSupplier bean instance supplier (can be null)
   * @param <T> target bean type
   * @since 4.0
   */
  public <T> void setInstanceSupplier(Supplier<T> instanceSupplier) {
    this.instanceSupplier = instanceSupplier;
  }

  /** @since 4.0 */
  @Nullable
  public Supplier<?> getInstanceSupplier() {
    return instanceSupplier;
  }

  /**
   * Specify the factory bean to use, if any.
   * This the name of the bean to call the specified factory method on.
   *
   * @see #setFactoryMethodName
   * @since 4.0
   */
  public void setFactoryBeanName(@Nullable String factoryBeanName) {
    this.factoryBeanName = factoryBeanName;
  }

  /**
   * Return the factory bean name, if any.
   *
   * @since 4.0
   */
  @Nullable
  public String getFactoryBeanName() {
    return this.factoryBeanName;
  }

  /**
   * Specify a factory method, if any. This method will be invoked with
   * constructor arguments, or with no arguments if none are specified.
   * The method will be invoked on the specified factory bean, if any,
   * or otherwise as a static method on the local bean class.
   *
   * @see #setFactoryBeanName
   * @see #setBeanClassName
   * @since 4.0
   */
  public void setFactoryMethodName(@Nullable String factoryMethodName) {
    this.factoryMethodName = factoryMethodName;
  }

  /**
   * Return a factory method, if any.
   *
   * @since 4.0
   */
  @Nullable
  public String getFactoryMethodName() {
    return this.factoryMethodName;
  }

  /** @since 4.0 */
  public void setConstructorArgs(@Nullable Object... constructorArgs) {
    this.constructorArgs = constructorArgs;
  }

  /** @since 4.0 */
  @Nullable
  public Object[] getConstructorArgs() {
    return constructorArgs;
  }

  public boolean isFactoryMethod(Method method) {
    return method.getName().equals(factoryMethodName);
  }

  /**
   * Set a resolved Java Method for the factory method on this bean definition.
   *
   * @param method the resolved factory method, or {@code null} to reset it
   * @since 4.0
   */
  public void setResolvedFactoryMethod(@Nullable Method method) {
    this.factoryMethodToIntrospect = method;
  }

  /**
   * Return the resolved factory method as a Java Method object, if available.
   *
   * @return the factory method, or {@code null} if not found or not resolved yet
   * @since 4.0
   */
  @Nullable
  public Method getResolvedFactoryMethod() {
    return this.factoryMethodToIntrospect;
  }

  /**
   * Set whether this bean definition is 'synthetic', that is, not defined
   * by the application itself (for example, an infrastructure bean such
   * as a helper for auto-proxying, created through {@code <aop:config>}).
   *
   * @since 4.0
   */
  public void setSynthetic(boolean synthetic) {
    this.synthetic = synthetic;
  }

  /**
   * Return whether this bean definition is 'synthetic', that is,
   * not defined by the application itself.
   * <p>
   * if synthetic==true don't process autowire
   * </p>
   *
   * @since 4.0
   */
  public boolean isSynthetic() {
    return this.synthetic;
  }

  /**
   * Set the role hint for this {@code BeanDefinition}. The role hint
   * provides the frameworks as well as tools with an indication of
   * the role and importance of a particular {@code BeanDefinition}.
   *
   * @see #ROLE_APPLICATION
   * @see #ROLE_INFRASTRUCTURE
   * @since 4.0
   */
  public void setRole(int role) {
    this.role = role;
  }

  /**
   * Get the role hint for this {@code BeanDefinition}. The role hint
   * provides the frameworks as well as tools with an indication of
   * the role and importance of a particular {@code BeanDefinition}.
   *
   * @see #ROLE_APPLICATION
   * @see #ROLE_INFRASTRUCTURE
   * @since 4.0
   */
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
  public void setPrimary(boolean primary) {
    this.primary = primary;
  }

  /**
   * Return whether this bean is a primary autowire candidate.
   *
   * @since 4.0
   */
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

  /**
   * Set the names of the beans that this bean depends on being initialized.
   * The bean factory will guarantee that these beans get initialized first.
   * <p>Note that dependencies are normally expressed through bean properties or
   * constructor arguments. This property should just be necessary for other kinds
   * of dependencies like statics (*ugh*) or database preparation on startup.
   *
   * @since 4.0
   */
  public void setDependsOn(@Nullable String... dependsOn) {
    this.dependsOn = dependsOn;
  }

  /**
   * Return the bean names that this bean depends on.
   *
   * @since 4.0
   */
  @Nullable
  public String[] getDependsOn() {
    return this.dependsOn;
  }

  /** @since 4.0 */
  public void setEnableDependencyInjection(boolean enableDependencyInjection) {
    this.enableDependencyInjection = enableDependencyInjection;
  }

  /** @since 4.0 */
  public boolean isEnableDependencyInjection() {
    return enableDependencyInjection;
  }

  /**
   * Set whether this bean is a candidate for getting autowired into some other bean.
   * <p>Note that this flag is designed to only affect type-based autowiring.
   * It does not affect explicit references by name, which will get resolved even
   * if the specified bean is not marked as an autowire candidate. As a consequence,
   * autowiring by name will nevertheless inject a bean if the name matches.
   */
  public void setAutowireCandidate(boolean autowireCandidate) {
    this.autowireCandidate = autowireCandidate;
  }

  /**
   * Return whether this bean is a candidate for getting autowired into some other bean.
   */
  public boolean isAutowireCandidate() {
    return this.autowireCandidate;
  }

  /**
   * Set a human-readable description of this bean definition.
   *
   * @since 4.0
   */
  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  /**
   * Return a human-readable description of this bean definition.
   *
   * @since 4.0
   */
  @Nullable
  public String getDescription() {
    return this.description;
  }

  /**
   * Validate bean definition
   *
   * @throws BeanDefinitionValidationException invalid {@link BeanDefinition}
   * @since 4.0
   */
  public void validate() throws BeanDefinitionValidationException {
    if (StringUtils.isEmpty(getBeanName())) {
      throw new BeanDefinitionValidationException("Definition's bean name can't be null");
    }
  }

  //---------------------------------------------------------------------
  // Qualifier
  //---------------------------------------------------------------------

  /**
   * Register a qualifier to be used for autowire candidate resolution,
   * keyed by the qualifier's type name.
   *
   * @see AutowireCandidateQualifier#getTypeName()
   */
  public void addQualifier(AutowireCandidateQualifier qualifier) {
    if (qualifiers == null) {
      this.qualifiers = new LinkedHashMap<>();
    }
    qualifiers.put(qualifier.getTypeName(), qualifier);
  }

  /**
   * Return whether this bean has the specified qualifier.
   */
  public boolean hasQualifier(String typeName) {
    if (qualifiers == null) {
      return false;
    }
    return this.qualifiers.containsKey(typeName);
  }

  /**
   * Return the qualifier mapped to the provided type name.
   */
  @Nullable
  public AutowireCandidateQualifier getQualifier(String typeName) {
    if (qualifiers == null) {
      return null;
    }
    return this.qualifiers.get(typeName);
  }

  /**
   * Return all registered qualifiers.
   *
   * @return the Set of {@link AutowireCandidateQualifier} objects.
   */
  public Set<AutowireCandidateQualifier> getQualifiers() {
    if (CollectionUtils.isNotEmpty(qualifiers)) {
      return new LinkedHashSet<>(qualifiers.values());
    }
    return Collections.emptySet();
  }

  /**
   * Copy the qualifiers from the supplied AbstractBeanDefinition to this bean definition.
   *
   * @param source the AbstractBeanDefinition to copy from
   */
  public void copyQualifiersFrom(BeanDefinition source) {
    Assert.notNull(source, "Source must not be null");
    if (source.qualifiers != null) {
      if (qualifiers == null) {
        this.qualifiers = new LinkedHashMap<>();
      }
      qualifiers.putAll(source.qualifiers);
    }
  }

  // postProcessingLock

  /**
   * Register an externally managed configuration method or field.
   *
   * @since 4.0
   */
  public void registerExternallyManagedConfigMember(Member configMember) {
    synchronized(this.postProcessingLock) {
      if (this.externallyManagedConfigMembers == null) {
        this.externallyManagedConfigMembers = new LinkedHashSet<>(1);
      }
      this.externallyManagedConfigMembers.add(configMember);
    }
  }

  /**
   * Check whether the given method or field is an externally managed configuration member.
   *
   * @since 4.0
   */
  public boolean isExternallyManagedConfigMember(Member configMember) {
    synchronized(this.postProcessingLock) {
      return (this.externallyManagedConfigMembers != null &&
              this.externallyManagedConfigMembers.contains(configMember));
    }
  }

  /**
   * Return all externally managed configuration methods and fields (as an immutable Set).
   *
   * @since 4.0
   */
  public Set<Member> getExternallyManagedConfigMembers() {
    synchronized(this.postProcessingLock) {
      return (this.externallyManagedConfigMembers != null ?
              Collections.unmodifiableSet(new LinkedHashSet<>(this.externallyManagedConfigMembers)) :
              Collections.emptySet());
    }
  }

  // Object

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof BeanDefinition other) {
      return Objects.equals(beanName, other.beanName)
              && role == other.role
              && lazyInit == other.lazyInit
              && beanClass == other.beanClass
              && synthetic == other.synthetic
              && enableDependencyInjection == other.enableDependencyInjection
              && instanceSupplier == other.instanceSupplier
              && Objects.equals(scope, other.scope)
              && Objects.equals(factoryBeanName, other.factoryBeanName)
              && Objects.equals(factoryMethodName, other.factoryMethodName)
              && Objects.deepEquals(initMethods, other.initMethods)
              && Objects.deepEquals(destroyMethod, other.destroyMethod)
              && Objects.equals(propertyValues, other.propertyValues);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
            beanName, beanClass, lazyInit, scope, synthetic, role, primary,
            enableDependencyInjection, factoryMethodName, factoryBeanName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("class [");
    sb.append(getBeanClassName()).append(']');
    sb.append("; scope=").append(this.scope);
    sb.append("; lazyInit=").append(this.lazyInit);
    sb.append("; primary=").append(this.primary);
    sb.append("; factoryBean=").append(this.factoryBean);
    sb.append("; initMethods=").append(Arrays.toString(initMethods));
    sb.append("; factoryBeanName=").append(this.factoryBeanName);
    sb.append("; factoryMethodName=").append(this.factoryMethodName);
    sb.append("; destroyMethod=").append(destroyMethod);

    if (this.source != null) {
      sb.append("; defined in ").append(this.source);
    }
    return sb.toString();
  }

}
