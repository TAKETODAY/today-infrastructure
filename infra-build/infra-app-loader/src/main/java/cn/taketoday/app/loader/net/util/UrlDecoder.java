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

package cn.taketoday.app.loader.net.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * Utility to decode URL strings.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public final class UrlDecoder {

  private UrlDecoder() {
  }

  /**
   * Decode the given string by decoding URL {@code '%'} escapes. This method should be
   * identical in behavior to the {@code decode} method in the internal
   * {@code sun.net.www.ParseUtil} JDK class.
   *
   * @param string the string to decode
   * @return the decoded string
   */
  public static String decode(String string) {
    int length = string.length();
    if ((length == 0) || (string.indexOf('%') < 0)) {
      return string;
    }
    StringBuilder result = new StringBuilder(length);
    ByteBuffer byteBuffer = ByteBuffer.allocate(length);
    CharBuffer charBuffer = CharBuffer.allocate(length);
    CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT);
    int index = 0;
    while (index < length) {
      char ch = string.charAt(index);
      if (ch != '%') {
        result.append(ch);
        if (index + 1 >= length) {
          return result.toString();
        }
        index++;
        continue;
      }
      index = fillByteBuffer(byteBuffer, string, index, length);
      decodeToCharBuffer(byteBuffer, charBuffer, decoder);
      result.append(charBuffer.flip());

    }
    return result.toString();
  }

  private static int fillByteBuffer(ByteBuffer byteBuffer, String string, int index, int length) {
    byteBuffer.clear();
    do {
      byteBuffer.put(unescape(string, index));
      index += 3;
    }
    while (index < length && string.charAt(index) == '%');
    byteBuffer.flip();
    return index;
  }

  private static byte unescape(String string, int index) {
    try {
      return (byte) Integer.parseInt(string, index + 1, index + 3, 16);
    }
    catch (NumberFormatException ex) {
      throw new IllegalArgumentException();
    }
  }

  private static void decodeToCharBuffer(ByteBuffer byteBuffer, CharBuffer charBuffer, CharsetDecoder decoder) {
    decoder.reset();
    charBuffer.clear();
    assertNoError(decoder.decode(byteBuffer, charBuffer, true));
    assertNoError(decoder.flush(charBuffer));
  }

  private static void assertNoError(CoderResult result) {
    if (result.isError()) {
      throw new IllegalArgumentException("Error decoding percent encoded characters");
    }
  }

}
