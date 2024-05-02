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
import cn.taketoday.lang.Nullable;

/**
 * A Zip64 end of central directory locator.
 *
 * @param pos the position where this record begins in the source {@link DataBlock}
 * @param numberOfThisDisk the number of the disk with the start of the zip64 end of
 * central directory
 * @param offsetToZip64EndOfCentralDirectoryRecord the relative offset of the zip64 end of
 * central directory record
 * @param totalNumberOfDisks the total number of disks
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see <a href="https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT">Chapter
 * 4.3.15 of the Zip File Format Specification</a>
 * @since 5.0
 */
record Zip64EndOfCentralDirectoryLocator(long pos, int numberOfThisDisk, long offsetToZip64EndOfCentralDirectoryRecord,
        int totalNumberOfDisks) {

  private static final DebugLogger debug = DebugLogger.get(Zip64EndOfCentralDirectoryLocator.class);

  private static final int SIGNATURE = 0x07064b50;

  /**
   * The size of this record.
   */
  static final int SIZE = 20;

  /**
   * Return the {@link Zip64EndOfCentralDirectoryLocator} or {@code null} if this is not
   * a Zip64 file.
   *
   * @param dataBlock the source data block
   * @param endOfCentralDirectoryPos the {@link ZipEndOfCentralDirectoryRecord} position
   * @return a {@link Zip64EndOfCentralDirectoryLocator} instance or null
   * @throws IOException on I/O error
   */
  @Nullable
  static Zip64EndOfCentralDirectoryLocator find(DataBlock dataBlock, long endOfCentralDirectoryPos)
          throws IOException {
    debug.log("Finding Zip64EndOfCentralDirectoryLocator from EOCD at %s", endOfCentralDirectoryPos);
    long pos = endOfCentralDirectoryPos - SIZE;
    if (pos < 0) {
      debug.log("No Zip64EndOfCentralDirectoryLocator due to negative position %s", pos);
      return null;
    }
    ByteBuffer buffer = ByteBuffer.allocate(SIZE);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    dataBlock.read(buffer, pos);
    buffer.rewind();
    int signature = buffer.getInt();
    if (signature != SIGNATURE) {
      debug.log("Found incorrect Zip64EndOfCentralDirectoryLocator signature %s at position %s", signature, pos);
      return null;
    }
    debug.log("Found Zip64EndOfCentralDirectoryLocator at position %s", pos);
    return new Zip64EndOfCentralDirectoryLocator(pos, buffer.getInt(), buffer.getLong(), buffer.getInt());
  }

}
