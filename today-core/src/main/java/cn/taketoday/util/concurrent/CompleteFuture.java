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
import java.util.concurrent.TimeUnit;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * A skeletal {@link Future} implementation which represents
 * a {@link Future} which has been completed already.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/26 21:28
 */
public abstract class CompleteFuture<V> extends AbstractFuture<V> {

  /**
   * Creates a new instance.
   *
   * @param executor the {@link Executor} associated with this future
   */
  protected CompleteFuture(@Nullable Executor executor) {
    super(executor);
  }

  @Override
  public CompleteFuture<V> onCompleted(FutureListener<? extends Future<V>> listener) {
    Assert.notNull(listener, "listener is required");
    DefaultFuture.notifyListener(executor, this, listener);
    return this;
  }

  @Override
  public <C> CompleteFuture<V> onCompleted(FutureContextListener<? extends Future<V>, C> listener, @Nullable C context) {
    DefaultFuture.notifyListener(executor, this, FutureListener.forAdaption(listener, context));
    return this;
  }

  @Override
  public CompleteFuture<V> await() throws InterruptedException {
    if (Thread.interrupted()) {
      throw new InterruptedException();
    }
    return this;
  }

  @Override
  public final boolean await(long timeout, TimeUnit unit) throws InterruptedException {
    if (Thread.interrupted()) {
      throw new InterruptedException();
    }
    return true;
  }

  @Override
  public CompleteFuture<V> sync() throws InterruptedException {
    return this;
  }

  @Override
  public CompleteFuture<V> syncUninterruptibly() {
    return this;
  }

  @Override
  public final boolean await(long timeoutMillis) throws InterruptedException {
    if (Thread.interrupted()) {
      throw new InterruptedException();
    }
    return true;
  }

  @Override
  public final CompleteFuture<V> awaitUninterruptibly() {
    return this;
  }

  @Override
  public final boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
    return true;
  }

  @Override
  public final boolean awaitUninterruptibly(long timeoutMillis) {
    return true;
  }

  @Override
  public final boolean isDone() {
    return true;
  }

  @Override
  public final boolean isCancelled() {
    return false;
  }

  /**
   * {@inheritDoc}
   *
   * @param mayInterruptIfRunning this value has no effect in this implementation.
   */
  @Override
  public final boolean cancel(boolean mayInterruptIfRunning) {
    return false;
  }

}
