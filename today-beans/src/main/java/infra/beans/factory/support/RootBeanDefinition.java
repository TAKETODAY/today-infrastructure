/*
 * Copyright 2017 - 2024 the original author or authors.
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

import java.io.Serial;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

import infra.beans.PropertyValues;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.BeanDefinitionHolder;
import infra.beans.factory.config.ConstructorArgumentValues;
import infra.core.ResolvableType;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.ClassUtils;

/**
 * A root bean definition represents the <b>merged bean definition at runtime</b>
 * that backs a specific bean in a BeanFactory. It might have been created
 * from multiple original bean definitions that inherit from each other, e.g.
 * {@link GenericBeanDefinition GenericBeanDefinitions} from XML declarations.
 * A root bean definition is essentially the 'unified' bean definition view at runtime.
 *
 * <p>Root bean definitions may also be used for <b>registering individual bean
 * definitions in the configuration phase.</b> This is particularly applicable for
 * programmatic definitions derived from factory methods (e.g. {@code @Bean} methods)
 * and instance suppliers (e.g. lambda expressions) which come with extra type metadata
 * (see {@link #setTargetType(ResolvableType)}/{@link #setResolvedFactoryMethod(Method)}).
 *
 * <p>Note: The preferred choice for bean definitions derived from declarative sources
 * (e.g. XML definitions) is the flexible {@link GenericBeanDefinition} variant.
 * GenericBeanDefinition comes with the advantage that it allows for dynamically
 * defining parent dependencies, not 'hard-coding' the role as a root bean definition,
 * even supporting parent relationship changes in the bean post-processor phase.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BeanDefinition
 * @see ChildBeanDefinition
 * @since 4.0 2022/3/7 16:41
 */
public class RootBeanDefinition extends AbstractBeanDefinition {

  @Serial
  private static final long serialVersionUID = 1L;

  @Nullable
  private BeanDefinitionHolder decoratedDefinition;

  @Nullable
  private AnnotatedElement qualifiedElement;

  /** Determines if the definition needs to be re-merged. */
  volatile boolean stale;

  boolean allowCaching = true;

  boolean isFactoryMethodUnique;

  @Nullable
  volatile ResolvableType targetType;

  /** Package-visible field for caching the determined Class of a given bean definition. */
  @Nullable
  volatile Class<?> resolvedTargetType;

  /** Package-visible field for caching if the bean is a factory bean. */
  @Nullable
  volatile Boolean isFactoryBean;

  /** Package-visible field for caching the return type of a generically typed factory method. */
  @Nullable
  volatile ResolvableType factoryMethodReturnType;

  /** Package-visible field for caching a unique factory method candidate for introspection. */
  @Nullable
  volatile Method factoryMethodToIntrospect;

  /** Package-visible field for caching a resolved destroy method name (also for inferred). */
  @Nullable
  volatile String resolvedDestroyMethodName;

  /** Common lock for the four constructor fields below. */
  final Object constructorArgumentLock = new Object();

  /** Package-visible field for caching the resolved constructor or factory method. */
  @Nullable
  Executable resolvedConstructorOrFactoryMethod;

  /** Package-visible field that marks the constructor arguments as resolved. */
  boolean constructorArgumentsResolved = false;

  /** Package-visible field for caching fully resolved constructor arguments. */
  @Nullable
  Object[] resolvedConstructorArguments;

  /** Package-visible field for caching partly prepared constructor arguments. */
  @Nullable
  Object[] preparedConstructorArguments;

  /** Common lock for the two post-processing fields below. */
  final Object postProcessingLock = new Object();

  /** Package-visible field that indicates MergedBeanDefinitionPostProcessor having been applied. */
  boolean postProcessed = false;

  Method[] initMethodArray;

  /** Package-visible field that indicates a before-instantiation post-processor having kicked in. */
  @Nullable
  volatile Boolean beforeInstantiationResolved;

  @Nullable
  private Set<Member> externallyManagedConfigMembers;

  @Nullable
  private Set<String> externallyManagedInitMethods;

  @Nullable
  private Set<String> externallyManagedDestroyMethods;

  /**
   * Create a new RootBeanDefinition, to be configured through its bean
   * properties and configuration methods.
   *
   * @see #setBeanClass
   * @see #setScope
   * @see #setConstructorArgumentValues
   * @see #setPropertyValues
   */
  public RootBeanDefinition() {
    super();
  }

  /**
   * Create a new RootBeanDefinition for a singleton.
   *
   * @param beanClass the class of the bean to instantiate
   * @see #setBeanClass
   */
  public RootBeanDefinition(@Nullable Class<?> beanClass) {
    super();
    setBeanClass(beanClass);
  }

