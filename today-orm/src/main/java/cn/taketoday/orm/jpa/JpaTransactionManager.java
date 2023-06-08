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

package cn.taketoday.orm.jpa;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;

import javax.sql.DataSource;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.dao.DataAccessException;
import cn.taketoday.dao.support.DataAccessUtils;
import cn.taketoday.jdbc.datasource.ConnectionHandle;
import cn.taketoday.jdbc.datasource.ConnectionHolder;
import cn.taketoday.jdbc.datasource.JdbcTransactionObjectSupport;
import cn.taketoday.jdbc.datasource.TransactionAwareDataSourceProxy;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.transaction.CannotCreateTransactionException;
import cn.taketoday.transaction.IllegalTransactionStateException;
import cn.taketoday.transaction.NestedTransactionNotSupportedException;
import cn.taketoday.transaction.SavepointManager;
import cn.taketoday.transaction.TransactionDefinition;
import cn.taketoday.transaction.TransactionException;
import cn.taketoday.transaction.TransactionSystemException;
import cn.taketoday.transaction.support.AbstractPlatformTransactionManager;
import cn.taketoday.transaction.support.DefaultTransactionStatus;
import cn.taketoday.transaction.support.DelegatingTransactionDefinition;
import cn.taketoday.transaction.support.ResourceTransactionDefinition;
import cn.taketoday.transaction.support.ResourceTransactionManager;
import cn.taketoday.transaction.support.SynchronizationInfo;
import cn.taketoday.transaction.support.TransactionSynchronizationManager;
import cn.taketoday.util.CollectionUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.RollbackException;

/**
 * {@link cn.taketoday.transaction.PlatformTransactionManager} implementation
 * for a single JPA {@link EntityManagerFactory}. Binds a JPA
 * EntityManager from the specified factory to the thread, potentially allowing for
 * one thread-bound EntityManager per factory. {@link SharedEntityManagerCreator} and
 * {@code @PersistenceContext} are aware of thread-bound entity managers and participate
 * in such transactions automatically. Using either is required for JPA access code
 * supporting this transaction management mechanism.
 *
 * <p>This transaction manager is appropriate for applications that use a single
 * JPA EntityManagerFactory for transactional data access. JTA (usually through
 * {@link cn.taketoday.transaction.jta.JtaTransactionManager}) is necessary
 * for accessing multiple transactional resources within the same transaction.
 * Note that you need to configure your JPA provider accordingly in order to make
 * it participate in JTA transactions.
 *
 * <p>This transaction manager also supports direct DataSource access within a
 * transaction (i.e. plain JDBC code working with the same DataSource).
 * This allows for mixing services which access JPA and services which use plain
 * JDBC (without being aware of JPA)! Application code needs to stick to the
 * same simple Connection lookup pattern as with
 * {@link cn.taketoday.jdbc.datasource.DataSourceTransactionManager}
 * (i.e. {@link cn.taketoday.jdbc.datasource.DataSourceUtils#getConnection}
 * or going through a
 * {@link cn.taketoday.jdbc.datasource.TransactionAwareDataSourceProxy}).
 * Note that this requires a vendor-specific {@link JpaDialect} to be configured.
 *
 * <p>Note: To be able to register a DataSource's Connection for plain JDBC code,
 * this instance needs to be aware of the DataSource ({@link #setDataSource}).
 * The given DataSource should obviously match the one used by the given
 * EntityManagerFactory. This transaction manager will autodetect the DataSource
 * used as the connection factory of the EntityManagerFactory, so you usually
 * don't need to explicitly specify the "dataSource" property.
 *
 * <p>This transaction manager supports nested transactions via JDBC 3.0 Savepoints.
 * The {@link #setNestedTransactionAllowed "nestedTransactionAllowed"} flag defaults
 * to {@code false} though, since nested transactions will just apply to the JDBC
 * Connection, not to the JPA EntityManager and its cached entity objects and related
 * context. You can manually set the flag to {@code true} if you want to use nested
 * transactions for JDBC access code which participates in JPA transactions (provided
 * that your JDBC driver supports Savepoints). <i>Note that JPA itself does not support
 * nested transactions! Hence, do not expect JPA access code to semantically
 * participate in a nested transaction.</i>
 *
 * @author Juergen Hoeller
 * @see #setEntityManagerFactory
 * @see #setDataSource
 * @see LocalEntityManagerFactoryBean
 * @see cn.taketoday.orm.jpa.support.SharedEntityManagerBean
 * @see cn.taketoday.jdbc.datasource.DataSourceUtils#getConnection
 * @see cn.taketoday.jdbc.datasource.DataSourceUtils#releaseConnection
 * @see cn.taketoday.jdbc.core.JdbcTemplate
 * @see cn.taketoday.jdbc.support.JdbcTransactionManager
 * @see cn.taketoday.transaction.jta.JtaTransactionManager
 * @since 4.0
 */
