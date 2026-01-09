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

package infra.scheduling.annotation;

import org.jspecify.annotations.Nullable;

import java.util.concurrent.Executor;

import infra.aop.interceptor.AsyncUncaughtExceptionHandler;
import infra.context.annotation.Configuration;

/**
 * Interface to be implemented by @{@link Configuration
 * Configuration} classes annotated with @{@link EnableAsync} that wish to customize the
 * {@link Executor} instance used when processing async method invocations or the
 * {@link AsyncUncaughtExceptionHandler} instance used to process exception thrown from
 * async method with {@code void} return type.
 *
 * <p>See @{@link EnableAsync} for usage examples.
 *
 * @author Chris Beams
 * @author Stephane Nicoll
 * @see AbstractAsyncConfiguration
 * @see EnableAsync
 * @since 4.0
 */
public interface AsyncConfigurer {

  /**
   * The {@link Executor} instance to be used when processing async
   * method invocations.
   */
  @Nullable
  default Executor getAsyncExecutor() {
    return null;
  }

  /**
   * The {@link AsyncUncaughtExceptionHandler} instance to be used
   * when an exception is thrown during an asynchronous method execution
   * with {@code void} return type.
   */
  @Nullable
  default AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
    return null;
  }

}