  /**
   * Create a new RootBeanDefinition for a singleton bean, constructing each instance
   * through calling the given supplier (possibly a lambda or method reference).
   *
   * @param beanClass the class of the bean to instantiate
   * @param instanceSupplier the supplier to construct a bean instance,
   * as an alternative to a declaratively specified factory method
   * @see #setInstanceSupplier
   */
  public <T> RootBeanDefinition(@Nullable Class<T> beanClass, @Nullable Supplier<T> instanceSupplier) {
    super();
    setBeanClass(beanClass);
    setInstanceSupplier(instanceSupplier);
  }

  /**
   * Create a new RootBeanDefinition for a scoped bean, constructing each instance
   * through calling the given supplier (possibly a lambda or method reference).
   *
   * @param beanClass the class of the bean to instantiate
   * @param scope the name of the corresponding scope
   * @param instanceSupplier the supplier to construct a bean instance,
   * as an alternative to a declaratively specified factory method
   * @see #setInstanceSupplier
   */
  public <T> RootBeanDefinition(@Nullable Class<T> beanClass, String scope, @Nullable Supplier<T> instanceSupplier) {
    super();
    setBeanClass(beanClass);
    setScope(scope);
    setInstanceSupplier(instanceSupplier);
  }

  /**
   * Create a new RootBeanDefinition for a singleton,
   * using the given autowire mode.
   *
   * @param beanClass the class of the bean to instantiate
   * @param autowireMode by name or type, using the constants in this interface
   * @param dependencyCheck whether to perform a dependency check for objects
   * (not applicable to autowiring a constructor, thus ignored there)
   */
  public RootBeanDefinition(@Nullable Class<?> beanClass, int autowireMode, boolean dependencyCheck) {
    super();
    setBeanClass(beanClass);
    setAutowireMode(autowireMode);
    if (dependencyCheck && getResolvedAutowireMode() != AUTOWIRE_CONSTRUCTOR) {
      setDependencyCheck(DEPENDENCY_CHECK_OBJECTS);
    }
  }

  /**
   * Create a new RootBeanDefinition for a singleton,
   * providing constructor arguments and property values.
   *
   * @param beanClass the class of the bean to instantiate
   * @param cargs the constructor argument values to apply
   * @param pvs the property values to apply
   */
  public RootBeanDefinition(@Nullable Class<?> beanClass, @Nullable ConstructorArgumentValues cargs,
          @Nullable PropertyValues pvs) {
    super(cargs, pvs);
    setBeanClass(beanClass);
  }

  /**
   * Create a new RootBeanDefinition for a singleton,
   * providing constructor arguments and property values.
   * <p>Takes a bean class name to avoid eager loading of the bean class.
   *
   * @param beanClassName the name of the class to instantiate
   */
  public RootBeanDefinition(String beanClassName) {
    setBeanClassName(beanClassName);
  }

  /**
   * Create a new RootBeanDefinition for a singleton,
   * providing constructor arguments and property values.
   * <p>Takes a bean class name to avoid eager loading of the bean class.
   *
   * @param beanClassName the name of the class to instantiate
   * @param cargs the constructor argument values to apply
   * @param pvs the property values to apply
   */
  public RootBeanDefinition(String beanClassName, ConstructorArgumentValues cargs, PropertyValues pvs) {
    super(cargs, pvs);
    setBeanClassName(beanClassName);
  }

  /**
   * Create a new RootBeanDefinition as deep copy of the given
   * bean definition.
   *
   * @param original the original bean definition to copy from
   */
  public RootBeanDefinition(RootBeanDefinition original) {
    super(original);
    this.decoratedDefinition = original.decoratedDefinition;
    this.qualifiedElement = original.qualifiedElement;
    this.allowCaching = original.allowCaching;
    this.isFactoryMethodUnique = original.isFactoryMethodUnique;
    this.targetType = original.targetType;
    this.factoryMethodToIntrospect = original.factoryMethodToIntrospect;
  }

  /**
   * Create a new RootBeanDefinition as deep copy of the given
   * bean definition.
   *
   * @param original the original bean definition to copy from
   */
  RootBeanDefinition(BeanDefinition original) {
    super(original);
  }

  @Override
  public String getParentName() {
    return null;
  }

  @Override
  public void setParentName(@Nullable String parentName) {
    if (parentName != null) {
      throw new IllegalArgumentException("Root bean cannot be changed into a child bean with parent reference");
    }
  }

  /**
   * Register a target definition that is being decorated by this bean definition.
   */
  public void setDecoratedDefinition(@Nullable BeanDefinitionHolder decoratedDefinition) {
    this.decoratedDefinition = decoratedDefinition;
  }

