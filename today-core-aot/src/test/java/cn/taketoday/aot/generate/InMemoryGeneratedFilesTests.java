/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.aot.generate;

import org.junit.jupiter.api.Test;

import cn.taketoday.aot.generate.GeneratedFiles.Kind;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link InMemoryGeneratedFiles}.
 *
 * @author Phillip Webb
 */
class InMemoryGeneratedFilesTests {

  private final InMemoryGeneratedFiles generatedFiles = new InMemoryGeneratedFiles();

  @Test
  void addFileAddsInMemoryFile() throws Exception {
    this.generatedFiles.addResourceFile("META-INF/test", "test");
    assertThat(this.generatedFiles.getGeneratedFileContent(Kind.RESOURCE,
            "META-INF/test")).isEqualTo("test");
  }

  @Test
  void addFileWhenFileAlreadyAddedThrowsException() {
    this.generatedFiles.addResourceFile("META-INF/test", "test");
    assertThatIllegalStateException().isThrownBy(
                    () -> this.generatedFiles.addResourceFile("META-INF/test", "test"))
            .withMessage("META-INF/test already exists");
  }

  @Test
  void getGeneratedFilesReturnsFiles() {
    this.generatedFiles.addResourceFile("META-INF/test1", "test1");
    this.generatedFiles.addResourceFile("META-INF/test2", "test2");
    assertThat(this.generatedFiles.getGeneratedFiles(Kind.RESOURCE))
            .containsKeys("META-INF/test1", "META-INF/test2");
  }

  @Test
  void getGeneratedFileContentWhenFileExistsReturnsContent() throws Exception {
    this.generatedFiles.addResourceFile("META-INF/test", "test");
    assertThat(this.generatedFiles.getGeneratedFileContent(Kind.RESOURCE,
            "META-INF/test")).isEqualTo("test");
  }

  @Test
  void getGeneratedFileContentWhenFileIsMissingReturnsNull() throws Exception {
    this.generatedFiles.addResourceFile("META-INF/test", "test");
    assertThat(this.generatedFiles.getGeneratedFileContent(Kind.RESOURCE,
            "META-INF/missing")).isNull();
  }

  @Test
  void getGeneratedFileWhenFileExistsReturnsInputStreamSource() {
    this.generatedFiles.addResourceFile("META-INF/test", "test");
    assertThat(this.generatedFiles.getGeneratedFile(Kind.RESOURCE, "META-INF/test"))
            .isNotNull();
  }

  @Test
  void getGeneratedFileWhenFileIsMissingReturnsNull() {
    this.generatedFiles.addResourceFile("META-INF/test", "test");
    assertThat(
            this.generatedFiles.getGeneratedFile(Kind.RESOURCE, "META-INF/missing"))
            .isNull();
  }

}
