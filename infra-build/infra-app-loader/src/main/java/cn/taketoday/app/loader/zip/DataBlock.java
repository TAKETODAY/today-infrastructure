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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Provides read access to a block of data contained somewhere in a zip file.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public interface DataBlock {

  /**
   * Return the size of this block.
   *
   * @return the block size
   * @throws IOException on I/O error
   */
  long size() throws IOException;

  /**
   * Read a sequence of bytes from this channel into the given buffer, starting at the
   * given block position.
   *
   * @param dst the buffer into which bytes are to be transferred
   * @param pos the position within the block at which the transfer is to begin
   * @return the number of bytes read, possibly zero, or {@code -1} if the given
   * position is greater than or equal to the block size
   * @throws IOException on I/O error
   * @see #readFully(ByteBuffer, long)
   * @see FileChannel#read(ByteBuffer, long)
   */
  int read(ByteBuffer dst, long pos) throws IOException;

  /**
   * Fully read a sequence of bytes from this channel into the given buffer, starting at
   * the given block position and filling {@link ByteBuffer#remaining() remaining} bytes
   * in the buffer.
   *
   * @param dst the buffer into which bytes are to be transferred
   * @param pos the position within the block at which the transfer is to begin
   * @throws EOFException if an attempt is made to read past the end of the block
   * @throws IOException on I/O error
   */
  default void readFully(ByteBuffer dst, long pos) throws IOException {
    do {
      int count = read(dst, pos);
      if (count <= 0) {
        throw new EOFException();
      }
      pos += count;
    }
    while (dst.hasRemaining());
  }

  /**
   * Return this {@link DataBlock} as an {@link InputStream}.
   *
   * @return an {@link InputStream} to read the data block content
   * @throws IOException on IO error
   */
  default InputStream asInputStream() throws IOException {
    return new DataBlockInputStream(this);
  }

}
