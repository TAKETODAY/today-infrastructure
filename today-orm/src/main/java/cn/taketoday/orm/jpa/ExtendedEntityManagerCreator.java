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

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import cn.taketoday.core.Ordered;
import cn.taketoday.dao.DataAccessException;
import cn.taketoday.dao.support.PersistenceExceptionTranslator;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.transaction.support.ResourceHolderSynchronization;
import cn.taketoday.transaction.support.TransactionSynchronizationManager;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ConcurrentReferenceHashMap;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TransactionRequiredException;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.PersistenceUnitTransactionType;

/**
 * Delegate for creating a variety of {@link EntityManager}
 * proxies that follow the JPA spec's semantics for "extended" EntityManagers.
 *
 * <p>Supports several different variants of "extended" EntityManagers:
 * in particular, an "application-managed extended EntityManager", as defined
 * by {@link EntityManagerFactory#createEntityManager()},
 * as well as a "container-managed extended EntityManager", as defined by
 * {@link jakarta.persistence.PersistenceContextType#EXTENDED}.
 *
 * <p>The original difference between "application-managed" and "container-managed"
 * was the need for explicit joining of an externally managed transaction through
 * the {@link EntityManager#joinTransaction()} method in the "application" case
 * versus the automatic joining on each user-level EntityManager operation in the
 * "container" case. As of JPA 2.1, both join modes are available with both kinds of
 * EntityManagers, so the difference between "application-" and "container-managed"
 * is now primarily in the join mode default and in the restricted lifecycle of a
 * container-managed EntityManager (i.e. tied to the object that it is injected into).
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author Mark Paluch
 * @see EntityManagerFactory#createEntityManager()
 * @see jakarta.persistence.PersistenceContextType#EXTENDED
 * @see EntityManager#joinTransaction()
 * @see SharedEntityManagerCreator
 * @since 4.0
 */
public abstract class ExtendedEntityManagerCreator {

  private static final ConcurrentReferenceHashMap<Class<?>, Class<?>[]>
          cachedEntityManagerInterfaces = new ConcurrentReferenceHashMap<>(4);

  /**
   * Create an application-managed extended EntityManager proxy.
   *
   * @param rawEntityManager the raw EntityManager to decorate
   * @param emfInfo the EntityManagerFactoryInfo to obtain the JpaDialect
   * and PersistenceUnitInfo from
   * @return an application-managed EntityManager that can join transactions
   * but does not participate in them automatically
   */
  public static EntityManager createApplicationManagedEntityManager(
          EntityManager rawEntityManager, EntityManagerFactoryInfo emfInfo) {

    return createProxy(rawEntityManager, emfInfo, false, false);
  }

  /**
   * Create an application-managed extended EntityManager proxy.
   *
   * @param rawEntityManager the raw EntityManager to decorate
   * @param emfInfo the EntityManagerFactoryInfo to obtain the JpaDialect
   * and PersistenceUnitInfo from
   * @param synchronizedWithTransaction whether to automatically join ongoing
   * transactions (according to the JPA 2.1 SynchronizationType rules)
   * @return an application-managed EntityManager that can join transactions
   * but does not participate in them automatically
   * @since 4.0
   */
  public static EntityManager createApplicationManagedEntityManager(
          EntityManager rawEntityManager, EntityManagerFactoryInfo emfInfo, boolean synchronizedWithTransaction) {

    return createProxy(rawEntityManager, emfInfo, false, synchronizedWithTransaction);
  }

  /**
   * Create a container-managed extended EntityManager proxy.
   *
   * @param rawEntityManager the raw EntityManager to decorate
   * @param emfInfo the EntityManagerFactoryInfo to obtain the JpaDialect
   * and PersistenceUnitInfo from
   * @return a container-managed EntityManager that will automatically participate
   * in any managed transaction
   */
  public static EntityManager createContainerManagedEntityManager(
          EntityManager rawEntityManager, EntityManagerFactoryInfo emfInfo) {

    return createProxy(rawEntityManager, emfInfo, true, true);
  }

  /**
   * Create a container-managed extended EntityManager proxy.
   *
   * @param emf the EntityManagerFactory to create the EntityManager with.
   * If this implements the EntityManagerFactoryInfo interface, the corresponding
   * JpaDialect and PersistenceUnitInfo will be detected accordingly.
   * @return a container-managed EntityManager that will automatically participate
   * in any managed transaction
   * @see EntityManagerFactory#createEntityManager()
   */
  public static EntityManager createContainerManagedEntityManager(EntityManagerFactory emf) {
    return createContainerManagedEntityManager(emf, null, true);
  }

