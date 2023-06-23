/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.core.test.tools;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;

import cn.taketoday.lang.Nullable;

/**
 * In-memory {@link JavaFileObject} used to hold class bytecode.
 *
 * @author Phillip Webb
 * @since 4.0
 */
class DynamicClassFileObject extends SimpleJavaFileObject {

  private final String className;

  @Nullable
  private volatile byte[] bytes;

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

  @Nullable
  byte[] getBytes() {
    return this.bytes;
  }

  class JavaClassOutputStream extends ByteArrayOutputStream {

    @Override
    public void close() {
      closeOutputStream(toByteArray());
    }

  }

}
