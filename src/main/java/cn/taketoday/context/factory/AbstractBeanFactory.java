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
package cn.taketoday.context.factory;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cn.taketoday.context.BeanNameCreator;
import cn.taketoday.context.Scope;
import cn.taketoday.context.annotation.Component;
import cn.taketoday.context.annotation.Primary;
import cn.taketoday.context.asm.Type;
import cn.taketoday.context.aware.Aware;
import cn.taketoday.context.aware.BeanClassLoaderAware;
import cn.taketoday.context.aware.BeanFactoryAware;
import cn.taketoday.context.aware.BeanNameAware;
import cn.taketoday.context.env.DefaultBeanNameCreator;
import cn.taketoday.context.exception.BeanInitializingException;
import cn.taketoday.context.exception.BeanInstantiationException;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.exception.NoSuchBeanDefinitionException;
import cn.taketoday.context.exception.PropertyValueException;
import cn.taketoday.context.loader.BeanDefinitionLoader;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.CollectionUtils;
import cn.taketoday.context.utils.ContextUtils;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.context.utils.OrderUtils;
import cn.taketoday.context.utils.StringUtils;

import static cn.taketoday.context.utils.ContextUtils.resolveParameter;
import static cn.taketoday.context.utils.ReflectionUtils.makeAccessible;
import static java.util.Objects.requireNonNull;

/**
 * @author TODAY <br>
 * 2018-06-23 11:20:58
 */
