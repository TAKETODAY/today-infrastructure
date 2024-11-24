/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.orm.mybatis;

import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import infra.dao.DataAccessException;
import infra.dao.TransientDataAccessResourceException;
import infra.dao.support.PersistenceExceptionTranslator;
import infra.jdbc.datasource.DataSourceUtils;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.orm.mybatis.transaction.ManagedTransactionFactory;
import infra.transaction.support.SynchronizationInfo;
import infra.transaction.support.TransactionSynchronization;
import infra.transaction.support.TransactionSynchronizationManager;

/**
 * Handles MyBatis SqlSession life cycle. It can register and get SqlSessions from
 * {@code TransactionSynchronizationManager}. Also works if no transaction is active.
 *
 * @author Hunter Presnall
 * @author Eduardo Macarron
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class SqlSessionUtils {
  private static final Logger log = LoggerFactory.getLogger(SqlSessionUtils.class);

  private static final String NO_SQL_SESSION_FACTORY_SPECIFIED = "No SqlSessionFactory specified";
  private static final String NO_SQL_SESSION_SPECIFIED = "No SqlSession specified";

  /**
   * Creates a new MyBatis {@code SqlSession} from the {@code SqlSessionFactory} provided as a parameter and using its
   * {@code DataSource} and {@code ExecutorType}
   *
   * @param sessionFactory a MyBatis {@code SqlSessionFactory} to create new sessions
   * @return a MyBatis {@code SqlSession}
   * @throws TransientDataAccessResourceException if a transaction is active and the {@code SqlSessionFactory} is not using a
   * {@code ManagedTransactionFactory}
   */
  public static SqlSession getSqlSession(SqlSessionFactory sessionFactory) {
    ExecutorType executorType = sessionFactory.getConfiguration().getDefaultExecutorType();
    return getSqlSession(sessionFactory, executorType, null);
  }

  /**
   * Gets an SqlSession from Transaction Manager or creates a new one if needed. Tries to get a SqlSession out of
   * current transaction. If there is not any, it creates a new one. Then, it synchronizes the SqlSession with the
   * transaction if Framework TX is active and <code>ManagedTransactionFactory</code> is configured as a transaction
   * manager.
   *
   * @param sessionFactory a MyBatis {@code SqlSessionFactory} to create new sessions
   * @param executorType The executor type of the SqlSession to create
   * @param exceptionTranslator Optional. Translates SqlSession.commit() exceptions to Framework exceptions.
   * @return an SqlSession managed by Transaction Manager
   * @throws TransientDataAccessResourceException if a transaction is active and the {@code SqlSessionFactory} is not using a
   * {@code ManagedTransactionFactory}
   * @see ManagedTransactionFactory
   */
  public static SqlSession getSqlSession(SqlSessionFactory sessionFactory,
          ExecutorType executorType, @Nullable PersistenceExceptionTranslator exceptionTranslator) {
    Assert.notNull(executorType, "No ExecutorType specified");
    Assert.notNull(sessionFactory, NO_SQL_SESSION_FACTORY_SPECIFIED);
    final boolean debugEnabled = log.isDebugEnabled();

    SynchronizationInfo info = TransactionSynchronizationManager.getSynchronizationInfo();
    SqlSessionHolder holder = info.getResource(sessionFactory);
    if (holder != null && holder.isSynchronizedWithTransaction()) {
      if (holder.getExecutorType() != executorType) {
        throw new TransientDataAccessResourceException(
                "Cannot change the ExecutorType when there is an existing transaction");
      }
      holder.requested();
      if (debugEnabled) {
        log.debug("Fetched SqlSession [{}] from current transaction", holder.getSqlSession());
      }
      return holder.getSqlSession();
    }

    if (debugEnabled) {
      log.debug("Creating a new SqlSession");
    }

    SqlSession session = sessionFactory.openSession(executorType);

    /* Register session holder if synchronization is active (i.e. an Infra TX is active).
     * Note: The DataSource used by the Environment should be synchronized with the
     * transaction either through DataSourceTxMgr or another tx synchronization.
     * Further, assume that if an exception is thrown, whatever started the transaction
     * will handle closing / rolling back the Connection associated with the SqlSession.
     */

    if (info.isSynchronizationActive()) {
      Environment environment = sessionFactory.getConfiguration().getEnvironment();
      if (environment.getTransactionFactory() instanceof ManagedTransactionFactory) {
        if (debugEnabled) {
          log.debug("Registering transaction synchronization for SqlSession [{}]", session);
        }
        holder = new SqlSessionHolder(session, executorType, exceptionTranslator);
        info.bindResource(sessionFactory, holder);
        info.registerSynchronization(new SqlSessionSynchronization(holder, sessionFactory));
        holder.setSynchronizedWithTransaction(true);
        holder.requested();
      }
      else {
        if (info.getResource(environment.getDataSource()) == null) {
          if (debugEnabled) {
            log.debug("SqlSession [{}] was not registered for synchronization because DataSource is not transactional", session);
          }
        }
        else {
          throw new TransientDataAccessResourceException(
                  "SqlSessionFactory must be using a ManagedTransactionFactory in order to use transaction synchronization");
        }
      }
    }
    else if (debugEnabled) {
      log.debug("SqlSession [{}] was not registered for synchronization because synchronization is not active", session);
    }

    return session;
  }

  /**
   * Checks if {@code SqlSession} passed as an argument is managed by {@code TransactionSynchronizationManager}
   * If it is not, it closes it, otherwise it just updates the reference counter and lets call the close callback
   * when the managed transaction ends
   *
   * @param session a target SqlSession
   * @param sessionFactory a factory of SqlSession
   */
  public static void closeSqlSession(SqlSession session, SqlSessionFactory sessionFactory) {
    Assert.notNull(session, NO_SQL_SESSION_SPECIFIED);
    Assert.notNull(sessionFactory, NO_SQL_SESSION_FACTORY_SPECIFIED);

    SqlSessionHolder holder = TransactionSynchronizationManager.getResource(sessionFactory);
    if ((holder != null) && (holder.getSqlSession() == session)) {
      if (log.isDebugEnabled()) {
        log.debug("Releasing transactional SqlSession [{}]", session);
      }
      holder.released();
    }
    else {
      if (log.isDebugEnabled()) {
        log.debug("Closing non transactional SqlSession [{}]", session);
      }
      session.close();
    }
  }

  /**
   * Returns if the {@code SqlSession} passed as an argument is being managed by
   *
   * @param session a MyBatis SqlSession to check
   * @param sessionFactory the SqlSessionFactory which the SqlSession was built with
   * @return true if session is transactional, otherwise false
   */
  public static boolean isSqlSessionTransactional(SqlSession session, SqlSessionFactory sessionFactory) {
    Assert.notNull(session, NO_SQL_SESSION_SPECIFIED);
    Assert.notNull(sessionFactory, NO_SQL_SESSION_FACTORY_SPECIFIED);

    SqlSessionHolder holder = TransactionSynchronizationManager.getResource(sessionFactory);
    return holder != null && holder.getSqlSession() == session;
  }

  /**
   * Callback for cleaning up resources. It cleans TransactionSynchronizationManager and also commits and closes the
   * {@code SqlSession}. It assumes that {@code Connection} life cycle will be managed by
   * {@code DataSourceTransactionManager} or {@code JtaTransactionManager}
   */
  private static final class SqlSessionSynchronization implements TransactionSynchronization {
    private boolean holderActive = true;
    private final SqlSessionHolder holder;
    private final SqlSessionFactory sessionFactory;

    public SqlSessionSynchronization(SqlSessionHolder holder, SqlSessionFactory sessionFactory) {
      Assert.notNull(holder, "Parameter 'holder' must be not null");
      Assert.notNull(sessionFactory, "Parameter 'sessionFactory' must be not null");

      this.holder = holder;
      this.sessionFactory = sessionFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getOrder() {
      // order right before any Connection synchronization
      return DataSourceUtils.CONNECTION_SYNCHRONIZATION_ORDER - 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void suspend() {
      if (this.holderActive) {
        if (log.isDebugEnabled()) {
          log.debug("Transaction synchronization suspending SqlSession [{}]", holder.getSqlSession());
        }
        TransactionSynchronizationManager.unbindResource(this.sessionFactory);
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resume() {
      if (this.holderActive) {
        if (log.isDebugEnabled()) {
          log.debug("Transaction synchronization resuming SqlSession [{}]", holder.getSqlSession());
        }
        TransactionSynchronizationManager.bindResource(this.sessionFactory, this.holder);
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeCommit(boolean readOnly) {
      // Connection commit or rollback will be handled by ConnectionSynchronization or
      // DataSourceTransactionManager.
      // But, do cleanup the SqlSession / Executor, including flushing BATCH statements so
      // they are actually executed.
      // ManagedTransaction will no-op the commit over the jdbc connection
      // TODO This updates 2nd level caches but the tx may be rolledback later on!
      if (TransactionSynchronizationManager.isActualTransactionActive()) {
        try {
          if (log.isDebugEnabled()) {
            log.debug("Transaction synchronization committing SqlSession [{}]", holder.getSqlSession());
          }
          this.holder.getSqlSession().commit();
        }
        catch (PersistenceException p) {
          if (this.holder.getPersistenceExceptionTranslator() != null) {
            DataAccessException translated = this.holder.getPersistenceExceptionTranslator()
                    .translateExceptionIfPossible(p);
            if (translated != null) {
              throw translated;
            }
          }
          throw p;
        }
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeCompletion() {
      // Issue #18 Close SqlSession and deregister it now
      // because afterCompletion may be called from a different thread
      if (!this.holder.isOpen()) {
        if (log.isDebugEnabled()) {
          log.debug("Transaction synchronization deregistering SqlSession [{}]", holder.getSqlSession());
          TransactionSynchronizationManager.unbindResource(sessionFactory);
          this.holderActive = false;
          log.debug("Transaction synchronization closing SqlSession [{}]", holder.getSqlSession());
        }
        else {
          TransactionSynchronizationManager.unbindResource(sessionFactory);
          this.holderActive = false;
        }
        this.holder.getSqlSession().close();
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterCompletion(int status) {
      if (this.holderActive) {
        // afterCompletion may have been called from a different thread
        // so avoid failing if there is nothing in this one
        if (log.isDebugEnabled()) {
          log.debug("Transaction synchronization deregistering SqlSession [{}]", holder.getSqlSession());
          TransactionSynchronizationManager.unbindResourceIfPossible(sessionFactory);
          this.holderActive = false;
          log.debug("Transaction synchronization closing SqlSession [{}]", holder.getSqlSession());
        }
        else {
          TransactionSynchronizationManager.unbindResourceIfPossible(sessionFactory);
          this.holderActive = false;
        }
        this.holder.getSqlSession().close();
      }
      this.holder.reset();
    }
  }

}
