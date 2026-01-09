/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
public class Promise<V extends @Nullable Object> extends AbstractFuture<V> {

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
