/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.util;

import org.jspecify.annotations.Nullable;

import java.io.Serializable;

import infra.core.style.ToStringBuilder;
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
