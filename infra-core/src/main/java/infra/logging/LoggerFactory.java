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

/**
 * Factory that creates {@link Logger} instances.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-11-04 19:06
 */
public abstract class LoggerFactory {

  public static final String LOG_TYPE_SYSTEM_PROPERTY = "logger.factory";

  private static final LoggerFactory factory = createFactory();

  protected abstract Logger createLogger(String name);

  /**
   * Return a logger associated with a particular class.
   */
  public static Logger getLogger(Class<?> clazz) {
    return getLogger(clazz.getName());
  }

  /**
   * Return a logger associated with a particular class name.
   */
  public static Logger getLogger(String name) {
    return factory.createLogger(name);
  }

  private static synchronized LoggerFactory createFactory() {
    if (factory == null) {
      final String type = System.getProperty(LOG_TYPE_SYSTEM_PROPERTY);
      if (type != null) {
        try {
          return (LoggerFactory) Class.forName(type).getConstructor().newInstance();
        }
        catch (Throwable e) {
          e.printStackTrace();
          System.err.printf("Could not find valid log-type from system property '%s', value '%s'%n", LOG_TYPE_SYSTEM_PROPERTY, type);
        }
      }
      try {
        return new Slf4jLoggerFactory();
      }
      catch (Throwable ignored) {
      }
      try {
        return new Log4j2LoggerFactory();
      }
      catch (Throwable ignored) {
      }
      return new JavaLoggingFactory();
    }
    return factory;
  }

}
