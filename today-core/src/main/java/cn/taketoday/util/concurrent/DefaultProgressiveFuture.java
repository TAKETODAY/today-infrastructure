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

import static io.netty.util.internal.ObjectUtil.checkPositiveOrZero;

/**
 * Default ProgressiveFuture
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/26 21:24
 */
public class DefaultProgressiveFuture<V> extends DefaultFuture<V> implements ProgressiveFuture<V> {

  /**
   * Creates a new instance.
   */
  public DefaultProgressiveFuture() { }

  /**
   * Creates a new instance.
   *
   * @param executor the {@link Executor} which is used to notify the
   * SettableFuture when it progresses or it is complete
   */
  public DefaultProgressiveFuture(@Nullable Executor executor) {
    super(executor);
  }

  @Override
  public ProgressiveFuture<V> setProgress(long progress, long total) {
    if (total < 0) {
      // total unknown
      total = -1; // normalize
      checkPositiveOrZero(progress, "progress");
    }
    else if (progress < 0 || progress > total) {
      throw new IllegalArgumentException(
              "progress: %s (expected: 0 <= progress <= total (%s))".formatted(progress, total));
    }

    if (isDone()) {
      throw new IllegalStateException("complete already");
    }

    notifyProgressiveListeners(progress, total);
    return this;
  }

  @Override
  public boolean tryProgress(long progress, long total) {
    if (total < 0) {
      total = -1;
      if (progress < 0 || isDone()) {
        return false;
      }
    }
    else if (progress < 0 || progress > total || isDone()) {
      return false;
    }

    notifyProgressiveListeners(progress, total);
    return true;
  }

  @Override
  public DefaultProgressiveFuture<V> addListener(FutureListener<? extends Future<V>> listener) {
    super.addListener(listener);
    return this;
  }

  @Override
  public <C> DefaultProgressiveFuture<V> addListener(FutureContextListener<C, ? extends Future<V>> listener, @Nullable C context) {
    super.addListener(listener, context);
    return this;
  }

  @Override
  public DefaultProgressiveFuture<V> sync() throws InterruptedException {
    super.sync();
    return this;
  }

  @Override
  public DefaultProgressiveFuture<V> syncUninterruptibly() {
    super.syncUninterruptibly();
    return this;
  }

  @Override
  public DefaultProgressiveFuture<V> await() throws InterruptedException {
    super.await();
    return this;
  }

  @Override
  public DefaultProgressiveFuture<V> awaitUninterruptibly() {
    super.awaitUninterruptibly();
    return this;
  }

  @Override
  public DefaultProgressiveFuture<V> setSuccess(@Nullable V result) {
    super.setSuccess(result);
    return this;
  }

  @Override
  public DefaultProgressiveFuture<V> setFailure(Throwable cause) {
    super.setFailure(cause);
    return this;
  }

}
