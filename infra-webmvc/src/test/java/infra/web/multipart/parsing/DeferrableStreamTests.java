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

package infra.web.multipart.parsing;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/12/4 21:37
 */
class DeferrableStreamTests {

  @Test
  void constructorWithZeroThresholdCreatesPersistedState() throws IOException {
    DefaultMultipartParser parser = mock(DefaultMultipartParser.class);
    when(parser.getThreshold()).thenReturn(0L);
    when(parser.getTempRepository()).thenReturn(Path.of(System.getProperty("java.io.tmpdir")));
    when(parser.isDeleteOnExit()).thenReturn(false);

    DeferrableStream stream = new DeferrableStream(parser);

    assertThat(stream.state).isEqualTo(DeferrableStream.State.persisted);
    assertThat(stream).extracting("wasPersisted").isEqualTo(true);
    assertThat(stream).extracting("baos").isNull();
  }

  @Test
  void constructorWithPositiveThresholdCreatesInitializedState() throws IOException {
    DefaultMultipartParser parser = mock(DefaultMultipartParser.class);
    when(parser.getThreshold()).thenReturn(1024L);

    DeferrableStream stream = new DeferrableStream(parser);

    assertThat(stream.state).isEqualTo(DeferrableStream.State.initialized);
    assertThat(stream).extracting("wasPersisted").isEqualTo(false);
    assertThat(stream).extracting("baos").isNotNull();
  }

  @Test
  void writeBelowThresholdKeepsDataInMemory() throws IOException {
    DefaultMultipartParser parser = mock(DefaultMultipartParser.class);
    when(parser.getThreshold()).thenReturn(10L);
    when(parser.getTempRepository()).thenReturn(Path.of(System.getProperty("java.io.tmpdir")));
    when(parser.isDeleteOnExit()).thenReturn(false);

    DeferrableStream stream = new DeferrableStream(parser);
    stream.write("hello".getBytes());

    assertThat(stream.state).isEqualTo(DeferrableStream.State.opened);
    assertThat(stream.isInMemory()).isTrue();
    assertThat(stream.size).isEqualTo(5L);
  }

  @Test
  void writeExceedingThresholdPersistsDataToFile() throws IOException {
    DefaultMultipartParser parser = mock(DefaultMultipartParser.class);
    when(parser.getThreshold()).thenReturn(3L);
    when(parser.getTempRepository()).thenReturn(Path.of(System.getProperty("java.io.tmpdir")));
    when(parser.isDeleteOnExit()).thenReturn(false);

    DeferrableStream stream = new DeferrableStream(parser);
    stream.write("hello".getBytes());

    assertThat(stream.state).isEqualTo(DeferrableStream.State.persisted);
    assertThat(stream).extracting("wasPersisted").isEqualTo(true);
    assertThat(stream.isInMemory()).isFalse();
    assertThat(stream.size).isEqualTo(5L);
  }

  @Test
  void closeInInitializedStateStoresBytes() throws IOException {
    DefaultMultipartParser parser = mock(DefaultMultipartParser.class);
    when(parser.getThreshold()).thenReturn(10L);

    DeferrableStream stream = new DeferrableStream(parser);
    stream.close();

    assertThat(stream.state).isEqualTo(DeferrableStream.State.closed);
    assertThat(stream.bytes).isNotNull();
    assertThat(stream).extracting("baos").isNull();
  }

  @Test
  void closeInPersistedStateClearsBytes() throws IOException {
    DefaultMultipartParser parser = mock(DefaultMultipartParser.class);
    when(parser.getThreshold()).thenReturn(3L);
    when(parser.getTempRepository()).thenReturn(Path.of(System.getProperty("java.io.tmpdir")));
    when(parser.isDeleteOnExit()).thenReturn(false);

    DeferrableStream stream = new DeferrableStream(parser);
    stream.write("hello".getBytes());
    stream.close();

    assertThat(stream.state).isEqualTo(DeferrableStream.State.closed);
    assertThat(stream.bytes).isNull();
    assertThat(stream.path).isNotNull();
  }

