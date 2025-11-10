/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.annotation.config.web;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import infra.aot.hint.ResourcePatternHint;
import infra.aot.hint.ResourcePatternHints;
import infra.aot.hint.RuntimeHints;
import infra.test.classpath.resources.WithResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/10 15:29
 */
@WithResource(name = "web/custom-resource.txt")
class WebResourcesRuntimeHintsTests {

  @Test
  void registerHintsWithAllLocations() {
    RuntimeHints hints = register(
            new TestClassLoader(List.of("META-INF/resources/", "resources/", "static/", "public/")));
    assertThat(hints.resources().resourcePatternHints()).singleElement()
            .satisfies(include("META-INF/resources/**", "resources/**", "static/**", "public/**"));
  }

  @Test
  void registerHintsWithOnlyStaticLocations() {
    RuntimeHints hints = register(new TestClassLoader(List.of("static/")));
    assertThat(hints.resources().resourcePatternHints()).singleElement().satisfies(include("static/**"));
  }

  @Test
  void registerHintsWithNoLocation() {
    RuntimeHints hints = register(new TestClassLoader(Collections.emptyList()));
    assertThat(hints.resources().resourcePatternHints()).isEmpty();
  }

  private RuntimeHints register(ClassLoader classLoader) {
    RuntimeHints hints = new RuntimeHints();
    WebResourcesRuntimeHints registrar = new WebResourcesRuntimeHints();
    registrar.registerHints(hints, classLoader);
    return hints;
  }

  private Consumer<ResourcePatternHints> include(String... patterns) {
    return (hint) -> assertThat(hint.getIncludes()).map(ResourcePatternHint::getPattern).contains(patterns);
  }

  private static class TestClassLoader extends ClassLoader {

    private final List<String> availableResources;

    TestClassLoader(List<String> availableResources) {
      super(Thread.currentThread().getContextClassLoader());
      this.availableResources = availableResources;
    }

    @Override
    public @Nullable URL getResource(String name) {
      return (this.availableResources.contains(name)) ? super.getResource("web/custom-resource.txt") : null;
    }

  }

}