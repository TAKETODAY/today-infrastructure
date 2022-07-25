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

package cn.taketoday.transaction.support;

import cn.taketoday.lang.Nullable;
import cn.taketoday.transaction.TransactionStatus;

/**
 * Callback interface for transactional code. Used with {@link TransactionTemplate}'s
 * {@code execute} method, often as anonymous class within a method implementation.
 *
 * <p>Typically used to assemble various calls to transaction-unaware data access
 * services into a higher-level service method with transaction demarcation. As an
 * alternative, consider the use of declarative transaction demarcation (e.g. through
 * Framework's {@link cn.taketoday.transaction.annotation.Transactional} annotation).
 *
 * @param <T> the result type
 * @author Juergen Hoeller
 * @see TransactionTemplate
 * @see CallbackPreferringPlatformTransactionManager
 * @since 4.0
 */
@FunctionalInterface
public interface TransactionCallback<T> {

  /**
   * Gets called by {@link TransactionTemplate#execute} within a transactional context.
   * Does not need to care about transactions itself, although it can retrieve and
   * influence the status of the current transaction via the given status object,
   * e.g. setting rollback-only.
   * <p>Allows for returning a result object created within the transaction, i.e. a
   * domain object or a collection of domain objects. A RuntimeException thrown by the
   * callback is treated as application exception that enforces a rollback. Any such
   * exception will be propagated to the caller of the template, unless there is a
   * problem rolling back, in which case a TransactionException will be thrown.
   *
   * @param status associated transaction status
   * @return a result object, or {@code null}
   * @see TransactionTemplate#execute
   * @see CallbackPreferringPlatformTransactionManager#execute
   */
  @Nullable
  T doInTransaction(TransactionStatus status);

}
