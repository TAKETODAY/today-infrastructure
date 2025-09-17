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

package infra.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import infra.util.FileSystemUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/20 23:28
 */
class ApplicationTempTests {

  //  @BeforeEach
//  @AfterEach
  void cleanup() throws IOException {
    FileSystemUtils.deleteRecursively(new ApplicationTemp().getDir());
  }

  @Test
  void generatesConsistentTemp() {
    ApplicationTemp t1 = new ApplicationTemp();
    ApplicationTemp t2 = new ApplicationTemp();
    assertThat(t1.getDir()).isNotNull();
    assertThat(t1.getDir()).isEqualTo(t2.getDir());
  }

  @Test
  void differentBasedOnUserDir() {
    String userDir = System.getProperty("user.dir");
    try {
      Path t1 = new ApplicationTemp().getDir();
      System.setProperty("user.dir", "abc");
      Path t2 = new ApplicationTemp().getDir();
      assertThat(t1).isEqualTo(t2);
    }
    finally {
      System.setProperty("user.dir", userDir);
    }
  }

  @Test
  void getSubDir() {
    ApplicationTemp temp = new ApplicationTemp();
    assertThat(temp.getDir("abc")).isEqualTo(new File(temp.getDir().toFile(), "abc").toPath());
  }

  @Test
  void posixPermissions() throws IOException {
    ApplicationTemp temp = new ApplicationTemp();
    Path path = temp.getDir();
    FileSystem fileSystem = path.getFileSystem();
    if (fileSystem.supportedFileAttributeViews().contains("posix")) {
      assertDirectoryPermissions(path);
      assertDirectoryPermissions(temp.getDir("sub"));
    }
  }

  @Test
  void createFileGeneratesUniqueFiles() throws IOException {
    ApplicationTemp temp = new ApplicationTemp();
    Path file1 = temp.createFile("subdir", "prefix");
    Path file2 = temp.createFile("subdir", "prefix");
    assertThat(file1).exists();
    assertThat(file2).exists();
    assertThat(file1).isNotEqualTo(file2);
  }

  @Test
  void createFileWithSuffixGeneratesCorrectExtension() throws IOException {
    ApplicationTemp temp = new ApplicationTemp();
    Path file = temp.createFile("subdir", "prefix", ".txt");
    assertThat(file.toString()).endsWith(".txt");
  }

  @Test
  void createFileInSubdirCreatesParentDirectories() throws IOException {
    ApplicationTemp temp = new ApplicationTemp();
    Path file = temp.createFile("deep/nested/dir", "test");
    assertThat(file.getParent()).exists();
    assertThat(Files.isDirectory(file.getParent())).isTrue();
  }

  @Test
  void sourceClassSpecificDirectoriesAreDifferent() {
    ApplicationTemp temp1 = new ApplicationTemp(String.class);
    ApplicationTemp temp2 = new ApplicationTemp(Integer.class);
    assertThat(temp1.getDir()).isNotEqualTo(temp2.getDir());
  }

  @Test
  void staticCreateDirectoryMethodUsesDefaultInstance() {
    Path dir = ApplicationTemp.createDirectory("test");
    assertThat(dir).isEqualTo(ApplicationTemp.instance.getDir("test"));
  }

  @Test
  void staticCreateFileMethodUsesDefaultInstance() {
    Path file = ApplicationTemp.createFile("test");
    assertThat(file.getParent()).isEqualTo(ApplicationTemp.instance.getDir());
  }

  @Test
  void toStringReturnsDirectoryPath() {
    ApplicationTemp temp = new ApplicationTemp();
    assertThat(temp.toString()).isEqualTo(temp.getDir().toString());
  }

  @Test
  void multipleGetDirCallsReturnSamePath() {
    ApplicationTemp temp = new ApplicationTemp();
    Path first = temp.getDir();
    Path second = temp.getDir();
    assertThat(first).isSameAs(second);
  }

  @Test
  void nullSubDirReturnsBasePath() {
    ApplicationTemp temp = new ApplicationTemp();
    assertThat(temp.getDir(null)).isEqualTo(temp.getDir());
  }

  @Test
  void tempDirectoryInvalidLocationThrowsException() {
    String original = System.getProperty("java.io.tmpdir");
    try {
      System.setProperty("java.io.tmpdir", "/invalid/location");
      ApplicationTemp temp = new ApplicationTemp();
      assertThatIllegalStateException()
              .isThrownBy(temp::getDir)
              .withMessageContaining("Temp directory '/invalid/location' does not exist");
    }
    finally {
      System.setProperty("java.io.tmpdir", original);
    }
  }

  @Test
  void tempDirectoryPointingToFileThrowsException(@TempDir Path tempDir) throws IOException {
    String original = System.getProperty("java.io.tmpdir");
    Path file = tempDir.resolve("file");
    Files.createFile(file);
    try {
      System.setProperty("java.io.tmpdir", file.toString());
      ApplicationTemp temp = new ApplicationTemp();
      assertThatIllegalStateException()
              .isThrownBy(temp::getDir)
              .withMessageContaining("is not a directory");
    }
    finally {
      System.setProperty("java.io.tmpdir", original);
    }
  }

  @Test
  void createFileWithNullPrefixAndSuffix() throws IOException {
    ApplicationTemp temp = new ApplicationTemp();
    Path file = temp.createFile(null, null);
    assertThat(file).exists();
    assertThat(file.getParent()).isEqualTo(temp.getDir());
  }

  @Test
  void createFileInNonExistentSubDirectoryCreatesDirectory() throws IOException {
    ApplicationTemp temp = new ApplicationTemp();
    String subDir = "nonexistent/subdirectory";
    Path file = temp.createFile(subDir, "test");
    assertThat(file).exists();
    assertThat(file.getParent()).isEqualTo(temp.getDir(subDir));
  }

  @Test
  void differentInstancesWithSameSourceClassShareDirectory() {
    ApplicationTemp temp1 = new ApplicationTemp(ApplicationTemp.class);
    ApplicationTemp temp2 = new ApplicationTemp(ApplicationTemp.class);
    assertThat(temp1.getDir()).isEqualTo(temp2.getDir());
  }

  private void assertDirectoryPermissions(Path path) throws IOException {
    Set<PosixFilePermission> permissions = Files.getFileAttributeView(path, PosixFileAttributeView.class)
            .readAttributes().permissions();
    assertThat(permissions).containsExactlyInAnyOrder(PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE);
  }

}
