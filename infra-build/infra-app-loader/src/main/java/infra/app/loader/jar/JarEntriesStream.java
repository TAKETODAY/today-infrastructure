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

package infra.app.loader.jar;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.zip.Inflater;
import java.util.zip.ZipEntry;

import infra.lang.Nullable;

/**
 * Helper class to iterate entries in a jar file and check that content matches a related
 * entry.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
class JarEntriesStream implements Closeable {

  private static final int BUFFER_SIZE = 4 * 1024;

  private final JarInputStream in;

  private final byte[] inBuffer = new byte[BUFFER_SIZE];

  private final byte[] compareBuffer = new byte[BUFFER_SIZE];

  private final Inflater inflater = new Inflater(true);

  @Nullable
  private JarEntry entry;

  JarEntriesStream(InputStream in) throws IOException {
    this.in = new JarInputStream(in);
  }

  @Nullable
  JarEntry getNextEntry() throws IOException {
    this.entry = this.in.getNextJarEntry();
    if (this.entry != null) {
      this.entry.getSize();
    }
    this.inflater.reset();
    return this.entry;
  }

  boolean matches(boolean directory, int size, int compressionMethod, InputStreamSupplier streamSupplier)
          throws IOException {
    if (this.entry.isDirectory() != directory) {
      fail("directory");
    }
    if (this.entry.getMethod() != compressionMethod) {
      fail("compression method");
    }
    if (this.entry.isDirectory()) {
      this.in.closeEntry();
      return true;
    }
    try (DataInputStream expected = new DataInputStream(getInputStream(size, streamSupplier))) {
      assertSameContent(expected);
    }
    return true;
  }

  private InputStream getInputStream(int size, InputStreamSupplier streamSupplier) throws IOException {
    InputStream inputStream = streamSupplier.get();
    return (this.entry.getMethod() != ZipEntry.DEFLATED) ? inputStream
            : new ZipInflaterInputStream(inputStream, this.inflater, size);
  }

  private void assertSameContent(DataInputStream expected) throws IOException {
    int len;
    while ((len = this.in.read(this.inBuffer)) > 0) {
      try {
        expected.readFully(this.compareBuffer, 0, len);
        if (Arrays.equals(this.inBuffer, 0, len, this.compareBuffer, 0, len)) {
          continue;
        }
      }
      catch (EOFException ex) {
        // Continue and throw exception due to mismatched content length.
      }
      fail("content");
    }
    if (expected.read() != -1) {
      fail("content");
    }
  }

  private void fail(String check) {
    throw new IllegalStateException("Content mismatch when reading security info for entry '%s' (%s check)"
            .formatted(this.entry.getName(), check));
  }

  @Override
  public void close() throws IOException {
    this.inflater.end();
    this.in.close();
  }

  @FunctionalInterface
  interface InputStreamSupplier {

    InputStream get() throws IOException;

  }

}
