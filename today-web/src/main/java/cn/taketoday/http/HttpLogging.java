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

package cn.taketoday.http;

import cn.taketoday.logging.LogDelegateFactory;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * Holds the shared logger named "cn.taketoday.http.HttpLogging" for HTTP
 * related logging when "cn.taketoday.http" is not enabled but
 * "cn.taketoday.web" is.
 *
 * <p>That means "cn.taketoday.web" enables all web logging including
 * from lower level packages such as "cn.taketoday.http" and modules
 * such as codecs from {@literal "today-core"} when those are wrapped with
 * {@link cn.taketoday.http.codec.EncoderHttpMessageWriter EncoderHttpMessageWriter} or
 * {@link cn.taketoday.http.codec.DecoderHttpMessageReader DecoderHttpMessageReader}.
 *
 * <p>To see logging from the primary class loggers simply enable logging for
 * "cn.taketoday.http" and "cn.taketoday.codec".
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see LogDelegateFactory
 * @since 4.0 2021/11/5 22:30
 */
public abstract class HttpLogging {

  private static final Logger fallbackLogger = LoggerFactory.getLogger(HttpLogging.class);

  /**
   * Create a primary logger for the given class and wrap it with a composite
   * that delegates to it or to the fallback logger
   * "cn.taketoday.web.HttpLogging", if the primary is not enabled.
   *
   * @param primaryLoggerClass the class for the name of the primary logger
   * @return the resulting composite logger
   */
  public static Logger forLogName(Class<?> primaryLoggerClass) {
    Logger primaryLogger = LoggerFactory.getLogger(primaryLoggerClass);
    return forLog(primaryLogger);
  }

  /**
   * Wrap the given primary logger with a composite logger that delegates to
   * it or to the fallback logger "cn.taketoday.web.HttpLogging",
   * if the primary is not enabled.
   *
   * @param primaryLogger the primary logger to use
   * @return the resulting composite logger
   */
  public static Logger forLog(Logger primaryLogger) {
    return LogDelegateFactory.getCompositeLog(primaryLogger, fallbackLogger);
  }

}
