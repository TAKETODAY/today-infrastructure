/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
package cn.taketoday.orm.mybatis;

import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import cn.taketoday.logger.Logger;
import cn.taketoday.logger.LoggerFactory;
import cn.taketoday.transaction.SynchronizationManager;
import cn.taketoday.transaction.SynchronizationManager.SynchronizationMetaData;
import cn.taketoday.transaction.TransactionSynchronization;

/**
 * @author TODAY <br>
 * 2018-11-06 21:36
 */
public abstract class SqlSessionUtils {

  private static final Logger log = LoggerFactory.getLogger(SqlSessionUtils.class);

  public static boolean debugEnabled = log.isDebugEnabled();

  /**
   * Creates a new MyBatis {@code SqlSession} from the {@code SqlSessionFactory}
   * provided as a parameter and using its {@code DataSource} and
   * {@code ExecutorType}
   *
   * @param sessionFactory
   *         a MyBatis {@code SqlSessionFactory} to create new sessions
   *
   * @return a MyBatis {@code SqlSession}
   */
  public static SqlSession getSqlSession(SqlSessionFactory sessionFactory) {
    return getSqlSession(sessionFactory, sessionFactory.getConfiguration().getDefaultExecutorType());
  }

  public static SqlSession getSqlSession(SqlSessionFactory sessionFactory, ExecutorType executorType) {
    return getSqlSession(SynchronizationManager.getMetaData(), sessionFactory, executorType);
  }

  /**
   * Gets an SqlSession from Today Transaction Manager or creates a new one if
   * needed. Tries to get a SqlSession out of current transaction. If there is not
   * any, it creates a new one.
   *
   * @param sessionFactory
   *         A MyBatis {@code SqlSessionFactory} to create new session
   * @param executorType
   *         The executor type of the SqlSession to create
   */
  public static SqlSession getSqlSession(
          final SynchronizationMetaData metaData,
          final SqlSessionFactory sessionFactory, final ExecutorType executorType) {

    SqlSession session = sessionHolder(executorType, (SqlSessionHolder) metaData.getResource(sessionFactory));
    if (session == null) {
      if (debugEnabled) {
        log.debug("Creating a new SqlSession");
      }
      session = sessionFactory.openSession(executorType);
      registerSessionHolder(metaData, sessionFactory, executorType, session);
    }
    return session;
  }

  /**
   * Register session holder if synchronization is active (i.e. a Today TX is
   * active).
   *
   * Note: The DataSource used by the Environment should be synchronized with the
   * transaction either through DataSourceTxMgr or another tx synchronization.
   * Further assume that if an exception is thrown, whatever started the
   * transaction will handle closing / rolling back the Connection associated with
   * the SqlSession.
   *
   * @param sessionFactory
   *         sqlSessionFactory used for registration.
   * @param executorType
   *         executorType used for registration.
   * @param session
   *         sqlSession used for registration.
   */
  private static void registerSessionHolder(
          final SynchronizationMetaData metaData,
          final SqlSessionFactory sessionFactory,
          final ExecutorType executorType, final SqlSession session) //
  {
    if (metaData.isActive()) {
      if (debugEnabled) {
        log.debug("Registering transaction synchronization for SqlSession: [{}]", session);
      }
      final SqlSessionHolder holder = new SqlSessionHolder(session, executorType);

      metaData.bindResource(sessionFactory, holder);
      metaData.registerSynchronization(new SqlSessionSynchronization(holder, sessionFactory));

      holder.setSynchronizedWithTransaction(true);
      holder.requested();
    }
    else if (debugEnabled) {
      log.debug("SqlSession [{}] was not registered cause synchronization is not active", session);
    }
  }

  public static SqlSession sessionHolder(ExecutorType executorType, SqlSessionHolder holder) {

    SqlSession session = null;
    if (holder != null && holder.isSynchronizedWithTransaction()) {
      if (holder.getExecutorType() != executorType) {
        throw new PersistenceException("Cannot change the ExecutorType when there is an existing transaction");
      }

      holder.requested();
      if (debugEnabled) {
        log.debug("Fetched SqlSession [{}] from current transaction", holder.getSqlSession());
      }
      session = holder.getSqlSession();
    }
    return session;
  }

  /**
   * Checks if {@code SqlSession} passed as an argument is managed by Today
   * {@code SynchronizationManager} If it is not, it closes it, otherwise it just
   * updates the reference counter and lets Today call the close callback when the
   * managed transaction ends
   */
  public static void closeSqlSession(final SqlSession session, final SqlSessionFactory sessionFactory) {
    closeSqlSession(SynchronizationManager.getMetaData(), session, sessionFactory);
  }

