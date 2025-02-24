/*
 * Copyright 2017 - 2025 the original author or authors.
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
