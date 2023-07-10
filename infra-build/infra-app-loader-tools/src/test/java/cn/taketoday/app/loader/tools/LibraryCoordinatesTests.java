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

package cn.taketoday.app.loader.tools;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link LibraryCoordinates}.
 *
 * @author Phillip Webb
 */
class LibraryCoordinatesTests {

	@Test
	void ofCreateLibraryCoordinates() {
		LibraryCoordinates coordinates = LibraryCoordinates.of("g", "a", "v");
		assertThat(coordinates.getGroupId()).isEqualTo("g");
		assertThat(coordinates.getArtifactId()).isEqualTo("a");
		assertThat(coordinates.getVersion()).isEqualTo("v");
		assertThat(coordinates).hasToString("g:a:v");
	}

	@Test
	void toStandardNotationStringWhenCoordinatesAreNull() {
		assertThat(LibraryCoordinates.toStandardNotationString(null)).isEqualTo("::");
	}

	@Test
	void toStandardNotationStringWhenCoordinatesElementsNull() {
		assertThat(LibraryCoordinates.toStandardNotationString(mock(LibraryCoordinates.class))).isEqualTo("::");
	}

	@Test
	void toStandardNotationString() {
		LibraryCoordinates coordinates = mock(LibraryCoordinates.class);
		given(coordinates.getGroupId()).willReturn("a");
		given(coordinates.getArtifactId()).willReturn("b");
		given(coordinates.getVersion()).willReturn("c");
		assertThat(LibraryCoordinates.toStandardNotationString(coordinates)).isEqualTo("a:b:c");
	}

}
