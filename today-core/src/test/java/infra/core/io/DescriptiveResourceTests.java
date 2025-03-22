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