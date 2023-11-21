/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.buildpack.platform.io;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link InspectedContent}.
 *
 * @author Phillip Webb
 */
class InspectedContentTests {

  @Test
  void ofWhenInputStreamThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> InspectedContent.of((InputStream) null))
            .withMessage("InputStream is required");
  }

  @Test
  void ofWhenContentIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> InspectedContent.of((Content) null))
            .withMessage("Content is required");
  }

  @Test
  void ofWhenConsumerIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> InspectedContent.of((IOConsumer<OutputStream>) null))
            .withMessage("Writer is required");
  }

  @Test
  void ofFromContent() throws Exception {
    InspectedContent content = InspectedContent.of(Content.of("test"));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    content.writeTo(outputStream);
    assertThat(outputStream.toByteArray()).containsExactly("test".getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void ofSmallContent() throws Exception {
    InputStream inputStream = new ByteArrayInputStream(new byte[] { 0, 1, 2 });
    InspectedContent content = InspectedContent.of(inputStream);
    assertThat(content.size()).isEqualTo(3);
    assertThat(readBytes(content)).containsExactly(0, 1, 2);
  }

  @Test
  void ofLargeContent() throws Exception {
    byte[] bytes = new byte[InspectedContent.MEMORY_LIMIT + 3];
    System.arraycopy(new byte[] { 0, 1, 2 }, 0, bytes, 0, 3);
    InputStream inputStream = new ByteArrayInputStream(bytes);
    InspectedContent content = InspectedContent.of(inputStream);
    assertThat(content.size()).isEqualTo(bytes.length);
    assertThat(readBytes(content)).isEqualTo(bytes);
  }

  @Test
  void ofWithInspector() throws Exception {
    InputStream inputStream = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    InspectedContent.of(inputStream, digest::update);
    assertThat(digest.digest()).inHexadecimal()
            .contains(0x9f, 0x86, 0xd0, 0x81, 0x88, 0x4c, 0x7d, 0x65, 0x9a, 0x2f, 0xea, 0xa0, 0xc5, 0x5a, 0xd0, 0x15,
                    0xa3, 0xbf, 0x4f, 0x1b, 0x2b, 0x0b, 0x82, 0x2c, 0xd1, 0x5d, 0x6c, 0x15, 0xb0, 0xf0, 0x0a, 0x08);
  }

  private byte[] readBytes(InspectedContent content) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    content.writeTo(outputStream);
    return outputStream.toByteArray();
  }

}
