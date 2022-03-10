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

package cn.taketoday.orm.jpa;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import javax.sql.DataSource;

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.beans.factory.BeanClassLoaderAware;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.BeanNameAware;
import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.core.task.AsyncTaskExecutor;
import cn.taketoday.dao.DataAccessException;
import cn.taketoday.dao.support.PersistenceExceptionTranslator;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import jakarta.persistence.SynchronizationType;
import jakarta.persistence.spi.PersistenceProvider;
import jakarta.persistence.spi.PersistenceUnitInfo;

/**
 * Abstract {@link cn.taketoday.beans.factory.FactoryBean} that creates
 * a local JPA {@link EntityManagerFactory} instance within
 * a Framework application context.
 *
 * <p>Encapsulates the common functionality between the different JPA bootstrap
 * contracts (standalone as well as container).
 *
 * <p>Implements support for standard JPA configuration conventions as well as
 * Framework's customizable {@link JpaVendorAdapter} mechanism, and controls the
 * EntityManagerFactory's lifecycle.
 *
 * <p>This class also implements the
 * {@link cn.taketoday.dao.support.PersistenceExceptionTranslator}
 * interface, as autodetected by Framework's
 * {@link cn.taketoday.dao.annotation.PersistenceExceptionTranslationPostProcessor},
 * for AOP-based translation of native exceptions to Framework DataAccessExceptions.
 * Hence, the presence of e.g. LocalEntityManagerFactoryBean automatically enables
 * a PersistenceExceptionTranslationPostProcessor to translate JPA exceptions.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @see LocalEntityManagerFactoryBean
 * @see LocalContainerEntityManagerFactoryBean
 * @since 4.0
 */
