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
 * Default SettableFuture
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/26 17:27
 */
class DefaultFuture<V> extends AbstractSettableFuture<V> implements SettableFuture<V> {

  /**
   * Creates a new instance.
   *
   * @param executor the {@link Executor} which is used to notify
   * the SettableFuture once it is complete.
   */
  DefaultFuture(@Nullable Executor executor) {
    super(executor);
  }

  @Override
  public DefaultFuture<V> setSuccess(@Nullable V result) {
    if (trySuccess(result)) {
      return this;
    }
    throw new IllegalStateException("complete already: " + this);
  }

  @Override
  public SettableFuture<V> setFailure(Throwable cause) {
    if (tryFailure(cause)) {
      return this;
    }
    throw new IllegalStateException("complete already: " + this, cause);
  }

  @Override
  public DefaultFuture<V> onCompleted(FutureListener<? extends Future<V>> listener) {
    super.onCompleted(listener);
    return this;
  }

  @Override
  public <C> DefaultFuture<V> onCompleted(FutureContextListener<? extends Future<V>, C> listener, @Nullable C context) {
    return onCompleted(FutureListener.forAdaption(listener, context));
  }

  @Override
  public DefaultFuture<V> sync() throws InterruptedException {
    super.sync();
    return this;
  }

  @Override
  public DefaultFuture<V> syncUninterruptibly() {
    super.syncUninterruptibly();
    return this;
  }

  @Override
  public DefaultFuture<V> await() throws InterruptedException {
    super.await();
    return this;
  }

  @Override
  public DefaultFuture<V> awaitUninterruptibly() {
    super.awaitUninterruptibly();
    return this;
  }

  @Override
  public DefaultFuture<V> cascadeTo(final SettableFuture<V> settable) {
    Futures.cascade(this, settable);
    return this;
  }

}
