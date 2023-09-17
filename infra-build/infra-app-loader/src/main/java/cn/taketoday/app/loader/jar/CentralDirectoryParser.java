/*
 * Copyright 2017 - 2023 the original author or authors.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.app.loader.jar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.app.loader.data.RandomAccessData;

/**
 * Parses the central directory from a JAR file.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see CentralDirectoryVisitor
 * @since 4.0
 */
class CentralDirectoryParser {

  private static final int CENTRAL_DIRECTORY_HEADER_BASE_SIZE = 46;

  private final List<CentralDirectoryVisitor> visitors = new ArrayList<>();

  <T extends CentralDirectoryVisitor> T addVisitor(T visitor) {
    this.visitors.add(visitor);
    return visitor;
  }

  /**
   * Parse the source data, triggering {@link CentralDirectoryVisitor visitors}.
   *
   * @param data the source data
   * @param skipPrefixBytes if prefix bytes should be skipped
   * @return the actual archive data without any prefix bytes
   * @throws IOException on error
   */
  RandomAccessData parse(RandomAccessData data, boolean skipPrefixBytes) throws IOException {
    CentralDirectoryEndRecord endRecord = new CentralDirectoryEndRecord(data);
    if (skipPrefixBytes) {
      data = getArchiveData(endRecord, data);
    }
    RandomAccessData centralDirectoryData = endRecord.getCentralDirectory(data);
    visitStart(endRecord, centralDirectoryData);
    parseEntries(endRecord, centralDirectoryData);
    visitEnd();
    return data;
  }

  private void parseEntries(CentralDirectoryEndRecord endRecord, RandomAccessData centralDirectoryData)
          throws IOException {
    byte[] bytes = centralDirectoryData.read(0, centralDirectoryData.getSize());
    CentralDirectoryFileHeader fileHeader = new CentralDirectoryFileHeader();
    int dataOffset = 0;
    for (int i = 0; i < endRecord.getNumberOfRecords(); i++) {
      fileHeader.load(bytes, dataOffset, null, 0, null);
      visitFileHeader(dataOffset, fileHeader);
      dataOffset += CENTRAL_DIRECTORY_HEADER_BASE_SIZE + fileHeader.getName().length()
              + fileHeader.getComment().length() + fileHeader.getExtra().length;
    }
  }

  private RandomAccessData getArchiveData(CentralDirectoryEndRecord endRecord, RandomAccessData data) {
    long offset = endRecord.getStartOfArchive(data);
    if (offset == 0) {
      return data;
    }
    return data.getSubsection(offset, data.getSize() - offset);
  }

  private void visitStart(CentralDirectoryEndRecord endRecord, RandomAccessData centralDirectoryData) {
    for (CentralDirectoryVisitor visitor : this.visitors) {
      visitor.visitStart(endRecord, centralDirectoryData);
    }
  }

  private void visitFileHeader(long dataOffset, CentralDirectoryFileHeader fileHeader) {
    for (CentralDirectoryVisitor visitor : this.visitors) {
      visitor.visitFileHeader(fileHeader, dataOffset);
    }
  }

  private void visitEnd() {
    for (CentralDirectoryVisitor visitor : this.visitors) {
      visitor.visitEnd();
    }
  }

}