@SuppressWarnings("serial")
public class JpaTransactionManager extends AbstractPlatformTransactionManager
        implements ResourceTransactionManager, BeanFactoryAware, InitializingBean {

  @Nullable
  private EntityManagerFactory entityManagerFactory;

  @Nullable
  private String persistenceUnitName;

  private final HashMap<String, Object> jpaPropertyMap = new HashMap<>();

  @Nullable
  private DataSource dataSource;

  private JpaDialect jpaDialect = new DefaultJpaDialect();

  @Nullable
  private Consumer<EntityManager> entityManagerInitializer;

  /**
   * Create a new JpaTransactionManager instance.
   * <p>An EntityManagerFactory has to be set to be able to use it.
   *
   * @see #setEntityManagerFactory
   */
  public JpaTransactionManager() {
    setNestedTransactionAllowed(true);
  }

  /**
   * Create a new JpaTransactionManager instance.
   *
   * @param emf the EntityManagerFactory to manage transactions for
   */
  public JpaTransactionManager(EntityManagerFactory emf) {
    this();
    this.entityManagerFactory = emf;
    afterPropertiesSet();
  }

  /**
   * Set the EntityManagerFactory that this instance should manage transactions for.
   * <p>Alternatively, specify the persistence unit name of the target EntityManagerFactory.
   * By default, a default EntityManagerFactory will be retrieved by finding a
   * single unique bean of type EntityManagerFactory in the containing BeanFactory.
   *
   * @see #setPersistenceUnitName
   */
  public void setEntityManagerFactory(@Nullable EntityManagerFactory emf) {
    this.entityManagerFactory = emf;
  }

  /**
   * Return the EntityManagerFactory that this instance should manage transactions for.
   */
  @Nullable
  public EntityManagerFactory getEntityManagerFactory() {
    return this.entityManagerFactory;
  }

  /**
   * Obtain the EntityManagerFactory for actual use.
   *
   * @return the EntityManagerFactory (never {@code null})
   * @throws IllegalStateException in case of no EntityManagerFactory set
   */
  protected final EntityManagerFactory obtainEntityManagerFactory() {
    EntityManagerFactory emf = getEntityManagerFactory();
    Assert.state(emf != null, "No EntityManagerFactory set");
    return emf;
  }

  /**
   * Set the name of the persistence unit to manage transactions for.
   * <p>This is an alternative to specifying the EntityManagerFactory by direct reference,
   * resolving it by its persistence unit name instead. If no EntityManagerFactory and
   * no persistence unit name have been specified, a default EntityManagerFactory will
   * be retrieved by finding a single unique bean of type EntityManagerFactory.
   *
   * @see #setEntityManagerFactory
   */
  public void setPersistenceUnitName(@Nullable String persistenceUnitName) {
    this.persistenceUnitName = persistenceUnitName;
  }

  /**
   * Return the name of the persistence unit to manage transactions for, if any.
   */
  @Nullable
  public String getPersistenceUnitName() {
    return this.persistenceUnitName;
  }

  /**
   * Specify JPA properties, to be passed into
   * {@code EntityManagerFactory.createEntityManager(Map)} (if any).
   * <p>Can be populated with a String "value" (parsed via PropertiesEditor)
   * or a "props" element in XML bean definitions.
   *
   * @see EntityManagerFactory#createEntityManager(Map)
   */
  public void setJpaProperties(@Nullable Properties jpaProperties) {
    CollectionUtils.mergePropertiesIntoMap(jpaProperties, this.jpaPropertyMap);
  }

  /**
   * Specify JPA properties as a Map, to be passed into
   * {@code EntityManagerFactory.createEntityManager(Map)} (if any).
   * <p>Can be populated with a "map" or "props" element in XML bean definitions.
   *
   * @see EntityManagerFactory#createEntityManager(Map)
   */
  public void setJpaPropertyMap(@Nullable Map<String, ?> jpaProperties) {
    if (jpaProperties != null) {
      this.jpaPropertyMap.putAll(jpaProperties);
    }
  }

  /**
   * Allow Map access to the JPA properties to be passed to the persistence
   * provider, with the option to add or override specific entries.
   * <p>Useful for specifying entries directly, for example via "jpaPropertyMap[myKey]".
   */
  public Map<String, Object> getJpaPropertyMap() {
    return this.jpaPropertyMap;
  }

  /**
   * Set the JDBC DataSource that this instance should manage transactions for.
   * The DataSource should match the one used by the JPA EntityManagerFactory:
   * for example, you could specify the same JNDI DataSource for both.
   * <p>If the EntityManagerFactory uses a known DataSource as its connection factory,
   * the DataSource will be autodetected: You can still explicitly specify the
   * DataSource, but you don't need to in this case.
   * <p>A transactional JDBC Connection for this DataSource will be provided to
   * application code accessing this DataSource directly via DataSourceUtils
   * or JdbcTemplate. The Connection will be taken from the JPA EntityManager.
   * <p>Note that you need to use a JPA dialect for a specific JPA implementation
   * to allow for exposing JPA transactions as JDBC transactions.
   * <p>The DataSource specified here should be the target DataSource to manage
   * transactions for, not a TransactionAwareDataSourceProxy. Only data access
   * code may work with TransactionAwareDataSourceProxy, while the transaction
   * manager needs to work on the underlying target DataSource. If there's
   * nevertheless a TransactionAwareDataSourceProxy passed in, it will be
   * unwrapped to extract its target DataSource.
   *
   * @see EntityManagerFactoryInfo#getDataSource()
   * @see #setJpaDialect
   * @see cn.taketoday.jdbc.datasource.TransactionAwareDataSourceProxy
   * @see cn.taketoday.jdbc.datasource.DataSourceUtils
   * @see cn.taketoday.jdbc.core.JdbcTemplate
   */
  public void setDataSource(@Nullable DataSource dataSource) {
    if (dataSource instanceof TransactionAwareDataSourceProxy proxy) {
      // If we got a TransactionAwareDataSourceProxy, we need to perform transactions
      // for its underlying target DataSource, else data access code won't see
      // properly exposed transactions (i.e. transactions for the target DataSource).
      this.dataSource = proxy.getTargetDataSource();
    }
    else {
      this.dataSource = dataSource;
    }
  }

  /**
   * Return the JDBC DataSource that this instance manages transactions for.
   */
  @Nullable
  public DataSource getDataSource() {
    return this.dataSource;
  }

  /**
   * Set the JPA dialect to use for this transaction manager.
   * Used for vendor-specific transaction management and JDBC connection exposure.
   * <p>If the EntityManagerFactory uses a known JpaDialect, it will be autodetected:
   * You can still explicitly specify the DataSource, but you don't need to in this case.
   * <p>The dialect object can be used to retrieve the underlying JDBC connection
   * and thus allows for exposing JPA transactions as JDBC transactions.
   *
   * @see EntityManagerFactoryInfo#getJpaDialect()
   * @see JpaDialect#beginTransaction
   * @see JpaDialect#getJdbcConnection
   */
  public void setJpaDialect(@Nullable JpaDialect jpaDialect) {
    this.jpaDialect = jpaDialect != null ? jpaDialect : new DefaultJpaDialect();
  }

  /**
   * Return the JPA dialect to use for this transaction manager.
   */
  public JpaDialect getJpaDialect() {
    return this.jpaDialect;
  }

  /**
   * Specify a callback for customizing every {@code EntityManager} resource
   * created for a new transaction managed by this {@code JpaTransactionManager}.
   * <p>This is an alternative to a factory-level {@code EntityManager} customizer
   * and to a {@code JpaVendorAdapter}-level {@code postProcessEntityManager}
   * callback, enabling specific customizations of transactional resources.
   *
   * @see #createEntityManagerForTransaction()
   * @see AbstractEntityManagerFactoryBean#setEntityManagerInitializer
   * @see JpaVendorAdapter#postProcessEntityManager
   */
  public void setEntityManagerInitializer(Consumer<EntityManager> entityManagerInitializer) {
    this.entityManagerInitializer = entityManagerInitializer;
  }

  /**
   * Retrieves an EntityManagerFactory by persistence unit name, if none set explicitly.
   * Falls back to a default EntityManagerFactory bean if no persistence unit specified.
   *
   * @see #setPersistenceUnitName
   */
  @Override
  public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
    if (getEntityManagerFactory() == null) {
      setEntityManagerFactory(
              EntityManagerFactoryUtils.findEntityManagerFactory(beanFactory, getPersistenceUnitName()));
    }
  }

  /**
   * Eagerly initialize the JPA dialect, creating a default one
   * for the specified EntityManagerFactory if none set.
   * Auto-detect the EntityManagerFactory's DataSource, if any.
   */
  @Override
  public void afterPropertiesSet() {
    if (getEntityManagerFactory() == null) {
      throw new IllegalArgumentException("'entityManagerFactory' or 'persistenceUnitName' is required");
    }
    if (getEntityManagerFactory() instanceof EntityManagerFactoryInfo emfInfo) {
      DataSource dataSource = emfInfo.getDataSource();
      if (dataSource != null) {
        setDataSource(dataSource);
      }
      JpaDialect jpaDialect = emfInfo.getJpaDialect();
      if (jpaDialect != null) {
        setJpaDialect(jpaDialect);
      }
    }
  }

  @Override
  public Object getResourceFactory() {
    return obtainEntityManagerFactory();
  }

  @Override
  protected Object doGetTransaction() {
    JpaTransactionObject txObject = new JpaTransactionObject();
    txObject.setSavepointAllowed(isNestedTransactionAllowed());
    SynchronizationInfo info = TransactionSynchronizationManager.getSynchronizationInfo();
    EntityManagerHolder emHolder = info.getResource(obtainEntityManagerFactory());
    if (emHolder != null) {
      if (logger.isDebugEnabled()) {
        logger.debug("Found thread-bound EntityManager [{}] for JPA transaction", emHolder.getEntityManager());
      }
      txObject.setEntityManagerHolder(emHolder, false);
    }

    if (getDataSource() != null) {
      ConnectionHolder conHolder = info.getResource(getDataSource());
      txObject.setConnectionHolder(conHolder);
    }

    return txObject;
  }

  @Override
  protected boolean isExistingTransaction(Object transaction) {
    return ((JpaTransactionObject) transaction).hasTransaction();
  }

  @Override
  protected void doBegin(Object transaction, TransactionDefinition definition) {
    JpaTransactionObject txObject = (JpaTransactionObject) transaction;

    if (txObject.hasConnectionHolder() && !txObject.getConnectionHolder().isSynchronizedWithTransaction()) {
      throw new IllegalTransactionStateException(
              "Pre-bound JDBC Connection found! JpaTransactionManager does not support " +
                      "running within DataSourceTransactionManager if told to manage the DataSource itself. " +
                      "It is recommended to use a single JpaTransactionManager for all transactions " +
                      "on a single DataSource, no matter whether JPA or JDBC access.");
    }

    try {
      if (!txObject.hasEntityManagerHolder() ||
              txObject.getEntityManagerHolder().isSynchronizedWithTransaction()) {
        EntityManager newEm = createEntityManagerForTransaction();
        if (logger.isDebugEnabled()) {
          logger.debug("Opened new EntityManager [{}] for JPA transaction", newEm);
        }
        txObject.setEntityManagerHolder(new EntityManagerHolder(newEm), true);
      }

      EntityManager em = txObject.getEntityManagerHolder().getEntityManager();

      // Delegate to JpaDialect for actual transaction begin.
      int timeoutToUse = determineTimeout(definition);
      Object transactionData = getJpaDialect().beginTransaction(em,
              new JpaTransactionDefinition(definition, timeoutToUse, txObject.isNewEntityManagerHolder()));
      txObject.setTransactionData(transactionData);
      txObject.setReadOnly(definition.isReadOnly());

      // Register transaction timeout.
      if (timeoutToUse != TransactionDefinition.TIMEOUT_DEFAULT) {
        txObject.getEntityManagerHolder().setTimeoutInSeconds(timeoutToUse);
      }

      // Register the JPA EntityManager's JDBC Connection for the DataSource, if set.
      if (getDataSource() != null) {
        ConnectionHandle conHandle = getJpaDialect().getJdbcConnection(em, definition.isReadOnly());
        if (conHandle != null) {
          ConnectionHolder conHolder = new ConnectionHolder(conHandle);
          if (timeoutToUse != TransactionDefinition.TIMEOUT_DEFAULT) {
            conHolder.setTimeoutInSeconds(timeoutToUse);
          }
          if (logger.isDebugEnabled()) {
            logger.debug("Exposing JPA transaction as JDBC [{}]", conHandle);
          }
          TransactionSynchronizationManager.bindResource(getDataSource(), conHolder);
          txObject.setConnectionHolder(conHolder);
        }
        else {
          if (logger.isDebugEnabled()) {
            logger.debug("Not exposing JPA transaction [{}] as JDBC transaction because " +
                    "JpaDialect [{}] does not support JDBC Connection retrieval", em, getJpaDialect());
          }
        }
      }

      // Bind the entity manager holder to the thread.
      if (txObject.isNewEntityManagerHolder()) {
        TransactionSynchronizationManager.bindResource(
                obtainEntityManagerFactory(), txObject.getEntityManagerHolder());
      }
      txObject.getEntityManagerHolder().setSynchronizedWithTransaction(true);
    }

    catch (TransactionException ex) {
      closeEntityManagerAfterFailedBegin(txObject);
      throw ex;
    }
    catch (Throwable ex) {
      closeEntityManagerAfterFailedBegin(txObject);
      throw new CannotCreateTransactionException("Could not open JPA EntityManager for transaction", ex);
    }
  }

  /**
   * Create a JPA EntityManager to be used for a transaction.
   * <p>The default implementation checks whether the EntityManagerFactory
   * is a Framework proxy and delegates to
   * {@link EntityManagerFactoryInfo#createNativeEntityManager}
   * if possible which in turns applies
   * {@link JpaVendorAdapter#postProcessEntityManager(EntityManager)}.
   *
   * @see EntityManagerFactory#createEntityManager()
   */
  protected EntityManager createEntityManagerForTransaction() {
    EntityManagerFactory emf = obtainEntityManagerFactory();
    Map<String, Object> properties = getJpaPropertyMap();
    EntityManager em;
    if (emf instanceof EntityManagerFactoryInfo) {
      em = ((EntityManagerFactoryInfo) emf).createNativeEntityManager(properties);
    }
    else {
      em = (CollectionUtils.isNotEmpty(properties) ?
            emf.createEntityManager(properties) : emf.createEntityManager());
    }
    if (this.entityManagerInitializer != null) {
      this.entityManagerInitializer.accept(em);
    }
    return em;
  }

  /**
   * Close the current transaction's EntityManager.
   * Called after a transaction begin attempt failed.
   *
   * @param txObject the current transaction
   */
  protected void closeEntityManagerAfterFailedBegin(JpaTransactionObject txObject) {
    if (txObject.isNewEntityManagerHolder()) {
      EntityManager em = txObject.getEntityManagerHolder().getEntityManager();
      try {
        if (em.getTransaction().isActive()) {
          em.getTransaction().rollback();
        }
      }
      catch (Throwable ex) {
        logger.debug("Could not rollback EntityManager after failed transaction begin", ex);
      }
      finally {
        EntityManagerFactoryUtils.closeEntityManager(em);
      }
      txObject.setEntityManagerHolder(null, false);
    }
  }

  @Override
  protected Object doSuspend(Object transaction) {
    JpaTransactionObject txObject = (JpaTransactionObject) transaction;
    txObject.setEntityManagerHolder(null, false);
    SynchronizationInfo info = TransactionSynchronizationManager.getSynchronizationInfo();
    EntityManagerHolder entityManagerHolder = (EntityManagerHolder)
            info.unbindResource(obtainEntityManagerFactory());
    txObject.setConnectionHolder(null);
    ConnectionHolder connectionHolder = null;
    if (getDataSource() != null && info.hasResource(getDataSource())) {
      connectionHolder = (ConnectionHolder) info.unbindResource(getDataSource());
    }
    return new SuspendedResourcesHolder(entityManagerHolder, connectionHolder);
  }

  @Override
  protected void doResume(@Nullable Object transaction, Object suspendedResources) {
    SuspendedResourcesHolder resourcesHolder = (SuspendedResourcesHolder) suspendedResources;
    SynchronizationInfo info = TransactionSynchronizationManager.getSynchronizationInfo();
    info.bindResource(obtainEntityManagerFactory(), resourcesHolder.getEntityManagerHolder());
    if (getDataSource() != null && resourcesHolder.getConnectionHolder() != null) {
      info.bindResource(getDataSource(), resourcesHolder.getConnectionHolder());
    }
  }

  /**
   * This implementation returns "true": a JPA commit will properly handle
   * transactions that have been marked rollback-only at a global level.
   */
  @Override
  protected boolean shouldCommitOnGlobalRollbackOnly() {
    return true;
  }

  @Override
  protected void doCommit(DefaultTransactionStatus status) {
    JpaTransactionObject txObject = (JpaTransactionObject) status.getTransaction();
    if (status.isDebug()) {
      logger.debug("Committing JPA transaction on EntityManager [{}]",
              txObject.getEntityManagerHolder().getEntityManager());
    }
    try {
      EntityTransaction tx = txObject.getEntityManagerHolder().getEntityManager().getTransaction();
      tx.commit();
    }
    catch (RollbackException ex) {
      if (ex.getCause() instanceof RuntimeException) {
        DataAccessException dae = getJpaDialect().translateExceptionIfPossible((RuntimeException) ex.getCause());
        if (dae != null) {
          throw dae;
        }
      }
      throw new TransactionSystemException("Could not commit JPA transaction", ex);
    }
    catch (RuntimeException ex) {
      // Assumably failed to flush changes to database.
      throw DataAccessUtils.translateIfNecessary(ex, getJpaDialect());
    }
  }

  @Override
  protected void doRollback(DefaultTransactionStatus status) {
    JpaTransactionObject txObject = (JpaTransactionObject) status.getTransaction();
    if (status.isDebug()) {
      logger.debug("Rolling back JPA transaction on EntityManager [{}]",
              txObject.getEntityManagerHolder().getEntityManager());
    }
    try {
      EntityTransaction tx = txObject.getEntityManagerHolder().getEntityManager().getTransaction();
      if (tx.isActive()) {
        tx.rollback();
      }
    }
    catch (PersistenceException ex) {
      throw new TransactionSystemException("Could not roll back JPA transaction", ex);
    }
    finally {
      if (!txObject.isNewEntityManagerHolder()) {
        // Clear all pending inserts/updates/deletes in the EntityManager.
        // Necessary for pre-bound EntityManagers, to avoid inconsistent state.
        txObject.getEntityManagerHolder().getEntityManager().clear();
      }
    }
  }

  @Override
  protected void doSetRollbackOnly(DefaultTransactionStatus status) {
    JpaTransactionObject txObject = (JpaTransactionObject) status.getTransaction();
    if (status.isDebug()) {
      logger.debug("Setting JPA transaction on EntityManager [{}] rollback-only",
              txObject.getEntityManagerHolder().getEntityManager());
    }
    txObject.setRollbackOnly();
  }

  @Override
  protected void doCleanupAfterCompletion(Object transaction) {
    JpaTransactionObject txObject = (JpaTransactionObject) transaction;
    SynchronizationInfo info = TransactionSynchronizationManager.getSynchronizationInfo();
    // Remove the entity manager holder from the thread, if still there.
    // (Could have been removed by EntityManagerFactoryUtils in order
    // to replace it with an unsynchronized EntityManager).
    if (txObject.isNewEntityManagerHolder()) {
      info.unbindResourceIfPossible(obtainEntityManagerFactory());
    }
    txObject.getEntityManagerHolder().clear();

    // Remove the JDBC connection holder from the thread, if exposed.
    if (getDataSource() != null && txObject.hasConnectionHolder()) {
      info.unbindResource(getDataSource());
      ConnectionHandle conHandle = txObject.getConnectionHolder().getConnectionHandle();
      if (conHandle != null) {
        try {
          getJpaDialect().releaseJdbcConnection(conHandle,
                  txObject.getEntityManagerHolder().getEntityManager());
        }
        catch (Throwable ex) {
          // Just log it, to keep a transaction-related exception.
          logger.error("Failed to release JDBC connection after transaction", ex);
        }
      }
    }

    getJpaDialect().cleanupTransaction(txObject.getTransactionData());

    // Remove the entity manager holder from the thread.
    if (txObject.isNewEntityManagerHolder()) {
      EntityManager em = txObject.getEntityManagerHolder().getEntityManager();
      if (logger.isDebugEnabled()) {
        logger.debug("Closing JPA EntityManager [{}] after transaction", em);
      }
      EntityManagerFactoryUtils.closeEntityManager(em);
    }
    else {
      logger.debug("Not closing pre-bound JPA EntityManager after transaction");
    }
  }

  /**
   * JPA transaction object, representing a EntityManagerHolder.
   * Used as transaction object by JpaTransactionManager.
   */
  private class JpaTransactionObject extends JdbcTransactionObjectSupport {

    @Nullable
    private EntityManagerHolder entityManagerHolder;

    private boolean newEntityManagerHolder;

    @Nullable
    private Object transactionData;

    public void setEntityManagerHolder(
            @Nullable EntityManagerHolder entityManagerHolder, boolean newEntityManagerHolder) {

      this.entityManagerHolder = entityManagerHolder;
      this.newEntityManagerHolder = newEntityManagerHolder;
    }

    public EntityManagerHolder getEntityManagerHolder() {
      Assert.state(this.entityManagerHolder != null, "No EntityManagerHolder available");
      return this.entityManagerHolder;
    }

    public boolean hasEntityManagerHolder() {
      return entityManagerHolder != null;
    }

    public boolean isNewEntityManagerHolder() {
      return this.newEntityManagerHolder;
    }

    public boolean hasTransaction() {
      return entityManagerHolder != null && entityManagerHolder.isTransactionActive();
    }

    public void setTransactionData(@Nullable Object transactionData) {
      this.transactionData = transactionData;
      getEntityManagerHolder().setTransactionActive(true);
      if (transactionData instanceof SavepointManager) {
        getEntityManagerHolder().setSavepointManager((SavepointManager) transactionData);
      }
    }

    @Nullable
    public Object getTransactionData() {
      return this.transactionData;
    }

    public void setRollbackOnly() {
      EntityTransaction tx = getEntityManagerHolder().getEntityManager().getTransaction();
      if (tx.isActive()) {
        tx.setRollbackOnly();
      }
      if (hasConnectionHolder()) {
        getConnectionHolder().setRollbackOnly();
      }
    }

    @Override
    public boolean isRollbackOnly() {
      EntityTransaction tx = getEntityManagerHolder().getEntityManager().getTransaction();
      return tx.getRollbackOnly();
    }

    @Override
    public void flush() {
      try {
        getEntityManagerHolder().getEntityManager().flush();
      }
      catch (RuntimeException ex) {
        throw DataAccessUtils.translateIfNecessary(ex, getJpaDialect());
      }
    }

    @Override
    public Object createSavepoint() throws TransactionException {
      if (getEntityManagerHolder().isRollbackOnly()) {
        throw new CannotCreateTransactionException(
                "Cannot create savepoint for transaction which is already marked as rollback-only");
      }
      return getSavepointManager().createSavepoint();
    }

    @Override
    public void rollbackToSavepoint(Object savepoint) throws TransactionException {
      getSavepointManager().rollbackToSavepoint(savepoint);
      getEntityManagerHolder().resetRollbackOnly();
    }

    @Override
    public void releaseSavepoint(Object savepoint) throws TransactionException {
      getSavepointManager().releaseSavepoint(savepoint);
    }

    private SavepointManager getSavepointManager() {
      if (!isSavepointAllowed()) {
        throw new NestedTransactionNotSupportedException(
                "Transaction manager does not allow nested transactions");
      }
      SavepointManager savepointManager = getEntityManagerHolder().getSavepointManager();
      if (savepointManager == null) {
        throw new NestedTransactionNotSupportedException(
                "JpaDialect does not support savepoints - check your JPA provider's capabilities");
      }
      return savepointManager;
    }
  }

  /**
   * JPA-specific transaction definition to be passed to {@link JpaDialect#beginTransaction}.
   */
  private static class JpaTransactionDefinition extends DelegatingTransactionDefinition
          implements ResourceTransactionDefinition {

    private final int timeout;

    private final boolean localResource;

    public JpaTransactionDefinition(TransactionDefinition targetDefinition, int timeout, boolean localResource) {
      super(targetDefinition);
      this.timeout = timeout;
      this.localResource = localResource;
    }

    @Override
    public int getTimeout() {
      return this.timeout;
    }

    @Override
    public boolean isLocalResource() {
      return this.localResource;
    }
  }

  /**
   * Holder for suspended resources.
   * Used internally by {@code doSuspend} and {@code doResume}.
   */
  private static final class SuspendedResourcesHolder {

    private final EntityManagerHolder entityManagerHolder;

    @Nullable
    private final ConnectionHolder connectionHolder;

    private SuspendedResourcesHolder(EntityManagerHolder emHolder, @Nullable ConnectionHolder conHolder) {
      this.entityManagerHolder = emHolder;
      this.connectionHolder = conHolder;
    }

    private EntityManagerHolder getEntityManagerHolder() {
      return this.entityManagerHolder;
    }

    @Nullable
    private ConnectionHolder getConnectionHolder() {
      return this.connectionHolder;
    }
  }

}
