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

import java.sql.SQLException;

import cn.taketoday.dao.support.PersistenceExceptionTranslator;
import cn.taketoday.jdbc.datasource.ConnectionHandle;
import cn.taketoday.lang.Nullable;
import cn.taketoday.transaction.TransactionDefinition;
import cn.taketoday.transaction.TransactionException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;

/**
 * SPI strategy that encapsulates certain functionality that standard JPA 3.0 does
 * not offer, such as access to the underlying JDBC Connection. This strategy is
 * mainly intended for standalone usage of a JPA provider; most of its functionality
 * is not relevant when running with JTA transactions.
 *
 * <p>In general, it is recommended to derive from {@link DefaultJpaDialect} instead
 * of implementing this interface directly. This allows for inheriting common behavior
 * (present and future) from DefaultJpaDialect, only overriding specific hooks to
 * plug in concrete vendor-specific behavior.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @see DefaultJpaDialect
 * @see JpaTransactionManager#setJpaDialect
 * @see JpaVendorAdapter#getJpaDialect()
 * @see AbstractEntityManagerFactoryBean#setJpaDialect
 * @see AbstractEntityManagerFactoryBean#setJpaVendorAdapter
 * @since 4.0
 */
public interface JpaDialect extends PersistenceExceptionTranslator {

  /**
   * Begin the given JPA transaction, applying the semantics specified by the
   * given Framework transaction definition (in particular, an isolation level
   * and a timeout). Called by JpaTransactionManager on transaction begin.
   * <p>An implementation can configure the JPA Transaction object and then
   * invoke {@code begin}, or invoke a special begin method that takes,
   * for example, an isolation level.
   * <p>An implementation can apply the read-only flag as flush mode. In that case,
   * a transaction data object can be returned that holds the previous flush mode
   * (and possibly other data), to be reset in {@code cleanupTransaction}.
   * It may also apply the read-only flag and isolation level to the underlying
   * JDBC Connection before beginning the transaction.
   * <p>Implementations can also use the Framework transaction name, as exposed by the
   * passed-in TransactionDefinition, to optimize for specific data access use cases
   * (effectively using the current transaction name as use case identifier).
   * <p>This method also allows for exposing savepoint capabilities if supported by
   * the persistence provider, through returning an Object that implements Framework's
   * {@link cn.taketoday.transaction.SavepointManager} interface.
   * {@link JpaTransactionManager} will use this capability if needed.
   *
   * @param entityManager the EntityManager to begin a JPA transaction on
   * @param definition the Framework transaction definition that defines semantics
   * @return an arbitrary object that holds transaction data, if any
   * (to be passed into {@link #cleanupTransaction}). May implement the
   * {@link cn.taketoday.transaction.SavepointManager} interface.
   * @throws PersistenceException if thrown by JPA methods
   * @throws SQLException if thrown by JDBC methods
   * @throws cn.taketoday.transaction.TransactionException in case of invalid arguments
   * @see #cleanupTransaction
   * @see jakarta.persistence.EntityTransaction#begin
   * @see cn.taketoday.jdbc.datasource.DataSourceUtils#prepareConnectionForTransaction
   */
  @Nullable
  Object beginTransaction(EntityManager entityManager, TransactionDefinition definition)
          throws PersistenceException, SQLException, TransactionException;

  /**
   * Prepare a JPA transaction, applying the specified semantics. Called by
   * EntityManagerFactoryUtils when enlisting an EntityManager in a JTA transaction
   * or a locally joined transaction (e.g. after upgrading an unsynchronized
   * EntityManager to a synchronized one).
   * <p>An implementation can apply the read-only flag as flush mode. In that case,
   * a transaction data object can be returned that holds the previous flush mode
   * (and possibly other data), to be reset in {@code cleanupTransaction}.
   * <p>Implementations can also use the Framework transaction name to optimize for
   * specific data access use cases (effectively using the current transaction
   * name as use case identifier).
   *
   * @param entityManager the EntityManager to begin a JPA transaction on
   * @param readOnly whether the transaction is supposed to be read-only
   * @param name the name of the transaction (if any)
   * @return an arbitrary object that holds transaction data, if any
   * (to be passed into cleanupTransaction)
   * @throws PersistenceException if thrown by JPA methods
   * @see #cleanupTransaction
   */
  @Nullable
  Object prepareTransaction(EntityManager entityManager, boolean readOnly, @Nullable String name)
          throws PersistenceException;

