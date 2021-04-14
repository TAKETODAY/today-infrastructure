/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.framework.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * @author TODAY 2021/4/13 21:10
 */
public class ByteBufferOutputStream extends OutputStream {
  private ByteBuffer byteBuffer;

  /**
   * Creates an uninitialized stream that cannot be used
   * until {@link #setByteBuffer(ByteBuffer)} is called.
   */
  public ByteBufferOutputStream() {}

  /**
   * Creates a stream with a new non-direct buffer of the specified size.
   */
  public ByteBufferOutputStream(int bufferSize) {
    this(ByteBuffer.allocate(bufferSize));
  }

  public ByteBufferOutputStream(ByteBuffer byteBuffer) {
    this.byteBuffer = byteBuffer;
  }

  public ByteBuffer getByteBuffer() {
    return byteBuffer;
  }

  public void setByteBuffer(ByteBuffer byteBuffer) {
    this.byteBuffer = byteBuffer;
  }

  public void write(int b) throws IOException {
    if (!byteBuffer.hasRemaining()) flush();
    byteBuffer.put((byte) b);
  }

  public void write(byte[] bytes, int offset, int length) throws IOException {
    if (byteBuffer.remaining() < length) {
      flush();
    }
    byteBuffer.put(bytes, offset, length);
  }
}
