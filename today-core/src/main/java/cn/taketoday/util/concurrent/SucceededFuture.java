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

/**
 * The {@link CompleteFuture} which is succeeded already.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/26 21:33
 */
public final class SucceededFuture<V> extends CompleteFuture<V> {

  @Nullable
  private final V result;

  /**
   * Creates a new instance.
   */
  public SucceededFuture(@Nullable V result) {
    super(null);
    this.result = result;
  }

  /**
   * Creates a new instance.
   *
   * @param executor the {@link Executor} associated with this future
   */
  public SucceededFuture(@Nullable Executor executor, @Nullable V result) {
    super(executor);
    this.result = result;
  }

  @Override
  public Throwable getCause() {
    return null;
  }

  @Override
  public boolean isSuccess() {
    return true;
  }

  @Override
  public boolean isFailed() {
    return false;
  }

  @Override
  public V getNow() {
    return result;
  }
}

