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

import java.io.Serial;
import java.io.Serializable;
import java.sql.SQLException;

import cn.taketoday.dao.DataAccessException;
import cn.taketoday.jdbc.datasource.ConnectionHandle;
import cn.taketoday.lang.Nullable;
import cn.taketoday.transaction.InvalidIsolationLevelException;
import cn.taketoday.transaction.TransactionDefinition;
import cn.taketoday.transaction.TransactionException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;

/**
 * Default implementation of the {@link JpaDialect} interface.
 * Used as default dialect by {@link JpaTransactionManager}.
 *
 * <p>Simply begins a standard JPA transaction in {@link #beginTransaction} and
 * performs standard exception translation through {@link EntityManagerFactoryUtils}.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see JpaTransactionManager#setJpaDialect
 * @since 4.0
 */
public class DefaultJpaDialect implements JpaDialect, Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * This implementation invokes the standard JPA {@code Transaction.begin}
   * method. Throws an InvalidIsolationLevelException if a non-default isolation
   * level is set.
   * <p>This implementation does not return any transaction data Object, since there
   * is no state to be kept for a standard JPA transaction. Hence, subclasses do not
   * have to care about the return value ({@code null}) of this implementation
   * and are free to return their own transaction data Object.
   *
   * @see jakarta.persistence.EntityTransaction#begin
   * @see cn.taketoday.transaction.InvalidIsolationLevelException
   * @see #cleanupTransaction
   */
  @Override
  @Nullable
  public Object beginTransaction(EntityManager entityManager, TransactionDefinition definition)
          throws PersistenceException, SQLException, TransactionException {

    if (definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT) {
      throw new InvalidIsolationLevelException(getClass().getSimpleName() +
              " does not support custom isolation levels due to limitations in standard JPA. " +
              "Specific arrangements may be implemented in custom JpaDialect variants.");
    }
    entityManager.getTransaction().begin();
    return null;
  }

  @Override
  @Nullable
  public Object prepareTransaction(EntityManager entityManager, boolean readOnly, @Nullable String name)
          throws PersistenceException {

    return null;
  }

  /**
   * This implementation does nothing, since the default {@code beginTransaction}
   * implementation does not require any cleanup.
   *
   * @see #beginTransaction
   */
  @Override
  public void cleanupTransaction(@Nullable Object transactionData) {

  }

  /**
   * This implementation always returns {@code null},
   * indicating that no JDBC Connection can be provided.
   */
  @Override
  @Nullable
  public ConnectionHandle getJdbcConnection(EntityManager entityManager, boolean readOnly)
          throws PersistenceException, SQLException {

    return null;
  }

  /**
   * This implementation does nothing, assuming that the Connection
   * will implicitly be closed with the EntityManager.
   * <p>If the JPA implementation returns a Connection handle that it expects
   * the application to close after use, the dialect implementation needs to invoke
   * {@code Connection.close()} (or some other method with similar effect) here.
   *
   * @see java.sql.Connection#close()
   */
  @Override
  public void releaseJdbcConnection(ConnectionHandle conHandle, EntityManager em)
          throws PersistenceException, SQLException {

  }

  //-----------------------------------------------------------------------------------
  // Hook for exception translation (used by JpaTransactionManager)
  //-----------------------------------------------------------------------------------

  /**
   * This implementation delegates to EntityManagerFactoryUtils.
   *
   * @see EntityManagerFactoryUtils#convertJpaAccessExceptionIfPossible
   */
  @Override
  @Nullable
  public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
    return EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(ex);
  }

}
