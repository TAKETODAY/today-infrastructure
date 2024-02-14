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
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TarArchive}.
 *
 * @author Phillip Webb
 */
class TarArchiveTests {

  @TempDir
  File tempDir;

  @Test
  void ofWritesTarContent() throws Exception {
    Owner owner = Owner.of(123, 456);
    TarArchive tarArchive = TarArchive.of((content) -> {
      content.directory("/workspace", owner);
      content.directory("/layers", owner);
      content.directory("/cnb", Owner.ROOT);
      content.directory("/cnb/buildpacks", Owner.ROOT);
      content.directory("/platform", Owner.ROOT);
      content.directory("/platform/env", Owner.ROOT);
    });
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    tarArchive.writeTo(outputStream);
    try (TarArchiveInputStream tarStream = new TarArchiveInputStream(
            new ByteArrayInputStream(outputStream.toByteArray()))) {
      List<TarArchiveEntry> entries = new ArrayList<>();
      TarArchiveEntry entry = tarStream.getNextEntry();
      while (entry != null) {
        entries.add(entry);
        entry = tarStream.getNextEntry();
      }
      assertThat(entries).hasSize(6);
      assertThat(entries.get(0).getName()).isEqualTo("/workspace/");
      assertThat(entries.get(0).getLongUserId()).isEqualTo(123);
      assertThat(entries.get(0).getLongGroupId()).isEqualTo(456);
      assertThat(entries.get(2).getName()).isEqualTo("/cnb/");
      assertThat(entries.get(2).getLongUserId()).isZero();
      assertThat(entries.get(2).getLongGroupId()).isZero();
    }
  }

  @Test
  void fromZipFileReturnsZipFileAdapter() throws Exception {
    Owner owner = Owner.of(123, 456);
    File file = new File(this.tempDir, "test.zip");
    writeTestZip(file);
    TarArchive tarArchive = TarArchive.fromZip(file, owner);
    assertThat(tarArchive).isInstanceOf(ZipFileTarArchive.class);
  }

  private void writeTestZip(File file) throws IOException {
    try (ZipArchiveOutputStream zip = new ZipArchiveOutputStream(file)) {
      ZipArchiveEntry dirEntry = new ZipArchiveEntry("spring/");
      zip.putArchiveEntry(dirEntry);
      zip.closeArchiveEntry();
    }
  }

}
