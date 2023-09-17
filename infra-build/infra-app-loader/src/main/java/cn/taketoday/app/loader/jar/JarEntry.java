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
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSigner;
import java.security.cert.Certificate;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Extended variant of {@link java.util.jar.JarEntry} returned by {@link JarFile}s.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class JarEntry extends java.util.jar.JarEntry implements FileHeader {

  private final int index;

  private final AsciiBytes name;

  private final AsciiBytes headerName;

  private final JarFile jarFile;

  private final long localHeaderOffset;

  private volatile JarEntryCertification certification;

  JarEntry(JarFile jarFile, int index, CentralDirectoryFileHeader header, AsciiBytes nameAlias) {
    super((nameAlias != null) ? nameAlias.toString() : header.getName().toString());
    this.index = index;
    this.name = (nameAlias != null) ? nameAlias : header.getName();
    this.headerName = header.getName();
    this.jarFile = jarFile;
    this.localHeaderOffset = header.getLocalHeaderOffset();
    setCompressedSize(header.getCompressedSize());
    setMethod(header.getMethod());
    setCrc(header.getCrc());
    setComment(header.getComment().toString());
    setSize(header.getSize());
    setTime(header.getTime());
    if (header.hasExtra()) {
      setExtra(header.getExtra());
    }
  }

  int getIndex() {
    return this.index;
  }

  AsciiBytes getAsciiBytesName() {
    return this.name;
  }

  @Override
  public boolean hasName(CharSequence name, char suffix) {
    return this.headerName.matches(name, suffix);
  }

  /**
   * Return a {@link URL} for this {@link JarEntry}.
   *
   * @return the URL for the entry
   * @throws MalformedURLException if the URL is not valid
   */
  URL getUrl() throws MalformedURLException {
    return new URL(this.jarFile.getUrl(), getName());
  }

  @Override
  public Attributes getAttributes() throws IOException {
    Manifest manifest = this.jarFile.getManifest();
    return (manifest != null) ? manifest.getAttributes(getName()) : null;
  }

  @Override
  public Certificate[] getCertificates() {
    return getCertification().getCertificates();
  }

  @Override
  public CodeSigner[] getCodeSigners() {
    return getCertification().getCodeSigners();
  }

  private JarEntryCertification getCertification() {
    if (!this.jarFile.isSigned()) {
      return JarEntryCertification.NONE;
    }
    JarEntryCertification certification = this.certification;
    if (certification == null) {
      certification = this.jarFile.getCertification(this);
      this.certification = certification;
    }
    return certification;
  }

  @Override
  public long getLocalHeaderOffset() {
    return this.localHeaderOffset;
  }

}
