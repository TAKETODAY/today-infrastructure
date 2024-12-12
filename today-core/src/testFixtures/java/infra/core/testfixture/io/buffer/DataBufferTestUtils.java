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

package infra.core.testfixture.io.buffer;

import infra.core.io.buffer.DataBuffer;
import infra.lang.Assert;

/**
 * Utility class for working with {@link DataBuffer}s in tests.
 *
 * <p>Note that this class is in the {@code test} tree of the project:
 * the methods contained herein are not suitable for production code bases.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
public abstract class DataBufferTestUtils {

  /**
   * Dump all the bytes in the given data buffer, and returns them as a byte array.
   * <p>Note that this method reads the entire buffer into the heap,  which might
   * consume a lot of memory.
   *
   * @param buffer the data buffer to dump the bytes of
   * @return the bytes in the given data buffer
   */
  public static byte[] dumpBytes(DataBuffer buffer) {
    Assert.notNull(buffer, "'buffer' is required");
    byte[] bytes = new byte[buffer.readableBytes()];
    buffer.read(bytes);
    return bytes;
  }

}