  /**
   * Checks if {@code SqlSession} passed as an argument is managed by Today
   * {@code SynchronizationManager} If it is not, it closes it, otherwise it just
   * updates the reference counter and lets Today call the close callback when the
   * managed transaction ends
   */
  public static void closeSqlSession(
          final SynchronizationMetaData metaData,
          final SqlSession session, final SqlSessionFactory sessionFactory) {

    final Object resource = metaData.getResource(sessionFactory);
    if (resource instanceof SqlSessionHolder) {
      final SqlSessionHolder holder = (SqlSessionHolder) resource;
      if (holder.getSqlSession() == session) {
        if (debugEnabled) {
          log.debug("Releasing transactional SqlSession [{}]", session);
        }
        holder.released();
      }
    }
    else {
      if (debugEnabled) {
        log.debug("Closing non transactional SqlSession [{}]", session);
      }
      session.close();
    }
  }

  public static boolean isSqlSessionTransactional(
          final SqlSession session, final SqlSessionFactory sessionFactory) {
    return isSqlSessionTransactional(SynchronizationManager.getMetaData(), session, sessionFactory);
  }

  /**
   * Returns if the {@code SqlSession} passed as an argument is being managed by
   * Today
   *
   * @param session
   *         a MyBatis SqlSession to check
   * @param sessionFactory
   *         the SqlSessionFactory which the SqlSession was built with
   *
   * @return true if session is transactional, otherwise false
   */
  public static boolean isSqlSessionTransactional(
          final SynchronizationMetaData metaData,
          final SqlSession session, final SqlSessionFactory sessionFactory) {
    SqlSessionHolder holder = (SqlSessionHolder) metaData.getResource(sessionFactory);
    return (holder != null) && (holder.getSqlSession() == session);
  }

  /**
   * Callback for cleaning up resources. It cleans SynchronizationManager and also
   * commits and closes the {@code SqlSession}. It assumes that {@code Connection}
   * life cycle will be managed by {@code DataSourceTransactionManager} or
   * {@code JtaTransactionManager}
   */
  private static final class SqlSessionSynchronization implements TransactionSynchronization {

    private final SqlSessionHolder holder;

    private final SqlSessionFactory sessionFactory;

    private boolean holderActive = true;

    public SqlSessionSynchronization(SqlSessionHolder holder, SqlSessionFactory sessionFactory) {
      this.holder = holder;
      this.sessionFactory = sessionFactory;
    }

    @Override
    public void suspend(final SynchronizationMetaData metaData) {
      if (this.holderActive) {
        if (debugEnabled) {
          log.debug("Transaction synchronization suspending SqlSession [{}]", this.holder.getSqlSession());
        }
        metaData.unbindResource(this.sessionFactory);
      }
    }

    @Override
    public void resume(final SynchronizationMetaData metaData) {
      if (this.holderActive) {
        if (debugEnabled) {
          log.debug("Transaction synchronization resuming SqlSession [{}]", this.holder.getSqlSession());
        }
        metaData.bindResource(this.sessionFactory, this.holder);
      }
    }

    @Override
    public void beforeCommit(final SynchronizationMetaData metaData, boolean readOnly) {
      // Connection commit or rollback will be handled by ConnectionSynchronization or
      // DataSourceTransactionManager.
      // But, do cleanup the SqlSession / Executor, including flushing BATCH
      // statements so
      // they are actually executed.
      // TodayManagedTransaction will no-op the commit over the jdbc connection
      // TODO This updates 2nd level caches but the tx may be rolledback later on!
      if (metaData.isActualActive()) {
        if (debugEnabled) {
          log.debug("Transaction synchronization committing SqlSession [{}]", this.holder.getSqlSession());
        }
        this.holder.getSqlSession().commit();
      }
    }

    @Override
    public void beforeCompletion(final SynchronizationMetaData metaData) {
      // Issue #18 Close SqlSession and deregister it now
      // because afterCompletion may be called from a different thread
      if (!this.holder.isOpen()) {
        if (debugEnabled) {
          log.debug("Transaction synchronization deregistering SqlSession [{}]", this.holder.getSqlSession());
        }
        metaData.unbindResource(sessionFactory);
        this.holderActive = false;
      }
    }

    @Override
    public void afterCompletion(final SynchronizationMetaData metaData, int status) {
      if (this.holderActive) {
        // afterCompletion may have been called from a different thread
        // so avoid failing if there is nothing in this one
        if (debugEnabled) {
          log.debug("Transaction synchronization deregistering SqlSession [{}]", this.holder.getSqlSession());
        }
        metaData.unbindResourceIfPossible(sessionFactory);
        this.holderActive = false;
      }
      this.holder.reset();
    }
  }

}
