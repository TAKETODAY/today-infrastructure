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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cn.taketoday.aop.TargetSource;
import cn.taketoday.aop.proxy.ProxyFactory;
import cn.taketoday.beans.ArgumentsResolver;
import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.DisposableBeanAdapter;
import cn.taketoday.beans.FactoryBean;
import cn.taketoday.beans.InitializingBean;
import cn.taketoday.beans.Primary;
import cn.taketoday.beans.PropertyValueException;
import cn.taketoday.beans.SmartFactoryBean;
import cn.taketoday.core.ConfigurationException;
import cn.taketoday.core.ObjectFactory;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.core.bytecode.Type;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Component;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * @author TODAY 2018-06-23 11:20:58
 */
public abstract class AbstractBeanFactory
        extends DefaultSingletonBeanRegistry implements ConfigurableBeanFactory, AutowireCapableBeanFactory {

  /** object factories */
  private Map<Class<?>, Object> objectFactories;
  /** dependencies */
  private final HashSet<BeanReferencePropertySetter> dependencies = new HashSet<>(128);
  /** Bean Post Processors */
  private final ArrayList<BeanPostProcessor> postProcessors = new ArrayList<>();
  private final HashMap<String, Scope> scopes = new HashMap<>();

  // @since 2.1.6
  private boolean fullPrototype = false;
  // @since 2.1.6
  private boolean fullLifecycle = false;

  /** Indicates whether any InstantiationAwareBeanPostProcessors have been registered.  @since 3.0 */
  private boolean hasInstantiationAwareBeanPostProcessors;
  /** @since 4.0 */
  private final ConcurrentHashMap<String, Supplier<?>> beanSupplier = new ConcurrentHashMap<>();

  /** @since 4.0 */
  private final ArgumentsResolver argumentsResolver = new ArgumentsResolver(this);

  /** Parent bean factory, for bean inheritance support. @since 4.0 */
  @Nullable
  private BeanFactory parentBeanFactory;

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
    return null;
  }

  /**
   * @throws ConfigurationException
   *         bean definition scope not exist in this bean factory
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
      Object bean = initializeSingleton(getSingleton(def.getName()), child);
      if (!def.isInitialized()) {
        // register as parent bean and set initialize flag
        registerSingleton(def.getName(), bean);
        def.setInitialized(true);
      }
      return bean;
    }
  }

  /**
   * Get bean for required type
   *
   * @param requiredType
   *         Bean type
   *
   * @since 2.1.2
   */
  protected <T> Object doGetBeanForType(Class<T> requiredType) {
    for (Entry<String, BeanDefinition> entry : getBeanDefinitions().entrySet()) {
      if (entry.getValue().isAssignableTo(requiredType)) {
        Object bean = getBean(entry.getValue());
        if (bean != null) {
          return bean;
        }
      }
    }
    // fix
    for (Object entry : getSingletons().values()) {
      if (requiredType.isAssignableFrom(entry.getClass())) {
        return entry;
      }
    }
    return null;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getBean(String name, Class<T> requiredType) {
    Object bean = getBean(name);
    return requiredType.isInstance(bean) ? (T) bean : null;
  }

  @Override
  public <A extends Annotation> A getAnnotationOnBean(String beanName, Class<A> annotationType) {
    return obtainBeanDefinition(beanName).getAnnotation(annotationType);
  }

  @Override
  public boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
    return isTypeMatch(name, ResolvableType.fromClass(typeToMatch));
  }

  @Override
  public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
    BeanDefinition beanDefinition = obtainBeanDefinition(name);
    return beanDefinition.isAssignableTo(typeToMatch);
  }

  @Override
  public <T> ObjectSupplier<T> getObjectSupplier(ResolvableType requiredType) {
    return null; // TODO
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
   * @param def
   *         Target {@link BeanDefinition} descriptor
   *
   * @return A new bean object
   *
   * @throws BeanInstantiationException
   *         When instantiation of a bean failed
   */
  protected Object createBeanInstance(BeanDefinition def) {
    if (hasInstantiationAwareBeanPostProcessors) {
      for (BeanPostProcessor processor : getPostProcessors()) {
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
   * Apply property values.
   *
   * @param bean
   *         Bean instance
   * @param def
   *         use {@link BeanDefinition}
   *
   * @throws PropertyValueException
   *         If any {@link Exception} occurred when apply
   *         {@link PropertySetter}
   * @throws NoSuchBeanDefinitionException
   *         If BeanReference is required and there isn't a bean in
   *         this {@link BeanFactory}
   */
  protected void applyPropertyValues(Object bean, BeanDefinition def) {
    for (PropertySetter propertySetter : def.getPropertySetters()) {
      propertySetter.applyValue(bean, this);
    }
  }

  /**
   * Invoke initialize methods
   *
   * @param bean
   *         Bean instance
   * @param def
   *         bean definition
   *
   * @throws BeanInitializingException
   *         when invoke init methods
   * @see Component
   * @see InitializingBean
   * @see javax.annotation.PostConstruct
   */
  protected void invokeInitMethods(Object bean, BeanDefinition def) {
    // invoke @PostConstruct or initMethods defined in @Component
    if (def instanceof DefaultBeanDefinition) {
      ((DefaultBeanDefinition) def).fastInvokeInitMethods(bean, this);
    }
    else {
      ArgumentsResolver resolver = getArgumentsResolver();
      for (Method method : def.getInitMethods()) { /*never be null*/
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
   * @param def
   *         Bean definition
   *
   * @return A initialized Prototype bean instance
   *
   * @throws BeanInstantiationException
   *         If any {@link Exception} occurred when create prototype
   */
  protected Object createPrototype(BeanDefinition def) {
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
   * @param <T>
   *         Target bean {@link Type}
   * @param def
   *         Target bean definition
   *
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
  protected String getFactoryBeanName(BeanDefinition def) {
    return FACTORY_BEAN_PREFIX.concat(def.getName());
  }

  @Override
  public Object getScopeBean(BeanDefinition def, Scope scope) {
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
  public Object initializeBean(Object bean, BeanDefinition def) {
    if (log.isDebugEnabled()) {
      log.debug("Initializing bean named: [{}].", def.getName());
    }
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
   * @param def
   *         Bean definition
   *
   * @return A initialized singleton bean
   *
   * @throws BeanInstantiationException
   *         When instantiation of a bean failed
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
   * Register {@link BeanPostProcessor}s
   */
  public void registerBeanPostProcessors() {
    log.info("Loading BeanPostProcessor.");
    postProcessors.addAll(getBeans(BeanPostProcessor.class));
    AnnotationAwareOrderComparator.sort(postProcessors);
  }

  // handleDependency
  // ---------------------------------------

  /**
   * Handle abstract dependencies
   */
  public void handleDependency() {
    // @since 3.0.3 fix ConcurrentModificationException
    LinkedHashSet<BeanReferencePropertySetter> dependencies = new LinkedHashSet<>(getDependencies());
    for (BeanReferencePropertySetter reference : dependencies) {
      String beanName = reference.getReferenceName();
      // fix: #2 when handle dependency some bean definition has already exist
      if (containsBeanDefinition(beanName)) {
        reference.setReference(getBeanDefinition(beanName));
        continue;
      }
      // handle dependency which is special bean like List<?> or Set<?>...
      // ----------------------------------------------------------------
      BeanDefinition handleDef = handleDependency(reference);
      if (handleDef != null) {
        registerBeanDefinition(beanName, handleDef);
        reference.setReference(handleDef);
        continue;
      }
      // handle dependency which is interface and parent object
      // --------------------------------------------------------
      Class<?> propertyType = reference.getReferenceClass();
      // find child beans
      List<BeanDefinition> childDefs = doGetChildDefinition(beanName, propertyType);
      if (CollectionUtils.isNotEmpty(childDefs)) {
        BeanDefinition childDef = getPrimaryBeanDefinition(childDefs);
        if (log.isDebugEnabled()) {
          log.debug("Found The Implementation Of [{}] Bean: [{}].", beanName, childDef.getName());
        }
        DefaultBeanDefinition def = new DefaultBeanDefinition(beanName, childDef);
        registerBeanDefinition(beanName, def);
        reference.setReference(def);
        continue;
      }
      if (reference.isRequired()) {
        throw new ConfigurationException("Context does not exist for this reference:[" + reference + "] of bean");
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
   * Process after register {@link BeanDefinition}
   *
   * @param targetDef
   *         Target {@link BeanDefinition}
   */
  protected void postProcessRegisterBeanDefinition(BeanDefinition targetDef) {
    PropertySetter[] propertySetters = targetDef.getPropertySetters();
    if (ObjectUtils.isNotEmpty(propertySetters)) {
      for (PropertySetter propertySetter : propertySetters) {
        if (propertySetter instanceof BeanReferencePropertySetter && !dependencies.contains(propertySetter)) {
          dependencies.add((BeanReferencePropertySetter) propertySetter);
        }
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
  protected BeanDefinition getPrimaryBeanDefinition(List<BeanDefinition> defs) {
    if (defs.size() > 1) {
      log.info("Finding primary bean which annotated @Primary in {}", defs);
      ArrayList<BeanDefinition> primaries = new ArrayList<>(defs.size());
      for (BeanDefinition def : defs) {
        if (def.isAnnotationPresent(Primary.class)) {
          primaries.add(def);
        }
      }
      if (!primaries.isEmpty()) {
        AnnotationAwareOrderComparator.sort(primaries); // size > 1 sort
        log.info("Found primary beans {} use first one", primaries);
        return primaries.get(0);
      }
      // not found sort bean-defs
      AnnotationAwareOrderComparator.sort(defs);
    }
    return defs.get(0);
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
  protected List<BeanDefinition> doGetChildDefinition(String beanName, Class<?> beanClass) {
    HashSet<BeanDefinition> ret = new HashSet<>();

    for (Entry<String, BeanDefinition> entry : getBeanDefinitions().entrySet()) {
      BeanDefinition childDef = entry.getValue();
      Class<?> clazz = childDef.getBeanClass();

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
  protected BeanDefinition handleDependency(BeanReferencePropertySetter ref) {
    // from objectFactories
    Map<Class<?>, Object> objectFactories = getObjectFactories();
    if (CollectionUtils.isNotEmpty(objectFactories)) {
      Object objectFactory = objectFactories.get(ref.getReferenceClass());
      if (objectFactory != null) {
        class DependencyBeanDefinition extends DefaultBeanDefinition {

          public DependencyBeanDefinition(String name, Class<?> beanClass) {
            super(name, beanClass);
          }

          @Override
          public Object newInstance(BeanFactory factory) {
            return createDependencyInstance(getBeanClass(), objectFactory);
          }
        }

        return new DependencyBeanDefinition(ref.getName(), ref.getReferenceClass());
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
  protected Object createDependencyInstance(Class<?> type, Object objectFactory) {
    if (type.isInstance(objectFactory)) {
      return objectFactory;
    }
    if (objectFactory instanceof Supplier) {
      return createObjectFactoryDependencyProxy(type, (Supplier<?>) objectFactory);
    }
    return null;
  }

  protected Object createObjectFactoryDependencyProxy(
          Class<?> type, Supplier<?> objectFactory) {
    // fixed @since 3.0.1
    ProxyFactory proxyFactory = createProxyFactory();
    proxyFactory.setTargetSource(new ObjectFactoryTargetSource(objectFactory, type));
    proxyFactory.setOpaque(true);
    return proxyFactory.getProxy(type.getClassLoader());
  }

  protected ProxyFactory createProxyFactory() {
    return new ProxyFactory();
  }

  static final class ObjectFactoryTargetSource implements TargetSource {
    private final Class<?> targetType;
    private final Supplier<?> objectFactory;

    ObjectFactoryTargetSource(Supplier<?> objectFactory, Class<?> targetType) {
      this.targetType = targetType;
      this.objectFactory = objectFactory;
    }

    @Override
    public Class<?> getTargetClass() {
      return targetType;
    }

    @Override
    public boolean isStatic() {
      return false;
    }

    @Override
    public Object getTarget() throws Exception {
      return objectFactory.get();
    }
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
   * @throws NoSuchBeanDefinitionException
   *         bean-definition not found
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
  public String getBeanName(Class<?> targetClass) {

    for (Entry<String, BeanDefinition> entry : getBeanDefinitions().entrySet()) {
      if (entry.getValue().getBeanClass() == targetClass) {
        return entry.getKey();
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

  public Set<BeanReferencePropertySetter> getDependencies() {
    return dependencies;
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
  public <T> T getBean(Class<T> requiredType) {
    return (T) doGetBeanForType(requiredType);
  }

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
            ret = targetSingleton = (T) getBean(def);
          }
          return ret;
        }

        @Override //@off
        public T get() { return getIfAvailable(); }
        public Stream<T> orderedStream() { return stream(); }
        public Stream<T> stream() { return Stream.of(targetSingleton); } //@on
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

  @Override
  public Set<String> getBeanNamesOfType(Class<?> requiredType, boolean includeNonSingletons) {
    return getBeanNamesOfType(requiredType, true, includeNonSingletons);
  }

  @Override
  public Set<String> getBeanNamesOfType(
          Class<?> requiredType, boolean includeNoneRegistered, boolean includeNonSingletons) {
    LinkedHashSet<String> beanNames = new LinkedHashSet<>();

    for (Entry<String, BeanDefinition> entry : getBeanDefinitions().entrySet()) {
      BeanDefinition def = entry.getValue();
      if (isEligibleBean(def, requiredType, includeNonSingletons)) {
        beanNames.add(entry.getKey());
      }
    }
    if (includeNoneRegistered) {
      for (Entry<String, Object> entry : getSingletons().entrySet()) {
        Object bean = entry.getValue();
        if (requiredType == null || requiredType.isInstance(bean)) {
          beanNames.add(entry.getKey());
        }
      }
    }
    return beanNames;
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
            && (requiredType == null || def.isAssignableTo(requiredType));
  }

  @Override
  public <T> Map<String, T> getBeansOfType(
          ResolvableType requiredType, boolean includeNoneRegistered, boolean includeNonSingletons) {

    return null;
  }

  @Override
  public Set<String> getBeanNamesOfType(
          ResolvableType requiredType, boolean includeNoneRegistered, boolean includeNonSingletons) {

    return null;
  }

  @Override
  public Map<String, Object> getBeansOfAnnotation(
          Class<? extends Annotation> annotationType, boolean includeNonSingletons) {
    Assert.notNull(annotationType, "annotationType must not be null");

    HashMap<String, Object> beans = new HashMap<>();
    for (Entry<String, BeanDefinition> entry : getBeanDefinitions().entrySet()) {
      BeanDefinition def = entry.getValue();
      if ((includeNonSingletons || def.isSingleton()) && def.isAnnotationPresent(annotationType)) {
        Object bean = getBean(def);
        if (bean != null) {
          beans.put(entry.getKey(), bean);
        }
      }
    }
    return beans;
  }

  @Override
  public Set<String> getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
    Assert.notNull(annotationType, "annotationType must not be null");

    LinkedHashSet<String> names = new LinkedHashSet<>();

    for (Entry<String, BeanDefinition> entry : getBeanDefinitions().entrySet()) {
      BeanDefinition def = entry.getValue();
      if (def.isAnnotationPresent(annotationType)) {
        names.add(entry.getKey());
      }
    }

    HashMap<String, Object> singletons = new HashMap<>(getSingletons());
    for (Entry<String, Object> entry : singletons.entrySet()) {
      String key = entry.getKey();
      if (!names.contains(key)) {
        Object value = entry.getValue();
        if (value != null && AnnotationUtils.isPresent(value.getClass(), annotationType)) {
          names.add(key);
        }
      }
    }
    return names;
  }

  //---------------------------------------------------------------------
  // Implementation of ArgumentsResolverProvider interface
  //---------------------------------------------------------------------

  /** @since 4.0 */
  @NonNull
  @Override
  public ArgumentsResolver getArgumentsResolver() {
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
   * @param beanInstance
   *         Bean instance
   * @param def
   *         Bean definition
   */
  @Override
  public void destroyBean(Object beanInstance, BeanDefinition def) {
    if (beanInstance == null || def == null) {
      return;
    }
    try {
      DisposableBeanAdapter.destroyBean(beanInstance, def, getPostProcessors());
    }
    catch (Throwable e) {
      log.warn("An Exception Occurred When Destroy a bean: [{}], With Msg: [{}]",
               def.getName(), e.toString(), e);
    }
  }

  @Override
  public void initialize(String name) {
    BeanDefinition def = obtainBeanDefinition(name);
    if (!def.isInitialized()) {
      createSingleton(def);
    }
    else if (log.isWarnEnabled()) {
      log.warn("A bean named: [{}] has already initialized", name);
    }
  }

  @Override
  public Object initialize(BeanDefinition def) {
    return getBean(def);
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
  }

  @Override
  public void removeBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
    getPostProcessors().remove(beanPostProcessor);

    for (BeanPostProcessor postProcessor : getPostProcessors()) {
      if (postProcessor instanceof InstantiationAwareBeanPostProcessor) {
        this.hasInstantiationAwareBeanPostProcessors = true;
        break;
      }
    }
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
  public void registerScope(String name, Scope scope) {
    Assert.notNull(name, "scope name must not be null");
    Assert.notNull(scope, "scope object must not be null");
    scopes.put(name, scope);
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
  public String toString() {
    return new StringBuilder(ObjectUtils.toHexString(this))
            .append(": defining beans [")
            .append(StringUtils.collectionToString(getBeanDefinitionNames()))
            .append("]").toString();
  }
}
