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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.cache.Cache;
import cn.taketoday.cache.support.NoOpCache;
import cn.taketoday.logging.Logger;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link LoggingCacheErrorHandler}.
 *
 * @author Adam Ostrožlík
 * @author Stephane Nicoll
 */
public class LoggingCacheErrorHandlerTests {

  private static final Cache CACHE = new NoOpCache("NOOP");

  private static final String KEY = "enigma";

  private final Logger logger = mock(Logger.class);

  private LoggingCacheErrorHandler handler = new LoggingCacheErrorHandler(this.logger, false);

  @BeforeEach
  void setUp() {
    given(this.logger.isWarnEnabled()).willReturn(true);
  }

  @Test
  void handleGetCacheErrorLogsAppropriateMessage() {
    this.handler.handleCacheGetError(new RuntimeException(), CACHE, KEY);
    verify(this.logger).warn("Cache 'NOOP' failed to get entry with key 'enigma'");
  }

  @Test
  void handlePutCacheErrorLogsAppropriateMessage() {
    this.handler.handleCachePutError(new RuntimeException(), CACHE, KEY, null);
    verify(this.logger).warn("Cache 'NOOP' failed to put entry with key 'enigma'");
  }

  @Test
  void handleEvictCacheErrorLogsAppropriateMessage() {
    this.handler.handleCacheEvictError(new RuntimeException(), CACHE, KEY);
    verify(this.logger).warn("Cache 'NOOP' failed to evict entry with key 'enigma'");
  }

  @Test
  void handleClearErrorLogsAppropriateMessage() {
    this.handler.handleCacheClearError(new RuntimeException(), CACHE);
    verify(this.logger).warn("Cache 'NOOP' failed to clear entries");
  }

  @Test
  void handleGetCacheErrorWithStackTraceLoggingEnabled() {
    this.handler = new LoggingCacheErrorHandler(this.logger, true);
    RuntimeException exception = new RuntimeException();
    this.handler.handleCacheGetError(exception, CACHE, KEY);
    verify(this.logger).warn("Cache 'NOOP' failed to get entry with key 'enigma'", exception);
  }

  @Test
  void constructorWithLoggerName() {
    assertThatCode(() -> new LoggingCacheErrorHandler("org.apache.commons.logging.Log", true))
            .doesNotThrowAnyException();
  }

}
