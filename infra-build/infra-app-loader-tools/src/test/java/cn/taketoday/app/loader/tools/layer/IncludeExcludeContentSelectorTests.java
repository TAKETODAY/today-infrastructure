/*
 * Copyright 2012 - 2023 the original author or authors.
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

package cn.taketoday.app.loader.tools.layer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import cn.taketoday.app.loader.tools.Layer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link IncludeExcludeContentSelector}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class IncludeExcludeContentSelectorTests {

	private static final Layer LAYER = new Layer("test");

	@Test
	void createWhenLayerIsNullThrowsException() {
		assertThatIllegalArgumentException()
			.isThrownBy(
					() -> new IncludeExcludeContentSelector<>(null, Collections.emptyList(), Collections.emptyList()))
			.withMessage("Layer must not be null");
	}

	@Test
	void createWhenFactoryIsNullThrowsException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new IncludeExcludeContentSelector<>(LAYER, null, null, null))
			.withMessage("FilterFactory must not be null");
	}

	@Test
	void getLayerReturnsLayer() {
		IncludeExcludeContentSelector<?> selector = new IncludeExcludeContentSelector<>(LAYER, null, null);
		assertThat(selector.getLayer()).isEqualTo(LAYER);
	}

	@Test
	void containsWhenEmptyIncludesAndEmptyExcludesReturnsTrue() {
		List<String> includes = Arrays.asList();
		List<String> excludes = Arrays.asList();
		IncludeExcludeContentSelector<String> selector = new IncludeExcludeContentSelector<>(LAYER, includes, excludes,
				TestContentsFilter::new);
		assertThat(selector.contains("A")).isTrue();
	}

	@Test
	void containsWhenNullIncludesAndEmptyExcludesReturnsTrue() {
		List<String> includes = null;
		List<String> excludes = null;
		IncludeExcludeContentSelector<String> selector = new IncludeExcludeContentSelector<>(LAYER, includes, excludes,
				TestContentsFilter::new);
		assertThat(selector.contains("A")).isTrue();
	}

	@Test
	void containsWhenEmptyIncludesAndNotExcludedReturnsTrue() {
		List<String> includes = Arrays.asList();
		List<String> excludes = Arrays.asList("B");
		IncludeExcludeContentSelector<String> selector = new IncludeExcludeContentSelector<>(LAYER, includes, excludes,
				TestContentsFilter::new);
		assertThat(selector.contains("A")).isTrue();
	}

	@Test
	void containsWhenEmptyIncludesAndExcludedReturnsFalse() {
		List<String> includes = Arrays.asList();
		List<String> excludes = Arrays.asList("A");
		IncludeExcludeContentSelector<String> selector = new IncludeExcludeContentSelector<>(LAYER, includes, excludes,
				TestContentsFilter::new);
		assertThat(selector.contains("A")).isFalse();
	}

	@Test
	void containsWhenIncludedAndEmptyExcludesReturnsTrue() {
		List<String> includes = Arrays.asList("A", "B");
		List<String> excludes = Arrays.asList();
		IncludeExcludeContentSelector<String> selector = new IncludeExcludeContentSelector<>(LAYER, includes, excludes,
				TestContentsFilter::new);
		assertThat(selector.contains("B")).isTrue();
	}

	@Test
	void containsWhenIncludedAndNotExcludedReturnsTrue() {
		List<String> includes = Arrays.asList("A", "B");
		List<String> excludes = Arrays.asList("C", "D");
		IncludeExcludeContentSelector<String> selector = new IncludeExcludeContentSelector<>(LAYER, includes, excludes,
				TestContentsFilter::new);
		assertThat(selector.contains("B")).isTrue();
	}

	@Test
	void containsWhenIncludedAndExcludedReturnsFalse() {
		List<String> includes = Arrays.asList("A", "B");
		List<String> excludes = Arrays.asList("C", "D");
		IncludeExcludeContentSelector<String> selector = new IncludeExcludeContentSelector<>(LAYER, includes, excludes,
				TestContentsFilter::new);
		assertThat(selector.contains("C")).isFalse();
	}

	/**
	 * {@link ContentFilter} used for testing.
	 */
	static class TestContentsFilter implements ContentFilter<String> {

		private final String match;

		TestContentsFilter(String match) {
			this.match = match;
		}

		@Override
		public boolean matches(String item) {
			return this.match.equals(item);
		}

	}

}
