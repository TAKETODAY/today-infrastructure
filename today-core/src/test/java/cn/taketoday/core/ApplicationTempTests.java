/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import cn.taketoday.util.FileSystemUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/20 23:28
 */
class ApplicationTempTests {

  @BeforeEach
  @AfterEach
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

  private void assertDirectoryPermissions(Path path) throws IOException {
    Set<PosixFilePermission> permissions = Files.getFileAttributeView(path, PosixFileAttributeView.class)
            .readAttributes().permissions();
    assertThat(permissions).containsExactlyInAnyOrder(PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE);
  }

}
