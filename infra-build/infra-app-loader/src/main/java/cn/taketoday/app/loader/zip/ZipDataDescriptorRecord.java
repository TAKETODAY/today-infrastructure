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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import cn.taketoday.app.loader.log.DebugLogger;

/**
 * A ZIP File "Data Descriptor" record.
 *
 * @param includeSignature if the signature bytes are written or not (see note in spec)
 * @param crc32 the CRC32 checksum
 * @param compressedSize the size of the entry when compressed
 * @param uncompressedSize the size of the entry when uncompressed
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see <a href="https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT">Chapter
 * 4.3.9 of the Zip File Format Specification</a>
 * @since 5.0
 */
record ZipDataDescriptorRecord(boolean includeSignature, int crc32, int compressedSize, int uncompressedSize) {

  private static final DebugLogger debug = DebugLogger.get(ZipDataDescriptorRecord.class);

  private static final int SIGNATURE = 0x08074b50;

  private static final int DATA_SIZE = 12;

  private static final int SIGNATURE_SIZE = 4;

  long size() {
    return (!includeSignature()) ? DATA_SIZE : DATA_SIZE + SIGNATURE_SIZE;
  }

  /**
   * Return the contents of this record as a byte array suitable for writing to a zip.
   *
   * @return the record as a byte array
   */
  byte[] asByteArray() {
    ByteBuffer buffer = ByteBuffer.allocate((int) size());
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    if (this.includeSignature) {
      buffer.putInt(SIGNATURE);
    }
    buffer.putInt(this.crc32);
    buffer.putInt(this.compressedSize);
    buffer.putInt(this.uncompressedSize);
    return buffer.array();
  }

  /**
   * Load the {@link ZipDataDescriptorRecord} from the given data block.
   *
   * @param dataBlock the source data block
   * @param pos the position of the record
   * @return a new {@link ZipLocalFileHeaderRecord} instance
   * @throws IOException on I/O error
   */
  static ZipDataDescriptorRecord load(DataBlock dataBlock, long pos) throws IOException {
    debug.log("Loading ZipDataDescriptorRecord from position %s", pos);
    ByteBuffer buffer = ByteBuffer.allocate(SIGNATURE_SIZE + DATA_SIZE);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.limit(SIGNATURE_SIZE);
    dataBlock.readFully(buffer, pos);
    buffer.rewind();
    int signatureOrCrc = buffer.getInt();
    boolean hasSignature = (signatureOrCrc == SIGNATURE);
    buffer.rewind();
    buffer.limit((!hasSignature) ? DATA_SIZE - SIGNATURE_SIZE : DATA_SIZE);
    dataBlock.readFully(buffer, pos + SIGNATURE_SIZE);
    buffer.rewind();
    return new ZipDataDescriptorRecord(hasSignature, (!hasSignature) ? signatureOrCrc : buffer.getInt(),
            buffer.getInt(), buffer.getInt());
  }

  /**
   * Return if the {@link ZipDataDescriptorRecord} is present based on the general
   * purpose bit flag in the given {@link ZipLocalFileHeaderRecord}.
   *
   * @param localRecord the local record to check
   * @return if the bit flag is set
   */
  static boolean isPresentBasedOnFlag(ZipLocalFileHeaderRecord localRecord) {
    return isPresentBasedOnFlag(localRecord.generalPurposeBitFlag());
  }

  /**
   * Return if the {@link ZipDataDescriptorRecord} is present based on the general
   * purpose bit flag in the given {@link ZipCentralDirectoryFileHeaderRecord}.
   *
   * @param centralRecord the central record to check
   * @return if the bit flag is set
   */
  static boolean isPresentBasedOnFlag(ZipCentralDirectoryFileHeaderRecord centralRecord) {
    return isPresentBasedOnFlag(centralRecord.generalPurposeBitFlag());
  }

  /**
   * Return if the {@link ZipDataDescriptorRecord} is present based on the given general
   * purpose bit flag.
   *
   * @param generalPurposeBitFlag the general purpose bit flag to check
   * @return if the bit flag is set
   */
  static boolean isPresentBasedOnFlag(int generalPurposeBitFlag) {
    return (generalPurposeBitFlag & 0b0000_1000) != 0;
  }

}
