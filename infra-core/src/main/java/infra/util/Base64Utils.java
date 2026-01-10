/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
