/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.cache.interceptor;

import cn.taketoday.cache.Cache;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * A {@link CacheErrorHandler} implementation that logs error message. Can be
 * used when underlying cache errors should be ignored.
 *
 * @author Adam Ostrožlík
 * @author Stephane Nicoll
 * @since 4.0
 */
public class LoggingCacheErrorHandler implements CacheErrorHandler {

  private final Logger logger;

  private final boolean logStacktrace;

  /**
   * Create an instance with the {@link Logger logger} to use.
   *
   * @param logger the logger to use
   * @param logStacktrace whether to log stack trace
   */
  public LoggingCacheErrorHandler(Logger logger, boolean logStacktrace) {
    Assert.notNull(logger, "Logger must not be null");
    this.logger = logger;
    this.logStacktrace = logStacktrace;
  }

  /**
   * Create an instance that does not log stack traces.
   */
  public LoggingCacheErrorHandler() {
    this(LoggerFactory.getLogger(LoggingCacheErrorHandler.class), false);
  }

  @Override
  public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
    logCacheError(logger,
            createMessage(cache, "failed to get entry with key '" + key + "'"),
            exception);
  }

  @Override
  public void handleCachePutError(RuntimeException exception, Cache cache, Object key, @Nullable Object value) {
    logCacheError(logger,
            createMessage(cache, "failed to put entry with key '" + key + "'"),
            exception);
  }

  @Override
  public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
    logCacheError(logger,
            createMessage(cache, "failed to evict entry with key '" + key + "'"),
            exception);
  }

  @Override
  public void handleCacheClearError(RuntimeException exception, Cache cache) {
    logCacheError(logger, createMessage(cache, "failed to clear entries"), exception);
  }

  /**
   * Log the specified message.
   *
   * @param logger the logger
   * @param message the message
   * @param ex the exception
   */
  protected void logCacheError(Logger logger, String message, RuntimeException ex) {
    if (this.logStacktrace) {
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
