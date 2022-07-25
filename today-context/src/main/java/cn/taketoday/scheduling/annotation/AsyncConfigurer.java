/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.scheduling.annotation;

import java.util.concurrent.Executor;

import cn.taketoday.aop.interceptor.AsyncUncaughtExceptionHandler;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.lang.Nullable;

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
