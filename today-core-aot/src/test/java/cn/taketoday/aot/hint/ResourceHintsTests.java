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

package cn.taketoday.aot.hint;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.DescriptiveResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests for {@link ResourceHints}.
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 */
class ResourceHintsTests {

	private final ResourceHints resourceHints = new ResourceHints();

	@Test
	void registerType() {
		this.resourceHints.registerType(String.class);
		assertThat(this.resourceHints.resourcePatternHints()).singleElement().satisfies(
				patternOf("/", "java", "java/lang", "java/lang/String.class"));
	}

	@Test
	void registerTypeWithNestedType() {
		this.resourceHints.registerType(TypeReference.of(Nested.class));
		assertThat(this.resourceHints.resourcePatternHints()).singleElement().satisfies(
				patternOf("/", "cn", "cn/taketoday", "cn/taketoday/aot", "cn/taketoday/aot/hint",
                "cn/taketoday/aot/hint/ResourceHintsTests$Nested.class"));
	}

	@Test
	void registerTypeWithInnerNestedType() {
		this.resourceHints.registerType(TypeReference.of(Nested.Inner.class));
		assertThat(this.resourceHints.resourcePatternHints()).singleElement().satisfies(
				patternOf("/", "cn", "cn/taketoday", "cn/taketoday/aot", "cn/taketoday/aot/hint",
                "cn/taketoday/aot/hint/ResourceHintsTests$Nested$Inner.class"));
	}

	@Test
	void registerTypeSeveralTimesAddsOnlyOneEntry() {
		this.resourceHints.registerType(String.class);
		this.resourceHints.registerType(TypeReference.of(String.class));
		assertThat(this.resourceHints.resourcePatternHints()).singleElement().satisfies(
				patternOf("/", "java", "java/lang", "java/lang/String.class"));
	}

	@Test
	void registerExactMatches() {
		this.resourceHints.registerPattern("com/example/test.properties");
		this.resourceHints.registerPattern("com/example/another.properties");
		assertThat(this.resourceHints.resourcePatternHints())
				.anySatisfy(patternOf("/", "com", "com/example", "com/example/test.properties"))
				.anySatisfy(patternOf("/", "com", "com/example", "com/example/another.properties"))
				.hasSize(2);
	}

	@Test
	void registerExactMatchesInRootDirectory() {
		this.resourceHints.registerPattern("test.properties");
		this.resourceHints.registerPattern("another.properties");
		assertThat(this.resourceHints.resourcePatternHints())
				.anySatisfy(patternOf("/", "test.properties"))
				.anySatisfy(patternOf("/", "another.properties"))
				.hasSize(2);
	}

	@Test
	void registerRootDirectory() {
		this.resourceHints.registerPattern("/");
		assertThat(this.resourceHints.resourcePatternHints()).singleElement().satisfies(
				patternOf("/"));
	}

	@Test
	void registerRootPattern() {
		this.resourceHints.registerPattern("*.properties");
		assertThat(this.resourceHints.resourcePatternHints()).singleElement().satisfies(
				patternOf("/", "*.properties"));
	}

	@Test
	void registerPattern() {
		this.resourceHints.registerPattern("com/example/*.properties");
		assertThat(this.resourceHints.resourcePatternHints()).singleElement().satisfies(
				patternOf("/", "com", "com/example", "com/example/*.properties"));
	}

	@Test
	void registerPatternWithIncludesAndExcludes() {
		this.resourceHints.registerPattern(resourceHint ->
				resourceHint.includes("com/example/*.properties").excludes("com/example/to-ignore.properties"));
		assertThat(this.resourceHints.resourcePatternHints()).singleElement().satisfies(patternOf(
				List.of("/", "com", "com/example", "com/example/*.properties"),
				List.of("com/example/to-ignore.properties")));
	}

	@Test
	void registerIfPresentRegisterExistingLocation() {
		this.resourceHints.registerPatternIfPresent(null, "META-INF/",
				resourceHint -> resourceHint.includes("com/example/*.properties"));
		assertThat(this.resourceHints.resourcePatternHints()).singleElement().satisfies(
				patternOf("/", "com", "com/example", "com/example/*.properties"));
	}

	@Test
	@SuppressWarnings("unchecked")
	void registerIfPresentIgnoreMissingLocation() {
		Consumer<ResourcePatternHints.Builder> hintBuilder = mock();
		this.resourceHints.registerPatternIfPresent(null, "location/does-not-exist/", hintBuilder);
		assertThat(this.resourceHints.resourcePatternHints()).isEmpty();
		verifyNoInteractions(hintBuilder);
	}

	@Test
	void registerResourceWithUnsupportedResourceType() {
		DescriptiveResource resource = new DescriptiveResource("bogus");
		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.resourceHints.registerResource(resource))
			.withMessage("Resource must be a ClassPathResource that exists: %s", resource);
	}

	@Test
	void registerResourceWithNonexistentClassPathResource() {
		ClassPathResource resource = new ClassPathResource("bogus", getClass());
		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.resourceHints.registerResource(resource))
			.withMessage("Resource must be a ClassPathResource that exists: %s", resource);
	}

	@Test
	void registerResourceWithExistingClassPathResource() {
		String path = "cn/taketoday/aot/hint/support";
		ClassPathResource resource = new ClassPathResource(path);
		this.resourceHints.registerResource(resource);
		assertThat(this.resourceHints.resourcePatternHints()).singleElement().satisfies(
				patternOf("/", "cn", "cn/taketoday", "cn/taketoday/aot", "cn/taketoday/aot/hint", path));
	}

	@Test
	void registerResourceWithExistingRelativeClassPathResource() {
		String path = "cn/taketoday/aot/hint/support";
		ClassPathResource resource = new ClassPathResource("support", RuntimeHints.class);
		this.resourceHints.registerResource(resource);
		assertThat(this.resourceHints.resourcePatternHints()).singleElement().satisfies(
				patternOf("/", "cn", "cn/taketoday", "cn/taketoday/aot", "cn/taketoday/aot/hint", path));
	}

	@Test
	void registerResourceBundle() {
		this.resourceHints.registerResourceBundle("com.example.message");
		assertThat(this.resourceHints.resourceBundleHints()).singleElement()
				.satisfies(resourceBundle("com.example.message"));
	}

	@Test
	void registerResourceBundleSeveralTimesAddsOneEntry() {
		this.resourceHints.registerResourceBundle("com.example.message")
				.registerResourceBundle("com.example.message");
		assertThat(this.resourceHints.resourceBundleHints()).singleElement()
				.satisfies(resourceBundle("com.example.message"));
	}


	private Consumer<ResourcePatternHints> patternOf(String... includes) {
		return patternOf(Arrays.asList(includes), Collections.emptyList());
	}

	private Consumer<ResourceBundleHint> resourceBundle(String baseName) {
		return resourceBundleHint -> assertThat(resourceBundleHint.getBaseName()).isEqualTo(baseName);
	}

	private Consumer<ResourcePatternHints> patternOf(List<String> includes, List<String> excludes) {
		return pattern -> {
			assertThat(pattern.getIncludes()).map(ResourcePatternHint::getPattern).containsExactlyInAnyOrderElementsOf(includes);
			assertThat(pattern.getExcludes()).map(ResourcePatternHint::getPattern).containsExactlyElementsOf(excludes);
		};
	}

	static class Nested {

		class Inner {
		}
	}

}
