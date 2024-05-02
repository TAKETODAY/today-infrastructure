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

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * {@link DataBlock} backed by a byte array .
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0s
 */
class ByteArrayDataBlock implements CloseableDataBlock {

  private final byte[] bytes;

  private final int maxReadSize;

  /**
   * Create a new {@link ByteArrayDataBlock} backed by the given bytes.
   *
   * @param bytes the bytes to use
   */
  ByteArrayDataBlock(byte... bytes) {
    this(bytes, -1);
  }

  ByteArrayDataBlock(byte[] bytes, int maxReadSize) {
    this.bytes = bytes;
    this.maxReadSize = maxReadSize;
  }

  @Override
  public long size() throws IOException {
    return this.bytes.length;
  }

  @Override
  public int read(ByteBuffer dst, long pos) throws IOException {
    return read(dst, (int) pos);
  }

  private int read(ByteBuffer dst, int pos) {
    int remaining = dst.remaining();
    int length = Math.min(this.bytes.length - pos, remaining);
    if (this.maxReadSize > 0 && length > this.maxReadSize) {
      length = this.maxReadSize;
    }
    dst.put(this.bytes, pos, length);
    return length;
  }

  @Override
  public void close() throws IOException {
  }

}
