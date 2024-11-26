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

package infra.app.loader.zip;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;

/**
 * Tests for {@link ZipLocalFileHeaderRecord}.
 *
 * @author Phillip Webb
 */
class ZipLocalFileHeaderRecordTests {

  @Test
  void loadLoadsData() throws Exception {
    DataBlock dataBlock = new ByteArrayDataBlock(new byte[] { //
            0x50, 0x4b, 0x03, 0x04, //
            0x01, 0x00, //
            0x02, 0x00, //
            0x03, 0x00, //
            0x04, 0x00, //
            0x05, 0x00, //
            0x06, 0x00, 0x00, 0x00, //
            0x07, 0x00, 0x00, 0x00, //
            0x08, 0x00, 0x00, 0x00, //
            0x09, 0x00, //
            0x0A, 0x00 }); //
    ZipLocalFileHeaderRecord record = ZipLocalFileHeaderRecord.load(dataBlock, 0);
    assertThat(record.versionNeededToExtract()).isEqualTo((short) 1);
    assertThat(record.generalPurposeBitFlag()).isEqualTo((short) 2);
    assertThat(record.compressionMethod()).isEqualTo((short) 3);
    assertThat(record.lastModFileTime()).isEqualTo((short) 4);
    assertThat(record.lastModFileDate()).isEqualTo((short) 5);
    assertThat(record.crc32()).isEqualTo(6);
    assertThat(record.compressedSize()).isEqualTo(7);
    assertThat(record.uncompressedSize()).isEqualTo(8);
    assertThat(record.fileNameLength()).isEqualTo((short) 9);
    assertThat(record.extraFieldLength()).isEqualTo((short) 10);
  }

  @Test
  void loadWhenSignatureDoesNotMatchThrowsException() {
    DataBlock dataBlock = new ByteArrayDataBlock(new byte[] { //
            0x51, 0x4b, 0x03, 0x04, //
            0x01, 0x00, //
            0x02, 0x00, //
            0x03, 0x00, //
            0x04, 0x00, //
            0x05, 0x00, //
            0x06, 0x00, 0x00, 0x00, //
            0x07, 0x00, 0x00, 0x00, //
            0x08, 0x00, 0x00, 0x00, //
            0x09, 0x00, //
            0x0A, 0x00 }); //
    assertThatIOException().isThrownBy(() -> ZipLocalFileHeaderRecord.load(dataBlock, 0))
            .withMessageContaining("'Local File Header Record' not found");
  }

  @Test
  void sizeReturnsSize() {
    ZipLocalFileHeaderRecord record = new ZipLocalFileHeaderRecord((short) 1, (short) 2, (short) 3, (short) 4,
            (short) 5, 6, 7, 8, (short) 9, (short) 10);
    assertThat(record.size()).isEqualTo(49L);
  }

  @Test
  void withExtraFieldLengthReturnsUpdatedInstance() {
    ZipLocalFileHeaderRecord record = new ZipLocalFileHeaderRecord((short) 1, (short) 2, (short) 3, (short) 4,
            (short) 5, 6, 7, 8, (short) 9, (short) 10)
            .withExtraFieldLength((short) 100);
    assertThat(record.extraFieldLength()).isEqualTo((short) 100);
  }

  @Test
  void withFileNameLengthReturnsUpdatedInstance() {
    ZipLocalFileHeaderRecord record = new ZipLocalFileHeaderRecord((short) 1, (short) 2, (short) 3, (short) 4,
            (short) 5, 6, 7, 8, (short) 9, (short) 10)
            .withFileNameLength((short) 100);
    assertThat(record.fileNameLength()).isEqualTo((short) 100);
  }

  @Test
  void asByteArrayReturnsByteArray() throws Exception {
    byte[] bytes = new byte[] { //
            0x50, 0x4b, 0x03, 0x04, //
            0x01, 0x00, //
            0x02, 0x00, //
            0x03, 0x00, //
            0x04, 0x00, //
            0x05, 0x00, //
            0x06, 0x00, 0x00, 0x00, //
            0x07, 0x00, 0x00, 0x00, //
            0x08, 0x00, 0x00, 0x00, //
            0x09, 0x00, //
            0x0A, 0x00 }; //
    ZipLocalFileHeaderRecord record = ZipLocalFileHeaderRecord.load(new ByteArrayDataBlock(bytes), 0);
    assertThat(record.asByteArray()).isEqualTo(bytes);
  }

}
