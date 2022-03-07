/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

import cn.taketoday.beans.BeanMetadataAttributeAccessor;
import cn.taketoday.beans.BeanMetadataElement;
import cn.taketoday.beans.BeanUtils;
import cn.taketoday.beans.NoSuchPropertyException;
import cn.taketoday.beans.PropertyValue;
import cn.taketoday.beans.PropertyValues;
import cn.taketoday.beans.factory.AutowireCapableBeanFactory;
import cn.taketoday.beans.factory.BeanDefinitionValidationException;
import cn.taketoday.beans.factory.BeanFactoryPostProcessor;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.beans.factory.Scope;
import cn.taketoday.core.AttributeAccessor;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.io.DescriptiveResource;
import cn.taketoday.core.io.Resource;
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
 * A BeanDefinition describes a bean instance, which has property values,
 * constructor argument values, and further information supplied by
 * concrete implementations.
 *
 * <p>This is just a minimal interface: The main intention is to allow a
 * {@link BeanFactoryPostProcessor} to introspect and modify property values
 * and other bean metadata.
 *
 * @author TODAY 2019-02-01 12:23
 */
public class BeanDefinition
        extends BeanMetadataAttributeAccessor implements AttributeAccessor, BeanMetadataElement {

  // @since 4.0
  public static final Method[] EMPTY_METHOD = Constant.EMPTY_METHOD_ARRAY;

  public static final String INIT_METHODS = "initMethods";
  public static final String DESTROY_METHOD = "destroyMethod";

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

  /**
   * Constant that indicates no external autowiring at all.
   *
   * @see #setAutowireMode
   * @since 4.0
   */
  public static final int AUTOWIRE_NO = AutowireCapableBeanFactory.AUTOWIRE_NO;

  /**
   * Constant that indicates autowiring bean properties by name.
   *
   * @see #setAutowireMode
   * @since 4.0
   */
  public static final int AUTOWIRE_BY_NAME = AutowireCapableBeanFactory.AUTOWIRE_BY_NAME;

  /**
   * Constant that indicates autowiring bean properties by type.
   *
   * @see #setAutowireMode
   * @since 4.0
   */
  public static final int AUTOWIRE_BY_TYPE = AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE;

  /**
   * Constant that indicates autowiring a constructor.
   *
   * @see #setAutowireMode
   * @since 4.0
   */
  public static final int AUTOWIRE_CONSTRUCTOR = AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR;

  /**
   * Constant that indicates no dependency check at all.
   *
   * @see #setDependencyCheck
   * @since 4.0
   */
  public static final int DEPENDENCY_CHECK_NONE = 0;

  /**
   * Constant that indicates dependency checking for object references.
   *
   * @see #setDependencyCheck
   * @since 4.0
   */
  public static final int DEPENDENCY_CHECK_OBJECTS = 1;

  /**
   * Constant that indicates dependency checking for "simple" properties.
   *
   * @see #setDependencyCheck
   * @see BeanUtils#isSimpleProperty
   * @since 4.0
   */
  public static final int DEPENDENCY_CHECK_SIMPLE = 2;

  /**
   * Constant that indicates dependency checking for all properties
   * (object references as well as "simple" properties).
   *
   * @see #setDependencyCheck
   * @since 4.0
   */
  public static final int DEPENDENCY_CHECK_ALL = 3;
  /** bean name. */
  private String beanName;

  // @since 4.0
  private String[] aliases;

  /** bean class. */
  private Object beanClass;
  /** bean scope. */

  @Nullable
  private String scope;

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

  @Nullable
  private PropertyValues propertyValues;

  @Nullable
  private String factoryBeanName;

  @Nullable
  private String factoryMethodName;

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

  // @since 4.0
  private int autowireMode = AUTOWIRE_NO;

  // @since 4.0
  private int dependencyCheck = DEPENDENCY_CHECK_NONE;

  // @since 4.0
  @Nullable
  private String parentName;

  // @since 4.0
  @Nullable
  private ConstructorArgumentValues constructorArgumentValues;

  private boolean nonPublicAccessAllowed = true;

  private boolean lenientConstructorResolution = true;

  // cache for fast access
  Executable executable;

  ResolvableType factoryMethodReturnType;

  /** Package-visible field that indicates a before-instantiation post-processor having kicked in. */
  Boolean beforeInstantiationResolved;

  Method[] initMethodArray;

  /** Package-visible field that indicates BeanDefinitionPostProcessor having been applied. */
  boolean postProcessed = false;

  boolean isFactoryMethodUnique;

  /** Common lock for the two post-processing fields below. */
  final Object postProcessingLock = new Object();

  /** Common lock for the four constructor fields below. */
  final Object constructorArgumentLock = new Object();

  @Nullable
  private Set<Member> externallyManagedConfigMembers;

  @Nullable
  private Set<String> externallyManagedInitMethods;

  @Nullable
  private Set<String> externallyManagedDestroyMethods;

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

  /** Package-visible field that marks the constructor arguments as resolved. */
  boolean constructorArgumentsResolved = false;

  /** Package-visible field for caching fully resolved constructor arguments. */
  @Nullable
  Object[] resolvedConstructorArguments;

  /** Package-visible field for caching partly prepared constructor arguments. */
  @Nullable
  Object[] preparedConstructorArguments;

  // @since 4.0
  private boolean enforceInitMethod;
  // @since 4.0
  private boolean enforceDestroyMethod;

  @Nullable
  private Resource resource;

  /**
   * @since 3.0
   */
  public void copyFrom(BeanDefinition from) {
    setBeanName(from.getBeanName());
    setScope(from.getScope());

    setFactoryBean(from.isFactoryBean());
    setDestroyMethod(from.getDestroyMethod());
    // copy

    this.role = from.role;
    this.resource = from.resource;
    this.primary = from.primary;
    this.lazyInit = from.lazyInit;
    this.synthetic = from.synthetic;
    this.beanClass = from.beanClass;
    this.dependsOn = from.dependsOn;
    this.initMethods = from.initMethods;
    this.description = from.description;
    this.autowireMode = from.autowireMode;
    this.dependencyCheck = from.dependencyCheck;
    this.postProcessed = from.postProcessed;
    this.factoryBeanName = from.factoryBeanName;
    this.enforceInitMethod = from.enforceInitMethod;
    this.enforceDestroyMethod = from.enforceDestroyMethod;
    this.autowireCandidate = from.autowireCandidate;
    this.factoryMethodName = from.factoryMethodName;
    this.instanceSupplier = from.instanceSupplier;
    this.qualifiedElement = from.qualifiedElement;
    this.enableDependencyInjection = from.enableDependencyInjection;
    this.factoryMethodToIntrospect = from.factoryMethodToIntrospect;
    this.nonPublicAccessAllowed = from.nonPublicAccessAllowed;
    this.lenientConstructorResolution = from.lenientConstructorResolution;

    this.executable = from.executable;
    this.targetType = from.targetType;
    this.initMethodArray = from.initMethodArray;
    this.resolvedTargetType = from.resolvedTargetType;
    this.isFactoryMethodUnique = from.isFactoryMethodUnique;
    this.factoryMethodReturnType = from.factoryMethodReturnType;
    this.beforeInstantiationResolved = from.beforeInstantiationResolved;

    if (from.hasConstructorArgumentValues()) {
      setConstructorArgumentValues(new ConstructorArgumentValues(from.getConstructorArgumentValues()));
    }

    if (from.getPropertyValues() != null) {
      propertyValues().add(from.getPropertyValues());
    }

    copyQualifiersFrom(from);
    copyAttributesFrom(from);

    setInitMethods(from.getInitMethods());
    setBeanClassName(from.getBeanClassName());
  }

  /** @since 4.0 */
  public BeanDefinition cloneDefinition() {
    return new BeanDefinition(this);
  }

  public BeanDefinition() { }

  public BeanDefinition(Class<?> beanClass) {
    setBeanClass(beanClass);
  }

  public BeanDefinition(Class<?> beanClass, int autowireMode) {
    setBeanClass(beanClass);
    setAutowireMode(autowireMode);
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
   * Create a new BeanDefinition for a singleton,
   * providing constructor arguments and property values.
   *
   * @param beanClass the class of the bean to instantiate
   * @param cargs the constructor argument values to apply
   * @param pvs the property values to apply
   */
  public BeanDefinition(
          @Nullable Class<?> beanClass,
          @Nullable ConstructorArgumentValues cargs,
          @Nullable PropertyValues pvs) {
    setPropertyValues(pvs);
    setBeanClass(beanClass);
    setConstructorArgumentValues(cargs);
  }

  /**
   * Create a new BeanDefinition with the given
   * constructor argument values and property values.
   */
  protected BeanDefinition(@Nullable ConstructorArgumentValues cargs, @Nullable PropertyValues pvs) {
    this.constructorArgumentValues = cargs;
    setPropertyValues(pvs);
  }

  /**
   * Create a new AbstractBeanDefinition as a deep copy of the given
   * bean definition.
   *
   * @param original the original bean definition to copy from
   */
  public BeanDefinition(BeanDefinition original) {
    copyFrom(original);
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
   * Set a description of the resource that this bean definition
   * came from (for the purpose of showing context in case of errors).
   *
   * @since 4.0
   */
  public void setResourceDescription(@Nullable String resourceDescription) {
    this.resource = resourceDescription != null ? new DescriptiveResource(resourceDescription) : null;
  }

  /**
   * Return a description of the resource that this bean definition
   * came from (for the purpose of showing context in case of errors).
   *
   * @since 4.0
   */
  @Nullable
  public String getResourceDescription() {
    return resource != null ? resource.toString() : null;
  }

  /**
   * Set the resource that this bean definition came from
   * (for the purpose of showing context in case of errors).
   *
   * @since 4.0
   */
  public void setResource(@Nullable Resource resource) {
    this.resource = resource;
  }

  /**
   * Return the resource that this bean definition came from.
   *
   * @since 4.0
   */
  @Nullable
  public Resource getResource() {
    return this.resource;
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
   * Specify whether or not the configured initializer method is the default.
   * <p>The default value is {@code true} for a locally specified init method
   * but switched to {@code false} for a shared setting in a defaults section
   * (e.g. {@code bean init-method} versus {@code beans default-init-method}
   * level in XML) which might not apply to all contained bean definitions.
   *
   * @see #setInitMethods
   * @see #applyDefaults
   * @since 4.0
   */
  public void setEnforceInitMethod(boolean enforceInitMethod) {
    this.enforceInitMethod = enforceInitMethod;
  }

  /**
   * Indicate whether the configured initializer method is the default.
   *
   * @see #getInitMethods()
   * @since 4.0
   */
  public boolean isEnforceInitMethod() {
    return this.enforceInitMethod;
  }

  /**
   * Specify whether or not the configured destroy method is the default.
   * <p>The default value is {@code true} for a locally specified destroy method
   * but switched to {@code false} for a shared setting in a defaults section
   * (e.g. {@code bean destroy-method} versus {@code beans default-destroy-method}
   * level in XML) which might not apply to all contained bean definitions.
   *
   * @see #setDestroyMethod
   * @see #applyDefaults
   * @since 4.0
   */
  public void setEnforceDestroyMethod(boolean enforceDestroyMethod) {
    this.enforceDestroyMethod = enforceDestroyMethod;
  }

  /**
   * Indicate whether the configured destroy method is the default.
   *
   * @see #getDestroyMethod()
   * @since 4.0
   */
  public boolean isEnforceDestroyMethod() {
    return this.enforceDestroyMethod;
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
  public void setScope(@Nullable String scope) {
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
    Object propertyValue = propertyValues().getPropertyValue(name);
    if (propertyValue == null) {
      throw new IllegalStateException("No propertyValue '" + name + "'");
    }
    return propertyValue;
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
    setEnforceInitMethod(false);
    setEnforceDestroyMethod(false);
    setAutowireMode(defaults.getAutowireMode());
    setInitMethods(defaults.getInitMethodName());
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

  /**
   * Specify a factory method name that refers to a non-overloaded method.
   *
   * @since 4.0
   */
  public void setUniqueFactoryMethodName(String name) {
    Assert.hasText(name, "Factory method name must not be empty");
    setFactoryMethodName(name);
    this.isFactoryMethodUnique = true;
  }

  /**
   * Specify a factory method name that refers to an overloaded method.
   *
   * @since 4.0
   */
  public void setNonUniqueFactoryMethodName(String name) {
    Assert.hasText(name, "Factory method name must not be empty");
    setFactoryMethodName(name);
    this.isFactoryMethodUnique = false;
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

  /**
   * Set the dependency check code.
   *
   * @param dependencyCheck the code to set.
   * Must be one of the four constants defined in this class.
   * @see #DEPENDENCY_CHECK_NONE
   * @see #DEPENDENCY_CHECK_OBJECTS
   * @see #DEPENDENCY_CHECK_SIMPLE
   * @see #DEPENDENCY_CHECK_ALL
   * @since 4.0
   */
  public void setDependencyCheck(int dependencyCheck) {
    this.dependencyCheck = dependencyCheck;
  }

  /**
   * Return the dependency check code.
   *
   * @since 4.0
   */
  public int getDependencyCheck() {
    return this.dependencyCheck;
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
   * Copy the qualifiers from the supplied BeanDefinition to this bean definition.
   *
   * @param source the BeanDefinition to copy from
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

  /**
   * Set the autowire mode. This determines whether any automagical detection
   * and setting of bean references will happen. Default is AUTOWIRE_NO
   * which means there won't be convention-based autowiring by name or type
   * (however, there may still be explicit annotation-driven autowiring).
   *
   * @param autowireMode the autowire mode to set.
   * Must be one of the constants defined in this class.
   * @see #AUTOWIRE_NO
   * @see #AUTOWIRE_BY_NAME
   * @see #AUTOWIRE_BY_TYPE
   * @see #AUTOWIRE_CONSTRUCTOR
   * @since 4.0
   */
  public void setAutowireMode(int autowireMode) {
    this.autowireMode = autowireMode;
  }

  /**
   * Return the autowire mode as specified in the bean definition.
   *
   * @since 4.0
   */
  public int getAutowireMode() {
    return this.autowireMode;
  }

  /**
   * Set the name of the parent definition of this bean definition, if any.
   *
   * @since 4.0
   */
  public void setParentName(@Nullable String parentName) {
    this.parentName = parentName;
  }

  /**
   * Return the name of the parent definition of this bean definition, if any.
   *
   * @since 4.0
   */
  @Nullable
  public String getParentName() {
    return parentName;
  }

  /**
   * Specify constructor argument values for this bean.
   *
   * @since 4.0
   */
  public void setConstructorArgumentValues(@Nullable ConstructorArgumentValues argumentValues) {
    this.constructorArgumentValues = argumentValues;
  }

  /**
   * Return constructor argument values for this bean (never {@code null}).
   *
   * @since 4.0
   */
  public ConstructorArgumentValues getConstructorArgumentValues() {
    if (this.constructorArgumentValues == null) {
      this.constructorArgumentValues = new ConstructorArgumentValues();
    }
    return this.constructorArgumentValues;
  }

  /**
   * Return if there are constructor argument values defined for this bean.
   *
   * @since 4.0
   */
  public boolean hasConstructorArgumentValues() {
    return constructorArgumentValues != null && !constructorArgumentValues.isEmpty();
  }

  /**
   * Specify whether to allow access to non-public constructors and methods,
   * for the case of externalized metadata pointing to those. The default is
   * {@code true}; switch this to {@code false} for public access only.
   * <p>This applies to constructor resolution, factory method resolution,
   * and also init/destroy methods. Bean property accessors have to be public
   * in any case and are not affected by this setting.
   * <p>Note that annotation-driven configuration will still access non-public
   * members as far as they have been annotated. This setting applies to
   * externalized metadata in this bean definition only.
   *
   * @since 4.0
   */
  public void setNonPublicAccessAllowed(boolean nonPublicAccessAllowed) {
    this.nonPublicAccessAllowed = nonPublicAccessAllowed;
  }

  /**
   * Return whether to allow access to non-public constructors and methods.
   *
   * @since 4.0
   */
  public boolean isNonPublicAccessAllowed() {
    return this.nonPublicAccessAllowed;
  }

  /**
   * Specify whether to resolve constructors in lenient mode ({@code true},
   * which is the default) or to switch to strict resolution (throwing an exception
   * in case of ambiguous constructors that all match when converting the arguments,
   * whereas lenient mode would use the one with the 'closest' type matches).
   *
   * @since 4.0
   */
  public void setLenientConstructorResolution(boolean lenientConstructorResolution) {
    this.lenientConstructorResolution = lenientConstructorResolution;
  }

  /**
   * Return whether to resolve constructors in lenient mode or in strict mode.
   *
   * @since 4.0
   */
  public boolean isLenientConstructorResolution() {
    return this.lenientConstructorResolution;
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

  /**
   * Register an externally managed configuration initialization method &mdash;
   * for example, a method annotated with JSR-250's
   * {@link jakarta.annotation.PostConstruct} annotation.
   * <p>The supplied {@code initMethod} may be the
   * {@linkplain Method#getName() simple method name} for non-private methods or the
   * {@linkplain cn.taketoday.util.ClassUtils#getQualifiedMethodName(Method)
   * qualified method name} for {@code private} methods. A qualified name is
   * necessary for {@code private} methods in order to disambiguate between
   * multiple private methods with the same name within a class hierarchy.
   *
   * @since 4.0
   */
  public void registerExternallyManagedInitMethod(String initMethod) {
    synchronized(this.postProcessingLock) {
      if (this.externallyManagedInitMethods == null) {
        this.externallyManagedInitMethods = new LinkedHashSet<>(1);
      }
      this.externallyManagedInitMethods.add(initMethod);
    }
  }

  /**
   * Determine if the given method name indicates an externally managed
   * initialization method.
   * <p>See {@link #registerExternallyManagedInitMethod} for details
   * regarding the format for the supplied {@code initMethod}.
   *
   * @since 4.0
   */
  public boolean isExternallyManagedInitMethod(String initMethod) {
    synchronized(this.postProcessingLock) {
      return (this.externallyManagedInitMethods != null &&
              this.externallyManagedInitMethods.contains(initMethod));
    }
  }

  /**
   * Determine if the given method name indicates an externally managed
   * initialization method, regardless of method visibility.
   * <p>In contrast to {@link #isExternallyManagedInitMethod(String)}, this
   * method also returns {@code true} if there is a {@code private} externally
   * managed initialization method that has been
   * {@linkplain #registerExternallyManagedInitMethod(String) registered}
   * using a qualified method name instead of a simple method name.
   *
   * @since 4.0
   */
  boolean hasAnyExternallyManagedInitMethod(String initMethod) {
    synchronized(this.postProcessingLock) {
      if (isExternallyManagedInitMethod(initMethod)) {
        return true;
      }
      if (this.externallyManagedInitMethods != null) {
        for (String candidate : this.externallyManagedInitMethods) {
          int indexOfDot = candidate.lastIndexOf(".");
          if (indexOfDot >= 0) {
            String methodName = candidate.substring(indexOfDot + 1);
            if (methodName.equals(initMethod)) {
              return true;
            }
          }
        }
      }
      return false;
    }
  }

  /**
   * Return all externally managed initialization methods (as an immutable Set).
   * <p>See {@link #registerExternallyManagedInitMethod} for details
   * regarding the format for the initialization methods in the returned set.
   *
   * @since 4.0
   */
  public Set<String> getExternallyManagedInitMethods() {
    synchronized(this.postProcessingLock) {
      return (this.externallyManagedInitMethods != null ?
              Collections.unmodifiableSet(new LinkedHashSet<>(this.externallyManagedInitMethods)) :
              Collections.emptySet());
    }
  }

  /**
   * Register an externally managed configuration destruction method &mdash;
   * for example, a method annotated with JSR-250's
   * {@link jakarta.annotation.PreDestroy} annotation.
   * <p>The supplied {@code destroyMethod} may be the
   * {@linkplain Method#getName() simple method name} for non-private methods or the
   * {@linkplain cn.taketoday.util.ClassUtils#getQualifiedMethodName(Method)
   * qualified method name} for {@code private} methods. A qualified name is
   * necessary for {@code private} methods in order to disambiguate between
   * multiple private methods with the same name within a class hierarchy.
   *
   * @since 4.0
   */
  public void registerExternallyManagedDestroyMethod(String destroyMethod) {
    synchronized(this.postProcessingLock) {
      if (this.externallyManagedDestroyMethods == null) {
        this.externallyManagedDestroyMethods = new LinkedHashSet<>(1);
      }
      this.externallyManagedDestroyMethods.add(destroyMethod);
    }
  }

  /**
   * Determine if the given method name indicates an externally managed
   * destruction method.
   * <p>See {@link #registerExternallyManagedDestroyMethod} for details
   * regarding the format for the supplied {@code destroyMethod}.
   *
   * @since 4.0
   */
  public boolean isExternallyManagedDestroyMethod(String destroyMethod) {
    synchronized(this.postProcessingLock) {
      return (this.externallyManagedDestroyMethods != null &&
              this.externallyManagedDestroyMethods.contains(destroyMethod));
    }
  }

  /**
   * Determine if the given method name indicates an externally managed
   * destruction method, regardless of method visibility.
   * <p>In contrast to {@link #isExternallyManagedDestroyMethod(String)}, this
   * method also returns {@code true} if there is a {@code private} externally
   * managed destruction method that has been
   * {@linkplain #registerExternallyManagedDestroyMethod(String) registered}
   * using a qualified method name instead of a simple method name.
   *
   * @since 4.0
   */
  boolean hasAnyExternallyManagedDestroyMethod(String destroyMethod) {
    synchronized(this.postProcessingLock) {
      if (isExternallyManagedDestroyMethod(destroyMethod)) {
        return true;
      }
      if (this.externallyManagedDestroyMethods != null) {
        for (String candidate : this.externallyManagedDestroyMethods) {
          int indexOfDot = candidate.lastIndexOf(".");
          if (indexOfDot >= 0) {
            String methodName = candidate.substring(indexOfDot + 1);
            if (methodName.equals(destroyMethod)) {
              return true;
            }
          }
        }
      }
      return false;
    }
  }

  /**
   * Get all externally managed destruction methods (as an immutable Set).
   * <p>See {@link #registerExternallyManagedDestroyMethod} for details
   * regarding the format for the destruction methods in the returned set.
   *
   * @since 4.0
   */
  public Set<String> getExternallyManagedDestroyMethods() {
    synchronized(this.postProcessingLock) {
      return (this.externallyManagedDestroyMethods != null ?
              Collections.unmodifiableSet(new LinkedHashSet<>(this.externallyManagedDestroyMethods)) :
              Collections.emptySet());
    }
  }

  // Object

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof BeanDefinition that)) {
      return false;
    }
    return Objects.equals(getBeanClassName(), that.getBeanClassName())
            && this.role == that.role
            && this.primary == that.primary
            && this.lazyInit == that.lazyInit
            && this.synthetic == that.synthetic
            && this.autowireMode == that.autowireMode
            && this.dependencyCheck == that.dependencyCheck
            && this.enforceInitMethod == that.enforceInitMethod
            && this.enforceDestroyMethod == that.enforceDestroyMethod
            && this.autowireCandidate == that.autowireCandidate
            && Objects.equals(this.scope, that.scope)
            && Objects.equals(this.destroyMethod, that.destroyMethod)
            && Objects.equals(this.factoryBeanName, that.factoryBeanName)
            && Objects.equals(this.factoryMethodName, that.factoryMethodName)
            && Objects.equals(this.qualifiers, that.qualifiers)
            && Objects.equals(this.parentName, that.parentName)
            && Arrays.equals(this.dependsOn, that.dependsOn)
            && equalsPropertyValues(that)
            && equalsConstructorArgumentValues(that)
            && super.equals(other);
  }

  private boolean equalsConstructorArgumentValues(BeanDefinition other) {
    if (!hasConstructorArgumentValues()) {
      return !other.hasConstructorArgumentValues();
    }
    return ObjectUtils.nullSafeEquals(this.constructorArgumentValues, other.constructorArgumentValues);
  }

  private boolean equalsPropertyValues(BeanDefinition other) {
    if (!hasPropertyValues()) {
      return !other.hasPropertyValues();
    }
    return ObjectUtils.nullSafeEquals(this.propertyValues, other.propertyValues);
  }

  @Override
  public int hashCode() {
    int hashCode = ObjectUtils.nullSafeHashCode(getBeanClassName());
    hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.scope);
    if (hasConstructorArgumentValues()) {
      hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.constructorArgumentValues);
    }
    if (hasPropertyValues()) {
      hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.propertyValues);
    }
    hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.factoryBeanName);
    hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.factoryMethodName);
    hashCode = 29 * hashCode + super.hashCode();
    return hashCode;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("class [");
    sb.append(getBeanClassName()).append(']');
    sb.append("; beanName=").append(this.beanName);
    sb.append("; aliases=").append(Arrays.toString(this.aliases));
    sb.append("; scope=").append(this.scope);
    sb.append("; lazyInit=").append(this.lazyInit);
    sb.append("; primary=").append(this.primary);
    sb.append("; factoryBean=").append(this.factoryBean);
    sb.append("; autowireMode=").append(this.autowireMode);
    sb.append("; dependencyCheck=").append(this.dependencyCheck);
    sb.append("; autowireCandidate=").append(this.autowireCandidate);
    sb.append("; initMethods=").append(Arrays.toString(initMethods));
    sb.append("; factoryBeanName=").append(this.factoryBeanName);
    sb.append("; factoryMethodName=").append(this.factoryMethodName);
    sb.append("; destroyMethod=").append(destroyMethod);

    if (this.resource != null) {
      sb.append("; defined in ").append(this.resource);
    }
    return sb.toString();
  }

}
