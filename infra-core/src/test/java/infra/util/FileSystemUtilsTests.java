/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author TODAY 2021/8/21 00:41
 */
class FileSystemUtilsTests {

  @AfterEach
  void tearDown() throws Exception {
    File tmp = new File("./tmp");
    if (tmp.exists()) {
      FileSystemUtils.deleteRecursively(tmp);
    }
    File dest = new File("./dest");
    if (dest.exists()) {
      FileSystemUtils.deleteRecursively(dest);
    }
  }

  @Test
  void deleteRecursively(@TempDir File tempDir) throws Exception {
    File root = new File(tempDir, "root");
    File child = new File(root, "child");
    File grandchild = new File(child, "grandchild");

    grandchild.mkdirs();

    File bar = new File(child, "bar.txt");
    bar.createNewFile();

    assertThat(root).exists();
    assertThat(child).exists();
    assertThat(grandchild).exists();
    assertThat(bar).exists();

    FileSystemUtils.deleteRecursively(root);

    assertThat(root).doesNotExist();
    assertThat(child).doesNotExist();
    assertThat(grandchild).doesNotExist();
    assertThat(bar).doesNotExist();
  }

  @Test
  void copyRecursively(@TempDir File tempDir) throws Exception {
    File src = new File(tempDir, "src");
    File child = new File(src, "child");
    File grandchild = new File(child, "grandchild");

    grandchild.mkdirs();

    File bar = new File(child, "bar.txt");
    bar.createNewFile();

    assertThat(src).exists();
    assertThat(child).exists();
    assertThat(grandchild).exists();
    assertThat(bar).exists();

    File dest = new File(tempDir, "/dest");
    FileSystemUtils.copyRecursively(src, dest);

    assertThat(dest).exists();
    assertThat(new File(dest, "child")).exists();
    assertThat(new File(dest, "child/bar.txt")).exists();

    String destPath = dest.toString().replace('\\', '/');
    if (!destPath.startsWith("/")) {
      destPath = "/" + destPath;
    }
    URI uri = URI.create("jar:file:" + destPath + "/archive.zip");
    Map<String, String> env = Map.of("create", "true");
    FileSystem zipfs = FileSystems.newFileSystem(uri, env);
    Path ziproot = zipfs.getPath("/");
    FileSystemUtils.copyRecursively(src.toPath(), ziproot);

    assertThat(zipfs.getPath("/child")).exists();
    assertThat(zipfs.getPath("/child/bar.txt")).exists();

    zipfs.close();
    FileSystemUtils.deleteRecursively(src);
    assertThat(src).doesNotExist();
  }

  @Test
  void deleteRecursivelyWithNullReturnsTrue() {
    assertThat(FileSystemUtils.deleteRecursively((File) null)).isFalse();
  }

  @Test
  void deleteRecursivelyWithNonExistentFileReturnsFalse() {
    assertThat(FileSystemUtils.deleteRecursively(new File("notexists"))).isFalse();
  }

  @Test
  void copyRecursivelyWithNonExistentSourceThrowsException() {
    File src = new File("notexists");
    File dest = new File("dest");

    assertThatThrownBy(() -> FileSystemUtils.copyRecursively(src, dest))
            .isInstanceOf(IOException.class);
  }

  @Test
  void copyRecursivelyWithReadOnlyFiles() throws IOException {
    File src = new File("./tmp/src");
    File file = new File(src, "readonly.txt");
    src.mkdirs();
    file.createNewFile();
    file.setReadOnly();

    File dest = new File("./tmp/dest");
    FileSystemUtils.copyRecursively(src, dest);

    File copiedFile = new File(dest, "readonly.txt");
    assertThat(copiedFile).exists();
    assertThat(copiedFile.canWrite()).isFalse();
  }

  @Test
  void copyRecursivelyWithSpecialCharactersInFilenames() throws IOException {
    File src = new File("./tmp/src");
    File specialFile = new File(src, "special#$@!.txt");
    src.mkdirs();
    specialFile.createNewFile();

    File dest = new File("./tmp/dest");
    FileSystemUtils.copyRecursively(src, dest);

    assertThat(new File(dest, "special#$@!.txt")).exists();
  }

  @Test
  void copyRecursivelyWithEmptyDirectory() throws IOException {
    File src = new File("./tmp/empty");
    src.mkdirs();

    File dest = new File("./tmp/dest");
    FileSystemUtils.copyRecursively(src, dest);

    assertThat(dest).exists().isDirectory();
    assertThat(dest.list()).isEmpty();
  }

  @Test
  void copyRecursivelyWithDeepNesting() throws IOException {
    File src = new File("./tmp/src");
    src.mkdirs();

    // Create deep nested structure
    File current = src;
    for (int i = 0; i < 10; i++) {
      current = new File(current, "level" + i);
      current.mkdir();
      new File(current, "file" + i + ".txt").createNewFile();
    }

    File dest = new File("./tmp/dest");
    FileSystemUtils.copyRecursively(src, dest);

    File deepFile = new File(dest, "level0/level1/level2/level3/level4/file4.txt");
    assertThat(deepFile).exists();
  }

  @Test
  void copyRecursivelyWithLargeFile() throws IOException {
    File src = new File("./tmp/src");
    src.mkdirs();

    File largeFile = new File(src, "large.dat");
    byte[] data = new byte[100 * 1024 * 1024]; // 100MB
    Files.write(largeFile.toPath(), data);

    File dest = new File("./tmp/dest");
    FileSystemUtils.copyRecursively(src, dest);

    File copiedFile = new File(dest, "large.dat");
    assertThat(copiedFile).exists();
    assertThat(copiedFile.length()).isEqualTo(data.length);
  }

  @Test
  void deleteRecursivelyWithOpenFileHandles() throws IOException {
    File root = new File("./tmp/root");
    root.mkdirs();

    File file = new File(root, "test.txt");
    file.createNewFile();

    // Keep file open while trying to delete
    try (var stream = Files.newInputStream(file.toPath())) {
      assertThat(FileSystemUtils.deleteRecursively(root)).isTrue();
    }

    assertThat(root).doesNotExist();
  }

  @Test
  void copyRecursivelyToExistingDestination() throws IOException {
    File src = new File("./tmp/src");
    src.mkdirs();
    new File(src, "file.txt").createNewFile();

    File dest = new File("./tmp/dest");
    dest.mkdirs();
    new File(dest, "existing.txt").createNewFile();

    FileSystemUtils.copyRecursively(src, dest);

    assertThat(new File(dest, "file.txt")).exists();
    assertThat(new File(dest, "existing.txt")).exists();
  }

  @Test
  void deleteRecursivelyWithReadOnlyFiles() throws IOException {
    File root = new File("./tmp/root");
    root.mkdirs();

    File readOnly = new File(root, "readonly.txt");
    readOnly.createNewFile();
    readOnly.setReadOnly();

    assertThat(FileSystemUtils.deleteRecursively(root)).isTrue();
    assertThat(root).doesNotExist();
  }

  @Test
  void copyRecursivelyPreservesFilePermissions() throws IOException {
    File src = new File("./tmp/src");
    src.mkdirs();

    File file = new File(src, "test.txt");
    file.createNewFile();
    file.setExecutable(true, true);

    File dest = new File("./tmp/dest");
    FileSystemUtils.copyRecursively(src, dest);

    File copiedFile = new File(dest, "test.txt");
    assertThat(copiedFile.canExecute()).isTrue();
  }

}
