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

package infra.app.loader.net.protocol.jar;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import infra.app.loader.ref.Cleaner;

/**
 * A {@link JarFile} subclass returned from a {@link JarUrlConnection}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class UrlJarFile extends JarFile {

  private final UrlJarManifest manifest;

  private final Consumer<JarFile> closeAction;

  UrlJarFile(File file, Runtime.Version version, Consumer<JarFile> closeAction) throws IOException {
    super(file, true, ZipFile.OPEN_READ, version);
    // Registered only for test cleanup since parent class is JarFile
    Cleaner.instance.register(this, null);
    this.manifest = new UrlJarManifest(super::getManifest);
    this.closeAction = closeAction;
  }

  @Override
  public ZipEntry getEntry(String name) {
    return UrlJarEntry.of(super.getEntry(name), this.manifest);
  }

  @Override
  public Manifest getManifest() throws IOException {
    return this.manifest.get();
  }

  @Override
  public void close() throws IOException {
    if (this.closeAction != null) {
      this.closeAction.accept(this);
    }
    super.close();
  }

}
