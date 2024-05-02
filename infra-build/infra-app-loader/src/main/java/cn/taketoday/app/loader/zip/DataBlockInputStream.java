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

package cn.taketoday.app.loader.zip;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * {@link InputStream} backed by a {@link DataBlock}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class DataBlockInputStream extends InputStream {

  private final DataBlock dataBlock;

  private long pos;

  private long remaining;

  private volatile boolean closed;

  DataBlockInputStream(DataBlock dataBlock) throws IOException {
    this.dataBlock = dataBlock;
    this.remaining = dataBlock.size();
  }

  @Override
  public int read() throws IOException {
    byte[] b = new byte[1];
    return (read(b, 0, 1) == 1) ? b[0] & 0xFF : -1;
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    ensureOpen();
    ByteBuffer dst = ByteBuffer.wrap(b, off, len);
    int count = this.dataBlock.read(dst, this.pos);
    if (count > 0) {
      this.pos += count;
      this.remaining -= count;
    }
    return count;
  }

  @Override
  public long skip(long n) throws IOException {
    long count = (n > 0) ? maxForwardSkip(n) : maxBackwardSkip(n);
    this.pos += count;
    this.remaining -= count;
    return count;
  }

  private long maxForwardSkip(long n) {
    boolean willCauseOverflow = (this.pos + n) < 0;
    return (willCauseOverflow || n > this.remaining) ? this.remaining : n;
  }

  private long maxBackwardSkip(long n) {
    return Math.max(-this.pos, n);
  }

  @Override
  public int available() {
    if (this.closed) {
      return 0;
    }
    return (this.remaining < Integer.MAX_VALUE) ? (int) this.remaining : Integer.MAX_VALUE;
  }

  private void ensureOpen() throws IOException {
    if (this.closed) {
      throw new IOException("InputStream closed");
    }
  }

  @Override
  public void close() throws IOException {
    if (this.closed) {
      return;
    }
    this.closed = true;
    if (this.dataBlock instanceof Closeable closeable) {
      closeable.close();
    }
  }

}
