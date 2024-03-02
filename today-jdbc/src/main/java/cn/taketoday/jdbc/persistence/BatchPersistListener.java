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

package cn.taketoday.jdbc.persistence;

import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/9/20 12:47
 */
public interface BatchPersistListener {

  /**
   * before batch processing
   *
   * @param execution batch execution
   * @param implicitExecution implicit Execution
   */
  default void beforeProcessing(BatchExecution execution, boolean implicitExecution) { }

  /**
   * after batch processing
   *
   * @param execution batch execution
   * @param implicitExecution implicit Execution
   * @param exception batch execution error
   */
  void afterProcessing(BatchExecution execution, boolean implicitExecution, @Nullable Throwable exception);

}
