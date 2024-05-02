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

import java.nio.ByteBuffer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ByteArrayDataBlock}.
 *
 * @author Phillip Webb
 */
class ByteArrayDataBlockTests {

  private final byte[] BYTES = { 0, 1, 2, 3, 4, 5, 6, 7 };

  @Test
  void sizeReturnsByteArrayLength() throws Exception {
    ByteArrayDataBlock dataBlock = new ByteArrayDataBlock(this.BYTES);
    assertThat(dataBlock.size()).isEqualTo(this.BYTES.length);
  }

  @Test
  void readPutsBytes() throws Exception {
    ByteArrayDataBlock dataBlock = new ByteArrayDataBlock(this.BYTES);
    ByteBuffer dst = ByteBuffer.allocate(8);
    int result = dataBlock.read(dst, 0);
    assertThat(result).isEqualTo(8);
    assertThat(dst.array()).containsExactly(this.BYTES);
  }

  @Test
  void readWhenLessBytesThanRemainingInBufferPutsBytes() throws Exception {
    ByteArrayDataBlock dataBlock = new ByteArrayDataBlock(this.BYTES);
    ByteBuffer dst = ByteBuffer.allocate(9);
    int result = dataBlock.read(dst, 0);
    assertThat(result).isEqualTo(8);
    assertThat(dst.array()).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 0);
  }

  @Test
  void readWhenLessRemainingInBufferThanLengthPutsBytes() throws Exception {
    ByteArrayDataBlock dataBlock = new ByteArrayDataBlock(this.BYTES);
    ByteBuffer dst = ByteBuffer.allocate(7);
    int result = dataBlock.read(dst, 0);
    assertThat(result).isEqualTo(7);
    assertThat(dst.array()).containsExactly(0, 1, 2, 3, 4, 5, 6);
  }

  @Test
  void readWhenHasPosOffsetReadsBytes() throws Exception {
    ByteArrayDataBlock dataBlock = new ByteArrayDataBlock(this.BYTES);
    ByteBuffer dst = ByteBuffer.allocate(3);
    int result = dataBlock.read(dst, 4);
    assertThat(result).isEqualTo(3);
    assertThat(dst.array()).containsExactly(4, 5, 6);
  }

}
