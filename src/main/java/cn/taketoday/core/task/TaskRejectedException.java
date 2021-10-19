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

package cn.taketoday.core.task;

import java.util.concurrent.RejectedExecutionException;

/**
 * Exception thrown when a {@link TaskExecutor} rejects to accept
 * a given task for execution.
 *
 * @author Juergen Hoeller
 * @see TaskExecutor#execute(Runnable)
 * @see TaskTimeoutException
 * @since 4.0
 */
@SuppressWarnings("serial")
public class TaskRejectedException extends RejectedExecutionException {

  /**
   * Create a new {@code TaskRejectedException}
   * with the specified detail message and no root cause.
   *
   * @param msg the detail message
   */
  public TaskRejectedException(String msg) {
    super(msg);
  }

  /**
   * Create a new {@code TaskRejectedException}
   * with the specified detail message and the given root cause.
   *
   * @param msg the detail message
   * @param cause the root cause (usually from using an underlying
   * API such as the {@code java.util.concurrent} package)
   * @see RejectedExecutionException
   */
  public TaskRejectedException(String msg, Throwable cause) {
    super(msg, cause);
  }

}
