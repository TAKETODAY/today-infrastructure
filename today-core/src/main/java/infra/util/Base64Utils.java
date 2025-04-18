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

package infra.util;

import java.util.Base64;

import infra.lang.Constant;

/**
 * A simple utility class for Base64 encoding and decoding.
 *
 * <p>Adapts to Java 8's {@link java.util.Base64} in a convenience fashion.
 *
 * @author Juergen Hoeller
 * @author Gary Russell
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see java.util.Base64
 * @since 4.0 2022/4/15 12:34
 */
public abstract class Base64Utils {

  /**
   * Base64-encode the given byte array.
   *
   * @param src the original byte array
   * @return the encoded byte array
   */
  public static byte[] encode(byte[] src) {
    if (src.length == 0) {
      return src;
    }
    return Base64.getEncoder().encode(src);
  }

  /**
   * Base64-decode the given byte array.
   *
   * @param src the encoded byte array
   * @return the original byte array
   */
  public static byte[] decode(byte[] src) {
    if (src.length == 0) {
      return src;
    }
    return Base64.getDecoder().decode(src);
  }

  /**
   * Base64-encode the given byte array using the RFC 4648
   * "URL and Filename Safe Alphabet".
   *
   * @param src the original byte array
   * @return the encoded byte array
   */
  public static byte[] encodeUrlSafe(byte[] src) {
    if (src.length == 0) {
      return src;
    }
    return Base64.getUrlEncoder().encode(src);
  }

  /**
   * Base64-decode the given byte array using the RFC 4648
   * "URL and Filename Safe Alphabet".
   *
   * @param src the encoded byte array
   * @return the original byte array
   */
  public static byte[] decodeUrlSafe(byte[] src) {
    if (src.length == 0) {
      return src;
    }
    return Base64.getUrlDecoder().decode(src);
  }

  /**
   * Base64-encode the given byte array to a String.
   *
   * @param src the original byte array
   * @return the encoded byte array as a UTF-8 String
   */
  public static String encodeToString(byte[] src) {
    if (src.length == 0) {
      return "";
    }
    return new String(encode(src), Constant.DEFAULT_CHARSET);
  }

  /**
   * Base64-decode the given byte array from an UTF-8 String.
   *
   * @param src the encoded UTF-8 String
   * @return the original byte array
   */
  public static byte[] decodeFromString(String src) {
    if (src.isEmpty()) {
      return Constant.EMPTY_BYTES;
    }
    return decode(src.getBytes(Constant.DEFAULT_CHARSET));
  }

  /**
   * Base64-encode the given byte array to a String using the RFC 4648
   * "URL and Filename Safe Alphabet".
   *
   * @param src the original byte array
   * @return the encoded byte array as a UTF-8 String
   */
  public static String encodeToUrlSafeString(byte[] src) {
    return new String(encodeUrlSafe(src), Constant.DEFAULT_CHARSET);
  }

  /**
   * Base64-decode the given byte array from an UTF-8 String using the RFC 4648
   * "URL and Filename Safe Alphabet".
   *
   * @param src the encoded UTF-8 String
   * @return the original byte array
   */
  public static byte[] decodeFromUrlSafeString(String src) {
    return decodeUrlSafe(src.getBytes(Constant.DEFAULT_CHARSET));
  }

}
