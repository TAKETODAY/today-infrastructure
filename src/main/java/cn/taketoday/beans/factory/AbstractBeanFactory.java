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

import cn.taketoday.beans.ArgumentsResolver;
import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.DisposableBean;
import cn.taketoday.beans.FactoryBean;
import cn.taketoday.beans.InitializingBean;
import cn.taketoday.beans.PropertyException;
import cn.taketoday.beans.SmartFactoryBean;
import cn.taketoday.beans.support.PropertyValuesBinder;
import cn.taketoday.context.annotation.BeanDefinitionBuilder;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.core.bytecode.Type;
import cn.taketoday.core.conversion.ConversionException;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.support.DefaultConversionService;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Component;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author TODAY 2018-06-23 11:20:58
 */
public abstract class AbstractBeanFactory
        extends DefaultSingletonBeanRegistry implements ConfigurableBeanFactory, AutowireCapableBeanFactory {
  private static final Logger log = LoggerFactory.getLogger(AbstractBeanFactory.class);

  /** object factories */
  private Map<Class<?>, Object> objectFactories;
  /** Bean Post Processors */
  protected final ArrayList<BeanPostProcessor> postProcessors = new ArrayList<>();
  private final HashMap<String, Scope> scopes = new HashMap<>();

  // @since 2.1.6
  private boolean fullPrototype = false;
  // @since 2.1.6
  private boolean fullLifecycle = false;

  /** Indicates whether any InstantiationAwareBeanPostProcessors have been registered.  @since 3.0 */
  protected boolean hasInstantiationAwareBeanPostProcessors;
  /** @since 4.0 */
  private final ConcurrentHashMap<String, Supplier<?>> beanSupplier = new ConcurrentHashMap<>();

  /** @since 4.0 */
  private ArgumentsResolver argumentsResolver;

  /** Parent bean factory, for bean inheritance support. @since 4.0 */
  @Nullable
  private BeanFactory parentBeanFactory;

  /** ClassLoader to resolve bean class names with, if necessary. @since 4.0 */
  private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

  // @since 4.0 for bean-property conversion
  private ConversionService conversionService;

  // @since 4.0
  private boolean autoInferDestroyMethod = true;

  /**
   * Return whether this factory holds a DestructionAwareBeanPostProcessor
   * that will get applied to singleton beans on shutdown.
   *
   * @since 4.0
   */
  protected boolean hasDestructionBeanPostProcessors;

  //

  //---------------------------------------------------------------------
  // Implementation of BeanFactory interface
  //---------------------------------------------------------------------

  public Object getBean(String name) {
    BeanDefinition def = getBeanDefinition(name);
    if (def != null) {
      return getBean(def);
    }
    // if not exits a bean definition return a bean may exits in singletons cache
    Object singleton = getSingleton(name);
    if (singleton != null) {
      return singleton;
    }
    return handleBeanNotFound(name);
  }

  protected Object handleBeanNotFound(String name) {
    // may exits in bean supplier @since 4.0
    Supplier<?> supplier = beanSupplier.get(name);
    if (supplier != null) {
      return supplier.get();
    }
    BeanFactory parentBeanFactory = getParentBeanFactory();
    if (parentBeanFactory != null) {
      return parentBeanFactory.getBean(name);
    }
    if (objectFactories != null) {
      Object obj = objectFactories.get(name);
      if (obj instanceof Supplier) {
        return ((Supplier<?>) obj).get();
      }
      return obj;
    }
    return null;
  }

  /**
   * @throws IllegalStateException bean definition scope not exist in this bean factory
   */
  @Override
  public Object getBean(BeanDefinition def) {
    if (def.isFactoryBean()) {
      return getFactoryBean(def).getBean();
    }
    if (def.isInitialized()) { // fix #7
      return getSingleton(def.getName());
    }
    BeanDefinition child = def.getChild();

    if (child == null) {
      if (def.isSingleton()) {
        return createSingleton(def);
      }
      else if (def.isPrototype()) {
        return createPrototype(def);
      }
      else {
        Scope scope = scopes.get(def.getScope());
        if (scope == null) {
          throw new IllegalStateException("No such scope: [" + def.getScope() + "] in this " + this);
        }
        return getScopeBean(def, scope);
      }
    }
    else {
      if (def.isPrototype()) {
        return createPrototype(child);
      }
      // initialize child bean
      Object bean = initializeSingleton(getSingleton(def.getName()), child);
      if (!def.isInitialized()) {
        // register as parent bean and set initialize flag
        registerSingleton(def.getName(), bean);
        def.setInitialized(true);
      }
      return bean;
    }
  }

  @Override
  public <T> T getBean(String name, Class<T> requiredType) {
    Object bean = getBean(name);
    return adaptBeanInstance(name, bean, requiredType);
  }

  @SuppressWarnings("unchecked")
  @Nullable
  protected <T> T adaptBeanInstance(String name, Object bean, @Nullable Class<?> requiredType) {
    // Check if required type matches the type of the actual bean instance.
    if (bean != null && requiredType != null && !requiredType.isInstance(bean)) {
      try {
        ConversionService conversionService = getConversionService();
        if (conversionService == null) {
          conversionService = DefaultConversionService.getSharedInstance();
        }
        Object convertedBean = conversionService.convert(bean, requiredType);
        if (convertedBean == null) {
          throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
        }
        return (T) convertedBean;
      }
      catch (ConversionException ex) {
        if (log.isTraceEnabled()) {
          log.trace("Failed to convert bean '{}' to required type '{}'",
                  name, ClassUtils.getQualifiedName(requiredType), ex);
        }
        throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
      }
    }
    return (T) bean;
  }

  @Override
  public boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
    return isTypeMatch(name, ResolvableType.fromClass(typeToMatch));
  }

  @Override
  public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {

    // 单例 父容器
    BeanDefinition beanDefinition = obtainBeanDefinition(name);
    return beanDefinition.isAssignableTo(typeToMatch);
  }

  @Override
  public <T> ObjectSupplier<T> getObjectSupplier(ResolvableType requiredType) {
    return getObjectSupplier(requiredType, true, true);
  }

  @Override
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public <T> ObjectSupplier<T> getObjectSupplier(
          ResolvableType requiredType, boolean includeNoneRegistered, boolean includeNonSingletons) {
    if (requiredType.isArray()) {
      // Bean[] beans
      ResolvableType type = requiredType.getComponentType();
      if (type == ResolvableType.NONE) {
        throw new IllegalArgumentException("cannot determine bean type");
      }
      // not supports iteration, stream
      return new AbstractResolvableTypeObjectSupplier(type, includeNoneRegistered, includeNonSingletons) {

        @Override
        Object getIfAvailable(ResolvableType requiredType, boolean nonRegistered, boolean nonSingletons) {
          Map<String, Object> beansOfType = getBeansOfType(requiredType, nonRegistered, nonSingletons);
          if (beansOfType.isEmpty()) {
            return Array.newInstance(requiredType.resolve(), 0);
          }
          Object array = Array.newInstance(requiredType.resolve(), beansOfType.size());
          return beansOfType.values().toArray((Object[]) array);
        }
      };
    }

    if (requiredType.isMap()) {
      ResolvableType type = requiredType.asMap().getGeneric(1);
      if (type == ResolvableType.NONE) {
        throw new IllegalArgumentException("cannot determine bean type");
      }
      // not supports iteration, stream
      return new AbstractResolvableTypeObjectSupplier(type, includeNoneRegistered, includeNonSingletons) {

        @Override
        Object getIfAvailable(ResolvableType requiredType, boolean nonRegistered, boolean nonSingletons) {
          return getBeansOfType(requiredType, nonRegistered, nonSingletons);
        }
      };
    }

    if (requiredType.isCollection()) {
      ResolvableType type = requiredType.asCollection().getGeneric(0);
      if (type == ResolvableType.NONE) {
        throw new IllegalArgumentException("cannot determine bean type");
      }
      // not supports iteration, stream
      return new AbstractResolvableTypeObjectSupplier(type, includeNoneRegistered, includeNonSingletons) {

        @Override
        Object getIfAvailable(ResolvableType requiredType, boolean nonRegistered, boolean nonSingletons) {
          Map<String, Object> beansOfType = getBeansOfType(requiredType, nonRegistered, nonSingletons);
          Collection<Object> ret = CollectionUtils.createCollection(requiredType.resolve());
          if (beansOfType.isEmpty()) {
            return ret;
          }

          ret.addAll(beansOfType.values());
          return ret;
        }
      };
    }

    // find like Bean<String>
    return new ResolvableTypeObjectSupplier<T>(this, requiredType, includeNoneRegistered, includeNonSingletons);
  }

  static boolean isInstance(ResolvableType type, Object obj) {
    return obj != null && type.isAssignableFrom(ClassUtils.getUserClass(obj));
  }

  /**
   * Create bean instance if necessary
   * <p>
   * <b> Note </b> If target bean is {@link Scope#SINGLETON} will be register is
   * to the singletons pool
   * </p>
   * <p>
   * Other scope will create directly
   * </p>
   *
   * @param def Bean definition
   * @return Target bean instance
   * @throws BeanInstantiationException When instantiation of a bean failed
   */
  protected Object createBeanIfNecessary(BeanDefinition def) {
    if (def.isSingleton()) {
      String name = def.getName();
      Object bean = getSingleton(name);
      if (bean == null) {
        bean = createBeanInstance(def);
        registerSingleton(name, bean);
      }
      return bean;
    }
    else {
      return createBeanInstance(def);
    }
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
    if (hasInstantiationAwareBeanPostProcessors) {
      for (BeanPostProcessor processor : postProcessors) {
        if (processor instanceof InstantiationAwareBeanPostProcessor) {
          Object bean = ((InstantiationAwareBeanPostProcessor) processor).postProcessBeforeInstantiation(def);
          if (bean != null) {
            return bean;
          }
        }
      }
    }
    return def.newInstance(this);
  }

  /**
   * Resolve the bean class for the specified bean definition,
   * resolving a bean class name into a Class reference (if necessary)
   * and storing the resolved Class in the bean definition for further use.
   *
   * @param def the merged bean definition to determine the class for
   * @return the resolved bean class (or {@code null} if none)
   * @throws BeanClassLoadFailedException if we failed to load the class
   */
  @Nullable
  protected Class<?> resolveBeanClass(BeanDefinition def) throws BeanClassLoadFailedException {
    if (def.hasBeanClass()) {
      return def.getBeanClass();
    }

    try {
      ClassLoader beanClassLoader = getBeanClassLoader();
      if (def instanceof DefaultBeanDefinition) {
        Class<?> beanClass = ((DefaultBeanDefinition) def).resolveBeanClass(beanClassLoader);
        if (beanClass != null) {
          return beanClass;
        }
      }
      String beanClassName = def.getBeanClassName();
      return ClassUtils.forName(beanClassName, beanClassLoader);
    }
    catch (ClassNotFoundException ex) {
      throw new BeanClassLoadFailedException(def, ex);
    }
    catch (LinkageError err) {
      throw new BeanClassLoadFailedException(def, err);
    }
  }

  protected Object createBeanInstance(BeanDefinition def, @Nullable Object[] args) {
    return def.newInstance(this, args);
  }

  /**
   * Apply property values.
   *
   * @param bean Bean instance
   * @param def use {@link BeanDefinition}
   * @throws PropertyException If any {@link Exception} occurred when apply
   * {@link PropertySetter}
   * @throws NoSuchBeanDefinitionException If BeanReference is required and there isn't a bean in
   * this {@link BeanFactory}
   */
  protected void applyPropertyValues(Object bean, BeanDefinition def) {
    Set<PropertySetter> propertySetters = null;
    if (!def.isSynthetic() && hasInstantiationAwareBeanPostProcessors) {
      String beanName = def.getName();
      for (BeanPostProcessor postProcessor : postProcessors) {
        if (postProcessor instanceof InstantiationAwareBeanPostProcessor) {
          Set<PropertySetter> ret = ((InstantiationAwareBeanPostProcessor) postProcessor).postProcessPropertyValues(bean, beanName);
          if (CollectionUtils.isNotEmpty(ret)) {
            if (propertySetters == null) {
              propertySetters = new LinkedHashSet<>();
            }
            propertySetters.addAll(ret);
          }
        }
      }
    }

    // apply simple property-values
    Set<PropertyValue> propertyValues = def.getPropertyValues();
    if (CollectionUtils.isNotEmpty(propertyValues)) {
      PropertyValuesBinder dataBinder = new PropertyValuesBinder(bean);
      initPropertyValuesBinder(dataBinder);
      dataBinder.bind(bean, propertyValues);
    }

    if (CollectionUtils.isNotEmpty(propertySetters)) {
      for (PropertySetter propertySetter : propertySetters) {
        propertySetter.applyValue(bean, this);
      }
    }

  }

  /** @since 4.0 */
  protected void initPropertyValuesBinder(PropertyValuesBinder dataBinder) {
    dataBinder.setConversionService(getConversionService());
  }

  /**
   * Invoke initialize methods
   *
   * @param bean Bean instance
   * @param def bean definition
   * @throws BeanInitializingException when invoke init methods
   * @see Component
   * @see InitializingBean
   * @see javax.annotation.PostConstruct
   */
  protected void invokeInitMethods(Object bean, BeanDefinition def) {
    String[] initMethods = def.getInitMethods();
    Method[] methods = BeanDefinitionBuilder.computeInitMethod(initMethods, bean.getClass());

    if (ObjectUtils.isNotEmpty(methods)) {
      ArgumentsResolver resolver = getArgumentsResolver();
      // invoke @PostConstruct or initMethods defined in @Component
      for (Method method : methods) {
        try {
          Object[] args = resolver.resolve(method, this);
          method.invoke(bean, args);
        }
        catch (Exception e) {
          throw new BeanInitializingException(
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
        throw new BeanInitializingException(
                "An Exception Occurred When [" + bean + "] apply after properties", e);
      }
    }
  }

  /**
   * Create prototype bean instance.
   *
   * @param def Bean definition
   * @return A initialized Prototype bean instance
   * @throws BeanInstantiationException If any {@link Exception} occurred when create prototype
   */
  protected Object createPrototype(BeanDefinition def) {
    return initializeBean(createBeanInstance(def), def); // initialize
  }

  /**
   * Get initialized {@link FactoryBean}
   *
   * @param def Target {@link BeanDefinition}
   * @return Initialized {@link FactoryBean} never be null
   * @throws BeanInstantiationException If any {@link Exception} occurred when get FactoryBean
   */
  @SuppressWarnings("unchecked")
  protected <T> FactoryBean<T> getFactoryBean(BeanDefinition def) {
    FactoryBean<T> factoryBean = getFactoryBeanInstance(def);
    if (def.isInitialized()) {
      return factoryBean;
    }
    if (factoryBean instanceof AbstractFactoryBean) {
      ((AbstractFactoryBean<?>) factoryBean).setSingleton(def.isSingleton());
    }
    // Initialize Factory
    // Factory is always a SINGLETON bean
    // ----------------------------------------
    if (log.isDebugEnabled()) {
      log.debug("Initialize FactoryBean: [{}]", def.getName());
    }
    Object initBean = initializeBean(factoryBean, def);
    def.setInitialized(true);
    registerSingleton(getFactoryBeanName(def), initBean); // Refresh bean to the mapping
    return (FactoryBean<T>) initBean;
  }

  /**
   * Get {@link FactoryBean} object
   *
   * @param <T> Target bean {@link Type}
   * @param def Target bean definition
   * @return {@link FactoryBean} object
   */
  @SuppressWarnings("unchecked")
  protected <T> FactoryBean<T> getFactoryBeanInstance(BeanDefinition def) {
    if (def instanceof FactoryBeanDefinition) {
      return ((FactoryBeanDefinition<T>) def).getFactory();
    }
    Object factory = getSingleton(getFactoryBeanName(def));
    if (factory instanceof FactoryBean) {
      // has already exits factory
      return (FactoryBean<T>) factory;
    }
    factory = createBeanInstance(def);
    if (factory instanceof FactoryBean) {
      return (FactoryBean<T>) factory;
    }
    throw new IllegalStateException("object must be FactoryBean");
  }

  /**
   * Get {@link FactoryBean} bean name
   *
   * @param def Target {@link FactoryBean} {@link BeanDefinition}
   * @return The name of target factory in this {@link BeanFactory}
   */
  protected String getFactoryBeanName(BeanDefinition def) {
    return FACTORY_BEAN_PREFIX.concat(def.getName());
  }

  @Override
  public Object getScopeBean(BeanDefinition def, Scope scope) {
    return scope.get(def, this::createPrototype);
  }

  /**
   * Initialize the given bean instance, applying factory callbacks
   * as well as init methods and bean post processors.
   * <p>Called from {@link #createBean} for traditionally defined beans,
   * and from {@link #initializeBean} for existing bean instances.
   *
   * @param bean the new bean instance we may need to initialize
   * @param def Bean definition
   * @return the initialized bean instance
   * @throws BeanInitializingException If any {@link Exception} occurred when initialize bean
   * @see BeanNameAware
   * @see BeanClassLoaderAware
   * @see BeanFactoryAware
   * @see #applyBeanPostProcessorsBeforeInitialization
   * @see #invokeInitMethods
   * @see #applyBeanPostProcessorsAfterInitialization
   */
  @Override
  public Object initializeBean(Object bean, BeanDefinition def) {
    if (log.isDebugEnabled()) {
      log.debug("Initializing bean named: [{}].", def.getName());
    }
    invokeAwareMethods(bean, def);
    Object ret = bean;
    // before properties
    for (BeanPostProcessor processor : postProcessors) {
      try {
        ret = processor.postProcessBeforeInitialization(ret, def);
      }
      catch (Exception e) {
        throw new BeanInitializingException(
                "An Exception Occurred When [" + bean + "] before properties set", e);
      }
    }
    // apply properties
    applyPropertyValues(ret, def);
    // invoke initialize methods
    invokeInitMethods(ret, def);
    // after properties
    for (BeanPostProcessor processor : postProcessors) {
      try {
        ret = processor.postProcessAfterInitialization(ret, def);
      }
      catch (Exception e) {
        throw new BeanInitializingException(
                "An Exception Occurred When [" + bean + "] after properties set", e);
      }
    }
    return ret;
  }

  private void invokeAwareMethods(Object bean, BeanDefinition def) {
    if (bean instanceof Aware) {
      if (bean instanceof BeanNameAware) {
        ((BeanNameAware) bean).setBeanName(def.getName());
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
   * Initialize a singleton bean with given name and it's definition.
   * <p>
   * Bean definition must be a singleton
   * </p>
   * this method will apply {@link BeanDefinition}'s 'initialized' property and
   * register is bean instance to the singleton pool
   * <p>
   * If the input bean is {@code null} then use
   * {@link #createSingleton(BeanDefinition)} To initialize singleton
   *
   * @param bean Input old bean
   * @param def Bean definition
   * @return A initialized singleton bean
   * @throws BeanInstantiationException When instantiation of a bean failed
   * @see #createSingleton(BeanDefinition)
   */
  protected Object initializeSingleton(Object bean, BeanDefinition def) {
    if (bean == null) {
      return createSingleton(def);
    }
    Assert.isTrue(def.isSingleton(), "Bean definition must be a singleton");
    if (def.isInitialized()) { // fix #7
      return bean;
    }
    Object afterInit = initializeBean(bean, def);
    if (afterInit != bean) {
      registerSingleton(def.getName(), afterInit);
    }
    // apply this bean definition's 'initialized' property
    def.setInitialized(true);
    return afterInit;
  }

  /**
   * Initialize a singleton bean with given name and it's definition.
   * <p>
   * Bean definition must be a singleton
   * </p>
   * this method will apply {@link BeanDefinition}'s 'initialized' property and
   * register is bean instance to the singleton pool
   *
   * @param def Bean definition
   * @return A initialized singleton bean
   * @throws BeanInstantiationException When instantiation of a bean failed
   */
  protected Object createSingleton(BeanDefinition def) {
    Assert.isTrue(def.isSingleton(), "Bean definition must be a singleton");

    if (def.isFactoryBean()) {
      Object bean = getFactoryBean(def).getBean();
      if (!containsSingleton(def.getName())) {
        registerSingleton(def.getName(), bean);
        def.setInitialized(true);
      }
      return bean;
    }

    if (def.isInitialized()) { // fix #7
      return getSingleton(def.getName());
    }

    Object bean = createBeanIfNecessary(def);
    // After initialization
    Object afterInit = initializeBean(bean, def);

    if (afterInit != bean) {
      registerSingleton(def.getName(), afterInit);
    }
    def.setInitialized(true);

    return afterInit;
  }

  /**
   * Determine whether the given bean requires destruction on shutdown.
   * <p>The default implementation checks the DisposableBean interface as well as
   * a specified destroy method and registered DestructionAwareBeanPostProcessors.
   *
   * @param bean the bean instance to check
   * @param mbd the corresponding bean definition
   * @see DisposableBean
   * @see DestructionBeanPostProcessor
   */
  protected boolean requiresDestruction(Object bean, BeanDefinition mbd) {
    if (DisposableBeanAdapter.hasDestroyMethod(bean, mbd)) {
      if (hasDestructionBeanPostProcessors) {
        if (!CollectionUtils.isEmpty(postProcessors)) {
          for (BeanPostProcessor processor : postProcessors) {
            if (processor instanceof DestructionBeanPostProcessor) {
              if (((DestructionBeanPostProcessor) processor).requiresDestruction(bean)) {
                return true;
              }
            }
          }
        }
      }
    }
    return false;
  }

  /**
   * Add the given bean to the list of disposable beans in this factory,
   * registering its DisposableBean interface and/or the given destroy method
   * to be called on factory shutdown (if applicable). Only applies to singletons.
   *
   * @param beanName the name of the bean
   * @param bean the bean instance
   * @param mbd the bean definition for the bean
   * @see BeanDefinition#isSingleton
   * @see #registerDisposableBean
   */
  protected void registerDisposableBeanIfNecessary(String beanName, Object bean, BeanDefinition mbd) {
    if (!mbd.isPrototype() && requiresDestruction(bean, mbd)) {
      if (mbd.isSingleton()) {
        // Register a DisposableBean implementation that performs all destruction
        // work for the given bean: DestructionAwareBeanPostProcessors,
        // DisposableBean interface, custom destroy method.
        registerDisposableBean(beanName, new DisposableBeanAdapter(
                autoInferDestroyMethod,
                bean, mbd, DisposableBeanAdapter.getFilteredPostProcessors(bean, postProcessors)));
      }
      else {
        // A bean with a custom scope...
        Scope scope = this.scopes.get(mbd.getScope());
        if (scope == null) {
          throw new IllegalStateException("No Scope registered for scope name '" + mbd.getScope() + "'");
        }
        scope.registerDestructionCallback(
                beanName, new DisposableBeanAdapter(
                        autoInferDestroyMethod, bean, mbd,
                        DisposableBeanAdapter.getFilteredPostProcessors(bean, postProcessors)));
      }
    }
  }

  @Override
  public abstract Map<String, BeanDefinition> getBeanDefinitions();

  /**
   * register bean-def for
   */
  protected abstract void registerBeanDefinition(String beanName, BeanDefinition def);

  /**
   * Get object {@link Supplier}s
   *
   * @return object {@link Supplier}s
   * @since 2.3.7
   */
  public final Map<Class<?>, Object> getObjectFactories() {
    if (objectFactories == null) {
      objectFactories = createObjectFactories();
    }
    return objectFactories;
  }

  @Override
  public void registerResolvableDependency(Class<?> dependencyType, @Nullable Object autowiredValue) {
    getObjectFactories().put(dependencyType, autowiredValue);
  }

  protected Map<Class<?>, Object> createObjectFactories() {
    return new HashMap<>();
  }

  public void setObjectFactories(Map<Class<?>, Object> objectFactories) {
    this.objectFactories = objectFactories;
  }

  // ---------------------------------------

  @Override
  public boolean isSingleton(String name) {
    BeanDefinition def = getBeanDefinition(name);
    if (def == null) {
      if (getSingleton(name) == null) {
        throw new NoSuchBeanDefinitionException(name);
      }
      return true;
    }
    return def.isSingleton();
  }

  /**
   * @throws NoSuchBeanDefinitionException bean-definition not found
   */
  public BeanDefinition obtainBeanDefinition(String name) {
    BeanDefinition def = getBeanDefinition(name);
    if (def == null) {
      throw new NoSuchBeanDefinitionException(name);
    }
    return def;
  }

  @Override
  public boolean isPrototype(String name) {
    return !isSingleton(name);
  }

  @Override
  public Class<?> getType(String beanName) {
    // Check manually registered singletons.
    Object beanInstance = getSingleton(beanName);
    if (beanInstance != null) {
      if (beanInstance instanceof FactoryBean && !BeanFactoryUtils.isFactoryDereference(beanName)) {
        return getTypeForFactoryBean((FactoryBean<?>) beanInstance);
      }
      else {
        return beanInstance.getClass();
      }
    }

    // No singleton instance found -> check bean definition.
    BeanFactory parentBeanFactory = getParentBeanFactory();
    if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
      // No bean definition found in this factory -> delegate to parent.
      return parentBeanFactory.getType(beanName);
    }

    BeanDefinition definition = obtainBeanDefinition(beanName);
    return predictBeanType(definition);
  }

  /**
   * Determine the type for the given FactoryBean.
   *
   * @param factoryBean the FactoryBean instance to check
   * @return the FactoryBean's object type,
   * or {@code null} if the type cannot be determined yet
   */
  @Nullable
  protected Class<?> getTypeForFactoryBean(FactoryBean<?> factoryBean) {
    try {
      return factoryBean.getBeanClass();
    }
    catch (Throwable ex) {
      // Thrown from the FactoryBean's getObjectType implementation.
      log.info("FactoryBean threw exception from getObjectType, despite the contract saying " +
              "that it should return null if the type of its object cannot be determined yet", ex);
      return null;
    }
  }

  /**
   * Predict the eventual bean type (of the processed bean instance) for the
   * specified bean. Called by {@link #getType} and {@link #isTypeMatch}.
   * Does not need to handle FactoryBeans specifically, since it is only
   * supposed to operate on the raw bean type.
   * <p>This implementation is simplistic in that it is not able to
   * handle factory methods and InstantiationAwareBeanPostProcessors.
   * It only predicts the bean type correctly for a standard bean.
   * To be overridden in subclasses, applying more sophisticated type detection.
   *
   * @param definition the bean definition to determine the type for
   * @return the type of the bean, or {@code null} if not predictable
   */
  @Nullable
  protected Class<?> predictBeanType(BeanDefinition definition) {
    if (definition instanceof FactoryBeanDefinition) {
      return definition.getBeanClass();
    }
    if (definition instanceof FactoryMethodBeanDefinition) {
      Method factoryMethod = ((FactoryMethodBeanDefinition) definition).getFactoryMethod();
      return factoryMethod.getReturnType();
    }
    return resolveBeanClass(definition);
  }

  @Override
  public Set<String> getAliases(Class<?> type) {
    return getBeanDefinitions()
            .entrySet()
            .stream()
            .filter(entry -> type.isAssignableFrom(entry.getValue().getBeanClass()))
            .map(Entry::getKey)
            .collect(Collectors.toSet());
  }

  @Override
  public String getBeanName(Class<?> targetClass) {
    String[] beanNames = getBeanDefinitionNames();
    for (String beanName : beanNames) {
      if (isTypeMatch(beanName, targetClass)) {
        return beanName;
      }
    }
    throw new NoSuchBeanDefinitionException(targetClass);
  }

  @Override
  public boolean containsBean(String beanName) {
    if (containsLocalBean(beanName)) {
      return true;
    }
    // Not found -> check parent.
    BeanFactory parentBeanFactory = getParentBeanFactory();
    return parentBeanFactory != null && parentBeanFactory.containsBean(beanName);
  }

  // -----------------------------

  public final List<BeanPostProcessor> getPostProcessors() {
    return postProcessors;
  }

  @Override
  public boolean isFullPrototype() {
    return fullPrototype;
  }

  @Override
  public boolean isFullLifecycle() {
    return fullLifecycle;
  }

  //---------------------------------------------------------------------
  // Listing Get operations for type-lookup
  //---------------------------------------------------------------------

  @Override
  @SuppressWarnings("unchecked")
  public <T> ObjectSupplier<T> getObjectSupplier(final BeanDefinition def) {
    Assert.notNull(def, "BeanDefinition must not be null");

    if (def.isSingleton()) {
      final class SingletonObjectSupplier implements ObjectSupplier<T> {
        volatile T targetSingleton;

        @Override
        public T getIfAvailable() throws BeansException {
          T ret = targetSingleton;
          if (ret == null) {
            synchronized(this) {
              ret = targetSingleton;
              if (ret == null) {
                ret = targetSingleton = (T) getBean(def);
              }
            }
          }
          return ret;
        }

        @Override
        public Iterator<T> iterator() {
          return CollectionUtils.singletonIterator(get());
        }

        @Override
        public T get() { return getIfAvailable(); }

        @Override
        public Stream<T> orderedStream() { return stream(); }

        @Override
        public Stream<T> stream() { return Stream.of(targetSingleton); }
      }
      return new SingletonObjectSupplier();
    }

    return new DefaultObjectSupplier<T>(def.getBeanClass(), this) {

      @Override
      public T getIfAvailable() throws BeansException {
        return (T) getBean(def);
      }
    };
  }

  @Override
  public <T> ObjectSupplier<T> getObjectSupplier(Class<T> requiredType) {
    Assert.notNull(requiredType, "requiredType must not be null");
    return new DefaultObjectSupplier<>(requiredType, this);
  }

  @Override
  public <T> Map<String, T> getBeansOfType(Class<T> requiredType, boolean includeNonSingletons) {
    return getBeansOfType(requiredType, true, includeNonSingletons);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Map<String, T> getBeansOfType(
          Class<T> requiredType, boolean includeNoneRegistered, boolean includeNonSingletons) {
    HashMap<String, T> beans = new HashMap<>();
    for (Entry<String, BeanDefinition> entry : getBeanDefinitions().entrySet()) {
      BeanDefinition def = entry.getValue();
      if (isEligibleBean(def, requiredType, includeNonSingletons)) {
        Object bean = getBean(def);
        if (bean != null) {
          beans.put(entry.getKey(), (T) bean);
        }
      }
    }

    if (includeNoneRegistered) {
      for (Entry<String, Object> entry : getSingletons().entrySet()) {
        Object bean = entry.getValue();
        if (!beans.containsKey(entry.getKey())
                && (requiredType == null || requiredType.isInstance(bean))) {
          beans.put(entry.getKey(), (T) bean);
        }
      }
    }
    return beans;
  }

  /**
   * Return bean matching the given type (including subclasses), judging from bean definitions
   *
   * @param def the BeanDefinition to check
   * @param requiredType the class or interface to match, or {@code null} for all bean names
   * @param includeNonSingletons whether to include prototype or scoped beans too
   * or just singletons (also applies to FactoryBeans)
   * @return the bean matching the given object type (including subclasses)
   */
  protected boolean isEligibleBean(BeanDefinition def, Class<?> requiredType, boolean includeNonSingletons) {
    if (!(includeNonSingletons || def.isSingleton())) {
      return false;
    }

    if (requiredType != null) {
      Class<?> type = getType(def.getName());
      if (type != null) {
        return requiredType.isAssignableFrom(type);
      }
      return false;
    }
    return true;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Map<String, T> getBeansOfType(
          @Nullable ResolvableType requiredType, boolean includeNoneRegistered, boolean includeNonSingletons) {
    HashMap<String, T> beans = new HashMap<>();
    for (Entry<String, BeanDefinition> entry : getBeanDefinitions().entrySet()) {
      BeanDefinition def = entry.getValue();
      if (includeNonSingletons || def.isSingleton()) {
        if (requiredType == null || def.isAssignableTo(requiredType)) {
          Object bean = getBean(def);
          if (bean != null) {
            beans.put(entry.getKey(), (T) bean);
          }
        }
      }
    }

    if (includeNoneRegistered) {
      for (Entry<String, Object> entry : getSingletons().entrySet()) {
        Object bean = entry.getValue();
        if (!beans.containsKey(entry.getKey())
                && (requiredType == null || isInstance(requiredType, bean))) {
          beans.put(entry.getKey(), (T) bean);
        }
      }
    }
    return beans;
  }

  @SuppressWarnings("unchecked")
  public <T> void getBeansOfType(
          @Nullable ResolvableType requiredType,
          boolean includeNoneRegistered, boolean includeNonSingletons, BiConsumer<String, T> consumer) {
    HashSet<String> accepted = new HashSet<>();
    for (Entry<String, BeanDefinition> entry : getBeanDefinitions().entrySet()) {
      BeanDefinition def = entry.getValue();
      if (includeNonSingletons || def.isSingleton()) {
        if (requiredType == null || def.isAssignableTo(requiredType)) {
          Object bean = getBean(def);
          if (bean != null) {
            accepted.add(entry.getKey());
            consumer.accept(entry.getKey(), (T) bean);
          }
        }
      }
    }

    if (includeNoneRegistered) {
      for (Entry<String, Object> entry : getSingletons().entrySet()) {
        Object bean = entry.getValue();
        if (!accepted.contains(entry.getKey())
                && (requiredType == null || isInstance(requiredType, bean))) {
          consumer.accept(entry.getKey(), (T) bean);
        }
      }
    }
  }

  @Override
  public Set<String> getBeanNamesOfType(
          @Nullable ResolvableType requiredType, boolean includeNoneRegistered, boolean includeNonSingletons) {
    LinkedHashSet<String> beanNames = new LinkedHashSet<>();

    for (Entry<String, BeanDefinition> entry : getBeanDefinitions().entrySet()) {
      BeanDefinition def = entry.getValue();
      if (includeNonSingletons || def.isSingleton()) {
        if (requiredType == null || def.isAssignableTo(requiredType)) {
          beanNames.add(entry.getKey());
        }
      }
    }

    if (includeNoneRegistered) {
      synchronized(getSingletons()) {
        for (Entry<String, Object> entry : getSingletons().entrySet()) {
          Object bean = entry.getValue();
          if (requiredType == null || isInstance(requiredType, bean)) {
            beanNames.add(entry.getKey());
          }
        }
      }
    }
    return beanNames;
  }

  //---------------------------------------------------------------------
  // Implementation of ArgumentsResolverProvider interface
  //---------------------------------------------------------------------

  /** @since 4.0 */
  @NonNull
  @Override
  public ArgumentsResolver getArgumentsResolver() {
    if (argumentsResolver == null) {
      this.argumentsResolver = new ArgumentsResolver(this);
    }
    return argumentsResolver;
  }

  //---------------------------------------------------------------------
  // Implementation of HierarchicalBeanFactory interface
  //---------------------------------------------------------------------

  @Override
  @Nullable
  public BeanFactory getParentBeanFactory() {
    return this.parentBeanFactory;
  }

  @Override
  public boolean containsLocalBean(String beanName) {
    return containsSingleton(beanName) || containsBeanDefinition(beanName);
  }

  //---------------------------------------------------------------------
  // Implementation of ConfigurableBeanFactory interface
  //---------------------------------------------------------------------

  @Override
  public void removeBean(String name) {
    removeSingleton(name);
  }

  @Override
  public void removeBean(Class<?> beanClass) {
    Set<String> beanNamesOfType = getBeanNamesOfType(beanClass, true, true);
    for (String name : beanNamesOfType) {
      removeBean(name);
    }
  }

  @Override
  public void destroyBean(String name) {
    destroyBean(name, getSingleton(name));
  }

  @Override
  public void destroyBean(String name, Object beanInstance) {
    BeanDefinition def = getBeanDefinition(name);
    if (def == null && name.charAt(0) == FACTORY_BEAN_PREFIX_CHAR) {
      // if it is a factory bean
      String factoryBeanName = name.substring(1);
      def = getBeanDefinition(factoryBeanName);
      if (def != null) {
        destroyBean(getSingleton(factoryBeanName), def);
      }
    }

    if (def == null) {
      def = getPrototypeBeanDefinition(ClassUtils.getUserClass(beanInstance));
    }
    destroyBean(beanInstance, def);
  }

  protected abstract BeanDefinition getPrototypeBeanDefinition(Class<?> userClass);

  public <T> void registerBean(String name, Supplier<T> supplier) throws BeanDefinitionStoreException {
    Assert.notNull(name, "bean-name must not be null");
    Assert.notNull(supplier, "bean-instance-supplier must not be null");
    beanSupplier.put(name, supplier);
  }

  /**
   * Destroy a bean with bean instance and bean definition
   *
   * @param beanInstance Bean instance
   * @param def Bean definition
   */
  @Override
  public void destroyBean(Object beanInstance, BeanDefinition def) {
    if (beanInstance == null || def == null) {
      return;
    }
    DisposableBeanAdapter.destroyBean(beanInstance, def, postProcessors);
  }

  @Override
  public void initializeSingletons() {
    log.debug("Initialization of singleton objects.");
    for (BeanDefinition def : getBeanDefinitions().values()) {
      // Trigger initialization of all non-lazy singleton beans...
      if (def.isSingleton() && !def.isInitialized() && !def.isLazyInit()) {
        if (def.isFactoryBean()) {
          FactoryBean<?> factoryBean = getFactoryBeanInstance(def);
          boolean isEagerInit = factoryBean instanceof SmartFactoryBean
                  && ((SmartFactoryBean<?>) factoryBean).isEagerInit();
          if (isEagerInit) {
            getBean(def);
          }
        }
        else {
          createSingleton(def);
        }
      }
    }

    // Trigger post-initialization callback for all applicable beans...
    for (Object singleton : getSingletons().values()) {
      postSingletonInitialization(singleton);
    }

    log.debug("The singleton objects are initialized.");
  }

  protected void postSingletonInitialization(Object singleton) {
    // SmartInitializingSingleton
    if (singleton instanceof SmartInitializingSingleton) {
      ((SmartInitializingSingleton) singleton).afterSingletonsInstantiated();
    }
  }

  /**
   * Initialization singletons that has already in context
   */
  public void preInitialization() {
    boolean debugEnabled = log.isDebugEnabled();

    for (Entry<String, Object> entry : new HashMap<>(getSingletons()).entrySet()) {

      String name = entry.getKey();
      BeanDefinition def = getBeanDefinition(name);
      if (def == null || def.isInitialized()) {
        continue;
      }
      initializeSingleton(entry.getValue(), def);
      if (debugEnabled) {
        log.debug("Pre initialize singleton bean is being stored in the name of [{}].", name);
      }
    }
  }

  // BeanPostProcessor

  @Override
  public void addBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
    Assert.notNull(beanPostProcessor, "BeanPostProcessor must not be null");
    postProcessors.remove(beanPostProcessor);
    postProcessors.add(beanPostProcessor);

    AnnotationAwareOrderComparator.sort(postProcessors);

    // Track whether it is instantiation/destruction aware
    if (beanPostProcessor instanceof InstantiationAwareBeanPostProcessor) {
      this.hasInstantiationAwareBeanPostProcessors = true;
    }

    if (beanPostProcessor instanceof DestructionBeanPostProcessor) {
      this.hasDestructionBeanPostProcessors = true;
    }
  }

  /**
   * Add new BeanPostProcessors that will get applied to beans created
   * by this factory. To be invoked during factory configuration.
   *
   * @see #addBeanPostProcessor
   * @since 4.0
   */
  public void addBeanPostProcessors(Collection<? extends BeanPostProcessor> beanPostProcessors) {
    this.postProcessors.removeAll(beanPostProcessors);
    this.postProcessors.addAll(beanPostProcessors);
    AnnotationAwareOrderComparator.sort(postProcessors);
  }

  @Override
  public void removeBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
    postProcessors.remove(beanPostProcessor);

    for (BeanPostProcessor postProcessor : postProcessors) {
      if (postProcessor instanceof InstantiationAwareBeanPostProcessor) {
        this.hasInstantiationAwareBeanPostProcessors = true;
      }
      else if (beanPostProcessor instanceof DestructionBeanPostProcessor) {
        this.hasDestructionBeanPostProcessors = true;
      }
    }
  }

  @Override
  public int getBeanPostProcessorCount() {
    return this.postProcessors.size();
  }

  @Override
  public void setFullPrototype(boolean fullPrototype) {
    this.fullPrototype = fullPrototype;
  }

  @Override
  public void setFullLifecycle(boolean fullLifecycle) {
    this.fullLifecycle = fullLifecycle;
  }

  @Override
  public void registerScope(String scopeName, Scope scope) {
    Assert.notNull(scopeName, "scope name must not be null");
    Assert.notNull(scope, "scope object must not be null");
    if (Scope.SINGLETON.equals(scopeName) || Scope.PROTOTYPE.equals(scopeName)) {
      throw new IllegalArgumentException("Cannot replace existing scopes 'singleton' and 'prototype'");
    }
    Scope previous = this.scopes.put(scopeName, scope);
    if (previous != null && previous != scope) {
      log.debug("Replacing scope '{}' from [{}] to [{}]", scopeName, previous, scope);
    }
    else if (log.isTraceEnabled()) {
      log.trace("Registering scope '{}' with implementation [{}]", scopeName, scope);
    }
  }

  @Override
  public String[] getRegisteredScopeNames() {
    return StringUtils.toStringArray(this.scopes.keySet());
  }

  @Override
  @Nullable
  public Scope getRegisteredScope(String scopeName) {
    Assert.notNull(scopeName, "Scope identifier must not be null");
    return this.scopes.get(scopeName);
  }

  @Override
  public void destroyScopedBean(String beanName) {
    BeanDefinition def = obtainBeanDefinition(beanName);
    if (def.isSingleton() || def.isPrototype()) {
      throw new IllegalArgumentException(
              "Bean name '" + beanName + "' does not correspond to an object in a mutable scope");
    }
    Scope scope = scopes.get(def.getScope());
    if (scope == null) {
      throw new IllegalStateException("No Scope SPI registered for scope name '" + def.getScope() + "'");
    }
    Object bean = scope.remove(beanName);
    if (bean != null) {
      destroyBean(bean, def);
    }
  }

  @Override
  public void setParentBeanFactory(@Nullable BeanFactory parentBeanFactory) {
    if (this.parentBeanFactory != null && this.parentBeanFactory != parentBeanFactory) {
      throw new IllegalStateException("Already associated with parent BeanFactory: " + this.parentBeanFactory);
    }
    if (this == parentBeanFactory) {
      throw new IllegalStateException("Cannot set parent bean factory to self");
    }
    this.parentBeanFactory = parentBeanFactory;
  }

  @Override
  public void setConversionService(ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  //  @since 4.0 for bean-property conversion
  @Override
  @Nullable
  public ConversionService getConversionService() {
    return conversionService;
  }

  public boolean isAutoInferDestroyMethod() {
    return autoInferDestroyMethod;
  }

  @Override
  public void setAutoInferDestroyMethod(boolean autoInferDestroyMethod) {
    this.autoInferDestroyMethod = autoInferDestroyMethod;
  }

  //

  @Override
  public void setBeanClassLoader(@Nullable ClassLoader beanClassLoader) {
    this.beanClassLoader = (beanClassLoader != null ? beanClassLoader : ClassUtils.getDefaultClassLoader());
  }

  @Override
  public ClassLoader getBeanClassLoader() {
    return beanClassLoader;
  }

  @Override
  public void copyConfigurationFrom(ConfigurableBeanFactory otherFactory) {
    Assert.notNull(otherFactory, "BeanFactory must not be null");
    setBeanClassLoader(otherFactory.getBeanClassLoader());
    setConversionService(otherFactory.getConversionService());
    if (otherFactory instanceof AbstractBeanFactory) {
      AbstractBeanFactory beanFactory = (AbstractBeanFactory) otherFactory;
      setAutoInferDestroyMethod(beanFactory.autoInferDestroyMethod);
      this.scopes.putAll(beanFactory.scopes);
      this.beanSupplier.putAll(beanFactory.beanSupplier);
      this.postProcessors.addAll(beanFactory.postProcessors);

      this.fullLifecycle = beanFactory.fullLifecycle;
      this.fullPrototype = beanFactory.fullPrototype;
      this.objectFactories = beanFactory.objectFactories;
      this.argumentsResolver = beanFactory.argumentsResolver;
      this.hasDestructionBeanPostProcessors = beanFactory.hasDestructionBeanPostProcessors;
      this.hasInstantiationAwareBeanPostProcessors = beanFactory.hasInstantiationAwareBeanPostProcessors;

    }
    else {
      String[] otherScopeNames = otherFactory.getRegisteredScopeNames();
      for (String scopeName : otherScopeNames) {
        this.scopes.put(scopeName, otherFactory.getRegisteredScope(scopeName));
      }
    }
  }

  @Override
  public String toString() {
    // ObjectUtils.toHexString(this)
    StringBuilder sb = new StringBuilder(ObjectUtils.identityToString(this));
    sb.append(": defining beans [");
    sb.append(StringUtils.arrayToString(getBeanDefinitionNames()));
    sb.append("]; ");
    BeanFactory parent = getParentBeanFactory();
    if (parent == null) {
      sb.append("root of factory hierarchy");
    }
    else {
      sb.append("parent: ").append(ObjectUtils.identityToString(parent));
    }
    return sb.toString();
  }

}
