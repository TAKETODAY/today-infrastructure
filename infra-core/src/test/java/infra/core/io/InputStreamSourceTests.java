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

import org.junit.jupiter.api.Disabled;
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

    try (Reader reader = source.getReader(StandardCharsets.ISO_8859_1)) {
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
  @Disabled
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