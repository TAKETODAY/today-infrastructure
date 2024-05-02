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

package cn.taketoday.app.loader.jar;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * {@link InflaterInputStream} that supports the writing of an extra "dummy" byte (which
 * is required when using an {@link Inflater} with {@code nowrap}) and returns accurate
 * available() results.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
abstract class ZipInflaterInputStream extends InflaterInputStream {

  private int available;

  private boolean extraBytesWritten;

  ZipInflaterInputStream(InputStream inputStream, Inflater inflater, int size) {
    super(inputStream, inflater, getInflaterBufferSize(size));
    this.available = size;
  }

  private static int getInflaterBufferSize(long size) {
    size += 2; // inflater likes some space
    size = (size > 65536) ? 8192 : size;
    size = (size <= 0) ? 4096 : size;
    return (int) size;
  }

  @Override
  public int available() throws IOException {
    return (this.available >= 0) ? this.available : super.available();
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    int result = super.read(b, off, len);
    if (result != -1) {
      this.available -= result;
    }
    return result;
  }

  @Override
  protected void fill() throws IOException {
    try {
      super.fill();
    }
    catch (EOFException ex) {
      if (this.extraBytesWritten) {
        throw ex;
      }
      this.len = 1;
      this.buf[0] = 0x0;
      this.extraBytesWritten = true;
      this.inf.setInput(this.buf, 0, this.len);
    }
  }

}
