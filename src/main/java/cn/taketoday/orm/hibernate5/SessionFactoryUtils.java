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

package cn.taketoday.orm.hibernate5;

import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.NonUniqueObjectException;
import org.hibernate.NonUniqueResultException;
import org.hibernate.ObjectDeletedException;
import org.hibernate.PersistentObjectException;
import org.hibernate.PessimisticLockException;
import org.hibernate.PropertyValueException;
import org.hibernate.QueryException;
import org.hibernate.QueryTimeoutException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;
import org.hibernate.TransientObjectException;
import org.hibernate.UnresolvableObjectException;
import org.hibernate.WrongClassException;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.lock.OptimisticEntityLockException;
import org.hibernate.dialect.lock.PessimisticEntityLockException;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.DataException;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.service.UnknownServiceException;

import java.lang.reflect.Method;
import java.util.Map;

import javax.sql.DataSource;

import cn.taketoday.dao.CannotAcquireLockException;
import cn.taketoday.dao.DataAccessException;
import cn.taketoday.dao.DataAccessResourceFailureException;
import cn.taketoday.dao.DataIntegrityViolationException;
import cn.taketoday.dao.DuplicateKeyException;
import cn.taketoday.dao.IncorrectResultSizeDataAccessException;
import cn.taketoday.dao.InvalidDataAccessApiUsageException;
import cn.taketoday.dao.InvalidDataAccessResourceUsageException;
import cn.taketoday.dao.PessimisticLockingFailureException;
import cn.taketoday.jdbc.datasource.DataSourceUtils;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ReflectionUtils;
import jakarta.persistence.PersistenceException;

/**
 * Helper class featuring methods for Hibernate Session handling.
 * Also provides support for exception translation.
 *
 * <p>Used internally by {@link HibernateTransactionManager}.
 * Can also be used directly in application code.
 *
 * @author Juergen Hoeller
 * @see HibernateExceptionTranslator
 * @see HibernateTransactionManager
 * @since 4.0
 */
public abstract class SessionFactoryUtils {

  /**
   * Order value for TransactionSynchronization objects that clean up Hibernate Sessions.
   * Returns {@code DataSourceUtils.CONNECTION_SYNCHRONIZATION_ORDER - 100}
   * to execute Session cleanup before JDBC Connection cleanup, if any.
   *
   * @see DataSourceUtils#CONNECTION_SYNCHRONIZATION_ORDER
   */
  public static final int SESSION_SYNCHRONIZATION_ORDER =
          DataSourceUtils.CONNECTION_SYNCHRONIZATION_ORDER - 100;

  static final Logger logger = LoggerFactory.getLogger(SessionFactoryUtils.class);

  /**
   * Trigger a flush on the given Hibernate Session, converting regular
   * {@link HibernateException} instances as well as Hibernate 5.2's
   * {@link PersistenceException} wrappers accordingly.
   *
   * @param session the Hibernate Session to flush
   * @param synch whether this flush is triggered by transaction synchronization
   * @throws DataAccessException in case of flush failures
   */
  static void flush(Session session, boolean synch) throws DataAccessException {
    if (synch) {
      logger.debug("Flushing Hibernate Session on transaction synchronization");
    }
    else {
      logger.debug("Flushing Hibernate Session on explicit request");
    }
    try {
      session.flush();
    }
    catch (HibernateException ex) {
      throw convertHibernateAccessException(ex);
    }
    catch (PersistenceException ex) {
      if (ex.getCause() instanceof HibernateException) {
        throw convertHibernateAccessException((HibernateException) ex.getCause());
      }
      throw ex;
    }

  }

  /**
   * Perform actual closing of the Hibernate Session,
   * catching and logging any cleanup exceptions thrown.
   *
   * @param session the Hibernate Session to close (may be {@code null})
   * @see Session#close()
   */
  public static void closeSession(@Nullable Session session) {
    if (session != null) {
      try {
        if (session.isOpen()) {
          session.close();
        }
      }
      catch (Throwable ex) {
        logger.error("Failed to release Hibernate Session", ex);
      }
    }
  }

