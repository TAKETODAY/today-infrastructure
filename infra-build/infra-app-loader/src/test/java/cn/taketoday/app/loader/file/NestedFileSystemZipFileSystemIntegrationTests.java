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

package cn.taketoday.app.loader.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import cn.taketoday.app.loader.net.protocol.jar.JarUrl;
import cn.taketoday.app.loader.testsupport.TestJar;
import cn.taketoday.app.loader.zip.AssertFileChannelDataBlocksClosed;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link NestedFileSystem} in combination with
 * {@code ZipFileSystem}.
 *
 * @author Phillip Webb
 */
@AssertFileChannelDataBlocksClosed
class NestedFileSystemZipFileSystemIntegrationTests {

  @TempDir
  File temp;

  @Test
  void zip() throws Exception {
    File file = new File(this.temp, "test.jar");
    TestJar.create(file);
    URI uri = JarUrl.create(file).toURI();
    try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
      assertThat(Files.readAllBytes(fs.getPath("1.dat"))).containsExactly(0x1);
    }
  }

  @Test
  void nestedZip() throws Exception {
    File file = new File(this.temp, "test.jar");
    TestJar.create(file);
    URI uri = JarUrl.create(file, "nested.jar").toURI();
    try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
      assertThat(Files.readAllBytes(fs.getPath("3.dat"))).containsExactly(0x3);
    }
  }

  @Test
  void nestedZipWithoutNewFileSystem() throws Exception {
    File file = new File(this.temp, "test.jar");
    TestJar.create(file);
    URI uri = JarUrl.create(file, "nested.jar", "3.dat").toURI();
    Path path = Path.of(uri);
    assertThat(Files.readAllBytes(path)).containsExactly(0x3);
  }

  @Test
    // gh-38592
  void nestedZipSplitAndRestore() throws Exception {
    File file = new File(this.temp, "test.jar");
    TestJar.create(file);
    URI uri = JarUrl.create(file, "nested.jar", "3.dat").toURI();
    String[] components = uri.toString().split("!");
    System.out.println(List.of(components));
    try (FileSystem rootFs = FileSystems.newFileSystem(URI.create(components[0]), Collections.emptyMap())) {
      Path childPath = rootFs.getPath(components[1]);
      try (FileSystem childFs = FileSystems.newFileSystem(childPath)) {
        Path nestedRoot = childFs.getPath("/");
        assertThat(Files.list(nestedRoot)).hasSize(4);
        Path path = childFs.getPath(components[2]);
        assertThat(Files.readAllBytes(path)).containsExactly(0x3);
      }
    }
  }

}
