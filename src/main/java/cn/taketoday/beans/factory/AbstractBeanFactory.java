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

import cn.taketoday.aop.TargetSource;
import cn.taketoday.aop.proxy.ProxyFactory;
import cn.taketoday.beans.ArgumentsResolver;
import cn.taketoday.beans.DisposableBean;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.core.bytecode.Type;
import cn.taketoday.core.conversion.ConversionException;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.support.DefaultConversionService;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * @author TODAY 2018-06-23 11:20:58
 */
public abstract class AbstractBeanFactory
        extends DefaultSingletonBeanRegistry implements ConfigurableBeanFactory {
  private static final Logger log = LoggerFactory.getLogger(AbstractBeanFactory.class);

  /** object factories */
  protected Map<Class<?>, Object> objectFactories;
  private final HashMap<String, Scope> scopes = new HashMap<>();

  // @since 2.1.6
  private boolean fullPrototype = false;
  // @since 2.1.6
  private boolean fullLifecycle = false;

  /** @since 4.0 */
  @Nullable // lazy load
  private ConcurrentHashMap<String, Supplier<?>> beanSupplier;

  /** @since 4.0 */
  private ArgumentsResolver argumentsResolver;

  /** Parent bean factory, for bean inheritance support. @since 4.0 */
  @Nullable
  private BeanFactory parentBeanFactory;

  /** ClassLoader to resolve bean class names with, if necessary. @since 4.0 */
  private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

  /** ClassLoader to temporarily resolve bean class names with, if necessary. */
  @Nullable
  private ClassLoader tempClassLoader;

  // @since 4.0 for bean-property conversion
  private ConversionService conversionService;

  // @since 4.0
  private boolean autoInferDestroyMethod = true;

  // @since 4.0
  private volatile BeanPostProcessors postProcessorCache;

  /** Bean Post Processors */
  private final ArrayList<BeanPostProcessor> postProcessors = new ArrayList<>();

  //---------------------------------------------------------------------
  // Implementation of BeanFactory interface
  //---------------------------------------------------------------------

  @Nullable
  @Override
  public Object getBean(String name) {
    return doGetBean(name, null, null);
  }

  /**
   * @throws IllegalStateException bean definition scope not exist in this bean factory
   */
  @Nullable
  @Override
  public Object getBean(BeanDefinition def) {
    return doGetBean(def.getName(), def.getBeanClass(), null);
  }

  @Override
  @Nullable
  public <T> T getBean(String name, Class<T> requiredType) {
    return doGetBean(name, requiredType, null);
  }

  @Nullable
  @Override
  public Object getBean(String name, Object... args) throws BeansException {
    return doGetBean(name, null, args);
  }

  @SuppressWarnings("unchecked")
  @Nullable
  protected <T> T doGetBean(String beanName, Class<?> requiredType, Object[] args) throws BeansException {
    BeanDefinition definition = getBeanDefinition(beanName);
    if (definition == null) {
      // definition not exist in this factory
      if (beanSupplier != null) {
        Supplier<?> supplier = beanSupplier.get(beanName);
        if (supplier != null && args == null) {
          return (T) supplier.get();
        }
      }
      // Check if bean definition exists in this factory.
      BeanFactory parentBeanFactory = getParentBeanFactory();
      if (parentBeanFactory != null) {
        // Not found -> check parent.
        if (parentBeanFactory instanceof AbstractBeanFactory) {
          return ((AbstractBeanFactory) parentBeanFactory).doGetBean(beanName, requiredType, args);
        }
        else if (args != null) {
          // Delegation to parent with explicit args.
          return (T) parentBeanFactory.getBean(beanName, args);
        }
        else if (requiredType != null) {
          // No args -> delegate to standard getBean method.
          return (T) parentBeanFactory.getBean(beanName, requiredType);
        }
        else {
          return (T) parentBeanFactory.getBean(beanName);
        }
      }

      // don't throw exception
      return null;
    }

    Object beanInstance = getSingleton(beanName);
    if (beanInstance == null) {
      // Create bean instance.
      if (definition.isSingleton()) {
        beanInstance = getSingleton(beanName, () -> {
          try {
            return createBean(definition, args);
          }
          catch (BeansException ex) {
            // Explicitly remove instance from singleton cache: It might have been put there
            // eagerly by the creation process, to allow for circular reference resolution.
            // Also remove any beans that received a temporary reference to the bean.
            destroySingleton(beanName);
            throw ex;
          }
        });
      }
      else if (definition.isPrototype()) {
        // It's a prototype -> create a new instance.
        try {
          beforePrototypeCreation(beanName);
          beanInstance = createBean(definition, args);
        }
        finally {
          afterPrototypeCreation(beanName);
        }
      }
      else {
        String scopeName = definition.getScope();
        if (StringUtils.isEmpty(scopeName)) {
          throw new IllegalStateException("No scope name defined for bean '" + beanName + "'");
        }
        Scope scope = this.scopes.get(scopeName);
        if (scope == null) {
          throw new IllegalStateException("No Scope registered for scope name '" + scopeName + "'");
        }
        beanInstance = scope.get(beanName, () -> {
          beforePrototypeCreation(beanName);
          try {
            return createBean(definition, args);
          }
          finally {
            afterPrototypeCreation(beanName);
          }
        });
      }
    }
    beanInstance = getObjectForBeanInstance(beanName, beanInstance);
    return adaptBeanInstance(beanName, beanInstance, requiredType);
  }

  protected void afterPrototypeCreation(String beanName) { }

  protected void beforePrototypeCreation(String beanName) { }

  /**
   * Create a bean instance for the given merged bean definition (and arguments).
   * The bean definition will already have been merged with the parent definition
   * in case of a child definition.
   * <p>All bean retrieval methods delegate to this method for actual bean creation.
   *
   * @param definition the bean definition for the bean
   * @param args explicit arguments to use for constructor or factory method invocation
   * @return a new instance of the bean
   * @throws BeanCreationException if the bean could not be created
   */
  protected abstract Object createBean(
          BeanDefinition definition, @Nullable Object[] args) throws BeanCreationException;

  protected Object getObjectForBeanInstance(String beanName, Object beanInstance) throws BeansException {

    // Don't let calling code try to dereference the factory if the bean isn't a factory.
    if (BeanFactoryUtils.isFactoryDereference(beanName)) {
      if (!(beanInstance instanceof FactoryBean)) {
        throw new BeanIsNotAFactoryException(beanName, beanInstance.getClass());
      }
      return beanInstance;
    }

    // Now we have the bean instance, which may be a normal bean or a FactoryBean.
    // If it's a FactoryBean, we use it to create a bean instance, unless the
    // caller actually wants a reference to the factory.
    if (beanInstance instanceof FactoryBean<?> factory) {
      if (log.isDebugEnabled()) {
        log.debug("Bean with name '{}' is a factory bean", beanName);
      }
      // get bean from FactoryBean
      beanInstance = getObjectFromFactoryBean(factory, beanName);
    }
    return beanInstance;
  }

  /**
   * Obtain an object to expose from the given FactoryBean.
   *
   * @param factory the FactoryBean instance
   * @param beanName the name of the bean
   * @return the object obtained from the FactoryBean
   * @throws BeanCreationException if FactoryBean object creation failed
   * @see FactoryBean#getObject()
   */
  protected Object getObjectFromFactoryBean(FactoryBean<?> factory, String beanName) {
    if (factory.isSingleton() && containsSingleton(beanName)) {
      synchronized(getSingletons()) {
        Object object = getSingleton(beanName);
        if (object == null) {
          object = doGetObjectFromFactoryBean(factory, beanName);
          Object alreadyThere = getSingleton(beanName);
          if (alreadyThere != null) {
            object = alreadyThere;
          }
          else {
            if (containsSingleton(beanName)) {
              registerSingleton(beanName, object);
            }
          }
        }
        return object;
      }
    }
    else {
      return doGetObjectFromFactoryBean(factory, beanName);
    }
  }

  /**
   * Obtain an object to expose from the given FactoryBean.
   *
   * @param factory the FactoryBean instance
   * @param beanName the name of the bean
   * @return the object obtained from the FactoryBean
   * @throws BeanCreationException if FactoryBean object creation failed
   * @see FactoryBean#getObject()
   */
  private Object doGetObjectFromFactoryBean(FactoryBean<?> factory, String beanName) throws BeanCreationException {
    Object object;
    try {
      object = factory.getObject();
    }
    catch (Throwable ex) {
      throw new BeanCreationException(beanName, "FactoryBean threw exception on object creation", ex);
    }

    // Do not accept a null value for a FactoryBean that's not fully
    // initialized yet: Many FactoryBeans just return null then.
    return object;
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
    return isTypeMatch(name, typeToMatch, true);
  }

  protected boolean isTypeMatch(String name, ResolvableType typeToMatch, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {
    // Check manually registered singletons.
    Object beanInstance = getSingleton(name);
    boolean isFactoryDereference = BeanFactoryUtils.isFactoryDereference(name);

    if (beanInstance != null) {
      if (beanInstance instanceof FactoryBean) {
        if (!isFactoryDereference) {
          Class<?> type = getTypeForFactoryBean((FactoryBean<?>) beanInstance);
          return type != null && typeToMatch.isAssignableFrom(type);
        }
        else {
          return typeToMatch.isInstance(beanInstance);
        }
      }
      else if (!isFactoryDereference) {
        if (typeToMatch.isInstance(beanInstance)) {
          // Direct match for exposed instance?
          return true;
        }
        else if (typeToMatch.hasGenerics() && containsBeanDefinition(name)) {
          // Generics potentially only match on the target class, not on the proxy...
          BeanDefinition mbd = obtainBeanDefinition(name);
          Class<?> targetType = null;
          if (mbd.hasBeanClass()) {
            targetType = mbd.getBeanClass();
          }
          if (targetType != null && targetType != ClassUtils.getUserClass(beanInstance)) {
            // Check raw class match as well, making sure it's exposed on the proxy.
            Class<?> classToMatch = typeToMatch.resolve();
            if (classToMatch != null && !classToMatch.isInstance(beanInstance)) {
              return false;
            }
            return typeToMatch.isAssignableFrom(targetType);
          }
        }
      }
      return false;
    }
    else if (containsSingleton(name) && !containsBeanDefinition(name)) {
      // null instance registered
      return false;
    }

    // No singleton instance found -> check bean definition.
    BeanFactory parentBeanFactory = getParentBeanFactory();
    if (parentBeanFactory != null && !containsBeanDefinition(name)) {
      // No bean definition found in this factory -> delegate to parent.
      return parentBeanFactory.isTypeMatch(name, typeToMatch);
    }

    if (isFactoryDereference) {
      String beanName = BeanFactoryUtils.transformedBeanName(name);
      BeanDefinition definition = obtainBeanDefinition(beanName);
      // lookup start-with '$' , hasn't create its factory
      if (!definition.isLazyInit() || allowFactoryBeanInit) {
        FactoryBean<Object> factoryBean = getFactoryBean(definition);
        Class<?> beanClass = factoryBean.getObjectType();
        return typeToMatch.isAssignableFrom(beanClass);
      }
    }

    BeanDefinition definition = obtainBeanDefinition(name);
    Class<?> type = predictBeanType(definition);
    if (type != null) {
      return typeToMatch.isAssignableFrom(type);
    }
    return false;
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
    return resolveBeanClass(def, false);
  }

  protected Class<?> resolveBeanClass(BeanDefinition def, boolean matchOnly) throws BeanClassLoadFailedException {
    if (def.hasBeanClass()) {
      return def.getBeanClass();
    }

    String beanClassName = def.getBeanClassName();
    try {
      if (beanClassName != null && matchOnly) {
        ClassLoader tempClassLoader = getTempClassLoader();
        if (tempClassLoader != null) {
          // When resolving against a temporary class loader, exit early in order
          // to avoid storing the resolved Class in the bean definition.
          try {
            return tempClassLoader.loadClass(beanClassName);
          }
          catch (ClassNotFoundException ex) {
            if (log.isTraceEnabled()) {
              log.trace("Could not load class [{}] from {}: {}", beanClassName, tempClassLoader, ex, ex);
            }
          }
          return ClassUtils.forName(beanClassName, tempClassLoader);
        }
      }

      ClassLoader beanClassLoader = getBeanClassLoader();
      Class<?> beanClass = def.resolveBeanClass(beanClassLoader);
      if (beanClass != null) {
        return beanClass;
      }

      return ClassUtils.forName(beanClassName, beanClassLoader);
    }
    catch (ClassNotFoundException ex) {
      throw new BeanClassLoadFailedException(def, ex);
    }
    catch (LinkageError err) {
      throw new BeanClassLoadFailedException(def, err);
    }
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
    Object initBean = createBean(def, null);
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
    Object factory = getSingleton(getFactoryBeanName(def));
    if (factory instanceof FactoryBean) {
      // has already exits factory
      return (FactoryBean<T>) factory;
    }
    factory = createBean(def, null);
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
      for (DestructionBeanPostProcessor processor : postProcessors().destruction) {
        if (processor.requiresDestruction(bean)) {
          return true;
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
                autoInferDestroyMethod, bean, mbd,
                DisposableBeanAdapter.getFilteredPostProcessors(bean, postProcessors().destruction)));
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
                        DisposableBeanAdapter.getFilteredPostProcessors(bean, postProcessors().destruction)));
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
   * Handle dependency {@link BeanDefinition}
   *
   * @param requiredType by type
   * @return Dependency {@link BeanDefinition}
   */
  protected Object resolveFromObjectFactories(Class<?> requiredType) {
    // from objectFactories
    if (CollectionUtils.isNotEmpty(objectFactories)) {
      Object objectFactory = objectFactories.get(requiredType);
      if (objectFactory != null) {
        Object obj = createFromObjectFactory(requiredType, objectFactory);
        if (obj != null) {
          return obj;
        }
      }
      // iterate objectFactories
      for (Entry<Class<?>, Object> entry : objectFactories.entrySet()) {
        if (entry.getKey().isAssignableFrom(requiredType)) {
          objectFactory = entry.getValue();
          Object obj = createFromObjectFactory(requiredType, objectFactory);
          if (obj != null) {
            return obj;
          }
        }
      }
    }
    return null;
  }

  private Object createFromObjectFactory(Class<?> requiredType, Object objectFactory) {
    if (requiredType.isInstance(objectFactory)) {
      return objectFactory;
    }
    if (objectFactory instanceof Supplier) { // TODO type check?
      return createObjectFactoryDependencyProxy(requiredType, (Supplier<?>) objectFactory);
    }
    return null;
  }

  protected Object createObjectFactoryDependencyProxy(Class<?> type, Supplier<?> objectFactory) {
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
  public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
    Object beanInstance = getSingleton(name);
    if (beanInstance != null) {
      return true;
    }

    // No singleton instance found -> check bean definition.
    BeanFactory parentBeanFactory = getParentBeanFactory();
    if (parentBeanFactory != null && !containsBeanDefinition(name)) {
      // No bean definition found in this factory -> delegate to parent.
      return parentBeanFactory.isSingleton(name);
    }

    return obtainBeanDefinition(name).isSingleton();
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
  public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
    Object beanInstance = getSingleton(name);
    if (beanInstance != null) {
      return false;
    }

    // No singleton instance found -> check bean definition.
    BeanFactory parentBeanFactory = getParentBeanFactory();
    if (parentBeanFactory != null && !containsBeanDefinition(name)) {
      // No bean definition found in this factory -> delegate to parent.
      return parentBeanFactory.isPrototype(name);
    }
    return obtainBeanDefinition(name).isPrototype();
  }

  @Override
  public Class<?> getType(String beanName) {
    // Check manually registered singletons.
    Object beanInstance = getSingleton(beanName);
    if (beanInstance != null) {
      if (beanInstance instanceof FactoryBean && !BeanFactoryUtils.isFactoryDereference(beanName)) {
        // If it's a FactoryBean, we want to look at what it creates, not at the factory class.
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
      return factoryBean.getObjectType();
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
    return resolveBeanClass(definition, true);
  }

  @Override
  public boolean isFactoryBean(String beanName) throws NoSuchBeanDefinitionException {
    Object beanInstance = getSingleton(beanName);
    if (beanInstance != null) {
      return beanInstance instanceof FactoryBean;
    }
    // No singleton instance found -> check bean definition.
    if (!containsBeanDefinition(beanName) && getParentBeanFactory() instanceof ConfigurableBeanFactory) {
      // No bean definition found in this factory -> delegate to parent.
      return ((ConfigurableBeanFactory) getParentBeanFactory()).isFactoryBean(beanName);
    }
    return isFactoryBean(obtainBeanDefinition(beanName));
  }

  /**
   * Check whether the given bean is defined as a {@link FactoryBean}.
   *
   * @param def the corresponding bean definition
   */
  protected boolean isFactoryBean(BeanDefinition def) {
    Boolean result = def.isFactoryBean();
    if (result == null) {
      Class<?> beanType = predictBeanType(def);
      result = beanType != null && FactoryBean.class.isAssignableFrom(beanType);
      def.setFactoryBean(result);
    }
    return result;
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

  @Override
  public boolean isFullPrototype() {
    return fullPrototype;
  }

  @Override
  public boolean isFullLifecycle() {
    return fullLifecycle;
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
    Set<String> beanNamesOfType = getBeanNamesForType(beanClass, true, true);
    for (String name : beanNamesOfType) {
      removeBean(name);
    }
  }

  @Override
  public void destroyBean(String name, Object beanInstance) {
    destroyBean(beanInstance, obtainBeanDefinition(name));
  }

  /**
   * Destroy a bean with bean instance and bean definition
   *
   * @param beanInstance Bean instance
   * @param def Bean definition
   */
  public void destroyBean(Object beanInstance, BeanDefinition def) {
    new DisposableBeanAdapter(isAutoInferDestroyMethod(), beanInstance, def, postProcessors().destruction)
            .destroy();
  }

  @Override
  public void setFullPrototype(boolean fullPrototype) {
    this.fullPrototype = fullPrototype;
  }

  @Override
  public void setFullLifecycle(boolean fullLifecycle) {
    this.fullLifecycle = fullLifecycle;
  }

  // Scope

  @Override
  public void registerScope(String scopeName, Scope scope) {
    Assert.notNull(scope, "scope object must not be null");
    Assert.notNull(scopeName, "scope name must not be null");
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
  public void setTempClassLoader(@Nullable ClassLoader tempClassLoader) {
    this.tempClassLoader = tempClassLoader;
  }

  @Override
  @Nullable
  public ClassLoader getTempClassLoader() {
    return this.tempClassLoader;
  }

  @Override
  public void copyConfigurationFrom(ConfigurableBeanFactory otherFactory) {
    Assert.notNull(otherFactory, "BeanFactory must not be null");
    setBeanClassLoader(otherFactory.getBeanClassLoader());
    setConversionService(otherFactory.getConversionService());
    if (otherFactory instanceof AbstractBeanFactory beanFactory) {
      setAutoInferDestroyMethod(beanFactory.autoInferDestroyMethod);
      this.scopes.putAll(beanFactory.scopes);
      this.fullLifecycle = beanFactory.fullLifecycle;
      this.fullPrototype = beanFactory.fullPrototype;
      this.objectFactories = beanFactory.objectFactories; // FIXME copy?
      this.argumentsResolver = beanFactory.argumentsResolver;
      this.postProcessors.addAll(beanFactory.postProcessors);

      if (beanFactory.beanSupplier != null) {
        beanSupplier().putAll(beanFactory.beanSupplier);
      }
    }
    else {
      String[] otherScopeNames = otherFactory.getRegisteredScopeNames();
      for (String scopeName : otherScopeNames) {
        this.scopes.put(scopeName, otherFactory.getRegisteredScope(scopeName));
      }
    }
  }

  // beanSupplier

  public <T> void registerBean(String name, Supplier<T> supplier) throws BeanDefinitionStoreException {
    Assert.notNull(name, "bean-name must not be null");
    Assert.notNull(supplier, "bean-instance-supplier must not be null");
    beanSupplier().put(name, supplier);
  }

  protected ConcurrentHashMap<String, Supplier<?>> beanSupplier() {
    ConcurrentHashMap<String, Supplier<?>> suppliers = getBeanSupplier();
    if (suppliers == null) {
      suppliers = new ConcurrentHashMap<>();
      this.beanSupplier = suppliers;
    }
    return suppliers;
  }

  @Nullable
  public ConcurrentHashMap<String, Supplier<?>> getBeanSupplier() {
    return beanSupplier;
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

  //---------------------------------------------------------------------
  // BeanPostProcessor
  //---------------------------------------------------------------------

  public final List<BeanPostProcessor> getBeanPostProcessors() {
    return postProcessors;
  }

  protected final BeanPostProcessors postProcessors() {
    BeanPostProcessors postProcessors = postProcessorCache;
    if (postProcessors == null) {
      synchronized(this) {
        postProcessors = postProcessorCache;
        if (postProcessors == null) {
          postProcessors = new BeanPostProcessors(this.postProcessors);
          this.postProcessorCache = postProcessors;
        }
      }
    }
    return postProcessors;
  }

  @Override
  public void addBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
    Assert.notNull(beanPostProcessor, "BeanPostProcessor must not be null");
    invalidatePostProcessorsCache();

    postProcessors.remove(beanPostProcessor);
    postProcessors.add(beanPostProcessor);
    AnnotationAwareOrderComparator.sort(postProcessors);
  }

  /**
   * Add new BeanPostProcessors that will get applied to beans created
   * by this factory. To be invoked during factory configuration.
   *
   * @see #addBeanPostProcessor
   * @since 4.0
   */
  public void addBeanPostProcessors(Collection<? extends BeanPostProcessor> beanPostProcessors) {
    invalidatePostProcessorsCache();

    postProcessors.removeAll(beanPostProcessors);
    postProcessors.addAll(beanPostProcessors);
    AnnotationAwareOrderComparator.sort(postProcessors);
  }

  @Override
  public void removeBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
    invalidatePostProcessorsCache();
    postProcessors.remove(beanPostProcessor);
  }

  @Override
  public int getBeanPostProcessorCount() {
    return postProcessors.size();
  }

  // @since 4.0
  private void invalidatePostProcessorsCache() {
    postProcessorCache = null;
  }

  protected final static class BeanPostProcessors {
    public final ArrayList<DestructionBeanPostProcessor> destruction = new ArrayList<>();
    public final ArrayList<DependenciesBeanPostProcessor> dependencies = new ArrayList<>();
    public final ArrayList<InitializationBeanPostProcessor> initialization = new ArrayList<>();
    public final ArrayList<InstantiationAwareBeanPostProcessor> instantiation = new ArrayList<>();

    BeanPostProcessors(ArrayList<BeanPostProcessor> postProcessors) {
      for (BeanPostProcessor postProcessor : postProcessors) {
        if (postProcessor instanceof DestructionBeanPostProcessor destruction) {
          this.destruction.add(destruction);
        }
        if (postProcessor instanceof DependenciesBeanPostProcessor dependencies) {
          this.dependencies.add(dependencies);
        }
        if (postProcessor instanceof InitializationBeanPostProcessor initialization) {
          this.initialization.add(initialization);
        }
        if (postProcessor instanceof InstantiationAwareBeanPostProcessor instantiation) {
          this.instantiation.add(instantiation);
        }
      }

      this.destruction.trimToSize();
      this.dependencies.trimToSize();
      this.instantiation.trimToSize();
      this.initialization.trimToSize();
    }

  }

}
