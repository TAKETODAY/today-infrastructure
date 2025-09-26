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

package infra.beans.factory.support;

import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import infra.beans.BeanMetadataAttributeAccessor;
import infra.beans.BeanUtils;
import infra.beans.PropertyValues;
import infra.beans.factory.BeanDefinitionValidationException;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.FactoryBean;
import infra.beans.factory.annotation.Autowired;
import infra.beans.factory.config.AutowireCapableBeanFactory;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.ConstructorArgumentValues;
import infra.core.AttributeAccessor;
import infra.core.Ordered;
import infra.core.ResolvableType;
import infra.core.annotation.Order;
import infra.core.io.DescriptiveResource;
import infra.core.io.Resource;
import infra.lang.Assert;
import infra.util.ClassUtils;
import infra.util.ObjectUtils;
import infra.util.ReflectionUtils;
import infra.util.StringUtils;

/**
 * Base class for concrete, full-fledged {@link BeanDefinition} classes,
 * factoring out common properties of {@link GenericBeanDefinition},
 * {@link RootBeanDefinition}, and {@link ChildBeanDefinition}.
 *
 * <p>The autowire constants match the ones defined in the
 * {@link AutowireCapableBeanFactory}
 * interface.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Mark Fisher
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see GenericBeanDefinition
 * @see RootBeanDefinition
 * @see ChildBeanDefinition
 * @since 4.0 2022/3/8 21:11
 */
