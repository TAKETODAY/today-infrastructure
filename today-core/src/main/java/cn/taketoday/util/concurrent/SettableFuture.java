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
 * Special {@link Future} which is writable.
 * <p>
 *
 * A {@link Future} whose result can be set by a {@link #setSuccess(Object)},
 * {@link #setFailure(Throwable)} call. It can also, like any other {@code Future},
 * be {@linkplain #cancel cancelled}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/26 15:57
 */
public class SettableFuture<V> extends AbstractFuture<V> {

  /**
   * Creates a new instance.
   *
   * @param executor the {@link Executor} which is used to notify
   * the SettableFuture once it is complete.
   */
  protected SettableFuture(@Nullable Executor executor) {
    super(executor);
  }

  /**
   * Marks this future as a success and notifies all
   * listeners.
   *
   * @throws IllegalStateException If it is success or failed already
   */
  public void setSuccess(@Nullable V result) throws IllegalStateException {
    if (!trySuccess(result)) {
      throw new IllegalStateException("complete already: " + this);
    }
  }

  /**
   * Marks this future as a failure and notifies all
   * listeners.
   *
   * @throws IllegalStateException If it is success or failed already
   */
  public void setFailure(Throwable cause) throws IllegalStateException {
    if (!tryFailure(cause)) {
      throw new IllegalStateException("complete already: " + this, cause);
    }
  }

  @Override
  public SettableFuture<V> onCompleted(FutureListener<? extends Future<V>> listener) {
    super.onCompleted(listener);
    return this;
  }

  @Override
  public <C> SettableFuture<V> onCompleted(FutureContextListener<? extends Future<V>, C> listener, @Nullable C context) {
    super.onCompleted(listener, context);
    return this;
  }

  @Override
  public SettableFuture<V> sync() throws InterruptedException {
    super.sync();
    return this;
  }

  @Override
  public SettableFuture<V> syncUninterruptibly() {
    super.syncUninterruptibly();
    return this;
  }

  @Override
  public SettableFuture<V> await() throws InterruptedException {
    super.await();
    return this;
  }

  @Override
  public SettableFuture<V> awaitUninterruptibly() {
    super.awaitUninterruptibly();
    return this;
  }

  @Override
  public SettableFuture<V> cascadeTo(final SettableFuture<V> settable) {
    super.cascadeTo(settable);
    return this;
  }

}
