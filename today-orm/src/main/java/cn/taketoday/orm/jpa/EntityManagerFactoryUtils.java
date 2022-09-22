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

import java.util.Map;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.core.Ordered;
import cn.taketoday.dao.CannotAcquireLockException;
import cn.taketoday.dao.DataAccessException;
import cn.taketoday.dao.DataAccessResourceFailureException;
import cn.taketoday.dao.DataIntegrityViolationException;
import cn.taketoday.dao.EmptyResultDataAccessException;
import cn.taketoday.dao.IncorrectResultSizeDataAccessException;
import cn.taketoday.dao.InvalidDataAccessApiUsageException;
import cn.taketoday.dao.PessimisticLockingFailureException;
import cn.taketoday.jdbc.datasource.DataSourceUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.transaction.support.ResourceHolderSynchronization;
import cn.taketoday.transaction.support.SynchronizationInfo;
import cn.taketoday.transaction.support.TransactionSynchronizationManager;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.StringUtils;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.PessimisticLockException;
import jakarta.persistence.Query;
import jakarta.persistence.QueryTimeoutException;
import jakarta.persistence.SynchronizationType;
import jakarta.persistence.TransactionRequiredException;

/**
 * Helper class featuring methods for JPA EntityManager handling,
 * allowing for reuse of EntityManager instances within transactions.
 * Also provides support for exception translation.
 *
 * <p>Mainly intended for internal use within the framework.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
public abstract class EntityManagerFactoryUtils {

  /**
   * Order value for TransactionSynchronization objects that clean up JPA
   * EntityManagers. Return DataSourceUtils.CONNECTION_SYNCHRONIZATION_ORDER - 100
   * to execute EntityManager cleanup before JDBC Connection cleanup, if any.
   *
   * @see cn.taketoday.jdbc.datasource.DataSourceUtils#CONNECTION_SYNCHRONIZATION_ORDER
   */
  public static final int ENTITY_MANAGER_SYNCHRONIZATION_ORDER =
          DataSourceUtils.CONNECTION_SYNCHRONIZATION_ORDER - 100;

  private static final Logger logger = LoggerFactory.getLogger(EntityManagerFactoryUtils.class);

  /**
   * Find an EntityManagerFactory with the given name in the given
   * Framework application context (represented as BeanFactory).
   * <p>The specified unit name will be matched against the configured
   * persistence unit, provided that a discovered EntityManagerFactory
   * implements the {@link EntityManagerFactoryInfo} interface. If not,
   * the persistence unit name will be matched against the Framework bean name,
   * assuming that the EntityManagerFactory bean names follow that convention.
   * <p>If no unit name has been given, this method will search for a default
   * EntityManagerFactory through {@link BeanFactory#getBean(Class)}.
   *
   * @param beanFactory the BeanFactory to search
   * @param unitName the name of the persistence unit (may be {@code null} or empty,
   * in which case a single bean of type EntityManagerFactory will be searched for)
   * @return the EntityManagerFactory
   * @throws NoSuchBeanDefinitionException if there is no such EntityManagerFactory in the context
   * @see EntityManagerFactoryInfo#getPersistenceUnitName()
   */
  public static EntityManagerFactory findEntityManagerFactory(
          BeanFactory beanFactory, @Nullable String unitName) throws NoSuchBeanDefinitionException {
    Assert.notNull(beanFactory, "BeanFactory must not be null");
    if (StringUtils.isNotEmpty(unitName)) {
      // See whether we can find an EntityManagerFactory with matching persistence unit name.
      var candidateNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, EntityManagerFactory.class);
      for (String candidateName : candidateNames) {
        EntityManagerFactory emf = (EntityManagerFactory) beanFactory.getBean(candidateName);
        if (emf instanceof EntityManagerFactoryInfo info
                && unitName.equals(info.getPersistenceUnitName())) {
          return emf;
        }
      }
      // No matching persistence unit found - simply take the EntityManagerFactory
      // with the persistence unit name as bean name (by convention).
      return beanFactory.getBean(unitName, EntityManagerFactory.class);
    }
    else {
      // Find unique EntityManagerFactory bean in the context, falling back to parent contexts.
      return beanFactory.getBean(EntityManagerFactory.class);
    }
  }

  /**
   * Obtain a JPA EntityManager from the given factory. Is aware of a corresponding
   * EntityManager bound to the current thread, e.g. when using JpaTransactionManager.
   * <p>Note: Will return {@code null} if no thread-bound EntityManager found!
   *
   * @param emf the EntityManagerFactory to create the EntityManager with
   * @return the EntityManager, or {@code null} if none found
   * @throws DataAccessResourceFailureException if the EntityManager couldn't be obtained
   * @see JpaTransactionManager
   */
  @Nullable
  public static EntityManager getTransactionalEntityManager(EntityManagerFactory emf)
          throws DataAccessResourceFailureException {

    return getTransactionalEntityManager(emf, null);
  }

  /**
   * Obtain a JPA EntityManager from the given factory. Is aware of a corresponding
   * EntityManager bound to the current thread, e.g. when using JpaTransactionManager.
   * <p>Note: Will return {@code null} if no thread-bound EntityManager found!
   *
   * @param emf the EntityManagerFactory to create the EntityManager with
   * @param properties the properties to be passed into the {@code createEntityManager}
   * call (may be {@code null})
   * @return the EntityManager, or {@code null} if none found
   * @throws DataAccessResourceFailureException if the EntityManager couldn't be obtained
   * @see JpaTransactionManager
   */
  @Nullable
  public static EntityManager getTransactionalEntityManager(EntityManagerFactory emf, @Nullable Map<?, ?> properties)
          throws DataAccessResourceFailureException {
    try {
      return doGetTransactionalEntityManager(emf, properties, true);
    }
    catch (PersistenceException ex) {
      throw new DataAccessResourceFailureException("Could not obtain JPA EntityManager", ex);
    }
  }

  /**
   * Obtain a JPA EntityManager from the given factory. Is aware of a corresponding
   * EntityManager bound to the current thread, e.g. when using JpaTransactionManager.
   * <p>Same as {@code getEntityManager}, but throwing the original PersistenceException.
   *
   * @param emf the EntityManagerFactory to create the EntityManager with
   * @param properties the properties to be passed into the {@code createEntityManager}
   * call (may be {@code null})
   * @return the EntityManager, or {@code null} if none found
   * @throws PersistenceException if the EntityManager couldn't be created
   * @see #getTransactionalEntityManager(EntityManagerFactory)
   * @see JpaTransactionManager
   */
  @Nullable
  public static EntityManager doGetTransactionalEntityManager(EntityManagerFactory emf, Map<?, ?> properties)
          throws PersistenceException {

    return doGetTransactionalEntityManager(emf, properties, true);
  }

  /**
   * Obtain a JPA EntityManager from the given factory. Is aware of a corresponding
   * EntityManager bound to the current thread, e.g. when using JpaTransactionManager.
   * <p>Same as {@code getEntityManager}, but throwing the original PersistenceException.
   *
   * @param emf the EntityManagerFactory to create the EntityManager with
   * @param properties the properties to be passed into the {@code createEntityManager}
   * call (may be {@code null})
   * @param synchronizedWithTransaction whether to automatically join ongoing
   * transactions (according to the JPA 2.1 SynchronizationType rules)
   * @return the EntityManager, or {@code null} if none found
   * @throws PersistenceException if the EntityManager couldn't be created
   * @see #getTransactionalEntityManager(EntityManagerFactory)
   * @see JpaTransactionManager
   */
  @Nullable
  public static EntityManager doGetTransactionalEntityManager(
          EntityManagerFactory emf, @Nullable Map<?, ?> properties, boolean synchronizedWithTransaction)
          throws PersistenceException {

    Assert.notNull(emf, "No EntityManagerFactory specified");
    SynchronizationInfo info = TransactionSynchronizationManager.getSynchronizationInfo();
    EntityManagerHolder emHolder =
            info.getResource(emf);
    if (emHolder != null) {
      if (synchronizedWithTransaction) {
        if (!emHolder.isSynchronizedWithTransaction()) {
          if (info.isActualTransactionActive()) {
            // Try to explicitly synchronize the EntityManager itself
            // with an ongoing JTA transaction, if any.
            try {
              emHolder.getEntityManager().joinTransaction();
            }
            catch (TransactionRequiredException ex) {
              logger.debug("Could not join transaction because none was actually active", ex);
            }
          }
          if (info.isSynchronizationActive()) {
            Object transactionData = prepareTransaction(emHolder.getEntityManager(), emf);
            info.registerSynchronization(
                    new TransactionalEntityManagerSynchronization(emHolder, emf, transactionData, false));
            emHolder.setSynchronizedWithTransaction(true);
          }
        }
        // Use holder's reference count to track synchronizedWithTransaction access.
        // isOpen() check used below to find out about it.
        emHolder.requested();
        return emHolder.getEntityManager();
      }
      else {
        // unsynchronized EntityManager demanded
        if (emHolder.isTransactionActive() && !emHolder.isOpen()) {
          if (!info.isSynchronizationActive()) {
            return null;
          }
          // EntityManagerHolder with an active transaction coming from JpaTransactionManager,
          // with no synchronized EntityManager having been requested by application code before.
          // Unbind in order to register a new unsynchronized EntityManager instead.
          info.unbindResource(emf);
        }
        else {
          // Either a previously bound unsynchronized EntityManager, or the application
          // has requested a synchronized EntityManager before and therefore upgraded
          // this transaction's EntityManager to synchronized before.
          return emHolder.getEntityManager();
        }
      }
    }
    else if (!info.isSynchronizationActive()) {
      // Indicate that we can't obtain a transactional EntityManager.
      return null;
    }

    // Create a new EntityManager for use within the current transaction.
    logger.debug("Opening JPA EntityManager");
    EntityManager em = null;
    if (!synchronizedWithTransaction) {
      try {
        em = emf.createEntityManager(SynchronizationType.UNSYNCHRONIZED, properties);
      }
      catch (AbstractMethodError err) {
        // JPA 2.1 API available but method not actually implemented in persistence provider:
        // falling back to regular createEntityManager method.
      }
    }
    if (em == null) {
      em = CollectionUtils.isNotEmpty(properties) ? emf.createEntityManager(properties) : emf.createEntityManager();
    }

    try {
      // Use same EntityManager for further JPA operations within the transaction.
      // Thread-bound object will get removed by synchronization at transaction completion.
      emHolder = new EntityManagerHolder(em);
      if (synchronizedWithTransaction) {
        Object transactionData = prepareTransaction(em, emf);
        info.registerSynchronization(
                new TransactionalEntityManagerSynchronization(emHolder, emf, transactionData, true));
        emHolder.setSynchronizedWithTransaction(true);
      }
      else {
        // Unsynchronized - just scope it for the transaction, as demanded by the JPA 2.1 spec...
        info.registerSynchronization(
                new TransactionScopedEntityManagerSynchronization(emHolder, emf));
      }
      info.bindResource(emf, emHolder);
    }
    catch (RuntimeException ex) {
      // Unexpected exception from external delegation call -> close EntityManager and rethrow.
      closeEntityManager(em);
      throw ex;
    }

    return em;
  }

  /**
   * Prepare a transaction on the given EntityManager, if possible.
   *
   * @param em the EntityManager to prepare
   * @param emf the EntityManagerFactory that the EntityManager has been created with
   * @return an arbitrary object that holds transaction data, if any
   * (to be passed into cleanupTransaction)
   * @see JpaDialect#prepareTransaction
   */
  @Nullable
  private static Object prepareTransaction(EntityManager em, EntityManagerFactory emf) {
    if (emf instanceof EntityManagerFactoryInfo emfInfo) {
      JpaDialect jpaDialect = emfInfo.getJpaDialect();
      if (jpaDialect != null) {
        return jpaDialect.prepareTransaction(em,
                TransactionSynchronizationManager.isCurrentTransactionReadOnly(),
                TransactionSynchronizationManager.getCurrentTransactionName());
      }
    }
    return null;
  }

  /**
   * Prepare a transaction on the given EntityManager, if possible.
   *
   * @param transactionData arbitrary object that holds transaction data, if any
   * (as returned by prepareTransaction)
   * @param emf the EntityManagerFactory that the EntityManager has been created with
   * @see JpaDialect#cleanupTransaction
   */
  private static void cleanupTransaction(@Nullable Object transactionData, EntityManagerFactory emf) {
    if (emf instanceof EntityManagerFactoryInfo emfInfo) {
      JpaDialect jpaDialect = emfInfo.getJpaDialect();
      if (jpaDialect != null) {
        jpaDialect.cleanupTransaction(transactionData);
      }
    }
  }

  /**
   * Apply the current transaction timeout, if any, to the given JPA Query object.
   * <p>This method sets the JPA query hint "jakarta.persistence.query.timeout" accordingly.
   *
   * @param query the JPA Query object
   * @param emf the JPA EntityManagerFactory that the Query was created for
   */
  public static void applyTransactionTimeout(Query query, EntityManagerFactory emf) {
    EntityManagerHolder emHolder = TransactionSynchronizationManager.getResource(emf);
    if (emHolder != null && emHolder.hasTimeout()) {
      int timeoutValue = (int) emHolder.getTimeToLiveInMillis();
      try {
        query.setHint("jakarta.persistence.query.timeout", timeoutValue);
      }
      catch (IllegalArgumentException ex) {
        // oh well, at least we tried...
      }
    }
  }

  /**
   * Convert the given runtime exception to an appropriate exception from the
   * {@code cn.taketoday.dao} hierarchy.
   * Return null if no translation is appropriate: any other exception may
   * have resulted from user code, and should not be translated.
   * <p>The most important cases like object not found or optimistic locking failure
   * are covered here. For more fine-granular conversion, JpaTransactionManager etc
   * support sophisticated translation of exceptions via a JpaDialect.
   *
   * @param ex runtime exception that occurred
   * @return the corresponding DataAccessException instance,
   * or {@code null} if the exception should not be translated
   */
  @Nullable
  public static DataAccessException convertJpaAccessExceptionIfPossible(RuntimeException ex) {
    // Following the JPA specification, a persistence provider can also
    // throw these two exceptions, besides PersistenceException.
    if (ex instanceof IllegalStateException) {
      return new InvalidDataAccessApiUsageException(ex.getMessage(), ex);
    }
    if (ex instanceof IllegalArgumentException) {
      return new InvalidDataAccessApiUsageException(ex.getMessage(), ex);
    }

    // Check for well-known PersistenceException subclasses.
    if (ex instanceof EntityNotFoundException) {
      return new JpaObjectRetrievalFailureException((EntityNotFoundException) ex);
    }
    if (ex instanceof NoResultException) {
      return new EmptyResultDataAccessException(ex.getMessage(), 1, ex);
    }
    if (ex instanceof NonUniqueResultException) {
      return new IncorrectResultSizeDataAccessException(ex.getMessage(), 1, ex);
    }
    if (ex instanceof QueryTimeoutException) {
      return new cn.taketoday.dao.QueryTimeoutException(ex.getMessage(), ex);
    }
    if (ex instanceof LockTimeoutException) {
      return new CannotAcquireLockException(ex.getMessage(), ex);
    }
    if (ex instanceof PessimisticLockException) {
      return new PessimisticLockingFailureException(ex.getMessage(), ex);
    }
    if (ex instanceof OptimisticLockException) {
      return new JpaOptimisticLockingFailureException((OptimisticLockException) ex);
    }
    if (ex instanceof EntityExistsException) {
      return new DataIntegrityViolationException(ex.getMessage(), ex);
    }
    if (ex instanceof TransactionRequiredException) {
      return new InvalidDataAccessApiUsageException(ex.getMessage(), ex);
    }

    // If we have another kind of PersistenceException, throw it.
    if (ex instanceof PersistenceException) {
      return new JpaSystemException(ex);
    }

    // If we get here, we have an exception that resulted from user code,
    // rather than the persistence provider, so we return null to indicate
    // that translation should not occur.
    return null;
  }

  /**
   * Close the given JPA EntityManager,
   * catching and logging any cleanup exceptions thrown.
   *
   * @param em the JPA EntityManager to close (may be {@code null})
   * @see EntityManager#close()
   */
  public static void closeEntityManager(@Nullable EntityManager em) {
    if (em != null) {
      try {
        if (em.isOpen()) {
          em.close();
        }
      }
      catch (Throwable ex) {
        logger.error("Failed to release JPA EntityManager", ex);
      }
    }
  }

  /**
   * Callback for resource cleanup at the end of a non-JPA transaction
   * (e.g. when participating in a JtaTransactionManager transaction),
   * fully synchronized with the ongoing transaction.
   *
   * @see cn.taketoday.transaction.jta.JtaTransactionManager
   */
  private static class TransactionalEntityManagerSynchronization
          extends ResourceHolderSynchronization<EntityManagerHolder, EntityManagerFactory>
          implements Ordered {

    @Nullable
    private final Object transactionData;

    @Nullable
    private final JpaDialect jpaDialect;

    private final boolean newEntityManager;

    public TransactionalEntityManagerSynchronization(
            EntityManagerHolder emHolder, EntityManagerFactory emf, @Nullable Object txData, boolean newEm) {

      super(emHolder, emf);
      this.transactionData = txData;
      this.jpaDialect = emf instanceof EntityManagerFactoryInfo info
                        ? info.getJpaDialect() : null;
      this.newEntityManager = newEm;
    }

    @Override
    public int getOrder() {
      return ENTITY_MANAGER_SYNCHRONIZATION_ORDER;
    }

    @Override
    protected void flushResource(EntityManagerHolder resourceHolder) {
      EntityManager em = resourceHolder.getEntityManager();
      if (em instanceof EntityManagerProxy proxy) {
        EntityManager target = proxy.getTargetEntityManager();
        if (TransactionSynchronizationManager.hasResource(target)) {
          // ExtendedEntityManagerSynchronization active after joinTransaction() call:
          // flush synchronization already registered.
          return;
        }
      }
      try {
        em.flush();
      }
      catch (RuntimeException ex) {
        DataAccessException dae;
        if (this.jpaDialect != null) {
          dae = this.jpaDialect.translateExceptionIfPossible(ex);
        }
        else {
          dae = convertJpaAccessExceptionIfPossible(ex);
        }
        throw dae != null ? dae : ex;
      }
    }

    @Override
    protected boolean shouldUnbindAtCompletion() {
      return this.newEntityManager;
    }

    @Override
    protected void releaseResource(EntityManagerHolder resourceHolder, EntityManagerFactory resourceKey) {
      closeEntityManager(resourceHolder.getEntityManager());
    }

    @Override
    protected void cleanupResource(
            EntityManagerHolder resourceHolder, EntityManagerFactory resourceKey, boolean committed) {

      if (!committed) {
        // Clear all pending inserts/updates/deletes in the EntityManager.
        // Necessary for pre-bound EntityManagers, to avoid inconsistent state.
        resourceHolder.getEntityManager().clear();
      }
      cleanupTransaction(this.transactionData, resourceKey);
    }
  }

  /**
   * Minimal callback that just closes the EntityManager at the end of the transaction.
   */
  private static class TransactionScopedEntityManagerSynchronization
          extends ResourceHolderSynchronization<EntityManagerHolder, EntityManagerFactory>
          implements Ordered {

    public TransactionScopedEntityManagerSynchronization(EntityManagerHolder emHolder, EntityManagerFactory emf) {
      super(emHolder, emf);
    }

    @Override
    public int getOrder() {
      return ENTITY_MANAGER_SYNCHRONIZATION_ORDER + 1;
    }

    @Override
    protected void releaseResource(EntityManagerHolder resourceHolder, EntityManagerFactory resourceKey) {
      closeEntityManager(resourceHolder.getEntityManager());
    }
  }

}
