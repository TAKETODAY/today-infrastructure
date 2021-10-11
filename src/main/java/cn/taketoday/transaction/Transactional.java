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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.lang.Constant;

/**
 * @author TODAY <br>
 * 2018-11-06 22:41
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface Transactional {

  /**
   * A qualifier determine the target transaction manager, matching the bean name
   * of a specific {@link TransactionManager} bean definition.
   *
   * @return {@link TransactionManager} bean name
   */
  String txManager() default Constant.BLANK;

  /**
   * The transaction propagation type.
   * <p>
   * Defaults to {@link Propagation#REQUIRED}.
   */
  Propagation propagation() default Propagation.REQUIRED;

  /**
   * The transaction isolation level.
   * <p>
   * Defaults to {@link Isolation#DEFAULT}.
   * <p>
   * Exclusively designed for use with {@link Propagation#REQUIRED} or
   * {@link Propagation#REQUIRES_NEW} since it only applies to newly started
   * transactions. Consider switching the "validateExistingTransactions" flag to
   * "true" on your transaction manager if you'd like isolation level declarations
   * to get rejected when participating in an existing transaction with a
   * different isolation level.
   */
  Isolation isolation() default Isolation.DEFAULT;

  /**
   * The timeout for this transaction.
   * <p>
   * Defaults to the default timeout of the underlying transaction system.
   * <p>
   * Exclusively designed for use with {@link Propagation#REQUIRED} or
   * {@link Propagation#REQUIRES_NEW} since it only applies to newly started
   * transactions.
   */
  int timeout() default TransactionDefinition.TIMEOUT_DEFAULT;

  boolean readOnly() default false;

  Class<? extends Throwable>[] rollbackOn() default {};

}
