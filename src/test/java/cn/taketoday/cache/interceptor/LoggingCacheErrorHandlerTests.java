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

import org.junit.jupiter.api.Test;

import cn.taketoday.cache.support.NoOpCache;
import cn.taketoday.logging.Logger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link LoggingCacheErrorHandler}.
 *
 * @author Adam Ostrožlík
 * @author Stephane Nicoll
 */
public class LoggingCacheErrorHandlerTests {

	@Test
	void handleGetCacheErrorLogsAppropriateMessage() {
		Logger logger = mock(Logger.class);
		LoggingCacheErrorHandler handler = new LoggingCacheErrorHandler(logger, false);
		handler.handleCacheGetError(new RuntimeException(), new NoOpCache("NOOP"), "key");
		verify(logger).warn("Cache 'NOOP' failed to get entry with key 'key'");
	}

	@Test
	void handlePutCacheErrorLogsAppropriateMessage() {
		Logger logger = mock(Logger.class);
		LoggingCacheErrorHandler handler = new LoggingCacheErrorHandler(logger, false);
		handler.handleCachePutError(new RuntimeException(), new NoOpCache("NOOP"), "key", new Object());
		verify(logger).warn("Cache 'NOOP' failed to put entry with key 'key'");
	}

	@Test
	void handleEvictCacheErrorLogsAppropriateMessage() {
		Logger logger = mock(Logger.class);
		LoggingCacheErrorHandler handler = new LoggingCacheErrorHandler(logger, false);
		handler.handleCacheEvictError(new RuntimeException(), new NoOpCache("NOOP"), "key");
		verify(logger).warn("Cache 'NOOP' failed to evict entry with key 'key'");
	}

	@Test
	void handleClearErrorLogsAppropriateMessage() {
		Logger logger = mock(Logger.class);
		LoggingCacheErrorHandler handler = new LoggingCacheErrorHandler(logger, false);
		handler.handleCacheClearError(new RuntimeException(), new NoOpCache("NOOP"));
		verify(logger).warn("Cache 'NOOP' failed to clear entries");
	}

	@Test
	void handleCacheErrorWithStacktrace() {
		Logger logger = mock(Logger.class);
		LoggingCacheErrorHandler handler = new LoggingCacheErrorHandler(logger, true);
		RuntimeException exception = new RuntimeException();
		handler.handleCacheGetError(exception, new NoOpCache("NOOP"), "key");
		verify(logger).warn("Cache 'NOOP' failed to get entry with key 'key'", exception);
	}

}
