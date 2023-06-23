/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

import java.io.Serial;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import cn.taketoday.beans.BeanMetadata;
import cn.taketoday.beans.BeanProperty;
import cn.taketoday.beans.BeanUtils;
import cn.taketoday.beans.BeanWrapper;
import cn.taketoday.beans.BeanWrapperImpl;
import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.PropertyAccessorUtils;
import cn.taketoday.beans.PropertyValue;
import cn.taketoday.beans.PropertyValues;
import cn.taketoday.beans.TypeConverter;
import cn.taketoday.beans.factory.Aware;
import cn.taketoday.beans.factory.BeanClassLoaderAware;
import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.beans.factory.BeanCurrentlyInCreationException;
import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.BeanDefinitionValidationException;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.BeanInitializationException;
import cn.taketoday.beans.factory.BeanNameAware;
import cn.taketoday.beans.factory.DependenciesBeanPostProcessor;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.InitializationBeanPostProcessor;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.beans.factory.InjectionPoint;
import cn.taketoday.beans.factory.SmartFactoryBean;
import cn.taketoday.beans.factory.SmartInitializingSingleton;
import cn.taketoday.beans.factory.UnsatisfiedDependencyException;
import cn.taketoday.beans.factory.config.AutowireCapableBeanFactory;
import cn.taketoday.beans.factory.config.AutowiredPropertyMarker;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.config.ConstructorArgumentValues;
import cn.taketoday.beans.factory.config.DependencyDescriptor;
import cn.taketoday.beans.factory.config.InstantiationAwareBeanPostProcessor;
import cn.taketoday.beans.factory.config.PropertyValueRetriever;
import cn.taketoday.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import cn.taketoday.beans.factory.config.TypedStringValue;
import cn.taketoday.core.DefaultParameterNameDiscoverer;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.NamedThreadLocal;
import cn.taketoday.core.ParameterNameDiscoverer;
import cn.taketoday.core.PriorityOrdered;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.lang.NullValue;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.util.function.ThrowingSupplier;

/**
 * Abstract bean factory superclass that implements default bean creation,
 * with the full capabilities specified by the {@link BeanDefinition} class.
 * Implements the {@link AutowireCapableBeanFactory} interface in addition
 * to AbstractBeanFactory's {@link #createBean} method.
 *
 * <p>Provides bean creation (with constructor resolution), property population,
 * wiring (including autowiring), and initialization. Handles runtime bean
 * references, resolves managed collections, calls initialization methods, etc.
 * Supports autowiring constructors, properties by name, and properties by type.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Mark Fisher
 * @author Costin Leau
 * @author Chris Beams
 * @author Sam Brannen
 * @author Phillip Webb
 * @author TODAY 2021/10/1 23:06
 * @see BeanDefinition
 * @see BeanDefinitionRegistry
 * @since 4.0
 */