  /**
   * Create a container-managed extended EntityManager proxy.
   *
   * @param emf the EntityManagerFactory to create the EntityManager with.
   * If this implements the EntityManagerFactoryInfo interface, the corresponding
   * JpaDialect and PersistenceUnitInfo will be detected accordingly.
   * @param properties the properties to be passed into the {@code createEntityManager}
   * call (may be {@code null})
   * @return a container-managed EntityManager that will automatically participate
   * in any managed transaction
   * @see EntityManagerFactory#createEntityManager(Map)
   */
  public static EntityManager createContainerManagedEntityManager(EntityManagerFactory emf, @Nullable Map<?, ?> properties) {
    return createContainerManagedEntityManager(emf, properties, true);
  }

  /**
   * Create a container-managed extended EntityManager proxy.
   *
   * @param emf the EntityManagerFactory to create the EntityManager with.
   * If this implements the EntityManagerFactoryInfo interface, the corresponding
   * JpaDialect and PersistenceUnitInfo will be detected accordingly.
   * @param properties the properties to be passed into the {@code createEntityManager}
   * call (may be {@code null})
   * @param synchronizedWithTransaction whether to automatically join ongoing
   * transactions (according to the JPA 2.1 SynchronizationType rules)
   * @return a container-managed EntityManager that expects container-driven lifecycle
   * management but may opt out of automatic transaction synchronization
   * @see EntityManagerFactory#createEntityManager(Map)
   */
  public static EntityManager createContainerManagedEntityManager(
          EntityManagerFactory emf, @Nullable Map<?, ?> properties, boolean synchronizedWithTransaction) {

    Assert.notNull(emf, "EntityManagerFactory is required");
    if (emf instanceof EntityManagerFactoryInfo emfInfo) {
      EntityManager rawEntityManager = emfInfo.createNativeEntityManager(properties);
      return createProxy(rawEntityManager, emfInfo, true, synchronizedWithTransaction);
    }
    else {
      EntityManager rawEntityManager =
              CollectionUtils.isNotEmpty(properties) ? emf.createEntityManager(properties) : emf.createEntityManager();
      return createProxy(rawEntityManager, null, null, null, null, true, synchronizedWithTransaction);
    }
  }

  /**
   * Actually create the EntityManager proxy.
   *
   * @param rawEntityManager raw EntityManager
   * @param emfInfo the EntityManagerFactoryInfo to obtain the JpaDialect
   * and PersistenceUnitInfo from
   * @param containerManaged whether to follow container-managed EntityManager
   * or application-managed EntityManager semantics
   * @param synchronizedWithTransaction whether to automatically join ongoing
   * transactions (according to the JPA 2.1 SynchronizationType rules)
   * @return the EntityManager proxy
   */
  private static EntityManager createProxy(EntityManager rawEntityManager,
          EntityManagerFactoryInfo emfInfo, boolean containerManaged, boolean synchronizedWithTransaction) {

    Assert.notNull(emfInfo, "EntityManagerFactoryInfo is required");
    JpaDialect jpaDialect = emfInfo.getJpaDialect();
    PersistenceUnitInfo pui = emfInfo.getPersistenceUnitInfo();
    Boolean jta = (pui != null ? pui.getTransactionType() == PersistenceUnitTransactionType.JTA : null);
    return createProxy(rawEntityManager, emfInfo.getEntityManagerInterface(),
            emfInfo.getBeanClassLoader(), jpaDialect, jta, containerManaged, synchronizedWithTransaction);
  }

