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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Extension of {@link CompletableFuture} which allows for cancelling
 * a delegate along with the {@link CompletableFuture} itself.
 *
 * @param <T> the result type returned by this Future's {@code get} method
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class DelegatingCompletableFuture<T> extends CompletableFuture<T> implements ListenableFutureCallback<T> {

  private final Future<T> delegate;

  public DelegatingCompletableFuture(Future<T> delegate) {
    Assert.notNull(delegate, "Delegate is required");
    this.delegate = delegate;
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    boolean result = this.delegate.cancel(mayInterruptIfRunning);
    super.cancel(mayInterruptIfRunning);
    return result;
  }

  @Override
  public void onFailure(Throwable ex) {
    completeExceptionally(ex);
  }

  @Override
  public void onSuccess(@Nullable T result) {
    complete(result);
  }

}
