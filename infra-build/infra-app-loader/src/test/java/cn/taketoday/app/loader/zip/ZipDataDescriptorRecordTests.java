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

package cn.taketoday.app.loader.zip;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ZipDataDescriptorRecord}.
 *
 * @author Phillip Webb
 */
class ZipDataDescriptorRecordTests {

  private static final short S0 = 0;

  @Test
  void loadWhenHasSignatureLoadsData() throws Exception {
    DataBlock dataBlock = new ByteArrayDataBlock(new byte[] { //
            0x50, 0x4b, 0x07, 0x08, //
            0x01, 0x00, 0x00, 0x00, //
            0x02, 0x00, 0x00, 0x00, //
            0x03, 0x00, 0x00, 0x00 }); //
    ZipDataDescriptorRecord record = ZipDataDescriptorRecord.load(dataBlock, 0);
    assertThat(record.includeSignature()).isTrue();
    assertThat(record.crc32()).isEqualTo(1);
    assertThat(record.compressedSize()).isEqualTo(2);
    assertThat(record.uncompressedSize()).isEqualTo(3);
  }

  @Test
  void loadWhenHasNoSignatureLoadsData() throws Exception {
    DataBlock dataBlock = new ByteArrayDataBlock(new byte[] { //
            0x01, 0x00, 0x00, 0x00, //
            0x02, 0x00, 0x00, 0x00, //
            0x03, 0x00, 0x00, 0x00 }); //
    ZipDataDescriptorRecord record = ZipDataDescriptorRecord.load(dataBlock, 0);
    assertThat(record.includeSignature()).isFalse();
    assertThat(record.crc32()).isEqualTo(1);
    assertThat(record.compressedSize()).isEqualTo(2);
    assertThat(record.uncompressedSize()).isEqualTo(3);
  }

  @Test
  void sizeWhenIncludeSignatureReturnsSize() {
    ZipDataDescriptorRecord record = new ZipDataDescriptorRecord(true, 0, 0, 0);
    assertThat(record.size()).isEqualTo(16);
  }

  @Test
  void sizeWhenNotIncludeSignatureReturnsSize() {
    ZipDataDescriptorRecord record = new ZipDataDescriptorRecord(false, 0, 0, 0);
    assertThat(record.size()).isEqualTo(12);
  }

  @Test
  void asByteArrayWhenIncludeSignatureReturnsByteArray() throws Exception {
    byte[] bytes = new byte[] { //
            0x50, 0x4b, 0x07, 0x08, //
            0x01, 0x00, 0x00, 0x00, //
            0x02, 0x00, 0x00, 0x00, //
            0x03, 0x00, 0x00, 0x00 }; //
    ZipDataDescriptorRecord record = ZipDataDescriptorRecord.load(new ByteArrayDataBlock(bytes), 0);
    assertThat(record.asByteArray()).isEqualTo(bytes);
  }

  @Test
  void asByteArrayWhenNotIncludeSignatureReturnsByteArray() throws Exception {
    byte[] bytes = new byte[] { //
            0x01, 0x00, 0x00, 0x00, //
            0x02, 0x00, 0x00, 0x00, //
            0x03, 0x00, 0x00, 0x00 }; //
    ZipDataDescriptorRecord record = ZipDataDescriptorRecord.load(new ByteArrayDataBlock(bytes), 0);
    assertThat(record.asByteArray()).isEqualTo(bytes);
  }

  @Test
  void isPresentBasedOnFlagWhenPresentReturnsTrue() {
    testIsPresentBasedOnFlag((short) 0x8, true);
  }

  @Test
  void isPresentBasedOnFlagWhenNotPresentReturnsFalse() {
    testIsPresentBasedOnFlag((short) 0x0, false);
  }

  private void testIsPresentBasedOnFlag(short flag, boolean expected) {
    ZipCentralDirectoryFileHeaderRecord centralRecord = new ZipCentralDirectoryFileHeaderRecord(S0, S0, flag, S0,
            S0, S0, S0, S0, S0, S0, S0, S0, S0, S0, S0, S0);
    ZipLocalFileHeaderRecord localRecord = new ZipLocalFileHeaderRecord(S0, flag, S0, S0, S0, S0, S0, S0, S0, S0);
    assertThat(ZipDataDescriptorRecord.isPresentBasedOnFlag(flag)).isEqualTo(expected);
    assertThat(ZipDataDescriptorRecord.isPresentBasedOnFlag(centralRecord)).isEqualTo(expected);
    assertThat(ZipDataDescriptorRecord.isPresentBasedOnFlag(localRecord)).isEqualTo(expected);
  }

}