  /**
   * Actually create the EntityManager proxy.
   *
   * @param rawEm raw EntityManager
   * @param emIfc the (potentially vendor-specific) EntityManager
   * interface to proxy, or {@code null} for default detection of all interfaces
   * @param cl the ClassLoader to use for proxy creation (maybe {@code null})
   * @param exceptionTranslator the PersistenceException translator to use
   * @param jta whether to create a JTA-aware EntityManager
   * (or {@code null} if not known in advance)
   * @param containerManaged whether to follow container-managed EntityManager
   * or application-managed EntityManager semantics
   * @param synchronizedWithTransaction whether to automatically join ongoing
   * transactions (according to the JPA 2.1 SynchronizationType rules)
   * @return the EntityManager proxy
   */
  private static EntityManager createProxy(
          EntityManager rawEm, @Nullable Class<? extends EntityManager> emIfc, @Nullable ClassLoader cl,
          @Nullable PersistenceExceptionTranslator exceptionTranslator, @Nullable Boolean jta,
          boolean containerManaged, boolean synchronizedWithTransaction) {

    Assert.notNull(rawEm, "EntityManager is required");
    Class<?>[] interfaces;

    if (emIfc != null) {
      interfaces = cachedEntityManagerInterfaces.computeIfAbsent(emIfc, key -> {
        if (EntityManagerProxy.class.equals(key)) {
          return new Class<?>[] { key };
        }
        return new Class<?>[] { key, EntityManagerProxy.class };
      });
    }
    else {
      interfaces = cachedEntityManagerInterfaces.computeIfAbsent(rawEm.getClass(), key -> {
        Set<Class<?>> ifcs = new LinkedHashSet<>(ClassUtils.getAllInterfacesForClassAsSet(key, cl));
        ifcs.add(EntityManagerProxy.class);
        return ClassUtils.toClassArray(ifcs);
      });
    }

    return (EntityManager) Proxy.newProxyInstance(
            (cl != null ? cl : ExtendedEntityManagerCreator.class.getClassLoader()),
            interfaces,
            new ExtendedEntityManagerInvocationHandler(
                    rawEm, exceptionTranslator, jta, containerManaged, synchronizedWithTransaction));
  }

  /**
   * InvocationHandler for extended EntityManagers as defined in the JPA spec.
   */
  @SuppressWarnings("serial")
  private static final class ExtendedEntityManagerInvocationHandler implements InvocationHandler, Serializable {

    private static final Logger logger = LoggerFactory.getLogger(ExtendedEntityManagerInvocationHandler.class);

    private final EntityManager target;

    @Nullable
    private final PersistenceExceptionTranslator exceptionTranslator;

    private final boolean jta;

    private final boolean containerManaged;

    private final boolean synchronizedWithTransaction;

    private ExtendedEntityManagerInvocationHandler(EntityManager target,
            @Nullable PersistenceExceptionTranslator exceptionTranslator, @Nullable Boolean jta,
            boolean containerManaged, boolean synchronizedWithTransaction) {

      this.target = target;
      this.exceptionTranslator = exceptionTranslator;
      this.jta = (jta != null ? jta : isJtaEntityManager());
      this.containerManaged = containerManaged;
      this.synchronizedWithTransaction = synchronizedWithTransaction;
    }

    private boolean isJtaEntityManager() {
      try {
        this.target.getTransaction();
        return false;
      }
      catch (IllegalStateException ex) {
        logger.debug("Cannot access EntityTransaction handle - assuming we're in a JTA environment");
        return true;
      }
    }

    @Override
    @Nullable
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      // Invocation on EntityManager interface coming in...

      switch (method.getName()) {
        case "equals":
          // Only consider equal when proxies are identical.
          return (proxy == args[0]);
        case "hashCode":
          // Use hashCode of EntityManager proxy.
          return hashCode();
        case "getTargetEntityManager":
          // Handle EntityManagerProxy interface.
          return this.target;
        case "unwrap":
          // Handle JPA 2.0 unwrap method - could be a proxy match.
          Class<?> targetClass = (Class<?>) args[0];
          if (targetClass == null) {
            return this.target;
          }
          else if (targetClass.isInstance(proxy)) {
            return proxy;
          }
          break;
        case "isOpen":
          if (this.containerManaged) {
            return true;
          }
          break;
        case "close":
          if (this.containerManaged) {
            throw new IllegalStateException("Invalid usage: Cannot close a container-managed EntityManager");
          }
          ExtendedEntityManagerSynchronization synch = (ExtendedEntityManagerSynchronization)
                  TransactionSynchronizationManager.getResource(this.target);
          if (synch != null) {
            // Local transaction joined - don't actually call close() before transaction completion
            synch.closeOnCompletion = true;
            return null;
          }
          break;
        case "getTransaction":
          if (this.synchronizedWithTransaction) {
            throw new IllegalStateException(
                    "Cannot obtain local EntityTransaction from a transaction-synchronized EntityManager");
          }
          break;
        case "joinTransaction":
          doJoinTransaction(true);
          return null;
        case "isJoinedToTransaction":
          // Handle JPA 2.1 isJoinedToTransaction method for the non-JTA case.
          if (!this.jta) {
            return TransactionSynchronizationManager.hasResource(this.target);
          }
          break;
      }

