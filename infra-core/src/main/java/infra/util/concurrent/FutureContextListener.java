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

import java.util.EventListener;

/**
 * Listens to the result of a {@link Future}.
 * The result of the asynchronous operation is notified once this listener
 * is added by calling {@link Future#onCompleted(FutureContextListener, Object)}.
 * <pre>{@code
 * Future f = Future.forSettable();
 * f.onCompleted((future, context) -> { .. }, context);
 * }</pre>
 *
 * @param <C> Context type
 * @param <F> Future type
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see FutureListener
 * @since 4.0 2024/3/21 16:36
 */
@FunctionalInterface
public interface FutureContextListener<F extends Future<?>, C extends @Nullable Object> extends EventListener {

  /**
   * Invoked when the operation associated with the {@link Future} has been completed.
   *
   * @param completed the source {@link Future} which called this callback
   */
  void operationComplete(F completed, C context) throws Throwable;

}
