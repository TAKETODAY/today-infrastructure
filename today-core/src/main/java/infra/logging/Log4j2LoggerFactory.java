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

package infra.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.spi.LoggerContext;

/**
 * LoggerFactory for log4j2
 */
final class Log4j2LoggerFactory extends LoggerFactory {

  Log4j2LoggerFactory() {
    LogManager.class.getName();
  }

  @Override
  protected Logger createLogger(String name) {
    LoggerContext context = Log4j2Logger.loggerContext;
    if (context == null) {
      // Circular call in early-init scenario -> static field not initialized yet
      context = LogManager.getContext(Log4j2Logger.class.getClassLoader(), false);
    }
    ExtendedLogger logger = context.getLogger(name);
    return new Log4j2Logger(logger);
  }
}
