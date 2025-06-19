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

package infra.core.io.buffer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

import infra.lang.Assert;

/**
 * An {@link InputStream} that reads from a {@link DataBuffer}.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see DataBuffer#asInputStream(boolean)
 * @since 4.0
 */
final class DataBufferInputStream extends InputStream {

  private final DataBuffer dataBuffer;

  private final int end;

  private final boolean releaseOnClose;

  private boolean closed;

  private int mark;

  public DataBufferInputStream(DataBuffer dataBuffer, boolean releaseOnClose) {
    Assert.notNull(dataBuffer, "DataBuffer is required");
    this.dataBuffer = dataBuffer;
    int start = this.dataBuffer.readPosition();
    this.end = start + this.dataBuffer.readableBytes();
    this.mark = start;
    this.releaseOnClose = releaseOnClose;
  }

  @Override
  public int read() throws IOException {
    checkClosed();
    if (available() == 0) {
      return -1;
    }
    return this.dataBuffer.read() & 0xFF;
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    checkClosed();
    int available = available();
    if (available == 0) {
      return -1;
    }
    len = Math.min(available, len);
    this.dataBuffer.read(b, off, len);
    return len;
  }

  @Override
  public boolean markSupported() {
    return true;
  }

  @Override
  public void mark(int readLimit) {
    Assert.isTrue(readLimit > 0, "readLimit must be greater than 0");
    this.mark = this.dataBuffer.readPosition();
  }

  @Override
  public int available() {
    return Math.max(0, this.end - this.dataBuffer.readPosition());
  }

  @Override
  public void reset() {
    this.dataBuffer.readPosition(this.mark);
  }

  @Override
  public void close() {
    if (this.closed) {
      return;
    }
    if (this.releaseOnClose) {
      dataBuffer.release();
    }
    this.closed = true;
  }

  @Override
  public byte[] readNBytes(int len) throws IOException {
    if (len < 0) {
      throw new IllegalArgumentException("len < 0");
    }
    checkClosed();
    int size = Math.min(available(), len);
    byte[] out = new byte[size];
    this.dataBuffer.read(out);
    return out;
  }

  @Override
  public long skip(long n) throws IOException {
    checkClosed();
    if (n <= 0) {
      return 0L;
    }
    int skipped = Math.min(available(), n > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) n);
    this.dataBuffer.readPosition(this.dataBuffer.readPosition() + skipped);
    return skipped;
  }

  @Override
  public long transferTo(OutputStream out) throws IOException {
    Objects.requireNonNull(out, "out");
    checkClosed();
    if (available() == 0) {
      return 0L;
    }
    byte[] buf = readAllBytes();
    out.write(buf);
    return buf.length;
  }

  private void checkClosed() throws IOException {
    if (this.closed) {
      throw new IOException("DataBufferInputStream is closed");
    }
  }

}
