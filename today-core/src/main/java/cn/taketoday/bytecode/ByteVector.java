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
package cn.taketoday.bytecode;

/**
 * A dynamically extensible vector of bytes. This class is roughly equivalent to a DataOutputStream
 * on top of a ByteArrayOutputStream, but is more efficient.
 *
 * @author Eric Bruneton
 */
public class ByteVector {

  /** The content of this vector. Only the first {@link #length} bytes contain real data. */
  byte[] data;

  /** The actual number of bytes in this vector. */
  int length;

  /** Constructs a new {@link ByteVector} with a default initial capacity. */
  public ByteVector() {
    data = new byte[64];
  }

  /**
   * Constructs a new {@link ByteVector} with the given initial capacity.
   *
   * @param initialCapacity the initial capacity of the byte vector to be constructed.
   */
  public ByteVector(final int initialCapacity) {
    data = new byte[initialCapacity];
  }

  /**
   * Constructs a new {@link ByteVector} from the given initial data.
   *
   * @param data the initial data of the new byte vector.
   */
  ByteVector(final byte[] data) {
    this.data = data;
    this.length = data.length;
  }

  /**
   * Returns the actual number of bytes in this vector.
   *
   * @return the actual number of bytes in this vector.
   */
  public int size() {
    return length;
  }

  /**
   * Puts a byte into this byte vector. The byte vector is automatically enlarged if necessary.
   *
   * @param byteValue a byte.
   * @return this byte vector.
   */
  public ByteVector putByte(final int byteValue) {
    int currentLength = length;
    if (currentLength + 1 > data.length) {
      enlarge(1);
    }
    data[currentLength++] = (byte) byteValue;
    length = currentLength;
    return this;
  }

  /**
   * Puts two bytes into this byte vector. The byte vector is automatically enlarged if necessary.
   *
   * @param byteValue1 a byte.
   * @param byteValue2 another byte.
   * @return this byte vector.
   */
  final ByteVector put11(final int byteValue1, final int byteValue2) {
    int currentLength = length;
    if (currentLength + 2 > data.length) {
      enlarge(2);
    }
    byte[] currentData = data;
    currentData[currentLength++] = (byte) byteValue1;
    currentData[currentLength++] = (byte) byteValue2;
    length = currentLength;
    return this;
  }

  /**
   * Puts a short into this byte vector. The byte vector is automatically enlarged if necessary.
   *
   * @param shortValue a short.
   * @return this byte vector.
   */
  public ByteVector putShort(final int shortValue) {
    int currentLength = length;
    if (currentLength + 2 > data.length) {
      enlarge(2);
    }
    byte[] currentData = data;
    currentData[currentLength++] = (byte) (shortValue >>> 8);
    currentData[currentLength++] = (byte) shortValue;
    length = currentLength;
    return this;
  }

  /**
   * Puts a byte and a short into this byte vector. The byte vector is automatically enlarged if
   * necessary.
   *
   * @param byteValue a byte.
   * @param shortValue a short.
   * @return this byte vector.
   */
  final ByteVector put12(final int byteValue, final int shortValue) {
    int currentLength = length;
    if (currentLength + 3 > data.length) {
      enlarge(3);
    }
    byte[] currentData = data;
    currentData[currentLength++] = (byte) byteValue;
    currentData[currentLength++] = (byte) (shortValue >>> 8);
    currentData[currentLength++] = (byte) shortValue;
    length = currentLength;
    return this;
  }

  /**
   * Puts two bytes and a short into this byte vector. The byte vector is automatically enlarged if
   * necessary.
   *
   * @param byteValue1 a byte.
   * @param byteValue2 another byte.
   * @param shortValue a short.
   * @return this byte vector.
   */
  final ByteVector put112(final int byteValue1, final int byteValue2, final int shortValue) {
    int currentLength = length;
    if (currentLength + 4 > data.length) {
      enlarge(4);
    }
    byte[] currentData = data;
    currentData[currentLength++] = (byte) byteValue1;
    currentData[currentLength++] = (byte) byteValue2;
    currentData[currentLength++] = (byte) (shortValue >>> 8);
    currentData[currentLength++] = (byte) shortValue;
    length = currentLength;
    return this;
  }