public abstract class AbstractBeanDefinition extends BeanMetadataAttributeAccessor
        implements BeanDefinition, Cloneable, Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Constant for the default scope name: {@code ""}, equivalent to singleton
   * status unless overridden from a parent bean definition (if applicable).
   */
  public static final String SCOPE_DEFAULT = "";

  /**
   * Constant that indicates no external autowiring at all.
   *
   * @see #setAutowireMode
   */
  public static final int AUTOWIRE_NO = AutowireCapableBeanFactory.AUTOWIRE_NO;

  /**
   * Constant that indicates autowiring bean properties by name.
   *
   * @see #setAutowireMode
   */
  public static final int AUTOWIRE_BY_NAME = AutowireCapableBeanFactory.AUTOWIRE_BY_NAME;

  /**
   * Constant that indicates autowiring bean properties by type.
   *
   * @see #setAutowireMode
   */
  public static final int AUTOWIRE_BY_TYPE = AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE;

  /**
   * Constant that indicates autowiring a constructor.
   *
   * @see #setAutowireMode
   */
  public static final int AUTOWIRE_CONSTRUCTOR = AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR;

  /**
   * Constant that indicates determining an appropriate autowire strategy
   * through introspection of the bean class.
   *
   * @see #setAutowireMode
   */
  public static final int AUTOWIRE_AUTODETECT = AutowireCapableBeanFactory.AUTOWIRE_AUTODETECT;

  /**
   * Constant that indicates no dependency check at all.
   *
   * @see #setDependencyCheck
   */
  public static final int DEPENDENCY_CHECK_NONE = 0;

  /**
   * Constant that indicates dependency checking for object references.
   *
   * @see #setDependencyCheck
   */
  public static final int DEPENDENCY_CHECK_OBJECTS = 1;

  /**
   * Constant that indicates dependency checking for "simple" properties.
   *
   * @see #setDependencyCheck
   * @see BeanUtils#isSimpleProperty
   */
  public static final int DEPENDENCY_CHECK_SIMPLE = 2;

  /**
   * Constant that indicates dependency checking for all properties
   * (object references as well as "simple" properties).
   *
   * @see #setDependencyCheck
   */
  public static final int DEPENDENCY_CHECK_ALL = 3;

  /**
   * Constant that indicates the container should attempt to infer the
   * {@link #setDestroyMethodName destroy method name} for a bean as opposed to
   * explicit specification of a method name. The value {@value} is specifically
   * designed to include characters otherwise illegal in a method name, ensuring
   * no possibility of collisions with legitimately named methods having the same
   * name.
   * <p>Currently, the method names detected during destroy method inference
   * are "close" and "shutdown", if present on the specific bean class.
   */
  public static final String INFER_METHOD = "(inferred)";

  /**
   * The name of an attribute that can be
   * {@link AttributeAccessor#setAttribute set} on a
   * {@link BeanDefinition} so that
   * bean definitions can indicate one or more preferred constructors. This is
   * analogous to {@code @Autowired} annotated constructors on the bean class.
   * <p>The attribute value may be a single {@link java.lang.reflect.Constructor}
   * reference or an array thereof.
   *
   * @see Autowired
   * @see RootBeanDefinition#getPreferredConstructors()
   */
  public static final String PREFERRED_CONSTRUCTORS_ATTRIBUTE = "preferredConstructors";

  /**
   * The name of an attribute that can be
   * {@link AttributeAccessor#setAttribute set} on a
   * {@link BeanDefinition} so that
   * bean definitions can indicate the sort order for the targeted bean.
   * This is analogous to the {@code @Order} annotation.
   *
   * @see Order
   * @see Ordered
   */
  public static final String ORDER_ATTRIBUTE = "order";

  @Nullable
  private volatile Object beanClass;

  @Nullable
  private String scope = SCOPE_DEFAULT;

  private boolean abstractFlag = false;

  @Nullable
  private Boolean lazyInit;

  private boolean backgroundInit = false;

  private int autowireMode = AUTOWIRE_NO;

  private int dependencyCheck = DEPENDENCY_CHECK_NONE;

  @Nullable
  private String[] dependsOn;

  private boolean autowireCandidate = true;

  private boolean defaultCandidate = true;

  private boolean primary = false;

  private boolean fallback = false;

  @Nullable
  private LinkedHashMap<String, AutowireCandidateQualifier> qualifiers;

  @Nullable
  private Supplier<?> instanceSupplier;

  private boolean nonPublicAccessAllowed = true;

  private boolean lenientConstructorResolution = true;

  @Nullable
  private String factoryBeanName;

  @Nullable
  private String factoryMethodName;

  @Nullable
  private ConstructorArgumentValues constructorArgumentValues;

  @Nullable
  private PropertyValues propertyValues;

  @Nullable
  private MethodOverrides methodOverrides;

  @Nullable
  private String[] initMethodNames;

  @Nullable
  private String[] destroyMethodNames;

  private boolean enforceInitMethod = true;

  private boolean enforceDestroyMethod = true;

  private boolean synthetic = false;

  private int role = ROLE_APPLICATION;

  @Nullable
  private String description;

  @Nullable
  private Resource resource;

  /** enable DI */
  private boolean enableDependencyInjection = true;

  /**
   * Create a new AbstractBeanDefinition with default settings.
   */
  protected AbstractBeanDefinition() {
    this(null, null);
  }

  /**
   * Create a new AbstractBeanDefinition with the given
   * constructor argument values and property values.
   */
  protected AbstractBeanDefinition(@Nullable ConstructorArgumentValues cargs, @Nullable PropertyValues pvs) {
    this.constructorArgumentValues = cargs;
    this.propertyValues = pvs;
  }

  /**
   * Create a new AbstractBeanDefinition as a deep copy of the given
   * bean definition.
   *
   * @param original the original bean definition to copy from
   */
  protected AbstractBeanDefinition(BeanDefinition original) {
    setParentName(original.getParentName());
    setBeanClassName(original.getBeanClassName());
    setScope(original.getScope());
    setAbstract(original.isAbstract());
    setFactoryBeanName(original.getFactoryBeanName());
    setFactoryMethodName(original.getFactoryMethodName());
    setRole(original.getRole());
    setSource(original.getSource());
    copyFrom(original);

    setEnableDependencyInjection(original.isEnableDependencyInjection());

    if (original instanceof AbstractBeanDefinition originalAbd) {
      if (originalAbd.hasBeanClass()) {
        setBeanClass(originalAbd.getBeanClass());
      }
      if (originalAbd.hasConstructorArgumentValues()) {
        setConstructorArgumentValues(new ConstructorArgumentValues(original.getConstructorArgumentValues()));
      }
      if (originalAbd.hasPropertyValues()) {
        setPropertyValues(new PropertyValues(original.getPropertyValues()));
      }
      if (originalAbd.hasMethodOverrides()) {
        setMethodOverrides(new MethodOverrides(originalAbd.getMethodOverrides()));
      }
      Boolean lazyInit = originalAbd.getLazyInit();
      if (lazyInit != null) {
        setLazyInit(lazyInit);
      }
      setBackgroundInit(originalAbd.isBackgroundInit());
      setAutowireMode(originalAbd.getAutowireMode());
      setDependencyCheck(originalAbd.getDependencyCheck());
      setDependsOn(originalAbd.getDependsOn());
      setAutowireCandidate(originalAbd.isAutowireCandidate());
      setDefaultCandidate(originalAbd.isDefaultCandidate());
      setPrimary(originalAbd.isPrimary());
      setFallback(originalAbd.isFallback());
      copyQualifiersFrom(originalAbd);
      setInstanceSupplier(originalAbd.getInstanceSupplier());
      setNonPublicAccessAllowed(originalAbd.isNonPublicAccessAllowed());
      setLenientConstructorResolution(originalAbd.isLenientConstructorResolution());
      setInitMethodNames(originalAbd.getInitMethodNames());
      setEnforceInitMethod(originalAbd.isEnforceInitMethod());
      setDestroyMethodNames(originalAbd.getDestroyMethodNames());
      setEnforceDestroyMethod(originalAbd.isEnforceDestroyMethod());
      setSynthetic(originalAbd.isSynthetic());
      setResource(originalAbd.getResource());
    }
    else {
      setConstructorArgumentValues(new ConstructorArgumentValues(original.getConstructorArgumentValues()));
      setPropertyValues(new PropertyValues(original.getPropertyValues()));
      setLazyInit(original.isLazyInit());
      setResourceDescription(original.getResourceDescription());
    }
  }

  /**
   * Override settings in this bean definition (presumably a copied parent
   * from a parent-child inheritance relationship) from the given bean
   * definition (presumably the child).
   * <ul>
   * <li>Will override beanClass if specified in the given bean definition.
   * <li>Will always take {@code abstract}, {@code scope},
   * {@code lazyInit}, {@code autowireMode}, {@code dependencyCheck},
   * and {@code dependsOn} from the given bean definition.
   * <li>Will add {@code constructorArgumentValues}, {@code propertyValues},
   * {@code methodOverrides} from the given bean definition to existing ones.
   * <li>Will override {@code factoryBeanName}, {@code factoryMethodName},
   * {@code initMethodName}, and {@code destroyMethodName} if specified
   * in the given bean definition.
   * </ul>
   */
  public void overrideFrom(BeanDefinition other) {
    if (StringUtils.isNotEmpty(other.getBeanClassName())) {
      setBeanClassName(other.getBeanClassName());
    }
    if (StringUtils.isNotEmpty(other.getScope())) {
      setScope(other.getScope());
    }
    setAbstract(other.isAbstract());
    if (StringUtils.isNotEmpty(other.getFactoryBeanName())) {
      setFactoryBeanName(other.getFactoryBeanName());
    }
    if (StringUtils.isNotEmpty(other.getFactoryMethodName())) {
      setFactoryMethodName(other.getFactoryMethodName());
    }
    setRole(other.getRole());
    setSource(other.getSource());
    copyFrom(other);

    setEnableDependencyInjection(other.isEnableDependencyInjection());

    if (other instanceof AbstractBeanDefinition otherAbd) {
      if (otherAbd.hasBeanClass()) {
        setBeanClass(otherAbd.getBeanClass());
      }
      if (otherAbd.hasConstructorArgumentValues()) {
        getConstructorArgumentValues().addArgumentValues(other.getConstructorArgumentValues());
      }
      if (otherAbd.hasPropertyValues()) {
        getPropertyValues().add(other.getPropertyValues());
      }
      if (otherAbd.hasMethodOverrides()) {
        getMethodOverrides().addOverrides(otherAbd.getMethodOverrides());
      }
      Boolean lazyInit = otherAbd.getLazyInit();
      if (lazyInit != null) {
        setLazyInit(lazyInit);
      }
      setBackgroundInit(otherAbd.isBackgroundInit());
      setAutowireMode(otherAbd.getAutowireMode());
      setDependencyCheck(otherAbd.getDependencyCheck());
      setDependsOn(otherAbd.getDependsOn());
      setAutowireCandidate(otherAbd.isAutowireCandidate());
      setDefaultCandidate(otherAbd.isDefaultCandidate());
      setPrimary(otherAbd.isPrimary());
      setFallback(otherAbd.isFallback());
      copyQualifiersFrom(otherAbd);
      setInstanceSupplier(otherAbd.getInstanceSupplier());
      setNonPublicAccessAllowed(otherAbd.isNonPublicAccessAllowed());
      setLenientConstructorResolution(otherAbd.isLenientConstructorResolution());
      if (otherAbd.getInitMethodNames() != null) {
        setInitMethodNames(otherAbd.getInitMethodNames());
        setEnforceInitMethod(otherAbd.isEnforceInitMethod());
      }
      if (otherAbd.getDestroyMethodNames() != null) {
        setDestroyMethodNames(otherAbd.getDestroyMethodNames());
        setEnforceDestroyMethod(otherAbd.isEnforceDestroyMethod());
      }
      setSynthetic(otherAbd.isSynthetic());
      setResource(otherAbd.getResource());
    }
    else {
      getConstructorArgumentValues().addArgumentValues(other.getConstructorArgumentValues());
      getPropertyValues().add(other.getPropertyValues());
      setLazyInit(other.isLazyInit());
      setResourceDescription(other.getResourceDescription());
    }
  }

  /**
   * Apply the provided default values to this bean.
   *
   * @param defaults the default settings to apply
   */
  public void applyDefaults(BeanDefinitionDefaults defaults) {
    Boolean lazyInit = defaults.getLazyInit();
    if (lazyInit != null) {
      setLazyInit(lazyInit);
    }
    setAutowireMode(defaults.getAutowireMode());
    setDependencyCheck(defaults.getDependencyCheck());
    setInitMethodName(defaults.getInitMethodName());
    setEnforceInitMethod(false);
    setDestroyMethodName(defaults.getDestroyMethodName());
    setEnforceDestroyMethod(false);
  }

  @Override
  public void setEnableDependencyInjection(boolean enableDependencyInjection) {
    this.enableDependencyInjection = enableDependencyInjection;
  }

  @Override
  public boolean isEnableDependencyInjection() {
    return enableDependencyInjection;
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
    Object beanClassObject = this.beanClass;  // defensive access to volatile beanClass field
    return beanClassObject instanceof Class<?> clazz ? clazz.getName() : (String) beanClassObject;
  }

  /**
   * Specify the class for this bean.
   *
   * @see #setBeanClassName(String)
   */
  public void setBeanClass(@Nullable Class<?> beanClass) {
    this.beanClass = beanClass;
  }

  /**
   * Return the specified class of the bean definition (assuming it is resolved already).
   * <p><b>NOTE:</b> This is an initial class reference as declared in the bean metadata
   * definition, potentially combined with a declared factory method or a
   * {@link FactoryBean} which may lead to a different
   * runtime type of the bean, or not being set at all in case of an instance-level
   * factory method (which is resolved via {@link #getFactoryBeanName()} instead).
   * <b>Do not use this for runtime type introspection of arbitrary bean definitions.</b>
   * The recommended way to find out about the actual runtime type of a particular bean
   * is a {@link BeanFactory#getType} call for the
   * specified bean name; this takes all of the above cases into account and returns the
   * type of object that a {@link BeanFactory#getBean}
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
    if (!(beanClassObject instanceof Class<?> clazz)) {
      throw new IllegalStateException("Bean class name [%s] has not been resolved into an actual Class"
              .formatted(beanClassObject));
    }
    return clazz;
  }

  /**
   * Return whether this definition specifies a bean class.
   *
   * @see #getBeanClass()
   * @see #setBeanClass(Class)
   * @see #resolveBeanClass(ClassLoader)
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
   * Return a resolvable type for this bean definition.
   * <p>This implementation delegates to {@link #getBeanClass()}.
   */
  @Override
  public ResolvableType getResolvableType() {
    return hasBeanClass() ? ResolvableType.forClass(getBeanClass()) : ResolvableType.NONE;
  }

  /**
   * Set the name of the target scope for the bean.
   * <p>The default is singleton status, although this is only applied once
   * a bean definition becomes active in the containing factory. A bean
   * definition may eventually inherit its scope from a parent bean definition.
   * For this reason, the default scope name is an empty string (i.e., {@code ""}),
   * with singleton status being assumed until a resolved scope is set.
   *
   * @see #SCOPE_SINGLETON
   * @see #SCOPE_PROTOTYPE
   */
  @Override
  public void setScope(@Nullable String scope) {
    this.scope = scope;
  }

  /**
   * Return the name of the target scope for the bean.
   */
  @Override
  @Nullable
  public String getScope() {
    return this.scope;
  }

  /**
   * Return whether this a <b>Singleton</b>, with a single shared instance
   * returned from all calls.
   *
   * @see #SCOPE_SINGLETON
   */
  @Override
  public boolean isSingleton() {
    return SCOPE_SINGLETON.equals(this.scope) || SCOPE_DEFAULT.equals(this.scope);
  }

  /**
   * Return whether this a <b>Prototype</b>, with an independent instance
   * returned for each call.
   *
   * @see #SCOPE_PROTOTYPE
   */
  @Override
  public boolean isPrototype() {
    return SCOPE_PROTOTYPE.equals(this.scope);
  }

  /**
   * Set if this bean is "abstract", i.e. not meant to be instantiated itself but
   * rather just serving as parent for concrete child bean definitions.
   * <p>Default is "false". Specify true to tell the bean factory to not try to
   * instantiate that particular bean in any case.
   */
  public void setAbstract(boolean abstractFlag) {
    this.abstractFlag = abstractFlag;
  }

  /**
   * Return whether this bean is "abstract", i.e. not meant to be instantiated
   * itself but rather just serving as parent for concrete child bean definitions.
   */
  @Override
  public boolean isAbstract() {
    return this.abstractFlag;
  }

  /**
   * Set whether this bean should be lazily initialized.
   * <p>If {@code false}, the bean will get instantiated on startup by bean
   * factories that perform eager initialization of singletons.
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
   */
  @Override
  public boolean isLazyInit() {
    return lazyInit != null && this.lazyInit;
  }

  /**
   * Return whether this bean should be lazily initialized, i.e. not
   * eagerly instantiated on startup. Only applicable to a singleton bean.
   *
   * @return the lazy-init flag if explicitly set, or {@code null} otherwise
   */
  @Nullable
  public Boolean getLazyInit() {
    return this.lazyInit;
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
   * @see #AUTOWIRE_AUTODETECT
   */
  public void setAutowireMode(int autowireMode) {
    this.autowireMode = autowireMode;
  }

  /**
   * Return the autowire mode as specified in the bean definition.
   */
  public int getAutowireMode() {
    return this.autowireMode;
  }

  /**
   * Return the resolved autowire code,
   * (resolving AUTOWIRE_AUTODETECT to AUTOWIRE_CONSTRUCTOR or AUTOWIRE_BY_TYPE).
   *
   * @see #AUTOWIRE_AUTODETECT
   * @see #AUTOWIRE_CONSTRUCTOR
   * @see #AUTOWIRE_BY_TYPE
   */
  public int getResolvedAutowireMode() {
    if (this.autowireMode == AUTOWIRE_AUTODETECT) {
      // Work out whether to apply setter autowiring or constructor autowiring.
      // If it has a no-arg constructor it's deemed to be setter autowiring,
      // otherwise we'll try constructor autowiring.
      Constructor<?>[] constructors = getBeanClass().getConstructors();
      for (Constructor<?> constructor : constructors) {
        if (constructor.getParameterCount() == 0) {
          return AUTOWIRE_BY_TYPE;
        }
      }
      return AUTOWIRE_CONSTRUCTOR;
    }
    else {
      return this.autowireMode;
    }
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
   */
  public void setDependencyCheck(int dependencyCheck) {
    this.dependencyCheck = dependencyCheck;
  }

  /**
   * Return the dependency check code.
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
   */
  @Override
  public void setDependsOn(@Nullable String... dependsOn) {
    this.dependsOn = dependsOn;
  }

  /**
   * Return the bean names that this bean depends on.
   */
  @Override
  @Nullable
  public String[] getDependsOn() {
    return this.dependsOn;
  }

  /**
   * {@inheritDoc}
   * <p>The default is {@code true}, allowing injection by type at any injection point.
   * Switch this to {@code false} in order to disable autowiring by type for this bean.
   *
   * @see #AUTOWIRE_BY_TYPE
   * @see #AUTOWIRE_BY_NAME
   */
  @Override
  public void setAutowireCandidate(boolean autowireCandidate) {
    this.autowireCandidate = autowireCandidate;
  }

  /**
   * {@inheritDoc}
   * <p>The default is {@code true}.
   */
  @Override
  public boolean isAutowireCandidate() {
    return this.autowireCandidate;
  }

  /**
   * Set whether this bean is a candidate for getting autowired into some other
   * bean based on the plain type, without any further indications such as a
   * qualifier match.
   * <p>The default is {@code true}, allowing injection by type at any injection point.
   * Switch this to {@code false} in order to restrict injection by default,
   * effectively enforcing an additional indication such as a qualifier match.
   */
  public void setDefaultCandidate(boolean defaultCandidate) {
    this.defaultCandidate = defaultCandidate;
  }

  /**
   * Return whether this bean is a candidate for getting autowired into some other
   * bean based on the plain type, without any further indications such as a
   * qualifier match?
   * <p>The default is {@code true}.
   */
  public boolean isDefaultCandidate() {
    return this.defaultCandidate;
  }

  /**
   * Set whether this bean is a primary autowire candidate.
   * <p>If this value is {@code true} for exactly one bean among multiple
   * matching candidates, it will serve as a tie-breaker.
   */
  @Override
  public void setPrimary(boolean primary) {
    this.primary = primary;
  }

  /**
   * Return whether this bean is a primary autowire candidate.
   */
  @Override
  public boolean isPrimary() {
    return this.primary;
  }

  /**
   * {@inheritDoc}
   * <p>The default is {@code false}.
   */
  @Override
  public void setFallback(boolean fallback) {
    this.fallback = fallback;
  }

  /**
   * {@inheritDoc}
   * <p>The default is {@code false}.
   */
  @Override
  public boolean isFallback() {
    return this.fallback;
  }

  /**
   * Register a qualifier to be used for autowire candidate resolution,
   * keyed by the qualifier's type name.
   *
   * @see AutowireCandidateQualifier#getTypeName()
   */
  public void addQualifier(AutowireCandidateQualifier qualifier) {
    qualifiers().put(qualifier.getTypeName(), qualifier);
  }

  /**
   * Return whether this bean has the specified qualifier.
   */
  public boolean hasQualifier(String typeName) {
    return qualifiers != null && qualifiers.containsKey(typeName);
  }

  /**
   * Return the qualifier mapped to the provided type name.
   */
  @Nullable
  public AutowireCandidateQualifier getQualifier(String typeName) {
    return qualifiers().get(typeName);
  }

  /**
   * Return all registered qualifiers.
   *
   * @return the Set of {@link AutowireCandidateQualifier} objects.
   */
  public Set<AutowireCandidateQualifier> getQualifiers() {
    if (qualifiers == null) {
      return new LinkedHashSet<>();
    }
    return new LinkedHashSet<>(this.qualifiers.values());
  }

  /**
   * Copy the qualifiers from the supplied AbstractBeanDefinition to this bean definition.
   *
   * @param source the AbstractBeanDefinition to copy from
   */
  public void copyQualifiersFrom(AbstractBeanDefinition source) {
    Assert.notNull(source, "Source is required");
    if (source.qualifiers != null) {
      qualifiers().putAll(source.qualifiers);
    }
  }

  private LinkedHashMap<String, AutowireCandidateQualifier> qualifiers() {
    LinkedHashMap<String, AutowireCandidateQualifier> qualifiers = this.qualifiers;
    if (qualifiers == null) {
      qualifiers = new LinkedHashMap<>();
      this.qualifiers = qualifiers;
    }
    return qualifiers;
  }

  /**
   * Specify a callback for creating an instance of the bean,
   * as an alternative to a declaratively specified factory method.
   * <p>If such a callback is set, it will override any other constructor
   * or factory method metadata. However, bean property population and
   * potential annotation-driven injection will still apply as usual.
   *
   * @see #setConstructorArgumentValues(ConstructorArgumentValues)
   * @see #setPropertyValues(PropertyValues)
   */
  public void setInstanceSupplier(@Nullable Supplier<?> instanceSupplier) {
    this.instanceSupplier = instanceSupplier;
  }

  /**
   * Return a callback for creating an instance of the bean, if any.
   */
  @Nullable
  public Supplier<?> getInstanceSupplier() {
    return this.instanceSupplier;
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
   */
  public void setNonPublicAccessAllowed(boolean nonPublicAccessAllowed) {
    this.nonPublicAccessAllowed = nonPublicAccessAllowed;
  }

  /**
   * Return whether to allow access to non-public constructors and methods.
   */
  public boolean isNonPublicAccessAllowed() {
    return this.nonPublicAccessAllowed;
  }

  /**
   * Specify whether to resolve constructors in lenient mode ({@code true},
   * which is the default) or to switch to strict resolution (throwing an exception
   * in case of ambiguous constructors that all match when converting the arguments,
   * whereas lenient mode would use the one with the 'closest' type matches).
   */
  public void setLenientConstructorResolution(boolean lenientConstructorResolution) {
    this.lenientConstructorResolution = lenientConstructorResolution;
  }

  /**
   * Return whether to resolve constructors in lenient mode or in strict mode.
   */
  public boolean isLenientConstructorResolution() {
    return this.lenientConstructorResolution;
  }

  /**
   * Specify the factory bean to use, if any.
   * This the name of the bean to call the specified factory method on.
   *
   * @see #setFactoryMethodName
   */
  @Override
  public void setFactoryBeanName(@Nullable String factoryBeanName) {
    this.factoryBeanName = factoryBeanName;
  }

  /**
   * Return the factory bean name, if any.
   */
  @Override
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
   */
  @Override
  public void setFactoryMethodName(@Nullable String factoryMethodName) {
    this.factoryMethodName = factoryMethodName;
  }

  /**
   * Return a factory method, if any.
   */
  @Override
  @Nullable
  public String getFactoryMethodName() {
    return this.factoryMethodName;
  }

  /**
   * Specify constructor argument values for this bean.
   */
  public void setConstructorArgumentValues(@Nullable ConstructorArgumentValues constructorArgumentValues) {
    this.constructorArgumentValues = constructorArgumentValues;
  }

  /**
   * Return constructor argument values for this bean (never {@code null}).
   */
  @Override
  public ConstructorArgumentValues getConstructorArgumentValues() {
    ConstructorArgumentValues cav = this.constructorArgumentValues;
    if (cav == null) {
      cav = new ConstructorArgumentValues();
      this.constructorArgumentValues = cav;
    }
    return cav;
  }

  /**
   * Return if there are constructor argument values defined for this bean.
   */
  @Override
  public boolean hasConstructorArgumentValues() {
    return constructorArgumentValues != null && !constructorArgumentValues.isEmpty();
  }

  /**
   * Specify property values for this bean, if any.
   */
  public void setPropertyValues(@Nullable PropertyValues propertyValues) {
    this.propertyValues = propertyValues;
  }

  /**
   * Return property values for this bean (never {@code null}).
   */
  @Override
  public PropertyValues getPropertyValues() {
    PropertyValues propertyValues = this.propertyValues;
    if (propertyValues == null) {
      propertyValues = new PropertyValues();
      this.propertyValues = propertyValues;
    }
    return propertyValues;
  }

  /**
   * Return if there are property values defined for this bean.
   */
  @Override
  public boolean hasPropertyValues() {
    return propertyValues != null && !propertyValues.isEmpty();
  }

  /**
   * Specify method overrides for the bean, if any.
   */
  public void setMethodOverrides(@Nullable MethodOverrides methodOverrides) {
    this.methodOverrides = methodOverrides;
  }

  /**
   * Return information about methods to be overridden by the IoC
   * container. This will be empty if there are no method overrides.
   * <p>Never returns {@code null}.
   */
  public MethodOverrides getMethodOverrides() {
    MethodOverrides methodOverrides = this.methodOverrides;
    if (methodOverrides == null) {
      methodOverrides = new MethodOverrides();
      this.methodOverrides = methodOverrides;
    }
    return methodOverrides;
  }

  /**
   * Return if there are method overrides defined for this bean.
   */
  public boolean hasMethodOverrides() {
    return methodOverrides != null && !methodOverrides.isEmpty();
  }

  /**
   * Specify the names of multiple initializer methods.
   * <p>The default is {@code null} in which case there are no initializer methods.
   *
   * @see #setInitMethodName
   */
  public void setInitMethodNames(@Nullable String... initMethodNames) {
    this.initMethodNames = initMethodNames;
  }

  /**
   * Return the names of the initializer methods.
   */
  @Nullable
  public String[] getInitMethodNames() {
    return this.initMethodNames;
  }

  /**
   * Set the name of the initializer method.
   * <p>The default is {@code null} in which case there is no initializer method.
   *
   * @see #setInitMethodNames
   */
  @Override
  public void setInitMethodName(@Nullable String initMethodName) {
    this.initMethodNames = (initMethodName != null ? new String[] { initMethodName } : null);
  }

  /**
   * Return the name of the initializer method (the first one in case of multiple methods).
   */
  @Override
  @Nullable
  public String getInitMethodName() {
    return ObjectUtils.isNotEmpty(this.initMethodNames) ? this.initMethodNames[0] : null;
  }

  /**
   * Specify whether or not the configured initializer method is the default.
   * <p>The default value is {@code true} for a locally specified init method
   * but switched to {@code false} for a shared setting in a defaults section
   * (e.g. {@code bean init-method} versus {@code beans default-init-method}
   * level in XML) which might not apply to all contained bean definitions.
   *
   * @see #setInitMethodName
   * @see #applyDefaults
   */
  public void setEnforceInitMethod(boolean enforceInitMethod) {
    this.enforceInitMethod = enforceInitMethod;
  }

  /**
   * Indicate whether the configured initializer method is the default.
   *
   * @see #getInitMethodName()
   */
  public boolean isEnforceInitMethod() {
    return this.enforceInitMethod;
  }

  /**
   * Specify the names of multiple destroy methods.
   * <p>The default is {@code null} in which case there are no destroy methods.
   *
   * @see #setDestroyMethodName
   */
  public void setDestroyMethodNames(@Nullable String... destroyMethodNames) {
    this.destroyMethodNames = destroyMethodNames;
  }

  /**
   * Return the names of the destroy methods.
   */
  @Nullable
  public String[] getDestroyMethodNames() {
    return this.destroyMethodNames;
  }

  /**
   * Set the name of the destroy method.
   * <p>The default is {@code null} in which case there is no destroy method.
   *
   * @see #setDestroyMethodNames
   */
  @Override
  public void setDestroyMethodName(@Nullable String destroyMethodName) {
    this.destroyMethodNames = (destroyMethodName != null ? new String[] { destroyMethodName } : null);
  }

  /**
   * Return the name of the destroy method (the first one in case of multiple methods).
   */
  @Override
  @Nullable
  public String getDestroyMethodName() {
    return ObjectUtils.isNotEmpty(destroyMethodNames) ? destroyMethodNames[0] : null;
  }

  /**
   * Specify whether or not the configured destroy method is the default.
   * <p>The default value is {@code true} for a locally specified destroy method
   * but switched to {@code false} for a shared setting in a defaults section
   * (e.g. {@code bean destroy-method} versus {@code beans default-destroy-method}
   * level in XML) which might not apply to all contained bean definitions.
   *
   * @see #setDestroyMethodName
   * @see #applyDefaults
   */
  public void setEnforceDestroyMethod(boolean enforceDestroyMethod) {
    this.enforceDestroyMethod = enforceDestroyMethod;
  }

  /**
   * Indicate whether the configured destroy method is the default.
   *
   * @see #getDestroyMethodName()
   */
  public boolean isEnforceDestroyMethod() {
    return this.enforceDestroyMethod;
  }

  /**
   * Set whether this bean definition is 'synthetic', that is, not defined
   * by the application itself (for example, an infrastructure bean such
   * as a helper for auto-proxying, created through {@code <aop:config>}).
   */
  public void setSynthetic(boolean synthetic) {
    this.synthetic = synthetic;
  }

  /**
   * Return whether this bean definition is 'synthetic', that is,
   * not defined by the application itself.
   */
  public boolean isSynthetic() {
    return this.synthetic;
  }

  /**
   * Set the role hint for this {@code BeanDefinition}.
   */
  @Override
  public void setRole(int role) {
    this.role = role;
  }

  /**
   * Return the role hint for this {@code BeanDefinition}.
   */
  @Override
  public int getRole() {
    return this.role;
  }

  /**
   * Set a human-readable description of this bean definition.
   */
  @Override
  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  /**
   * Return a human-readable description of this bean definition.
   */
  @Override
  @Nullable
  public String getDescription() {
    return this.description;
  }

  /**
   * Set the resource that this bean definition came from
   * (for the purpose of showing context in case of errors).
   */
  public void setResource(@Nullable Resource resource) {
    this.resource = resource;
  }

  /**
   * Return the resource that this bean definition came from.
   */
  @Nullable
  public Resource getResource() {
    return this.resource;
  }

  /**
   * Set a description of the resource that this bean definition
   * came from (for the purpose of showing context in case of errors).
   */
  public void setResourceDescription(@Nullable String resourceDescription) {
    this.resource = resourceDescription != null ? new DescriptiveResource(resourceDescription) : null;
  }

  /**
   * Return a description of the resource that this bean definition
   * came from (for the purpose of showing context in case of errors).
   */
  @Override
  @Nullable
  public String getResourceDescription() {
    return resource != null ? resource.toString() : null;
  }

  /**
   * Set the originating (e.g. decorated) BeanDefinition, if any.
   */
  public void setOriginatingBeanDefinition(BeanDefinition originatingBd) {
    this.resource = new BeanDefinitionResource(originatingBd);
  }

  /**
   * Specify the bootstrap mode for this bean: default is {@code false} for using
   * the main pre-instantiation thread for non-lazy singleton beans and the caller
   * thread for prototype beans.
   * <p>Set this flag to {@code true} to allow for instantiating this bean on a
   * background thread. For a non-lazy singleton, a background pre-instantiation
   * thread can be used then, while still enforcing the completion at the end of
   * {@link StandardBeanFactory#preInstantiateSingletons()}.
   * For a lazy singleton, a background pre-instantiation thread can be used as well
   * - with completion allowed at a later point, enforcing it when actually accessed.
   * <p>Note that this flag may be ignored by bean factories not set up for
   * background bootstrapping, always applying single-threaded bootstrapping
   * for non-lazy singleton beans.
   *
   * @see #setLazyInit
   * @see StandardBeanFactory#setBootstrapExecutor
   * @since 4.0
   */
  public void setBackgroundInit(boolean backgroundInit) {
    this.backgroundInit = backgroundInit;
  }

  /**
   * Return the bootstrap mode for this bean: default is {@code false} for using
   * the main pre-instantiation thread for non-lazy singleton beans and the caller
   * thread for prototype beans.
   *
   * @since 4.0
   */
  public boolean isBackgroundInit() {
    return this.backgroundInit;
  }

  /**
   * Return the originating BeanDefinition, or {@code null} if none.
   * Allows for retrieving the decorated bean definition, if any.
   * <p>Note that this method returns the immediate originator. Iterate through the
   * originator chain to find the original BeanDefinition as defined by the user.
   */
  @Override
  @Nullable
  public BeanDefinition getOriginatingBeanDefinition() {
    return resource instanceof BeanDefinitionResource bdr ? bdr.getBeanDefinition() : null;
  }

  /**
   * Validate this bean definition.
   *
   * @throws BeanDefinitionValidationException in case of validation failure
   */
  public void validate() throws BeanDefinitionValidationException {
    if (hasMethodOverrides() && getFactoryMethodName() != null) {
      throw new BeanDefinitionValidationException(
              "Cannot combine factory method with container-generated method overrides: " +
                      "the factory method must create the concrete bean instance.");
    }
    if (hasBeanClass()) {
      prepareMethodOverrides();
    }
  }

  /**
   * Validate and prepare the method overrides defined for this bean.
   * Checks for existence of a method with the specified name.
   *
   * @throws BeanDefinitionValidationException in case of validation failure
   */
  public void prepareMethodOverrides() throws BeanDefinitionValidationException {
    // Check that lookup methods exist and determine their overloaded status.
    MethodOverrides methodOverrides = this.methodOverrides;
    if (methodOverrides != null && !methodOverrides.isEmpty()) {
      for (MethodOverride override : methodOverrides.getOverrides()) {
        prepareMethodOverride(override);
      }
    }
  }

  /**
   * Validate and prepare the given method override.
   * Checks for existence of a method with the specified name,
   * marking it as not overloaded if none found.
   *
   * @param mo the MethodOverride object to validate
   * @throws BeanDefinitionValidationException in case of validation failure
   */
  protected void prepareMethodOverride(MethodOverride mo) throws BeanDefinitionValidationException {
    int count = ReflectionUtils.getMethodCountForName(getBeanClass(), mo.getMethodName());
    if (count == 0) {
      throw new BeanDefinitionValidationException("Invalid method override: no method with name '%s' on class [%s]"
              .formatted(mo.getMethodName(), getBeanClassName()));
    }
    else if (count == 1) {
      // Mark override as not overloaded, to avoid the overhead of arg type checking.
      mo.setOverloaded(false);
    }
  }

  /**
   * Public declaration of Object's {@code clone()} method.
   * Delegates to {@link #cloneBeanDefinition()}.
   *
   * @see Object#clone()
   */
  @Override
  public AbstractBeanDefinition clone() {
    return cloneBeanDefinition();
  }

  /**
   * Clone this bean definition.
   * To be implemented by concrete subclasses.
   *
   * @return the cloned bean definition object
   */
  public abstract AbstractBeanDefinition cloneBeanDefinition();

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof AbstractBeanDefinition that)) {
      return false;
    }
    return this.primary == that.primary
            && this.role == that.role
            && this.lazyInit == that.lazyInit
            && this.synthetic == that.synthetic
            && this.abstractFlag == that.abstractFlag
            && this.autowireMode == that.autowireMode
            && this.dependencyCheck == that.dependencyCheck
            && this.enforceInitMethod == that.enforceInitMethod
            && this.autowireCandidate == that.autowireCandidate
            && this.nonPublicAccessAllowed == that.nonPublicAccessAllowed
            && this.enableDependencyInjection == that.enableDependencyInjection
            && this.lenientConstructorResolution == that.lenientConstructorResolution
            && this.enforceDestroyMethod == that.enforceDestroyMethod
            && Objects.equals(this.scope, that.scope)
            && Objects.equals(this.methodOverrides, that.methodOverrides)
            && Objects.equals(this.factoryBeanName, that.factoryBeanName)
            && Objects.equals(this.factoryMethodName, that.factoryMethodName)
            && Objects.equals(this.qualifiers, that.qualifiers)
            && Objects.equals(getBeanClassName(), that.getBeanClassName())
            && Arrays.equals(this.initMethodNames, that.initMethodNames)
            && Arrays.equals(this.destroyMethodNames, that.destroyMethodNames)
            && Arrays.equals(this.dependsOn, that.dependsOn)
            && equalsConstructorArgumentValues(that)
            && equalsPropertyValues(that)

            && super.equals(other);
  }

  private boolean equalsConstructorArgumentValues(AbstractBeanDefinition other) {
    if (!hasConstructorArgumentValues()) {
      return !other.hasConstructorArgumentValues();
    }
    return ObjectUtils.nullSafeEquals(this.constructorArgumentValues, other.constructorArgumentValues);
  }

  private boolean equalsPropertyValues(AbstractBeanDefinition other) {
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
    StringBuilder sb = new StringBuilder("class=").append(getBeanClassName());
    sb.append("; scope=").append(this.scope);
    sb.append("; abstract=").append(this.abstractFlag);
    sb.append("; lazyInit=").append(this.lazyInit);
    sb.append("; autowireMode=").append(this.autowireMode);
    sb.append("; dependencyCheck=").append(this.dependencyCheck);
    sb.append("; autowireCandidate=").append(this.autowireCandidate);
    sb.append("; primary=").append(this.primary);
    sb.append("; fallback=").append(this.fallback);
    sb.append("; factoryBeanName=").append(this.factoryBeanName);
    sb.append("; factoryMethodName=").append(this.factoryMethodName);
    sb.append("; initMethodNames=").append(Arrays.toString(this.initMethodNames));
    sb.append("; destroyMethodNames=").append(Arrays.toString(this.destroyMethodNames));
    sb.append("; enableDependencyInjection=").append(isEnableDependencyInjection());
    if (resource != null) {
      sb.append("; defined in ").append(resource);
    }
    return sb.toString();
  }

}
