/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.core.test.tools;

import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;

/**
 * In-memory {@link JavaFileObject} used to hold class bytecode.
 *
 * @author Phillip Webb
 * @since 4.0
 */
class DynamicClassFileObject extends SimpleJavaFileObject {

  private final String className;

  private volatile byte @Nullable [] bytes;

  DynamicClassFileObject(String className) {
    super(createUri(className), Kind.CLASS);
    this.className = className;
  }

  DynamicClassFileObject(String className, byte[] bytes) {
    super(createUri(className), Kind.CLASS);
    this.className = className;
    this.bytes = bytes;
  }

  private static URI createUri(String className) {
    return URI.create("class:///" + className.replace('.', '/') + ".class");
  }

  @Override
  public InputStream openInputStream() throws IOException {
    byte[] content = this.bytes;
    if (content == null) {
      throw new IOException("No data written");
    }
    return new ByteArrayInputStream(content);
  }

  @Override
  public OutputStream openOutputStream() {
    return new JavaClassOutputStream();
  }

  private void closeOutputStream(byte[] bytes) {
    this.bytes = bytes;
  }

  String getClassName() {
    return this.className;
  }

  byte @Nullable [] getBytes() {
    return this.bytes;
  }

  class JavaClassOutputStream extends ByteArrayOutputStream {

    @Override
    public void close() {
      closeOutputStream(toByteArray());
    }

  }

}
