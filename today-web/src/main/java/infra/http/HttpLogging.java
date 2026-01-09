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

package infra.http;

import infra.logging.LogDelegateFactory;
import infra.logging.Logger;
import infra.logging.LoggerFactory;

/**
 * Holds the shared logger named "infra.http.HttpLogging" for HTTP
 * related logging when "infra.http" is not enabled but
 * "infra.web" is.
 *
 * <p>That means "infra.web" enables all web logging including
 * from lower level packages such as "infra.http" and modules
 * such as codecs from {@literal "today-core"} when those are wrapped with
 * {@link infra.http.codec.EncoderHttpMessageWriter EncoderHttpMessageWriter} or
 * {@link infra.http.codec.DecoderHttpMessageReader DecoderHttpMessageReader}.
 *
 * <p>To see logging from the primary class loggers simply enable logging for
 * "infra.http" and "infra.codec".
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
   * "infra.web.HttpLogging", if the primary is not enabled.
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
   * it or to the fallback logger "infra.web.HttpLogging",
   * if the primary is not enabled.
   *
   * @param primaryLogger the primary logger to use
   * @return the resulting composite logger
   */
  public static Logger forLog(Logger primaryLogger) {
    return LogDelegateFactory.getCompositeLog(primaryLogger, fallbackLogger);
  }

}
