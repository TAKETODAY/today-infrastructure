/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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
package cn.taketoday.transaction;

import java.sql.Connection;

/**
 * @author TODAY <br>
 * 2018-10-09 11:51
 */
public interface TransactionDefinition {

  /**
   * Support a current transaction; create a new one if none exists. Analogous to
   * the EJB transaction attribute of the same name.
   * <p>
   * This is typically the default setting of a transaction definition, and
   * typically defines a transaction synchronization scope.
   */
  int PROPAGATION_REQUIRED = 0;

  int PROPAGATION_SUPPORTS = 1;

  /**
   * Support a current transaction; throw an exception if no current transaction
   * exists. Analogous to the EJB transaction attribute of the same name.
   * <p>
   * Note that transaction synchronization within a {@code PROPAGATION_MANDATORY}
   * scope will always be driven by the surrounding transaction.
   */
  int PROPAGATION_MANDATORY = 2;

  /**
   * Create a new transaction, suspending the current transaction if one exists.
   * Analogous to the EJB transaction attribute of the same name.
   * <p>
   * A {@code PROPAGATION_REQUIRES_NEW} scope always defines its own transaction
   * synchronizations. Existing synchronizations will be suspended and resumed
   * appropriately.
   */
  int PROPAGATION_REQUIRES_NEW = 3;

  int PROPAGATION_NOT_SUPPORTED = 4;

  int PROPAGATION_NEVER = 5;

  int PROPAGATION_NESTED = 6;

  int ISOLATION_DEFAULT = -1;

  int ISOLATION_READ_UNCOMMITTED = Connection.TRANSACTION_READ_UNCOMMITTED;

  int ISOLATION_READ_COMMITTED = Connection.TRANSACTION_READ_COMMITTED;

  int ISOLATION_REPEATABLE_READ = Connection.TRANSACTION_REPEATABLE_READ;

  int ISOLATION_SERIALIZABLE = Connection.TRANSACTION_SERIALIZABLE;

  int TIMEOUT_DEFAULT = -1;

  int getPropagationBehavior();

  int getIsolationLevel();

  int getTimeout();

  boolean isReadOnly();

  String getName();

  /**
   * Return a qualifier value associated with this transaction attribute.
   * <p>
   * This may be used for choosing a corresponding transaction manager to process
   * this specific transaction.
   */
  String getQualifier();

  /**
   * Should we roll back on the given exception?
   *
   * @param ex
   *         the exception to evaluate
   *
   * @return whether to perform a rollback or not
   */
  boolean rollbackOn(Throwable ex);
}
