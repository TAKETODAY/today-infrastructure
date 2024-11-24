/*
 * Copyright 2017 - 2024 the original author or authors.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import infra.util.DigestUtils;
import infra.util.FastByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author TODAY 2021/8/21 01:26
 */
class FastByteArrayOutputStreamTests {

  private static final int INITIAL_CAPACITY = 256;

  private final FastByteArrayOutputStream os = new FastByteArrayOutputStream(INITIAL_CAPACITY);

  private final byte[] helloBytes = "Hello World".getBytes(StandardCharsets.UTF_8);

  @Test
  void size() throws Exception {
    this.os.write(this.helloBytes);
    assertThat(this.helloBytes.length).isEqualTo(this.os.size());
  }

  @Test
  void resize() throws Exception {
    this.os.write(this.helloBytes);
    int sizeBefore = this.os.size();
    this.os.resize(64);
    assertByteArrayEqualsString(this.os);
    assertThat(this.os.size()).isEqualTo(sizeBefore);
  }

  @Test
  void stringConversion() throws Exception {
    this.os.write(this.helloBytes);
    assertThat(this.os.toString()).isEqualTo("Hello World");
    assertThat(this.os.toString(StandardCharsets.UTF_8)).isEqualTo("Hello World");

    @SuppressWarnings("resource")
    FastByteArrayOutputStream empty = new FastByteArrayOutputStream();
    assertThat(empty.toString()).isEqualTo("");
    assertThat(empty.toString(StandardCharsets.US_ASCII)).isEqualTo("");

    @SuppressWarnings("resource")
    FastByteArrayOutputStream outputStream = new FastByteArrayOutputStream(5);
    // Add bytes in multiple writes to ensure we get more than one buffer internally
    outputStream.write(this.helloBytes, 0, 5);
    outputStream.write(this.helloBytes, 5, 6);
    assertThat(outputStream.toString()).isEqualTo("Hello World");
    assertThat(outputStream.toString(StandardCharsets.UTF_8)).isEqualTo("Hello World");
  }

  @Test
  void autoGrow() throws IOException {
    this.os.resize(1);
    for (int i = 0; i < 10; i++) {
      this.os.write(1);
    }
    assertThat(this.os.size()).isEqualTo(10);
    assertThat(new byte[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 }).isEqualTo(this.os.toByteArray());
  }

  @Test
  void write() throws Exception {
    this.os.write(this.helloBytes);
    assertByteArrayEqualsString(this.os);
  }

  @Test
  void reset() throws Exception {
    this.os.write(this.helloBytes);
    assertByteArrayEqualsString(this.os);
    this.os.reset();
    assertThat(this.os.size()).isZero();
    this.os.write(this.helloBytes);
    assertByteArrayEqualsString(this.os);
  }

  @Test
  void close() {
    this.os.close();
    assertThatIOException().isThrownBy(() ->
            this.os.write(this.helloBytes));
  }

  @Test
  void toByteArrayUnsafe() throws Exception {
    this.os.write(this.helloBytes);
    assertByteArrayEqualsString(this.os);
    assertThat(this.os.toByteArrayUnsafe()).isSameAs(this.os.toByteArrayUnsafe());
    assertThat(this.helloBytes).isEqualTo(this.os.toByteArray());
  }

  @Test
  void writeTo() throws Exception {
    this.os.write(this.helloBytes);
    assertByteArrayEqualsString(this.os);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    this.os.writeTo(baos);
    assertThat(this.helloBytes).isEqualTo(baos.toByteArray());
  }

  @Test
  void failResize() throws Exception {
    this.os.write(this.helloBytes);
    assertThatIllegalArgumentException().isThrownBy(() ->
            this.os.resize(5));
  }

  @Test
  void getInputStream() throws Exception {
    this.os.write(this.helloBytes);
    assertThat(this.os.getInputStream()).isNotNull();
  }

