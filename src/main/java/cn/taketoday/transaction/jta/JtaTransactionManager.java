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

package cn.taketoday.transaction.jta;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Properties;

import javax.naming.NamingException;

import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.jndi.JndiTemplate;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.transaction.CannotCreateTransactionException;
import cn.taketoday.transaction.HeuristicCompletionException;
import cn.taketoday.transaction.IllegalTransactionStateException;
import cn.taketoday.transaction.InvalidIsolationLevelException;
import cn.taketoday.transaction.NestedTransactionNotSupportedException;
import cn.taketoday.transaction.PlatformTransactionManager;
import cn.taketoday.transaction.TransactionDefinition;
import cn.taketoday.transaction.TransactionSuspensionNotSupportedException;
import cn.taketoday.transaction.TransactionSystemException;
import cn.taketoday.transaction.UnexpectedRollbackException;
import cn.taketoday.transaction.support.AbstractPlatformTransactionManager;
import cn.taketoday.transaction.support.DefaultTransactionStatus;
import cn.taketoday.transaction.support.TransactionSynchronization;
import cn.taketoday.util.StringUtils;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.InvalidTransactionException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;
import jakarta.transaction.UserTransaction;

/**
 * {@link PlatformTransactionManager} implementation
 * for JTA, delegating to a backend JTA provider. This is typically used to delegate
 * to a Jakarta EE server's transaction coordinator, but may also be configured with a
 * local JTA provider which is embedded within the application.
 *
 * <p>This transaction manager is appropriate for handling distributed transactions,
 * i.e. transactions that span multiple resources, and for controlling transactions on
 * application server resources (e.g. JDBC DataSources available in JNDI) in general.
 * For a single JDBC DataSource, DataSourceTransactionManager is perfectly sufficient,
 * and for accessing a single resource with Hibernate (including transactional cache),
 * HibernateTransactionManager is appropriate, for example.
 *
 * <p><b>For typical JTA transactions (REQUIRED, SUPPORTS, MANDATORY, NEVER), a plain
 * JtaTransactionManager definition is all you need, portable across all Jakarta EE servers.</b>
 * This corresponds to the functionality of the JTA UserTransaction, for which Jakarta EE
 * specifies a standard JNDI name ("java:comp/UserTransaction"). There is no need to
 * configure a server-specific TransactionManager lookup for this kind of JTA usage.
 *
 * <p><b>Transaction suspension (REQUIRES_NEW, NOT_SUPPORTED) is just available with a
 * JTA TransactionManager being registered.</b> Common TransactionManager locations are
 * autodetected by JtaTransactionManager, provided that the "autodetectTransactionManager"
 * flag is set to "true" (which it is by default).
 *
 * <p>Note: Support for the JTA TransactionManager interface is not required by Jakarta EE.
 * Almost all Jakarta EE servers expose it, but do so as extension to EE. There might be some
 * issues with compatibility, despite the TransactionManager interface being part of JTA.
 *
 * <p>This pure JtaTransactionManager class supports timeouts but not per-transaction
 * isolation levels. Custom subclasses may override the {@link #doJtaBegin} method for
 * specific JTA extensions in order to provide this functionality. Such adapters for
 * specific Jakarta EE transaction coordinators may also expose transaction names for
 * monitoring; with standard JTA, transaction names will simply be ignored.
 *
 * <p>JTA 1.1 adds the TransactionSynchronizationRegistry facility, as public Jakarta EE
 * API in addition to the standard JTA UserTransaction handle. this
 * JtaTransactionManager autodetects the TransactionSynchronizationRegistry and uses
 * it for registering Framework-managed synchronizations when participating in an existing
 * JTA transaction (e.g. controlled by EJB CMT). If no TransactionSynchronizationRegistry
 * is available, then such synchronizations will be registered via the (non-EE) JTA
 * TransactionManager handle.
 *
 * <p>This class is serializable. However, active synchronizations do not survive serialization.
 *
 * @author Juergen Hoeller
 * @see UserTransaction
 * @see TransactionManager
 * @see TransactionSynchronizationRegistry
 * @see #setUserTransactionName
 * @see #setUserTransaction
 * @see #setTransactionManagerName
 * @see #setTransactionManager
 * @since 4.0
 */