  /**
   * Return the target definition that is being decorated by this bean definition, if any.
   */
  @Nullable
  public BeanDefinitionHolder getDecoratedDefinition() {
    return this.decoratedDefinition;
  }

  /**
   * Specify the {@link AnnotatedElement} defining qualifiers,
   * to be used instead of the target class or factory method.
   *
   * @see #setTargetType(ResolvableType)
   * @see #getResolvedFactoryMethod()
   */
  public void setQualifiedElement(@Nullable AnnotatedElement qualifiedElement) {
    this.qualifiedElement = qualifiedElement;
  }

  /**
   * Return the {@link AnnotatedElement} defining qualifiers, if any.
   * Otherwise, the factory method and target class will be checked.
   */
  @Nullable
  public AnnotatedElement getQualifiedElement() {
    return this.qualifiedElement;
  }

  /**
   * Specify a generics-containing target type of this bean definition, if known in advance.
   */
  public void setTargetType(@Nullable ResolvableType targetType) {
    this.targetType = targetType;
  }

  /**
   * Specify the target type of this bean definition, if known in advance.
   */
  public void setTargetType(@Nullable Class<?> targetType) {
    this.targetType = (targetType != null ? ResolvableType.forClass(targetType) : null);
  }

  /**
   * Return the target type of this bean definition, if known
   * (either specified in advance or resolved on first instantiation).
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
   * Return a {@link ResolvableType} for this bean definition,
   * either from runtime-cached type information or from configuration-time
   * {@link #setTargetType(ResolvableType)} or {@link #setBeanClass(Class)},
   * also considering resolved factory method definitions.
   *
   * @see #setTargetType(ResolvableType)
   * @see #setBeanClass(Class)
   * @see #setResolvedFactoryMethod(Method)
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
    Method factoryMethod = getResolvedFactoryMethod();
    if (factoryMethod != null) {
      return ResolvableType.forReturnType(factoryMethod);
    }
    return super.getResolvableType();
  }

  /**
   * Determine preferred constructors to use for default construction, if any.
   * Constructor arguments will be autowired if necessary.
   * <p>the default implementation of this method takes the
   * {@link #PREFERRED_CONSTRUCTORS_ATTRIBUTE} attribute into account.
   * Subclasses are encouraged to preserve this through a {@code super} call,
   * either before or after their own preferred constructor determination.
   *
   * @return one or more preferred constructors, or {@code null} if none
   * (in which case the regular no-arg default constructor will be called)
   */
  @Nullable
  public Constructor<?>[] getPreferredConstructors() {
    Object attribute = getAttribute(PREFERRED_CONSTRUCTORS_ATTRIBUTE);
    if (attribute == null) {
      return null;
    }
    if (attribute instanceof Constructor<?> constructor) {
      return new Constructor<?>[] { constructor };
    }
    if (attribute instanceof Constructor<?>[]) {
      return (Constructor<?>[]) attribute;
    }
    throw new IllegalArgumentException("Invalid value type for attribute '" +
            PREFERRED_CONSTRUCTORS_ATTRIBUTE + "': " + attribute.getClass().getName());
  }

  /**
   * Specify a factory method name that refers to a non-overloaded method.
   */
  public void setUniqueFactoryMethodName(String name) {
    Assert.hasText(name, "Factory method name must not be empty");
    setFactoryMethodName(name);
    this.isFactoryMethodUnique = true;
  }

  /**
   * Specify a factory method name that refers to an overloaded method.
   */
  public void setNonUniqueFactoryMethodName(String name) {
    Assert.hasText(name, "Factory method name must not be empty");
    setFactoryMethodName(name);
    this.isFactoryMethodUnique = false;
  }

  /**
   * Check whether the given candidate qualifies as a factory method.
   */
  public boolean isFactoryMethod(Method candidate) {
    return candidate.getName().equals(getFactoryMethodName());
  }

  /**
   * Set a resolved Java Method for the factory method on this bean definition.
   *
   * @param method the resolved factory method, or {@code null} to reset it
   */
  public void setResolvedFactoryMethod(@Nullable Method method) {
    this.factoryMethodToIntrospect = method;
    if (method != null) {
      setUniqueFactoryMethodName(method.getName());
    }
  }

  /**
   * Return the resolved factory method as a Java Method object, if available.
   *
   * @return the factory method, or {@code null} if not found or not resolved yet
   */
  @Nullable
  public Method getResolvedFactoryMethod() {
    Method factoryMethod = this.factoryMethodToIntrospect;
    if (factoryMethod == null
            && getInstanceSupplier() instanceof InstanceSupplier<?> is) {
      factoryMethod = is.getFactoryMethod();
    }
    return factoryMethod;
  }

