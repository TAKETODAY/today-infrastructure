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

package infra.web.socket;

import infra.util.concurrent.Future;
import infra.util.concurrent.Promise;
import io.netty.util.concurrent.GenericFutureListener;

/**
 * Adapt the completion state of a Netty future to an infrastructure future.
 *
 * <p>Success, failure, and cancellation are propagated from the source.
 * Cancelling the adapted future does not cancel the source operation.
 *
 * @param <V> value type
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public class PromiseAdapter<V> implements GenericFutureListener<io.netty.util.concurrent.Future<V>> {

  private final Promise<V> promise;

  PromiseAdapter(Promise<V> promise) {
    this.promise = promise;
  }

  @Override
  public void operationComplete(io.netty.util.concurrent.Future<V> future) {
    if (future.isCancelled()) {
      promise.cancel(future.cause(), false);
      return;
    }
    Throwable cause = future.cause();
    if (cause != null) {
      promise.tryFailure(cause);
    }
    else {
      promise.trySuccess(future.getNow());
    }
  }

  /**
   * Adapt a Netty future using the default infrastructure notification executor.
   *
   * @param future the source future
   * @param <T> the result type
   * @return a future representing the source completion state
   */
  public static <T> Future<T> adapt(io.netty.util.concurrent.Future<T> future) {
    return adapt(future, Future.forPromise());
  }

  /**
   * Propagate the source completion state to the given promise. Notifications
   * use the promise's executor rather than the source's executor.
   *
   * @param future the source future
   * @param settable the target promise
   * @param <T> the result type
   * @return the target promise
   */
  public static <T> Future<T> adapt(io.netty.util.concurrent.Future<T> future, Promise<T> settable) {
    future.addListener(new PromiseAdapter<>(settable));
    return settable;
  }
}