  @Test
  void getInputStreamFromClosedInMemoryStream() throws IOException {
    DefaultMultipartParser parser = mock(DefaultMultipartParser.class);
    when(parser.getThreshold()).thenReturn(10L);

    DeferrableStream stream = new DeferrableStream(parser);
    stream.write("hello".getBytes());
    stream.close();

    InputStream inputStream = stream.getInputStream();
    assertThat(inputStream).isNotNull();
    assertThat(inputStream).isInstanceOf(ByteArrayInputStream.class);
  }

  @Test
  void getInputStreamFromClosedFileStream() throws IOException {
    DefaultMultipartParser parser = mock(DefaultMultipartParser.class);
    when(parser.getThreshold()).thenReturn(3L);
    when(parser.getTempRepository()).thenReturn(Path.of(System.getProperty("java.io.tmpdir")));
    when(parser.isDeleteOnExit()).thenReturn(false);

    DeferrableStream stream = new DeferrableStream(parser);
    stream.write("hello".getBytes());
    stream.close();

    InputStream inputStream = stream.getInputStream();
    assertThat(inputStream).isNotNull();
    assertThat(inputStream).isNotInstanceOf(ByteArrayInputStream.class);
  }

  @Test
  void getInputStreamFromOpenStreamThrowsException() throws IOException {
    DefaultMultipartParser parser = mock(DefaultMultipartParser.class);
    when(parser.getThreshold()).thenReturn(10L);

    DeferrableStream stream = new DeferrableStream(parser);

    assertThatThrownBy(() -> stream.getInputStream())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("This stream isn't yet closed.");
  }

  @Test
  void writeSingleByteBelowThreshold() throws IOException {
    DefaultMultipartParser parser = mock(DefaultMultipartParser.class);
    when(parser.getThreshold()).thenReturn(2L);

    DeferrableStream stream = new DeferrableStream(parser);
    stream.write('a');

    assertThat(stream.state).isEqualTo(DeferrableStream.State.opened);
    assertThat(stream.size).isEqualTo(1L);
  }

  @Test
  void writeSingleByteExceedingThreshold() throws IOException {
    DefaultMultipartParser parser = mock(DefaultMultipartParser.class);
    when(parser.getThreshold()).thenReturn(0L);
    when(parser.getTempRepository()).thenReturn(Path.of(System.getProperty("java.io.tmpdir")));
    when(parser.isDeleteOnExit()).thenReturn(false);

    DeferrableStream stream = new DeferrableStream(parser);
    stream.write('a');

    assertThat(stream.state).isEqualTo(DeferrableStream.State.persisted);
    assertThat(stream.size).isEqualTo(1L);
  }

  @Test
  void writeToClosedStreamThrowsException() throws IOException {
    DefaultMultipartParser parser = mock(DefaultMultipartParser.class);
    when(parser.getThreshold()).thenReturn(10L);

    DeferrableStream stream = new DeferrableStream(parser);
    stream.close();

    assertThatThrownBy(() -> stream.write('a'))
            .isInstanceOf(IOException.class)
            .hasMessage("This stream has already been closed.");
  }

  @Test
  void isInMemoryReturnsTrueForInMemoryData() throws IOException {
    DefaultMultipartParser parser = mock(DefaultMultipartParser.class);
    when(parser.getThreshold()).thenReturn(10L);

    DeferrableStream stream = new DeferrableStream(parser);
    stream.write("data".getBytes());

    assertThat(stream.isInMemory()).isTrue();
  }

  @Test
  void isInMemoryReturnsFalseForFileData() throws IOException {
    DefaultMultipartParser parser = mock(DefaultMultipartParser.class);
    when(parser.getThreshold()).thenReturn(2L);
    when(parser.getTempRepository()).thenReturn(Path.of(System.getProperty("java.io.tmpdir")));
    when(parser.isDeleteOnExit()).thenReturn(false);

    DeferrableStream stream = new DeferrableStream(parser);
    stream.write("data".getBytes());

    assertThat(stream.isInMemory()).isFalse();
  }

}