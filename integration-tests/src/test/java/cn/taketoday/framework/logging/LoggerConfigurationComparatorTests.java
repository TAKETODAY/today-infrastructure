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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LoggerConfigurationComparator}.
 *
 * @author Ben Hale
 */
class LoggerConfigurationComparatorTests {

	private final LoggerConfigurationComparator comparator = new LoggerConfigurationComparator("ROOT");

	@Test
	void rootLoggerFirst() {
		LoggerConfiguration first = new LoggerConfiguration("ROOT", null, LogLevel.OFF);
		LoggerConfiguration second = new LoggerConfiguration("alpha", null, LogLevel.OFF);
		assertThat(this.comparator.compare(first, second)).isLessThan(0);
	}

	@Test
	void rootLoggerSecond() {
		LoggerConfiguration first = new LoggerConfiguration("alpha", null, LogLevel.OFF);
		LoggerConfiguration second = new LoggerConfiguration("ROOT", null, LogLevel.OFF);
		assertThat(this.comparator.compare(first, second)).isGreaterThan(0);
	}

	@Test
	void rootLoggerFirstEmpty() {
		LoggerConfiguration first = new LoggerConfiguration("ROOT", null, LogLevel.OFF);
		LoggerConfiguration second = new LoggerConfiguration("", null, LogLevel.OFF);
		assertThat(this.comparator.compare(first, second)).isLessThan(0);
	}

	@Test
	void rootLoggerSecondEmpty() {
		LoggerConfiguration first = new LoggerConfiguration("", null, LogLevel.OFF);
		LoggerConfiguration second = new LoggerConfiguration("ROOT", null, LogLevel.OFF);
		assertThat(this.comparator.compare(first, second)).isGreaterThan(0);
	}

	@Test
	void lexicalFirst() {
		LoggerConfiguration first = new LoggerConfiguration("alpha", null, LogLevel.OFF);
		LoggerConfiguration second = new LoggerConfiguration("bravo", null, LogLevel.OFF);
		assertThat(this.comparator.compare(first, second)).isLessThan(0);
	}

	@Test
	void lexicalSecond() {
		LoggerConfiguration first = new LoggerConfiguration("bravo", null, LogLevel.OFF);
		LoggerConfiguration second = new LoggerConfiguration("alpha", null, LogLevel.OFF);
		assertThat(this.comparator.compare(first, second)).isGreaterThan(0);
	}

	@Test
	void lexicalEqual() {
		LoggerConfiguration first = new LoggerConfiguration("alpha", null, LogLevel.OFF);
		LoggerConfiguration second = new LoggerConfiguration("alpha", null, LogLevel.OFF);
		assertThat(this.comparator.compare(first, second)).isZero();
	}

}
