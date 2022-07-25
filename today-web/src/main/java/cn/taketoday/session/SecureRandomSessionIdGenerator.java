/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.session;

import java.security.SecureRandom;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.TodayStrategies;

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

  private volatile int length = 30;

  private static final char[] SESSION_ID_ALPHABET;
  private static final String ALPHABET_PROPERTY = "secureRandomSessionIdGenerator.ALPHABET";

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

  public int getLength() {
    return length;
  }

  public void setLength(final int length) {
    Assert.isTrue(length > 0, "length must be greater than 0");
    this.length = length;
  }

  /**
   * Encode the bytes into a String with a slightly modified Base64-algorithm
   * This code was written by Kevin Kelley <kelley@ruralnet.net>
   * and adapted by Thomas Peuss <jboss@peuss.de>
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
