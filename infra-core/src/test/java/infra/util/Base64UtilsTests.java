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

package infra.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import jakarta.xml.bind.DatatypeConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

  @Test
  void invalidBase64InputShouldThrowException() {
    assertThatThrownBy(() -> Base64Utils.decodeFromString("Invalid!@#"))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void multiLineInputShouldBeHandled() {
    String input = """
            Line 1
            Line 2
            Line 3""";
    byte[] data = input.getBytes(StandardCharsets.UTF_8);
    String encoded = Base64Utils.encodeToString(data);
    assertThat(new String(Base64Utils.decodeFromString(encoded)))
            .isEqualTo(input);
  }

  @Test
  void nullCharactersShouldBePreserved() {
    byte[] data = "Hello\0World".getBytes(StandardCharsets.UTF_8);
    assertThat(Base64Utils.decode(Base64Utils.encode(data)))
            .isEqualTo(data);
  }

  @Test
  void whitespaceInEncodedStringShouldBeRejected() {
    String encoded = "SGVs bG8=";
    assertThatThrownBy(() -> Base64Utils.decodeFromString(encoded))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void nonAsciiCharactersShouldBeRejected() {
    String encoded = "SGVsbG8世界=";
    assertThatThrownBy(() -> Base64Utils.decodeFromString(encoded))
            .isInstanceOf(IllegalArgumentException.class);
  }

}