  /**
   * Mark this bean definition as post-processed,
   * i.e. processed by {@link MergedBeanDefinitionPostProcessor}.
   */
  public void markAsPostProcessed() {
    synchronized(this.postProcessingLock) {
      this.postProcessed = true;
    }
  }

  /**
   * Register an externally managed configuration method or field.
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
   * Determine if the given method or field is an externally managed configuration member.
   */
  public boolean isExternallyManagedConfigMember(Member configMember) {
    synchronized(this.postProcessingLock) {
      return (this.externallyManagedConfigMembers != null &&
              this.externallyManagedConfigMembers.contains(configMember));
    }
  }

  /**
   * Get all externally managed configuration methods and fields (as an immutable Set).
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
   * for example, a method annotated with JSR-250's {@code javax.annotation.PostConstruct}
   * or Jakarta's {@link jakarta.annotation.PostConstruct} annotation.
   * <p>The supplied {@code initMethod} may be a
   * {@linkplain Method#getName() simple method name} or a
   * {@linkplain ClassUtils#getQualifiedMethodName(Method)
   * qualified method name} for package-private and {@code private} methods.
   * A qualified name is necessary for package-private and {@code private} methods
   * in order to disambiguate between multiple such methods with the same name
   * within a type hierarchy.
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
   */
  boolean hasAnyExternallyManagedInitMethod(String initMethod) {
    synchronized(postProcessingLock) {
      if (isExternallyManagedInitMethod(initMethod)) {
        return true;
      }
      return hasAnyExternallyManagedMethod(externallyManagedInitMethods, initMethod);
    }
  }

  /**
   * Return all externally managed initialization methods (as an immutable Set).
   * <p>See {@link #registerExternallyManagedInitMethod} for details
   * regarding the format for the initialization methods in the returned set.
   */
  public Set<String> getExternallyManagedInitMethods() {
    synchronized(this.postProcessingLock) {
      return (this.externallyManagedInitMethods != null ?
              Collections.unmodifiableSet(new LinkedHashSet<>(this.externallyManagedInitMethods)) :
              Collections.emptySet());
    }
  }

  /**
   * Resolve the inferred destroy method if necessary.
   */
  public void resolveDestroyMethodIfNecessary() {
    setDestroyMethodNames(DisposableBeanAdapter
            .inferDestroyMethodsIfNecessary(getResolvableType().toClass(), this));
  }

  /**
   * Register an externally managed configuration destruction method &mdash;
   * for example, a method annotated with JSR-250's
   * {@link jakarta.annotation.PreDestroy} annotation.
   * <p>The supplied {@code destroyMethod} may be the
   * {@linkplain Method#getName() simple method name} for non-private methods or the
   * {@linkplain ClassUtils#getQualifiedMethodName(Method)
   * qualified method name} for {@code private} methods. A qualified name is
   * necessary for {@code private} methods in order to disambiguate between
   * multiple private methods with the same name within a class hierarchy.
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
   */
  boolean hasAnyExternallyManagedDestroyMethod(String destroyMethod) {
    synchronized(postProcessingLock) {
      if (isExternallyManagedDestroyMethod(destroyMethod)) {
        return true;
      }
      return hasAnyExternallyManagedMethod(externallyManagedDestroyMethods, destroyMethod);
    }
  }

  private static boolean hasAnyExternallyManagedMethod(@Nullable Set<String> candidates, String methodName) {
    if (candidates != null) {
      for (String candidate : candidates) {
        int indexOfDot = candidate.lastIndexOf('.');
        if (indexOfDot > 0) {
          String candidateMethodName = candidate.substring(indexOfDot + 1);
          if (candidateMethodName.equals(methodName)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * Get all externally managed destruction methods (as an immutable Set).
   * <p>See {@link #registerExternallyManagedDestroyMethod} for details
   * regarding the format for the destruction methods in the returned set.
   */
  public Set<String> getExternallyManagedDestroyMethods() {
    synchronized(this.postProcessingLock) {
      return (this.externallyManagedDestroyMethods != null ?
              Collections.unmodifiableSet(new LinkedHashSet<>(this.externallyManagedDestroyMethods)) :
              Collections.emptySet());
    }
  }

  @Override
  public RootBeanDefinition cloneBeanDefinition() {
    return new RootBeanDefinition(this);
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return (this == other || (other instanceof RootBeanDefinition && super.equals(other)));
  }

  @Override
  public String toString() {
    return "Root bean: " + super.toString();
  }

}
