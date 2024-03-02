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

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Abstract {@link ListenableFuture} implementation which does not allow for cancellation.
 *
 * @param <V>
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class AbstractFuture<V> implements ListenableFuture<V> {
  @Nullable
  @Override
  public V get() throws InterruptedException, ExecutionException {
    await();

    Throwable cause = getCause();
    if (cause == null) {
      return getNow();
    }
    if (cause instanceof CancellationException) {
      throw (CancellationException) cause;
    }
    throw new ExecutionException(cause);
  }

  @Nullable
  @Override
  public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    if (await(timeout, unit)) {
      Throwable cause = getCause();
      if (cause == null) {
        return getNow();
      }
      if (cause instanceof CancellationException) {
        throw (CancellationException) cause;
      }
      throw new ExecutionException(cause);
    }
    throw new TimeoutException("Timeout");
  }

  @Override
  public V obtain() {
    V now = getNow();
    Assert.state(now != null, "Result is required");
    return now;
  }

}
