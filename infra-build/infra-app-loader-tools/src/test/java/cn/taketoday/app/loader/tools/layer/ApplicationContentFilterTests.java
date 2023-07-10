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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ApplicationContentFilter}.
 *
 * @author Madhura Bhave
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class ApplicationContentFilterTests {

	@Test
	void createWhenPatternIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ApplicationContentFilter(null))
			.withMessage("Pattern must not be empty");
	}

	@Test
	void createWhenPatternIsEmptyThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ApplicationContentFilter(""))
			.withMessage("Pattern must not be empty");
	}

	@Test
	void matchesWhenWildcardPatternMatchesReturnsTrue() {
		ApplicationContentFilter filter = new ApplicationContentFilter("META-INF/**");
		assertThat(filter.matches("META-INF/resources/application.yml")).isTrue();
	}

	@Test
	void matchesWhenWildcardPatternDoesNotMatchReturnsFalse() {
		ApplicationContentFilter filter = new ApplicationContentFilter("META-INF/**");
		assertThat(filter.matches("src/main/resources/application.yml")).isFalse();
	}

}
