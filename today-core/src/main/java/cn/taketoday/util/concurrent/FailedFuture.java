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

package cn.taketoday.util.concurrent;

import java.util.concurrent.Executor;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ExceptionUtils;

/**
 * The {@link CompleteFuture} which is failed already.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/26 21:31
 */
final class FailedFuture<V> extends CompleteFuture<V> {

  private final Throwable cause;

  /**
   * Creates a new instance.
   *
   * @param executor the {@link Executor} associated with this future
   * @param cause the cause of failure
   */
  FailedFuture(@Nullable Executor executor, Throwable cause) {
    super(executor);
    this.cause = cause;
  }

  @Override
  public Throwable getCause() {
    return cause;
  }

  @Override
  public boolean isSuccess() {
    return false;
  }

  @Override
  public boolean isFailed() {
    return true;
  }

  @Override
  public FailedFuture<V> sync() {
    throw ExceptionUtils.sneakyThrow(cause);
  }

  @Override
  public FailedFuture<V> syncUninterruptibly() {
    throw ExceptionUtils.sneakyThrow(cause);
  }

  @Override
  public V getNow() {
    return null;
  }

  @Override
  public V obtain() throws IllegalStateException {
    throw new IllegalStateException("FailedFuture");
  }

}
