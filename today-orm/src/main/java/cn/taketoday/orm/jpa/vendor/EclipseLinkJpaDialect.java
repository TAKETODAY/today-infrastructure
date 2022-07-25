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

package cn.taketoday.orm.jpa.vendor;

import org.eclipse.persistence.sessions.UnitOfWork;

import java.sql.Connection;
import java.sql.SQLException;

import cn.taketoday.jdbc.datasource.ConnectionHandle;
import cn.taketoday.lang.Nullable;
import cn.taketoday.orm.jpa.DefaultJpaDialect;
import cn.taketoday.transaction.TransactionDefinition;
import cn.taketoday.transaction.TransactionException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;

/**
 * {@link cn.taketoday.orm.jpa.JpaDialect} implementation for Eclipse
 * Persistence Services (EclipseLink). Developed and tested against EclipseLink 2.7;
 * backwards-compatible with EclipseLink 2.5 and 2.6 at runtime.
 *
 * <p>By default, this class acquires an early EclipseLink transaction with an early
 * JDBC Connection for non-read-only transactions. This allows for mixing JDBC and
 * JPA/EclipseLink operations in the same transaction, with cross visibility of
 * their impact. If this is not needed, set the "lazyDatabaseTransaction" flag to
 * {@code true} or consistently declare all affected transactions as read-only.
 * this will reliably avoid early JDBC Connection retrieval
 * and therefore keep EclipseLink in shared cache mode.
 *
 * @author Juergen Hoeller
 * @see #setLazyDatabaseTransaction
 * @see cn.taketoday.jdbc.datasource.LazyConnectionDataSourceProxy
 * @since 4.0
 */
@SuppressWarnings("serial")
public class EclipseLinkJpaDialect extends DefaultJpaDialect {

  private boolean lazyDatabaseTransaction = false;

  /**
   * Set whether to lazily start a database resource transaction within a
   * Framework-managed EclipseLink transaction.
   * <p>By default, read-only transactions are started lazily but regular
   * non-read-only transactions are started early. This allows for reusing the
   * same JDBC Connection throughout an entire EclipseLink transaction, for
   * enforced isolation and consistent visibility with JDBC access code working
   * on the same DataSource.
   * <p>Switch this flag to "true" to enforce a lazy database transaction begin
   * even for non-read-only transactions, allowing access to EclipseLink's
   * shared cache and following EclipseLink's connection mode configuration,
   * assuming that isolation and visibility at the JDBC level are less important.
   *
   * @see org.eclipse.persistence.sessions.UnitOfWork#beginEarlyTransaction()
   */
  public void setLazyDatabaseTransaction(boolean lazyDatabaseTransaction) {
    this.lazyDatabaseTransaction = lazyDatabaseTransaction;
  }

  @Override
  @Nullable
  public Object beginTransaction(EntityManager entityManager, TransactionDefinition definition)
          throws PersistenceException, SQLException, TransactionException {

    if (definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT) {
      // Pass custom isolation level on to EclipseLink's DatabaseLogin configuration
      UnitOfWork uow = entityManager.unwrap(UnitOfWork.class);
      uow.getLogin().setTransactionIsolation(definition.getIsolationLevel());
    }

    entityManager.getTransaction().begin();

    if (!definition.isReadOnly() && !this.lazyDatabaseTransaction) {
      // Begin an early transaction to force EclipseLink to get a JDBC Connection
      // so that Framework can manage transactions with JDBC as well as EclipseLink.
      entityManager.unwrap(UnitOfWork.class).beginEarlyTransaction();
    }

    return null;
  }

  @Override
  public ConnectionHandle getJdbcConnection(EntityManager entityManager, boolean readOnly)
          throws PersistenceException, SQLException {

    //  we're using a custom ConnectionHandle for lazy retrieval
    // of the underlying Connection (allowing for deferred internal transaction begin
    // within the EclipseLink EntityManager)
    return new EclipseLinkConnectionHandle(entityManager);
  }

  /**
   * {@link ConnectionHandle} implementation that lazily fetches an
   * EclipseLink-provided Connection on the first {@code getConnection} call -
   * which may never come if no application code requests a JDBC Connection.
   * This is useful to defer the early transaction begin that obtaining a
   * JDBC Connection implies within an EclipseLink EntityManager.
   */
  private static class EclipseLinkConnectionHandle implements ConnectionHandle {

    private final EntityManager entityManager;

    @Nullable
    private Connection connection;

    public EclipseLinkConnectionHandle(EntityManager entityManager) {
      this.entityManager = entityManager;
    }

    @Override
    public Connection getConnection() {
      if (this.connection == null) {
        this.connection = this.entityManager.unwrap(Connection.class);
      }
      return this.connection;
    }
  }

}
