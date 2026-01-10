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
