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

package cn.taketoday.transaction.support;

import cn.taketoday.lang.Nullable;
import cn.taketoday.transaction.TransactionStatus;

/**
 * Simple convenience class for TransactionCallback implementation.
 * Allows for implementing a doInTransaction version without result,
 * i.e. without the need for a return statement.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see TransactionTemplate
 * @since 4.0
 */
@FunctionalInterface
public interface TransactionCallbackWithoutResult extends TransactionCallback<Object> {

  @Override
  @Nullable
  default Object doInTransaction(TransactionStatus status) {
    doInTransactionWithoutResult(status);
    return null;
  }

  /**
   * Gets called by {@code TransactionTemplate.execute} within a transactional
   * context. Does not need to care about transactions itself, although it can retrieve
   * and influence the status of the current transaction via the given status object,
   * e.g. setting rollback-only.
   * <p>A RuntimeException thrown by the callback is treated as application
   * exception that enforces a rollback. An exception gets propagated to the
   * caller of the template.
   * <p>Note when using JTA: JTA transactions only work with transactional
   * JNDI resources, so implementations need to use such resources if they
   * want transaction support.
   *
   * @param status associated transaction status
   * @see TransactionTemplate#executeWithoutResult
   */
  void doInTransactionWithoutResult(TransactionStatus status);

}
