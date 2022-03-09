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

package cn.taketoday.cache.interceptor;

import cn.taketoday.lang.Nullable;

/**
 * Abstract the invocation of a cache operation.
 *
 * <p>Does not provide a way to transmit checked exceptions but
 * provide a special exception that should be used to wrap any
 * exception that was thrown by the underlying invocation.
 * Callers are expected to handle this issue type specifically.
 *
 * @author Stephane Nicoll
 * @since 4.0
 */
@FunctionalInterface
public interface CacheOperationInvoker {

  /**
   * Invoke the cache operation defined by this instance. Wraps any exception
   * that is thrown during the invocation in a {@link ThrowableWrapper}.
   *
   * @return the result of the operation
   * @throws ThrowableWrapper if an error occurred while invoking the operation
   */
  @Nullable
  Object invoke() throws ThrowableWrapper;

  /**
   * Wrap any exception thrown while invoking {@link #invoke()}.
   */
  @SuppressWarnings("serial")
  class ThrowableWrapper extends RuntimeException {

    private final Throwable original;

    public ThrowableWrapper(Throwable original) {
      super(original.getMessage(), original);
      this.original = original;
    }

    public Throwable getOriginal() {
      return this.original;
    }
  }

}
