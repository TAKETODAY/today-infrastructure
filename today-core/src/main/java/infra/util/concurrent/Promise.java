/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.util.concurrent;

import org.jspecify.annotations.Nullable;

import java.util.concurrent.Executor;

/**
 * Special {@link Future} which is writable.
 * <p>
 *
 * A {@link Future} whose result can be set by a {@link #setSuccess(Object)},
 * {@link #setFailure(Throwable)} call. It can also, like any other {@code Future},
 * be {@linkplain #cancel cancelled}.
 *
 * @param <V> Value type
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/26 15:57
 */
public class Promise<V> extends AbstractFuture<V> {

  /**
   * Creates a new instance.
   *
   * @param executor the {@link Executor} which is used to notify
   * the Promise once it is complete.
   * @see Future#forPromise(Executor)
   */
  protected Promise(@Nullable Executor executor) {
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

}
