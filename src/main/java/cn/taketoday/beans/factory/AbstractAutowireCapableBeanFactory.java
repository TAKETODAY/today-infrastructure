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

package cn.taketoday.beans.factory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import cn.taketoday.beans.ArgumentsResolver;
import cn.taketoday.beans.InitializingBean;
import cn.taketoday.beans.support.BeanInstantiator;
import cn.taketoday.beans.support.BeanMetadata;
import cn.taketoday.beans.support.BeanUtils;
import cn.taketoday.beans.support.PropertyValuesBinder;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.reflect.MethodInvoker;
import cn.taketoday.lang.Component;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;

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
  private static final Logger log = LoggerFactory.getLogger(AbstractAutowireCapableBeanFactory.class);

  /** Whether to automatically try to resolve circular references between beans. */
  private boolean allowCircularReferences = true;

  /**
   * Whether to resort to injecting a raw bean instance in case of circular reference,
   * even if the injected bean eventually got wrapped.
   */
  private boolean allowRawInjectionDespiteWrapping = false;

  /** Cache of unfinished FactoryBean instances: FactoryBean name to its instance. */
  private final ConcurrentHashMap<String, Object> factoryBeanInstanceCache = new ConcurrentHashMap<>();

  //---------------------------------------------------------------------
  // Implementation of AutowireCapableBeanFactory interface
  //---------------------------------------------------------------------

  @Override
  @SuppressWarnings("unchecked")
  public <T> T createBean(Class<T> beanClass, boolean cacheBeanDef) {
    BeanDefinition defToUse;
    if (cacheBeanDef) {
      if ((defToUse = getBeanDefinition(beanClass)) == null) {
        defToUse = getPrototypeBeanDefinition(beanClass);
        registerBeanDefinition(defToUse.getName(), defToUse);
      }
    }
    else {
      defToUse = getPrototypeBeanDefinition(beanClass);
    }
    return (T) createBean(defToUse, null);
  }

  @Override
  public void autowireBean(Object existingBean) {
    Class<Object> userClass = ClassUtils.getUserClass(existingBean);
    BeanDefinition prototypeDef = getPrototypeBeanDefinition(userClass);
    if (log.isDebugEnabled()) {
      log.debug("Autowiring bean '{}'", prototypeDef.getName());
    }

    // apply properties
    populateBean(existingBean, prototypeDef);
  }

  @Override
  protected Object createBean(BeanDefinition definition, @Nullable Object[] args) throws BeanCreationException {
    if (log.isDebugEnabled()) {
      log.debug("Creating instance of bean '{}'", definition.getName());
    }

    Class<?> resolvedClass = resolveBeanClass(definition);
    try {
      // Give BeanPostProcessors a chance to return a proxy instead of the target bean instance.
      Object bean = resolveBeforeInstantiation(resolvedClass, definition);
      if (bean != null) {
        return bean;
      }
    }
    catch (Throwable ex) {
      throw new BeanCreationException(
              definition.getResourceDescription(), definition.getName(),
              "BeanPostProcessor before instantiation of bean failed", ex);
    }

    try {
      Object beanInstance = doCreateBean(definition, args);
      if (log.isDebugEnabled()) {
        log.debug("Finished creating instance of bean '{}'", definition.getName());
      }
      return beanInstance;
    }
    catch (BeanCreationException ex) {
      // A previously detected exception with proper bean creation context already,
      // or illegal singleton state to be communicated up to DefaultSingletonBeanRegistry.
      throw ex;
    }
    catch (Throwable ex) {
      throw new BeanCreationException(
              definition.getResourceDescription(),
              definition.getName(), "Unexpected exception during bean creation", ex);
    }
  }

  /**
   * Actually create the specified bean. Pre-creation processing has already happened
   * at this point, e.g. checking {@code postProcessBeforeInstantiation} callbacks.
   * <p>Differentiates between default bean instantiation, use of a
   * factory method, and autowiring a constructor.
   *
   * @param definition the merged bean definition for the bean
   * @param args explicit arguments to use for constructor or factory method invocation
   * @return a new instance of the bean
   * @throws BeanCreationException if the bean could not be created
   */
  protected Object doCreateBean(BeanDefinition definition, @Nullable Object[] args) throws BeanCreationException {
    Object bean = getObject(definition, args);
    String beanName = definition.getName();

    // Eagerly cache singletons to be able to resolve circular references
    // even when triggered by lifecycle interfaces like BeanFactoryAware.
    boolean earlySingletonExposure = isEarlySingletonExposure(definition, beanName);
    if (earlySingletonExposure) {
      if (log.isTraceEnabled()) {
        log.trace("Eagerly caching bean '{}' to allow for resolving potential circular references", beanName);
      }
      addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, definition, bean));
    }

    Object fullyInitializedBean;
    try {
      // apply properties
      populateBean(bean, definition);
      // Initialize the bean instance.
      fullyInitializedBean = initializeBean(bean, definition);
    }
    catch (Throwable ex) {
      if (ex instanceof BeanCreationException && beanName.equals(((BeanCreationException) ex).getBeanName())) {
        throw (BeanCreationException) ex;
      }
      else {
        throw new BeanCreationException(
                definition.getResourceDescription(), beanName, "Initialization of bean failed", ex);
      }
    }

    if (earlySingletonExposure) {
      Object earlySingletonReference = getSingleton(beanName, false);
      if (earlySingletonReference != null) {
        if (fullyInitializedBean == bean) {
          fullyInitializedBean = earlySingletonReference;
        }
      }
    }

    // Register bean as disposable.
    try {
      registerDisposableBeanIfNecessary(beanName, bean, definition);
    }
    catch (BeanDefinitionValidationException ex) {
      throw new BeanCreationException(
              definition.getResourceDescription(), beanName, "Invalid destruction signature", ex);
    }
    return fullyInitializedBean;
  }

  private Object getObject(BeanDefinition definition, @Nullable Object[] args) {
    Object bean = null;
    if (definition.isSingleton()) {
      bean = this.factoryBeanInstanceCache.remove(definition.getName());
    }

    if (bean == null) {
      bean = createBeanInstance(definition, args);
    }
    return bean;
  }

  private boolean isEarlySingletonExposure(BeanDefinition definition, String beanName) {
    return definition.isSingleton() && this.allowCircularReferences && isSingletonCurrentlyInCreation(beanName);
  }

  /**
   * Obtain a reference for early access to the specified bean,
   * typically for the purpose of resolving a circular reference.
   *
   * @param beanName the name of the bean (for error handling purposes)
   * @param mbd the merged bean definition for the bean
   * @param bean the raw bean instance
   * @return the object to expose as bean reference
   */
  protected Object getEarlyBeanReference(String beanName, BeanDefinition mbd, Object bean) {
    Object exposedObject = bean;
    if (!mbd.isSynthetic()) {
      for (InstantiationAwareBeanPostProcessor bp : postProcessors().instantiation) {
        exposedObject = bp.getEarlyBeanReference(exposedObject, beanName);
      }
    }
    return exposedObject;
  }

  @Override
  public Object initializeBean(Object existingBean) throws BeanInitializationException {
    return initializeBean(existingBean, createBeanName(existingBean.getClass()));
  }

  @Override
  public Object initializeBean(Object existingBean, String beanName) {
    return initializeBean(existingBean, beanName, null);
  }

  @Override
  public Object initializeBean(Object bean, BeanDefinition def) throws BeanInitializationException {
    return initializeBean(bean, def.getName(), def);
  }

  /**
   * Fully initialize the given raw bean, applying factory callbacks such as
   * {@code setBeanName} and {@code setBeanFactory}, also applying all bean post
   * processors (including ones which might wrap the given raw bean).
   * <p>
   * Note that no bean definition of the given name has to exist in the bean
   * factory. The passed-in bean name will simply be used for callbacks but not
   * checked against the registered bean definitions.
   *
   * @param existingBean the existing bean instance
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
  public Object initializeBean(Object existingBean, String beanName, @Nullable BeanDefinition def) throws BeanInitializationException {
    if (log.isDebugEnabled()) {
      log.debug("Initializing bean named '{}'", beanName);
    }
    invokeAwareMethods(existingBean, beanName);
    existingBean = applyBeanPostProcessorsBeforeInitialization(existingBean, beanName);
    // invoke initialize methods
    invokeInitMethods(existingBean, def);
    // after properties
    existingBean = applyBeanPostProcessorsAfterInitialization(existingBean, beanName);
    return existingBean;
  }

  private void invokeAwareMethods(Object bean, String beanName) {
    if (bean instanceof Aware) {
      if (bean instanceof BeanNameAware) {
        ((BeanNameAware) bean).setBeanName(beanName);
      }
      if (bean instanceof BeanClassLoaderAware) {
        ((BeanClassLoaderAware) bean).setBeanClassLoader(getBeanClassLoader());
      }
      if (bean instanceof BeanFactoryAware) {
        ((BeanFactoryAware) bean).setBeanFactory(this);
      }
    }
  }

  /**
   * Invoke initialize methods
   *
   * @param bean Bean instance
   * @param def bean definition
   * @throws BeanInitializationException when invoke init methods
   * @see Component
   * @see InitializingBean
   * @see jakarta.annotation.PostConstruct
   */
  protected void invokeInitMethods(Object bean, @Nullable BeanDefinition def) {
    Method[] methods = initMethodArray(bean, def);
    if (ObjectUtils.isNotEmpty(methods)) {
      ArgumentsResolver resolver = getArgumentsResolver();
      // invoke @PostConstruct or initMethods defined in @Component
      for (Method method : methods) {
        try {
          Object[] args = resolver.resolve(method, this);
          method.invoke(bean, args);
        }
        catch (Exception e) {
          throw new BeanInitializationException(
                  "An Exception Occurred When [" + bean
                          + "] invoke init method: [" + method + "]", e);
        }
      }
    }

    // InitializingBean#afterPropertiesSet
    if (bean instanceof InitializingBean) {
      try {
        ((InitializingBean) bean).afterPropertiesSet();
      }
      catch (Exception e) {
        throw new BeanInitializationException(
                "An Exception Occurred When [" + bean + "] apply after properties", e);
      }
    }
  }

  private Method[] initMethodArray(Object bean, @Nullable BeanDefinition def) {
    if (def != null) {
      Method[] initMethodArray = def.initMethodArray;
      if (def.initMethodArray == null) {
        initMethodArray = BeanDefinitionBuilder.computeInitMethod(def.getInitMethods(), bean.getClass());
        def.initMethodArray = initMethodArray;
      }
      return initMethodArray;
    }
    return BeanDefinitionBuilder.computeInitMethod(null, bean.getClass());
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
   * @param beanClass bean class
   * @param definition the bean definition for the bean
   * @return the shortcut-determined bean instance, or {@code null} if none
   */
  @Nullable
  protected Object resolveBeforeInstantiation(Class<?> beanClass, BeanDefinition definition) {
    Object bean = null;
    if (!Boolean.FALSE.equals(definition.beforeInstantiationResolved)) {
      // Make sure bean class is actually resolved at this point.
      if (!definition.isSynthetic()) {
        bean = applyBeanPostProcessorsBeforeInstantiation(beanClass, definition.getName());
        if (bean != null) {
          bean = applyBeanPostProcessorsAfterInitialization(bean, definition.getName());
        }
      }
      definition.beforeInstantiationResolved = (bean != null);
    }
    return bean;
  }

  /**
   * Apply InstantiationAwareBeanPostProcessors to the specified bean definition
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

  protected Object createBeanInstance(BeanDefinition mbd, @Nullable Object[] args) {
    Supplier<?> instanceSupplier = mbd.getInstanceSupplier();
    if (instanceSupplier != null) {
      return instanceSupplier.get();
    }
    return instantiate(mbd, args);
  }

  /**
   * Create new bean instance
   * <p>
   * Apply before-instantiation post-processors, resolving whether there is a
   * before-instantiation shortcut for the specified bean.
   * </p>
   *
   * @param def Target {@link BeanDefinition} descriptor
   * @return A new bean object
   * @throws BeanInstantiationException When instantiation of a bean failed
   */
  protected Object createBeanInstance(BeanDefinition def) {
    return createBeanInstance(def, null);
  }

  private Object instantiate(BeanDefinition def, @Nullable Object[] constructorArgs) {
    BeanInstantiator instantiator = resolveBeanInstantiator(def);
    if (constructorArgs == null) {
      constructorArgs = def.getConstructorArgs();
      if (constructorArgs == null) {
        constructorArgs = getArgumentsResolver().resolve(def.executable, this);
      }
    }
    return instantiator.instantiate(constructorArgs);
  }

  private BeanInstantiator resolveBeanInstantiator(BeanDefinition definition) {
    if (definition.instantiator == null) {
      String factoryMethodName = definition.getFactoryMethodName();
      // instantiate using factory-method
      if (factoryMethodName != null) {
        String factoryBeanName = definition.getFactoryBeanName();
        Class<?> factoryClass = getFactoryClass(definition, factoryBeanName);
        Method factoryMethod = getFactoryMethod(definition, factoryClass, factoryMethodName);
        MethodInvoker factoryMethodInvoker = determineMethodInvoker(definition, factoryMethod);
        if (Modifier.isStatic(factoryMethod.getModifiers())) {
          definition.instantiator = BeanInstantiator.fromStaticMethod(factoryMethodInvoker);
        }
        else {
          // this is not a FactoryBean just a factory
          Object factoryBean = getBean(factoryBeanName);
          definition.instantiator = BeanInstantiator.fromMethod(factoryMethodInvoker, factoryBean);
        }
        definition.executable = factoryMethod;
      }
      else {
        // use a suitable constructor
        Class<?> beanClass = resolveBeanClass(definition);
        Constructor<?> constructor = BeanUtils.getConstructor(beanClass);
        if (definition.isSingleton()) {
          // use java-reflect invoking
          definition.instantiator = BeanInstantiator.fromReflective(constructor);
        }
        else {
          // provide fast access the method
          definition.instantiator = BeanInstantiator.fromConstructor(constructor);
        }
        definition.executable = constructor;
      }
    }
    return definition.instantiator;
  }

  private MethodInvoker determineMethodInvoker(BeanDefinition definition, Method factoryMethod) {
    if (definition.isSingleton()) {
      // use java-reflect invoking
      return MethodInvoker.formReflective(factoryMethod);
    }
    else {
      // provide fast access the method
      return MethodInvoker.fromMethod(factoryMethod);
    }
  }

  @Override
  public Object autowire(Class<?> beanClass) throws BeansException {
    BeanDefinition prototypeDef = getPrototypeBeanDefinition(beanClass);
    Object existingBean = instantiate(prototypeDef, null);
    populateBean(existingBean, prototypeDef);
    return existingBean;
  }

  @Override
  public Object configureBean(Object existingBean, String beanName) throws BeansException {
    BeanDefinition definition = getBeanDefinition(beanName);
    if (definition == null) {
      definition = getPrototypeBeanDefinition(existingBean.getClass());
    }
    else {
      definition = definition.cloneDefinition();
      definition.setScope(Scope.PROTOTYPE);
    }
    populateBean(existingBean, definition);
    return initializeBean(existingBean, definition);
  }

  public void populateBean(Object bean, BeanDefinition definition) {
    // Give any InstantiationAwareBeanPostProcessors the opportunity to modify the
    // state of the bean before properties are set. This can be used, for example,
    // to support styles of field injection.
    if (!definition.isSynthetic()) {
      String name = definition.getName();
      for (InstantiationAwareBeanPostProcessor processor : postProcessors().instantiation) {
        if (!processor.postProcessAfterInstantiation(bean, name)) {
          return;
        }
      }
    }

    Map<String, Object> propertyValues = definition.getPropertyValues();
    if (CollectionUtils.isNotEmpty(propertyValues)) {
      BeanMetadata metadata = getMetadata(bean, definition);
      PropertyValuesBinder binder = new PropertyValuesBinder(metadata, bean);
      initPropertyValuesBinder(binder);

      // property-path -> property-value (maybe PropertyValueRetriever)
      for (Map.Entry<String, Object> entry : propertyValues.entrySet()) {
        Object value = entry.getValue();
        String propertyPath = entry.getKey();
        if (value instanceof PropertyValueRetriever retriever) {
          value = retriever.retrieve(propertyPath, binder, this);
          if (value == PropertyValueRetriever.DO_NOT_SET) {
            continue;
          }
        }
        binder.setProperty(bean, metadata, propertyPath, value);
      }
    }

    if (definition.isEnableDependencyInjection()) {
      // -----------------------------------------------
      // apply dependency injection (DI)
      // apply outside framework expanded
      // -----------------------------------------------
      for (DependenciesBeanPostProcessor processor : postProcessors().dependencies) {
        processor.postProcessDependencies(bean, definition);
      }
    }
  }

  @NonNull
  private BeanMetadata getMetadata(Object bean, BeanDefinition definition) {
    if (definition.isSingleton()) {
      return new BeanMetadata(bean.getClass());
    }
    return BeanMetadata.ofObject(bean);
  }

  /** @since 4.0 */
  protected void initPropertyValuesBinder(PropertyValuesBinder dataBinder) {
    dataBinder.setConversionService(getConversionService());
  }

  @Override
  public void destroyBean(Object existingBean) {
    new DisposableBeanAdapter(existingBean, postProcessors().destruction).destroy();
  }

  protected BeanDefinition getPrototypeBeanDefinition(Class<?> beanClass) {
    BeanDefinition defaults = BeanDefinitionBuilder.defaults(beanClass);
    defaults.setScope(Scope.PROTOTYPE);
    return defaults;
  }

  //---------------------------------------------------------------------
  // Implementation of AbstractBeanFactory class
  //---------------------------------------------------------------------

  @Override
  @Nullable
  protected Class<?> predictBeanType(BeanDefinition definition) {
    String factoryMethodName = definition.getFactoryMethodName();
    if (factoryMethodName != null) {
      return getTypeForFactoryMethod(definition);
    }
    return resolveBeanClass(definition, true);
  }

  /**
   * Determine the target type for the given bean definition which is based on
   * a factory method. Only called if there is no singleton instance registered
   * for the target bean already.
   * <p>This implementation determines the type matching {@link #createBean}'s
   * different creation strategies. As far as possible, we'll perform static
   * type checking to avoid creation of the target bean.
   *
   * @param def the merged bean definition for the bean
   * @return the type for the bean if determinable, or {@code null} otherwise
   * @see #createBean
   */
  @Nullable
  protected Class<?> getTypeForFactoryMethod(BeanDefinition def) {
    ResolvableType cachedReturnType = def.factoryMethodReturnType;
    if (cachedReturnType != null) {
      return cachedReturnType.resolve();
    }
    Method factoryMethod;
    Executable uniqueCandidate = def.executable;
    if (uniqueCandidate instanceof Method) {
      factoryMethod = ((Method) uniqueCandidate);
    }
    else {
      String factoryBeanName = def.getFactoryBeanName();
      Class<?> factoryClass = getFactoryClass(def, factoryBeanName);
      // If all factory methods have the same return type, return that type.
      // Can't clearly figure out exact method due to type converting / autowiring!
      factoryMethod = getFactoryMethod(def, factoryClass, def.getFactoryMethodName());
      def.executable = factoryMethod;
      return factoryMethod.getReturnType();
    }

    // Common return type found: all factory methods return same type. For a non-parameterized
    // unique candidate, cache the full type declaration context of the target factory method.
    cachedReturnType = ResolvableType.forReturnType(factoryMethod);
    def.factoryMethodReturnType = cachedReturnType;
    return cachedReturnType.resolve();
  }

  @Override
  @SuppressWarnings("unchecked")
  protected <T> FactoryBean<T> getFactoryBean(Class<?> factoryBean, BeanDefinition def) {
    String beanName = def.getName();
    if (def.isSingleton()) {
      synchronized(getSingletonMutex()) {
        Object singleton = factoryBeanInstanceCache.get(beanName);
        if (singleton instanceof FactoryBean factory) {
          return factory;
        }
        singleton = getSingleton(beanName, false);
        if (singleton instanceof FactoryBean factory) {
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
          singleton = resolveBeforeInstantiation(factoryBean, def);
          if (singleton == null) {
            singleton = createBeanInstance(def, null);
          }
          if (singleton != null) {
            factoryBeanInstanceCache.put(beanName, singleton);
          }
          return (FactoryBean<T>) singleton;
        }
        finally {
          // Finished partial creation of this bean.
          afterSingletonCreation(beanName);
        }
      }
    }
    else {
      try {
        // Mark this bean as currently in creation, even if just partially.
        beforePrototypeCreation(beanName);
        // Give BeanPostProcessors a chance to return a proxy instead of the target bean instance.
        Object instance = resolveBeforeInstantiation(factoryBean, def);
        if (instance == null) {
          instance = createBeanInstance(def, null);
        }
        return (FactoryBean<T>) instance;
      }
      finally {
        // Finished partial creation of this bean.
        afterPrototypeCreation(beanName);
      }
    }
  }

  @Override
  public void preInstantiateSingletons() {
    log.debug("Initialization of singleton objects.");
    // Iterate over a copy to allow for init methods which in turn register new bean definitions.
    // While this may not be part of the regular factory bootstrap, it does otherwise work fine.

    String[] beanNames = getBeanDefinitionNames();
    // Trigger initialization of all non-lazy singleton beans...
    for (String beanName : beanNames) {
      BeanDefinition def = obtainBeanDefinition(beanName);
      // Trigger initialization of all non-lazy singleton beans...
      if (def.isSingleton() && !def.isInitialized() && !def.isLazyInit()) {
        if (isFactoryBean(def)) {
          Object bean = getBean(FACTORY_BEAN_PREFIX + beanName);
          if (bean instanceof SmartFactoryBean smartFactory && smartFactory.isEagerInit()) {
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
        smartSingleton.afterSingletonsInstantiated();
      }
    }

    log.debug("The singleton objects are initialized.");
  }

  protected abstract BeanDefinition getBeanDefinition(Class<?> beanClass);

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

}
