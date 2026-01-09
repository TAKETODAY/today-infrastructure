/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.session;

import java.security.SecureRandom;

import infra.lang.Assert;
import infra.lang.TodayStrategies;

/**
 * A {@link SessionIdGenerator} that uses a secure random to generate a
 * session ID.
 *
 * On some systems this may perform poorly if not enough entropy is available,
 * depending on the algorithm in use.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/30 23:29
 */
public class SecureRandomSessionIdGenerator implements SessionIdGenerator {

  private final SecureRandom random = new SecureRandom();

  private int length = 15;

  private static final char[] SESSION_ID_ALPHABET;
  private static final String ALPHABET_PROPERTY = "server.session.id.source";

  static {
    String alphabet = TodayStrategies.getProperty(
            ALPHABET_PROPERTY, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_");
    Assert.state(alphabet.length() == 64, "SecureRandomSessionIdGenerator must be exactly 64 characters long");
    SESSION_ID_ALPHABET = alphabet.toCharArray();
  }

  @Override
  public String generateId() {
    final byte[] bytes = new byte[length];
    random.nextBytes(bytes);
    return new String(encode(bytes));
  }

  /**
   * set session id length
   */
  public void setSessionIdLength(int length) {
    Assert.isTrue(length > 0, "length must be greater than 0");
    this.length = length / 4 * 3; // 3 bytes encode to 4 chars.
  }

  /**
   * Encode the bytes into a String with a slightly modified Base64-algorithm
   * This code was written by Kevin Kelley {@code kelley@ruralnet.net}
   * and adapted by Thomas Peuss {@code jboss@peuss.de}
   *
   * @param data The bytes you want to encode
   * @return the encoded String
   */
  private char[] encode(byte[] data) {
    char[] out = new char[((data.length + 2) / 3) * 4];
    char[] alphabet = SESSION_ID_ALPHABET;
    //
    // 3 bytes encode to 4 chars.  Output is always an even
    // multiple of 4 characters.
    //
    for (int i = 0, index = 0; i < data.length; i += 3, index += 4) {
      boolean quad = false;
      boolean trip = false;

      int val = (0xFF & (int) data[i]);
      val <<= 8;
      if ((i + 1) < data.length) {
        val |= (0xFF & (int) data[i + 1]);
        trip = true;
      }
      val <<= 8;
      if ((i + 2) < data.length) {
        val |= (0xFF & (int) data[i + 2]);
        quad = true;
      }
      out[index + 3] = alphabet[(quad ? (val & 0x3F) : 63)];
      val >>= 6;
      out[index + 2] = alphabet[(trip ? (val & 0x3F) : 63)];
      val >>= 6;
      out[index + 1] = alphabet[val & 0x3F];
      val >>= 6;
      out[index] = alphabet[val & 0x3F];
    }
    return out;
  }

}