  /**
   * Puts an int into this byte vector. The byte vector is automatically enlarged if necessary.
   *
   * @param intValue an int.
   * @return this byte vector.
   */
  public ByteVector putInt(final int intValue) {
    int currentLength = length;
    if (currentLength + 4 > data.length) {
      enlarge(4);
    }
    byte[] currentData = data;
    currentData[currentLength++] = (byte) (intValue >>> 24);
    currentData[currentLength++] = (byte) (intValue >>> 16);
    currentData[currentLength++] = (byte) (intValue >>> 8);
    currentData[currentLength++] = (byte) intValue;
    length = currentLength;
    return this;
  }

  /**
   * Puts one byte and two shorts into this byte vector. The byte vector is automatically enlarged
   * if necessary.
   *
   * @param byteValue a byte.
   * @param shortValue1 a short.
   * @param shortValue2 another short.
   * @return this byte vector.
   */
  final ByteVector put122(final int byteValue, final int shortValue1, final int shortValue2) {
    int currentLength = length;
    if (currentLength + 5 > data.length) {
      enlarge(5);
    }
    byte[] currentData = data;
    currentData[currentLength++] = (byte) byteValue;
    currentData[currentLength++] = (byte) (shortValue1 >>> 8);
    currentData[currentLength++] = (byte) shortValue1;
    currentData[currentLength++] = (byte) (shortValue2 >>> 8);
    currentData[currentLength++] = (byte) shortValue2;
    length = currentLength;
    return this;
  }

  /**
   * Puts a long into this byte vector. The byte vector is automatically enlarged if necessary.
   *
   * @param longValue a long.
   * @return this byte vector.
   */
  public ByteVector putLong(final long longValue) {
    int currentLength = length;
    if (currentLength + 8 > data.length) {
      enlarge(8);
    }
    byte[] currentData = data;
    int intValue = (int) (longValue >>> 32);
    currentData[currentLength++] = (byte) (intValue >>> 24);
    currentData[currentLength++] = (byte) (intValue >>> 16);
    currentData[currentLength++] = (byte) (intValue >>> 8);
    currentData[currentLength++] = (byte) intValue;
    intValue = (int) longValue;
    currentData[currentLength++] = (byte) (intValue >>> 24);
    currentData[currentLength++] = (byte) (intValue >>> 16);
    currentData[currentLength++] = (byte) (intValue >>> 8);
    currentData[currentLength++] = (byte) intValue;
    length = currentLength;
    return this;
  }

  /**
   * Puts an UTF8 string into this byte vector. The byte vector is automatically enlarged if
   * necessary.
   *
   * @param stringValue a String whose UTF8 encoded length must be less than 65536.
   * @return this byte vector.
   */
  // DontCheck(AbbreviationAsWordInName): can't be renamed (for backward binary compatibility).
  public ByteVector putUTF8(final String stringValue) {
    int charLength = stringValue.length();
    if (charLength > 65535) {
      throw new IllegalArgumentException("UTF8 string too large");
    }
    int currentLength = length;
    if (currentLength + 2 + charLength > data.length) {
      enlarge(2 + charLength);
    }
    byte[] currentData = data;
    // Optimistic algorithm: instead of computing the byte length and then serializing the string
    // (which requires two loops), we assume the byte length is equal to char length (which is the
    // most frequent case), and we start serializing the string right away. During the
    // serialization, if we find that this assumption is wrong, we continue with the general method.
    currentData[currentLength++] = (byte) (charLength >>> 8);
    currentData[currentLength++] = (byte) charLength;
    for (int i = 0; i < charLength; ++i) {
      char charValue = stringValue.charAt(i);
      if (charValue >= '\u0001' && charValue <= '\u007F') {
        currentData[currentLength++] = (byte) charValue;
      }
      else {
        length = currentLength;
        return encodeUtf8(stringValue, i, 65535);
      }
    }
    length = currentLength;
    return this;
  }

