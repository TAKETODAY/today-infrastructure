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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import infra.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/22 15:00
 */
class InputStreamSourceTests {

  @Test
  void getInputStreamReturnsValidStream() throws IOException {
    InputStreamSource source = () -> new ByteArrayInputStream("test".getBytes());

    try (InputStream stream = source.getInputStream()) {
      assertThat(stream.readAllBytes()).isEqualTo("test".getBytes());
    }
  }

  @Test
  void getReaderWithDefaultEncodingReadsUtf8() throws IOException {
    InputStreamSource source = () -> new ByteArrayInputStream("测试".getBytes(StandardCharsets.UTF_8));

    try (Reader reader = source.getReader()) {
      char[] chars = new char[2];
      reader.read(chars);
      assertThat(new String(chars)).isEqualTo("测试");
    }
  }

  @Test
  void getReaderWithCustomEncodingReadsCorrectly() throws IOException {
    InputStreamSource source = () -> new ByteArrayInputStream("test".getBytes(StandardCharsets.ISO_8859_1));

    try (Reader reader = source.getReader("ISO-8859-1")) {
      assertThat(FileCopyUtils.copyToString(reader)).isEqualTo("test");
    }
  }

  @Test
  void readableChannelReadsAllContent() throws IOException {
    String content = "test content";
    InputStreamSource source = () -> new ByteArrayInputStream(content.getBytes());

    try (ReadableByteChannel channel = source.readableChannel()) {
      ByteBuffer buffer = ByteBuffer.allocate(content.length());
      channel.read(buffer);
      buffer.flip();
      assertThat(new String(buffer.array())).isEqualTo(content);
    }
  }

  @Test
  void transferToWritesAllContent() throws IOException {
    byte[] content = "test content".getBytes();
    InputStreamSource source = () -> new ByteArrayInputStream(content);
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    long transferred = source.transferTo(output);

    assertThat(transferred).isEqualTo(content.length);
    assertThat(output.toByteArray()).isEqualTo(content);
  }

  @Test
  void acceptWithExceptionTransfersContent() throws Exception {
    byte[] content = "test content".getBytes();
    InputStreamSource source = () -> new ByteArrayInputStream(content);
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    source.acceptWithException(output);

    assertThat(output.toByteArray()).isEqualTo(content);
  }

  @Test
  void transferToWithNullOutputStreamThrowsException() {
    InputStreamSource source = () -> new ByteArrayInputStream(new byte[0]);

    assertThatNullPointerException()
            .isThrownBy(() -> source.transferTo(null));
  }

  @Test
  void streamClosedAfterTransferTo() throws IOException {
    AtomicBoolean streamClosed = new AtomicBoolean(false);
    InputStream wrappedStream = new ByteArrayInputStream("test".getBytes()) {
      @Override
      public void close() throws IOException {
        streamClosed.set(true);
        super.close();
      }
    };

    InputStreamSource source = () -> wrappedStream;
    source.transferTo(new ByteArrayOutputStream());

    assertThat(streamClosed).isTrue();
  }

}