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

package cn.taketoday.retry;

/**
 * Callback interface for an operation that can be retried using a
 * {@link RetryOperations}.
 *
 * @param <T> the type of object returned by the callback
 * @param <E> the type of exception it declares may be thrown
 * @author Rob Harrop
 * @author Dave Syer
 * @since 4.0
 */
public interface RetryCallback<T, E extends Throwable> {

  /**
   * Execute an operation with retry semantics. Operations should generally be
   * idempotent, but implementations may choose to implement compensation semantics when
   * an operation is retried.
   *
   * @param context the current retry context.
   * @return the result of the successful operation.
   * @throws E of type E if processing fails
   */
  T doWithRetry(RetryContext context) throws E;

}