  /**
   * Clean up the transaction via the given transaction data. Called by
   * JpaTransactionManager and EntityManagerFactoryUtils on transaction cleanup.
   * <p>An implementation can, for example, reset read-only flag and
   * isolation level of the underlying JDBC Connection. Furthermore,
   * an exposed data access use case can be reset here.
   *
   * @param transactionData arbitrary object that holds transaction data, if any
   * (as returned by beginTransaction or prepareTransaction)
   * @see #beginTransaction
   * @see cn.taketoday.jdbc.datasource.DataSourceUtils#resetConnectionAfterTransaction
   */
  void cleanupTransaction(@Nullable Object transactionData);

  /**
   * Retrieve the JDBC Connection that the given JPA EntityManager uses underneath,
   * if accessing a relational database. This method will just get invoked if actually
   * needing access to the underlying JDBC Connection, usually within an active JPA
   * transaction (for example, by JpaTransactionManager). The returned handle will
   * be passed into the {@code releaseJdbcConnection} method when not needed anymore.
   * <p>This strategy is necessary as JPA does not provide a standard way to retrieve
   * the underlying JDBC Connection (due to the fact that a JPA implementation might not
   * work with a relational database at all).
   * <p>Implementations are encouraged to return an unwrapped Connection object, i.e.
   * the Connection as they got it from the connection pool. This makes it easier for
   * application code to get at the underlying native JDBC Connection, like an
   * OracleConnection, which is sometimes necessary for LOB handling etc. We assume
   * that calling code knows how to properly handle the returned Connection object.
   * <p>In a simple case where the returned Connection will be auto-closed with the
   * EntityManager or can be released via the Connection object itself, an
   * implementation can return a SimpleConnectionHandle that just contains the
   * Connection. If some other object is needed in {@code releaseJdbcConnection},
   * an implementation should use a special handle that references that other object.
   *
   * @param entityManager the current JPA EntityManager
   * @param readOnly whether the Connection is only needed for read-only purposes
   * @return a handle for the Connection, to be passed into {@code releaseJdbcConnection},
   * or {@code null} if no JDBC Connection can be retrieved
   * @throws PersistenceException if thrown by JPA methods
   * @throws SQLException if thrown by JDBC methods
   * @see #releaseJdbcConnection
   * @see cn.taketoday.jdbc.datasource.ConnectionHandle#getConnection
   * @see cn.taketoday.jdbc.datasource.SimpleConnectionHandle
   * @see JpaTransactionManager#setDataSource
   */
  @Nullable
  ConnectionHandle getJdbcConnection(EntityManager entityManager, boolean readOnly)
          throws PersistenceException, SQLException;

  /**
   * Release the given JDBC Connection, which has originally been retrieved
   * via {@code getJdbcConnection}. This should be invoked in any case,
   * to allow for proper release of the retrieved Connection handle.
   * <p>An implementation might simply do nothing, if the Connection returned
   * by {@code getJdbcConnection} will be implicitly closed when the JPA
   * transaction completes or when the EntityManager is closed.
   *
   * @param conHandle the JDBC Connection handle to release
   * @param entityManager the current JPA EntityManager
   * @throws PersistenceException if thrown by JPA methods
   * @throws SQLException if thrown by JDBC methods
   * @see #getJdbcConnection
   */
  void releaseJdbcConnection(ConnectionHandle conHandle, EntityManager entityManager)
          throws PersistenceException, SQLException;

}
