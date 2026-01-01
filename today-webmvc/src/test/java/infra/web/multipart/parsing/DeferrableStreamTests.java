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