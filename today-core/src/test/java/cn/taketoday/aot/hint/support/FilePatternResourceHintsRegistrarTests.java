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

package cn.taketoday.aot.hint.support;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import cn.taketoday.aot.hint.ResourceHints;
import cn.taketoday.aot.hint.ResourcePatternHint;
import cn.taketoday.aot.hint.ResourcePatternHints;

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
  void createWithInvalidName() {
    assertThatIllegalArgumentException().isThrownBy(() -> new FilePatternResourceHintsRegistrar(
                    List.of("test*"), List.of(""), List.of(".txt")))
            .withMessageContaining("cannot contain '*'");
  }

  @Test
  void createWithInvalidExtension() {
    assertThatIllegalArgumentException().isThrownBy(() -> new FilePatternResourceHintsRegistrar(
                    List.of("test"), List.of(""), List.of("txt")))
            .withMessageContaining("should start with '.'");
  }

  @Test
  void registerWithSinglePattern() {
    new FilePatternResourceHintsRegistrar(List.of("test"), List.of(""), List.of(".txt"))
            .registerHints(this.hints, null);
    assertThat(this.hints.resourcePatternHints()).singleElement()
            .satisfies(includes("/", "test*.txt"));
  }

  @Test
  void registerWithMultipleNames() {
    new FilePatternResourceHintsRegistrar(List.of("test", "another"), List.of(""), List.of(".txt"))
            .registerHints(this.hints, null);
    assertThat(this.hints.resourcePatternHints()).singleElement()
            .satisfies(includes("/", "test*.txt", "another*.txt"));
  }

  @Test
  void registerWithMultipleLocations() {
    new FilePatternResourceHintsRegistrar(List.of("test"), List.of("", "META-INF"), List.of(".txt"))
            .registerHints(this.hints, null);
    assertThat(this.hints.resourcePatternHints()).singleElement()
            .satisfies(includes("/", "test*.txt", "META-INF", "META-INF/test*.txt"));
  }

  @Test
  void registerWithMultipleExtensions() {
    new FilePatternResourceHintsRegistrar(List.of("test"), List.of(""), List.of(".txt", ".conf"))
            .registerHints(this.hints, null);
    assertThat(this.hints.resourcePatternHints()).singleElement()
            .satisfies(includes("/", "test*.txt", "test*.conf"));
  }

  @Test
  void registerWithLocationWithoutTrailingSlash() {
    new FilePatternResourceHintsRegistrar(List.of("test"), List.of("META-INF"), List.of(".txt"))
            .registerHints(this.hints, null);
    assertThat(this.hints.resourcePatternHints()).singleElement()
            .satisfies(includes("/", "META-INF", "META-INF/test*.txt"));
  }

  @Test
  void registerWithLocationWithLeadingSlash() {
    new FilePatternResourceHintsRegistrar(List.of("test"), List.of("/"), List.of(".txt"))
            .registerHints(this.hints, null);
    assertThat(this.hints.resourcePatternHints()).singleElement()
            .satisfies(includes("/", "test*.txt"));
  }

  @Test
  void registerWithLocationUsingResourceClasspathPrefix() {
    new FilePatternResourceHintsRegistrar(List.of("test"), List.of("classpath:META-INF"), List.of(".txt"))
            .registerHints(this.hints, null);
    assertThat(this.hints.resourcePatternHints()).singleElement()
            .satisfies(includes("/", "META-INF", "META-INF/test*.txt"));
  }

  @Test
  void registerWithLocationUsingResourceClasspathPrefixAndTrailingSlash() {
    new FilePatternResourceHintsRegistrar(List.of("test"), List.of("classpath:/META-INF"), List.of(".txt"))
            .registerHints(this.hints, null);
    assertThat(this.hints.resourcePatternHints()).singleElement()
            .satisfies(includes("/", "META-INF", "META-INF/test*.txt"));
  }

  @Test
  void registerWithNonExistingLocationDoesNotRegisterHint() {
    new FilePatternResourceHintsRegistrar(List.of("test"),
            List.of("does-not-exist/", "another-does-not-exist/"),
            List.of(".txt")).registerHints(this.hints, null);
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
