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
import static org.assertj.core.api.Assertions.assertThatIOException;

/**
 * Tests for {@link Zip64EndOfCentralDirectoryRecord}.
 *
 * @author Phillip Webb
 */
class Zip64EndOfCentralDirectoryRecordTests {

  @Test
  void loadLoadsData() throws Exception {
    DataBlock dataBlock = new ByteArrayDataBlock(new byte[] { //
            0x50, 0x4b, 0x06, 0x06, //
            0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, //
            0x02, 0x00, //
            0x03, 0x00, //
            0x04, 0x00, 0x00, 0x00, //
            0x05, 0x00, 0x00, 0x00, //
            0x06, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, //
            0x07, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, //
            0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, //
            0x09, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 }); //
    Zip64EndOfCentralDirectoryLocator locator = new Zip64EndOfCentralDirectoryLocator(56, 0, 0, 0);
    Zip64EndOfCentralDirectoryRecord eocd = Zip64EndOfCentralDirectoryRecord.load(dataBlock, locator);
    assertThat(eocd.size()).isEqualTo(56);
    assertThat(eocd.sizeOfZip64EndOfCentralDirectoryRecord()).isEqualTo(1);
    assertThat(eocd.versionMadeBy()).isEqualTo((short) 2);
    assertThat(eocd.versionNeededToExtract()).isEqualTo((short) 3);
    assertThat(eocd.numberOfThisDisk()).isEqualTo(4);
    assertThat(eocd.diskWhereCentralDirectoryStarts()).isEqualTo(5);
    assertThat(eocd.numberOfCentralDirectoryEntriesOnThisDisk()).isEqualTo(6);
    assertThat(eocd.totalNumberOfCentralDirectoryEntries()).isEqualTo(7);
    assertThat(eocd.sizeOfCentralDirectory()).isEqualTo(8);
    assertThat(eocd.offsetToStartOfCentralDirectory());
  }

  @Test
  void loadWhenSignatureDoesNotMatchThrowsException() {
    DataBlock dataBlock = new ByteArrayDataBlock(new byte[] { //
            0x51, 0x4b, 0x06, 0x06, //
            0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, //
            0x02, 0x00, //
            0x03, 0x00, //
            0x04, 0x00, 0x00, 0x00, //
            0x05, 0x00, 0x00, 0x00, //
            0x06, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, //
            0x07, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, //
            0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, //
            0x09, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 }); //
    Zip64EndOfCentralDirectoryLocator locator = new Zip64EndOfCentralDirectoryLocator(56, 0, 0, 0);
    assertThatIOException().isThrownBy(() -> Zip64EndOfCentralDirectoryRecord.load(dataBlock, locator))
            .withMessageContaining("Zip64 'End Of Central Directory Record' not found at position");
  }

}
