/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.webmvc.config;

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
import infra.webmvc.config.WebResourcesRuntimeHints;

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