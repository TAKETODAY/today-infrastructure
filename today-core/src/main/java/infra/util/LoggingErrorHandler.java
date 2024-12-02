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

package infra.util;

import java.io.Serializable;

import infra.core.style.ToStringBuilder;
import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;

/**
 * An {@link ErrorHandler} implementation that logs the Throwable at error
 * level. It does not perform any additional error handling. This can be
 * useful when suppression of errors is the intended behavior.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2024/12/2 16:34
 */
class LoggingErrorHandler implements ErrorHandler, Serializable {

  private static final String defaultMessage = "Unexpected error occurred";

  @Nullable
  private volatile Logger logger;

  private final String message;

  private final String loggerName;

  public LoggingErrorHandler(@Nullable String message, @Nullable String loggerName) {
    this.message = StringUtils.isBlank(message) ? defaultMessage : message;
    this.loggerName = StringUtils.isBlank(loggerName) ? getClass().getName() : loggerName;
  }

  @Override
  public void handleError(Throwable t) {
    Logger logger = this.logger;
    if (logger == null) {
      synchronized(this) {
        logger = this.logger;
        if (logger == null) {
          logger = LoggerFactory.getLogger(loggerName);
          this.logger = logger;
        }
      }
    }
    logger.error(message, t);
  }

  @Override
  public String toString() {
    return ToStringBuilder.forInstance(this)
            .append("message", message)
            .append("loggerName", loggerName)
            .toString();
  }

}
