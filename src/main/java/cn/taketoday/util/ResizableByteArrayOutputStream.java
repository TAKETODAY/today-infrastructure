/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.util;

import java.io.ByteArrayOutputStream;

import cn.taketoday.lang.Assert;

/**
 * An extension of {@link java.io.ByteArrayOutputStream} that:
 * <ul>
 * <li>has public {@link ResizableByteArrayOutputStream#grow(int)}
 * and {@link ResizableByteArrayOutputStream#resize(int)} methods
 * to get more control over the size of the internal buffer</li>
 * <li>has a higher initial capacity (256) by default</li>
 * </ul>
 *
 * <p>this class has been superseded by {@link FastByteArrayOutputStream}
 * for internal use where no assignability to {@link ByteArrayOutputStream}
 * is needed (since {@link FastByteArrayOutputStream} is more efficient with buffer
 * resize management but doesn't extend the standard {@link ByteArrayOutputStream}).
 *
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @author TODAY 2021/8/21 01:19
 * @see #resize
 * @see FastByteArrayOutputStream
 * @since 4.0
 */
public class ResizableByteArrayOutputStream extends ByteArrayOutputStream {

  private static final int DEFAULT_INITIAL_CAPACITY = 256;

  /**
   * Create a new <code>ResizableByteArrayOutputStream</code>
   * with the default initial capacity of 256 bytes.
   */
  public ResizableByteArrayOutputStream() {
    super(DEFAULT_INITIAL_CAPACITY);
  }

  /**
   * Create a new <code>ResizableByteArrayOutputStream</code>
   * with the specified initial capacity.
   *
   * @param initialCapacity the initial buffer size in bytes
   */
  public ResizableByteArrayOutputStream(int initialCapacity) {
    super(initialCapacity);
  }

  /**
   * Resize the internal buffer size to a specified capacity.
   *
   * @param targetCapacity the desired size of the buffer
   * @throws IllegalArgumentException if the given capacity is smaller than
   * the actual size of the content stored in the buffer already
   * @see ResizableByteArrayOutputStream#size()
   */
  public synchronized void resize(int targetCapacity) {
    Assert.isTrue(targetCapacity >= this.count, "New capacity must not be smaller than current size");
    byte[] resizedBuffer = new byte[targetCapacity];
    System.arraycopy(this.buf, 0, resizedBuffer, 0, this.count);
    this.buf = resizedBuffer;
  }

  /**
   * Grow the internal buffer size.
   *
   * @param additionalCapacity the number of bytes to add to the current buffer size
   * @see ResizableByteArrayOutputStream#size()
   */
  public synchronized void grow(int additionalCapacity) {
    Assert.isTrue(additionalCapacity >= 0, "Additional capacity must be 0 or higher");
    if (this.count + additionalCapacity > this.buf.length) {
      int newCapacity = Math.max(this.buf.length * 2, this.count + additionalCapacity);
      resize(newCapacity);
    }
  }

  /**
   * Return the current size of this stream's internal buffer.
   */
  public synchronized int capacity() {
    return this.buf.length;
  }

}
