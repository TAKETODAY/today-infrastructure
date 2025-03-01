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

import infra.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WithResourceDirectory}.
 *
 * @author Andy Wilkinson
 */
class WithResourceDirectoryTests {

  @Test
  @WithResourceDirectory("test")
  void whenWithResourceDirectoryIsUsedOnAMethodThenDirectoryIsCreated() throws IOException {
    assertThat(new ClassPathResource("test").getFile()).isDirectory();
  }

  @Test
  @WithResourceDirectory("com/example/nested")
  void whenWithResourceDirectoryNamesANestedDirectoryThenDirectoryIsCreated() throws IOException {
    assertThat(new ClassPathResource("com/example/nested").getFile()).isDirectory();
  }

  @Test
  @WithResourceDirectory("1")
  @WithResourceDirectory("2")
  @WithResourceDirectory("3")
  void whenWithResourceDirectoryIsRepeatedOnAMethodThenAllResourceDirectoriesAreCreated() throws IOException {
    assertThat(new ClassPathResource("1").getFile()).isDirectory();
    assertThat(new ClassPathResource("2").getFile()).isDirectory();
    assertThat(new ClassPathResource("3").getFile()).isDirectory();
  }

}
