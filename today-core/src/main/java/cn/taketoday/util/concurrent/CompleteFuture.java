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
 * A skeletal {@link ListenableFuture} implementation which represents
 * a {@link ListenableFuture} which has been completed already.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/26 21:28
 */
public abstract class CompleteFuture<V> extends AbstractFuture<V> {

  @Nullable
  private final Executor executor;

  protected CompleteFuture() {
    executor = null;
  }

  /**
   * Creates a new instance.
   *
   * @param executor the {@link Executor} associated with this future
   */
  protected CompleteFuture(@Nullable Executor executor) {
    this.executor = executor;
  }

  /**
   * Return the {@link Executor} which is used by this {@link CompleteFuture}.
   */
  @Nullable
  protected Executor executor() {
    return executor;
  }

  @Override
  public ListenableFuture<V> addListener(FutureListener<? extends ListenableFuture<V>> listener) {
    Assert.notNull(listener, "listener is required");
    DefaultFuture.notifyListener(executor(), this, listener);
    return this;
  }

  @Override
  public ListenableFuture<V> addListeners(FutureListener<? extends ListenableFuture<V>>... listeners) {
    Assert.notNull(listeners, "listeners is required");
    for (var l : listeners) {
      if (l == null) {
        break;
      }
      DefaultFuture.notifyListener(executor(), this, l);
    }
    return this;
  }

  @Override
  public ListenableFuture<V> removeListener(FutureListener<? extends ListenableFuture<V>> listener) {
    return this;
  }

  @Override
  public ListenableFuture<V> removeListeners(FutureListener<? extends ListenableFuture<V>>... listeners) {
    return this;
  }

  @Override
  public ListenableFuture<V> await() throws InterruptedException {
    if (Thread.interrupted()) {
      throw new InterruptedException();
    }
    return this;
  }

  @Override
  public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
    if (Thread.interrupted()) {
      throw new InterruptedException();
    }
    return true;
  }

  @Override
  public ListenableFuture<V> sync() throws InterruptedException {
    return this;
  }

  @Override
  public ListenableFuture<V> syncUninterruptibly() {
    return this;
  }

  @Override
  public boolean await(long timeoutMillis) throws InterruptedException {
    if (Thread.interrupted()) {
      throw new InterruptedException();
    }
    return true;
  }

  @Override
  public ListenableFuture<V> awaitUninterruptibly() {
    return this;
  }

  @Override
  public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
    return true;
  }

  @Override
  public boolean awaitUninterruptibly(long timeoutMillis) {
    return true;
  }

  @Override
  public boolean isDone() {
    return true;
  }

  @Override
  public boolean isCancellable() {
    return false;
  }

  @Override
  public boolean isCancelled() {
    return false;
  }

  /**
   * {@inheritDoc}
   *
   * @param mayInterruptIfRunning this value has no effect in this implementation.
   */
  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return false;
  }

}
