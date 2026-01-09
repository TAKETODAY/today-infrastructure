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

package infra.cache.interceptor;

import org.jspecify.annotations.Nullable;

import infra.cache.Cache;
import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;

/**
 * A {@link CacheErrorHandler} implementation that logs error message. Can be
 * used when underlying cache errors should be ignored.
 *
 * @author Adam Ostrožlík
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class LoggingCacheErrorHandler implements CacheErrorHandler {

  private final Logger logger;

  private final boolean logStackTraces;

  /**
   * Create an instance that does not log stack traces.
   */
  public LoggingCacheErrorHandler() {
    this(false);
  }

  /**
   * Create an instance with the {@link Logger logger} to use.
   *
   * @param logger the logger to use
   * @param logStackTraces whether to log stack trace
   */
  public LoggingCacheErrorHandler(Logger logger, boolean logStackTraces) {
    Assert.notNull(logger, "Logger is required");
    this.logger = logger;
    this.logStackTraces = logStackTraces;
  }

  /**
   * Create a {@code LoggingCacheErrorHandler} that uses the default logging
   * category and the supplied {@code logStackTraces} flag.
   * <p>The default logging category is
   * "{@code infra.cache.interceptor.LoggingCacheErrorHandler}".
   *
   * @param logStackTraces whether to log stack traces
   */
  public LoggingCacheErrorHandler(boolean logStackTraces) {
    this(LoggerFactory.getLogger(LoggingCacheErrorHandler.class), logStackTraces);
  }

  /**
   * Create a {@code LoggingCacheErrorHandler} that uses the supplied
   * {@code loggerName} and {@code logStackTraces} flag.
   *
   * @param loggerName the logger name to use
   * @param logStackTraces whether to log stack traces
   */
  public LoggingCacheErrorHandler(String loggerName, boolean logStackTraces) {
    Assert.notNull(loggerName, "'loggerName' is required");
    this.logger = LoggerFactory.getLogger(loggerName);
    this.logStackTraces = logStackTraces;
  }

  @Override
  public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
    if (logger.isWarnEnabled()) {
      doLogCacheError(logger,
              createMessage(cache, "failed to get entry with key '" + key + "'"),
              exception);
    }
  }

  @Override
  public void handleCachePutError(RuntimeException exception, Cache cache, Object key, @Nullable Object value) {
    if (logger.isWarnEnabled()) {
      doLogCacheError(logger,
              createMessage(cache, "failed to put entry with key '" + key + "'"),
              exception);
    }
  }

  @Override
  public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
    if (logger.isWarnEnabled()) {
      doLogCacheError(logger,
              createMessage(cache, "failed to evict entry with key '" + key + "'"),
              exception);
    }
  }

  @Override
  public void handleCacheClearError(RuntimeException exception, Cache cache) {
    if (logger.isWarnEnabled()) {
      doLogCacheError(logger, createMessage(cache, "failed to clear entries"), exception);
    }
  }

  /**
   * Get the logger for this {@code LoggingCacheErrorHandler}.
   *
   * @return the logger
   */
  protected final Logger getLogger() {
    return logger;
  }

  /**
   * Get the {@code logStackTraces} flag for this {@code LoggingCacheErrorHandler}.
   *
   * @return {@code true} if this {@code LoggingCacheErrorHandler} logs stack traces
   */
  protected final boolean isLogStackTraces() {
    return this.logStackTraces;
  }

  /**
   * Log the specified message.
   *
   * @param logger the logger
   * @param message the message
   * @param ex the exception
   */
  protected void doLogCacheError(Logger logger, String message, RuntimeException ex) {
    if (logStackTraces) {
      logger.warn(message, ex);
    }
    else {
      logger.warn(message);
    }
  }

  private String createMessage(Cache cache, String reason) {
    return String.format("Cache '%s' %s", cache.getName(), reason);
  }

}
