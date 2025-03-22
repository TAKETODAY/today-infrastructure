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

package infra.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import jakarta.xml.bind.DatatypeConverter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/26 15:29
 */
class Base64UtilsTests {

  @Test
  void encode() {
    byte[] bytes = new byte[]
            { -0x4f, 0xa, -0x73, -0x4f, 0x64, -0x20, 0x75, 0x41, 0x5, -0x49, -0x57, -0x65, -0x19, 0x2e, 0x3f, -0x1b };
    assertThat(Base64Utils.decode(Base64Utils.encode(bytes))).isEqualTo(bytes);

    bytes = "Hello World".getBytes(StandardCharsets.UTF_8);
    assertThat(Base64Utils.decode(Base64Utils.encode(bytes))).isEqualTo(bytes);

    bytes = "Hello World\r\nSecond Line".getBytes(StandardCharsets.UTF_8);
    assertThat(Base64Utils.decode(Base64Utils.encode(bytes))).isEqualTo(bytes);

    bytes = "Hello World\r\nSecond Line\r\n".getBytes(StandardCharsets.UTF_8);
    assertThat(Base64Utils.decode(Base64Utils.encode(bytes))).isEqualTo(bytes);

    bytes = new byte[] { (byte) 0xfb, (byte) 0xf0 };
    assertThat(Base64Utils.encode(bytes)).isEqualTo("+/A=".getBytes());
    assertThat(Base64Utils.decode(Base64Utils.encode(bytes))).isEqualTo(bytes);

    assertThat(Base64Utils.encodeUrlSafe(bytes)).isEqualTo("-_A=".getBytes());
    assertThat(Base64Utils.decodeUrlSafe(Base64Utils.encodeUrlSafe(bytes))).isEqualTo(bytes);
  }

  @Test
  void encodeToStringWithJdk8VsJaxb() {
    byte[] bytes = new byte[]
            { -0x4f, 0xa, -0x73, -0x4f, 0x64, -0x20, 0x75, 0x41, 0x5, -0x49, -0x57, -0x65, -0x19, 0x2e, 0x3f, -0x1b };
    assertThat(DatatypeConverter.printBase64Binary(bytes)).isEqualTo(Base64Utils.encodeToString(bytes));
    assertThat(Base64Utils.decodeFromString(Base64Utils.encodeToString(bytes))).isEqualTo(bytes);
    assertThat(DatatypeConverter.parseBase64Binary(DatatypeConverter.printBase64Binary(bytes))).isEqualTo(bytes);

    bytes = "Hello World".getBytes(StandardCharsets.UTF_8);
    assertThat(DatatypeConverter.printBase64Binary(bytes)).isEqualTo(Base64Utils.encodeToString(bytes));
    assertThat(Base64Utils.decodeFromString(Base64Utils.encodeToString(bytes))).isEqualTo(bytes);
    assertThat(DatatypeConverter.parseBase64Binary(DatatypeConverter.printBase64Binary(bytes))).isEqualTo(bytes);

    bytes = "Hello World\r\nSecond Line".getBytes(StandardCharsets.UTF_8);
    assertThat(DatatypeConverter.printBase64Binary(bytes)).isEqualTo(Base64Utils.encodeToString(bytes));
    assertThat(Base64Utils.decodeFromString(Base64Utils.encodeToString(bytes))).isEqualTo(bytes);
    assertThat(DatatypeConverter.parseBase64Binary(DatatypeConverter.printBase64Binary(bytes))).isEqualTo(bytes);

    bytes = "Hello World\r\nSecond Line\r\n".getBytes(StandardCharsets.UTF_8);
    assertThat(DatatypeConverter.printBase64Binary(bytes)).isEqualTo(Base64Utils.encodeToString(bytes));
    assertThat(Base64Utils.decodeFromString(Base64Utils.encodeToString(bytes))).isEqualTo(bytes);
    assertThat(DatatypeConverter.parseBase64Binary(DatatypeConverter.printBase64Binary(bytes))).isEqualTo(bytes);
  }

