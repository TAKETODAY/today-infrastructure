/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.aot.generate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.function.Function;

import cn.taketoday.aot.generate.GeneratedFiles.Kind;
import cn.taketoday.core.io.ByteArrayResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link FileSystemGeneratedFiles}.
 *
 * @author Phillip Webb
 */
class FileSystemGeneratedFilesTests {

  @TempDir
  Path root;

  @Test
  void addFilesCopiesToFileSystem() {
    FileSystemGeneratedFiles generatedFiles = new FileSystemGeneratedFiles(this.root);
    generatedFiles.addSourceFile("com.example.Test", "{}");
    generatedFiles.addResourceFile("META-INF/test", "test");
    generatedFiles.addClassFile("com/example/TestProxy.class",
            new ByteArrayResource("!".getBytes(StandardCharsets.UTF_8)));
    assertThat(this.root.resolve("sources/com/example/Test.java")).content().isEqualTo("{}");
    assertThat(this.root.resolve("resources/META-INF/test")).content().isEqualTo("test");
    assertThat(this.root.resolve("classes/com/example/TestProxy.class")).content().isEqualTo("!");
  }

  @Test
  void addFilesWithCustomRootsCopiesToFileSystem() {
    FileSystemGeneratedFiles generatedFiles = new FileSystemGeneratedFiles(
            kind -> this.root.resolve("the-" + kind));
    generatedFiles.addSourceFile("com.example.Test", "{}");
    generatedFiles.addResourceFile("META-INF/test", "test");
    generatedFiles.addClassFile("com/example/TestProxy.class",
            new ByteArrayResource("!".getBytes(StandardCharsets.UTF_8)));
    assertThat(this.root.resolve("the-SOURCE/com/example/Test.java")).content().isEqualTo("{}");
    assertThat(this.root.resolve("the-RESOURCE/META-INF/test")).content().isEqualTo("test");
    assertThat(this.root.resolve("the-CLASS/com/example/TestProxy.class")).content().isEqualTo("!");
  }

  @Test
  void createWhenRootIsNullThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new FileSystemGeneratedFiles((Path) null))
            .withMessage("'root' is required");
  }

  @Test
  void createWhenRootsIsNullThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new FileSystemGeneratedFiles((Function<Kind, Path>) null))
            .withMessage("'roots' is required");
  }

  @Test
  void createWhenRootsResultsInNullThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new FileSystemGeneratedFiles(kind -> (kind != Kind.CLASS) ?
                                                                   this.root.resolve(kind.toString()) : null))
            .withMessage("'roots' must return a value for all file kinds");
  }

  @Test
  void addFileWhenPathIsOutsideOfRootThrowsException() {
    FileSystemGeneratedFiles generatedFiles = new FileSystemGeneratedFiles(this.root);
    assertPathMustBeRelative(generatedFiles, "/test");
    assertPathMustBeRelative(generatedFiles, "../test");
    assertPathMustBeRelative(generatedFiles, "test/../../test");
  }

  private void assertPathMustBeRelative(FileSystemGeneratedFiles generatedFiles, String path) {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> generatedFiles.addResourceFile(path, "test"))
            .withMessage("'path' must be relative");
  }

}