public abstract class AbstractAutowireCapableBeanFactory
        extends AbstractBeanFactory implements AutowireCapableBeanFactory {

  /** Whether to automatically try to resolve circular references between beans. @since 4.0 */
  private boolean allowCircularReferences = true;

  /**
   * Whether to resort to injecting a raw bean instance in case of circular reference,
   * even if the injected bean eventually got wrapped.
   *
   * @since 4.0
   */
  private boolean allowRawInjectionDespiteWrapping = false;

  /** Cache of unfinished FactoryBean instances: FactoryBean name to its instance. @since 4.0 */
  private final ConcurrentHashMap<String, BeanWrapper> factoryBeanInstanceCache = new ConcurrentHashMap<>();

  /**
   * Dependency types to ignore on dependency check and autowire, as Set of
   * Class objects: for example, String. Default is none.
   *
   * @since 4.0
   */
  private final HashSet<Class<?>> ignoredDependencyTypes = new HashSet<>();

  /**
   * Dependency interfaces to ignore on dependency check and autowire, as Set of
   * Class objects. By default, only the BeanFactory interface is ignored.
   *
   * @since 4.0
   */
  private final HashSet<Class<?>> ignoredDependencyInterfaces = new HashSet<>();

  /** Resolver strategy for method parameter names. @since 4.0 */
  @Nullable
  private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

  /** Strategy for creating bean instances. */
  private InstantiationStrategy instantiationStrategy;

  /** Cache of filtered BeanProperty: bean Class to PropertyDescriptor array. */
  private final ConcurrentMap<Class<?>, BeanProperty[]> filteredBeanPropertiesCache =
          new ConcurrentHashMap<>();

  /**
   * The name of the currently created bean, for implicit dependency registration
   * on getBean etc invocations triggered from a user-specified Supplier callback.
   */
  private final NamedThreadLocal<String> currentlyCreatedBean = new NamedThreadLocal<>("Currently created bean");

  /**
   * Create a new AbstractAutowireCapableBeanFactory.
   */
  public AbstractAutowireCapableBeanFactory() {
    ignoreDependencyInterface(BeanNameAware.class);
    ignoreDependencyInterface(BeanFactoryAware.class);
    ignoreDependencyInterface(BeanClassLoaderAware.class);
    this.instantiationStrategy = new CglibSubclassingInstantiationStrategy();
  }

  /**
   * Create a new AbstractAutowireCapableBeanFactory with the given parent.
   *
   * @param parentBeanFactory parent bean factory, or {@code null} if none
   */
  public AbstractAutowireCapableBeanFactory(@Nullable BeanFactory parentBeanFactory) {
    this();
    setParentBeanFactory(parentBeanFactory);
  }

  //---------------------------------------------------------------------
  // Implementation of AutowireCapableBeanFactory interface
  //---------------------------------------------------------------------

  @Override
  public <T> T createBean(Class<T> beanClass, int autowireMode) throws BeansException {
    return createBean(beanClass, autowireMode, false);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T createBean(Class<T> beanClass, int autowireMode, boolean dependencyCheck) throws BeansException {
    // Use non-singleton bean merged, to avoid registering bean as dependent bean.
    RootBeanDefinition bd = new RootBeanDefinition(beanClass, autowireMode, dependencyCheck);
    bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
    return (T) createBean(beanClass.getName(), bd, null);
  }

  @Override
  public void autowireBean(Object existingBean) {
    // Use non-singleton bean merged, to avoid registering bean as dependent bean.
    RootBeanDefinition merged = new RootBeanDefinition(ClassUtils.getUserClass(existingBean));
    merged.setScope(BeanDefinition.SCOPE_PROTOTYPE);
    merged.allowCaching = ClassUtils.isCacheSafe(merged.getBeanClass(), getBeanClassLoader());
    BeanWrapper bw = createBeanWrapper(merged, existingBean);

    populateBean(merged.getBeanClass().getName(), merged, bw);
  }

  //---------------------------------------------------------------------
  // Implementation of relevant AbstractBeanFactory template methods
  //---------------------------------------------------------------------

  /**
   * Central method of this class: creates a bean instance,
   * populates the bean instance, applies post-processors, etc.
   *
   * @see #doCreateBean
   */
  @Nullable
  protected Object createBean(
          String beanName, RootBeanDefinition merged, @Nullable Object[] args) throws BeanCreationException {
    if (log.isDebugEnabled()) {
      log.trace("Creating instance of bean '{}'", beanName);
    }
    RootBeanDefinition mbdToUse = merged;

    // Make sure bean class is actually resolved at this point, and
    // clone the bean merged in case of a dynamically resolved Class
    // which cannot be stored in the shared merged bean merged.
    Class<?> resolvedClass = resolveBeanClass(beanName, merged);
    if (resolvedClass != null && !merged.hasBeanClass() && merged.getBeanClassName() != null) {
      mbdToUse = new RootBeanDefinition(merged);
      mbdToUse.setBeanClass(resolvedClass);
    }

    // Prepare method overrides.
    try {
      mbdToUse.prepareMethodOverrides();
    }
    catch (BeanDefinitionValidationException ex) {
      throw new BeanDefinitionStoreException(mbdToUse.getResourceDescription(),
              beanName, "Validation of method overrides failed", ex);
    }

    try {
      // Give BeanPostProcessors a chance to return a proxy instead of the target bean instance.
      Object bean = resolveBeforeInstantiation(beanName, mbdToUse);
      if (bean != null) {
        return bean;
      }
    }
    catch (Throwable ex) {
      throw new BeanCreationException(
              mbdToUse.getResourceDescription(), beanName,
              "BeanPostProcessor before instantiation of bean failed", ex);
    }

    try {
      Object beanInstance = doCreateBean(beanName, mbdToUse, args);
      if (log.isDebugEnabled()) {
        log.trace("Finished creating instance of bean '{}'", beanName);
      }
      return beanInstance;
    }
    catch (BeanCreationException | ImplicitlyAppearedSingletonException ex) {
      // A previously detected exception with proper bean creation context already,
      // or illegal singleton state to be communicated up to DefaultSingletonBeanRegistry.
      throw ex;
    }
    catch (Throwable ex) {
      throw new BeanCreationException(
              mbdToUse.getResourceDescription(),
              beanName, "Unexpected exception during bean creation", ex);
    }
  }

  /**
   * Actually create the specified bean. Pre-creation processing has already happened
   * at this point, e.g. checking {@code postProcessBeforeInstantiation} callbacks.
   * <p>Differentiates between default bean instantiation, use of a
   * factory method, and autowiring a constructor.
   *
   * @param merged the merged bean merged for the bean
   * @param args explicit arguments to use for constructor or factory method invocation
   * @return a new instance of the bean
   * @throws BeanCreationException if the bean could not be created
   */
  @Nullable
  protected Object doCreateBean(
          String beanName, RootBeanDefinition merged, @Nullable Object[] args) throws BeanCreationException {
    // Instantiate the bean.
    BeanWrapper instanceWrapper = null;
    if (merged.isSingleton()) {
      instanceWrapper = factoryBeanInstanceCache.remove(beanName);
    }

    if (instanceWrapper == null) {
      instanceWrapper = createBeanInstance(beanName, merged, args);
    }

    Object bean = instanceWrapper.getWrappedInstance();

    if (bean == NullValue.INSTANCE) {
      return null;
    }

    merged.resolvedTargetType = instanceWrapper.getWrappedClass();

    // Allow post-processors to modify the merged bean merged.
    synchronized(merged.postProcessingLock) {
      if (!merged.postProcessed) {
        try {
          applyBeanDefinitionPostProcessors(merged, bean, beanName);
        }
        catch (Throwable ex) {
          throw new BeanCreationException(merged.getResourceDescription(), beanName,
                  "Post-processing of bean merged failed", ex);
        }
        merged.postProcessed = true;
      }
    }

    // Eagerly cache singletons to be able to resolve circular references
    // even when triggered by lifecycle interfaces like BeanFactoryAware.
    boolean earlySingletonExposure = isEarlySingletonExposure(merged, beanName);
    if (earlySingletonExposure) {
      if (log.isDebugEnabled()) {
        log.trace("Eagerly caching bean '{}' to allow for resolving potential circular references", beanName);
      }
      addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, merged, bean));
    }

    // Initialize the bean instance.
    Object fullyInitializedBean;
    try {
      // apply properties
      populateBean(beanName, merged, instanceWrapper);
      // Initialize the bean instance.
      fullyInitializedBean = initializeBean(bean, beanName, merged);
    }
    catch (Throwable ex) {
      if (ex instanceof BeanCreationException && beanName.equals(((BeanCreationException) ex).getBeanName())) {
        throw (BeanCreationException) ex;
      }
      else {
        throw new BeanCreationException(merged.getResourceDescription(), beanName, ex.getMessage(), ex);
      }
    }

    if (earlySingletonExposure) {
      Object earlySingletonReference = getSingleton(beanName, false);
      if (earlySingletonReference != null) {
        if (fullyInitializedBean == bean) {
          fullyInitializedBean = earlySingletonReference;
        }
        else if (!this.allowRawInjectionDespiteWrapping && hasDependentBean(beanName)) {
          String[] dependentBeans = getDependentBeans(beanName);
          LinkedHashSet<String> actualDependentBeans = new LinkedHashSet<>(dependentBeans.length);
          for (String dependentBean : dependentBeans) {
            if (!removeSingletonIfCreatedForTypeCheckOnly(dependentBean)) {
              actualDependentBeans.add(dependentBean);
            }
          }
          if (!actualDependentBeans.isEmpty()) {
            throw new BeanCurrentlyInCreationException(beanName,
                    "Bean with name '" + beanName + "' has been injected into other beans [" +
                            StringUtils.collectionToCommaDelimitedString(actualDependentBeans) +
                            "] in its raw version as part of a circular reference, but has eventually been " +
                            "wrapped. This means that said other beans do not use the final version of the " +
                            "bean. This is often the result of over-eager type matching - consider using " +
                            "'getBeanNamesForType' with the 'allowEagerInit' flag turned off, for example.");
          }
        }
      }
    }

    // Register bean as disposable.
    try {
      registerDisposableBeanIfNecessary(beanName, bean, merged);
    }
    catch (BeanDefinitionValidationException ex) {
      throw new BeanCreationException(
              merged.getResourceDescription(), beanName, "Invalid destruction signature", ex);
    }
    return fullyInitializedBean;
  }

  protected BeanWrapperImpl createBeanWrapper(BeanDefinition merged, Object bean) {
    BeanMetadata metadata;
    if (merged.isSingleton()) {
      metadata = new BeanMetadata(bean.getClass());
    }
    else {
      // fast access from cache
      metadata = BeanMetadata.from(bean);
    }

    BeanWrapperImpl beanWrapper = new BeanWrapperImpl(bean, metadata);
    initBeanWrapper(beanWrapper);
    return beanWrapper;
  }

  private boolean isEarlySingletonExposure(BeanDefinition definition, String beanName) {
    return definition.isSingleton() && this.allowCircularReferences && isSingletonCurrentlyInCreation(beanName);
  }

  /**
   * Obtain a reference for early access to the specified bean,
   * typically for the purpose of resolving a circular reference.
   *
   * @param beanName the name of the bean (for error handling purposes)
   * @param merged the merged bean merged for the bean
   * @param bean the raw bean instance
   * @return the object to expose as bean reference
   */
  protected Object getEarlyBeanReference(String beanName, RootBeanDefinition merged, Object bean) {
    Object exposedObject = bean;
    if (!merged.isSynthetic()) {
      for (SmartInstantiationAwareBeanPostProcessor bp : postProcessors().smartInstantiation) {
        exposedObject = bp.getEarlyBeanReference(exposedObject, beanName);
      }
    }
    return exposedObject;
  }

  @Override
  public Object initializeBean(Object existingBean) throws BeansException {
    return initializeBean(existingBean, BeanDefinitionBuilder.defaultBeanName(existingBean.getClass()));
  }

  @Override
  public Object initializeBean(Object existingBean, String beanName) {
    return initializeBean(existingBean, beanName, null);
  }

  /**
   * Fully initialize the given raw bean, applying factory callbacks such as
   * {@code setBeanName} and {@code setBeanFactory}, also applying all bean post
   * processors (including ones which might wrap the given raw bean).
   * <p>
   * Note that no bean merged of the given name has to exist in the bean
   * factory. The passed-in bean name will simply be used for callbacks but not
   * checked against the registered bean definitions.
   *
   * @param bean the new bean instance we may need to initialize
   * @param def the bean def of the bean
   * @return the bean instance to use, either the original or a wrapped one
   * @throws BeanInitializationException if the initialization failed
   * @see BeanNameAware
   * @see BeanClassLoaderAware
   * @see BeanFactoryAware
   * @see #applyBeanPostProcessorsBeforeInitialization
   * @see #invokeInitMethods
   * @see #applyBeanPostProcessorsAfterInitialization
   */
  public Object initializeBean(Object bean, String beanName, @Nullable RootBeanDefinition def) throws BeansException {
    invokeAwareMethods(bean, beanName);

    if (def == null || !def.isSynthetic()) {
      bean = applyBeanPostProcessorsBeforeInitialization(bean, beanName);
    }

    try {
      invokeInitMethods(beanName, bean, def);
    }
    catch (Throwable ex) {
      throw new BeanCreationException(
              def != null ? def.getResourceDescription() : null, beanName, ex.getMessage(), ex);
    }
    if (def == null || !def.isSynthetic()) {
      bean = applyBeanPostProcessorsAfterInitialization(bean, beanName);
    }

    return bean;
  }

  private void invokeAwareMethods(Object bean, String beanName) {
    if (bean instanceof Aware) {
      if (bean instanceof BeanNameAware) {
        ((BeanNameAware) bean).setBeanName(beanName);
      }
      if (bean instanceof BeanClassLoaderAware) {
        ClassLoader bcl = getBeanClassLoader();
        if (bcl != null) {
          ((BeanClassLoaderAware) bean).setBeanClassLoader(bcl);
        }
      }
      if (bean instanceof BeanFactoryAware) {
        ((BeanFactoryAware) bean).setBeanFactory(this);
      }
    }
  }

  /**
   * Give a bean a chance to initialize itself after all its properties are set,
   * and a chance to know about its owning bean factory (this object).
   * <p>This means checking whether the bean implements {@link InitializingBean}
   * or defines any custom init methods, and invoking the necessary callback(s)
   * if it does.
   *
   * @param beanName the bean name in the factory (for debugging purposes)
   * @param bean the new bean instance we may need to initialize
   * @param def the merged bean definition that the bean was created with
   * (can also be {@code null}, if given an existing bean instance)
   * @throws Throwable if thrown by init methods or by the invocation process
   * @see cn.taketoday.stereotype.Component
   * @see InitializingBean
   * @see jakarta.annotation.PostConstruct
   */
  protected void invokeInitMethods(String beanName, Object bean, @Nullable RootBeanDefinition def) throws Throwable {

    boolean isInitializingBean = (bean instanceof InitializingBean);
    if (isInitializingBean && (def == null || !def.hasAnyExternallyManagedInitMethod("afterPropertiesSet"))) {
      if (log.isTraceEnabled()) {
        log.trace("Invoking afterPropertiesSet() on bean with name '{}'", beanName);
      }
      ((InitializingBean) bean).afterPropertiesSet();
    }

    if (def != null) {
      Method[] methods = initMethodArray(beanName, isInitializingBean, bean, def);
      if (ObjectUtils.isNotEmpty(methods)) {
        DependencyInjector injector = getInjector();
        // invoke or initMethods defined in @Component
        for (Method method : methods) {
          if (log.isTraceEnabled()) {
            log.trace("Invoking init method '{}' on bean with name '{}'", method.getName(), beanName);
          }
          ReflectionUtils.makeAccessible(method);
          injector.inject(method, bean);
        }
      }
    }
  }

  private Method[] initMethodArray(String beanName, boolean isInitializingBean, Object bean, RootBeanDefinition def) {
    Method[] initMethodArray = def.initMethodArray;
    if (def.initMethodArray == null) {
      String[] initMethodNames = def.getInitMethodNames();
      if (ObjectUtils.isNotEmpty(initMethodNames)) {
        ArrayList<Method> methods = new ArrayList<>(2);
        for (String initMethodName : initMethodNames) {
          if (StringUtils.isNotEmpty(initMethodName)
                  && !(isInitializingBean && "afterPropertiesSet".equals(initMethodName))
                  && !def.hasAnyExternallyManagedInitMethod(initMethodName)) {

            Method initMethod = def.isNonPublicAccessAllowed() ?
                                BeanUtils.findMethod(bean.getClass(), initMethodName) :
                                ReflectionUtils.getMethodIfAvailable(bean.getClass(), initMethodName);

            if (initMethod == null) {
              if (def.isEnforceInitMethod()) {
                throw new BeanDefinitionValidationException("Could not find an init method named '" +
                        initMethodName + "' on bean with name '" + beanName + "'");
              }
              else {
                if (log.isTraceEnabled()) {
                  log.trace("No default init method named '{}' found on bean with name '{}'",
                          initMethodName, beanName);
                }
                // Ignore non-existent default lifecycle methods.
                continue;
              }
            }

            Method methodToInvoke = ReflectionUtils.getInterfaceMethodIfPossible(initMethod, bean.getClass());
            methods.add(methodToInvoke);
          }
        }

        AnnotationAwareOrderComparator.sort(methods);
        initMethodArray = methods.toArray(BeanDefinition.EMPTY_METHOD);
      }
      else {
        initMethodArray = BeanDefinition.EMPTY_METHOD;
      }

      def.initMethodArray = initMethodArray;
    }

    return initMethodArray;
  }

  @Override
  public Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName) throws BeansException {
    Object result = existingBean;
    for (InitializationBeanPostProcessor processor : postProcessors().initialization) {
      Object current = processor.postProcessBeforeInitialization(result, beanName);
      if (current == null) {
        return result;
      }
      result = current;
    }
    return result;
  }

  @Override
  public Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName) throws BeansException {
    Object result = existingBean;
    for (InitializationBeanPostProcessor processor : postProcessors().initialization) {
      Object current = processor.postProcessAfterInitialization(result, beanName);
      if (current == null) {
        return result;
      }
      result = current;
    }
    return result;
  }

  /**
   * Apply before-instantiation post-processors, resolving whether there is a
   * before-instantiation shortcut for the specified bean.
   *
   * @param beanName the name of the bean
   * @param definition the bean merged for the bean
   * @return the shortcut-determined bean instance, or {@code null} if none
   */
  @Nullable
  protected Object resolveBeforeInstantiation(String beanName, RootBeanDefinition definition) {
    Object bean = null;
    if (!Boolean.FALSE.equals(definition.beforeInstantiationResolved)) {
      // Make sure bean class is actually resolved at this point.
      if (!definition.isSynthetic()) {
        Class<?> targetType = determineTargetType(beanName, definition);
        if (targetType != null) {
          bean = applyBeanPostProcessorsBeforeInstantiation(targetType, beanName);
          if (bean != null) {
            bean = applyBeanPostProcessorsAfterInitialization(bean, beanName);
          }
        }
      }
      definition.beforeInstantiationResolved = (bean != null);
    }
    return bean;
  }

  /**
   * Apply InstantiationAwareBeanPostProcessors to the specified bean merged
   * (by class and name), invoking their {@code postProcessBeforeInstantiation} methods.
   * <p>Any returned object will be used as the bean instead of actually instantiating
   * the target bean. A {@code null} return value from the post-processor will
   * result in the target bean being instantiated.
   *
   * @param beanClass the class of the bean to be instantiated
   * @param beanName the name of the bean
   * @return the bean object to use instead of a default instance of the target bean, or {@code null}
   * @see InstantiationAwareBeanPostProcessor#postProcessBeforeInstantiation
   */
  @Nullable
  protected Object applyBeanPostProcessorsBeforeInstantiation(Class<?> beanClass, String beanName) {
    for (InstantiationAwareBeanPostProcessor processor : postProcessors().instantiation) {
      Object result = processor.postProcessBeforeInstantiation(beanClass, beanName);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  /**
   * Apply BeanDefinitionPostProcessors to the specified bean merged,
   * invoking their {@code postProcessBeanDefinition} methods.
   *
   * @param mbd the merged bean merged for the bean
   * @param bean the actual bean instance
   * @param beanName the name of the bean
   */
  protected void applyBeanDefinitionPostProcessors(RootBeanDefinition mbd, Object bean, String beanName) {
    for (MergedBeanDefinitionPostProcessor processor : postProcessors().definitions) {
      processor.postProcessMergedBeanDefinition(mbd, bean, beanName);
    }
  }

  /**
   * Create a new instance for the specified bean, using an appropriate instantiation strategy:
   * factory method, constructor autowiring, or simple instantiation.
   *
   * @param beanName the name of the bean
   * @param merged the bean merged for the bean
   * @param args explicit arguments to use for constructor or factory method invocation
   * @return a BeanWrapper for the new instance
   * @see #obtainFromSupplier
   * @see #instantiateUsingFactoryMethod
   * @see #autowireConstructor
   * @see #instantiateBean
   */
  @SuppressWarnings("rawtypes")
  protected BeanWrapper createBeanInstance(String beanName, RootBeanDefinition merged, @Nullable Object[] args) {
    // Make sure bean class is actually resolved at this point.
    Class<?> beanClass = resolveBeanClass(beanName, merged);

    if (beanClass != null
            && !Modifier.isPublic(beanClass.getModifiers())
            && !merged.isNonPublicAccessAllowed()) {
      throw new BeanCreationException(merged.getResourceDescription(),
              "Bean class isn't public, and non-public access not allowed: " + beanClass.getName());
    }

    Supplier<?> instanceSupplier = merged.getInstanceSupplier();
    if (instanceSupplier != null) {
      return obtainFromSupplier(merged, instanceSupplier, beanName); // maybe null
    }

    if (merged.getFactoryMethodName() != null) {
      return instantiateUsingFactoryMethod(beanName, merged, args);
    }

    // Shortcut when re-creating the same bean...
    boolean resolved = false;
    boolean autowireNecessary = false;
    if (args == null) {
      synchronized(merged.constructorArgumentLock) {
        if (merged.resolvedConstructorOrFactoryMethod != null) {
          resolved = true;
          autowireNecessary = merged.constructorArgumentsResolved;
        }
      }
    }

    if (resolved) {
      if (autowireNecessary) {
        return autowireConstructor(beanName, merged, null, null);
      }
      else {
        return instantiateBean(beanName, merged);
      }
    }

    // Candidate constructors for autowiring?
    Constructor<?>[] constructors = determineConstructorsFromPostProcessors(beanClass, beanName);
    if (constructors != null
            || merged.getResolvedAutowireMode() == AUTOWIRE_CONSTRUCTOR
            || merged.hasConstructorArgumentValues()
            || ObjectUtils.isNotEmpty(args)) {
      return autowireConstructor(beanName, merged, constructors, args);
    }

    if (beanClass != null && !merged.hasConstructorArgumentValues()) {
      Constructor<?> selected = BeanUtils.getConstructor(beanClass);
      if (selected != null && selected.getParameterCount() > 0) {
        try {
          return autowireConstructor(beanName, merged, new Constructor[] { selected }, args);
        }
        catch (BeansException ignored) { }
      }
      // fallback to default constructor
    }

    return instantiateBean(beanName, merged);
  }

  /**
   * Obtain a bean instance from the given supplier.
   *
   * @param merged merged bean def
   * @param instanceSupplier the configured supplier
   * @param beanName the corresponding bean name
   * @return a BeanWrapper for the new instance
   * @since 4.0
   */
  protected BeanWrapper obtainFromSupplier(RootBeanDefinition merged, Supplier<?> instanceSupplier, String beanName) {
    Object instance = obtainInstanceFromSupplier(instanceSupplier, beanName, merged);
    if (instance == null) {
      instance = NullValue.INSTANCE;
    }

    return createBeanWrapper(merged, instance);
  }

  private Object obtainInstanceFromSupplier(Supplier<?> supplier, String beanName, RootBeanDefinition merged) {
    String outerBean = currentlyCreatedBean.get();
    currentlyCreatedBean.set(beanName);
    try {
      if (supplier instanceof InstanceSupplier<?> instanceSupplier) {
        return instanceSupplier.get(RegisteredBean.of(this, beanName, merged));
      }
      if (supplier instanceof ThrowingSupplier<?> throwableSupplier) {
        return throwableSupplier.getWithException();
      }
      return supplier.get();
    }
    catch (Throwable ex) {
      if (ex instanceof BeansException beansException) {
        throw beansException;
      }
      throw new BeanCreationException(beanName,
              "Instantiation of supplied bean failed", ex);
    }
    finally {
      if (outerBean != null) {
        currentlyCreatedBean.set(outerBean);
      }
      else {
        currentlyCreatedBean.remove();
      }
    }
  }

  /**
   * Overridden in order to implicitly register the currently created bean as
   * dependent on further beans getting programmatically retrieved during a
   * {@link Supplier} callback.
   *
   * @see #obtainFromSupplier
   * @since 4.0
   */
  @Nullable
  @Override
  protected Object handleFactoryBean(String name, String beanName, @Nullable RootBeanDefinition definition, Object beanInstance) throws BeansException {
    String currentlyCreatedBean = this.currentlyCreatedBean.get();
    if (currentlyCreatedBean != null) {
      registerDependentBean(beanName, currentlyCreatedBean);
    }
    return super.handleFactoryBean(name, beanName, definition, beanInstance);
  }

  /**
   * Instantiate the bean using a named factory method. The method may be static, if the
   * merged parameter specifies a class, rather than a factoryBean, or an instance variable
   * on a factory object itself configured using Dependency Injection.
   *
   * @param beanName beanName the name of the bean
   * @param mbd the bean merged for the bean
   * @param explicitArgs argument values passed in programmatically via the getBean method,
   * or {@code null} if none (implying the use of constructor argument values from bean merged)
   * @return a BeanWrapper for the new instance
   * @see #getBean(String, Object[])
   * @since 4.0
   */
  protected BeanWrapper instantiateUsingFactoryMethod(
          String beanName, RootBeanDefinition mbd, @Nullable Object[] explicitArgs) {
    return new ConstructorResolver(this).instantiateUsingFactoryMethod(beanName, mbd, explicitArgs);
  }

  /**
   * "autowire constructor" (with constructor arguments by type) behavior.
   * Also applied if explicit constructor argument values are specified,
   * matching all remaining arguments with beans from the bean factory.
   * <p>This corresponds to constructor injection: In this mode, a Framework
   * bean factory is able to host components that expect constructor-based
   * dependency resolution.
   *
   * @param beanName the name of the bean
   * @param mbd the bean merged for the bean
   * @param ctors the chosen candidate constructors
   * @param explicitArgs argument values passed in programmatically via the getBean method,
   * or {@code null} if none (implying the use of constructor argument values from bean merged)
   * @return a BeanWrapper for the new instance
   * @since 4.0
   */
  protected BeanWrapper autowireConstructor(
          String beanName, RootBeanDefinition mbd, @Nullable Constructor<?>[] ctors, @Nullable Object[] explicitArgs) {

    return new ConstructorResolver(this).autowireConstructor(beanName, mbd, ctors, explicitArgs);
  }

  /**
   * Determine candidate constructors to use for the given bean, checking all registered
   * {@link SmartInstantiationAwareBeanPostProcessor SmartInstantiationAwareBeanPostProcessors}.
   *
   * @param beanClass the raw class of the bean
   * @param beanName the name of the bean
   * @return the candidate constructors, or {@code null} if none specified
   * @throws BeansException in case of errors
   * @see SmartInstantiationAwareBeanPostProcessor#determineCandidateConstructors
   */
  @Nullable
  protected Constructor<?>[] determineConstructorsFromPostProcessors(
          @Nullable Class<?> beanClass, String beanName) throws BeansException {

    if (beanClass != null) {
      var smartInstantiation = postProcessors().smartInstantiation;
      if (!smartInstantiation.isEmpty()) {
        for (SmartInstantiationAwareBeanPostProcessor bp : smartInstantiation) {
          var ctors = bp.determineCandidateConstructors(beanClass, beanName);
          if (ctors != null) {
            return ctors;
          }
        }
      }
    }
    return null;
  }

  /**
   * Instantiate the given bean using its default constructor.
   *
   * @param beanName the name of the bean
   * @param merged the bean merged for the bean
   * @return a BeanWrapper for the new instance
   */
  protected BeanWrapper instantiateBean(String beanName, RootBeanDefinition merged) {
    try {
      Object beanInstance = getInstantiationStrategy().instantiate(merged, beanName, this);
      return createBeanWrapper(merged, beanInstance);
    }
    catch (Throwable ex) {
      throw new BeanCreationException(merged.getResourceDescription(), beanName, ex.getMessage(), ex);
    }
  }

  @Override
  public Object autowire(Class<?> beanClass) throws BeansException {
    return autowire(beanClass, AUTOWIRE_CONSTRUCTOR, false);
  }

  @Override
  public Object autowire(Class<?> beanClass, int autowireMode) throws BeansException {
    return autowire(beanClass, autowireMode, false);
  }

  @Override
  public Object autowire(Class<?> beanClass, int autowireMode, boolean dependencyCheck) throws BeansException {
    // Use non-singleton bean merged, to avoid registering bean as dependent bean.
    RootBeanDefinition merged = new RootBeanDefinition(beanClass, autowireMode, dependencyCheck);
    merged.setScope(BeanDefinition.SCOPE_PROTOTYPE);
    if (merged.getResolvedAutowireMode() == AUTOWIRE_CONSTRUCTOR) {
      return autowireConstructor(beanClass.getName(), merged, null, null).getWrappedInstance();
    }
    else {
      Object bean = getInstantiationStrategy().instantiate(merged, null, this);
      BeanWrapperImpl beanWrapper = createBeanWrapper(merged, bean);
      populateBean(beanClass.getName(), merged, beanWrapper);
      return bean;
    }
  }

  @Override
  public void autowireBeanProperties(Object existingBean, int autowireMode) throws BeansException {
    autowireBeanProperties(existingBean, autowireMode, false);
  }

  @Override
  public void autowireBeanProperties(Object existingBean, int autowireMode, boolean dependencyCheck) throws BeansException {

    if (autowireMode == AUTOWIRE_CONSTRUCTOR) {
      throw new IllegalArgumentException("AUTOWIRE_CONSTRUCTOR not supported for existing bean instance");
    }
    // Use non-singleton bean merged, to avoid registering bean as dependent bean.
    RootBeanDefinition merged =
            new RootBeanDefinition(ClassUtils.getUserClass(existingBean), autowireMode, dependencyCheck);
    merged.setScope(BeanDefinition.SCOPE_PROTOTYPE);
    BeanWrapper bw = createBeanWrapper(merged, existingBean);

    populateBean(merged.getBeanClass().getName(), merged, bw);
  }

  @Override
  public void applyBeanPropertyValues(Object existingBean, String beanName) throws BeansException {
    markBeanAsCreated(beanName);
    BeanDefinition merged = getMergedBeanDefinition(beanName);
    BeanWrapper bw = createBeanWrapper(merged, existingBean);
    applyPropertyValues(beanName, merged, bw, merged.getPropertyValues());
  }

  @Override
  public Object configureBean(Object existingBean, String beanName) throws BeansException {
    markBeanAsCreated(beanName);
    BeanDefinition merged = getMergedBeanDefinition(beanName);
    RootBeanDefinition bd = null;
    if (merged instanceof RootBeanDefinition rbd) {
      bd = rbd.isPrototype() ? rbd : rbd.cloneBeanDefinition();
    }
    if (bd == null) {
      bd = new RootBeanDefinition(merged);
    }
    if (!bd.isPrototype()) {
      bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
      bd.allowCaching = ClassUtils.isCacheSafe(ClassUtils.getUserClass(existingBean), getBeanClassLoader());
    }

    BeanWrapper bw = createBeanWrapper(merged, existingBean);
    populateBean(beanName, bd, bw);
    return initializeBean(existingBean, beanName, bd);
  }

  public void populateBean(String beanName, RootBeanDefinition merged, BeanWrapper beanWrapper) {
    if (beanWrapper == null) {
      if (merged.hasPropertyValues()) {
        throw new BeanCreationException(
                merged.getResourceDescription(), beanName, "Cannot apply property values to null instance");
      }
      else {
        // Skip property population phase for null instance.
        return;
      }
    }

    if (beanWrapper.getWrappedClass().isRecord()) {
      if (merged.hasPropertyValues()) {
        throw new BeanCreationException(
                merged.getResourceDescription(), beanName, "Cannot apply property values to a record");
      }
      else {
        // Skip property population phase for records since they are immutable.
        return;
      }
    }

    // Give any InstantiationAwareBeanPostProcessors the opportunity to modify the
    // state of the bean before properties are set. This can be used, for example,
    // to support styles of field injection.

    Object wrappedInstance = beanWrapper.getWrappedInstance();
    if (!merged.isSynthetic()) {
      for (InstantiationAwareBeanPostProcessor processor : postProcessors().instantiation) {
        if (!processor.postProcessAfterInstantiation(wrappedInstance, beanName)) {
          return;
        }
      }
    }

    // maybe null
    PropertyValues propertyValues = merged.getPropertyValues();
    int resolvedAutowireMode = merged.getResolvedAutowireMode();
    if (resolvedAutowireMode == AUTOWIRE_BY_NAME || resolvedAutowireMode == AUTOWIRE_BY_TYPE) {
      PropertyValues newPvs = new PropertyValues(propertyValues);
      // Add property values based on autowire by name if applicable.
      if (resolvedAutowireMode == AUTOWIRE_BY_NAME) {
        autowireByName(beanName, merged, beanWrapper, newPvs);
      }
      // Add property values based on autowire by type if applicable.
      if (resolvedAutowireMode == AUTOWIRE_BY_TYPE) {
        autowireByType(beanName, merged, beanWrapper, newPvs);
      }
      propertyValues = newPvs;
    }

    boolean hasDependenciesProcessors = !postProcessors().dependencies.isEmpty();
    if (hasDependenciesProcessors && !merged.isSynthetic() && merged.isEnableDependencyInjection()) {
      // -----------------------------------------------
      // apply dependency injection (DI)
      // apply outside framework expanded
      // -----------------------------------------------

      if (propertyValues == null) {
        propertyValues = merged.getPropertyValues();
      }

      for (DependenciesBeanPostProcessor processor : postProcessors().dependencies) {
        PropertyValues pvsToUse = processor.processDependencies(propertyValues, wrappedInstance, beanName);
        if (pvsToUse == null) {
          return;
        }
        propertyValues = pvsToUse;
      }
    }

    boolean needsDepCheck = merged.getDependencyCheck() != AbstractBeanDefinition.DEPENDENCY_CHECK_NONE;
    if (needsDepCheck) {
      BeanProperty[] filteredPds = filterBeanPropertiesForDependencyCheck(beanWrapper, merged.allowCaching);
      checkDependencies(beanName, merged, filteredPds, propertyValues);
    }

    if (propertyValues != null) {
      applyPropertyValues(beanName, merged, beanWrapper, propertyValues);
    }
  }

  /**
   * Apply the given property values, resolving any runtime references
   * to other beans in this bean factory. Must use deep copy, so we
   * don't permanently modify this property.
   *
   * @param beanName the bean name passed for better exception information
   * @param definition the bean merged
   * @param beanWrapper the BeanWrapper wrapping the target object
   * @param pvs the new property values
   */
  protected void applyPropertyValues(
          String beanName, BeanDefinition definition, BeanWrapper beanWrapper, @Nullable PropertyValues pvs) {
    if (pvs == null || pvs.isEmpty()) {
      return;
    }

    if (pvs.isConverted()) {
      // Shortcut: use the pre-converted values as-is.
      try {
        beanWrapper.setPropertyValues(pvs);
        return;
      }
      catch (BeansException ex) {
        throw new BeanCreationException(
                definition.getResourceDescription(), beanName, "Error setting property values", ex);
      }
    }
    List<PropertyValue> original = pvs.asList();

    TypeConverter converter = getCustomTypeConverter();
    if (converter == null) {
      converter = beanWrapper;
    }
    var valueResolver = new BeanDefinitionValueResolver(this, beanName, definition, converter);

    // Create a deep copy, resolving any references for values.
    ArrayList<PropertyValue> deepCopy = new ArrayList<>(original.size());
    boolean resolveNecessary = false;
    for (PropertyValue pv : original) {
      if (pv.isConverted()) {
        deepCopy.add(pv);
      }
      else {
        String propertyName = pv.getName();
        Object originalValue = pv.getValue();

        Object resolvedValue;
        if (originalValue instanceof PropertyValueRetriever retriever) {
          resolvedValue = retriever.retrieve(propertyName, beanWrapper, this);
          if (resolvedValue == PropertyValueRetriever.DO_NOT_SET) {
            continue;
          }
        }
        else {
          if (originalValue == AutowiredPropertyMarker.INSTANCE) {
            if (!beanWrapper.isWritableProperty(propertyName)) {
              throw new IllegalArgumentException("Autowire marker for property without write method: " + pv);
            }
            BeanProperty property = beanWrapper.getBeanProperty(propertyName);
            MethodParameter writeMethodParameter = property.getWriteMethodParameter();
            if (writeMethodParameter != null) {
              originalValue = new DependencyDescriptor(writeMethodParameter, true);
            }
            else {
              originalValue = new DependencyDescriptor(property.getField(), true);
            }
          }
          resolvedValue = valueResolver.resolveValueIfNecessary(pv, originalValue);
        }

        Object convertedValue = resolvedValue;
        boolean convertible = beanWrapper.isWritableProperty(propertyName)
                && !PropertyAccessorUtils.isNestedOrIndexedProperty(propertyName);
        if (convertible) {
          convertedValue = convertForProperty(resolvedValue, propertyName, beanWrapper, converter);
        }
        // Possibly store converted value in merged bean merged,
        // in order to avoid re-conversion for every created bean instance.
        if (resolvedValue == originalValue) {
          if (convertible) {
            pv.setConvertedValue(convertedValue);
          }
          deepCopy.add(pv);
        }
        else if (convertible && originalValue instanceof TypedStringValue
                && !((TypedStringValue) originalValue).isDynamic()
                && !(convertedValue instanceof Collection || ObjectUtils.isArray(convertedValue))) {
          pv.setConvertedValue(convertedValue);
          deepCopy.add(pv);
        }
        else {
          resolveNecessary = true;
          deepCopy.add(new PropertyValue(pv, convertedValue));
        }
      }
    }
    if (!resolveNecessary) {
      pvs.setConverted();
    }

    // Set our (possibly massaged) deep copy.
    try {
      beanWrapper.setPropertyValues(new PropertyValues(deepCopy));
    }
    catch (BeansException ex) {
      throw new BeanCreationException(
              definition.getResourceDescription(), beanName, ex.getMessage(), ex);
    }
  }

  /**
   * Convert the given value for the specified target property.
   */
  @Nullable
  private Object convertForProperty(
          @Nullable Object value, String propertyName, BeanWrapper bw, TypeConverter converter) {
    if (converter instanceof BeanWrapperImpl) {
      return ((BeanWrapperImpl) converter).convertForProperty(value, propertyName);
    }
    else {
      BeanProperty pd = bw.getBeanProperty(propertyName);
      MethodParameter methodParam = pd.getWriteMethodParameter();
      return converter.convertIfNecessary(value, pd.getType(), methodParam);
    }
  }

  /**
   * Fill in any missing property values with references to
   * other beans in this factory if autowire is set to "byName".
   *
   * @param beanName the name of the bean we're wiring up.
   * Useful for debugging messages; not used functionally.
   * @param definition bean merged to update through autowiring
   * @param bw the BeanWrapper from which we can obtain information about the bean
   * @param pvs the PropertyValues to register wired objects with
   */
  protected void autowireByName(
          String beanName, AbstractBeanDefinition definition, BeanWrapper bw, PropertyValues pvs) {
    String[] propertyNames = unsatisfiedNonSimpleProperties(definition, bw);
    for (String propertyName : propertyNames) {
      if (containsBean(propertyName)) {
        Object bean = getBean(propertyName);
        pvs.add(propertyName, bean);
        registerDependentBean(propertyName, beanName);
        if (log.isTraceEnabled()) {
          log.trace("Added autowiring by name from bean name '{}' via property '{}' to bean named '{}'",
                  beanName, propertyName, propertyName);
        }
      }
      else {
        if (log.isTraceEnabled()) {
          log.trace("Not autowiring property '{}' of bean '{]' by name: no matching bean found",
                  propertyName, beanName);
        }
      }
    }
  }

  /**
   * Abstract method defining "autowire by type" (bean properties by type) behavior.
   * <p>This is like PicoContainer default, in which there must be exactly one bean
   * of the property type in the bean factory. This makes bean factories simple to
   * configure for small namespaces, but doesn't work as well as standard Framework
   * behavior for bigger applications.
   *
   * @param beanName the name of the bean to autowire by type
   * @param definition the merged bean merged to update through autowiring
   * @param wrapper the BeanWrapper from which we can obtain information about the bean
   * @param pvs the PropertyValues to register wired objects with
   */
  protected void autowireByType(
          String beanName, BeanDefinition definition, BeanWrapper wrapper, PropertyValues pvs) {
    BeanMetadata metadata = wrapper.getMetadata();
    LinkedHashSet<String> autowiredBeanNames = new LinkedHashSet<>(4);

    String[] propertyNames = unsatisfiedNonSimpleProperties(definition, wrapper);
    for (String propertyName : propertyNames) {
      try {
        BeanProperty beanProperty = metadata.obtainBeanProperty(propertyName);
        // Don't try autowiring by type for type Object: never makes sense,
        // even if it technically is an unsatisfied, non-simple property, non-writeable.
        if (Object.class != beanProperty.getType() && beanProperty.isWriteable()) {
          // Do not allow eager init for type matching in case of a prioritized post-processor.
          boolean eager = !(wrapper.getWrappedInstance() instanceof PriorityOrdered);
          DependencyDescriptor desc;
          MethodParameter methodParam = beanProperty.getWriteMethodParameter();
          if (methodParam != null) {
            desc = new AutowireByTypeDependencyDescriptor(methodParam, eager);
          }
          else {
            desc = new AutowireByTypeDependencyDescriptor(beanProperty.getField(), eager);
          }
          Object autowiredArgument = resolveDependency(desc, beanName, autowiredBeanNames, null);
          if (autowiredArgument != null) {
            pvs.add(propertyName, autowiredArgument);
          }
          for (String autowiredBeanName : autowiredBeanNames) {
            registerDependentBean(autowiredBeanName, beanName);
            if (log.isTraceEnabled()) {
              log.trace("Autowiring by type from bean name '{}' via property '{}' to bean named '{}'",
                      beanName, propertyName, autowiredBeanName);
            }
          }
          autowiredBeanNames.clear();
        }
      }
      catch (BeansException ex) {
        throw new UnsatisfiedDependencyException(definition.getResourceDescription(), beanName, propertyName, ex);
      }
    }
  }

  /**
   * Return an array of non-simple bean properties that are unsatisfied.
   * These are probably unsatisfied references to other beans in the
   * factory. Does not include simple properties like primitives or Strings.
   *
   * @param definition the merged bean merged the bean was created with
   * @param wrapper the BeanWrapper the bean was created with
   * @return an array of bean property names
   * @see BeanUtils#isSimpleProperty
   */
  protected String[] unsatisfiedNonSimpleProperties(BeanDefinition definition, BeanWrapper wrapper) {
    TreeSet<String> result = new TreeSet<>();
    PropertyValues pvs = definition.getPropertyValues();
    for (BeanProperty property : wrapper.getBeanProperties()) {
      if (property.isWriteable()
              && !isExcludedFromDependencyCheck(property)
              && (pvs == null || !pvs.contains(property.getName()))
              && !BeanUtils.isSimpleProperty(property.getType())) {
        result.add(property.getName());
      }
    }
    return StringUtils.toStringArray(result);
  }

  /**
   * Extract a filtered set of PropertyDescriptors from the given BeanWrapper,
   * excluding ignored dependency types or properties defined on ignored dependency interfaces.
   *
   * @param bw the BeanWrapper the bean was created with
   * @param cache whether to cache filtered PropertyDescriptors for the given bean Class
   * @return the filtered PropertyDescriptors
   * @see #isExcludedFromDependencyCheck
   * @see #filterBeanPropertiesForDependencyCheck(BeanWrapper)
   * @since 4.0
   */
  protected BeanProperty[] filterBeanPropertiesForDependencyCheck(BeanWrapper bw, boolean cache) {
    BeanProperty[] filtered = filteredBeanPropertiesCache.get(bw.getWrappedClass());
    if (filtered == null) {
      filtered = filterBeanPropertiesForDependencyCheck(bw);
      if (cache) {
        BeanProperty[] existing = filteredBeanPropertiesCache.putIfAbsent(bw.getWrappedClass(), filtered);
        if (existing != null) {
          filtered = existing;
        }
      }
    }
    return filtered;
  }

  /**
   * Extract a filtered set of BeanProperties from the given BeanWrapper,
   * excluding ignored dependency types or properties defined on ignored dependency interfaces.
   *
   * @param bw the BeanWrapper the bean was created with
   * @return the filtered PropertyDescriptors
   * @see #isExcludedFromDependencyCheck
   */
  protected BeanProperty[] filterBeanPropertiesForDependencyCheck(BeanWrapper bw) {
    ArrayList<BeanProperty> pds = new ArrayList<>(bw.getBeanProperties());
    pds.removeIf(this::isExcludedFromDependencyCheck);
    return pds.toArray(new BeanProperty[0]);
  }

  /**
   * Perform a dependency check that all properties exposed have been set,
   * if desired. Dependency checks can be objects (collaborating beans),
   * simple (primitives and String), or all (both).
   *
   * @param beanName the name of the bean
   * @param mbd the merged bean merged the bean was created with
   * @param beanProperties the relevant property descriptors for the target bean
   * @param propertyValues the property values to be applied to the bean
   * @see #isExcludedFromDependencyCheck(BeanProperty)
   */
  protected void checkDependencies(String beanName, AbstractBeanDefinition mbd,
          BeanProperty[] beanProperties, @Nullable PropertyValues propertyValues) throws UnsatisfiedDependencyException {

    int dependencyCheck = mbd.getDependencyCheck();
    for (BeanProperty property : beanProperties) {
      if (property.isWriteable() && (propertyValues == null || !propertyValues.contains(property.getName()))) {
        boolean isSimple = BeanUtils.isSimpleProperty(property.getType());
        boolean unsatisfied = (dependencyCheck == AbstractBeanDefinition.DEPENDENCY_CHECK_ALL)
                || (isSimple && dependencyCheck == AbstractBeanDefinition.DEPENDENCY_CHECK_SIMPLE)
                || (!isSimple && dependencyCheck == AbstractBeanDefinition.DEPENDENCY_CHECK_OBJECTS);
        if (unsatisfied) {
          throw new UnsatisfiedDependencyException(mbd.getResourceDescription(), beanName, property.getName(),
                  "Set this property value or disable dependency checking for this bean.");
        }
      }
    }
  }

  /**
   * Determine whether the given bean property is excluded from dependency checks.
   * <p>This implementation excludes properties defined by CGLIB and
   * properties whose type matches an ignored dependency type or which
   * are defined by an ignored dependency interface.
   *
   * @param property the BeanProperty of the bean property
   * @return whether the bean property is excluded
   * @see #ignoreDependencyType(Class)
   * @see #ignoreDependencyInterface(Class)
   */
  protected boolean isExcludedFromDependencyCheck(BeanProperty property) {
    return ignoredDependencyTypes.contains(property.getType())
            || AutowireUtils.isExcludedFromDependencyCheck(property)
            || AutowireUtils.isSetterDefinedInInterface(property, ignoredDependencyInterfaces);
  }

  @Override
  public void destroyBean(Object existingBean) {
    new DisposableBeanAdapter(existingBean, postProcessors().destruction).destroy();
  }

  //---------------------------------------------------------------------
  // Implementation of AbstractBeanFactory class
  //---------------------------------------------------------------------

  /**
   * Applies the {@code postProcessAfterInitialization} callback of all
   * registered BeanPostProcessors, giving them a chance to post-process the
   * object obtained from FactoryBeans (for example, to auto-proxy them).
   *
   * @see #applyBeanPostProcessorsAfterInitialization
   */
  @Override
  protected Object postProcessObjectFromFactoryBean(Object object, String beanName) {
    return applyBeanPostProcessorsAfterInitialization(object, beanName);
  }

  /**
   * Overridden to clear FactoryBean instance cache as well.
   */
  @Override
  public void removeSingleton(String beanName) {
    synchronized(getSingletonMutex()) {
      super.removeSingleton(beanName);
      this.factoryBeanInstanceCache.remove(beanName);
    }
  }

  /**
   * Overridden to clear FactoryBean instance cache as well.
   */
  @Override
  protected void clearSingletonCache() {
    synchronized(getSingletonMutex()) {
      super.clearSingletonCache();
      this.factoryBeanInstanceCache.clear();
    }
  }

  @Override
  @Nullable
  protected Class<?> predictBeanType(String beanName, RootBeanDefinition merged, Class<?>... typesToMatch) {
    Class<?> targetType = determineTargetType(beanName, merged, typesToMatch);
    // Apply SmartInstantiationAwareBeanPostProcessors to predict the
    // eventual type after a before-instantiation shortcut.
    if (targetType != null && !merged.isSynthetic()) {
      ArrayList<SmartInstantiationAwareBeanPostProcessor> instantiation = postProcessors().smartInstantiation;
      if (!instantiation.isEmpty()) {
        boolean matchingOnlyFactoryBean = typesToMatch.length == 1 && typesToMatch[0] == FactoryBean.class;
        for (SmartInstantiationAwareBeanPostProcessor bp : instantiation) {
          Class<?> predicted = bp.predictBeanType(targetType, beanName);
          if (predicted != null &&
                  (!matchingOnlyFactoryBean || FactoryBean.class.isAssignableFrom(predicted))) {
            return predicted;
          }
        }
      }
    }
    return targetType;
  }

  /**
   * Determine the target type for the given bean merged.
   *
   * @param beanName the name of the bean (for error handling purposes)
   * @param mbd the merged bean merged for the bean
   * @param typesToMatch the types to match in case of internal type matching purposes
   * (also signals that the returned {@code Class} will never be exposed to application code)
   * @return the type for the bean if determinable, or {@code null} otherwise
   */
  @Nullable
  protected Class<?> determineTargetType(String beanName, RootBeanDefinition mbd, Class<?>... typesToMatch) {
    Class<?> targetType = mbd.getTargetType();
    if (targetType == null) {
      if (mbd.getFactoryMethodName() != null) {
        targetType = getTypeForFactoryMethod(beanName, mbd, typesToMatch);
      }
      else {
        targetType = resolveBeanClass(beanName, mbd, typesToMatch);
        if (mbd.hasBeanClass()) {
          targetType = getInstantiationStrategy().getActualBeanClass(mbd, beanName, this);
        }
      }
      if (ObjectUtils.isEmpty(typesToMatch) || getTempClassLoader() == null) {
        mbd.resolvedTargetType = targetType;
      }
    }
    return targetType;
  }

  /**
   * Determine the target type for the given bean merged which is based on
   * a factory method. Only called if there is no singleton instance registered
   * for the target bean already.
   * <p>This implementation determines the type matching {@link #createBean}'s
   * different creation strategies. As far as possible, we'll perform static
   * type checking to avoid creation of the target bean.
   *
   * @param beanName the name of the bean (for error handling purposes)
   * @param merged the merged bean merged for the bean
   * @return the type for the bean if determinable, or {@code null} otherwise
   * @see #createBean
   */
  @Nullable
  protected Class<?> getTypeForFactoryMethod(String beanName, RootBeanDefinition merged, Class<?>... typesToMatch) {
    ResolvableType cachedReturnType = merged.factoryMethodReturnType;
    if (cachedReturnType != null) {
      return cachedReturnType.resolve();
    }

    Class<?> commonType = null;
    Method uniqueCandidate = merged.factoryMethodToIntrospect;

    if (uniqueCandidate == null) {
      Class<?> factoryClass;
      boolean isStatic = true;

      String factoryBeanName = merged.getFactoryBeanName();
      if (factoryBeanName != null) {
        if (factoryBeanName.equals(beanName)) {
          throw new BeanDefinitionStoreException(merged.getResourceDescription(),
                  "factory-bean reference points back to the same bean merged");
        }
        // Check declared factory method return type on factory class.
        factoryClass = getType(factoryBeanName);
        isStatic = false;
      }
      else {
        // Check declared factory method return type on bean class.
        factoryClass = resolveBeanClass(beanName, merged, typesToMatch);
      }

      if (factoryClass == null) {
        return null;
      }
      factoryClass = ClassUtils.getUserClass(factoryClass);

      // If all factory methods have the same return type, return that type.
      // Can't clearly figure out exact method due to type converting / autowiring!
      int minNrOfArgs = merged.hasConstructorArgumentValues()
                        ? merged.getConstructorArgumentValues().getArgumentCount() : 0;
      Method[] candidates = ReflectionUtils.getUniqueDeclaredMethods(
              factoryClass, ReflectionUtils.USER_DECLARED_METHODS);

      for (Method candidate : candidates) {
        if (Modifier.isStatic(candidate.getModifiers()) == isStatic
                && candidate.getParameterCount() >= minNrOfArgs
                && merged.isFactoryMethod(candidate)) {
          // Declared type variables to inspect?
          if (candidate.getTypeParameters().length > 0) {
            try {
              // Fully resolve parameter names and argument values.
              ConstructorArgumentValues cav = merged.getConstructorArgumentValues();

              Class<?>[] paramTypes = candidate.getParameterTypes();
              String[] paramNames = null;

              if (cav.containsNamedArgument()) {
                ParameterNameDiscoverer pnd = getParameterNameDiscoverer();
                if (pnd != null) {
                  paramNames = pnd.getParameterNames(candidate);
                }
              }

              var usedValueHolders = new HashSet<ConstructorArgumentValues.ValueHolder>(paramTypes.length);
              Object[] args = new Object[paramTypes.length];
              for (int i = 0; i < args.length; i++) {
                String requiredName = paramNames != null ? paramNames[i] : null;

                ConstructorArgumentValues.ValueHolder valueHolder =
                        cav.getArgumentValue(i, paramTypes[i], requiredName, usedValueHolders);
                if (valueHolder == null) {
                  valueHolder = cav.getGenericArgumentValue(null, null, usedValueHolders);
                }
                if (valueHolder != null) {
                  args[i] = valueHolder.getValue();
                  usedValueHolders.add(valueHolder);
                }
              }
              Class<?> returnType = AutowireUtils.resolveReturnTypeForFactoryMethod(
                      candidate, args, getBeanClassLoader());
              uniqueCandidate = commonType == null && returnType == candidate.getReturnType()
                                ? candidate : null;
              commonType = ClassUtils.determineCommonAncestor(returnType, commonType);
              if (commonType == null) {
                // Ambiguous return types found: return null to indicate "not determinable".
                return null;
              }
            }
            catch (Throwable ex) {
              if (log.isDebugEnabled()) {
                log.debug("Failed to resolve generic return type for factory method: {}", ex.toString());
              }
            }
          }
          else {
            uniqueCandidate = commonType == null ? candidate : null;
            commonType = ClassUtils.determineCommonAncestor(candidate.getReturnType(), commonType);
            if (commonType == null) {
              // Ambiguous return types found: return null to indicate "not determinable".
              return null;
            }
          }
        }
      }

      merged.factoryMethodToIntrospect = uniqueCandidate;
      if (commonType == null) {
        return null;
      }
    }

    // Common return type found: all factory methods return same type. For a non-parameterized
    // unique candidate, cache the full type declaration context of the target factory method.
    cachedReturnType = uniqueCandidate != null
                       ? ResolvableType.forReturnType(uniqueCandidate)
                       : ResolvableType.forClass(commonType);
    merged.factoryMethodReturnType = cachedReturnType;
    return cachedReturnType.resolve();
  }

  /**
   * This implementation attempts to query the FactoryBean's generic parameter metadata
   * if present to determine the object type. If not present, i.e. the FactoryBean is
   * declared as a raw type, checks the FactoryBean's {@code getObjectType} method
   * on a plain instance of the FactoryBean, without bean properties applied yet.
   * If this doesn't return a type yet, and {@code allowInit} is {@code true} a
   * full creation of the FactoryBean is used as fallback (through delegation to the
   * superclass's implementation).
   * <p>The shortcut check for a FactoryBean is only applied in case of a singleton
   * FactoryBean. If the FactoryBean instance itself is not kept as singleton,
   * it will be fully created to check the type of its exposed object.
   */
  @Override
  protected ResolvableType getTypeForFactoryBean(String beanName, RootBeanDefinition merged, boolean allowInit) {
    // Check if the bean merged itself has defined the type with an attribute
    ResolvableType result = getTypeForFactoryBeanFromAttributes(merged);
    if (result != ResolvableType.NONE) {
      return result;
    }

    ResolvableType beanType = merged.hasBeanClass()
                              ? ResolvableType.forClass(merged.getBeanClass())
                              : ResolvableType.NONE;

    // For instance supplied beans try the target type and bean class
    if (merged.getInstanceSupplier() != null) {
      result = getFactoryBeanGeneric(merged.targetType);
      if (result.resolve() != null) {
        return result;
      }
      result = getFactoryBeanGeneric(beanType);
      Class<?> resolved = result.resolve();
      if (resolved != null) {
        if (resolved == Object.class) {
          if (!allowInit) {
            // initialization not allowed, so return Object.class
            return result;
          }
        }
        else {
          return result;
        }
      }
    }

    // Consider factory methods
    String factoryBeanName = merged.getFactoryBeanName();
    String factoryMethodName = merged.getFactoryMethodName();

    // Scan the factory bean methods
    if (factoryBeanName != null) {
      if (factoryMethodName != null) {
        // Try to obtain the FactoryBean's object type from its factory method
        // declaration without instantiating the containing bean at all.

        Class<?> factoryBeanClass;
        BeanDefinition factoryBeanDefinition = getBeanDefinition(factoryBeanName);
        if (factoryBeanDefinition instanceof AbstractBeanDefinition &&
                ((AbstractBeanDefinition) factoryBeanDefinition).hasBeanClass()) {
          factoryBeanClass = ((AbstractBeanDefinition) factoryBeanDefinition).getBeanClass();
        }
        else {
          RootBeanDefinition factoryMerged = getMergedBeanDefinition(factoryBeanName, factoryBeanDefinition);
          factoryBeanClass = determineTargetType(factoryBeanName, factoryMerged);
        }

        if (factoryBeanClass != null) {
          result = getTypeForFactoryBeanFromMethod(factoryBeanClass, factoryMethodName);
          if (result.resolve() != null) {
            return result;
          }
        }
      }
      // If not resolvable above and the referenced factory bean doesn't exist yet,
      // exit here - we don't want to force the creation of another bean just to
      // obtain a FactoryBean's object type...
      if (!isBeanEligibleForMetadataCaching(factoryBeanName)) {
        return ResolvableType.NONE;
      }
    }

    // If we're allowed, we can create the factory bean and call getObjectType() early
    if (allowInit) {
      FactoryBean<?> factoryBean = getFactoryBeanForTypeCheck(beanName, merged);
      if (factoryBean != null) {
        // Try to obtain the FactoryBean's object type from this early stage of the instance.
        Class<?> type = getTypeForFactoryBean(factoryBean);
        if (type != null) {
          return ResolvableType.forClass(type);
        }
        // No type found for shortcut FactoryBean instance:
        // fall back to full creation of the FactoryBean instance.
        return super.getTypeForFactoryBean(beanName, merged, true);
      }
    }

    if (factoryBeanName == null && merged.hasBeanClass() && factoryMethodName != null) {
      // No early bean instantiation possible: determine FactoryBean's type from
      // static factory method signature or from class inheritance hierarchy...
      return getTypeForFactoryBeanFromMethod(merged.getBeanClass(), factoryMethodName);
    }
    result = getFactoryBeanGeneric(beanType);
    if (result.resolve() != null) {
      return result;
    }
    return ResolvableType.NONE;
  }

  private ResolvableType getFactoryBeanGeneric(@Nullable ResolvableType type) {
    if (type == null) {
      return ResolvableType.NONE;
    }
    return type.as(FactoryBean.class).getGeneric();
  }

  /**
   * Introspect the factory method signatures on the given bean class,
   * trying to find a common {@code FactoryBean} object type declared there.
   *
   * @param beanClass the bean class to find the factory method on
   * @param factoryMethodName the name of the factory method
   * @return the common {@code FactoryBean} object type, or {@code null} if none
   */
  private ResolvableType getTypeForFactoryBeanFromMethod(Class<?> beanClass, String factoryMethodName) {
    // CGLIB subclass methods hide generic parameters; look at the original user class.
    Class<?> factoryBeanClass = ClassUtils.getUserClass(beanClass);
    FactoryBeanMethodTypeFinder finder = new FactoryBeanMethodTypeFinder(factoryMethodName);
    ReflectionUtils.doWithMethods(factoryBeanClass, finder, ReflectionUtils.USER_DECLARED_METHODS);
    return finder.getResult();
  }

  @Nullable
  private FactoryBean<?> getFactoryBeanForTypeCheck(String beanName, RootBeanDefinition def) {
    if (def.isSingleton()) {
      synchronized(getSingletonMutex()) {
        BeanWrapper beanWrapper = this.factoryBeanInstanceCache.get(beanName);
        if (beanWrapper != null && beanWrapper.getWrappedInstance() instanceof FactoryBean<?> factory) {
          return factory;
        }
        Object instance = getSingleton(beanName, false);
        if (instance == NullValue.INSTANCE) { // created and its instance is null
          return null;
        }
        if (instance instanceof FactoryBean<?> factory) {
          return factory;
        }

        if (isSingletonCurrentlyInCreation(beanName)
                || (def.getFactoryBeanName() != null && isSingletonCurrentlyInCreation(def.getFactoryBeanName()))) {
          return null;
        }

        try {
          // Mark this bean as currently in creation, even if just partially.
          beforeSingletonCreation(beanName);
          // Give BeanPostProcessors a chance to return a proxy instead of the target bean instance.
          instance = resolveBeforeInstantiation(beanName, def);
          if (instance == null) {
            beanWrapper = createBeanInstance(beanName, def, null);
            instance = beanWrapper.getWrappedInstance();
          }
        }
        catch (UnsatisfiedDependencyException ex) {
          // Don't swallow, probably misconfiguration...
          throw ex;
        }
        catch (BeanCreationException ex) {
          // Don't swallow a linkage error since it contains a full stacktrace on
          // first occurrence... and just a plain NoClassDefFoundError afterwards.
          if (ex.contains(LinkageError.class)) {
            throw ex;
          }
          // Instantiation failure, maybe too early...
          if (log.isDebugEnabled()) {
            log.debug("Bean creation exception on singleton FactoryBean type check: {}", ex.toString());
          }
          onSuppressedException(ex);
          return null;
        }
        finally {
          // Finished partial creation of this bean.
          afterSingletonCreation(beanName);
        }
        // put to factoryBeanInstanceCache
        FactoryBean<?> factory = getFactoryBean(beanName, instance);
        if (beanWrapper != null) {
          factoryBeanInstanceCache.put(beanName, beanWrapper);
        }
        return factory;
      }
    }
    else {
      // prototype
      if (isPrototypeCurrentlyInCreation(beanName)) {
        return null;
      }
      // Prototype
      Object instance;
      try {
        // Mark this bean as currently in creation, even if just partially.
        beforePrototypeCreation(beanName);
        // Give BeanPostProcessors a chance to return a proxy instead of the target bean instance.
        instance = resolveBeforeInstantiation(beanName, def);
        if (instance == null) {
          BeanWrapper beanWrapper = createBeanInstance(beanName, def, null);
          instance = beanWrapper.getWrappedInstance();
        }
      }
      catch (UnsatisfiedDependencyException ex) {
        // Don't swallow, probably misconfiguration...
        throw ex;
      }
      catch (BeanCreationException ex) {
        // Instantiation failure, maybe too early...
        if (log.isDebugEnabled()) {
          log.debug("Bean creation exception on non-singleton FactoryBean type check: {}", ex.toString());
        }
        onSuppressedException(ex);
        return null;
      }
      finally {
        // Finished partial creation of this bean.
        afterPrototypeCreation(beanName);
      }
      return getFactoryBean(beanName, instance);
    }
  }

  @Override
  public Object resolveBeanByName(String name, DependencyDescriptor descriptor) {
    InjectionPoint previousInjectionPoint = ConstructorResolver.setCurrentInjectionPoint(descriptor);
    try {
      return getBean(name, descriptor.getDependencyType());
    }
    finally {
      ConstructorResolver.setCurrentInjectionPoint(previousInjectionPoint);
    }
  }

  @Override
  public void preInstantiateSingletons() {
    if (log.isTraceEnabled()) {
      log.trace("Pre-instantiating singletons in {}", this);
    }
    // Iterate over a copy to allow for init methods which in turn register new bean definitions.
    // While this may not be part of the regular factory bootstrap, it does otherwise work fine.

    String[] beanNames = getBeanDefinitionNames();
    // Trigger initialization of all non-lazy singleton beans...
    for (String beanName : beanNames) {
      RootBeanDefinition merged = getMergedLocalBeanDefinition(beanName);
      // Trigger initialization of all non-lazy singleton beans...
      if (!merged.isAbstract() && merged.isSingleton() && !merged.isLazyInit()) {
        if (isFactoryBean(beanName)) {
          Object bean = getBean(FACTORY_BEAN_PREFIX + beanName);
          if (bean instanceof SmartFactoryBean<?> smartFactory && smartFactory.isEagerInit()) {
            getBean(beanName);
          }
        }
        else {
          getBean(beanName);
        }
      }
    }

    // Trigger post-initialization callback for all applicable beans...
    for (String beanName : beanNames) {
      Object singletonInstance = getSingleton(beanName);
      if (singletonInstance instanceof SmartInitializingSingleton smartSingleton) {
        smartSingleton.afterSingletonsInstantiated(this);
      }
    }

    log.debug("The singleton objects are initialized.");
  }

  /**
   * Set the instantiation strategy to use for creating bean instances.
   * Default is CglibSubclassingInstantiationStrategy.
   *
   * @since 4.0
   */
  public void setInstantiationStrategy(InstantiationStrategy instantiationStrategy) {
    this.instantiationStrategy = instantiationStrategy;
  }

  /**
   * Return the instantiation strategy to use for creating bean instances.
   *
   * @since 4.0
   */
  public InstantiationStrategy getInstantiationStrategy() {
    return this.instantiationStrategy;
  }

  /**
   * Set the ParameterNameDiscoverer to use for resolving method parameter
   * names if needed (e.g. for constructor names).
   * <p>Default is a {@link DefaultParameterNameDiscoverer}.
   *
   * @since 4.0
   */
  public void setParameterNameDiscoverer(@Nullable ParameterNameDiscoverer parameterNameDiscoverer) {
    this.parameterNameDiscoverer = parameterNameDiscoverer;
  }

  /**
   * Return the ParameterNameDiscoverer to use for resolving method parameter
   * names if needed.
   *
   * @since 4.0
   */
  @Nullable
  protected ParameterNameDiscoverer getParameterNameDiscoverer() {
    return this.parameterNameDiscoverer;
  }

  /**
   * Set whether to allow circular references between beans - and automatically
   * try to resolve them.
   * <p>Note that circular reference resolution means that one of the involved beans
   * will receive a reference to another bean that is not fully initialized yet.
   * This can lead to subtle and not-so-subtle side effects on initialization;
   * it does work fine for many scenarios, though.
   * <p>Default is "true". Turn this off to throw an exception when encountering
   * a circular reference, disallowing them completely.
   * <p><b>NOTE:</b> It is generally recommended to not rely on circular references
   * between your beans. Refactor your application logic to have the two beans
   * involved delegate to a third bean that encapsulates their common logic.
   *
   * @since 4.0
   */
  public void setAllowCircularReferences(boolean allowCircularReferences) {
    this.allowCircularReferences = allowCircularReferences;
  }

  /**
   * Return whether to allow circular references between beans.
   *
   * @see #setAllowCircularReferences
   * @since 4.0
   */
  public boolean isAllowCircularReferences() {
    return this.allowCircularReferences;
  }

  /**
   * Set whether to allow the raw injection of a bean instance into some other
   * bean's property, despite the injected bean eventually getting wrapped
   * (for example, through AOP auto-proxying).
   * <p>This will only be used as a last resort in case of a circular reference
   * that cannot be resolved otherwise: essentially, preferring a raw instance
   * getting injected over a failure of the entire bean wiring process.
   * <p>Default is "false". Turn this on to allow for non-wrapped
   * raw beans injected into some of your references.
   * <p><b>NOTE:</b> It is generally recommended to not rely on circular references
   * between your beans, in particular with auto-proxying involved.
   *
   * @see #setAllowCircularReferences
   * @since 4.0
   */
  public void setAllowRawInjectionDespiteWrapping(boolean allowRawInjectionDespiteWrapping) {
    this.allowRawInjectionDespiteWrapping = allowRawInjectionDespiteWrapping;
  }

  /**
   * Return whether to allow the raw injection of a bean instance.
   *
   * @see #setAllowRawInjectionDespiteWrapping
   * @since 4.0
   */
  public boolean isAllowRawInjectionDespiteWrapping() {
    return this.allowRawInjectionDespiteWrapping;
  }

  /**
   * Ignore the given dependency type for autowiring:
   * for example, String. Default is none.
   *
   * @since 4.0
   */
  @Override
  public void ignoreDependencyType(Class<?> type) {
    this.ignoredDependencyTypes.add(type);
  }

  /**
   * Ignore the given dependency interface for autowiring.
   * <p>This will typically be used by application contexts to register
   * dependencies that are resolved in other ways, like BeanFactory through
   * BeanFactoryAware or ApplicationContext through ApplicationContextAware.
   * <p>By default, only the BeanFactoryAware interface is ignored.
   * For further types to ignore, invoke this method for each type.
   *
   * @see BeanFactoryAware
   * @see cn.taketoday.context.ApplicationContextAware
   * @since 4.0
   */
  @Override
  public void ignoreDependencyInterface(Class<?> ifc) {
    this.ignoredDependencyInterfaces.add(ifc);
  }

  @Override
  public void copyConfigurationFrom(ConfigurableBeanFactory otherFactory) {
    super.copyConfigurationFrom(otherFactory);
    if (otherFactory instanceof AbstractAutowireCapableBeanFactory otherAutowireFactory) {
      this.instantiationStrategy = otherAutowireFactory.instantiationStrategy;
      this.allowCircularReferences = otherAutowireFactory.allowCircularReferences;
      this.ignoredDependencyTypes.addAll(otherAutowireFactory.ignoredDependencyTypes);
      this.ignoredDependencyInterfaces.addAll(otherAutowireFactory.ignoredDependencyInterfaces);
    }
  }

  /**
   * Expose the logger to collaborating delegates.
   */
  Logger getLogger() {
    return log;
  }

  /**
   * Special DependencyDescriptor variant for Framework's good old autowire="byType" mode.
   * Always optional; never considering the parameter name for choosing a primary candidate.
   */
  private static class AutowireByTypeDependencyDescriptor extends DependencyDescriptor {
    @Serial
    private static final long serialVersionUID = 1L;

    public AutowireByTypeDependencyDescriptor(MethodParameter methodParameter, boolean eager) {
      super(methodParameter, false, eager);
    }

    public AutowireByTypeDependencyDescriptor(Field field, boolean eager) {
      super(field, false, eager);
    }

    @Override
    public String getDependencyName() {
      return null;
    }
  }

  /**
   * {@link ReflectionUtils.MethodCallback} used to find {@link FactoryBean} type information.
   */
  private static class FactoryBeanMethodTypeFinder implements ReflectionUtils.MethodCallback {

    private final String factoryMethodName;

    private ResolvableType result = ResolvableType.NONE;

    FactoryBeanMethodTypeFinder(String factoryMethodName) {
      this.factoryMethodName = factoryMethodName;
    }

    @Override
    public void doWith(Method method) throws IllegalArgumentException {
      if (isFactoryBeanMethod(method)) {
        ResolvableType returnType = ResolvableType.forReturnType(method);
        ResolvableType candidate = returnType.as(FactoryBean.class).getGeneric();
        if (this.result == ResolvableType.NONE) {
          this.result = candidate;
        }
        else {
          Class<?> resolvedResult = this.result.resolve();
          Class<?> commonAncestor = ClassUtils.determineCommonAncestor(candidate.resolve(), resolvedResult);
          if (!ObjectUtils.nullSafeEquals(resolvedResult, commonAncestor)) {
            this.result = ResolvableType.forClass(commonAncestor);
          }
        }
      }
    }

    private boolean isFactoryBeanMethod(Method method) {
      return (method.getName().equals(this.factoryMethodName) &&
              FactoryBean.class.isAssignableFrom(method.getReturnType()));
    }

    ResolvableType getResult() {
      Class<?> resolved = this.result.resolve();
      boolean foundResult = resolved != null && resolved != Object.class;
      return (foundResult ? this.result : ResolvableType.NONE);
    }
  }

}
