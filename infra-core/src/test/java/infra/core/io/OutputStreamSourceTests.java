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