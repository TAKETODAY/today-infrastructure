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

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ZipFileTarArchive}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class ZipFileTarArchiveTests {

  @TempDir
  File tempDir;

  @Test
  void createWhenZipIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new ZipFileTarArchive(null, Owner.ROOT))
            .withMessage("Zip must not be null");
  }

  @Test
  void createWhenOwnerIsNullThrowsException() throws Exception {
    File file = new File(this.tempDir, "test.zip");
    writeTestZip(file);
    assertThatIllegalArgumentException().isThrownBy(() -> new ZipFileTarArchive(file, null))
            .withMessage("Owner must not be null");
  }

  @Test
  void writeToAdaptsContent() throws Exception {
    Owner owner = Owner.of(123, 456);
    File file = new File(this.tempDir, "test.zip");
    writeTestZip(file);
    TarArchive tarArchive = TarArchive.fromZip(file, owner);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    tarArchive.writeTo(outputStream);
    try (TarArchiveInputStream tarStream = new TarArchiveInputStream(
            new ByteArrayInputStream(outputStream.toByteArray()))) {
      TarArchiveEntry dirEntry = tarStream.getNextTarEntry();
      assertThat(dirEntry.getName()).isEqualTo("spring/");
      assertThat(dirEntry.getLongUserId()).isEqualTo(123);
      assertThat(dirEntry.getLongGroupId()).isEqualTo(456);
      TarArchiveEntry fileEntry = tarStream.getNextTarEntry();
      assertThat(fileEntry.getName()).isEqualTo("spring/boot");
      assertThat(fileEntry.getLongUserId()).isEqualTo(123);
      assertThat(fileEntry.getLongGroupId()).isEqualTo(456);
      assertThat(fileEntry.getSize()).isEqualTo(4);
      assertThat(fileEntry.getMode()).isEqualTo(0755);
      assertThat(tarStream).hasContent("test");
    }
  }

  private void writeTestZip(File file) throws IOException {
    try (ZipArchiveOutputStream zip = new ZipArchiveOutputStream(file)) {
      ZipArchiveEntry dirEntry = new ZipArchiveEntry("spring/");
      zip.putArchiveEntry(dirEntry);
      zip.closeArchiveEntry();
      ZipArchiveEntry fileEntry = new ZipArchiveEntry("spring/boot");
      fileEntry.setUnixMode(0755);
      zip.putArchiveEntry(fileEntry);
      zip.write("test".getBytes(StandardCharsets.UTF_8));
      zip.closeArchiveEntry();
    }
  }

}
