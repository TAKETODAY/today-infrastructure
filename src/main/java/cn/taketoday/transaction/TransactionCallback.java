/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

/**
 * @author Today <br>
 * 
 *         2018-12-30 20:59
 */
@FunctionalInterface
public interface TransactionCallback<T> {

    /**
     * Gets called by {@link TransactionTemplate#execute} within a transactional
     * context. Does not need to care about transactions itself, although it can
     * retrieve and influence the status of the current transaction via the given
     * status object, e.g. setting rollback-only.
     * <p>
     * Allows for returning a result object created within the transaction, i.e. a
     * domain object or a collection of domain objects. A RuntimeException thrown by
     * the callback is treated as application exception that enforces a rollback.
     * Any such exception will be propagated to the caller of the template, unless
     * there is a problem rolling back, in which case a TransactionException will be
     * thrown.
     * 
     * @param status
     *            associated transaction status
     * @return a result object, or {@code null}
     * @see TransactionTemplate#execute
     * @see CallbackPreferringPlatformTransactionManager#execute
     */
    T doInTransaction(TransactionStatus status);

}
