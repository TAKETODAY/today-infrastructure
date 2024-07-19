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

package cn.taketoday.app.loader.file;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * URL Path Encoder based.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
final class UriPathEncoder {

  private static final char[] ALLOWED = "/:@-._~!$&\'()*+,;=".toCharArray();

  private UriPathEncoder() {
  }

  static String encode(String path) {
    byte[] bytes = path.getBytes(StandardCharsets.UTF_8);
    for (byte b : bytes) {
      if (!isAllowed(b)) {
        return encode(bytes);
      }
    }
    return path;
  }

  private static String encode(byte[] bytes) {
    ByteArrayOutputStream result = new ByteArrayOutputStream(bytes.length);
    for (byte b : bytes) {
      if (isAllowed(b)) {
        result.write(b);
      }
      else {
        result.write('%');
        result.write(Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, 16)));
        result.write(Character.toUpperCase(Character.forDigit(b & 0xF, 16)));
      }
    }
    return result.toString(StandardCharsets.UTF_8);
  }

  private static boolean isAllowed(int ch) {
    for (char allowed : ALLOWED) {
      if (ch == allowed) {
        return true;
      }
    }
    return isAlpha(ch) || isDigit(ch);
  }

  private static boolean isAlpha(int ch) {
    return (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z');
  }

  private static boolean isDigit(int ch) {
    return (ch >= '0' && ch <= '9');
  }

}
