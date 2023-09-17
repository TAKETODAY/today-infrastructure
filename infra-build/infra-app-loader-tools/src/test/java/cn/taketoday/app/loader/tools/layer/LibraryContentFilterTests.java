/*
 * Copyright 2017 - 2023 the original author or authors.
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

import cn.taketoday.app.loader.tools.Library;
import cn.taketoday.app.loader.tools.LibraryCoordinates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link LibraryContentFilter}.
 *
 * @author Madhura Bhave
 * @author Scott Frederick
 * @author Phillip Webb
 */
class LibraryContentFilterTests {

	@Test
	void createWhenCoordinatesPatternIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new LibraryContentFilter(null))
			.withMessage("CoordinatesPattern must not be empty");
	}

	@Test
	void createWhenCoordinatesPatternIsEmptyThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new LibraryContentFilter(""))
			.withMessage("CoordinatesPattern must not be empty");
	}

	@Test
	void matchesWhenGroupIdIsNullAndToMatchHasWildcardReturnsTrue() {
		LibraryContentFilter filter = new LibraryContentFilter("*:*");
		assertThat(filter.matches(mockLibrary(null, null, null))).isTrue();
	}

	@Test
	void matchesWhenArtifactIdIsNullAndToMatchHasWildcardReturnsTrue() {
		LibraryContentFilter filter = new LibraryContentFilter("org.acme:*");
		assertThat(filter.matches(mockLibrary("org.acme", null, null))).isTrue();
	}

	@Test
	void matchesWhenVersionIsNullAndToMatchHasWildcardReturnsTrue() {
		LibraryContentFilter filter = new LibraryContentFilter("org.acme:something:*");
		assertThat(filter.matches(mockLibrary("org.acme", "something", null))).isTrue();
	}

	@Test
	void matchesWhenGroupIdDoesNotMatchReturnsFalse() {
		LibraryContentFilter filter = new LibraryContentFilter("org.acme:*");
		assertThat(filter.matches(mockLibrary("other.foo", null, null))).isFalse();
	}

	@Test
	void matchesWhenWhenArtifactIdDoesNotMatchReturnsFalse() {
		LibraryContentFilter filter = new LibraryContentFilter("org.acme:test:*");
		assertThat(filter.matches(mockLibrary("org.acme", "other", null))).isFalse();
	}

	@Test
	void matchesWhenArtifactIdMatchesReturnsTrue() {
		LibraryContentFilter filter = new LibraryContentFilter("org.acme:test:*");
		assertThat(filter.matches(mockLibrary("org.acme", "test", null))).isTrue();
	}

	@Test
	void matchesWhenVersionDoesNotMatchReturnsFalse() {
		LibraryContentFilter filter = new LibraryContentFilter("org.acme:test:*SNAPSHOT");
		assertThat(filter.matches(mockLibrary("org.acme", "test", "1.0.0"))).isFalse();
	}

	@Test
	void matchesWhenVersionMatchesReturnsTrue() {
		LibraryContentFilter filter = new LibraryContentFilter("org.acme:test:*SNAPSHOT");
		assertThat(filter.matches(mockLibrary("org.acme", "test", "1.0.0-SNAPSHOT"))).isTrue();
	}

	private Library mockLibrary(String groupId, String artifactId, String version) {
		return mockLibrary(LibraryCoordinates.of(groupId, artifactId, version));
	}

	private Library mockLibrary(LibraryCoordinates coordinates) {
		Library library = mock(Library.class);
		given(library.getCoordinates()).willReturn(coordinates);
		return library;
	}

}
