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

package cn.taketoday.aot.hint.predicate;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.function.Predicate;

import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.TypeReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ReflectionHintsPredicates}.
 *
 * @author Brian Clozel
 * @author Sam Brannen
 */
class ResourceHintsPredicatesTests {

	private final ResourceHintsPredicates resources = new ResourceHintsPredicates();

	private final RuntimeHints runtimeHints = new RuntimeHints();


	@Test
	void resourcePatternMatchesResourceName() {
		this.runtimeHints.resources().registerPattern("test/*");
		assertPredicateMatches(resources.forResource("/test/spring.properties"));
	}

	@Test
	void resourcePatternDoesNotMatchResourceName() {
		this.runtimeHints.resources().registerPattern("test/spring.*");
		assertPredicateDoesNotMatch(resources.forResource("/test/other.properties"));
	}

	@Test
	void resourcePatternMatchesTypeAndResourceName() {
		this.runtimeHints.resources().registerPattern("cn/taketoday/aot/hint/predicate/spring.*");
		assertPredicateMatches(resources.forResource(TypeReference.of(getClass()), "spring.properties"));
	}

	@Test
	void resourcePatternMatchesTypeAndAbsoluteResourceName() {
		this.runtimeHints.resources().registerPattern("spring.*");
		assertPredicateMatches(resources.forResource(TypeReference.of(getClass()), "/spring.properties"));
	}

	@Test
	void resourcePatternMatchesTypeInDefaultPackageAndResourceName() {
		this.runtimeHints.resources().registerPattern("spring.*");
		assertPredicateMatches(resources.forResource(TypeReference.of("DummyClass"), "spring.properties"));
	}

	@Test
	void resourcePatternMatchesTypeInDefaultPackageAndAbsoluteResourceName() {
		this.runtimeHints.resources().registerPattern("spring.*");
		assertPredicateMatches(resources.forResource(TypeReference.of("DummyClass"), "/spring.properties"));
	}

	@Test
	void resourcePatternDoesNotMatchTypeAndResourceName() {
		this.runtimeHints.resources().registerPattern("spring.*");
		assertPredicateDoesNotMatch(resources.forResource(TypeReference.of(getClass()), "spring.properties"));
	}

	@Test
	void resourceBundleMatchesBundleName() {
		this.runtimeHints.resources().registerResourceBundle("spring");
		assertPredicateMatches(resources.forBundle("spring"));
	}

	@Test
	void resourceBundleDoesNotMatchBundleName() {
		this.runtimeHints.resources().registerResourceBundle("spring");
		assertPredicateDoesNotMatch(resources.forBundle("other"));
	}


	private void assertPredicateMatches(Predicate<RuntimeHints> predicate) {
		Assertions.assertThat(predicate).accepts(this.runtimeHints);
	}

	private void assertPredicateDoesNotMatch(Predicate<RuntimeHints> predicate) {
		Assertions.assertThat(predicate).rejects(this.runtimeHints);
	}

}
