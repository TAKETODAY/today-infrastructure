/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.scheduling.support;

import cn.taketoday.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Callback interface that can be used to customize a {@link ThreadPoolTaskExecutor}.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see TaskExecutorBuilder
 * @since 4.0
 */
@Deprecated
@FunctionalInterface
public interface TaskExecutorCustomizer {

  /**
   * Callback to customize a {@link ThreadPoolTaskExecutor} instance.
   *
   * @param taskExecutor the task executor to customize
   */
  void customize(ThreadPoolTaskExecutor taskExecutor);

}
