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

package cn.taketoday.buildpack.platform.io;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;

import cn.taketoday.util.StreamUtils;

/**
 * {@link Layout} for writing TAR archive content directly to an {@link OutputStream}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class TarLayoutWriter implements Layout, Closeable {

  static final long NORMALIZED_MOD_TIME = TarArchive.NORMALIZED_TIME.toEpochMilli();

  private final TarArchiveOutputStream outputStream;

  TarLayoutWriter(OutputStream outputStream) {
    this.outputStream = new TarArchiveOutputStream(outputStream);
    this.outputStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
  }

  @Override
  public void directory(String name, Owner owner, int mode) throws IOException {
    this.outputStream.putArchiveEntry(createDirectoryEntry(name, owner, mode));
    this.outputStream.closeArchiveEntry();
  }

  @Override
  public void file(String name, Owner owner, int mode, Content content) throws IOException {
    this.outputStream.putArchiveEntry(createFileEntry(name, owner, mode, content.size()));
    content.writeTo(StreamUtils.nonClosing(this.outputStream));
    this.outputStream.closeArchiveEntry();
  }

  private TarArchiveEntry createDirectoryEntry(String name, Owner owner, int mode) {
    return createEntry(name, owner, TarConstants.LF_DIR, mode, 0);
  }

  private TarArchiveEntry createFileEntry(String name, Owner owner, int mode, int size) {
    return createEntry(name, owner, TarConstants.LF_NORMAL, mode, size);
  }

  private TarArchiveEntry createEntry(String name, Owner owner, byte linkFlag, int mode, int size) {
    TarArchiveEntry entry = new TarArchiveEntry(name, linkFlag, true);
    entry.setUserId(owner.getUid());
    entry.setGroupId(owner.getGid());
    entry.setMode(mode);
    entry.setModTime(NORMALIZED_MOD_TIME);
    entry.setSize(size);
    return entry;
  }

  void finish() throws IOException {
    this.outputStream.finish();
  }

  @Override
  public void close() throws IOException {
    this.outputStream.close();
  }

}