      // Do automatic joining if required. Excludes toString, equals, hashCode calls.
      if (this.synchronizedWithTransaction && method.getDeclaringClass().isInterface()) {
        doJoinTransaction(false);
      }

      // Invoke method on current EntityManager.
      try {
        return method.invoke(this.target, args);
      }
      catch (InvocationTargetException ex) {
        throw ex.getTargetException();
      }
    }

    /**
     * Join an existing transaction, if not already joined.
     *
     * @param enforce whether to enforce the transaction
     * (i.e. whether failure to join is considered fatal)
     */
    private void doJoinTransaction(boolean enforce) {
      if (this.jta) {
        // Let's try whether we're in a JTA transaction.
        try {
          this.target.joinTransaction();
          logger.debug("Joined JTA transaction");
        }
        catch (TransactionRequiredException ex) {
          if (!enforce) {
            logger.debug("No JTA transaction to join: " + ex);
          }
          else {
            throw ex;
          }
        }
      }
      else {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
          if (!TransactionSynchronizationManager.hasResource(this.target) &&
                  !this.target.getTransaction().isActive()) {
            enlistInCurrentTransaction();
          }
          logger.debug("Joined local transaction");
        }
        else {
          if (!enforce) {
            logger.debug("No local transaction to join");
          }
          else {
            throw new TransactionRequiredException("No local transaction to join");
          }
        }
      }
    }

    /**
     * Enlist this application-managed EntityManager in the current transaction.
     */
    private void enlistInCurrentTransaction() {
      // Resource local transaction, need to acquire the EntityTransaction,
      // start a transaction now and enlist a synchronization for commit or rollback later.
      EntityTransaction et = this.target.getTransaction();
      et.begin();
      if (logger.isDebugEnabled()) {
        logger.debug("Starting resource-local transaction on application-managed EntityManager [{}]", target);
      }
      ExtendedEntityManagerSynchronization extendedEntityManagerSynchronization =
              new ExtendedEntityManagerSynchronization(this.target, this.exceptionTranslator);
      TransactionSynchronizationManager.bindResource(this.target, extendedEntityManagerSynchronization);
      TransactionSynchronizationManager.registerSynchronization(extendedEntityManagerSynchronization);
    }
  }

  /**
   * TransactionSynchronization enlisting an extended EntityManager
   * with a current Framework transaction.
   */
  private static class ExtendedEntityManagerSynchronization
          extends ResourceHolderSynchronization<EntityManagerHolder, EntityManager>
          implements Ordered {

    private final EntityManager entityManager;

    @Nullable
    private final PersistenceExceptionTranslator exceptionTranslator;

    public volatile boolean closeOnCompletion;

    public ExtendedEntityManagerSynchronization(
            EntityManager em, @Nullable PersistenceExceptionTranslator exceptionTranslator) {

      super(new EntityManagerHolder(em), em);
      this.entityManager = em;
      this.exceptionTranslator = exceptionTranslator;
    }

    @Override
    public int getOrder() {
      return EntityManagerFactoryUtils.ENTITY_MANAGER_SYNCHRONIZATION_ORDER - 1;
    }

    @Override
    protected void flushResource(EntityManagerHolder resourceHolder) {
      try {
        this.entityManager.flush();
      }
      catch (RuntimeException ex) {
        throw convertException(ex);
      }
    }

    @Override
    protected boolean shouldReleaseBeforeCompletion() {
      return false;
    }

    @Override
    public void afterCommit() {
      super.afterCommit();
      // Trigger commit here to let exceptions propagate to the caller.
      try {
        this.entityManager.getTransaction().commit();
      }
      catch (RuntimeException ex) {
        throw convertException(ex);
      }
    }

    @Override
    public void afterCompletion(int status) {
      try {
        super.afterCompletion(status);
        if (status != STATUS_COMMITTED) {
          // Haven't had an afterCommit call: trigger a rollback.
          try {
            this.entityManager.getTransaction().rollback();
          }
          catch (RuntimeException ex) {
            throw convertException(ex);
          }
        }
      }
      finally {
        if (this.closeOnCompletion) {
          EntityManagerFactoryUtils.closeEntityManager(this.entityManager);
        }
      }
    }

    private RuntimeException convertException(RuntimeException ex) {
      DataAccessException dae = (this.exceptionTranslator != null) ?
                                this.exceptionTranslator.translateExceptionIfPossible(ex) :
                                EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(ex);
      return (dae != null ? dae : ex);
    }
  }

}
