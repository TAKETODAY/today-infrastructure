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
 * A ZIP File "Local file header record" (LFH).
 *
 * @param versionNeededToExtract the version needed to extract the zip
 * @param generalPurposeBitFlag the general purpose bit flag
 * @param compressionMethod the compression method used for this entry
 * @param lastModFileTime the last modified file time
 * @param lastModFileDate the last modified file date
 * @param crc32 the CRC32 checksum
 * @param compressedSize the size of the entry when compressed
 * @param uncompressedSize the size of the entry when uncompressed
 * @param fileNameLength the file name length
 * @param extraFieldLength the extra field length
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see <a href="https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT">Chapter
 * 4.3.7 of the Zip File Format Specification</a>
 * @since 5.0
 */
record ZipLocalFileHeaderRecord(short versionNeededToExtract, short generalPurposeBitFlag, short compressionMethod,
        short lastModFileTime, short lastModFileDate, int crc32, int compressedSize, int uncompressedSize,
        short fileNameLength, short extraFieldLength) {

  private static final DebugLogger debug = DebugLogger.get(ZipLocalFileHeaderRecord.class);

  private static final int SIGNATURE = 0x04034b50;

  private static final int MINIMUM_SIZE = 30;

  /**
   * Return the size of this record.
   *
   * @return the record size
   */
  long size() {
    return MINIMUM_SIZE + fileNameLength() + extraFieldLength();
  }

  /**
   * Return a new {@link ZipLocalFileHeaderRecord} with a new
   * {@link #extraFieldLength()}.
   *
   * @param extraFieldLength the new extra field length
   * @return a new {@link ZipLocalFileHeaderRecord} instance
   */
  ZipLocalFileHeaderRecord withExtraFieldLength(short extraFieldLength) {
    return new ZipLocalFileHeaderRecord(this.versionNeededToExtract, this.generalPurposeBitFlag,
            this.compressionMethod, this.lastModFileTime, this.lastModFileDate, this.crc32, this.compressedSize,
            this.uncompressedSize, this.fileNameLength, extraFieldLength);
  }

  /**
   * Return a new {@link ZipLocalFileHeaderRecord} with a new {@link #fileNameLength()}.
   *
   * @param fileNameLength the new file name length
   * @return a new {@link ZipLocalFileHeaderRecord} instance
   */
  ZipLocalFileHeaderRecord withFileNameLength(short fileNameLength) {
    return new ZipLocalFileHeaderRecord(this.versionNeededToExtract, this.generalPurposeBitFlag,
            this.compressionMethod, this.lastModFileTime, this.lastModFileDate, this.crc32, this.compressedSize,
            this.uncompressedSize, fileNameLength, this.extraFieldLength);
  }

  /**
   * Return the contents of this record as a byte array suitable for writing to a zip.
   *
   * @return the record as a byte array
   */
  byte[] asByteArray() {
    ByteBuffer buffer = ByteBuffer.allocate(MINIMUM_SIZE);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(SIGNATURE);
    buffer.putShort(this.versionNeededToExtract);
    buffer.putShort(this.generalPurposeBitFlag);
    buffer.putShort(this.compressionMethod);
    buffer.putShort(this.lastModFileTime);
    buffer.putShort(this.lastModFileDate);
    buffer.putInt(this.crc32);
    buffer.putInt(this.compressedSize);
    buffer.putInt(this.uncompressedSize);
    buffer.putShort(this.fileNameLength);
    buffer.putShort(this.extraFieldLength);
    return buffer.array();
  }

  /**
   * Load the {@link ZipLocalFileHeaderRecord} from the given data block.
   *
   * @param dataBlock the source data block
   * @param pos the position of the record
   * @return a new {@link ZipLocalFileHeaderRecord} instance
   * @throws IOException on I/O error
   */
  static ZipLocalFileHeaderRecord load(DataBlock dataBlock, long pos) throws IOException {
    debug.log("Loading LocalFileHeaderRecord from position %s", pos);
    ByteBuffer buffer = ByteBuffer.allocate(MINIMUM_SIZE);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    dataBlock.readFully(buffer, pos);
    buffer.rewind();
    if (buffer.getInt() != SIGNATURE) {
      throw new IOException("Zip 'Local File Header Record' not found at position " + pos);
    }
    return new ZipLocalFileHeaderRecord(buffer.getShort(), buffer.getShort(), buffer.getShort(), buffer.getShort(),
            buffer.getShort(), buffer.getInt(), buffer.getInt(), buffer.getInt(), buffer.getShort(),
            buffer.getShort());
  }

}
