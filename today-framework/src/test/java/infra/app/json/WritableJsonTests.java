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

package infra.app.json;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import infra.core.io.FileSystemResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/2/22 17:32
 */
class WritableJsonTests {

  @TempDir
  File temp;

  @Test
  void toJsonStringReturnsString() {
    WritableJson writable = (out) -> out.append("{}");
    assertThat(writable.toJsonString()).isEqualTo("{}");
  }

  @Test
  void toJsonStringWhenIOExceptionIsThrownThrowsUncheckedIOException() {
    WritableJson writable = (out) -> {
      throw new IOException("bad");
    };
    assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() -> writable.toJsonString())
            .havingCause()
            .withMessage("bad");
  }

  @Test
  void toByteArrayReturnsByteArray() {
    WritableJson writable = (out) -> out.append("{}");
    assertThat(writable.toByteArray()).isEqualTo("{}".getBytes());
  }

  @Test
  void toResourceWritesJson() throws Exception {
    File file = new File(this.temp, "out.json");
    WritableJson writable = (out) -> out.append("{}");
    writable.toResource(new FileSystemResource(file));
    assertThat(file).content().isEqualTo("{}");
  }

  @Test
  void toResourceWithCharsetWritesJson() throws Exception {
    File file = new File(this.temp, "out.json");
    WritableJson writable = (out) -> out.append("{}");
    writable.toResource(new FileSystemResource(file), StandardCharsets.ISO_8859_1);
    assertThat(file).content(StandardCharsets.ISO_8859_1).isEqualTo("{}");
  }

  @Test
  void toResourceWithCharsetWhenOutIsNullThrowsException() {
    WritableJson writable = (out) -> out.append("{}");
    assertThatIllegalArgumentException().isThrownBy(() -> writable.toResource(null, StandardCharsets.UTF_8))
            .withMessage("'out' is required");
  }

  @Test
  void toResourceWithCharsetWhenCharsetIsNullThrowsException() {
    File file = new File(this.temp, "out.json");
    WritableJson writable = (out) -> out.append("{}");
    assertThatIllegalArgumentException().isThrownBy(() -> writable.toResource(new FileSystemResource(file), null))
            .withMessage("'charset' is required");
  }

  @Test
  void toOutputStreamWritesJson() throws Exception {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    WritableJson writable = (out) -> out.append("{}");
    writable.toOutputStream(outputStream);
    assertThat(outputStream.toString(StandardCharsets.UTF_8)).isEqualTo("{}");
  }

  @Test
  void toOutputStreamWithCharsetWritesJson() throws Exception {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    WritableJson writable = (out) -> out.append("{}");
    writable.toOutputStream(outputStream, StandardCharsets.ISO_8859_1);
    assertThat(outputStream.toString(StandardCharsets.ISO_8859_1)).isEqualTo("{}");
  }

  @Test
  void toOutputStreamWithCharsetWhenOutIsNullThrowsException() {
    WritableJson writable = (out) -> out.append("{}");
    assertThatIllegalArgumentException().isThrownBy(() -> writable.toOutputStream(null, StandardCharsets.UTF_8))
            .withMessage("'out' is required");
  }

  @Test
  void toOutputStreamWithCharsetWhenCharsetIsNullThrowsException() {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    WritableJson writable = (out) -> out.append("{}");
    assertThatIllegalArgumentException().isThrownBy(() -> writable.toOutputStream(outputStream, null))
            .withMessage("'charset' is required");
  }

  @Test
  void toWriterWritesJson() throws Exception {
    StringWriter writer = new StringWriter();
    WritableJson writable = (out) -> out.append("{}");
    writable.toWriter(writer);
    assertThat(writer).hasToString("{}");
  }

  @Test
  void toWriterWhenWriterIsNullThrowsException() {
    WritableJson writable = (out) -> out.append("{}");
    assertThatIllegalArgumentException().isThrownBy(() -> writable.toWriter(null))
            .withMessage("'out' is required");
  }

  @Test
  void ofReturnsInstanceWithSensibleToString() {
    WritableJson writable = WritableJson.of((out) -> out.append("{}"));
    assertThat(writable).hasToString("{}");
  }

}