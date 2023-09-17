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

package cn.taketoday.gradle.tasks.bundling;

import org.apache.commons.compress.archivers.zip.UnixStat;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.gradle.api.file.FileTreeElement;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import cn.taketoday.util.StreamUtils;

/**
 * Internal utility used to copy entries from the {@code infra-app-loader.jar}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class LoaderZipEntries {

  private final Long entryTime;

  LoaderZipEntries(Long entryTime) {
    this.entryTime = entryTime;
  }

  WrittenEntries writeTo(ZipArchiveOutputStream out) throws IOException {
    WrittenEntries written = new WrittenEntries();
    try (ZipInputStream loaderJar = new ZipInputStream(
            getClass().getResourceAsStream("/META-INF/loader/infra-app-loader.jar"))) {
      java.util.zip.ZipEntry entry = loaderJar.getNextEntry();
      while (entry != null) {
        if (entry.isDirectory() && !entry.getName().equals("META-INF/")) {
          writeDirectory(new ZipArchiveEntry(entry), out);
          written.addDirectory(entry);
        }
        else if (entry.getName().endsWith(".class")) {
          writeClass(new ZipArchiveEntry(entry), loaderJar, out);
          written.addFile(entry);
        }
        entry = loaderJar.getNextEntry();
      }
    }
    return written;
  }

  private void writeDirectory(ZipArchiveEntry entry, ZipArchiveOutputStream out) throws IOException {
    prepareEntry(entry, UnixStat.DIR_FLAG | UnixStat.DEFAULT_DIR_PERM);
    out.putArchiveEntry(entry);
    out.closeArchiveEntry();
  }

  private void writeClass(ZipArchiveEntry entry, ZipInputStream in, ZipArchiveOutputStream out) throws IOException {
    prepareEntry(entry, UnixStat.FILE_FLAG | UnixStat.DEFAULT_FILE_PERM);
    out.putArchiveEntry(entry);
    copy(in, out);
    out.closeArchiveEntry();
  }

  private void prepareEntry(ZipArchiveEntry entry, int unixMode) {
    if (this.entryTime != null) {
      entry.setTime(DefaultTimeZoneOffset.INSTANCE.removeFrom(this.entryTime));
    }
    entry.setUnixMode(unixMode);
  }

  private void copy(InputStream in, OutputStream out) throws IOException {
    StreamUtils.copy(in, out);
  }

  /**
   * Tracks entries that have been written.
   */
  static class WrittenEntries {

    private final Set<String> directories = new LinkedHashSet<>();

    private final Set<String> files = new LinkedHashSet<>();

    private void addDirectory(ZipEntry entry) {
      this.directories.add(entry.getName());
    }

    private void addFile(ZipEntry entry) {
      this.files.add(entry.getName());
    }

    boolean isWrittenDirectory(FileTreeElement element) {
      String path = element.getRelativePath().getPathString();
      if (element.isDirectory() && !path.endsWith(("/"))) {
        path += "/";
      }
      return this.directories.contains(path);
    }

    Set<String> getFiles() {
      return this.files;
    }

  }

}
