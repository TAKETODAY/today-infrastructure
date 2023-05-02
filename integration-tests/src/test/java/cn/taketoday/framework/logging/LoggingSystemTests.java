/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.framework.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.framework.logging.LoggingSystem.NoOpLoggingSystem;
import cn.taketoday.framework.logging.logback.LogbackLoggingSystem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link LoggingSystem}.
 *
 * @author Andy Wilkinson
 */
class LoggingSystemTests {

	@AfterEach
	void clearSystemProperty() {
		System.clearProperty(LoggingSystem.SYSTEM_PROPERTY);
	}

	@Test
	void logbackIsTheDefaultLoggingSystem() {
		assertThat(LoggingSystem.get(getClass().getClassLoader())).isInstanceOf(LogbackLoggingSystem.class);
	}

	@Test
	void loggingSystemCanBeDisabled() {
		System.setProperty(LoggingSystem.SYSTEM_PROPERTY, LoggingSystem.NONE);
		LoggingSystem loggingSystem = LoggingSystem.get(getClass().getClassLoader());
		assertThat(loggingSystem).isInstanceOf(NoOpLoggingSystem.class);
	}

	@Test
	void getLoggerConfigurationIsUnsupported() {
		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> new StubLoggingSystem().getLoggerConfiguration("test-logger-name"));
	}

	@Test
	void listLoggerConfigurationsIsUnsupported() {
		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> new StubLoggingSystem().getLoggerConfigurations());
	}

	private static final class StubLoggingSystem extends LoggingSystem {

		@Override
		public void beforeInitialize() {
			// Stub implementation
		}

		@Override
		public void setLogLevel(String loggerName, LogLevel level) {
			// Stub implementation
		}

	}

}