@SuppressWarnings("serial")
public class JtaTransactionManager extends AbstractPlatformTransactionManager
        implements TransactionFactory, InitializingBean, Serializable {

  /**
   * Default JNDI location for the JTA UserTransaction. Many Jakarta EE servers
   * also provide support for the JTA TransactionManager interface there.
   *
   * @see #setUserTransactionName
   * @see #setAutodetectTransactionManager
   */
  public static final String DEFAULT_USER_TRANSACTION_NAME = "java:comp/UserTransaction";

  /**
   * Fallback JNDI locations for the JTA TransactionManager. Applied if
   * the JTA UserTransaction does not implement the JTA TransactionManager
   * interface, provided that the "autodetectTransactionManager" flag is "true".
   *
   * @see #setTransactionManagerName
   * @see #setAutodetectTransactionManager
   */
  public static final String[] FALLBACK_TRANSACTION_MANAGER_NAMES =
          new String[] { "java:comp/TransactionManager", "java:appserver/TransactionManager",
                  "java:pm/TransactionManager", "java:/TransactionManager" };

  /**
   * Standard Jakarta EE JNDI location for the JTA TransactionSynchronizationRegistry.
   * Autodetected when available.
   */
  public static final String DEFAULT_TRANSACTION_SYNCHRONIZATION_REGISTRY_NAME =
          "java:comp/TransactionSynchronizationRegistry";

  private transient JndiTemplate jndiTemplate = new JndiTemplate();

  @Nullable
  private transient UserTransaction userTransaction;

  @Nullable
  private String userTransactionName;

  private boolean autodetectUserTransaction = true;

  private boolean cacheUserTransaction = true;

  private boolean userTransactionObtainedFromJndi = false;

  @Nullable
  private transient TransactionManager transactionManager;

  @Nullable
  private String transactionManagerName;

  private boolean autodetectTransactionManager = true;

  @Nullable
  private transient TransactionSynchronizationRegistry transactionSynchronizationRegistry;

  @Nullable
  private String transactionSynchronizationRegistryName;

  private boolean autodetectTransactionSynchronizationRegistry = true;

  private boolean allowCustomIsolationLevels = false;

  /**
   * Create a new JtaTransactionManager instance, to be configured as bean.
   * Invoke {@code afterPropertiesSet} to activate the configuration.
   *
   * @see #setUserTransactionName
   * @see #setUserTransaction
   * @see #setTransactionManagerName
   * @see #setTransactionManager
   * @see #afterPropertiesSet()
   */
  public JtaTransactionManager() {
    setNestedTransactionAllowed(true);
  }

  /**
   * Create a new JtaTransactionManager instance.
   *
   * @param userTransaction the JTA UserTransaction to use as direct reference
   */
  public JtaTransactionManager(UserTransaction userTransaction) {
    this();
    Assert.notNull(userTransaction, "UserTransaction must not be null");
    this.userTransaction = userTransaction;
  }

  /**
   * Create a new JtaTransactionManager instance.
   *
   * @param userTransaction the JTA UserTransaction to use as direct reference
   * @param transactionManager the JTA TransactionManager to use as direct reference
   */
  public JtaTransactionManager(UserTransaction userTransaction, TransactionManager transactionManager) {
    this();
    Assert.notNull(userTransaction, "UserTransaction must not be null");
    Assert.notNull(transactionManager, "TransactionManager must not be null");
    this.userTransaction = userTransaction;
    this.transactionManager = transactionManager;
  }

  /**
   * Create a new JtaTransactionManager instance.
   *
   * @param transactionManager the JTA TransactionManager to use as direct reference
   */
  public JtaTransactionManager(TransactionManager transactionManager) {
    this();
    Assert.notNull(transactionManager, "TransactionManager must not be null");
    this.transactionManager = transactionManager;
    this.userTransaction = buildUserTransaction(transactionManager);
  }

  /**
   * Set the JndiTemplate to use for JNDI lookups.
   * A default one is used if not set.
   */
  public void setJndiTemplate(JndiTemplate jndiTemplate) {
    Assert.notNull(jndiTemplate, "JndiTemplate must not be null");
    this.jndiTemplate = jndiTemplate;
  }

  /**
   * Return the JndiTemplate used for JNDI lookups.
   */
  public JndiTemplate getJndiTemplate() {
    return this.jndiTemplate;
  }

  /**
   * Set the JNDI environment to use for JNDI lookups.
   * Creates a JndiTemplate with the given environment settings.
   *
   * @see #setJndiTemplate
   */
  public void setJndiEnvironment(@Nullable Properties jndiEnvironment) {
    this.jndiTemplate = new JndiTemplate(jndiEnvironment);
  }

  /**
   * Return the JNDI environment to use for JNDI lookups.
   */
  @Nullable
  public Properties getJndiEnvironment() {
    return this.jndiTemplate.getEnvironment();
  }

  /**
   * Set the JTA UserTransaction to use as direct reference.
   * <p>Typically just used for local JTA setups; in a Jakarta EE environment,
   * the UserTransaction will always be fetched from JNDI.
   *
   * @see #setUserTransactionName
   * @see #setAutodetectUserTransaction
   */
  public void setUserTransaction(@Nullable UserTransaction userTransaction) {
    this.userTransaction = userTransaction;
  }

  /**
   * Return the JTA UserTransaction that this transaction manager uses.
   */
  @Nullable
  public UserTransaction getUserTransaction() {
    return this.userTransaction;
  }

  /**
   * Set the JNDI name of the JTA UserTransaction.
   * <p>Note that the UserTransaction will be autodetected at the Jakarta EE
   * default location "java:comp/UserTransaction" if not specified explicitly.
   *
   * @see #DEFAULT_USER_TRANSACTION_NAME
   * @see #setUserTransaction
   * @see #setAutodetectUserTransaction
   */
  public void setUserTransactionName(String userTransactionName) {
    this.userTransactionName = userTransactionName;
  }

  /**
   * Set whether to autodetect the JTA UserTransaction at its default
   * JNDI location "java:comp/UserTransaction", as specified by Jakarta EE.
   * Will proceed without UserTransaction if none found.
   * <p>Default is "true", autodetecting the UserTransaction unless
   * it has been specified explicitly. Turn this flag off to allow for
   * JtaTransactionManager operating against the TransactionManager only,
   * despite a default UserTransaction being available.
   *
   * @see #DEFAULT_USER_TRANSACTION_NAME
   */
  public void setAutodetectUserTransaction(boolean autodetectUserTransaction) {
    this.autodetectUserTransaction = autodetectUserTransaction;
  }

  /**
   * Set whether to cache the JTA UserTransaction object fetched from JNDI.
   * <p>Default is "true": UserTransaction lookup will only happen at startup,
   * reusing the same UserTransaction handle for all transactions of all threads.
   * This is the most efficient choice for all application servers that provide
   * a shared UserTransaction object (the typical case).
   * <p>Turn this flag off to enforce a fresh lookup of the UserTransaction
   * for every transaction. This is only necessary for application servers
   * that return a new UserTransaction for every transaction, keeping state
   * tied to the UserTransaction object itself rather than the current thread.
   *
   * @see #setUserTransactionName
   */
  public void setCacheUserTransaction(boolean cacheUserTransaction) {
    this.cacheUserTransaction = cacheUserTransaction;
  }

  /**
   * Set the JTA TransactionManager to use as direct reference.
   * <p>A TransactionManager is necessary for suspending and resuming transactions,
   * as this not supported by the UserTransaction interface.
   * <p>Note that the TransactionManager will be autodetected if the JTA
   * UserTransaction object implements the JTA TransactionManager interface too,
   * as well as autodetected at various well-known fallback JNDI locations.
   *
   * @see #setTransactionManagerName
   * @see #setAutodetectTransactionManager
   */
  public void setTransactionManager(@Nullable TransactionManager transactionManager) {
    this.transactionManager = transactionManager;
  }

  /**
   * Return the JTA TransactionManager that this transaction manager uses, if any.
   */
  @Nullable
  public TransactionManager getTransactionManager() {
    return this.transactionManager;
  }

  /**
   * Set the JNDI name of the JTA TransactionManager.
   * <p>A TransactionManager is necessary for suspending and resuming transactions,
   * as this not supported by the UserTransaction interface.
   * <p>Note that the TransactionManager will be autodetected if the JTA
   * UserTransaction object implements the JTA TransactionManager interface too,
   * as well as autodetected at various well-known fallback JNDI locations.
   *
   * @see #setTransactionManager
   * @see #setAutodetectTransactionManager
   */
  public void setTransactionManagerName(String transactionManagerName) {
    this.transactionManagerName = transactionManagerName;
  }

  /**
   * Set whether to autodetect a JTA UserTransaction object that implements
   * the JTA TransactionManager interface too (i.e. the JNDI location for the
   * TransactionManager is "java:comp/UserTransaction", same as for the UserTransaction).
   * Also checks the fallback JNDI locations "java:comp/TransactionManager" and
   * "java:/TransactionManager". Will proceed without TransactionManager if none found.
   * <p>Default is "true", autodetecting the TransactionManager unless it has been
   * specified explicitly. Can be turned off to deliberately ignore an available
   * TransactionManager, for example when there are known issues with suspend/resume
   * and any attempt to use REQUIRES_NEW or NOT_SUPPORTED should fail fast.
   *
   * @see #FALLBACK_TRANSACTION_MANAGER_NAMES
   */
  public void setAutodetectTransactionManager(boolean autodetectTransactionManager) {
    this.autodetectTransactionManager = autodetectTransactionManager;
  }

  /**
   * Set the JTA 1.1 TransactionSynchronizationRegistry to use as direct reference.
   * <p>A TransactionSynchronizationRegistry allows for interposed registration
   * of transaction synchronizations, as an alternative to the regular registration
   * methods on the JTA TransactionManager API. Also, it is an official part of the
   * Jakarta EE platform, in contrast to the JTA TransactionManager itself.
   * <p>Note that the TransactionSynchronizationRegistry will be autodetected in JNDI and
   * also from the UserTransaction/TransactionManager object if implemented there as well.
   *
   * @see #setTransactionSynchronizationRegistryName
   * @see #setAutodetectTransactionSynchronizationRegistry
   */
  public void setTransactionSynchronizationRegistry(@Nullable TransactionSynchronizationRegistry transactionSynchronizationRegistry) {
    this.transactionSynchronizationRegistry = transactionSynchronizationRegistry;
  }

  /**
   * Return the JTA 1.1 TransactionSynchronizationRegistry that this transaction manager uses, if any.
   */
  @Nullable
  public TransactionSynchronizationRegistry getTransactionSynchronizationRegistry() {
    return this.transactionSynchronizationRegistry;
  }

  /**
   * Set the JNDI name of the JTA 1.1 TransactionSynchronizationRegistry.
   * <p>Note that the TransactionSynchronizationRegistry will be autodetected
   * at the Jakarta EE default location "java:comp/TransactionSynchronizationRegistry"
   * if not specified explicitly.
   *
   * @see #DEFAULT_TRANSACTION_SYNCHRONIZATION_REGISTRY_NAME
   */
  public void setTransactionSynchronizationRegistryName(String transactionSynchronizationRegistryName) {
    this.transactionSynchronizationRegistryName = transactionSynchronizationRegistryName;
  }

  /**
   * Set whether to autodetect a JTA 1.1 TransactionSynchronizationRegistry object
   * at its default JDNI location ("java:comp/TransactionSynchronizationRegistry")
   * if the UserTransaction has also been obtained from JNDI, and also whether
   * to fall back to checking whether the JTA UserTransaction/TransactionManager
   * object implements the JTA TransactionSynchronizationRegistry interface too.
   * <p>Default is "true", autodetecting the TransactionSynchronizationRegistry
   * unless it has been specified explicitly. Can be turned off to delegate
   * synchronization registration to the regular JTA TransactionManager API.
   */
  public void setAutodetectTransactionSynchronizationRegistry(boolean autodetectTransactionSynchronizationRegistry) {
    this.autodetectTransactionSynchronizationRegistry = autodetectTransactionSynchronizationRegistry;
  }

  /**
   * Set whether to allow custom isolation levels to be specified.
   * <p>Default is "false", throwing an exception if a non-default isolation level
   * is specified for a transaction. Turn this flag on if affected resource adapters
   * check the thread-bound transaction context and apply the specified isolation
   * levels individually (e.g. through an IsolationLevelDataSourceAdapter).
   *
   * @see cn.taketoday.jdbc.datasource.IsolationLevelDataSourceAdapter
   * @see cn.taketoday.jdbc.datasource.lookup.IsolationLevelDataSourceRouter
   */
  public void setAllowCustomIsolationLevels(boolean allowCustomIsolationLevels) {
    this.allowCustomIsolationLevels = allowCustomIsolationLevels;
  }

  /**
   * Initialize the UserTransaction as well as the TransactionManager handle.
   *
   * @see #initUserTransactionAndTransactionManager()
   */
  @Override
  public void afterPropertiesSet() throws TransactionSystemException {
    initUserTransactionAndTransactionManager();
    checkUserTransactionAndTransactionManager();
    initTransactionSynchronizationRegistry();
  }

  /**
   * Initialize the UserTransaction as well as the TransactionManager handle.
   *
   * @throws TransactionSystemException if initialization failed
   */
  protected void initUserTransactionAndTransactionManager() throws TransactionSystemException {
    if (this.userTransaction == null) {
      // Fetch JTA UserTransaction from JNDI, if necessary.
      if (StringUtils.isNotEmpty(this.userTransactionName)) {
        this.userTransaction = lookupUserTransaction(this.userTransactionName);
        this.userTransactionObtainedFromJndi = true;
      }
      else {
        this.userTransaction = retrieveUserTransaction();
        if (this.userTransaction == null && this.autodetectUserTransaction) {
          // Autodetect UserTransaction at its default JNDI location.
          this.userTransaction = findUserTransaction();
        }
      }
    }

    if (this.transactionManager == null) {
      // Fetch JTA TransactionManager from JNDI, if necessary.
      if (StringUtils.isNotEmpty(this.transactionManagerName)) {
        this.transactionManager = lookupTransactionManager(this.transactionManagerName);
      }
      else {
        this.transactionManager = retrieveTransactionManager();
        if (this.transactionManager == null && this.autodetectTransactionManager) {
          // Autodetect UserTransaction object that implements TransactionManager,
          // and check fallback JNDI locations otherwise.
          this.transactionManager = findTransactionManager(this.userTransaction);
        }
      }
    }

    // If only JTA TransactionManager specified, create UserTransaction handle for it.
    if (this.userTransaction == null && this.transactionManager != null) {
      this.userTransaction = buildUserTransaction(this.transactionManager);
    }
  }

  /**
   * Check the UserTransaction as well as the TransactionManager handle,
   * assuming standard JTA requirements.
   *
   * @throws IllegalStateException if no sufficient handles are available
   */
  protected void checkUserTransactionAndTransactionManager() throws IllegalStateException {
    // We at least need the JTA UserTransaction.
    if (this.userTransaction != null) {
      if (logger.isDebugEnabled()) {
        logger.debug("Using JTA UserTransaction: {}", this.userTransaction);
      }
    }
    else {
      throw new IllegalStateException("No JTA UserTransaction available - specify either " +
              "'userTransaction' or 'userTransactionName' or 'transactionManager' or 'transactionManagerName'");
    }

    // For transaction suspension, the JTA TransactionManager is necessary too.
    if (this.transactionManager != null) {
      if (logger.isDebugEnabled()) {
        logger.debug("Using JTA TransactionManager: {}", this.transactionManager);
      }
    }
    else {
      logger.warn("No JTA TransactionManager found: transaction suspension not available");
    }
  }

  /**
   * Initialize the JTA 1.1 TransactionSynchronizationRegistry, if available.
   * <p>To be called after {@link #initUserTransactionAndTransactionManager()},
   * since it may check the UserTransaction and TransactionManager handles.
   *
   * @throws TransactionSystemException if initialization failed
   */
  protected void initTransactionSynchronizationRegistry() {
    if (this.transactionSynchronizationRegistry == null) {
      // Fetch JTA TransactionSynchronizationRegistry from JNDI, if necessary.
      if (StringUtils.isNotEmpty(this.transactionSynchronizationRegistryName)) {
        this.transactionSynchronizationRegistry =
                lookupTransactionSynchronizationRegistry(this.transactionSynchronizationRegistryName);
      }
      else {
        this.transactionSynchronizationRegistry = retrieveTransactionSynchronizationRegistry();
        if (this.transactionSynchronizationRegistry == null && this.autodetectTransactionSynchronizationRegistry) {
          // Autodetect in JNDI if applicable, and check UserTransaction/TransactionManager
          // object that implements TransactionSynchronizationRegistry otherwise.
          this.transactionSynchronizationRegistry =
                  findTransactionSynchronizationRegistry(this.userTransaction, this.transactionManager);
        }
      }
    }

    if (this.transactionSynchronizationRegistry != null) {
      if (logger.isDebugEnabled()) {
        logger.debug("Using JTA TransactionSynchronizationRegistry: {}", transactionSynchronizationRegistry);
      }
    }
  }

  /**
   * Build a UserTransaction handle based on the given TransactionManager.
   *
   * @param transactionManager the TransactionManager
   * @return a corresponding UserTransaction handle
   */
  protected UserTransaction buildUserTransaction(TransactionManager transactionManager) {
    if (transactionManager instanceof UserTransaction) {
      return (UserTransaction) transactionManager;
    }
    else {
      return new UserTransactionAdapter(transactionManager);
    }
  }

  /**
   * Look up the JTA UserTransaction in JNDI via the configured name.
   * <p>Called by {@code afterPropertiesSet} if no direct UserTransaction reference was set.
   * Can be overridden in subclasses to provide a different UserTransaction object.
   *
   * @param userTransactionName the JNDI name of the UserTransaction
   * @return the UserTransaction object
   * @throws TransactionSystemException if the JNDI lookup failed
   * @see #setJndiTemplate
   * @see #setUserTransactionName
   */
  protected UserTransaction lookupUserTransaction(String userTransactionName)
          throws TransactionSystemException {
    try {
      if (logger.isDebugEnabled()) {
        logger.debug("Retrieving JTA UserTransaction from JNDI location [{}]", userTransactionName);
      }
      return getJndiTemplate().lookup(userTransactionName, UserTransaction.class);
    }
    catch (NamingException ex) {
      throw new TransactionSystemException(
              "JTA UserTransaction is not available at JNDI location [" + userTransactionName + "]", ex);
    }
  }

  /**
   * Look up the JTA TransactionManager in JNDI via the configured name.
   * <p>Called by {@code afterPropertiesSet} if no direct TransactionManager reference was set.
   * Can be overridden in subclasses to provide a different TransactionManager object.
   *
   * @param transactionManagerName the JNDI name of the TransactionManager
   * @return the UserTransaction object
   * @throws TransactionSystemException if the JNDI lookup failed
   * @see #setJndiTemplate
   * @see #setTransactionManagerName
   */
  protected TransactionManager lookupTransactionManager(String transactionManagerName)
          throws TransactionSystemException {
    try {
      if (logger.isDebugEnabled()) {
        logger.debug("Retrieving JTA TransactionManager from JNDI location [{}]", transactionManagerName);
      }
      return getJndiTemplate().lookup(transactionManagerName, TransactionManager.class);
    }
    catch (NamingException ex) {
      throw new TransactionSystemException(
              "JTA TransactionManager is not available at JNDI location [" + transactionManagerName + "]", ex);
    }
  }

  /**
   * Look up the JTA 1.1 TransactionSynchronizationRegistry in JNDI via the configured name.
   * <p>Can be overridden in subclasses to provide a different TransactionManager object.
   *
   * @param registryName the JNDI name of the
   * TransactionSynchronizationRegistry
   * @return the TransactionSynchronizationRegistry object
   * @throws TransactionSystemException if the JNDI lookup failed
   * @see #setJndiTemplate
   * @see #setTransactionSynchronizationRegistryName
   */
  protected TransactionSynchronizationRegistry lookupTransactionSynchronizationRegistry(String registryName) throws TransactionSystemException {
    try {
      if (logger.isDebugEnabled()) {
        logger.debug("Retrieving JTA TransactionSynchronizationRegistry from JNDI location [{}]", registryName);
      }
      return getJndiTemplate().lookup(registryName, TransactionSynchronizationRegistry.class);
    }
    catch (NamingException ex) {
      throw new TransactionSystemException(
              "JTA TransactionSynchronizationRegistry is not available at JNDI location [" + registryName + "]", ex);
    }
  }

  /**
   * Allows subclasses to retrieve the JTA UserTransaction in a vendor-specific manner.
   * Only called if no "userTransaction" or "userTransactionName" specified.
   * <p>The default implementation simply returns {@code null}.
   *
   * @return the JTA UserTransaction handle to use, or {@code null} if none found
   * @throws TransactionSystemException in case of errors
   * @see #setUserTransaction
   * @see #setUserTransactionName
   */
  @Nullable
  protected UserTransaction retrieveUserTransaction() throws TransactionSystemException {
    return null;
  }

  /**
   * Allows subclasses to retrieve the JTA TransactionManager in a vendor-specific manner.
   * Only called if no "transactionManager" or "transactionManagerName" specified.
   * <p>The default implementation simply returns {@code null}.
   *
   * @return the JTA TransactionManager handle to use, or {@code null} if none found
   * @throws TransactionSystemException in case of errors
   * @see #setTransactionManager
   * @see #setTransactionManagerName
   */
  @Nullable
  protected TransactionManager retrieveTransactionManager() throws TransactionSystemException {
    return null;
  }

  /**
   * Allows subclasses to retrieve the JTA 1.1 TransactionSynchronizationRegistry
   * in a vendor-specific manner.
   * <p>The default implementation simply returns {@code null}.
   *
   * @return the JTA TransactionSynchronizationRegistry handle to use,
   * or {@code null} if none found
   * @throws TransactionSystemException in case of errors
   */
  @Nullable
  protected TransactionSynchronizationRegistry retrieveTransactionSynchronizationRegistry() throws TransactionSystemException {
    return null;
  }

  /**
   * Find the JTA UserTransaction through a default JNDI lookup:
   * "java:comp/UserTransaction".
   *
   * @return the JTA UserTransaction reference, or {@code null} if not found
   * @see #DEFAULT_USER_TRANSACTION_NAME
   */
  @Nullable
  protected UserTransaction findUserTransaction() {
    String jndiName = DEFAULT_USER_TRANSACTION_NAME;
    try {
      UserTransaction ut = getJndiTemplate().lookup(jndiName, UserTransaction.class);
      if (logger.isDebugEnabled()) {
        logger.debug("JTA UserTransaction found at default JNDI location [{}]", jndiName);
      }
      this.userTransactionObtainedFromJndi = true;
      return ut;
    }
    catch (NamingException ex) {
      if (logger.isDebugEnabled()) {
        logger.debug("No JTA UserTransaction found at default JNDI location [{}]", jndiName, ex);
      }
      return null;
    }
  }

  /**
   * Find the JTA TransactionManager through autodetection: checking whether the
   * UserTransaction object implements the TransactionManager, and checking the
   * fallback JNDI locations.
   *
   * @param ut the JTA UserTransaction object
   * @return the JTA TransactionManager reference, or {@code null} if not found
   * @see #FALLBACK_TRANSACTION_MANAGER_NAMES
   */
  @Nullable
  protected TransactionManager findTransactionManager(@Nullable UserTransaction ut) {
    if (ut instanceof TransactionManager) {
      if (logger.isDebugEnabled()) {
        logger.debug("JTA UserTransaction object [{}] implements TransactionManager", ut);
      }
      return (TransactionManager) ut;
    }

    // Check fallback JNDI locations.
    for (String jndiName : FALLBACK_TRANSACTION_MANAGER_NAMES) {
      try {
        TransactionManager tm = getJndiTemplate().lookup(jndiName, TransactionManager.class);
        if (logger.isDebugEnabled()) {
          logger.debug("JTA TransactionManager found at fallback JNDI location [{}]", jndiName);
        }
        return tm;
      }
      catch (NamingException ex) {
        if (logger.isDebugEnabled()) {
          logger.debug("No JTA TransactionManager found at fallback JNDI location [{]]", jndiName, ex);
        }
      }
    }

    // OK, so no JTA TransactionManager is available...
    return null;
  }

  /**
   * Find the JTA 1.1 TransactionSynchronizationRegistry through autodetection:
   * checking whether the UserTransaction object or TransactionManager object
   * implements it, and checking Jakarta EE's standard JNDI location.
   * <p>The default implementation simply returns {@code null}.
   *
   * @param ut the JTA UserTransaction object
   * @param tm the JTA TransactionManager object
   * @return the JTA TransactionSynchronizationRegistry handle to use,
   * or {@code null} if none found
   * @throws TransactionSystemException in case of errors
   */
  @Nullable
  protected TransactionSynchronizationRegistry findTransactionSynchronizationRegistry(
          @Nullable UserTransaction ut, @Nullable TransactionManager tm) throws TransactionSystemException {

    if (this.userTransactionObtainedFromJndi) {
      // UserTransaction has already been obtained from JNDI, so the
      // TransactionSynchronizationRegistry probably sits there as well.
      String jndiName = DEFAULT_TRANSACTION_SYNCHRONIZATION_REGISTRY_NAME;
      try {
        TransactionSynchronizationRegistry tsr = getJndiTemplate().lookup(jndiName, TransactionSynchronizationRegistry.class);
        if (logger.isDebugEnabled()) {
          logger.debug("JTA TransactionSynchronizationRegistry found at default JNDI location [{}]", jndiName);
        }
        return tsr;
      }
      catch (NamingException ex) {
        if (logger.isDebugEnabled()) {
          logger.debug("No JTA TransactionSynchronizationRegistry found at default JNDI location [{}]",
                  jndiName, ex);
        }
      }
    }
    // Check whether the UserTransaction or TransactionManager implements it...
    if (ut instanceof TransactionSynchronizationRegistry) {
      return (TransactionSynchronizationRegistry) ut;
    }
    if (tm instanceof TransactionSynchronizationRegistry) {
      return (TransactionSynchronizationRegistry) tm;
    }
    // OK, so no JTA 1.1 TransactionSynchronizationRegistry is available...
    return null;
  }

  /**
   * This implementation returns a JtaTransactionObject instance for the
   * JTA UserTransaction.
   * <p>The UserTransaction object will either be looked up freshly for the
   * current transaction, or the cached one looked up at startup will be used.
   * The latter is the default: Most application servers use a shared singleton
   * UserTransaction that can be cached. Turn off the "cacheUserTransaction"
   * flag to enforce a fresh lookup for every transaction.
   *
   * @see #setCacheUserTransaction
   */
  @Override
  protected Object doGetTransaction() {
    UserTransaction ut = getUserTransaction();
    if (ut == null) {
      throw new CannotCreateTransactionException("No JTA UserTransaction available - " +
              "programmatic PlatformTransactionManager.getTransaction usage not supported");
    }
    if (!this.cacheUserTransaction) {
      ut = lookupUserTransaction(
              this.userTransactionName != null ? this.userTransactionName : DEFAULT_USER_TRANSACTION_NAME);
    }
    return doGetJtaTransaction(ut);
  }

  /**
   * Get a JTA transaction object for the given current UserTransaction.
   * <p>Subclasses can override this to provide a JtaTransactionObject
   * subclass, for example holding some additional JTA handle needed.
   *
   * @param ut the UserTransaction handle to use for the current transaction
   * @return the JtaTransactionObject holding the UserTransaction
   */
  protected JtaTransactionObject doGetJtaTransaction(UserTransaction ut) {
    return new JtaTransactionObject(ut);
  }

  @Override
  protected boolean isExistingTransaction(Object transaction) {
    JtaTransactionObject txObject = (JtaTransactionObject) transaction;
    try {
      return (txObject.getUserTransaction().getStatus() != Status.STATUS_NO_TRANSACTION);
    }
    catch (SystemException ex) {
      throw new TransactionSystemException("JTA failure on getStatus", ex);
    }
  }

  /**
   * This implementation returns false to cause a further invocation
   * of doBegin despite an already existing transaction.
   * <p>JTA implementations might support nested transactions via further
   * {@code UserTransaction.begin()} invocations, but never support savepoints.
   *
   * @see #doBegin
   * @see UserTransaction#begin()
   */
  @Override
  protected boolean useSavepointForNestedTransaction() {
    return false;
  }

  @Override
  protected void doBegin(Object transaction, TransactionDefinition definition) {
    JtaTransactionObject txObject = (JtaTransactionObject) transaction;
    try {
      doJtaBegin(txObject, definition);
    }
    catch (NotSupportedException | UnsupportedOperationException ex) {
      throw new NestedTransactionNotSupportedException(
              "JTA implementation does not support nested transactions", ex);
    }
    catch (SystemException ex) {
      throw new CannotCreateTransactionException("JTA failure on begin", ex);
    }
  }

  /**
   * Perform a JTA begin on the JTA UserTransaction or TransactionManager.
   * <p>This implementation only supports standard JTA functionality:
   * that is, no per-transaction isolation levels and no transaction names.
   * Can be overridden in subclasses, for specific JTA implementations.
   * <p>Calls {@code applyIsolationLevel} and {@code applyTimeout}
   * before invoking the UserTransaction's {@code begin} method.
   *
   * @param txObject the JtaTransactionObject containing the UserTransaction
   * @param definition the TransactionDefinition instance, describing propagation
   * behavior, isolation level, read-only flag, timeout, and transaction name
   * @throws NotSupportedException if thrown by JTA methods
   * @throws SystemException if thrown by JTA methods
   * @see #getUserTransaction
   * @see #getTransactionManager
   * @see #applyIsolationLevel
   * @see #applyTimeout
   * @see JtaTransactionObject#getUserTransaction()
   * @see UserTransaction#setTransactionTimeout
   * @see UserTransaction#begin
   */
  protected void doJtaBegin(JtaTransactionObject txObject, TransactionDefinition definition)
          throws NotSupportedException, SystemException {

    applyIsolationLevel(txObject, definition.getIsolationLevel());
    int timeout = determineTimeout(definition);
    applyTimeout(txObject, timeout);
    txObject.getUserTransaction().begin();
  }

  /**
   * Apply the given transaction isolation level. The default implementation
   * will throw an exception for any level other than ISOLATION_DEFAULT.
   * <p>To be overridden in subclasses for specific JTA implementations,
   * as alternative to overriding the full {@link #doJtaBegin} method.
   *
   * @param txObject the JtaTransactionObject containing the UserTransaction
   * @param isolationLevel isolation level taken from transaction definition
   * @throws InvalidIsolationLevelException if the given isolation level
   * cannot be applied
   * @throws SystemException if thrown by the JTA implementation
   * @see #doJtaBegin
   * @see JtaTransactionObject#getUserTransaction()
   * @see #getTransactionManager()
   */
  protected void applyIsolationLevel(JtaTransactionObject txObject, int isolationLevel)
          throws InvalidIsolationLevelException, SystemException {

    if (!this.allowCustomIsolationLevels && isolationLevel != TransactionDefinition.ISOLATION_DEFAULT) {
      throw new InvalidIsolationLevelException(
              "JtaTransactionManager does not support custom isolation levels by default - " +
                      "switch 'allowCustomIsolationLevels' to 'true'");
    }
  }

  /**
   * Apply the given transaction timeout. The default implementation will call
   * {@code UserTransaction.setTransactionTimeout} for a non-default timeout value.
   *
   * @param txObject the JtaTransactionObject containing the UserTransaction
   * @param timeout the timeout value taken from transaction definition
   * @throws SystemException if thrown by the JTA implementation
   * @see #doJtaBegin
   * @see JtaTransactionObject#getUserTransaction()
   * @see UserTransaction#setTransactionTimeout(int)
   */
  protected void applyTimeout(JtaTransactionObject txObject, int timeout) throws SystemException {
    if (timeout > TransactionDefinition.TIMEOUT_DEFAULT) {
      txObject.getUserTransaction().setTransactionTimeout(timeout);
      if (timeout > 0) {
        txObject.resetTransactionTimeout = true;
      }
    }
  }

  @Override
  protected Object doSuspend(Object transaction) {
    JtaTransactionObject txObject = (JtaTransactionObject) transaction;
    try {
      return doJtaSuspend(txObject);
    }
    catch (SystemException ex) {
      throw new TransactionSystemException("JTA failure on suspend", ex);
    }
  }

  /**
   * Perform a JTA suspend on the JTA TransactionManager.
   * <p>Can be overridden in subclasses, for specific JTA implementations.
   *
   * @param txObject the JtaTransactionObject containing the UserTransaction
   * @return the suspended JTA Transaction object
   * @throws SystemException if thrown by JTA methods
   * @see #getTransactionManager()
   * @see TransactionManager#suspend()
   */
  protected Object doJtaSuspend(JtaTransactionObject txObject) throws SystemException {
    if (getTransactionManager() == null) {
      throw new TransactionSuspensionNotSupportedException(
              "JtaTransactionManager needs a JTA TransactionManager for suspending a transaction: " +
                      "specify the 'transactionManager' or 'transactionManagerName' property");
    }
    return getTransactionManager().suspend();
  }

  @Override
  protected void doResume(@Nullable Object transaction, Object suspendedResources) {
    JtaTransactionObject txObject = (JtaTransactionObject) transaction;
    try {
      doJtaResume(txObject, suspendedResources);
    }
    catch (InvalidTransactionException ex) {
      throw new IllegalTransactionStateException("Tried to resume invalid JTA transaction", ex);
    }
    catch (IllegalStateException ex) {
      throw new TransactionSystemException("Unexpected internal transaction state", ex);
    }
    catch (SystemException ex) {
      throw new TransactionSystemException("JTA failure on resume", ex);
    }
  }

  /**
   * Perform a JTA resume on the JTA TransactionManager.
   * <p>Can be overridden in subclasses, for specific JTA implementations.
   *
   * @param txObject the JtaTransactionObject containing the UserTransaction
   * @param suspendedTransaction the suspended JTA Transaction object
   * @throws InvalidTransactionException if thrown by JTA methods
   * @throws SystemException if thrown by JTA methods
   * @see #getTransactionManager()
   * @see TransactionManager#resume(Transaction)
   */
  protected void doJtaResume(@Nullable JtaTransactionObject txObject, Object suspendedTransaction)
          throws InvalidTransactionException, SystemException {

    if (getTransactionManager() == null) {
      throw new TransactionSuspensionNotSupportedException(
              "JtaTransactionManager needs a JTA TransactionManager for suspending a transaction: " +
                      "specify the 'transactionManager' or 'transactionManagerName' property");
    }
    getTransactionManager().resume((Transaction) suspendedTransaction);
  }

  /**
   * This implementation returns "true": a JTA commit will properly handle
   * transactions that have been marked rollback-only at a global level.
   */
  @Override
  protected boolean shouldCommitOnGlobalRollbackOnly() {
    return true;
  }

  @Override
  protected void doCommit(DefaultTransactionStatus status) {
    JtaTransactionObject txObject = (JtaTransactionObject) status.getTransaction();
    try {
      int jtaStatus = txObject.getUserTransaction().getStatus();
      if (jtaStatus == Status.STATUS_NO_TRANSACTION) {
        // Should never happen... would have thrown an exception before
        // and as a consequence led to a rollback, not to a commit call.
        // In any case, the transaction is already fully cleaned up.
        throw new UnexpectedRollbackException("JTA transaction already completed - probably rolled back");
      }
      if (jtaStatus == Status.STATUS_ROLLEDBACK) {
        // Only really happens on JBoss 4.2 in case of an early timeout...
        // Explicit rollback call necessary to clean up the transaction.
        // IllegalStateException expected on JBoss; call still necessary.
        try {
          txObject.getUserTransaction().rollback();
        }
        catch (IllegalStateException ex) {
          if (logger.isDebugEnabled()) {
            logger.debug("Rollback failure with transaction already marked as rolled back: {}", ex.toString());
          }
        }
        throw new UnexpectedRollbackException("JTA transaction already rolled back (probably due to a timeout)");
      }
      txObject.getUserTransaction().commit();
    }
    catch (RollbackException ex) {
      throw new UnexpectedRollbackException(
              "JTA transaction unexpectedly rolled back (maybe due to a timeout)", ex);
    }
    catch (HeuristicMixedException ex) {
      throw new HeuristicCompletionException(HeuristicCompletionException.STATE_MIXED, ex);
    }
    catch (HeuristicRollbackException ex) {
      throw new HeuristicCompletionException(HeuristicCompletionException.STATE_ROLLED_BACK, ex);
    }
    catch (IllegalStateException ex) {
      throw new TransactionSystemException("Unexpected internal transaction state", ex);
    }
    catch (SystemException ex) {
      throw new TransactionSystemException("JTA failure on commit", ex);
    }
  }

  @Override
  protected void doRollback(DefaultTransactionStatus status) {
    JtaTransactionObject txObject = (JtaTransactionObject) status.getTransaction();
    try {
      int jtaStatus = txObject.getUserTransaction().getStatus();
      if (jtaStatus != Status.STATUS_NO_TRANSACTION) {
        try {
          txObject.getUserTransaction().rollback();
        }
        catch (IllegalStateException ex) {
          if (jtaStatus == Status.STATUS_ROLLEDBACK) {
            // Only really happens on JBoss 4.2 in case of an early timeout...
            if (logger.isDebugEnabled()) {
              logger.debug("Rollback failure with transaction already marked as rolled back: {}", ex.toString());
            }
          }
          else {
            throw new TransactionSystemException("Unexpected internal transaction state", ex);
          }
        }
      }
    }
    catch (SystemException ex) {
      throw new TransactionSystemException("JTA failure on rollback", ex);
    }
  }

  @Override
  protected void doSetRollbackOnly(DefaultTransactionStatus status) {
    JtaTransactionObject txObject = (JtaTransactionObject) status.getTransaction();
    if (status.isDebug()) {
      logger.debug("Setting JTA transaction rollback-only");
    }
    try {
      int jtaStatus = txObject.getUserTransaction().getStatus();
      if (jtaStatus != Status.STATUS_NO_TRANSACTION && jtaStatus != Status.STATUS_ROLLEDBACK) {
        txObject.getUserTransaction().setRollbackOnly();
      }
    }
    catch (IllegalStateException ex) {
      throw new TransactionSystemException("Unexpected internal transaction state", ex);
    }
    catch (SystemException ex) {
      throw new TransactionSystemException("JTA failure on setRollbackOnly", ex);
    }
  }

  @Override
  protected void registerAfterCompletionWithExistingTransaction(
          Object transaction, List<TransactionSynchronization> synchronizations) {

    JtaTransactionObject txObject = (JtaTransactionObject) transaction;
    logger.debug("Registering after-completion synchronization with existing JTA transaction");
    try {
      doRegisterAfterCompletionWithJtaTransaction(txObject, synchronizations);
    }
    catch (SystemException ex) {
      throw new TransactionSystemException("JTA failure on registerSynchronization", ex);
    }
    catch (Exception ex) {
      // Note: JBoss throws plain RuntimeException with RollbackException as cause.
      if (ex instanceof RollbackException || ex.getCause() instanceof RollbackException) {
        logger.debug("Participating in existing JTA transaction that has been marked for rollback: " +
                "cannot register Framework after-completion callbacks with outer JTA transaction - " +
                "immediately performing Framework after-completion callbacks with outcome status 'rollback'. " +
                "Original exception: {}", ex.toString());
        invokeAfterCompletion(synchronizations, TransactionSynchronization.STATUS_ROLLED_BACK);
      }
      else {
        logger.debug("Participating in existing JTA transaction, but unexpected internal transaction " +
                "state encountered: cannot register Framework after-completion callbacks with outer JTA " +
                "transaction - processing Framework after-completion callbacks with outcome status 'unknown'" +
                "Original exception: {}", ex.toString());
        invokeAfterCompletion(synchronizations, TransactionSynchronization.STATUS_UNKNOWN);
      }
    }
  }

  /**
   * Register a JTA synchronization on the JTA TransactionManager, for calling
   * {@code afterCompletion} on the given Framework TransactionSynchronizations.
   * <p>The default implementation registers the synchronizations on the
   * JTA 1.1 TransactionSynchronizationRegistry, if available, or on the
   * JTA TransactionManager's current Transaction - again, if available.
   * If none of the two is available, a warning will be logged.
   * <p>Can be overridden in subclasses, for specific JTA implementations.
   *
   * @param txObject the current transaction object
   * @param synchronizations a List of TransactionSynchronization objects
   * @throws RollbackException if thrown by JTA methods
   * @throws SystemException if thrown by JTA methods
   * @see #getTransactionManager()
   * @see Transaction#registerSynchronization
   * @see TransactionSynchronizationRegistry#registerInterposedSynchronization
   */
  protected void doRegisterAfterCompletionWithJtaTransaction(
          JtaTransactionObject txObject, List<TransactionSynchronization> synchronizations)
          throws RollbackException, SystemException {

    int jtaStatus = txObject.getUserTransaction().getStatus();
    if (jtaStatus == Status.STATUS_NO_TRANSACTION) {
      throw new RollbackException("JTA transaction already completed - probably rolled back");
    }
    if (jtaStatus == Status.STATUS_ROLLEDBACK) {
      throw new RollbackException("JTA transaction already rolled back (probably due to a timeout)");
    }

    if (this.transactionSynchronizationRegistry != null) {
      // JTA 1.1 TransactionSynchronizationRegistry available - use it.
      this.transactionSynchronizationRegistry.registerInterposedSynchronization(
              new JtaAfterCompletionSynchronization(synchronizations));
    }

    else if (getTransactionManager() != null) {
      // At least the JTA TransactionManager available - use that one.
      Transaction transaction = getTransactionManager().getTransaction();
      if (transaction == null) {
        throw new IllegalStateException("No JTA Transaction available");
      }
      transaction.registerSynchronization(new JtaAfterCompletionSynchronization(synchronizations));
    }

    else {
      // No JTA TransactionManager available - log a warning.
      logger.warn("Participating in existing JTA transaction, but no JTA TransactionManager available: " +
              "cannot register Framework after-completion callbacks with outer JTA transaction - " +
              "processing Framework after-completion callbacks with outcome status 'unknown'");
      invokeAfterCompletion(synchronizations, TransactionSynchronization.STATUS_UNKNOWN);
    }
  }

  @Override
  protected void doCleanupAfterCompletion(Object transaction) {
    JtaTransactionObject txObject = (JtaTransactionObject) transaction;
    if (txObject.resetTransactionTimeout) {
      try {
        txObject.getUserTransaction().setTransactionTimeout(0);
      }
      catch (SystemException ex) {
        logger.debug("Failed to reset transaction timeout after JTA completion", ex);
      }
    }
  }

  //---------------------------------------------------------------------
  // Implementation of TransactionFactory interface
  //---------------------------------------------------------------------

  @Override
  public Transaction createTransaction(@Nullable String name, int timeout) throws NotSupportedException, SystemException {
    TransactionManager tm = getTransactionManager();
    Assert.state(tm != null, "No JTA TransactionManager available");
    if (timeout >= 0) {
      tm.setTransactionTimeout(timeout);
    }
    tm.begin();
    return new ManagedTransactionAdapter(tm);
  }

  @Override
  public boolean supportsResourceAdapterManagedTransactions() {
    return false;
  }

  //---------------------------------------------------------------------
  // Serialization support
  //---------------------------------------------------------------------

  @Serial
  private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
    // Rely on default serialization; just initialize state after deserialization.
    ois.defaultReadObject();

    // Create template for client-side JNDI lookup.
    this.jndiTemplate = new JndiTemplate();

    // Perform a fresh lookup for JTA handles.
    initUserTransactionAndTransactionManager();
    initTransactionSynchronizationRegistry();
  }

}
