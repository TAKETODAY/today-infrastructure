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
import java.util.function.BiConsumer;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Utility class that can be used to export a fully packaged archive to an OCI image.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ImagePackager extends Packager {

  /**
   * Create a new {@link ImagePackager} instance.
   *
   * @param source the source file to package
   * @param backupFile the backup of the source file to package
   */
  public ImagePackager(File source, File backupFile) {
    super(source);
    setBackupFile(backupFile);
    if (isAlreadyPackaged()) {
      Assert.isTrue(getBackupFile().exists() && getBackupFile().isFile(),
              "Original source '" + getBackupFile() + "' is required for building an image");
      Assert.state(!isAlreadyPackaged(getBackupFile()),
              () -> "Repackaged archive file " + source + " cannot be used to build an image");
    }
  }

  /**
   * Create a packaged image.
   *
   * @param libraries the contained libraries
   * @param exporter the exporter used to write the image
   * @throws IOException on IO error
   */
  public void packageImage(Libraries libraries, BiConsumer<ZipEntry, EntryWriter> exporter) throws IOException {
    packageImage(libraries, new DelegatingJarWriter(exporter));
  }

  private void packageImage(Libraries libraries, AbstractJarWriter writer) throws IOException {
    File source = isAlreadyPackaged() ? getBackupFile() : getSource();
    try (JarFile sourceJar = new JarFile(source)) {
      write(sourceJar, libraries, writer);
    }
  }

  /**
   * {@link AbstractJarWriter} that delegates to a {@link BiConsumer}.
   */
  private static class DelegatingJarWriter extends AbstractJarWriter {

    private final BiConsumer<ZipEntry, EntryWriter> exporter;

    DelegatingJarWriter(BiConsumer<ZipEntry, EntryWriter> exporter) {
      this.exporter = exporter;
    }

    @Override
    protected void writeToArchive(ZipEntry entry, @Nullable EntryWriter entryWriter) throws IOException {
      this.exporter.accept(entry, entryWriter);
    }

  }

}
