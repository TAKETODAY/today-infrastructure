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

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import infra.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WithResource} when used on a super-class.
 *
 * @author Andy Wilkinson
 */
class OnSuperClassWithResourceTests extends WithResourceClass {

  @Test
  void whenWithResourceIsUsedOnASuperClassThenResourceIsAvailable() throws IOException {
    assertThat(new ClassPathResource("on-super-class").getContentAsString(StandardCharsets.UTF_8))
            .isEqualTo("super-class content");
  }

  @Test
  @WithResource(name = "method-resource", content = "method")
  void whenWithResourceIsUsedOnASuperClassAndMethodThenBothResourcesAreAvailable() throws IOException {
    assertThat(new ClassPathResource("on-super-class").getContentAsString(StandardCharsets.UTF_8))
            .isEqualTo("super-class content");
    assertThat(new ClassPathResource("method-resource").getContentAsString(StandardCharsets.UTF_8))
            .isEqualTo("method");
  }

  @Test
  @WithResource(name = "method-resource-1", content = "method-1")
  @WithResource(name = "method-resource-2", content = "method-2")
  void whenWithResourceIsUsedOnASuperClassAndRepeatedOnMethodThenAllResourcesAreAvailable() throws IOException {
    assertThat(new ClassPathResource("on-super-class").getContentAsString(StandardCharsets.UTF_8))
            .isEqualTo("super-class content");
    assertThat(new ClassPathResource("method-resource-1").getContentAsString(StandardCharsets.UTF_8))
            .isEqualTo("method-1");
    assertThat(new ClassPathResource("method-resource-2").getContentAsString(StandardCharsets.UTF_8))
            .isEqualTo("method-2");
  }

}
