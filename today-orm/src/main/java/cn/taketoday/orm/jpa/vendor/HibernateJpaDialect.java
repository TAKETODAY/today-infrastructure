/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.orm.jpa.vendor;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.FlushMode;
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
import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;
import org.hibernate.TransientObjectException;
import org.hibernate.UnresolvableObjectException;
import org.hibernate.WrongClassException;
import org.hibernate.dialect.lock.OptimisticEntityLockException;
import org.hibernate.dialect.lock.PessimisticEntityLockException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.DataException;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.SQLGrammarException;

import java.sql.Connection;
import java.sql.SQLException;

import cn.taketoday.dao.CannotAcquireLockException;
import cn.taketoday.dao.DataAccessException;
import cn.taketoday.dao.DataAccessResourceFailureException;
import cn.taketoday.dao.DataIntegrityViolationException;
import cn.taketoday.dao.DuplicateKeyException;
import cn.taketoday.dao.IncorrectResultSizeDataAccessException;
import cn.taketoday.dao.InvalidDataAccessApiUsageException;
import cn.taketoday.dao.InvalidDataAccessResourceUsageException;
import cn.taketoday.dao.PessimisticLockingFailureException;
import cn.taketoday.jdbc.datasource.ConnectionHandle;
import cn.taketoday.jdbc.datasource.DataSourceUtils;
import cn.taketoday.jdbc.support.SQLExceptionSubclassTranslator;
import cn.taketoday.jdbc.support.SQLExceptionTranslator;
import cn.taketoday.lang.Nullable;
import cn.taketoday.orm.ObjectOptimisticLockingFailureException;
import cn.taketoday.orm.ObjectRetrievalFailureException;
import cn.taketoday.orm.jpa.DefaultJpaDialect;
import cn.taketoday.orm.jpa.EntityManagerFactoryUtils;
import cn.taketoday.orm.jpa.JpaSystemException;
import cn.taketoday.transaction.InvalidIsolationLevelException;
import cn.taketoday.transaction.TransactionDefinition;
import cn.taketoday.transaction.TransactionException;
import cn.taketoday.transaction.support.ResourceTransactionDefinition;
import cn.taketoday.util.ReflectionUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;

/**
 * {@link cn.taketoday.orm.jpa.JpaDialect} implementation for Hibernate.
 * Compatible with Hibernate ORM 5.5/5.6 as well as 6.0/6.1.
 *
 * @author Juergen Hoeller
 * @author Costin Leau
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see HibernateJpaVendorAdapter
 * @see Session#setHibernateFlushMode
 * @see org.hibernate.Transaction#setTimeout
 * @since 4.0
 */
@SuppressWarnings("serial")
public class HibernateJpaDialect extends DefaultJpaDialect {

  boolean prepareConnection = true;

  @Nullable
  private SQLExceptionTranslator jdbcExceptionTranslator;

  @Nullable
  private SQLExceptionTranslator transactionExceptionTranslator = new SQLExceptionSubclassTranslator();

  /**
   * Set whether to prepare the underlying JDBC Connection of a transactional
   * Hibernate Session, that is, whether to apply a transaction-specific
   * isolation level and/or the transaction's read-only flag to the underlying
   * JDBC Connection.
   * <p>Default is "true". If you turn this flag off, JPA transaction management
   * will not support per-transaction isolation levels anymore. It will not call
   * {@code Connection.setReadOnly(true)} for read-only transactions anymore either.
   * If this flag is turned off, no cleanup of a JDBC Connection is required after
   * a transaction, since no Connection settings will get modified.
   * <p><b>NOTE:</b> The default behavior in terms of read-only handling changed
   * in propagating the read-only status to the JDBC Connection now,
   * analogous to other Framework transaction managers. This may have the effect
   * that you're running into read-only enforcement now where previously write
   * access has accidentally been tolerated: Please revise your transaction
   * declarations accordingly, removing invalid read-only markers if necessary.
   *
   * @see java.sql.Connection#setTransactionIsolation
   * @see java.sql.Connection#setReadOnly
   */
  public void setPrepareConnection(boolean prepareConnection) {
    this.prepareConnection = prepareConnection;
  }

  /**
   * Set the JDBC exception translator for Hibernate exception translation purposes.
   * <p>Applied to any detected {@link java.sql.SQLException} root cause of a Hibernate
   * {@link JDBCException}, overriding Hibernate's own {@code SQLException} translation
   * (which is based on a Hibernate Dialect for a specific target database).
   * <p>also applied to {@link org.hibernate.TransactionException} translation
   * with a {@link SQLException} root cause (where Hibernate does not translate itself
   * at all), overriding Infra default {@link SQLExceptionSubclassTranslator} there.
   *
   * @param exceptionTranslator the {@link SQLExceptionTranslator} to delegate to, or
   * {@code null} for none. By default, a {@link SQLExceptionSubclassTranslator} will
   * be used for {@link org.hibernate.TransactionException} translation as of 4;
   * this can be reverted to pre-6.1 behavior through setting {@code null} here.
   * @see java.sql.SQLException
   * @see org.hibernate.JDBCException
   * @see cn.taketoday.jdbc.support.SQLExceptionSubclassTranslator
   * @see cn.taketoday.jdbc.support.SQLErrorCodeSQLExceptionTranslator
   */
  public void setJdbcExceptionTranslator(@Nullable SQLExceptionTranslator exceptionTranslator) {
    this.jdbcExceptionTranslator = exceptionTranslator;
    this.transactionExceptionTranslator = exceptionTranslator;
  }

