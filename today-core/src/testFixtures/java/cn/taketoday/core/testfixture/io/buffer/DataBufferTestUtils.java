/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.core.testfixture.io.buffer;

import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.lang.Assert;

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
    Assert.notNull(buffer, "'buffer' must not be null");
    byte[] bytes = new byte[buffer.readableByteCount()];
    buffer.read(bytes);
    return bytes;
  }

}
