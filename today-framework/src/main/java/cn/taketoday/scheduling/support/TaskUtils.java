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

package cn.taketoday.scheduling.support;

import java.util.concurrent.Future;

import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ErrorHandler;
import cn.taketoday.util.ReflectionUtils;

/**
 * Utility methods for decorating tasks with error handling.
 *
 * <p><b>NOTE:</b> This class is intended for internal use by  scheduler
 * implementations. It is only public so that it may be accessed from impl classes
 * within other packages. It is <i>not</i> intended for general use.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 4.0
 */
public abstract class TaskUtils {

  /**
   * An ErrorHandler strategy that will log the Exception but perform
   * no further handling. This will suppress the error so that
   * subsequent executions of the task will not be prevented.
   */
  public static final ErrorHandler LOG_AND_SUPPRESS_ERROR_HANDLER = new LoggingErrorHandler();

  /**
   * An ErrorHandler strategy that will log at error level and then
   * re-throw the Exception. Note: this will typically prevent subsequent
   * execution of a scheduled task.
   */
  public static final ErrorHandler LOG_AND_PROPAGATE_ERROR_HANDLER = new PropagatingErrorHandler();

  /**
   * Decorate the task for error handling. If the provided {@link ErrorHandler}
   * is not {@code null}, it will be used. Otherwise, repeating tasks will have
   * errors suppressed by default whereas one-shot tasks will have errors
   * propagated by default since those errors may be expected through the
   * returned {@link Future}. In both cases, the errors will be logged.
   */
  public static DelegatingErrorHandlingRunnable decorateTaskWithErrorHandler(
          Runnable task, @Nullable ErrorHandler errorHandler, boolean isRepeatingTask) {

    if (task instanceof DelegatingErrorHandlingRunnable) {
      return (DelegatingErrorHandlingRunnable) task;
    }
    ErrorHandler eh = (errorHandler != null ? errorHandler : getDefaultErrorHandler(isRepeatingTask));
    return new DelegatingErrorHandlingRunnable(task, eh);
  }

  /**
   * Return the default {@link ErrorHandler} implementation based on the boolean
   * value indicating whether the task will be repeating or not. For repeating tasks
   * it will suppress errors, but for one-time tasks it will propagate. In both
   * cases, the error will be logged.
   */
  public static ErrorHandler getDefaultErrorHandler(boolean isRepeatingTask) {
    return (isRepeatingTask ? LOG_AND_SUPPRESS_ERROR_HANDLER : LOG_AND_PROPAGATE_ERROR_HANDLER);
  }

  /**
   * An {@link ErrorHandler} implementation that logs the Throwable at error
   * level. It does not perform any additional error handling. This can be
   * useful when suppression of errors is the intended behavior.
   */
  private static class LoggingErrorHandler implements ErrorHandler {

    private final Logger logger = LoggerFactory.getLogger(LoggingErrorHandler.class);

    @Override
    public void handleError(Throwable t) {
      logger.error("Unexpected error occurred in scheduled task", t);
    }
  }

  /**
   * An {@link ErrorHandler} implementation that logs the Throwable at error
   * level and then propagates it.
   */
  private static class PropagatingErrorHandler extends LoggingErrorHandler {

    @Override
    public void handleError(Throwable t) {
      super.handleError(t);
      ReflectionUtils.rethrowRuntimeException(t);
    }
  }

}