  @Test
  void encodeDecodeUrlSafe() {
    byte[] bytes = new byte[] { (byte) 0xfb, (byte) 0xf0 };
    assertThat(Base64Utils.encodeUrlSafe(bytes)).isEqualTo("-_A=".getBytes());
    assertThat(Base64Utils.decodeUrlSafe(Base64Utils.encodeUrlSafe(bytes))).isEqualTo(bytes);

    assertThat(Base64Utils.encodeToUrlSafeString(bytes)).isEqualTo("-_A=");
    assertThat(Base64Utils.decodeFromUrlSafeString(Base64Utils.encodeToUrlSafeString(bytes))).isEqualTo(bytes);
  }

  @Test
  void emptyByteArrayShouldReturnEmpty() {
    byte[] empty = new byte[0];
    assertThat(Base64Utils.encode(empty)).isEmpty();
    assertThat(Base64Utils.decode(empty)).isEmpty();
    assertThat(Base64Utils.encodeUrlSafe(empty)).isEmpty();
    assertThat(Base64Utils.decodeUrlSafe(empty)).isEmpty();
  }

  @Test
  void emptyStringShouldReturnEmpty() {
    assertThat(Base64Utils.encodeToString(new byte[0])).isEmpty();
    assertThat(Base64Utils.decodeFromString("")).isEmpty();
  }

  @Test
  void specialCharactersShouldBeEncodedAndDecoded() {
    byte[] data = "!@#$%^&*()_+".getBytes(StandardCharsets.UTF_8);
    assertThat(Base64Utils.decode(Base64Utils.encode(data))).isEqualTo(data);

    String encoded = Base64Utils.encodeToString(data);
    assertThat(Base64Utils.decodeFromString(encoded)).isEqualTo(data);
  }

  @Test
  void binaryDataShouldBeEncodedAndDecoded() {
    byte[] data = { (byte) 0xFF, (byte) 0x00, (byte) 0xAA, (byte) 0x55 };
    assertThat(Base64Utils.decode(Base64Utils.encode(data))).isEqualTo(data);

    String encoded = Base64Utils.encodeToString(data);
    assertThat(Base64Utils.decodeFromString(encoded)).isEqualTo(data);
  }

  @Test
  void urlSafeEncodingShouldNotContainPlusOrSlash() {
    byte[] data = { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };
    String urlSafe = Base64Utils.encodeToUrlSafeString(data);
    assertThat(urlSafe).doesNotContain("+", "/");
  }

  @Test
  void largeDataShouldBeEncodedAndDecoded() {
    byte[] data = new byte[10000];
    for (int i = 0; i < data.length; i++) {
      data[i] = (byte) (i % 256);
    }
    assertThat(Base64Utils.decode(Base64Utils.encode(data))).isEqualTo(data);

    String encoded = Base64Utils.encodeToString(data);
    assertThat(Base64Utils.decodeFromString(encoded)).isEqualTo(data);
  }

  @Test
  void unicodeStringShouldBeEncodedAndDecoded() {
    byte[] data = "こんにちは世界".getBytes(StandardCharsets.UTF_8);
    assertThat(Base64Utils.decode(Base64Utils.encode(data))).isEqualTo(data);

    String encoded = Base64Utils.encodeToString(data);
    assertThat(Base64Utils.decodeFromString(encoded)).isEqualTo(data);
  }

  @Test
  void paddedAndUnpaddedInputShouldBeDecoded() {
    String padded = "SGVsbG8=";
    String unpadded = "SGVsbG8";

    byte[] expected = "Hello".getBytes(StandardCharsets.UTF_8);
    assertThat(Base64Utils.decodeFromString(padded)).isEqualTo(expected);
    assertThat(Base64Utils.decodeFromString(unpadded)).isEqualTo(expected);
  }

}