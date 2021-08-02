// ASM: a very small and fast Java bytecode manipulation framework
// Copyright (c) 2000-2011 INRIA, France Telecom
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
// 3. Neither the name of the copyright holders nor the names of its
//    contributors may be used to endorse or promote products derived from
//    this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
// THE POSSIBILITY OF SUCH DAMAGE.
package cn.taketoday.asm;

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
