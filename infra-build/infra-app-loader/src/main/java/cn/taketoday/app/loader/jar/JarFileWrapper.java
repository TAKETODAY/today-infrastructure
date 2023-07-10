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

package cn.taketoday.app.loader.jar;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Permission;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

/**
 * A wrapper used to create a copy of a {@link JarFile} so that it can be safely closed
 * without closing the original.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class JarFileWrapper extends AbstractJarFile {

  private final JarFile parent;

  JarFileWrapper(JarFile parent) throws IOException {
    super(parent.getRootJarFile().getFile());
    this.parent = parent;
    super.close();
  }

  @Override
  URL getUrl() throws MalformedURLException {
    return this.parent.getUrl();
  }

  @Override
  JarFileType getType() {
    return this.parent.getType();
  }

  @Override
  Permission getPermission() {
    return this.parent.getPermission();
  }

  @Override
  public Manifest getManifest() throws IOException {
    return this.parent.getManifest();
  }

  @Override
  public Enumeration<JarEntry> entries() {
    return this.parent.entries();
  }

  @Override
  public Stream<JarEntry> stream() {
    return this.parent.stream();
  }

  @Override
  public JarEntry getJarEntry(String name) {
    return this.parent.getJarEntry(name);
  }

  @Override
  public ZipEntry getEntry(String name) {
    return this.parent.getEntry(name);
  }

  @Override
  InputStream getInputStream() throws IOException {
    return this.parent.getInputStream();
  }

  @Override
  public synchronized InputStream getInputStream(ZipEntry ze) throws IOException {
    return this.parent.getInputStream(ze);
  }

  @Override
  public String getComment() {
    return this.parent.getComment();
  }

  @Override
  public int size() {
    return this.parent.size();
  }

  @Override
  public String toString() {
    return this.parent.toString();
  }

  @Override
  public String getName() {
    return this.parent.getName();
  }

  static JarFile unwrap(java.util.jar.JarFile jarFile) {
    if (jarFile instanceof JarFile file) {
      return file;
    }
    if (jarFile instanceof JarFileWrapper wrapper) {
      return unwrap(wrapper.parent);
    }
    throw new IllegalStateException("Not a JarFile or Wrapper");
  }

}
