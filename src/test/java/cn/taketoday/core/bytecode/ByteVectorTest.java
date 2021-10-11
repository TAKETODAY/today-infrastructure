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
package cn.taketoday.core.bytecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link ByteVector}.
 *
 * @author Eric Bruneton
 */
public class ByteVectorTest {

  @Test
  public void testPutByte() {
    ByteVector byteVector = new ByteVector(0);

    byteVector.putByte(1);

    assertArrayEquals(new byte[] { 1 }, toArray(byteVector));
  }

  @Test
  public void testPut11() {
    ByteVector byteVector = new ByteVector(0);

    byteVector.put11(1, 2);

    assertArrayEquals(new byte[] { 1, 2 }, toArray(byteVector));
  }

  @Test
  public void testPutShort() {
    ByteVector byteVector = new ByteVector(0);

    byteVector.putShort(0x0102);

    assertArrayEquals(new byte[] { 1, 2 }, toArray(byteVector));
  }

  @Test
  public void testPut12() {
    ByteVector byteVector = new ByteVector(0);

    byteVector.put12(1, 0x0203);

    assertArrayEquals(new byte[] { 1, 2, 3 }, toArray(byteVector));
  }

  @Test
  public void testPut112() {
    ByteVector byteVector = new ByteVector(0);

    byteVector.put112(1, 2, 0x0304);

    assertArrayEquals(new byte[] { 1, 2, 3, 4 }, toArray(byteVector));
  }

  @Test
  public void testPutInt() {
    ByteVector byteVector = new ByteVector(0);

    byteVector.putInt(0x01020304);

    assertArrayEquals(new byte[] { 1, 2, 3, 4 }, toArray(byteVector));
  }

  @Test
  public void testPut122() {
    ByteVector byteVector = new ByteVector(0);

    byteVector.put122(1, 0x0203, 0x0405);

    assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, toArray(byteVector));
  }

  @Test
  public void testPutLong() {
    ByteVector byteVector = new ByteVector(0);

    byteVector.putLong(0x0102030405060708L);

    assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 }, toArray(byteVector));
  }

  @Test
  public void testPutUtf8_ascii() {
    ByteVector byteVector = new ByteVector(0);

    byteVector.putUTF8("abc");

    assertArrayEquals(new byte[] { 0, 3, 'a', 'b', 'c' }, toArray(byteVector));
  }

  @ParameterizedTest
  @ValueSource(ints = { 65535, 65536 })
  public void testPutUtf8_ascii_tooLarge(final int size) {
    ByteVector byteVector = new ByteVector(0);
    char[] charBuffer = new char[size];
    Arrays.fill(charBuffer, 'A');

    Executable putUtf8 = () -> byteVector.putUTF8(new String(charBuffer));

    if (size > 65535) {
      Exception exception = assertThrows(IllegalArgumentException.class, putUtf8);
      assertEquals("UTF8 string too large", exception.getMessage());
    }
    else {
      assertDoesNotThrow(putUtf8);
    }
  }

  @Test
  public void testPutUtf8_unicode() {
    ByteVector byteVector = new ByteVector(0);

    byteVector.putUTF8(new String(new char[] { 'a', 0x0000, 0x0080, 0x0800 }));

    assertArrayEquals(
            new byte[] { 0, 8, 'a', -64, -128, -62, -128, -32, -96, -128 }, toArray(byteVector));
  }

  @Test
  public void testPutUtf8_unicode_tooLarge() {
    ByteVector byteVector = new ByteVector(0);
    char[] charBuffer = new char[32768];
    Arrays.fill(charBuffer, (char) 0x07FF);

    Executable putUtf8 = () -> byteVector.putUTF8(new String(charBuffer));

    Exception exception = assertThrows(IllegalArgumentException.class, putUtf8);
    assertEquals("UTF8 string too large", exception.getMessage());
  }

  @Test
  public void testPutByteArray() {
    ByteVector byteVector = new ByteVector(0);

    byteVector.putByteArray(new byte[] { 0, 1, 2, 3, 4, 5 }, 1, 3);

    assertArrayEquals(new byte[] { 1, 2, 3 }, toArray(byteVector));
  }

  private static byte[] toArray(final ByteVector byteVector) {
    byte[] result = new byte[byteVector.length];
    System.arraycopy(byteVector.data, 0, result, 0, byteVector.length);
    return result;
  }
}
