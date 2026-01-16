/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.app.web.context.reactive;

import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;

import infra.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/11 22:27
 */
class FilteredReactiveWebContextResourceTests {

  @Test
  void shouldCreateResourceWithGivenPath() {
    // given
    String path = "test/path";

    // when
    FilteredReactiveWebContextResource resource = new FilteredReactiveWebContextResource(path);

    // then
    assertThat(resource).isNotNull();
  }

  @Test
  void shouldAlwaysReturnFalseForExists() {
    // given
    FilteredReactiveWebContextResource resource = new FilteredReactiveWebContextResource("test/path");

    // when & then
    assertThat(resource.exists()).isFalse();
  }

  @Test
  void shouldCreateRelativeResource() throws IOException {
    // given
    FilteredReactiveWebContextResource resource = new FilteredReactiveWebContextResource("test/path");

    Resource relativeResource = resource.createRelative("subpath");

    assertThat(relativeResource).isNotNull();
    assertThat(relativeResource).isInstanceOf(FilteredReactiveWebContextResource.class);
  }

  @Test
  void shouldThrowFileNotFoundExceptionWhenGetInputStream() {
    // given
    FilteredReactiveWebContextResource resource = new FilteredReactiveWebContextResource("test/path");

    assertThatThrownBy(resource::getInputStream)
            .isInstanceOf(FileNotFoundException.class)
            .hasMessageContaining("cannot be opened because it does not exist");
  }

  @Test
  void shouldReturnCorrectToStringRepresentation() {
    // given
    String path = "test/path";
    FilteredReactiveWebContextResource resource = new FilteredReactiveWebContextResource(path);

    String stringRepresentation = resource.toString();

    assertThat(stringRepresentation).isEqualTo("ReactiveWebContext resource [test/path]");
  }

}