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
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TarLayoutWriter}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class TarLayoutWriterTests {

  @Test
  void writesTarArchive() throws Exception {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try (TarLayoutWriter writer = new TarLayoutWriter(outputStream)) {
      writer.directory("/foo", Owner.ROOT);
      writer.file("/foo/bar.txt", Owner.of(1, 1), 0777, Content.of("test"));
    }
    try (TarArchiveInputStream tarInputStream = new TarArchiveInputStream(
            new ByteArrayInputStream(outputStream.toByteArray()))) {
      TarArchiveEntry directoryEntry = tarInputStream.getNextEntry();
      TarArchiveEntry fileEntry = tarInputStream.getNextEntry();
      byte[] fileContent = new byte[(int) fileEntry.getSize()];
      tarInputStream.read(fileContent);
      assertThat(tarInputStream.getNextEntry()).isNull();
      assertThat(directoryEntry.getName()).isEqualTo("/foo/");
      assertThat(directoryEntry.getMode()).isEqualTo(0755);
      assertThat(directoryEntry.getLongUserId()).isZero();
      assertThat(directoryEntry.getLongGroupId()).isZero();
      assertThat(directoryEntry.getModTime()).isEqualTo(new Date(TarLayoutWriter.NORMALIZED_MOD_TIME));
      assertThat(fileEntry.getName()).isEqualTo("/foo/bar.txt");
      assertThat(fileEntry.getMode()).isEqualTo(0777);
      assertThat(fileEntry.getLongUserId()).isOne();
      assertThat(fileEntry.getLongGroupId()).isOne();
      assertThat(fileEntry.getModTime()).isEqualTo(new Date(TarLayoutWriter.NORMALIZED_MOD_TIME));
      assertThat(fileContent).isEqualTo("test".getBytes(StandardCharsets.UTF_8));
    }
  }

}
