/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.transaction.support;

import cn.taketoday.lang.Nullable;
import cn.taketoday.transaction.PlatformTransactionManager;
import cn.taketoday.transaction.TransactionDefinition;
import cn.taketoday.transaction.TransactionException;

/**
 * Extension of the {@link PlatformTransactionManager}
 * interface, exposing a method for executing a given callback within a transaction.
 *
 * <p>Implementors of this interface automatically express a preference for
 * callbacks over programmatic {@code getTransaction}, {@code commit}
 * and {@code rollback} calls. Calling code may check whether a given
 * transaction manager implements this interface to choose to prepare a
 * callback instead of explicit transaction demarcation control.
 *
 * <p>Framework's {@link TransactionTemplate} and
 * {@link cn.taketoday.transaction.interceptor.TransactionInterceptor}
 * detect and use this PlatformTransactionManager variant automatically.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see TransactionTemplate
 * @see cn.taketoday.transaction.interceptor.TransactionInterceptor
 * @since 4.0
 */
public interface CallbackPreferringPlatformTransactionManager extends PlatformTransactionManager {

  /**
   * Execute the action specified by the given callback object within a transaction.
   * <p>Allows for returning a result object created within the transaction, that is,
   * a domain object or a collection of domain objects. A RuntimeException thrown
   * by the callback is treated as a fatal exception that enforces a rollback.
   * Such an exception gets propagated to the caller of the template.
   *
   * @param definition the definition for the transaction to wrap the callback in
   * @param callback the callback object that specifies the transactional action
   * @return a result object returned by the callback, or {@code null} if none
   * @throws TransactionException in case of initialization, rollback, or system errors
   * @throws RuntimeException if thrown by the TransactionCallback
   */
  @Nullable
  <T> T execute(@Nullable TransactionDefinition definition, TransactionCallback<T> callback)
          throws TransactionException;

}
