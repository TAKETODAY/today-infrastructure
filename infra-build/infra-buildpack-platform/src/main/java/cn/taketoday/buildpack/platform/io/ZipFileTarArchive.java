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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

import cn.taketoday.lang.Assert;
import cn.taketoday.util.StreamUtils;

/**
 * Adapter class to convert a ZIP file to a {@link TarArchive}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 4.0
 */
public class ZipFileTarArchive implements TarArchive {

  static final long NORMALIZED_MOD_TIME = TarArchive.NORMALIZED_TIME.toEpochMilli();

  private final File zip;

  private final Owner owner;

  /**
   * Creates an archive from the contents of the given {@code zip}. Each entry in the
   * archive will be owned by the given {@code owner}.
   *
   * @param zip the zip to use as a source
   * @param owner the owner of the tar entries
   */
  public ZipFileTarArchive(File zip, Owner owner) {
    Assert.notNull(zip, "Zip is required");
    Assert.notNull(owner, "Owner is required");
    assertArchiveHasEntries(zip);
    this.zip = zip;
    this.owner = owner;
  }

  @Override
  public void writeTo(OutputStream outputStream) throws IOException {
    TarArchiveOutputStream tar = new TarArchiveOutputStream(outputStream);
    tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
    try (ZipFile zipFile = new ZipFile(this.zip)) {
      Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
      while (entries.hasMoreElements()) {
        ZipArchiveEntry zipEntry = entries.nextElement();
        copy(zipEntry, zipFile.getInputStream(zipEntry), tar);
      }
    }
    tar.finish();
  }

  private void assertArchiveHasEntries(File file) {
    try (ZipFile zipFile = new ZipFile(file)) {
      Assert.state(zipFile.getEntries().hasMoreElements(), () -> "Archive file '" + file + "' is not valid");
    }
    catch (IOException ex) {
      throw new IllegalStateException("File '" + file + "' is not readable", ex);
    }
  }

  private void copy(ZipArchiveEntry zipEntry, InputStream zip, TarArchiveOutputStream tar) throws IOException {
    TarArchiveEntry tarEntry = convert(zipEntry);
    tar.putArchiveEntry(tarEntry);
    if (tarEntry.isFile()) {
      StreamUtils.copyRange(zip, tar, 0, tarEntry.getSize());
    }
    tar.closeArchiveEntry();
  }

  private TarArchiveEntry convert(ZipArchiveEntry zipEntry) {
    byte linkFlag = (zipEntry.isDirectory()) ? TarConstants.LF_DIR : TarConstants.LF_NORMAL;
    TarArchiveEntry tarEntry = new TarArchiveEntry(zipEntry.getName(), linkFlag, true);
    tarEntry.setUserId(this.owner.getUid());
    tarEntry.setGroupId(this.owner.getGid());
    tarEntry.setModTime(NORMALIZED_MOD_TIME);
    tarEntry.setMode(zipEntry.getUnixMode());
    if (!zipEntry.isDirectory()) {
      tarEntry.setSize(zipEntry.getSize());
    }
    return tarEntry;
  }

}
