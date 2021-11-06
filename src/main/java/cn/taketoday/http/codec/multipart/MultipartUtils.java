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

package cn.taketoday.http.codec.multipart;

import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.util.MediaType;
import cn.taketoday.http.HttpHeaders;

/**
 * Various static utility methods for dealing with multipart parsing.
 *
 * @author Arjen Poutsma
 * @since 4.0
 */
abstract class MultipartUtils {

  /**
   * Return the character set of the given headers, as defined in the
   * {@link HttpHeaders#getContentType()} header.
   */
  public static Charset charset(HttpHeaders headers) {
    MediaType contentType = headers.getContentType();
    if (contentType != null) {
      Charset charset = contentType.getCharset();
      if (charset != null) {
        return charset;
      }
    }
    return StandardCharsets.UTF_8;
  }

  /**
   * Concatenates the given array of byte arrays.
   */
  public static byte[] concat(byte[]... byteArrays) {
    int len = 0;
    for (byte[] byteArray : byteArrays) {
      len += byteArray.length;
    }
    byte[] result = new byte[len];
    len = 0;
    for (byte[] byteArray : byteArrays) {
      System.arraycopy(byteArray, 0, result, len, byteArray.length);
      len += byteArray.length;
    }
    return result;
  }

  /**
   * Slices the given buffer to the given index (exclusive).
   */
  public static DataBuffer sliceTo(DataBuffer buf, int idx) {
    int pos = buf.readPosition();
    int len = idx - pos + 1;
    return buf.retainedSlice(pos, len);
  }

  /**
   * Slices the given buffer from the given index (inclusive).
   */
  public static DataBuffer sliceFrom(DataBuffer buf, int idx) {
    int len = buf.writePosition() - idx - 1;
    return buf.retainedSlice(idx + 1, len);
  }

  public static void closeChannel(Channel channel) {
    try {
      if (channel.isOpen()) {
        channel.close();
      }
    }
    catch (IOException ignore) {
    }
  }

}
