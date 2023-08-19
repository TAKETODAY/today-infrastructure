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

package cn.taketoday.aot.hint.support;

import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import cn.taketoday.aot.hint.ResourceHints;
import cn.taketoday.aot.hint.ResourcePatternHint;
import cn.taketoday.aot.hint.ResourcePatternHints;
import cn.taketoday.aot.hint.support.FilePatternResourceHintsRegistrar.Builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link FilePatternResourceHintsRegistrar}.
 *
 * @author Stephane Nicoll
 */
class FilePatternResourceHintsRegistrarTests {

  private final ResourceHints hints = new ResourceHints();

  @Test
  void configureWithNoClasspathLocation() {
    assertThatIllegalArgumentException().isThrownBy(FilePatternResourceHintsRegistrar::forClassPathLocations)
            .withMessageContaining("At least one classpath location should be specified");
  }

  @Test
  void configureWithInvalidFilePrefix() {
    Builder builder = FilePatternResourceHintsRegistrar.forClassPathLocations("");
    assertThatIllegalArgumentException().isThrownBy(() -> builder.withFilePrefixes("test*"))
            .withMessageContaining("cannot contain '*'");
  }

  @Test
  void configureWithInvalidFileExtension() {
    Builder builder = FilePatternResourceHintsRegistrar.forClassPathLocations("");
    assertThatIllegalArgumentException().isThrownBy(() -> builder.withFileExtensions("txt"))
            .withMessageContaining("should start with '.'");
  }

  @Test
  void registerWithSinglePattern() {
    FilePatternResourceHintsRegistrar.forClassPathLocations("")
            .withFilePrefixes("test").withFileExtensions(".txt")
            .registerHints(this.hints, null);
    assertThat(this.hints.resourcePatternHints()).singleElement()
            .satisfies(includes("/", "test*.txt"));
  }

  @Test
  void registerWithMultipleFilePrefixes() {
    FilePatternResourceHintsRegistrar.forClassPathLocations("")
            .withFilePrefixes("test").withFilePrefixes("another")
            .withFileExtensions(".txt")
            .registerHints(this.hints, null);
    assertThat(this.hints.resourcePatternHints()).singleElement()
            .satisfies(includes("/", "test*.txt", "another*.txt"));
  }

  @Test
  void registerWithMultipleClasspathLocations() {
    FilePatternResourceHintsRegistrar.forClassPathLocations("").withClasspathLocations("META-INF")
            .withFilePrefixes("test").withFileExtensions(".txt")
            .registerHints(this.hints, null);
    assertThat(this.hints.resourcePatternHints()).singleElement()
            .satisfies(includes("/", "test*.txt", "META-INF", "META-INF/test*.txt"));
  }

  @Test
  void registerWithMultipleFileExtensions() {
    FilePatternResourceHintsRegistrar.forClassPathLocations("")
            .withFilePrefixes("test").withFileExtensions(".txt").withFileExtensions(".conf")
            .registerHints(this.hints, null);
    assertThat(this.hints.resourcePatternHints()).singleElement()
            .satisfies(includes("/", "test*.txt", "test*.conf"));
  }

  @Test
  void registerWithClasspathLocationWithoutTrailingSlash() {
    FilePatternResourceHintsRegistrar.forClassPathLocations("META-INF")
            .withFilePrefixes("test").withFileExtensions(".txt")
            .registerHints(this.hints, null);
    assertThat(this.hints.resourcePatternHints()).singleElement()
            .satisfies(includes("/", "META-INF", "META-INF/test*.txt"));
  }

  @Test
  void registerWithClasspathLocationWithLeadingSlash() {
    FilePatternResourceHintsRegistrar.forClassPathLocations("/")
            .withFilePrefixes("test").withFileExtensions(".txt")
            .registerHints(this.hints, null);
    assertThat(this.hints.resourcePatternHints()).singleElement()
            .satisfies(includes("/", "test*.txt"));
  }

  @Test
  void registerWithClasspathLocationUsingResourceClasspathPrefix() {
    FilePatternResourceHintsRegistrar.forClassPathLocations("classpath:META-INF")
            .withFilePrefixes("test").withFileExtensions(".txt")
            .registerHints(this.hints, null);
    assertThat(this.hints.resourcePatternHints()).singleElement()
            .satisfies(includes("/", "META-INF", "META-INF/test*.txt"));
  }

  @Test
  void registerWithClasspathLocationUsingResourceClasspathPrefixAndTrailingSlash() {
    FilePatternResourceHintsRegistrar.forClassPathLocations("classpath:/META-INF")
            .withFilePrefixes("test").withFileExtensions(".txt")
            .registerHints(this.hints, null);
    assertThat(this.hints.resourcePatternHints()).singleElement()
            .satisfies(includes("/", "META-INF", "META-INF/test*.txt"));
  }

  @Test
  void registerWithNonExistingLocationDoesNotRegisterHint() {
    FilePatternResourceHintsRegistrar.forClassPathLocations("does-not-exist/")
            .withClasspathLocations("another-does-not-exist/")
            .withFilePrefixes("test").withFileExtensions(".txt")
            .registerHints(this.hints, null);
    assertThat(this.hints.resourcePatternHints()).isEmpty();
  }

  private Consumer<ResourcePatternHints> includes(String... patterns) {
    return hint -> {
      assertThat(hint.getIncludes().stream().map(ResourcePatternHint::getPattern))
              .containsExactlyInAnyOrder(patterns);
      assertThat(hint.getExcludes()).isEmpty();
    };
  }

}
