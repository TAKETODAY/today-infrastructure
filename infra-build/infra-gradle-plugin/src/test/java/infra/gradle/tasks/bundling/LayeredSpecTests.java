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

package infra.gradle.tasks.bundling;

import org.junit.jupiter.api.Test;

import java.io.File;

import infra.app.loader.tools.Library;
import infra.app.loader.tools.LibraryCoordinates;
import infra.app.loader.tools.layer.ApplicationContentFilter;
import infra.app.loader.tools.layer.ContentSelector;
import infra.app.loader.tools.layer.LibraryContentFilter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LayeredSpec}.
 */
class LayeredSpecTests {

  @Test
  void excludeExcludesApplicationContent() {
    LayeredSpec.IntoLayerSpec spec = new LayeredSpec.IntoLayerSpec("application");
    spec.exclude("**/excluded/**");
    assertThat(spec.getIncludes()).isEmpty();
    assertThat(spec.getExcludes()).containsExactly("**/excluded/**");
    ContentSelector<String> selector = spec.asSelector(ApplicationContentFilter::new);
    assertThat(selector.contains("org/example/included/Foo.class")).isTrue();
    assertThat(selector.contains("org/example/excluded/Foo.class")).isFalse();
  }

  @Test
  void includeAndExcludeSelectApplicationContent() {
    LayeredSpec.IntoLayerSpec spec = new LayeredSpec.IntoLayerSpec("application");
    spec.include("org/example/**");
    spec.exclude("org/example/internal/**");
    ContentSelector<String> selector = spec.asSelector(ApplicationContentFilter::new);
    assertThat(selector.contains("org/example/Foo.class")).isTrue();
    assertThat(selector.contains("org/example/internal/Foo.class")).isFalse();
    assertThat(selector.contains("org/other/Foo.class")).isFalse();
  }

  @Test
  void excludeExcludesDependencies() {
    LayeredSpec.DependenciesIntoLayerSpec spec = new LayeredSpec.DependenciesIntoLayerSpec("dependencies");
    spec.exclude("com.example:*");
    assertThat(spec.getIncludes()).isEmpty();
    assertThat(spec.getExcludes()).containsExactly("com.example:*");
    ContentSelector<Library> selector = spec.asLibrarySelector(LibraryContentFilter::new);
    assertThat(selector.contains(library("com.example", "demo", "1.0"))).isFalse();
    assertThat(selector.contains(library("com.other", "demo", "1.0"))).isTrue();
  }

  @Test
  void includeAndExcludeSelectDependencies() {
    LayeredSpec.DependenciesIntoLayerSpec spec = new LayeredSpec.DependenciesIntoLayerSpec("dependencies");
    spec.include("com.example:*");
    spec.exclude("com.example:internal:*");
    ContentSelector<Library> selector = spec.asLibrarySelector(LibraryContentFilter::new);
    assertThat(selector.contains(library("com.example", "demo", "1.0"))).isTrue();
    assertThat(selector.contains(library("com.example", "internal", "1.0"))).isFalse();
    assertThat(selector.contains(library("com.other", "demo", "1.0"))).isFalse();
  }

  private Library library(String groupId, String artifactId, String version) {
    return new Library(null, new File(artifactId + "-" + version + ".jar"), null,
            LibraryCoordinates.of(groupId, artifactId, version), false, false, true);
  }

}
