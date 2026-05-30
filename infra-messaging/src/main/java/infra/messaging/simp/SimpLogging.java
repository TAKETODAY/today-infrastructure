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

package infra.messaging.simp;

import infra.logging.LogDelegateFactory;
import infra.logging.Logger;
import infra.logging.LoggerFactory;

/**
 * Holds the shared logger named "infra.web.SimpLogging" to use
 * for STOMP over WebSocket messaging when logging for
 * "infra.messaging.simp" is off but logging for
 * "infra.web" is on.
 *
 * <p>This makes it possible to enable all web related logging via
 * "infra.web" including logging from lower-level packages such as
 * "infra.messaging.simp".
 *
 * <p>To see logging from the primary classes where log messages originate from,
 * simply enable logging for "infra.messaging".
 *
 * @author Rossen Stoyanchev
 * @see LogDelegateFactory
 * @since 5.0
 */
public abstract class SimpLogging {

  private static final Logger fallbackLogger =
          LoggerFactory.getLogger("infra.web." + SimpLogging.class.getSimpleName());

  /**
   * Create a primary logger for the given class and wrap it with a composite
   * that delegates to it or to the fallback logger named
   * "infra.web.SimpLogging", if the primary is not enabled.
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
   * either the primary or to the shared fallback logger
   * "infra.web.HttpLogging", if the primary is not enabled.
   *
   * @param primaryLogger the primary logger to use
   * @return the resulting composite logger
   */
  public static Logger forLog(Logger primaryLogger) {
    return LogDelegateFactory.getCompositeLog(primaryLogger, fallbackLogger);
  }

}