@SuppressWarnings("serial")
public abstract class AbstractEntityManagerFactoryBean implements FactoryBean<EntityManagerFactory>,
        BeanClassLoaderAware, BeanFactoryAware, BeanNameAware, InitializingBean, DisposableBean,
        EntityManagerFactoryInfo, PersistenceExceptionTranslator, Serializable {

  /** Logger available to subclasses. */
  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Nullable
  private PersistenceProvider persistenceProvider;

  @Nullable
  private String persistenceUnitName;

  private final Map<String, Object> jpaPropertyMap = new HashMap<>();

  @Nullable
  private Class<? extends EntityManagerFactory> entityManagerFactoryInterface;

  @Nullable
  private Class<? extends EntityManager> entityManagerInterface;

  @Nullable
  private JpaDialect jpaDialect;

  @Nullable
  private JpaVendorAdapter jpaVendorAdapter;

  @Nullable
  private Consumer<EntityManager> entityManagerInitializer;

  @Nullable
  private AsyncTaskExecutor bootstrapExecutor;

  private ClassLoader beanClassLoader = getClass().getClassLoader();

  @Nullable
  private BeanFactory beanFactory;

  @Nullable
  private String beanName;

  /** Raw EntityManagerFactory as returned by the PersistenceProvider. */
  @Nullable
  private EntityManagerFactory nativeEntityManagerFactory;

  /** Future for lazily initializing raw target EntityManagerFactory. */
  @Nullable
  private Future<EntityManagerFactory> nativeEntityManagerFactoryFuture;

  /** Exposed client-level EntityManagerFactory proxy. */
  @Nullable
  private EntityManagerFactory entityManagerFactory;

  /**
   * Set the PersistenceProvider implementation class to use for creating the
   * EntityManagerFactory. If not specified, the persistence provider will be
   * taken from the JpaVendorAdapter (if any) or retrieved through scanning
   * (as far as possible).
   *
   * @see JpaVendorAdapter#getPersistenceProvider()
   * @see PersistenceProvider
   * @see jakarta.persistence.Persistence
   */
  public void setPersistenceProviderClass(Class<? extends PersistenceProvider> persistenceProviderClass) {
    this.persistenceProvider = BeanUtils.newInstance(persistenceProviderClass);
  }

  /**
   * Set the PersistenceProvider instance to use for creating the
   * EntityManagerFactory. If not specified, the persistence provider
   * will be taken from the JpaVendorAdapter (if any) or determined
   * by the persistence unit deployment descriptor (as far as possible).
   *
   * @see JpaVendorAdapter#getPersistenceProvider()
   * @see PersistenceProvider
   * @see jakarta.persistence.Persistence
   */
  public void setPersistenceProvider(@Nullable PersistenceProvider persistenceProvider) {
    this.persistenceProvider = persistenceProvider;
  }

  @Override
  @Nullable
  public PersistenceProvider getPersistenceProvider() {
    return this.persistenceProvider;
  }

  /**
   * Specify the name of the EntityManagerFactory configuration.
   * <p>Default is none, indicating the default EntityManagerFactory
   * configuration. The persistence provider will throw an exception if
   * ambiguous EntityManager configurations are found.
   *
   * @see jakarta.persistence.Persistence#createEntityManagerFactory(String)
   */
  public void setPersistenceUnitName(@Nullable String persistenceUnitName) {
    this.persistenceUnitName = persistenceUnitName;
  }

  @Override
  @Nullable
  public String getPersistenceUnitName() {
    return this.persistenceUnitName;
  }

  /**
   * Specify JPA properties, to be passed into
   * {@code Persistence.createEntityManagerFactory} (if any).
   * <p>Can be populated with a String "value" (parsed via PropertiesEditor) or a
   * "props" element in XML bean definitions.
   *
   * @see jakarta.persistence.Persistence#createEntityManagerFactory(String, Map)
   * @see PersistenceProvider#createContainerEntityManagerFactory(PersistenceUnitInfo, Map)
   */
  public void setJpaProperties(Properties jpaProperties) {
    CollectionUtils.mergePropertiesIntoMap(jpaProperties, this.jpaPropertyMap);
  }

  /**
   * Specify JPA properties as a Map, to be passed into
   * {@code Persistence.createEntityManagerFactory} (if any).
   * <p>Can be populated with a "map" or "props" element in XML bean definitions.
   *
   * @see jakarta.persistence.Persistence#createEntityManagerFactory(String, Map)
   * @see PersistenceProvider#createContainerEntityManagerFactory(PersistenceUnitInfo, Map)
   */
  public void setJpaPropertyMap(@Nullable Map<String, ?> jpaProperties) {
    if (jpaProperties != null) {
      this.jpaPropertyMap.putAll(jpaProperties);
    }
  }

  /**
   * Allow Map access to the JPA properties to be passed to the persistence
   * provider, with the option to add or override specific entries.
   * <p>Useful for specifying entries directly, for example via
   * "jpaPropertyMap[myKey]".
   */
  public Map<String, Object> getJpaPropertyMap() {
    return this.jpaPropertyMap;
  }

  /**
   * Specify the (potentially vendor-specific) EntityManagerFactory interface
   * that this EntityManagerFactory proxy is supposed to implement.
   * <p>The default will be taken from the specific JpaVendorAdapter, if any,
   * or set to the standard {@code jakarta.persistence.EntityManagerFactory}
   * interface else.
   *
   * @see JpaVendorAdapter#getEntityManagerFactoryInterface()
   */
  public void setEntityManagerFactoryInterface(Class<? extends EntityManagerFactory> emfInterface) {
    this.entityManagerFactoryInterface = emfInterface;
  }

  /**
   * Specify the (potentially vendor-specific) EntityManager interface
   * that this factory's EntityManagers are supposed to implement.
   * <p>The default will be taken from the specific JpaVendorAdapter, if any,
   * or set to the standard {@code jakarta.persistence.EntityManager}
   * interface else.
   *
   * @see JpaVendorAdapter#getEntityManagerInterface()
   * @see EntityManagerFactoryInfo#getEntityManagerInterface()
   */
  public void setEntityManagerInterface(@Nullable Class<? extends EntityManager> emInterface) {
    this.entityManagerInterface = emInterface;
  }

  @Override
  @Nullable
  public Class<? extends EntityManager> getEntityManagerInterface() {
    return this.entityManagerInterface;
  }

  /**
   * Specify the vendor-specific JpaDialect implementation to associate with
   * this EntityManagerFactory. This will be exposed through the
   * EntityManagerFactoryInfo interface, to be picked up as default dialect by
   * accessors that intend to use JpaDialect functionality.
   *
   * @see EntityManagerFactoryInfo#getJpaDialect()
   */
  public void setJpaDialect(@Nullable JpaDialect jpaDialect) {
    this.jpaDialect = jpaDialect;
  }

  @Override
  @Nullable
  public JpaDialect getJpaDialect() {
    return this.jpaDialect;
  }

  /**
   * Specify the JpaVendorAdapter implementation for the desired JPA provider,
   * if any. This will initialize appropriate defaults for the given provider,
   * such as persistence provider class and JpaDialect, unless locally
   * overridden in this FactoryBean.
   */
  public void setJpaVendorAdapter(@Nullable JpaVendorAdapter jpaVendorAdapter) {
    this.jpaVendorAdapter = jpaVendorAdapter;
  }

  /**
   * Return the JpaVendorAdapter implementation for this EntityManagerFactory,
   * or {@code null} if not known.
   */
  @Nullable
  public JpaVendorAdapter getJpaVendorAdapter() {
    return this.jpaVendorAdapter;
  }

  /**
   * Specify a callback for customizing every {@code EntityManager} created
   * by the exposed {@code EntityManagerFactory}.
   * <p>This is an alternative to a {@code JpaVendorAdapter}-level
   * {@code postProcessEntityManager} implementation, enabling convenient
   * customizations for application purposes, e.g. setting Hibernate filters.
   *
   * @see JpaVendorAdapter#postProcessEntityManager
   * @see JpaTransactionManager#setEntityManagerInitializer
   */
  public void setEntityManagerInitializer(Consumer<EntityManager> entityManagerInitializer) {
    this.entityManagerInitializer = entityManagerInitializer;
  }

  /**
   * Specify an asynchronous executor for background bootstrapping,
   * e.g. a {@link cn.taketoday.core.task.SimpleAsyncTaskExecutor}.
   * <p>{@code EntityManagerFactory} initialization will then switch into background
   * bootstrap mode, with a {@code EntityManagerFactory} proxy immediately returned for
   * injection purposes instead of waiting for the JPA provider's bootstrapping to complete.
   * However, note that the first actual call to a {@code EntityManagerFactory} method will
   * then block until the JPA provider's bootstrapping completed, if not ready by then.
   * For maximum benefit, make sure to avoid early {@code EntityManagerFactory} calls
   * in init methods of related beans, even for metadata introspection purposes.
   */
  public void setBootstrapExecutor(@Nullable AsyncTaskExecutor bootstrapExecutor) {
    this.bootstrapExecutor = bootstrapExecutor;
  }

  /**
   * Return the asynchronous executor for background bootstrapping, if any.
   */
  @Nullable
  public AsyncTaskExecutor getBootstrapExecutor() {
    return this.bootstrapExecutor;
  }

  @Override
  public void setBeanClassLoader(ClassLoader classLoader) {
    this.beanClassLoader = classLoader;
  }

  @Override
  public ClassLoader getBeanClassLoader() {
    return this.beanClassLoader;
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  @Override
  public void setBeanName(String name) {
    this.beanName = name;
  }

  @Override
  public void afterPropertiesSet() throws PersistenceException {
    JpaVendorAdapter jpaVendorAdapter = getJpaVendorAdapter();
    if (jpaVendorAdapter != null) {
      if (persistenceProvider == null) {
        this.persistenceProvider = jpaVendorAdapter.getPersistenceProvider();
      }
      PersistenceUnitInfo pui = getPersistenceUnitInfo();
      Map<String, ?> vendorPropertyMap = (pui != null ? jpaVendorAdapter.getJpaPropertyMap(pui) :
                                          jpaVendorAdapter.getJpaPropertyMap());
      if (CollectionUtils.isNotEmpty(vendorPropertyMap)) {
        vendorPropertyMap.forEach((key, value) -> {
          if (!jpaPropertyMap.containsKey(key)) {
            jpaPropertyMap.put(key, value);
          }
        });
      }
      if (entityManagerFactoryInterface == null) {
        this.entityManagerFactoryInterface = jpaVendorAdapter.getEntityManagerFactoryInterface();
        if (!ClassUtils.isVisible(entityManagerFactoryInterface, beanClassLoader)) {
          this.entityManagerFactoryInterface = EntityManagerFactory.class;
        }
      }
      if (entityManagerInterface == null) {
        this.entityManagerInterface = jpaVendorAdapter.getEntityManagerInterface();
        if (!ClassUtils.isVisible(entityManagerInterface, beanClassLoader)) {
          this.entityManagerInterface = EntityManager.class;
        }
      }
      if (jpaDialect == null) {
        this.jpaDialect = jpaVendorAdapter.getJpaDialect();
      }
    }

    AsyncTaskExecutor bootstrapExecutor = getBootstrapExecutor();
    if (bootstrapExecutor != null) {
      this.nativeEntityManagerFactoryFuture = bootstrapExecutor.submit(this::buildNativeEntityManagerFactory);
    }
    else {
      this.nativeEntityManagerFactory = buildNativeEntityManagerFactory();
    }

    // Wrap the EntityManagerFactory in a factory implementing all its interfaces.
    // This allows interception of createEntityManager methods to return an
    // application-managed EntityManager proxy that automatically joins
    // existing transactions.
    this.entityManagerFactory = createEntityManagerFactoryProxy(nativeEntityManagerFactory);
  }

  private EntityManagerFactory buildNativeEntityManagerFactory() {
    EntityManagerFactory emf;
    try {
      emf = createNativeEntityManagerFactory();
    }
    catch (PersistenceException ex) {
      if (ex.getClass() == PersistenceException.class) {
        // Plain PersistenceException wrapper for underlying exception?
        // Make sure the nested exception message is properly exposed,
        // along the lines of Framework's NestedRuntimeException.getMessage()
        Throwable cause = ex.getCause();
        if (cause != null) {
          String message = ex.getMessage();
          String causeString = cause.toString();
          if (!message.endsWith(causeString)) {
            ex = new PersistenceException(message + "; nested exception is " + causeString, cause);
          }
        }
      }
      if (logger.isErrorEnabled()) {
        logger.error("Failed to initialize JPA EntityManagerFactory: {}", ex.getMessage());
      }
      throw ex;
    }

    JpaVendorAdapter jpaVendorAdapter = getJpaVendorAdapter();
    if (jpaVendorAdapter != null) {
      jpaVendorAdapter.postProcessEntityManagerFactory(emf);
    }

    if (logger.isInfoEnabled()) {
      logger.info("Initialized JPA EntityManagerFactory for persistence unit '{}'", getPersistenceUnitName());
    }
    return emf;
  }

  /**
   * Create a proxy for the given {@link EntityManagerFactory}. We do this to be able to
   * return a transaction-aware proxy for an application-managed {@link EntityManager}.
   *
   * @param emf the EntityManagerFactory as returned by the persistence provider,
   * if initialized already
   * @return the EntityManagerFactory proxy
   */
  protected EntityManagerFactory createEntityManagerFactoryProxy(@Nullable EntityManagerFactory emf) {
    Set<Class<?>> ifcs = new LinkedHashSet<>();
    Class<?> entityManagerFactoryInterface = this.entityManagerFactoryInterface;
    if (entityManagerFactoryInterface != null) {
      ifcs.add(entityManagerFactoryInterface);
    }
    else if (emf != null) {
      ifcs.addAll(ClassUtils.getAllInterfacesForClassAsSet(emf.getClass(), beanClassLoader));
    }
    else {
      ifcs.add(EntityManagerFactory.class);
    }
    ifcs.add(EntityManagerFactoryInfo.class);

    try {
      return (EntityManagerFactory) Proxy.newProxyInstance(beanClassLoader,
              ClassUtils.toClassArray(ifcs), new ManagedEntityManagerFactoryInvocationHandler(this));
    }
    catch (IllegalArgumentException ex) {
      if (entityManagerFactoryInterface != null) {
        throw new IllegalStateException("EntityManagerFactory interface [" + entityManagerFactoryInterface +
                "] seems to conflict with Framework's EntityManagerFactoryInfo mixin - consider resetting the " +
                "'entityManagerFactoryInterface' property to plain [jakarta.persistence.EntityManagerFactory]", ex);
      }
      else {
        throw new IllegalStateException("Conflicting EntityManagerFactory interfaces - " +
                "consider specifying the 'jpaVendorAdapter' or 'entityManagerFactoryInterface' property " +
                "to select a specific EntityManagerFactory interface to proceed with", ex);
      }
    }
  }

  /**
   * Delegate an incoming invocation from the proxy, dispatching to EntityManagerFactoryInfo
   * or the native EntityManagerFactory accordingly.
   */
  Object invokeProxyMethod(Method method, @Nullable Object[] args) throws Throwable {
    if (method.getDeclaringClass().isAssignableFrom(EntityManagerFactoryInfo.class)) {
      return method.invoke(this, args);
    }
    else if (method.getName().equals("createEntityManager") && args != null && args.length > 0 &&
            args[0] == SynchronizationType.SYNCHRONIZED) {
      // JPA 2.1's createEntityManager(SynchronizationType, Map)
      // Redirect to plain createEntityManager and add synchronization semantics through Framework proxy
      EntityManager rawEntityManager = (args.length > 1 ?
                                        getNativeEntityManagerFactory().createEntityManager((Map<?, ?>) args[1]) :
                                        getNativeEntityManagerFactory().createEntityManager());
      postProcessEntityManager(rawEntityManager);
      return ExtendedEntityManagerCreator.createApplicationManagedEntityManager(rawEntityManager, this, true);
    }

    // Look for Query arguments, primarily JPA 2.1's addNamedQuery(String, Query)
    if (args != null) {
      for (int i = 0; i < args.length; i++) {
        Object arg = args[i];
        if (arg instanceof Query query && Proxy.isProxyClass(arg.getClass())) {
          // Assumably a Framework-generated proxy from SharedEntityManagerCreator:
          // since we're passing it back to the native EntityManagerFactory,
          // let's unwrap it to the original Query object from the provider.
          try {
            args[i] = query.unwrap(null);
          }
          catch (RuntimeException ex) {
            // Ignore - simply proceed with given Query object then
          }
        }
      }
    }

    // Standard delegation to the native factory, just post-processing EntityManager return values
    Object retVal = method.invoke(getNativeEntityManagerFactory(), args);
    if (retVal instanceof EntityManager rawEntityManager) {
      // Any other createEntityManager variant - expecting non-synchronized semantics
      postProcessEntityManager(rawEntityManager);
      retVal = ExtendedEntityManagerCreator.createApplicationManagedEntityManager(rawEntityManager, this, false);
    }
    return retVal;
  }

  /**
   * Subclasses must implement this method to create the EntityManagerFactory
   * that will be returned by the {@code getObject()} method.
   *
   * @return the EntityManagerFactory instance returned by this FactoryBean
   * @throws PersistenceException if the EntityManager cannot be created
   */
  protected abstract EntityManagerFactory createNativeEntityManagerFactory() throws PersistenceException;

  /**
   * Implementation of the PersistenceExceptionTranslator interface, as
   * autodetected by Framework's PersistenceExceptionTranslationPostProcessor.
   * <p>Uses the dialect's conversion if possible; otherwise falls back to
   * standard JPA exception conversion.
   *
   * @see cn.taketoday.dao.annotation.PersistenceExceptionTranslationPostProcessor
   * @see JpaDialect#translateExceptionIfPossible
   * @see EntityManagerFactoryUtils#convertJpaAccessExceptionIfPossible
   */
  @Override
  @Nullable
  public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
    JpaDialect jpaDialect = getJpaDialect();
    return (jpaDialect != null ? jpaDialect.translateExceptionIfPossible(ex) :
            EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(ex));
  }

  @Override
  public EntityManagerFactory getNativeEntityManagerFactory() {
    if (nativeEntityManagerFactory != null) {
      return nativeEntityManagerFactory;
    }
    else {
      Assert.state(nativeEntityManagerFactoryFuture != null, "No native EntityManagerFactory available");
      try {
        return nativeEntityManagerFactoryFuture.get();
      }
      catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("Interrupted during initialization of native EntityManagerFactory", ex);
      }
      catch (ExecutionException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof PersistenceException persistenceException) {
          // Rethrow a provider configuration exception (possibly with a nested cause) directly
          throw persistenceException;
        }
        throw new IllegalStateException("Failed to asynchronously initialize native EntityManagerFactory: " +
                ex.getMessage(), cause);
      }
    }
  }

  @Override
  public EntityManager createNativeEntityManager(@Nullable Map<?, ?> properties) {
    EntityManager rawEntityManager = (CollectionUtils.isNotEmpty(properties) ?
                                      getNativeEntityManagerFactory().createEntityManager(properties) :
                                      getNativeEntityManagerFactory().createEntityManager());
    postProcessEntityManager(rawEntityManager);
    return rawEntityManager;
  }

  /**
   * Optional callback for post-processing the native EntityManager
   * before active use.
   * <p>The default implementation delegates to
   * {@link JpaVendorAdapter#postProcessEntityManager}, if available.
   *
   * @param rawEntityManager the EntityManager to post-process
   * @see #createNativeEntityManager
   * @see JpaVendorAdapter#postProcessEntityManager
   * @since 4.0
   */
  protected void postProcessEntityManager(EntityManager rawEntityManager) {
    JpaVendorAdapter jpaVendorAdapter = getJpaVendorAdapter();
    if (jpaVendorAdapter != null) {
      jpaVendorAdapter.postProcessEntityManager(rawEntityManager);
    }
    Consumer<EntityManager> customizer = this.entityManagerInitializer;
    if (customizer != null) {
      customizer.accept(rawEntityManager);
    }
  }

  @Override
  @Nullable
  public PersistenceUnitInfo getPersistenceUnitInfo() {
    return null;
  }

  @Override
  @Nullable
  public DataSource getDataSource() {
    return null;
  }

  /**
   * Return the singleton EntityManagerFactory.
   */
  @Override
  @Nullable
  public EntityManagerFactory getObject() {
    return entityManagerFactory;
  }

  @Override
  public Class<? extends EntityManagerFactory> getObjectType() {
    return entityManagerFactory != null ? entityManagerFactory.getClass() : EntityManagerFactory.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

  /**
   * Close the EntityManagerFactory on bean factory shutdown.
   */
  @Override
  public void destroy() {
    if (entityManagerFactory != null) {
      logger.info("Closing JPA EntityManagerFactory for persistence unit '{}'", getPersistenceUnitName());
      entityManagerFactory.close();
    }
  }

  //---------------------------------------------------------------------
  // Serialization support
  //---------------------------------------------------------------------

  @Serial
  private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
    throw new NotSerializableException("An EntityManagerFactoryBean itself is not deserializable - " +
            "just a SerializedEntityManagerFactoryBeanReference is");
  }

  @Serial
  protected Object writeReplace() throws ObjectStreamException {
    if (beanFactory != null && beanName != null) {
      return new SerializedEntityManagerFactoryBeanReference(beanFactory, beanName);
    }
    else {
      throw new NotSerializableException("EntityManagerFactoryBean does not run within a BeanFactory");
    }
  }

  /**
   * Minimal bean reference to the surrounding AbstractEntityManagerFactoryBean.
   * Resolved to the actual AbstractEntityManagerFactoryBean instance on deserialization.
   */
  @SuppressWarnings("serial")
  private static class SerializedEntityManagerFactoryBeanReference implements Serializable {

    private final BeanFactory beanFactory;

    private final String lookupName;

    public SerializedEntityManagerFactoryBeanReference(BeanFactory beanFactory, String beanName) {
      this.beanFactory = beanFactory;
      this.lookupName = BeanFactory.FACTORY_BEAN_PREFIX + beanName;
    }

    @Serial
    private Object readResolve() {
      return beanFactory.getBean(lookupName, AbstractEntityManagerFactoryBean.class);
    }
  }

  /**
   * Dynamic proxy invocation handler for an {@link EntityManagerFactory}, returning a
   * proxy {@link EntityManager} (if necessary) from {@code createEntityManager} methods.
   */
  @SuppressWarnings("serial")
  private static class ManagedEntityManagerFactoryInvocationHandler implements InvocationHandler, Serializable {

    private final AbstractEntityManagerFactoryBean entityManagerFactoryBean;

    public ManagedEntityManagerFactoryInvocationHandler(AbstractEntityManagerFactoryBean emfb) {
      this.entityManagerFactoryBean = emfb;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      switch (method.getName()) {
        case "equals":
          // Only consider equal when proxies are identical.
          return (proxy == args[0]);
        case "hashCode":
          // Use hashCode of EntityManagerFactory proxy.
          return System.identityHashCode(proxy);
        case "unwrap":
          // Handle JPA 2.1 unwrap method - could be a proxy match.
          Class<?> targetClass = (Class<?>) args[0];
          if (targetClass == null) {
            return this.entityManagerFactoryBean.getNativeEntityManagerFactory();
          }
          else if (targetClass.isInstance(proxy)) {
            return proxy;
          }
          break;
      }

      try {
        return this.entityManagerFactoryBean.invokeProxyMethod(method, args);
      }
      catch (InvocationTargetException ex) {
        throw ex.getTargetException();
      }
    }
  }

}