  @Override
  public Object beginTransaction(EntityManager entityManager, TransactionDefinition definition)
          throws PersistenceException, SQLException, TransactionException {

    SessionImplementor session = getSession(entityManager);

    if (definition.getTimeout() != TransactionDefinition.TIMEOUT_DEFAULT) {
      session.getTransaction().setTimeout(definition.getTimeout());
    }

    boolean isolationLevelNeeded = definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT;
    Integer previousIsolationLevel = null;
    Connection preparedCon = null;

    if (isolationLevelNeeded || definition.isReadOnly()) {
      if (this.prepareConnection && ConnectionReleaseMode.ON_CLOSE.equals(
              session.getJdbcCoordinator().getLogicalConnection().getConnectionHandlingMode().getReleaseMode())) {
        preparedCon = session.getJdbcCoordinator().getLogicalConnection().getPhysicalConnection();
        previousIsolationLevel = DataSourceUtils.prepareConnectionForTransaction(preparedCon, definition);
      }
      else if (isolationLevelNeeded) {
        throw new InvalidIsolationLevelException(
                "HibernateJpaDialect is not allowed to support custom isolation levels: " +
                        "make sure that its 'prepareConnection' flag is on (the default) and that the " +
                        "Hibernate connection release mode is set to ON_CLOSE.");
      }
    }

    // Standard JPA transaction begin call for full JPA context setup...
    entityManager.getTransaction().begin();

    // Adapt flush mode and store previous isolation level, if any.
    FlushMode previousFlushMode = prepareFlushMode(session, definition.isReadOnly());
    if (definition instanceof ResourceTransactionDefinition rtd && rtd.isLocalResource()) {
      // As of 5.1, we explicitly optimize for a transaction-local EntityManager,
      // aligned with native HibernateTransactionManager behavior.
      previousFlushMode = null;
      if (definition.isReadOnly()) {
        session.setDefaultReadOnly(true);
      }
    }
    return new SessionTransactionData(
            session, previousFlushMode, preparedCon != null, previousIsolationLevel, definition.isReadOnly());
  }

  @Override
  public Object prepareTransaction(EntityManager entityManager, boolean readOnly, @Nullable String name)
          throws PersistenceException {

    SessionImplementor session = getSession(entityManager);
    FlushMode previousFlushMode = prepareFlushMode(session, readOnly);
    return new SessionTransactionData(session, previousFlushMode, false, null, readOnly);
  }

  @Nullable
  protected FlushMode prepareFlushMode(Session session, boolean readOnly) throws PersistenceException {
    FlushMode flushMode = session.getHibernateFlushMode();
    if (readOnly) {
      // We should suppress flushing for a read-only transaction.
      if (!flushMode.equals(FlushMode.MANUAL)) {
        session.setHibernateFlushMode(FlushMode.MANUAL);
        return flushMode;
      }
    }
    else {
      // We need AUTO or COMMIT for a non-read-only transaction.
      if (flushMode.lessThan(FlushMode.COMMIT)) {
        session.setHibernateFlushMode(FlushMode.AUTO);
        return flushMode;
      }
    }
    // No FlushMode change needed...
    return null;
  }

  @Override
  public void cleanupTransaction(@Nullable Object transactionData) {
    if (transactionData instanceof SessionTransactionData sessionTransactionData) {
      sessionTransactionData.resetSessionState();
    }
  }

  @Override
  public ConnectionHandle getJdbcConnection(EntityManager entityManager, boolean readOnly)
          throws PersistenceException, SQLException {

    SessionImplementor session = getSession(entityManager);
    return new HibernateConnectionHandle(session);
  }

