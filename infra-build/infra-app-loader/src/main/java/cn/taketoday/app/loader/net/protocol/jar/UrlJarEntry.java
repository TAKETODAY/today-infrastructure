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

package cn.taketoday.app.loader.net.protocol.jar;

import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;

/**
 * A {@link JarEntry} returned from a {@link UrlJarFile} or {@link UrlNestedJarFile}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
final class UrlJarEntry extends JarEntry {

  private final UrlJarManifest manifest;

  private UrlJarEntry(JarEntry entry, UrlJarManifest manifest) {
    super(entry);
    this.manifest = manifest;
  }

  @Override
  public Attributes getAttributes() throws IOException {
    return this.manifest.getEntryAttributes(this);
  }

  static UrlJarEntry of(ZipEntry entry, UrlJarManifest manifest) {
    return (entry != null) ? new UrlJarEntry((JarEntry) entry, manifest) : null;
  }

}
