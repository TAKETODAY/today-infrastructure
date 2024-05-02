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

package cn.taketoday.app.loader.net.protocol.nested;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import cn.taketoday.app.loader.ref.Cleaner;
import cn.taketoday.app.loader.zip.CloseableDataBlock;
import cn.taketoday.app.loader.zip.ZipContent;

/**
 * Resources created managed and cleaned by a {@link NestedUrlConnection} instance and
 * suitable for registration with a {@link Cleaner}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class NestedUrlConnectionResources implements Runnable {

  private final NestedLocation location;

  private volatile ZipContent zipContent;

  private volatile long size = -1;

  private volatile InputStream inputStream;

  NestedUrlConnectionResources(NestedLocation location) {
    this.location = location;
  }

  NestedLocation getLocation() {
    return this.location;
  }

  void connect() throws IOException {
    synchronized(this) {
      if (this.zipContent == null) {
        this.zipContent = ZipContent.open(this.location.path(), this.location.nestedEntryName());
        try {
          connectData();
        }
        catch (IOException | RuntimeException ex) {
          this.zipContent.close();
          this.zipContent = null;
          throw ex;
        }
      }
    }
  }

  private void connectData() throws IOException {
    CloseableDataBlock data = this.zipContent.openRawZipData();
    try {
      this.size = data.size();
      this.inputStream = data.asInputStream();
    }
    catch (IOException | RuntimeException ex) {
      data.close();
    }
  }

  InputStream getInputStream() throws IOException {
    synchronized(this) {
      if (this.inputStream == null) {
        throw new IOException("Nested location not found " + this.location);
      }
      return this.inputStream;
    }
  }

  long getContentLength() {
    return this.size;
  }

  @Override
  public void run() {
    releaseAll();
  }

  private void releaseAll() {
    synchronized(this) {
      if (this.zipContent != null) {
        IOException exceptionChain = null;
        try {
          this.inputStream.close();
        }
        catch (IOException ex) {
          exceptionChain = addToExceptionChain(exceptionChain, ex);
        }
        try {
          this.zipContent.close();
        }
        catch (IOException ex) {
          exceptionChain = addToExceptionChain(exceptionChain, ex);
        }
        this.size = -1;
        if (exceptionChain != null) {
          throw new UncheckedIOException(exceptionChain);
        }
      }
    }
  }

  private IOException addToExceptionChain(IOException exceptionChain, IOException ex) {
    if (exceptionChain != null) {
      exceptionChain.addSuppressed(ex);
      return exceptionChain;
    }
    return ex;
  }

}
