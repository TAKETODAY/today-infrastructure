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

package infra.core.io;

import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/22 14:56
 */
class DescriptiveResourceTests {

  @Test
  void resourceWithNullDescriptionUsesEmptyString() {
    DescriptiveResource resource = new DescriptiveResource(null);
    assertThat(resource.toString()).isEmpty();
  }

  @Test
  void resourceWithDescriptionReturnsDescription() {
    DescriptiveResource resource = new DescriptiveResource("test description");
    assertThat(resource.toString()).isEqualTo("test description");
  }

  @Test
  void resourceNeverExists() {
    DescriptiveResource resource = new DescriptiveResource("any description");
    assertThat(resource.exists()).isFalse();
  }

  @Test
  void getInputStreamThrowsFileNotFoundException() {
    DescriptiveResource resource = new DescriptiveResource("test");
    assertThatExceptionOfType(FileNotFoundException.class)
            .isThrownBy(resource::getInputStream)
            .withMessageContaining("cannot be opened because it does not point to a readable resource");
  }

  @Test
  void equalityBasedOnDescription() {
    DescriptiveResource resource1 = new DescriptiveResource("test");
    DescriptiveResource resource2 = new DescriptiveResource("test");
    DescriptiveResource resource3 = new DescriptiveResource("different");

    assertThat(resource1)
            .isEqualTo(resource1)
            .isEqualTo(resource2)
            .isNotEqualTo(resource3)
            .isNotEqualTo(null)
            .isNotEqualTo(new Object());
  }

  @Test
  void hashCodeBasedOnDescription() {
    String description = "test description";
    DescriptiveResource resource = new DescriptiveResource(description);
    assertThat(resource.hashCode()).isEqualTo(description.hashCode());
  }

  @Test
  void resourceWithUnicodeCharactersInDescription() {
    String unicodeDesc = "测试资源";
    DescriptiveResource resource = new DescriptiveResource(unicodeDesc);
    assertThat(resource.toString()).isEqualTo(unicodeDesc);
  }

  @Test
  void resourceWithEmptyDescription() {
    DescriptiveResource resource = new DescriptiveResource("");
    assertThat(resource.toString()).isEmpty();
  }

}