/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.app.loader.net.util;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

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
   * Decode the given encoded URI component value by replacing each "<i>{@code %xy}</i>"
   * sequence with a hexadecimal representation of the character in
   * {@link StandardCharsets#UTF_8 UTF-8}, leaving other characters unmodified.
   *
   * @param source the encoded URI component value
   * @return the decoded value
   */
  public static String decode(String source) {
    return decode(source, StandardCharsets.UTF_8);
  }

  /**
   * Decode the given encoded URI component value by replacing each "<i>{@code %xy}</i>"
   * sequence with a hexadecimal representation of the character in the specified
   * character encoding, leaving other characters unmodified.
   *
   * @param source the encoded URI component value
   * @param charset the character encoding to use to decode the "<i>{@code %xy}</i>"
   * sequences
   * @return the decoded value
   */
  public static String decode(String source, Charset charset) {
    int length = source.length();
    int firstPercentIndex = source.indexOf('%');
    if (length == 0 || firstPercentIndex < 0) {
      return source;
    }

    StringBuilder output = new StringBuilder(length);
    output.append(source, 0, firstPercentIndex);
    byte[] bytes = null;
    int i = firstPercentIndex;
    while (i < length) {
      char ch = source.charAt(i);
      if (ch == '%') {
        try {
          if (bytes == null) {
            bytes = new byte[(length - i) / 3];
          }

          int pos = 0;
          while (i + 2 < length && ch == '%') {
            bytes[pos++] = (byte) HexFormat.fromHexDigits(source, i + 1, i + 3);
            i += 3;
            if (i < length) {
              ch = source.charAt(i);
            }
          }

          if (i < length && ch == '%') {
            throw new IllegalArgumentException("Incomplete trailing escape (%) pattern");
          }

          output.append(new String(bytes, 0, pos, charset));
        }
        catch (NumberFormatException ex) {
          throw new IllegalArgumentException("Invalid encoded sequence \"" + source.substring(i) + "\"");
        }
      }
      else {
        output.append(ch);
        i++;
      }
    }
    return output.toString();
  }

}
