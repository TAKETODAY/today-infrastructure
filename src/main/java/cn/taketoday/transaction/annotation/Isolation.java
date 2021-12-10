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

package cn.taketoday.transaction.annotation;

import cn.taketoday.transaction.TransactionDefinition;

/**
 * Enumeration that represents transaction isolation levels for use
 * with the {@link Transactional} annotation, corresponding to the
 * {@link TransactionDefinition} interface.
 *
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @since 4.0
 */
public enum Isolation {

  /**
   * Use the default isolation level of the underlying datastore.
   * All other levels correspond to the JDBC isolation levels.
   *
   * @see java.sql.Connection
   */
  DEFAULT(TransactionDefinition.ISOLATION_DEFAULT),

  /**
   * A constant indicating that dirty reads, non-repeatable reads and phantom reads
   * can occur. This level allows a row changed by one transaction to be read by
   * another transaction before any changes in that row have been committed
   * (a "dirty read"). If any of the changes are rolled back, the second
   * transaction will have retrieved an invalid row.
   *
   * @see java.sql.Connection#TRANSACTION_READ_UNCOMMITTED
   */
  READ_UNCOMMITTED(TransactionDefinition.ISOLATION_READ_UNCOMMITTED),

  /**
   * A constant indicating that dirty reads are prevented; non-repeatable reads
   * and phantom reads can occur. This level only prohibits a transaction
   * from reading a row with uncommitted changes in it.
   *
   * @see java.sql.Connection#TRANSACTION_READ_COMMITTED
   */
  READ_COMMITTED(TransactionDefinition.ISOLATION_READ_COMMITTED),

  /**
   * A constant indicating that dirty reads and non-repeatable reads are
   * prevented; phantom reads can occur. This level prohibits a transaction
   * from reading a row with uncommitted changes in it, and it also prohibits
   * the situation where one transaction reads a row, a second transaction
   * alters the row, and the first transaction rereads the row, getting
   * different values the second time (a "non-repeatable read").
   *
   * @see java.sql.Connection#TRANSACTION_REPEATABLE_READ
   */
  REPEATABLE_READ(TransactionDefinition.ISOLATION_REPEATABLE_READ),

  /**
   * A constant indicating that dirty reads, non-repeatable reads and phantom
   * reads are prevented. This level includes the prohibitions in
   * {@code ISOLATION_REPEATABLE_READ} and further prohibits the situation
   * where one transaction reads all rows that satisfy a {@code WHERE}
   * condition, a second transaction inserts a row that satisfies that
   * {@code WHERE} condition, and the first transaction rereads for the
   * same condition, retrieving the additional "phantom" row in the second read.
   *
   * @see java.sql.Connection#TRANSACTION_SERIALIZABLE
   */
  SERIALIZABLE(TransactionDefinition.ISOLATION_SERIALIZABLE);

  private final int value;

  Isolation(int value) {
    this.value = value;
  }

  public int value() {
    return this.value;
  }

}
