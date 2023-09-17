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

package cn.taketoday.app.loader.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.attribute.FileTime;
import java.util.jar.JarFile;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Utility class that can be used to repackage an archive so that it can be executed using
 * '{@literal java -jar}'.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Madhura Bhave
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class Repackager extends Packager {

  private boolean backupSource = true;

  /**
   * Create a new {@link Repackager} instance.
   *
   * @param source the source archive file to package
   */
  public Repackager(File source) {
    super(source);
  }

  /**
   * Sets if source files should be backed up when they would be overwritten.
   *
   * @param backupSource if source files should be backed up
   */
  public void setBackupSource(boolean backupSource) {
    this.backupSource = backupSource;
  }

  /**
   * Repackage the source file so that it can be run using '{@literal java -jar}'.
   *
   * @param libraries the libraries required to run the archive
   * @throws IOException if the file cannot be repackaged
   */
  public void repackage(Libraries libraries) throws IOException {
    repackage(getSource(), libraries);
  }

  /**
   * Repackage to the given destination so that it can be launched using '
   * {@literal java -jar}'.
   *
   * @param destination the destination file (may be the same as the source)
   * @param libraries the libraries required to run the archive
   * @throws IOException if the file cannot be repackaged
   */
  public void repackage(File destination, Libraries libraries) throws IOException {
    repackage(destination, libraries, null);
  }

  /**
   * Repackage to the given destination so that it can be launched using '
   * {@literal java -jar}'.
   *
   * @param destination the destination file (may be the same as the source)
   * @param libraries the libraries required to run the archive
   * @param launchScript an optional launch script prepended to the front of the jar
   * @throws IOException if the file cannot be repackaged
   */
  public void repackage(File destination, Libraries libraries, @Nullable LaunchScript launchScript) throws IOException {
    repackage(destination, libraries, launchScript, null);
  }

  /**
   * Repackage to the given destination so that it can be launched using '
   * {@literal java -jar}'.
   *
   * @param destination the destination file (may be the same as the source)
   * @param libraries the libraries required to run the archive
   * @param launchScript an optional launch script prepended to the front of the jar
   * @param lastModifiedTime an optional last modified time to apply to the archive and
   * its contents
   * @throws IOException if the file cannot be repackaged
   */
  public void repackage(File destination, Libraries libraries,
          @Nullable LaunchScript launchScript, @Nullable FileTime lastModifiedTime) throws IOException {
    Assert.isTrue(destination != null && !destination.isDirectory(), "Invalid destination");
    getLayout(); // get layout early
    destination = destination.getAbsoluteFile();
    File source = getSource();
    if (isAlreadyPackaged() && source.equals(destination)) {
      return;
    }
    File workingSource = source;
    if (source.equals(destination)) {
      workingSource = getBackupFile();
      workingSource.delete();
      renameFile(source, workingSource);
    }
    destination.delete();
    try {
      try (JarFile sourceJar = new JarFile(workingSource)) {
        repackage(sourceJar, destination, libraries, launchScript, lastModifiedTime);
      }
    }
    finally {
      if (!this.backupSource && !source.equals(workingSource)) {
        deleteFile(workingSource);
      }
    }
  }

  private void repackage(JarFile sourceJar, File destination, Libraries libraries,
          @Nullable LaunchScript launchScript, @Nullable FileTime lastModifiedTime) throws IOException {
    try (JarWriter writer = new JarWriter(destination, launchScript, lastModifiedTime)) {
      write(sourceJar, libraries, writer, lastModifiedTime != null);
    }
    if (lastModifiedTime != null) {
      destination.setLastModified(lastModifiedTime.toMillis());
    }
  }

  private void renameFile(File file, File dest) {
    if (!file.renameTo(dest)) {
      throw new IllegalStateException("Unable to rename '" + file + "' to '" + dest + "'");
    }
  }

  private void deleteFile(File file) {
    if (!file.delete()) {
      throw new IllegalStateException("Unable to delete '" + file + "'");
    }
  }

}
