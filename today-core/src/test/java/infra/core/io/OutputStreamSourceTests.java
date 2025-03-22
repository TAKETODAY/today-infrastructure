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

package infra.core.io;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import infra.lang.Constant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/22 14:55
 */
class OutputStreamSourceTests {

  @Test
  void outputStreamWritesData() throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    OutputStreamSource source = () -> output;

    try (OutputStream stream = source.getOutputStream()) {
      stream.write("test".getBytes());
    }

    assertThat(output.toString()).isEqualTo("test");
  }

  @Test
  void writerWritesCharacterData() throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    OutputStreamSource source = () -> output;

    try (Writer writer = source.getWriter()) {
      writer.write("测试");
    }

    assertThat(output.toString(StandardCharsets.UTF_8)).isEqualTo("测试");
  }

  @Test
  void channelWritesBytes() throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    OutputStreamSource source = () -> output;

    ByteBuffer buffer = ByteBuffer.wrap("test".getBytes());
    try (WritableByteChannel channel = source.writableChannel()) {
      channel.write(buffer);
    }

    assertThat(output.toString()).isEqualTo("test");
  }

  @Test
  void nullOutputStreamThrowsException() {
    OutputStreamSource source = () -> null;
    assertThatNullPointerException().isThrownBy(source::getWriter);
  }

  @Test
  void outputStreamThrowsIOException() {
    OutputStreamSource source = () -> {
      throw new IOException("Test exception");
    };

    assertThatIOException()
            .isThrownBy(source::getOutputStream)
            .withMessage("Test exception");
  }

  @Test
  void writerUsesDefaultCharset() throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    OutputStreamSource source = () -> output;

    try (Writer writer = source.getWriter()) {
      writer.write("测试");
    }

    assertThat(output.toString(Constant.DEFAULT_CHARSET)).isEqualTo("测试");
  }

  @Test
  void channelWritesLargeData() throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    OutputStreamSource source = () -> output;

    byte[] data = new byte[8192];
    Arrays.fill(data, (byte) 'x');
    ByteBuffer buffer = ByteBuffer.wrap(data);

    try (WritableByteChannel channel = source.writableChannel()) {
      channel.write(buffer);
    }

    assertThat(output.toByteArray()).containsExactly(data);
  }

}