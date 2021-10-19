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

/**
 * Exception thrown when a {@link AsyncTaskExecutor} rejects to accept
 * a given task for execution because of the specified timeout.
 *
 * @author Juergen Hoeller
 * @see AsyncTaskExecutor#execute(Runnable, long)
 * @see TaskRejectedException
 * @since 4.0
 */
@SuppressWarnings("serial")
public class TaskTimeoutException extends TaskRejectedException {

  /**
   * Create a new {@code TaskTimeoutException}
   * with the specified detail message and no root cause.
   *
   * @param msg the detail message
   */
  public TaskTimeoutException(String msg) {
    super(msg);
  }

  /**
   * Create a new {@code TaskTimeoutException}
   * with the specified detail message and the given root cause.
   *
   * @param msg the detail message
   * @param cause the root cause (usually from using an underlying
   * API such as the {@code java.util.concurrent} package)
   * @see java.util.concurrent.RejectedExecutionException
   */
  public TaskTimeoutException(String msg, Throwable cause) {
    super(msg, cause);
  }

}