  /**
   * Puts an UTF8 string into this byte vector. The byte vector is automatically enlarged if
   * necessary. The string length is encoded in two bytes before the encoded characters, if there is
   * space for that (i.e. if this.length - offset - 2 &gt;= 0).
   *
   * @param stringValue the String to encode.
   * @param offset the index of the first character to encode. The previous characters are supposed
   * to have already been encoded, using only one byte per character.
   * @param maxByteLength the maximum byte length of the encoded string, including the already
   * encoded characters.
   * @return this byte vector.
   */
  final ByteVector encodeUtf8(final String stringValue, final int offset, final int maxByteLength) {
    int charLength = stringValue.length();
    int byteLength = offset;
    for (int i = offset; i < charLength; ++i) {
      char charValue = stringValue.charAt(i);
      if (charValue >= 0x0001 && charValue <= 0x007F) {
        byteLength++;
      }
      else if (charValue <= 0x07FF) {
        byteLength += 2;
      }
      else {
        byteLength += 3;
      }
    }
    if (byteLength > maxByteLength) {
      throw new IllegalArgumentException("UTF8 string too large");
    }
    // Compute where 'byteLength' must be stored in 'data', and store it at this location.
    int byteLengthOffset = length - offset - 2;
    byte[] currentData = this.data;
    if (byteLengthOffset >= 0) {
      currentData[byteLengthOffset] = (byte) (byteLength >>> 8);
      currentData[byteLengthOffset + 1] = (byte) byteLength;
    }
    if (length + byteLength - offset > currentData.length) {
      enlarge(currentData, byteLength - offset);
      currentData = this.data;
    }
    int currentLength = length;
    for (int i = offset; i < charLength; ++i) {
      char charValue = stringValue.charAt(i);
      if (charValue >= 0x0001 && charValue <= 0x007F) {
        currentData[currentLength++] = (byte) charValue;
      }
      else if (charValue <= 0x07FF) {
        currentData[currentLength++] = (byte) (0xC0 | charValue >> 6 & 0x1F);
        currentData[currentLength++] = (byte) (0x80 | charValue & 0x3F);
      }
      else {
        currentData[currentLength++] = (byte) (0xE0 | charValue >> 12 & 0xF);
        currentData[currentLength++] = (byte) (0x80 | charValue >> 6 & 0x3F);
        currentData[currentLength++] = (byte) (0x80 | charValue & 0x3F);
      }
    }
    length = currentLength;
    return this;
  }

  /**
   * Puts an array of bytes into this byte vector. The byte vector is automatically enlarged if
   * necessary.
   *
   * @param byteArrayValue an array of bytes. May be {@literal null} to put {@code byteLength} null
   * bytes into this byte vector.
   * @param byteOffset index of the first byte of byteArrayValue that must be copied.
   * @param byteLength number of bytes of byteArrayValue that must be copied.
   * @return this byte vector.
   */
  public ByteVector putByteArray(
          final byte[] byteArrayValue, final int byteOffset, final int byteLength) {
    if (length + byteLength > data.length) {
      enlarge(byteLength);
    }
    if (byteArrayValue != null) {
      System.arraycopy(byteArrayValue, byteOffset, data, length, byteLength);
    }
    length += byteLength;
    return this;
  }

  /**
   * Enlarges this byte vector so that it can receive 'size' more bytes.
   *
   * @param size number of additional bytes that this byte vector should be able to receive.
   */
  private void enlarge(final int size) {
    enlarge(data, size);
  }

  private void enlarge(byte[] data, final int size) {
    if (length > data.length) {
      throw new AssertionError("Internal error");
    }
    int doubleCapacity = 2 * data.length;
    int minimalCapacity = length + size;
    byte[] newData = new byte[Math.max(doubleCapacity, minimalCapacity)];
    System.arraycopy(data, 0, newData, 0, length);
    this.data = newData;
  }
}