  /**
   * Determine the DataSource of the given SessionFactory.
   *
   * @param sessionFactory the SessionFactory to check
   * @return the DataSource, or {@code null} if none found
   * @see ConnectionProvider
   */
  @Nullable
  public static DataSource getDataSource(SessionFactory sessionFactory) {
    Method getProperties = ReflectionUtils.getMethodIfAvailable(sessionFactory.getClass(), "getProperties");
    if (getProperties != null) {
      Map<?, ?> props = (Map<?, ?>) ReflectionUtils.invokeMethod(getProperties, sessionFactory);
      if (props != null) {
        Object dataSourceValue = props.get(Environment.DATASOURCE);
        if (dataSourceValue instanceof DataSource) {
          return (DataSource) dataSourceValue;
        }
      }
    }
    if (sessionFactory instanceof SessionFactoryImplementor sfi) {
      try {
        ConnectionProvider cp = sfi.getServiceRegistry().getService(ConnectionProvider.class);
        if (cp != null) {
          return cp.unwrap(DataSource.class);
        }
      }
      catch (UnknownServiceException ex) {
        logger.debug("No ConnectionProvider found - cannot determine DataSource for SessionFactory: {}", ex.toString());
      }
    }
    return null;
  }

  /**
   * Convert the given HibernateException to an appropriate exception
   * from the {@code cn.taketoday.dao} hierarchy.
   *
   * @param ex the HibernateException that occurred
   * @return the corresponding DataAccessException instance
   * @see HibernateExceptionTranslator#convertHibernateAccessException
   * @see HibernateTransactionManager#convertHibernateAccessException
   */
  public static DataAccessException convertHibernateAccessException(HibernateException ex) {
    if (ex instanceof JDBCConnectionException) {
      return new DataAccessResourceFailureException(ex.getMessage(), ex);
    }
    if (ex instanceof SQLGrammarException jdbcEx) {
      return new InvalidDataAccessResourceUsageException(ex.getMessage() + "; SQL [" + jdbcEx.getSQL() + "]", ex);
    }
    if (ex instanceof QueryTimeoutException jdbcEx) {
      return new cn.taketoday.dao.QueryTimeoutException(ex.getMessage() + "; SQL [" + jdbcEx.getSQL() + "]", ex);
    }
    if (ex instanceof LockAcquisitionException jdbcEx) {
      return new CannotAcquireLockException(ex.getMessage() + "; SQL [" + jdbcEx.getSQL() + "]", ex);
    }
    if (ex instanceof PessimisticLockException jdbcEx) {
      return new PessimisticLockingFailureException(ex.getMessage() + "; SQL [" + jdbcEx.getSQL() + "]", ex);
    }
    if (ex instanceof ConstraintViolationException jdbcEx) {
      return new DataIntegrityViolationException(ex.getMessage() + "; SQL [" + jdbcEx.getSQL() +
              "]; constraint [" + jdbcEx.getConstraintName() + "]", ex);
    }
    if (ex instanceof DataException jdbcEx) {
      return new DataIntegrityViolationException(ex.getMessage() + "; SQL [" + jdbcEx.getSQL() + "]", ex);
    }
    if (ex instanceof JDBCException) {
      return new HibernateJdbcException((JDBCException) ex);
    }
    // end of JDBCException (subclass) handling

    if (ex instanceof QueryException) {
      return new HibernateQueryException((QueryException) ex);
    }
    if (ex instanceof NonUniqueResultException) {
      return new IncorrectResultSizeDataAccessException(ex.getMessage(), 1, ex);
    }
    if (ex instanceof NonUniqueObjectException) {
      return new DuplicateKeyException(ex.getMessage(), ex);
    }
    if (ex instanceof PropertyValueException) {
      return new DataIntegrityViolationException(ex.getMessage(), ex);
    }
    if (ex instanceof PersistentObjectException) {
      return new InvalidDataAccessApiUsageException(ex.getMessage(), ex);
    }
    if (ex instanceof TransientObjectException) {
      return new InvalidDataAccessApiUsageException(ex.getMessage(), ex);
    }
    if (ex instanceof ObjectDeletedException) {
      return new InvalidDataAccessApiUsageException(ex.getMessage(), ex);
    }
    if (ex instanceof UnresolvableObjectException) {
      return new HibernateObjectRetrievalFailureException((UnresolvableObjectException) ex);
    }
    if (ex instanceof WrongClassException) {
      return new HibernateObjectRetrievalFailureException((WrongClassException) ex);
    }
    if (ex instanceof StaleObjectStateException) {
      return new HibernateOptimisticLockingFailureException((StaleObjectStateException) ex);
    }
    if (ex instanceof StaleStateException) {
      return new HibernateOptimisticLockingFailureException((StaleStateException) ex);
    }
    if (ex instanceof OptimisticEntityLockException) {
      return new HibernateOptimisticLockingFailureException((OptimisticEntityLockException) ex);
    }
    if (ex instanceof PessimisticEntityLockException) {
      if (ex.getCause() instanceof LockAcquisitionException) {
        return new CannotAcquireLockException(ex.getMessage(), ex.getCause());
      }
      return new PessimisticLockingFailureException(ex.getMessage(), ex);
    }

    // fallback
    return new HibernateSystemException(ex);
  }

}
