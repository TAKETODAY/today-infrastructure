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
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.function.Function;

import cn.taketoday.aot.generate.GeneratedFiles.FileHandler;
import cn.taketoday.aot.generate.GeneratedFiles.Kind;
import cn.taketoday.core.io.ByteArrayResource;
import cn.taketoday.util.function.ThrowingConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

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

  @Test
  void addFileWhenFileAlreadyAddedThrowsException() {
    FileSystemGeneratedFiles generatedFiles = new FileSystemGeneratedFiles(this.root);
    generatedFiles.addResourceFile("META-INF/mydir", "test");
    assertThatIllegalStateException()
            .isThrownBy(() -> generatedFiles.addResourceFile("META-INF/mydir", "test"))
            .withMessageContainingAll("META-INF", "mydir", "already exists");
  }

  @Test
  void handleFileWhenFileExistsProvidesFileHandler() {
    FileSystemGeneratedFiles generatedFiles = new FileSystemGeneratedFiles(this.root);
    generatedFiles.addResourceFile("META-INF/test", "test");
    generatedFiles.handleFile(Kind.RESOURCE, "META-INF/test", handler -> {
      assertThat(handler.exists()).isTrue();
      assertThat(handler.getContent()).isNotNull();
      assertThat(handler.getContent().getInputStream()).hasContent("test");
    });
    assertThat(this.root.resolve("resources/META-INF/test")).content().isEqualTo("test");
  }

  @Test
  void handleFileWhenFileExistsFailsToCreate() {
    FileSystemGeneratedFiles generatedFiles = new FileSystemGeneratedFiles(this.root);
    generatedFiles.addResourceFile("META-INF/mydir", "test");
    ThrowingConsumer<FileHandler> consumer = handler ->
            handler.create(new ByteArrayResource("should fail".getBytes(StandardCharsets.UTF_8)));
    assertThatIllegalStateException()
            .isThrownBy(() -> generatedFiles.handleFile(Kind.RESOURCE, "META-INF/mydir", consumer))
            .withMessageContainingAll("META-INF", "mydir", "already exists");
  }

  @Test
  void handleFileWhenFileExistsCanOverrideContent() {
    FileSystemGeneratedFiles generatedFiles = new FileSystemGeneratedFiles(this.root);
    generatedFiles.addResourceFile("META-INF/mydir", "test");
    generatedFiles.handleFile(Kind.RESOURCE, "META-INF/mydir", handler ->
            handler.override(new ByteArrayResource("overridden".getBytes(StandardCharsets.UTF_8))));
    assertThat(this.root.resolve("resources/META-INF/mydir")).content().isEqualTo("overridden");
  }

  private void assertPathMustBeRelative(FileSystemGeneratedFiles generatedFiles, String path) {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> generatedFiles.addResourceFile(path, "test"))
            .withMessage("'path' must be relative");
  }

}
