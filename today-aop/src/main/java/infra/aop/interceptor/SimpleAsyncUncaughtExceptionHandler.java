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
package infra.aop.interceptor;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;

import infra.logging.Logger;
import infra.logging.LoggerFactory;

/**
 * A default {@link AsyncUncaughtExceptionHandler} that simply logs the exception.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @author TODAY
 * @since 3.0
 */
public class SimpleAsyncUncaughtExceptionHandler implements AsyncUncaughtExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(SimpleAsyncUncaughtExceptionHandler.class);

  @Override
  public void handleUncaughtException(Throwable ex, Method method, @Nullable Object... params) {
    if (log.isErrorEnabled()) {
      log.error("Unexpected exception occurred invoking async method: {}", method, ex);
    }
  }

}
