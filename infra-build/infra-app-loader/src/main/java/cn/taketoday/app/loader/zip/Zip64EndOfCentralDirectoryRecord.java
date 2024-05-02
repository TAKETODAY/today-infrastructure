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
 * A Zip64 end of central directory record.
 *
 * @param size the size of this record
 * @param sizeOfZip64EndOfCentralDirectoryRecord the size of zip64 end of central
 * directory record
 * @param versionMadeBy the version that made the zip
 * @param versionNeededToExtract the version needed to extract the zip
 * @param numberOfThisDisk the number of this disk
 * @param diskWhereCentralDirectoryStarts the disk where central directory starts
 * @param numberOfCentralDirectoryEntriesOnThisDisk the number of central directory
 * entries on this disk
 * @param totalNumberOfCentralDirectoryEntries the total number of central directory
 * entries
 * @param sizeOfCentralDirectory the size of central directory (bytes)
 * @param offsetToStartOfCentralDirectory the offset of start of central directory,
 * relative to start of archive
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see <a href="https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT">Chapter
 * 4.3.14 of the Zip File Format Specification</a>
 * @since 5.0
 */
record Zip64EndOfCentralDirectoryRecord(long size, long sizeOfZip64EndOfCentralDirectoryRecord, short versionMadeBy,
        short versionNeededToExtract, int numberOfThisDisk, int diskWhereCentralDirectoryStarts,
        long numberOfCentralDirectoryEntriesOnThisDisk, long totalNumberOfCentralDirectoryEntries,
        long sizeOfCentralDirectory, long offsetToStartOfCentralDirectory) {

  private static final DebugLogger debug = DebugLogger.get(Zip64EndOfCentralDirectoryRecord.class);

  private static final int SIGNATURE = 0x06064b50;

  private static final int MINIMUM_SIZE = 56;

  /**
   * Load the {@link Zip64EndOfCentralDirectoryRecord} from the given data block based
   * on the offset given in the locator.
   *
   * @param dataBlock the source data block
   * @param locator the {@link Zip64EndOfCentralDirectoryLocator} or {@code null}
   * @return a new {@link ZipCentralDirectoryFileHeaderRecord} instance or {@code null}
   * if the locator is {@code null}
   * @throws IOException on I/O error
   */
  @Nullable
  static Zip64EndOfCentralDirectoryRecord load(DataBlock dataBlock, @Nullable Zip64EndOfCentralDirectoryLocator locator)
          throws IOException {
    if (locator == null) {
      return null;
    }
    ByteBuffer buffer = ByteBuffer.allocate(MINIMUM_SIZE);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    long size = locator.pos() - locator.offsetToZip64EndOfCentralDirectoryRecord();
    long pos = locator.pos() - size;
    debug.log("Loading Zip64EndOfCentralDirectoryRecord from position %s size %s", pos, size);
    dataBlock.readFully(buffer, pos);
    buffer.rewind();
    int signature = buffer.getInt();
    if (signature != SIGNATURE) {
      debug.log("Found incorrect Zip64EndOfCentralDirectoryRecord signature %s at position %s", signature, pos);
      throw new IOException("Zip64 'End Of Central Directory Record' not found at position " + pos
              + ". Zip file is corrupt or includes prefixed bytes which are not supported with Zip64 files");
    }
    return new Zip64EndOfCentralDirectoryRecord(size, buffer.getLong(), buffer.getShort(), buffer.getShort(),
            buffer.getInt(), buffer.getInt(), buffer.getLong(), buffer.getLong(), buffer.getLong(),
            buffer.getLong());
  }

}
