/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.app.logging.logback;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggerContextVO;
import ch.qos.logback.classic.spi.LoggingEvent;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EnclosedInSquareBracketsConverter}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class EnclosedInSquareBracketsConverterTests {

	private final EnclosedInSquareBracketsConverter converter;

	private final LoggingEvent event = new LoggingEvent();

	EnclosedInSquareBracketsConverterTests() {
		this.converter = new EnclosedInSquareBracketsConverter();
		this.converter.setContext(new LoggerContext());
		this.event.setLoggerContextRemoteView(
				new LoggerContextVO("test", Collections.emptyMap(), System.currentTimeMillis()));
	}

	@Test
	void transformWhenNull() {
		assertThat(this.converter.transform(this.event, null)).isEqualTo("");
	}

	@Test
	void transformWhenEmpty() {
		assertThat(this.converter.transform(this.event, "")).isEqualTo("");
	}

	@Test
	void transformWhenName() {
		assertThat(this.converter.transform(this.event, "My Application")).isEqualTo("[My Application] ");
	}

	@Test
	void transformWhenEmptyFromFirstOption() {
		withLoggedApplicationName("spring", null, () -> {
			this.converter.setOptionList(List.of("spring"));
			this.converter.start();
			String converted = this.converter.convert(this.event);
			assertThat(converted).isEqualTo("");
		});
	}

	@Test
	void transformWhenNameFromFirstOption() {
		withLoggedApplicationName("spring", "boot", () -> {
			this.converter.setOptionList(List.of("spring"));
			this.converter.start();
			String converted = this.converter.convert(this.event);
			assertThat(converted).isEqualTo("[boot] ");
		});
	}

	private void withLoggedApplicationName(String name, String value, Runnable action) {
		if (value == null) {
			System.clearProperty(name);
		}
		else {
			System.setProperty(name, value);
		}
		try {
			action.run();
		}
		finally {
			System.clearProperty(name);
		}
	}

}
