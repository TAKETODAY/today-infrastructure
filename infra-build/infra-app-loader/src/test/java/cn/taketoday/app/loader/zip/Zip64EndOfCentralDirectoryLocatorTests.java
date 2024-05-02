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

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Zip64EndOfCentralDirectoryLocator}.
 *
 * @author Phillip Webb
 */
class Zip64EndOfCentralDirectoryLocatorTests {

  @Test
  void findReturnsRecord() throws Exception {
    DataBlock dataBlock = new ByteArrayDataBlock(new byte[] { //
            0x50, 0x4b, 0x06, 0x07, //
            0x01, 0x00, 0x00, 0x00, //
            0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, //
            0x03, 0x00, 0x00, 0x00 }); //
    Zip64EndOfCentralDirectoryLocator eocd = Zip64EndOfCentralDirectoryLocator.find(dataBlock, 20);
    assertThat(eocd.pos()).isEqualTo(0);
    assertThat(eocd.numberOfThisDisk()).isEqualTo(1);
    assertThat(eocd.offsetToZip64EndOfCentralDirectoryRecord()).isEqualTo(2);
    assertThat(eocd.totalNumberOfDisks()).isEqualTo(3);
  }

  @Test
  void findWhenSignatureDoesNotMatchReturnsNull() throws IOException {
    DataBlock dataBlock = new ByteArrayDataBlock(new byte[] { //
            0x51, 0x4b, 0x06, 0x07, //
            0x01, 0x00, 0x00, 0x00, //
            0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, //
            0x03, 0x00, 0x00, 0x00 }); //
    Zip64EndOfCentralDirectoryLocator eocd = Zip64EndOfCentralDirectoryLocator.find(dataBlock, 20);
    assertThat(eocd).isNull();

  }

}
