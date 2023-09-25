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

package cn.taketoday.buildpack.platform.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Collections;
import java.util.Set;

import cn.taketoday.core.ApplicationTemp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link FilePermissions}.
 *
 * @author Scott Frederick
 */
class FilePermissionsTests {

  @TempDir
  Path tempDir;

  @Test
  @DisabledOnOs(OS.WINDOWS)
  void umaskForPath() throws IOException {
    FileAttribute<Set<PosixFilePermission>> fileAttribute = PosixFilePermissions
            .asFileAttribute(PosixFilePermissions.fromString("rw-r-----"));
    Path tempFile = Files.createTempFile(this.tempDir, "umask", null, fileAttribute);
    assertThat(FilePermissions.umaskForPath(tempFile)).isEqualTo(0640);
  }

  @Test
  @DisabledOnOs(OS.WINDOWS)
  void umaskForPathWithNonExistentFile() {
    assertThatIOException()
            .isThrownBy(() -> FilePermissions.umaskForPath(Paths.get(this.tempDir.toString(), "does-not-exist")));
  }

  @Test
  @EnabledOnOs(OS.WINDOWS)
  void umaskForPathOnWindowsFails() throws IOException {
    Path tempFile = ApplicationTemp.createFile("umask");
    assertThatIllegalStateException().isThrownBy(() -> FilePermissions.umaskForPath(tempFile))
            .withMessageContaining("Unsupported file type for retrieving Posix attributes");
  }

  @Test
  void umaskForPathWithNullPath() {
    assertThatIllegalArgumentException().isThrownBy(() -> FilePermissions.umaskForPath(null));
  }

  @Test
  void posixPermissionsToUmask() {
    Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rwxrw-r--");
    assertThat(FilePermissions.posixPermissionsToUmask(permissions)).isEqualTo(0764);
  }

  @Test
  void posixPermissionsToUmaskWithEmptyPermissions() {
    Set<PosixFilePermission> permissions = Collections.emptySet();
    assertThat(FilePermissions.posixPermissionsToUmask(permissions)).isZero();
  }

  @Test
  void posixPermissionsToUmaskWithNullPermissions() {
    assertThatIllegalArgumentException().isThrownBy(() -> FilePermissions.posixPermissionsToUmask(null));
  }

}
