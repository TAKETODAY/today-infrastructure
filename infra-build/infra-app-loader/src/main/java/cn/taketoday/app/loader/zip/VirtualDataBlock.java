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
import java.util.Collection;
import java.util.List;

/**
 * A virtual {@link DataBlock} build from a collection of other {@link DataBlock}
 * instances.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class VirtualDataBlock implements DataBlock {

  private List<DataBlock> parts;

  private long size;

  /**
   * Create a new {@link VirtualDataBlock} instance. The {@link #setParts(Collection)}
   * method must be called before the data block can be used.
   */
  protected VirtualDataBlock() {
  }

  /**
   * Create a new {@link VirtualDataBlock} backed by the given parts.
   *
   * @param parts the parts that make up the virtual data block
   * @throws IOException in I/O error
   */
  VirtualDataBlock(Collection<? extends DataBlock> parts) throws IOException {
    setParts(parts);
  }

  /**
   * Set the parts that make up the virtual data block.
   *
   * @param parts the data block parts
   * @throws IOException on I/O error
   */
  protected void setParts(Collection<? extends DataBlock> parts) throws IOException {
    this.parts = List.copyOf(parts);
    long size = 0;
    for (DataBlock part : parts) {
      size += part.size();
    }
    this.size = size;
  }

  @Override
  public long size() throws IOException {
    return this.size;
  }

  @Override
  public int read(ByteBuffer dst, long pos) throws IOException {
    if (pos < 0 || pos >= this.size) {
      return -1;
    }
    long offset = 0;
    int result = 0;
    for (DataBlock part : this.parts) {
      while (pos >= offset && pos < offset + part.size()) {
        int count = part.read(dst, pos - offset);
        result += Math.max(count, 0);
        if (count <= 0 || !dst.hasRemaining()) {
          return result;
        }
        pos += count;
      }
      offset += part.size();
    }
    return result;
  }

}
