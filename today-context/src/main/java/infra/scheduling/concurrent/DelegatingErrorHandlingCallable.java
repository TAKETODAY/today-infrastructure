/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.scheduling.concurrent;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.Callable;

import infra.scheduling.support.TaskUtils;
import infra.util.ErrorHandler;
import infra.util.ReflectionUtils;

/**
 * {@link Callable} adapter for an {@link ErrorHandler}.
 *
 * @param <V> the value type
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class DelegatingErrorHandlingCallable<V> implements Callable<V> {

  private final Callable<V> delegate;

  private final ErrorHandler errorHandler;

  public DelegatingErrorHandlingCallable(Callable<V> delegate, @Nullable ErrorHandler errorHandler) {
    this.delegate = delegate;
    this.errorHandler = (errorHandler != null ? errorHandler :
            TaskUtils.getDefaultErrorHandler(false));
  }

  @Override
  @Nullable
  public V call() throws Exception {
    try {
      return this.delegate.call();
    }
    catch (Throwable ex) {
      try {
        this.errorHandler.handleError(ex);
      }
      catch (UndeclaredThrowableException exToPropagate) {
        ReflectionUtils.rethrowException(exToPropagate.getUndeclaredThrowable());
      }
      return null;
    }
  }

}
