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
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link Content}.
 *
 * @author Phillip Webb
 */
class ContentTests {

  @Test
  void ofWhenStreamIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> Content.of(1, null))
            .withMessage("Supplier is required");
  }

  @Test
  void ofWhenStreamReturnsWritable() throws Exception {
    byte[] bytes = { 1, 2, 3, 4 };
    ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
    Content writable = Content.of(4, () -> inputStream);
    assertThat(writeToAndGetBytes(writable)).isEqualTo(bytes);
  }

  @Test
  void ofWhenStringIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> Content.of((String) null))
            .withMessage("String is required");
  }

  @Test
  void ofWhenStringReturnsWritable() throws Exception {
    Content writable = Content.of("spring");
    assertThat(writeToAndGetBytes(writable)).isEqualTo("spring".getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void ofWhenBytesIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> Content.of((byte[]) null))
            .withMessage("Bytes is required");
  }

  @Test
  void ofWhenBytesReturnsWritable() throws Exception {
    byte[] bytes = { 1, 2, 3, 4 };
    Content writable = Content.of(bytes);
    assertThat(writeToAndGetBytes(writable)).isEqualTo(bytes);
  }

  private byte[] writeToAndGetBytes(Content writable) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    writable.writeTo(outputStream);
    return outputStream.toByteArray();
  }

}
