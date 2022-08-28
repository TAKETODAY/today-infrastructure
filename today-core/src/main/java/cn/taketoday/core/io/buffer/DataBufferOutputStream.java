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
import java.io.OutputStream;

import cn.taketoday.lang.Assert;

/**
 * An {@link OutputStream} that writes to a {@link DataBuffer}.
 *
 * @author Arjen Poutsma
 * @see DataBuffer#asOutputStream()
 * @since 4.0
 */
final class DataBufferOutputStream extends OutputStream {

  private final DataBuffer dataBuffer;

  private boolean closed;

  public DataBufferOutputStream(DataBuffer dataBuffer) {
    Assert.notNull(dataBuffer, "DataBuffer must not be null");
    this.dataBuffer = dataBuffer;
  }

  @Override
  public void write(int b) throws IOException {
    checkClosed();
    this.dataBuffer.ensureWritable(1);
    this.dataBuffer.write((byte) b);
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    checkClosed();
    if (len > 0) {
      this.dataBuffer.ensureWritable(len);
      this.dataBuffer.write(b, off, len);
    }
  }

  @Override
  public void close() {
    if (this.closed) {
      return;
    }
    this.closed = true;
  }

  private void checkClosed() throws IOException {
    if (this.closed) {
      throw new IOException("DataBufferOutputStream is closed");
    }
  }

}
