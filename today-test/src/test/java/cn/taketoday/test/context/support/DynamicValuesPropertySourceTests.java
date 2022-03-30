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

package cn.taketoday.test.context.support;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.function.Supplier;

import cn.taketoday.test.context.support.DynamicValuesPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DynamicValuesPropertySource}.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 */
class DynamicValuesPropertySourceTests {

	@SuppressWarnings("serial")
	private final DynamicValuesPropertySource source = new DynamicValuesPropertySource("test",
		new HashMap<String, Supplier<Object>>() {{
			put("a", () -> "A");
			put("b", () -> "B");
		}});


	@Test
	void getPropertyReturnsSuppliedProperty() throws Exception {
		assertThat(this.source.getProperty("a")).isEqualTo("A");
		assertThat(this.source.getProperty("b")).isEqualTo("B");
	}

	@Test
	void getPropertyWhenMissingReturnsNull() throws Exception {
		assertThat(this.source.getProperty("c")).isNull();
	}

	@Test
	void containsPropertyWhenPresentReturnsTrue() {
		assertThat(this.source.containsProperty("a")).isTrue();
		assertThat(this.source.containsProperty("b")).isTrue();
	}

	@Test
	void containsPropertyWhenMissingReturnsFalse() {
		assertThat(this.source.containsProperty("c")).isFalse();
	}

	@Test
	void getPropertyNamesReturnsNames() {
		assertThat(this.source.getPropertyNames()).containsExactly("a", "b");
	}

}