  @Override
  @Nullable
  public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
    if (ex instanceof HibernateException hibernateEx) {
      return convertHibernateAccessException(hibernateEx);
    }
    if (ex instanceof PersistenceException && ex.getCause() instanceof HibernateException hibernateEx) {
      return convertHibernateAccessException(hibernateEx);
    }
    return EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(ex);
  }

  /**
   * Convert the given HibernateException to an appropriate exception
   * from the {@code cn.taketoday.dao} hierarchy.
   *
   * @param ex the HibernateException that occurred
   * @return the corresponding DataAccessException instance
   */
  protected DataAccessException convertHibernateAccessException(HibernateException ex) {
    if (this.jdbcExceptionTranslator != null && ex instanceof JDBCException jdbcEx) {
      DataAccessException dae = this.jdbcExceptionTranslator.translate(
              "Hibernate operation: " + jdbcEx.getMessage(), jdbcEx.getSQL(), jdbcEx.getSQLException());
      if (dae != null) {
        return dae;
      }
    }
    if (this.transactionExceptionTranslator != null && ex instanceof org.hibernate.TransactionException) {
      if (ex.getCause() instanceof SQLException sqlEx) {
        DataAccessException dae = this.transactionExceptionTranslator.translate(
                "Hibernate transaction: " + ex.getMessage(), null, sqlEx);
        if (dae != null) {
          return dae;
        }
      }
    }

    if (ex instanceof JDBCConnectionException) {
      return new DataAccessResourceFailureException(ex.getMessage(), ex);
    }
    if (ex instanceof SQLGrammarException hibEx) {
      return new InvalidDataAccessResourceUsageException(ex.getMessage() + "; SQL [" + hibEx.getSQL() + "]", ex);
    }
    if (ex instanceof QueryTimeoutException hibEx) {
      return new cn.taketoday.dao.QueryTimeoutException(ex.getMessage() + "; SQL [" + hibEx.getSQL() + "]", ex);
    }
    if (ex instanceof LockAcquisitionException hibEx) {
      return new CannotAcquireLockException(ex.getMessage() + "; SQL [" + hibEx.getSQL() + "]", ex);
    }
    if (ex instanceof PessimisticLockException hibEx) {
      return new PessimisticLockingFailureException(ex.getMessage() + "; SQL [" + hibEx.getSQL() + "]", ex);
    }
    if (ex instanceof ConstraintViolationException hibEx) {
      return new DataIntegrityViolationException(ex.getMessage() + "; SQL [" + hibEx.getSQL() +
              "]; constraint [" + hibEx.getConstraintName() + "]", ex);
    }
    if (ex instanceof DataException hibEx) {
      return new DataIntegrityViolationException(ex.getMessage() + "; SQL [" + hibEx.getSQL() + "]", ex);
    }
    // end of JDBCException subclass handling

    if (ex instanceof QueryException) {
      return new InvalidDataAccessResourceUsageException(ex.getMessage(), ex);
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
    if (ex instanceof UnresolvableObjectException hibEx) {
      return new ObjectRetrievalFailureException(hibEx.getEntityName(), getIdentifier(hibEx), ex.getMessage(), ex);
    }
    if (ex instanceof WrongClassException hibEx) {
      return new ObjectRetrievalFailureException(hibEx.getEntityName(), getIdentifier(hibEx), ex.getMessage(), ex);
    }
    if (ex instanceof StaleObjectStateException hibEx) {
      return new ObjectOptimisticLockingFailureException(hibEx.getEntityName(), getIdentifier(hibEx), ex.getMessage(), ex);
    }
    if (ex instanceof StaleStateException) {
      return new ObjectOptimisticLockingFailureException(ex.getMessage(), ex);
    }
    if (ex instanceof OptimisticEntityLockException) {
      return new ObjectOptimisticLockingFailureException(ex.getMessage(), ex);
    }
    if (ex instanceof PessimisticEntityLockException) {
      if (ex.getCause() instanceof LockAcquisitionException) {
        return new CannotAcquireLockException(ex.getMessage(), ex.getCause());
      }
      return new PessimisticLockingFailureException(ex.getMessage(), ex);
    }

    // fallback
    return new JpaSystemException(ex);
  }

  protected SessionImplementor getSession(EntityManager entityManager) {
    return entityManager.unwrap(SessionImplementor.class);
  }

  @Nullable
  protected Object getIdentifier(HibernateException hibEx) {
    try {
      return ReflectionUtils.invokeMethod(hibEx.getClass().getMethod("getIdentifier"), hibEx);
    }
    catch (NoSuchMethodException ex) {
      return null;
    }
  }

  private static class SessionTransactionData {

    private final SessionImplementor session;

    @Nullable
    private final FlushMode previousFlushMode;

    private final boolean needsConnectionReset;

    @Nullable
    private final Integer previousIsolationLevel;

    private final boolean readOnly;

    public SessionTransactionData(SessionImplementor session, @Nullable FlushMode previousFlushMode,
            boolean connectionPrepared, @Nullable Integer previousIsolationLevel, boolean readOnly) {

      this.session = session;
      this.previousFlushMode = previousFlushMode;
      this.needsConnectionReset = connectionPrepared;
      this.previousIsolationLevel = previousIsolationLevel;
      this.readOnly = readOnly;
    }

    public void resetSessionState() {
      if (this.previousFlushMode != null) {
        this.session.setHibernateFlushMode(this.previousFlushMode);
      }
      if (this.needsConnectionReset &&
              this.session.getJdbcCoordinator().getLogicalConnection().isPhysicallyConnected()) {
        Connection con = this.session.getJdbcCoordinator().getLogicalConnection().getPhysicalConnection();
        DataSourceUtils.resetConnectionAfterTransaction(
                con, this.previousIsolationLevel, this.readOnly);
      }
    }
  }

  private static class HibernateConnectionHandle implements ConnectionHandle {

    private final SessionImplementor session;

    public HibernateConnectionHandle(SessionImplementor session) {
      this.session = session;
    }

    @Override
    public Connection getConnection() {
      return this.session.getJdbcCoordinator().getLogicalConnection().getPhysicalConnection();
    }
  }

}