  @Test
  void getInputStreamAvailable() throws Exception {
    this.os.write(this.helloBytes);
    assertThat(this.helloBytes.length).isEqualTo(this.os.getInputStream().available());
  }

  @Test
  void getInputStreamRead() throws Exception {
    this.os.write(this.helloBytes);
    InputStream inputStream = this.os.getInputStream();
    assertThat(this.helloBytes[0]).isEqualTo((byte) inputStream.read());
    assertThat(this.helloBytes[1]).isEqualTo((byte) inputStream.read());
    assertThat(this.helloBytes[2]).isEqualTo((byte) inputStream.read());
    assertThat(this.helloBytes[3]).isEqualTo((byte) inputStream.read());
  }

  @Test
  void getInputStreamReadBytePromotion() throws Exception {
    byte[] bytes = new byte[] { -1 };
    this.os.write(bytes);
    InputStream inputStream = this.os.getInputStream();
    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
    assertThat(inputStream.read()).isEqualTo(bais.read());
  }

  @Test
  void getInputStreamReadAll() throws Exception {
    this.os.write(this.helloBytes);
    InputStream inputStream = this.os.getInputStream();
    byte[] actual = new byte[inputStream.available()];
    int bytesRead = inputStream.read(actual);
    assertThat(bytesRead).isEqualTo(this.helloBytes.length);
    assertThat(actual).isEqualTo(this.helloBytes);
    assertThat(inputStream.available()).isZero();
  }

  @Test
  void getInputStreamReadBeyondEndOfStream() throws Exception {
    this.os.write(this.helloBytes);
    InputStream inputStream = os.getInputStream();
    byte[] actual = new byte[inputStream.available() + 1];
    int bytesRead = inputStream.read(actual);
    assertThat(bytesRead).isEqualTo(this.helloBytes.length);
    for (int i = 0; i < bytesRead; i++) {
      assertThat(actual[i]).isEqualTo(this.helloBytes[i]);
    }
    assertThat(actual[this.helloBytes.length]).isEqualTo((byte) 0);
    assertThat(inputStream.available()).isZero();
  }

  @Test
  void getInputStreamSkip() throws Exception {
    this.os.write(this.helloBytes);
    InputStream inputStream = this.os.getInputStream();
    assertThat(this.helloBytes[0]).isEqualTo((byte) inputStream.read());
    assertThat(inputStream.skip(1)).isEqualTo(1);
    assertThat(this.helloBytes[2]).isEqualTo((byte) inputStream.read());
    assertThat(inputStream.available()).isEqualTo((this.helloBytes.length - 3));
  }

  @Test
  void getInputStreamSkipAll() throws Exception {
    this.os.write(this.helloBytes);
    InputStream inputStream = this.os.getInputStream();
    assertThat(this.helloBytes.length).isEqualTo(inputStream.skip(1000));
    assertThat(inputStream.available()).isZero();
  }

  @Test
  void updateMessageDigest() throws Exception {
    StringBuilder builder = new StringBuilder("\"0");
    this.os.write(this.helloBytes);
    InputStream inputStream = this.os.getInputStream();
    DigestUtils.appendMd5DigestAsHex(inputStream, builder);
    builder.append('"');
    String actual = builder.toString();
    assertThat(actual).isEqualTo("\"0b10a8db164e0754105b7a99be72e3fe5\"");
  }

  @Test
  void updateMessageDigestManyBuffers() throws Exception {
    StringBuilder builder = new StringBuilder("\"0");
    // filling at least one 256 buffer
    for (int i = 0; i < 30; i++) {
      this.os.write(this.helloBytes);
    }
    InputStream inputStream = this.os.getInputStream();
    DigestUtils.appendMd5DigestAsHex(inputStream, builder);
    builder.append('"');
    String actual = builder.toString();
    assertThat(actual).isEqualTo("\"06225ca1e4533354c516e74512065331d\"");
  }

  private void assertByteArrayEqualsString(FastByteArrayOutputStream actual) {
    assertThat(actual.toByteArray()).isEqualTo(this.helloBytes);
  }

}
