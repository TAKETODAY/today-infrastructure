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

package cn.taketoday.scheduling.concurrent;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.Callable;

import cn.taketoday.lang.Nullable;
import cn.taketoday.scheduling.support.TaskUtils;
import cn.taketoday.util.ErrorHandler;
import cn.taketoday.util.ReflectionUtils;

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
