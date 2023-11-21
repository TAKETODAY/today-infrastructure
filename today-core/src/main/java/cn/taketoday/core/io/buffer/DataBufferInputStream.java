/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.core.io.buffer;

import java.io.IOException;
import java.io.InputStream;

import cn.taketoday.lang.Assert;

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
    this.end = start + this.dataBuffer.readableByteCount();
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
      DataBufferUtils.release(this.dataBuffer);
    }
    this.closed = true;
  }

  private void checkClosed() throws IOException {
    if (this.closed) {
      throw new IOException("DataBufferInputStream is closed");
    }
  }

}
