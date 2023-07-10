/*
 * Copyright 2012 - 2023 the original author or authors.
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

package cn.taketoday.app.loader.tools;

import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveOutputStream;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.attribute.FileTime;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

import cn.taketoday.lang.Nullable;

/**
 * Writes JAR content, ensuring valid directory entries are always created and duplicate
 * items are ignored.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class JarWriter extends AbstractJarWriter implements AutoCloseable {

  private final JarArchiveOutputStream jarOutputStream;

  @Nullable
  private final FileTime lastModifiedTime;

  /**
   * Create a new {@link JarWriter} instance.
   *
   * @param file the file to write
   * @throws IOException if the file cannot be opened
   * @throws FileNotFoundException if the file cannot be found
   */
  public JarWriter(File file) throws FileNotFoundException, IOException {
    this(file, null);
  }

  /**
   * Create a new {@link JarWriter} instance.
   *
   * @param file the file to write
   * @param launchScript an optional launch script to prepend to the front of the jar
   * @throws IOException if the file cannot be opened
   * @throws FileNotFoundException if the file cannot be found
   */
  public JarWriter(File file, @Nullable LaunchScript launchScript) throws FileNotFoundException, IOException {
    this(file, launchScript, null);
  }

  /**
   * Create a new {@link JarWriter} instance.
   *
   * @param file the file to write
   * @param launchScript an optional launch script to prepend to the front of the jar
   * @param lastModifiedTime an optional last modified time to apply to the written
   * entries
   * @throws IOException if the file cannot be opened
   * @throws FileNotFoundException if the file cannot be found
   */
  public JarWriter(File file, @Nullable LaunchScript launchScript, @Nullable FileTime lastModifiedTime)
          throws FileNotFoundException, IOException {
    this.jarOutputStream = new JarArchiveOutputStream(new FileOutputStream(file));
    if (launchScript != null) {
      this.jarOutputStream.writePreamble(launchScript.toByteArray());
      file.setExecutable(true);
    }
    this.jarOutputStream.setEncoding("UTF-8");
    this.lastModifiedTime = lastModifiedTime;
  }

  @Override
  protected void writeToArchive(ZipEntry entry, @Nullable EntryWriter entryWriter) throws IOException {
    JarArchiveEntry jarEntry = asJarArchiveEntry(entry);
    if (this.lastModifiedTime != null) {
      jarEntry.setTime(DefaultTimeZoneOffset.INSTANCE.removeFrom(this.lastModifiedTime).toMillis());
    }
    this.jarOutputStream.putArchiveEntry(jarEntry);
    if (entryWriter != null) {
      entryWriter.write(this.jarOutputStream);
    }
    this.jarOutputStream.closeArchiveEntry();
  }

  private JarArchiveEntry asJarArchiveEntry(ZipEntry entry) throws ZipException {
    if (entry instanceof JarArchiveEntry jarArchiveEntry) {
      return jarArchiveEntry;
    }
    return new JarArchiveEntry(entry);
  }

  /**
   * Close the writer.
   *
   * @throws IOException if the file cannot be closed
   */
  @Override
  public void close() throws IOException {
    this.jarOutputStream.close();
  }

}