public abstract class AbstractBeanFactory
        implements ConfigurableBeanFactory, AutowireCapableBeanFactory {

  private static final Logger log = LoggerFactory.getLogger(AbstractBeanFactory.class);

  /** bean name creator */
  private BeanNameCreator beanNameCreator;
  /** object factories */
  private Map<Class<?>, Object> objectFactories;
  /** dependencies */
  private final HashSet<BeanReferencePropertyValue> dependencies = new HashSet<>(128);
  /** Bean Post Processors */
  private final ArrayList<BeanPostProcessor> postProcessors = new ArrayList<>();
  /** Map of bean instance, keyed by bean name */
  private final HashMap<String, Object> singletons = new HashMap<>(128);
  private final HashMap<String, Scope> scopes = new HashMap<>();
  /** Map of bean definition objects, keyed by bean name */
  private final ConcurrentHashMap<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(64);

  // @since 2.1.6
  private boolean fullPrototype = false;
  // @since 2.1.6
  private boolean fullLifecycle = false;

  /** Indicates whether any InstantiationAwareBeanPostProcessors have been registered.  @since 3.0 */
  private boolean hasInstantiationAwareBeanPostProcessors;

  @Override
  public Object getBean(final String name) {
    final BeanDefinition def = getBeanDefinition(name);
    // if not exits a bean definition return a bean may exits in singletons cache
    return def != null ? getBean(def) : getSingleton(name);
  }

  @Override
  public Object getBean(final BeanDefinition def) {
    if (def.isFactoryBean()) {
      return getFactoryBean(def).getBean();
    }
    if (def.isInitialized()) { // fix #7
      return getSingleton(def.getName());
    }
    final BeanDefinition child = def.getChild();

    if (child == null) {
      if (def.isSingleton()) {
        return createSingleton(def);
      }
      else if (def.isPrototype()) {
        return createPrototype(def);
      }
      else {
        final Scope scope = scopes.get(def.getScope());
        if (scope == null) {
          throw new ConfigurationException("No such scope: [" + def.getScope() + "] in this " + this);
        }
        return getScopeBean(def, scope);
      }
    }
    else {
      if (def.isPrototype()) {
        return createPrototype(child);
      }
      // initialize child bean
      final Object bean = initializeSingleton(getSingleton(def.getName()), child);
      if (!def.isInitialized()) {
        // register as parent bean and set initialize flag
        registerSingleton(def.getName(), bean);
        def.setInitialized(true);
      }
      return bean;
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getBean(final Class<T> requiredType) {
    final Object bean = getBean(getBeanNameCreator().create(requiredType));
    return (T) (requiredType.isInstance(bean) ? bean : doGetBeanForType(requiredType));
  }

  /**
   * Get bean for required type
   *
   * @param requiredType
   *         Bean type
   *
   * @since 2.1.2
   */
  <T> Object doGetBeanForType(final Class<T> requiredType) {
    for (final Entry<String, BeanDefinition> entry : getBeanDefinitions().entrySet()) {
      if (requiredType.isAssignableFrom(entry.getValue().getBeanClass())) {
        final Object bean = getBean(entry.getValue());
        if (bean != null) {
          return bean;
        }
      }
    }
    // fix
    for (final Object entry : getSingletons().values()) {
      if (requiredType.isAssignableFrom(entry.getClass())) {
        return entry;
      }
    }
    return null;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getBean(String name, Class<T> requiredType) {
    final Object bean = getBean(name);
    return requiredType.isInstance(bean) ? (T) bean : null;
  }

  static class SingletonObjectSupplier<T> implements ObjectSupplier<T> {
    final T targetSingleton;

    SingletonObjectSupplier(T targetSingleton) {
      this.targetSingleton = targetSingleton;
    }

    @Override
    public T get() throws BeansException {
      return targetSingleton;
    }

    @Override
    public T getIfAvailable() throws BeansException {
      return targetSingleton;
    }

    @Override
    public Stream<T> stream() {
      return Stream.of(targetSingleton);
    }

    @Override
    public Stream<T> orderedStream() {
      return stream();
    }

  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> ObjectSupplier<T> getBeanSupplier(final BeanDefinition def) {
    Assert.notNull(def, "BeanDefinition must not be null");

    if (def.isSingleton()) {
      return new SingletonObjectSupplier<>((T) getBean(def));
    }

    return new DefaultObjectSupplier<T>(def.getBeanClass(), this) {

      @Override
      public T getIfAvailable() throws BeansException {
        return (T) getBean(def);
      }
    };
  }

  @Override
  public <T> ObjectSupplier<T> getBeanSupplier(Class<T> requiredType) {
    Assert.notNull(requiredType, "requiredType must not be null");
    return new DefaultObjectSupplier<>(requiredType, this);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Map<String, T> getBeansOfType(Class<T> requiredType, boolean includeNonSingletons) {
    final HashMap<String, T> beans = new HashMap<>();

    for (final Entry<String, BeanDefinition> entry : getBeanDefinitions().entrySet()) {
      final BeanDefinition def = entry.getValue();
      if (isEligibleBean(def, requiredType, includeNonSingletons)) {
        final Object bean = getBean(def);
        if (bean != null) {
          beans.put(entry.getKey(), (T) bean);
        }
      }
    }
    return beans;
  }

  @Override
  public String[] getBeanNamesOfType(Class<?> requiredType, boolean includeNonSingletons) {
    final List<String> beanNames = new ArrayList<>();

    for (final Entry<String, BeanDefinition> entry : getBeanDefinitions().entrySet()) {
      final BeanDefinition def = entry.getValue();
      if (isEligibleBean(def, requiredType, includeNonSingletons)) {
        final Object bean = getBean(def);
        if (bean != null) {
          beanNames.add(entry.getKey());
        }
      }
    }
    return beanNames.toArray(new String[0]);
  }

  /**
   * Return bean matching the given type (including subclasses), judging from bean definitions
   *
   * @param def
   *         the BeanDefinition to check
   * @param requiredType
   *         the class or interface to match, or {@code null} for all bean names
   * @param includeNonSingletons
   *         whether to include prototype or scoped beans too
   *         or just singletons (also applies to FactoryBeans)
   *
   * @return the bean matching the given object type (including subclasses)
   */
  static boolean isEligibleBean(BeanDefinition def, Class<?> requiredType, boolean includeNonSingletons) {
    return (includeNonSingletons || def.isSingleton())
            && (requiredType == null || requiredType.isAssignableFrom(def.getBeanClass()));
  }

  @Override
  public Map<String, Object> getBeansOfAnnotation(Class<? extends Annotation> annotationType, boolean includeNonSingletons) {
    Assert.notNull(annotationType, "annotationType must not be null");

    final HashMap<String, Object> beans = new HashMap<>();
    for (final Entry<String, BeanDefinition> entry : getBeanDefinitions().entrySet()) {
      final BeanDefinition def = entry.getValue();
      if ((includeNonSingletons || def.isSingleton()) && def.isAnnotationPresent(annotationType)) {
        final Object bean = getBean(def);
        if (bean != null) {
          beans.put(entry.getKey(), bean);
        }
      }
    }
    return beans;
  }

  @Override
  public <A extends Annotation> A getAnnotationOnBean(String beanName, Class<A> annotationType) {
    return obtainBeanDefinition(beanName).getAnnotation(annotationType);
  }

  @Override
  public Map<String, BeanDefinition> getBeanDefinitions() {
    return beanDefinitionMap;
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
   * @param def
   *         Bean definition
   *
   * @return Target bean instance
   *
   * @throws BeanInstantiationException
   *         When instantiation of a bean failed
   */
  protected Object createBeanIfNecessary(final BeanDefinition def) {
    if (def.isSingleton()) {
      final String name = def.getName();
      Object bean = getSingleton(name);
      if (bean == null) {
        registerSingleton(name, bean = createBeanInstance(def));
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
   * @param def
   *         Target {@link BeanDefinition} descriptor
   *
   * @return A new bean object
   *
   * @throws BeanInstantiationException
   *         When instantiation of a bean failed
   */
  protected Object createBeanInstance(final BeanDefinition def) {
    if (hasInstantiationAwareBeanPostProcessors) {
      for (final BeanPostProcessor processor : getPostProcessors()) {
        if (processor instanceof InstantiationAwareBeanPostProcessor) {
          final Object bean = ((InstantiationAwareBeanPostProcessor) processor).postProcessBeforeInstantiation(def);
          if (bean != null) {
            return bean;
          }
        }
      }
    }
    return def.newInstance(this);
  }

  /**
   * Apply property values.
   *
   * @param bean
   *         Bean instance
   * @param def
   *         use {@link BeanDefinition}
   *
   * @throws PropertyValueException
   *         If any {@link Exception} occurred when apply
   *         {@link PropertyValue}
   * @throws NoSuchBeanDefinitionException
   *         If {@link BeanReference} is required and there isn't a bean in
   *         this {@link BeanFactory}
   */
  protected void applyPropertyValues(final Object bean, final BeanDefinition def) {
    for (final PropertyValue propertyValue : def.getPropertyValues()) {
      propertyValue.applyValue(bean, this);
    }
  }

  /**
   * Invoke initialize methods
   *
   * @param bean
   *         Bean instance
   * @param methods
   *         Initialize methods
   *
   * @throws BeanInitializingException
   *         when invoke init methods
   */
  protected void invokeInitMethods(final Object bean, final Method[] methods) {

    for (final Method method : methods) {
      try {
        //method.setAccessible(true); // fix: can not access a member
        method.invoke(bean, resolveParameter(makeAccessible(method), this));
      }
      catch (Exception e) {
        throw new BeanInitializingException(
                "An Exception Occurred When [" + bean
                        + "] invoke init method: [" + method + "]", e);
      }
    }

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
   * @param def
   *         Bean definition
   *
   * @return A initialized Prototype bean instance
   *
   * @throws BeanInstantiationException
   *         If any {@link Exception} occurred when create prototype
   */
  protected Object createPrototype(final BeanDefinition def) {
    return initializeBean(createBeanInstance(def), def); // initialize
  }

  /**
   * Get initialized {@link FactoryBean}
   *
   * @param def
   *         Target {@link BeanDefinition}
   *
   * @return Initialized {@link FactoryBean} never be null
   *
   * @throws BeanInstantiationException
   *         If any {@link Exception} occurred when get FactoryBean
   */
  @SuppressWarnings("unchecked")
  protected <T> FactoryBean<T> getFactoryBean(final BeanDefinition def) {

    final FactoryBean<T> factoryBean = getFactoryBeanInstance(def);

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
    final Object initBean = initializeBean(factoryBean, def);

    def.setInitialized(true);
    registerSingleton(getFactoryBeanName(def), initBean); // Refresh bean to the mapping
    return (FactoryBean<T>) initBean;
  }

  /**
   * Get {@link FactoryBean} object
   *
   * @param <T>
   *         Target bean {@link Type}
   * @param def
   *         Target bean definition
   *
   * @return {@link FactoryBean} object
   */
  @SuppressWarnings("unchecked")
  protected <T> FactoryBean<T> getFactoryBeanInstance(final BeanDefinition def) {
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
    throw new ConfigurationException("object must be FactoryBean");
  }

  /**
   * Get {@link FactoryBean} bean name
   *
   * @param def
   *         Target {@link FactoryBean} {@link BeanDefinition}
   *
   * @return The name of target factory in this {@link BeanFactory}
   */
  protected String getFactoryBeanName(final BeanDefinition def) {
    return FACTORY_BEAN_PREFIX.concat(def.getName());
  }

  @Override
  public Object getScopeBean(final BeanDefinition def, Scope scope) {
    return scope.get(def, this::createPrototype);
  }

  /**
   * Initializing bean, with given bean instance and bean definition
   *
   * @param bean
   *         Bean instance
   * @param def
   *         Bean definition
   *
   * @return A initialized object, never be null
   *
   * @throws BeanInitializingException
   *         If any {@link Exception} occurred when initialize bean
   */
  @Override
  public Object initializeBean(final Object bean, final BeanDefinition def) {

    if (log.isDebugEnabled()) {
      log.debug("Initializing bean named: [{}].", def.getName());
    }
    aware(bean, def);

    final List<BeanPostProcessor> postProcessors = getPostProcessors();
    if (postProcessors.isEmpty()) {
      // apply properties
      applyPropertyValues(bean, def);
      // invoke initialize methods
      invokeInitMethods(bean, def.getInitMethods());
      return bean;
    }
    return initWithPostProcessors(bean, def, postProcessors);
  }

  /**
   * Initialize with {@link BeanPostProcessor}s
   *
   * @param bean
   *         Bean instance
   * @param def
   *         Current {@link BeanDefinition}
   * @param processors
   *         {@link BeanPostProcessor}s
   *
   * @return Initialized bean
   *
   * @throws BeanInitializingException
   *         If any {@link Exception} occurred when initialize with processors
   */
  protected Object initWithPostProcessors(
          final Object bean, final BeanDefinition def, final List<BeanPostProcessor> processors
  ) {
    Object ret = bean;
    // before properties
    for (final BeanPostProcessor processor : processors) {
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
    invokeInitMethods(ret, def.getInitMethods());
    // after properties
    for (final BeanPostProcessor processor : processors) {
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

  /**
   * Inject FrameWork {@link Component}s to target bean
   *
   * @param bean
   *         Bean instance
   * @param def
   *         Bean definition
   */
  public final void aware(final Object bean, final BeanDefinition def) {
    if (bean instanceof Aware) {
      awareInternal(bean, def);
    }
  }

  /**
   * Do Inject FrameWork {@link Component}s to target bean
   *
   * @param bean
   *         Target bean
   * @param def
   *         Target {@link BeanDefinition}
   */
  protected void awareInternal(final Object bean, final BeanDefinition def) {

    if (bean instanceof BeanNameAware) {
      ((BeanNameAware) bean).setBeanName(def.getName());
    }
    if (bean instanceof BeanFactoryAware) {
      ((BeanFactoryAware) bean).setBeanFactory(this);
    }
    if (bean instanceof BeanClassLoaderAware) {
      ((BeanClassLoaderAware) bean).setBeanClassLoader(bean.getClass().getClassLoader());
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
   * @param bean
   *         Input old bean
   * @param def
   *         Bean definition
   *
   * @return A initialized singleton bean
   *
   * @throws BeanInstantiationException
   *         When instantiation of a bean failed
   * @see #createSingleton(BeanDefinition)
   */
  protected Object initializeSingleton(final Object bean, final BeanDefinition def) {
    if (bean == null) {
      return createSingleton(def);
    }
    Assert.isTrue(def.isSingleton(), "Bean definition must be a singleton");
    if (def.isInitialized()) { // fix #7
      return bean;
    }
    final Object afterInit = initializeBean(bean, def);
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
   * @param def
   *         Bean definition
   *
   * @return A initialized singleton bean
   *
   * @throws BeanInstantiationException
   *         When instantiation of a bean failed
   */
  protected Object createSingleton(final BeanDefinition def) {
    Assert.isTrue(def.isSingleton(), "Bean definition must be a singleton");

    if (def.isFactoryBean()) {
      final Object bean = getFactoryBean(def).getBean();
      if (!containsSingleton(def.getName())) {
        registerSingleton(def.getName(), bean);
        def.setInitialized(true);
      }
      return bean;
    }

    if (def.isInitialized()) { // fix #7
      return getSingleton(def.getName());
    }

    final Object bean = createBeanIfNecessary(def);
    // After initialization
    final Object afterInit = initializeBean(bean, def);

    if (afterInit != bean) {
      registerSingleton(def.getName(), afterInit);
    }
    def.setInitialized(true);

    return afterInit;
  }

  /**
   * Register {@link BeanPostProcessor}s
   */
  public void registerBeanPostProcessors() {
    log.debug("Start loading BeanPostProcessor.");
    postProcessors.addAll(getBeans(BeanPostProcessor.class));
    OrderUtils.reversedSort(postProcessors);
  }

  // handleDependency
  // ---------------------------------------

  /**
   * Handle abstract dependencies
   */
  public void handleDependency() {

    for (final BeanReferencePropertyValue propertyValue : getDependencies()) {

      final BeanReference ref = propertyValue.getReference();
      final String beanName = ref.getName();

      // fix: #2 when handle dependency some bean definition has already exist
      if (containsBeanDefinition(beanName)) {
        ref.setReference(getBeanDefinition(beanName));
        continue;
      }

      // handle dependency which is special bean like List<?> or Set<?>...
      // ----------------------------------------------------------------

      final BeanDefinition handleDef = handleDependency(ref);
      if (handleDef != null) {
        registerBeanDefinition(beanName, handleDef);
        ref.setReference(handleDef);
        continue;
      }

      // handle dependency which is interface and parent object
      // --------------------------------------------------------

      final Class<?> propertyType = ref.getReferenceClass();

      // find child beans
      final List<BeanDefinition> childDefs = doGetChildDefinition(beanName, propertyType);

      if (!CollectionUtils.isEmpty(childDefs)) {
        final BeanDefinition childDef = getPrimaryBeanDefinition(childDefs);
        if (log.isDebugEnabled()) {
          log.debug("Found The Implementation Of [{}] Bean: [{}].", beanName, childDef.getName());
        }

        DefaultBeanDefinition def = new DefaultBeanDefinition(beanName, childDef);
        registerBeanDefinition(beanName, def);
        ref.setReference(def);
        continue;
      }

      if (ref.isRequired()) {
        throw new ConfigurationException("Context does not exist for this reference:[" + ref + "] of bean");
      }
    }
  }

  /**
   * Get {@link Primary} {@link BeanDefinition}
   *
   * @param defs
   *         All suitable {@link BeanDefinition}s
   *
   * @return A {@link Primary} {@link BeanDefinition}
   */
  protected BeanDefinition getPrimaryBeanDefinition(final List<BeanDefinition> defs) {
    BeanDefinition target = null;
    if (defs.size() > 1) {
      OrderUtils.reversedSort(defs); // size > 1 sort
      for (final BeanDefinition def : defs) {
        if (def.isAnnotationPresent(Primary.class)) {
          target = def;
          break;
        }
      }
    }
    if (target == null) {
      target = defs.get(0); // first one
    }
    return target;
  }

  /**
   * Get child {@link BeanDefinition}s
   *
   * @param beanName
   *         Bean name
   * @param beanClass
   *         Bean class
   *
   * @return A list of {@link BeanDefinition}s, Never be null
   */
  protected List<BeanDefinition> doGetChildDefinition(final String beanName, final Class<?> beanClass) {

    final HashSet<BeanDefinition> ret = new HashSet<>();

    for (final Entry<String, BeanDefinition> entry : getBeanDefinitions().entrySet()) {
      final BeanDefinition childDef = entry.getValue();
      final Class<?> clazz = childDef.getBeanClass();

      if (beanClass != clazz
              && beanClass.isAssignableFrom(clazz)
              && !beanName.equals(childDef.getName())) {

        ret.add(childDef); // is beanClass's Child Bean
      }
    }

    return ret.isEmpty() ? null : new ArrayList<>(ret);
  }

  /**
   * Handle dependency {@link BeanDefinition}
   *
   * @param ref
   *         BeanReference
   *
   * @return Dependency {@link BeanDefinition}
   */
  protected BeanDefinition handleDependency(final BeanReference ref) {
    // from objectFactories
    final Map<Class<?>, Object> objectFactories = getObjectFactories();
    if (!CollectionUtils.isEmpty(objectFactories)) {
      final Object objectFactory = objectFactories.get(ref.getReferenceClass());
      if (objectFactory != null) {
        return new DefaultBeanDefinition(ref.getName(), ref.getReferenceClass()) {
          @Override
          public Object newInstance(final BeanFactory factory) {
            return createDependencyInstance(getBeanClass(), objectFactory);
          }
        };
      }
    }

    return null;
  }

  /**
   * Create dependency object
   *
   * @param type
   *         dependency type
   * @param objectFactory
   *         Object factory
   *
   * @return Dependency object
   */
  protected Object createDependencyInstance(final Class<?> type, final Object objectFactory) {
    if (type.isInstance(objectFactory)) {
      return objectFactory;
    }
    if (objectFactory instanceof ObjectFactory) {
      return createObjectFactoryDependencyProxy(type, (ObjectFactory<?>) objectFactory);
    }
    return null;
  }

  protected Object createObjectFactoryDependencyProxy(final Class<?> type, final ObjectFactory<?> objectFactory) {
    return Proxy.newProxyInstance(type.getClassLoader(), new Class[] { type },
                                  new ObjectFactoryDelegatingHandler(objectFactory));
  }

  /**
   * Get {@link ObjectFactory}s
   *
   * @return {@link ObjectFactory}s
   *
   * @since 2.3.7
   */
  public final Map<Class<?>, Object> getObjectFactories() {
    if (objectFactories == null) {
      objectFactories = createObjectFactories();
    }
    return objectFactories;
  }

  protected Map<Class<?>, Object> createObjectFactories() {
    return new HashMap<>();
  }

  public void setObjectFactories(Map<Class<?>, Object> objectFactories) {
    this.objectFactories = objectFactories;
  }

  /**
   * Reflective InvocationHandler for lazy access to the current target object.
   */
  public static class ObjectFactoryDelegatingHandler implements InvocationHandler {

    private final ObjectFactory<?> objectFactory;

    public ObjectFactoryDelegatingHandler(ObjectFactory<?> objectFactory) {
      this.objectFactory = requireNonNull(objectFactory);
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
      try {
        return method.invoke(objectFactory.getObject(), args);
      }
      catch (InvocationTargetException ex) {
        throw ex.getTargetException();
      }
    }
  }

  // ---------------------------------------

  @Override
  public boolean isSingleton(String name) {
    return obtainBeanDefinition(name).isSingleton();
  }

  public BeanDefinition obtainBeanDefinition(String name) {
    final BeanDefinition def = getBeanDefinition(name);
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
  public Class<?> getType(String name) {
    return obtainBeanDefinition(name).getBeanClass();
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
  public void registerBean(Class<?> clazz) {
    registerBean(getBeanNameCreator().create(clazz), clazz);
  }

  @Override
  public void registerBean(Set<Class<?>> candidates) {
    final BeanNameCreator nameCreator = getBeanNameCreator();
    for (final Class<?> candidate : candidates) {
      registerBean(nameCreator.create(candidate), candidate);
    }
  }

  @Override
  public void registerBean(String name, Class<?> clazz) {
    getBeanDefinitionLoader().loadBeanDefinition(name, clazz);
  }

  @Override
  public void registerBean(String name, BeanDefinition beanDefinition) {
    getBeanDefinitionLoader().register(name, beanDefinition);
  }

  @Override
  public void registerBean(Object obj) {
    registerBean(getBeanNameCreator().create(obj.getClass()), obj);
  }

  @Override
  public void registerBean(final String name, final Object obj) {
    Assert.notNull(obj, "bean instance must not be null");

    String nameToUse = name;
    final Class<?> beanClass = obj.getClass();
    if (StringUtils.isEmpty(nameToUse)) {
      nameToUse = getBeanNameCreator().create(beanClass);
    }
    getBeanDefinitionLoader().loadBeanDefinition(nameToUse, beanClass);
    final BeanDefinition def = getBeanDefinition(name);
    if (def.isSingleton()) {
      registerSingleton(name, obj);
    }
  }

  @Override
  public void addBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
    Assert.notNull(beanPostProcessor, "BeanPostProcessor must not be null");

    final List<BeanPostProcessor> postProcessors = getPostProcessors();
    postProcessors.remove(beanPostProcessor);
    postProcessors.add(beanPostProcessor);

    OrderUtils.reversedSort(postProcessors);

    // Track whether it is instantiation/destruction aware
    if (beanPostProcessor instanceof InstantiationAwareBeanPostProcessor) {
      this.hasInstantiationAwareBeanPostProcessors = true;
    }
  }

  @Override
  public void removeBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
    getPostProcessors().remove(beanPostProcessor);

    for (final BeanPostProcessor postProcessor : getPostProcessors()) {
      if (postProcessor instanceof InstantiationAwareBeanPostProcessor) {
        this.hasInstantiationAwareBeanPostProcessors = true;
        break;
      }
    }
  }

  @Override
  public void registerSingleton(final String name, final Object singleton) {
    Assert.notNull(name, "Bean name must not be null");
    Assert.notNull(singleton, "Singleton object must not be null");

    synchronized (singletons) {
      final Object oldBean = singletons.put(name, singleton);
      if (log.isDebugEnabled()) {
        if (oldBean == null) {
          log.debug("Register Singleton: [{}] = [{}]", name, ObjectUtils.toHexString(singleton));
        }
        else if (oldBean != singleton) {
          log.debug("Refresh Singleton: [{}] = [{}] old bean: [{}] ",
                    name, ObjectUtils.toHexString(singleton), ObjectUtils.toHexString(oldBean));
        }
      }
    }
  }

  @Override
  public void registerSingleton(Object bean) {
    registerSingleton(getBeanNameCreator().create(bean.getClass()), bean);
  }

  @Override
  public Map<String, Object> getSingletons() {
    return singletons;
  }

  @Override
  public Object getSingleton(String name) {
    return singletons.get(name);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getSingleton(final Class<T> requiredType) {
    final String maybe = getBeanNameCreator().create(requiredType);
    final Object singleton = getSingleton(maybe);
    if (singleton == null) {
      final Map<String, Object> singletons = getSingletons();
      for (final Object value : singletons.values()) {
        if (requiredType.isInstance(value)) {
          return (T) value;
        }
      }
    }
    else if (requiredType.isInstance(singleton)) {
      return (T) singleton;
    }
    return null;
  }

  /**
   * Get target singleton
   *
   * @param name
   *         Bean name
   * @param targetClass
   *         Target class
   *
   * @return Target singleton
   */
  @SuppressWarnings("unchecked")
  public <T> T getSingleton(String name, Class<T> targetClass) {
    final Object singleton = getSingleton(name);
    return targetClass.isInstance(singleton) ? (T) singleton : null;
  }

  @Override
  public void removeSingleton(String name) {
    singletons.remove(name);
  }

  @Override
  public void removeBean(String name) {
    removeBeanDefinition(name);
    removeSingleton(name);
  }

  @Override
  public boolean containsSingleton(String name) {
    return singletons.containsKey(name);
  }

  @Override
  public void registerBeanDefinition(final String beanName, final BeanDefinition beanDefinition) {
    this.beanDefinitionMap.put(beanName, beanDefinition);

    postRegisterBeanDefinition(beanDefinition);
  }

  protected void postRegisterBeanDefinition(final BeanDefinition beanDefinition) {
    final PropertyValue[] propertyValues = beanDefinition.getPropertyValues();
    if (ObjectUtils.isNotEmpty(propertyValues)) {
      for (final PropertyValue propertyValue : propertyValues) {
        if (propertyValue instanceof BeanReferencePropertyValue) {
          this.dependencies.add((BeanReferencePropertyValue) propertyValue);
        }
      }
    }
  }

  @Override
  public void registerScope(String name, Scope scope) {
    Assert.notNull(name, "scope name must not be null");
    Assert.notNull(scope, "scope object must not be null");
    scopes.put(name, scope);
  }

  /**
   * Destroy a bean with bean instance and bean definition
   *
   * @param beanInstance
   *         Bean instance
   * @param def
   *         Bean definition
   */
  public void destroyBean(final Object beanInstance, final BeanDefinition def) {
    if (beanInstance == null || def == null) {
      return;
    }
    try {
      ContextUtils.destroyBean(beanInstance, def, getPostProcessors());
    }
    catch (Throwable e) {
      removeBean(def.getName());
      log.error("An Exception Occurred When Destroy a bean: [{}], With Msg: [{}]",
                def.getName(), e.toString(), e);
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
      final String factoryBeanName = name.substring(1);
      def = getBeanDefinition(factoryBeanName);
      if (def != null) {
        destroyBean(getSingleton(factoryBeanName), def);
        removeBean(factoryBeanName);
      }
    }

    if (def == null) {
      def = getPrototypeBeanDefinition(ClassUtils.getUserClass(beanInstance));
    }
    destroyBean(beanInstance, def);
    removeBean(name);
  }

  @Override
  public void destroyScopedBean(String beanName) {
    final BeanDefinition def = obtainBeanDefinition(beanName);
    if (def.isSingleton() || def.isPrototype()) {
      throw new IllegalArgumentException(
              "Bean name '" + beanName + "' does not correspond to an object in a mutable scope");
    }
    final Scope scope = scopes.get(def.getScope());
    if (scope == null) {
      throw new IllegalStateException("No Scope SPI registered for scope name '" + def.getScope() + "'");
    }
    final Object bean = scope.remove(beanName);
    if (bean != null) {
      destroyBean(bean, def);
    }
  }

  @Override
  public String getBeanName(Class<?> targetClass) {

    for (final Entry<String, BeanDefinition> entry : getBeanDefinitions().entrySet()) {
      if (entry.getValue().getBeanClass() == targetClass) {
        return entry.getKey();
      }
    }
    throw new NoSuchBeanDefinitionException(targetClass);
  }

  @Override
  public void removeBeanDefinition(String beanName) {
    beanDefinitionMap.remove(beanName);
  }

  @Override
  public BeanDefinition getBeanDefinition(String beanName) {
    return beanDefinitionMap.get(beanName);
  }

  @Override
  public BeanDefinition getBeanDefinition(Class<?> beanClass) {
    final BeanDefinition def = getBeanDefinition(getBeanNameCreator().create(beanClass));
    if (def != null && beanClass.isAssignableFrom(def.getBeanClass())) {
      return def;
    }
    for (final BeanDefinition definition : getBeanDefinitions().values()) {
      if (beanClass.isAssignableFrom(definition.getBeanClass())) {
        return definition;
      }
    }
    return null;
  }

  @Override
  public boolean containsBeanDefinition(String beanName) {
    return getBeanDefinitions().containsKey(beanName);
  }

  @Override
  public boolean containsBeanDefinition(Class<?> type) {
    return containsBeanDefinition(type, false);
  }

  @Override
  public boolean containsBeanDefinition(final Class<?> type, final boolean equals) {

    final Predicate<BeanDefinition> predicate = getPredicate(type, equals);
    final BeanDefinition def = getBeanDefinition(getBeanNameCreator().create(type));
    if (def != null && predicate.test(def)) {
      return true;
    }

    for (final BeanDefinition beanDef : getBeanDefinitions().values()) {
      if (predicate.test(beanDef)) {
        return true;
      }
    }
    return false;
  }

  private Predicate<BeanDefinition> getPredicate(final Class<?> type, final boolean equals) {
    return equals
           ? beanDef -> type == beanDef.getBeanClass()
           : beanDef -> type.isAssignableFrom(beanDef.getBeanClass());
  }

  @Override
  public Set<String> getBeanDefinitionNames() {
    return getBeanDefinitions().keySet();
  }

  @Override
  public int getBeanDefinitionCount() {
    return getBeanDefinitions().size();
  }

  public Set<BeanReferencePropertyValue> getDependencies() {
    return dependencies;
  }

  @Override
  public void initializeSingletons() {
    log.debug("Initialization of singleton objects.");
    for (final BeanDefinition def : getBeanDefinitions().values()) {
      // Trigger initialization of all non-lazy singleton beans...
      if (def.isSingleton() && !def.isInitialized() && !def.isLazyInit()) {
        if (def.isFactoryBean()) {
          final FactoryBean<?> factoryBean = getFactoryBeanInstance(def);
          final boolean isEagerInit = factoryBean instanceof SmartFactoryBean && ((SmartFactoryBean<?>) factoryBean).isEagerInit();
          if (isEagerInit) {
            getBean(def);
          }
          else {
          }
          createSingleton(def);
        }
      }
    }

    // Trigger post-initialization callback for all applicable beans...
    for (final Object singleton : getSingletons().values()) {
      postSingletonInitialization(singleton);
    }

    log.debug("The singleton objects are initialized.");
  }

  protected void postSingletonInitialization(final Object singleton) {
    // SmartInitializingSingleton
    if (singleton instanceof SmartInitializingSingleton) {
      ((SmartInitializingSingleton) singleton).afterSingletonsInstantiated();
    }
  }

  /**
   * Initialization singletons that has already in context
   */
  public void preInitialization() {
    final boolean debugEnabled = log.isDebugEnabled();

    for (final Entry<String, Object> entry : new HashMap<>(getSingletons()).entrySet()) {

      final String name = entry.getKey();
      final BeanDefinition def = getBeanDefinition(name);
      if (def == null || def.isInitialized()) {
        continue;
      }
      initializeSingleton(entry.getValue(), def);
      if (debugEnabled) {
        log.debug("Pre initialize singleton bean is being stored in the name of [{}].", name);
      }
    }
  }

  // -----------------------------------------------------

  @Override
  public void refresh(String name) {
    final BeanDefinition def = obtainBeanDefinition(name);
    if (!def.isInitialized()) {
      createSingleton(def);
    }
    else if (log.isWarnEnabled()) {
      log.warn("A bean named: [{}] has already initialized", name);
    }
  }

  @Override
  public Object refresh(BeanDefinition def) {
    return getBean(def);
  }

  // -----------------------------

  public abstract BeanDefinitionLoader getBeanDefinitionLoader();

  /**
   * Get a bean name creator
   *
   * @return {@link BeanNameCreator}
   */
  public BeanNameCreator getBeanNameCreator() {
    final BeanNameCreator ret = this.beanNameCreator;
    return ret == null ? this.beanNameCreator = createBeanNameCreator() : ret;
  }

  /**
   * create {@link BeanNameCreator}
   *
   * @return a default {@link BeanNameCreator}
   */
  protected BeanNameCreator createBeanNameCreator() {
    return new DefaultBeanNameCreator(true);
  }

  public final List<BeanPostProcessor> getPostProcessors() {
    return postProcessors;
  }

  @Override
  public void enableFullPrototype() {
    setFullPrototype(true);
  }

  @Override
  public void enableFullLifecycle() {
    setFullLifecycle(true);
  }

  public boolean isFullPrototype() {
    return fullPrototype;
  }

  public boolean isFullLifecycle() {
    return fullLifecycle;
  }

  public void setFullPrototype(boolean fullPrototype) {
    this.fullPrototype = fullPrototype;
  }

  public void setFullLifecycle(boolean fullLifecycle) {
    this.fullLifecycle = fullLifecycle;
  }

  public void setBeanNameCreator(BeanNameCreator beanNameCreator) {
    this.beanNameCreator = beanNameCreator;
  }

  // AutowireCapableBeanFactory
  // ---------------------------------

  @Override
  @SuppressWarnings("unchecked")
  public <T> T createBean(final Class<T> beanClass, final boolean cacheBeanDef) {
    BeanDefinition defToUse;
    if (cacheBeanDef) {
      if ((defToUse = getBeanDefinition(beanClass)) == null) {
        defToUse = getPrototypeBeanDefinition(beanClass);
        registerBean(defToUse);
      }
    }
    else {
      defToUse = getPrototypeBeanDefinition(beanClass);
    }
    return (T) getBean(defToUse);
  }

  @Override
  public void autowireBean(final Object existingBean) {
    final Class<Object> userClass = ClassUtils.getUserClass(existingBean);
    final BeanDefinition prototypeDef = getPrototypeBeanDefinition(userClass);
    if (log.isDebugEnabled()) {
      log.debug("Autowiring bean named: [{}].", prototypeDef.getName());
    }
    aware(existingBean, prototypeDef);
    // apply properties
    applyPropertyValues(existingBean, prototypeDef);
    // invoke initialize methods
    invokeInitMethods(existingBean, prototypeDef.getInitMethods());
  }

  @Override
  public void autowireBeanProperties(final Object existingBean) {
    final Class<Object> userClass = ClassUtils.getUserClass(existingBean);
    final BeanDefinition prototypeDef = getPrototypeBeanDefinition(userClass);
    if (log.isDebugEnabled()) {
      log.debug("Autowiring bean properties named: [{}].", prototypeDef.getName());
    }
    // apply properties
    applyPropertyValues(existingBean, prototypeDef);
  }

  @Override
  public Object initializeBean(Object existingBean) throws BeanInitializingException {
    return initializeBean(existingBean, getBeanNameCreator().create(existingBean.getClass()));
  }

  @Override
  public Object initializeBean(final Object existingBean, final String beanName) {
    final BeanDefinition prototypeDef = getPrototypeBeanDefinition(existingBean, beanName);
    return initializeBean(existingBean, prototypeDef);
  }

  @Override
  public Object applyBeanPostProcessorsBeforeInitialization(
          final Object existingBean, final String beanName
  ) {
    Object ret = existingBean;
    final BeanDefinition prototypeDef = getPrototypeBeanDefinition(existingBean, beanName);
    // before properties
    for (final BeanPostProcessor processor : getPostProcessors()) {
      try {
        ret = processor.postProcessBeforeInitialization(ret, prototypeDef);
      }
      catch (Exception e) {
        throw new BeanInitializingException(
                "An Exception Occurred When [" + existingBean + "] before properties set", e);
      }
    }
    return ret;
  }

  @Override
  public Object applyBeanPostProcessorsAfterInitialization(
          final Object existingBean, final String beanName
  ) {
    Object ret = existingBean;
    final BeanDefinition prototypeDef = getPrototypeBeanDefinition(existingBean, beanName);
    // after properties
    for (final BeanPostProcessor processor : getPostProcessors()) {
      try {
        ret = processor.postProcessAfterInitialization(ret, prototypeDef);
      }
      catch (Exception e) {
        throw new BeanInitializingException(
                "An Exception Occurred When [" + existingBean + "] after properties set", e);
      }
    }
    return ret;
  }

  @Override
  public void destroyBean(Object existingBean) {
    destroyBean(existingBean, getPrototypeBeanDefinition(ClassUtils.getUserClass(existingBean)));
  }

  private BeanDefinition getPrototypeBeanDefinition(Class<?> beanClass) {
    return getBeanDefinitionLoader()
            .createBeanDefinition(beanClass)
            .setScope(Scope.PROTOTYPE);
  }

  private BeanDefinition getPrototypeBeanDefinition(final Object existingBean, final String beanName) {
    return getPrototypeBeanDefinition(ClassUtils.getUserClass(existingBean)).setName(beanName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(ObjectUtils.toHexString(this));
    sb.append(": defining beans [");
    sb.append(StringUtils.collectionToString(this.beanDefinitionMap.keySet()));
    sb.append("]");
    return sb.toString();
  }
}
