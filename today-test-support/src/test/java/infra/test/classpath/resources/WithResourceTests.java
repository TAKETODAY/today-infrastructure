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

package infra.test.classpath.resources;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import infra.core.io.ClassPathResource;
import infra.core.io.PathMatchingPatternResourceLoader;
import infra.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WithResource}.
 *
 * @author Andy Wilkinson
 */
class WithResourceTests {

  @Test
  @WithResource(name = "test", content = "content")
  void whenWithResourceIsUsedOnAMethodThenResourceIsAvailable() throws IOException {
    assertThat(new ClassPathResource("test").getContentAsString(StandardCharsets.UTF_8)).isEqualTo("content");
  }

  @Test
  @WithResource(name = "test", content = "content")
  void whenWithResourceIsUsedOnAMethodThenResourceIsAvailableFromFileResourcesRoot(@ResourcesRoot File root) {
    assertThat(new File(root, "test")).hasContent("content");
  }

  @Test
  @WithResource(name = "test", content = "content")
  void whenWithResourceIsUsedOnAMethodThenResourceIsAvailableFromPathResourcesRoot(@ResourcesRoot Path root) {
    assertThat(root.resolve("test")).hasContent("content");
  }

  @Test
  @WithResource(name = "test", content = "content")
  void whenWithResourceIsUsedOnAMethodThenResourceIsAvailableFromPathResourcePath(
          @ResourcePath("test") Path resource) {
    assertThat(resource).hasContent("content");
  }

  @Test
  @WithResource(name = "test", content = "content")
  void whenWithResourceIsUsedOnAMethodThenResourceIsAvailableFromFileResourcePath(
          @ResourcePath("test") File resource) {
    assertThat(resource).hasContent("content");
  }

  @Test
  @WithResource(name = "test", content = "content")
  void whenWithResourceIsUsedOnAMethodThenResourceIsAvailableFromStringResourcePath(
          @ResourcePath("test") String resource) {
    assertThat(new File(resource)).hasContent("content");
  }

  @Test
  @WithResource(name = "test", content = "content")
  void whenWithResourceIsUsedOnAMethodThenResourceContentIsAvailableAsAString(
          @ResourceContent("test") String content) {
    assertThat(content).isEqualTo("content");
  }

  @Test
  @WithResource(name = "com/example/test-resource", content = "content")
  void whenWithResourceNameIncludesADirectoryThenResourceIsAvailable() throws IOException {
    assertThat(new ClassPathResource("com/example/test-resource").getContentAsString(StandardCharsets.UTF_8))
            .isEqualTo("content");
  }

  @Test
  @WithResource(name = "1", content = "one")
  @WithResource(name = "2", content = "two")
  @WithResource(name = "3", content = "three")
  void whenWithResourceIsRepeatedOnAMethodThenAllResourcesAreAvailable() throws IOException {
    assertThat(new ClassPathResource("1").getContentAsString(StandardCharsets.UTF_8)).isEqualTo("one");
    assertThat(new ClassPathResource("2").getContentAsString(StandardCharsets.UTF_8)).isEqualTo("two");
    assertThat(new ClassPathResource("3").getContentAsString(StandardCharsets.UTF_8)).isEqualTo("three");
  }

  @Test
  @WithResource(name = "infra/test/classpath/resources/resource-1.txt", content = "from-with-resource")
  void whenWithResourceCreatesResourceThatIsAvailableElsewhereBothResourcesCanBeLoaded() throws IOException {
    Resource[] resources = new PathMatchingPatternResourceLoader()
            .getResourcesArray("classpath*:infra/test/classpath/resources/resource-1.txt");
    assertThat(resources).hasSize(2);
    assertThat(resources).extracting((resource) -> resource.getContentAsString(StandardCharsets.UTF_8))
            .containsExactly("from-with-resource", "one");
  }

  @Test
  @WithResource(name = "infra/test/classpath/resources/resource-1.txt",
          content = "from-with-resource", additional = false)
  void whenWithResourceCreatesResourceThatIsNotAdditionalThenResourceThatIsAvailableElsewhereCannotBeLoaded()
          throws IOException {
    Resource[] resources = new PathMatchingPatternResourceLoader()
            .getResourcesArray("classpath*:infra/test/classpath/resources/resource-1.txt");
    assertThat(resources).hasSize(1);
    assertThat(resources[0].getContentAsString(StandardCharsets.UTF_8)).isEqualTo("from-with-resource");
  }

}
