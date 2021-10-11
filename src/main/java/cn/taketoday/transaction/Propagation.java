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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.transaction;

/**
 * Enumeration that represents transaction propagation behaviors for use with
 * the {@link Transactional} annotation, corresponding to the
 * {@link TransactionDefinition} interface.
 *
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 */
public enum Propagation {

  /**
   * Support a current transaction, create a new one if none exists. Analogous to
   * EJB transaction attribute of the same name.
   * <p>
   * This is the default setting of a transaction annotation.
   */
  REQUIRED(TransactionDefinition.PROPAGATION_REQUIRED),

  /**
   * Support a current transaction, execute non-transactionally if none exists.
   * Analogous to EJB transaction attribute of the same name.
   * <p>
   * Note: For transaction managers with transaction synchronization,
   * PROPAGATION_SUPPORTS is slightly different from no transaction at all, as it
   * defines a transaction scope that synchronization will apply for. As a
   * consequence, the same resources (JDBC Connection, Hibernate Session, etc)
   * will be shared for the entire specified scope. Note that this depends on the
   * actual synchronization configuration of the transaction manager.
   */
  SUPPORTS(TransactionDefinition.PROPAGATION_SUPPORTS),

  /**
   * Support a current transaction, throw an exception if none exists. Analogous
   * to EJB transaction attribute of the same name.
   */
  MANDATORY(TransactionDefinition.PROPAGATION_MANDATORY),

  /**
   * Create a new transaction, and suspend the current transaction if one exists.
   * Analogous to the EJB transaction attribute of the same name.
   * <p>
   */
  REQUIRES_NEW(TransactionDefinition.PROPAGATION_REQUIRES_NEW),

  /**
   * Execute non-transactionally, suspend the current transaction if one exists.
   * Analogous to EJB transaction attribute of the same name.
   * <p>
   */
  NOT_SUPPORTED(TransactionDefinition.PROPAGATION_NOT_SUPPORTED),

  /**
   * Execute non-transactionally, throw an exception if a transaction exists.
   * Analogous to EJB transaction attribute of the same name.
   */
  NEVER(TransactionDefinition.PROPAGATION_NEVER),

  /**
   * Execute within a nested transaction if a current transaction exists, behave
   * like PROPAGATION_REQUIRED else. There is no analogous feature in EJB.
   * <p>
   * Note: Actual creation of a nested transaction will only work on specific
   * transaction managers. Out of the box, this only applies to the JDBC
   * DataSourceTransactionManager when working on a JDBC 3.0 driver. Some JTA
   * providers might support nested transactions as well.
   */
  NESTED(TransactionDefinition.PROPAGATION_NESTED);

  private final int value;

  Propagation(int value) {
    this.value = value;
  }

  public int value() {
    return this.value;
  }

}
