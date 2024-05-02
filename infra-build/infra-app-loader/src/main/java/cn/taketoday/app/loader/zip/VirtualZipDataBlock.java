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
import java.nio.file.FileSystem;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.lang.Nullable;

/**
 * {@link DataBlock} that creates a virtual zip. This class allows us to create virtual
 * zip files that can be parsed by regular JDK classes such as the zip {@link FileSystem}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class VirtualZipDataBlock extends VirtualDataBlock implements CloseableDataBlock {

  private final CloseableDataBlock data;

  /**
   * Create a new {@link VirtualZipDataBlock} for the given entries.
   *
   * @param data the source zip data
   * @param nameOffsetLookups the name offsets to apply
   * @param centralRecords the records that should be copied to the virtual zip
   * @param centralRecordPositions the record positions in the data block.
   * @throws IOException on I/O error
   */
  VirtualZipDataBlock(CloseableDataBlock data, NameOffsetLookups nameOffsetLookups,
          ZipCentralDirectoryFileHeaderRecord[] centralRecords, long[] centralRecordPositions) throws IOException {
    this.data = data;
    List<DataBlock> parts = new ArrayList<>();
    List<DataBlock> centralParts = new ArrayList<>();
    long offset = 0;
    long sizeOfCentralDirectory = 0;
    for (int i = 0; i < centralRecords.length; i++) {
      ZipCentralDirectoryFileHeaderRecord centralRecord = centralRecords[i];
      int nameOffset = nameOffsetLookups.get(i);
      long centralRecordPos = centralRecordPositions[i];
      DataBlock name = new DataPart(
              centralRecordPos + ZipCentralDirectoryFileHeaderRecord.FILE_NAME_OFFSET + nameOffset,
              Short.toUnsignedLong(centralRecord.fileNameLength()) - nameOffset);
      long localRecordPos = Integer.toUnsignedLong(centralRecord.offsetToLocalHeader());
      ZipLocalFileHeaderRecord localRecord = ZipLocalFileHeaderRecord.load(this.data, localRecordPos);
      DataBlock content = new DataPart(localRecordPos + localRecord.size(), centralRecord.compressedSize());
      boolean hasDescriptorRecord = ZipDataDescriptorRecord.isPresentBasedOnFlag(centralRecord);
      ZipDataDescriptorRecord dataDescriptorRecord = (!hasDescriptorRecord) ? null
              : ZipDataDescriptorRecord.load(data, localRecordPos + localRecord.size() + content.size());
      sizeOfCentralDirectory += addToCentral(centralParts, centralRecord, centralRecordPos, name, (int) offset);
      offset += addToLocal(parts, centralRecord, localRecord, dataDescriptorRecord, name, content);
    }
    parts.addAll(centralParts);
    ZipEndOfCentralDirectoryRecord eocd = new ZipEndOfCentralDirectoryRecord((short) centralRecords.length,
            (int) sizeOfCentralDirectory, (int) offset);
    parts.add(new ByteArrayDataBlock(eocd.asByteArray()));
    setParts(parts);
  }

  private long addToCentral(List<DataBlock> parts, ZipCentralDirectoryFileHeaderRecord originalRecord,
          long originalRecordPos, DataBlock name, int offsetToLocalHeader) throws IOException {
    ZipCentralDirectoryFileHeaderRecord record = originalRecord.withFileNameLength((short) (name.size() & 0xFFFF))
            .withOffsetToLocalHeader(offsetToLocalHeader);
    int originalExtraFieldLength = Short.toUnsignedInt(originalRecord.extraFieldLength());
    int originalFileCommentLength = Short.toUnsignedInt(originalRecord.fileCommentLength());
    DataBlock extraFieldAndComment = new DataPart(
            originalRecordPos + originalRecord.size() - originalExtraFieldLength - originalFileCommentLength,
            originalExtraFieldLength + originalFileCommentLength);
    parts.add(new ByteArrayDataBlock(record.asByteArray()));
    parts.add(name);
    parts.add(extraFieldAndComment);
    return record.size();
  }

  private long addToLocal(List<DataBlock> parts, ZipCentralDirectoryFileHeaderRecord centralRecord,
          ZipLocalFileHeaderRecord originalRecord, @Nullable ZipDataDescriptorRecord dataDescriptorRecord, DataBlock name,
          DataBlock content) throws IOException {
    ZipLocalFileHeaderRecord record = originalRecord.withFileNameLength((short) (name.size() & 0xFFFF));
    long originalRecordPos = Integer.toUnsignedLong(centralRecord.offsetToLocalHeader());
    int extraFieldLength = Short.toUnsignedInt(originalRecord.extraFieldLength());
    parts.add(new ByteArrayDataBlock(record.asByteArray()));
    parts.add(name);
    parts.add(new DataPart(originalRecordPos + originalRecord.size() - extraFieldLength, extraFieldLength));
    parts.add(content);
    if (dataDescriptorRecord != null) {
      parts.add(new ByteArrayDataBlock(dataDescriptorRecord.asByteArray()));
    }
    return record.size() + content.size() + ((dataDescriptorRecord != null) ? dataDescriptorRecord.size() : 0);
  }

  @Override
  public void close() throws IOException {
    this.data.close();
  }

  /**
   * {@link DataBlock} that points to part of the original data block.
   */
  final class DataPart implements DataBlock {

    private final long offset;

    private final long size;

    DataPart(long offset, long size) {
      this.offset = offset;
      this.size = size;
    }

    @Override
    public long size() throws IOException {
      return this.size;
    }

    @Override
    public int read(ByteBuffer dst, long pos) throws IOException {
      int remaining = (int) (this.size - pos);
      if (remaining <= 0) {
        return -1;
      }
      int originalLimit = -1;
      if (dst.remaining() > remaining) {
        originalLimit = dst.limit();
        dst.limit(dst.position() + remaining);
      }
      int result = VirtualZipDataBlock.this.data.read(dst, this.offset + pos);
      if (originalLimit != -1) {
        dst.limit(originalLimit);
      }
      return result;
    }

  }

}
