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

package cn.taketoday.framework.logging.logback;

import org.junit.jupiter.api.Test;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ExtendedWhitespaceThrowableProxyConverter}.
 *
 * @author Phillip Webb
 * @author Chanwit Kaewkasi
 */
class ExtendedWhitespaceThrowableProxyConverterTests {

	private final ExtendedWhitespaceThrowableProxyConverter converter = new ExtendedWhitespaceThrowableProxyConverter();

	private final LoggingEvent event = new LoggingEvent();

	@Test
	void noStackTrace() {
		String s = this.converter.convert(this.event);
		assertThat(s).isEmpty();
	}

	@Test
	void withStackTrace() {
		this.event.setThrowableProxy(new ThrowableProxy(new RuntimeException()));
		String s = this.converter.convert(this.event);
		assertThat(s).startsWith(System.lineSeparator()).endsWith(System.lineSeparator());
	}

}
