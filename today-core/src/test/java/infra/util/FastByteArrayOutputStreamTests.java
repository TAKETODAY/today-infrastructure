/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

  @Test
  void writeSingleByteIncrementsSize() throws IOException {
    FastByteArrayOutputStream os = new FastByteArrayOutputStream();
    os.write(65);
    assertThat(os.size()).isEqualTo(1);
  }

  @Test
  void writeMultipleBytesIncrementsSizeCorrectly() throws IOException {
    FastByteArrayOutputStream os = new FastByteArrayOutputStream();
    byte[] data = "Hello".getBytes();
    os.write(data);
    assertThat(os.size()).isEqualTo(5);
  }

  @Test
  void writeByteArrayWithOffsetAndLength() throws IOException {
    FastByteArrayOutputStream os = new FastByteArrayOutputStream();
    byte[] data = "Hello World".getBytes();
    os.write(data, 6, 5);
    assertThat(os.toString()).isEqualTo("World");
  }

  @Test
  void writingAfterClosedStreamThrowsException() {
    FastByteArrayOutputStream os = new FastByteArrayOutputStream();
    os.close();
    assertThatIOException().isThrownBy(() -> os.write(1));
  }

  @Test
  void writingNegativeOffsetThrowsException() {
    FastByteArrayOutputStream os = new FastByteArrayOutputStream();
    byte[] data = "Test".getBytes();
    assertThatThrownBy(() -> os.write(data, -1, 4))
            .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void writingNegativeLengthThrowsException() {
    FastByteArrayOutputStream os = new FastByteArrayOutputStream();
    byte[] data = "Test".getBytes();
    assertThatThrownBy(() -> os.write(data, 0, -1))
            .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void writingBeyondArrayBoundsThrowsException() {
    FastByteArrayOutputStream os = new FastByteArrayOutputStream();
    byte[] data = "Test".getBytes();
    assertThatThrownBy(() -> os.write(data, 2, 4))
            .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void resettingStreamClearsContent() throws IOException {
    FastByteArrayOutputStream os = new FastByteArrayOutputStream();
    os.write("Hello".getBytes());
    os.reset();
    assertThat(os.size()).isZero();
    assertThat(os.toString()).isEmpty();
  }

  @Test
  void resizingToSmallerCapacityThrowsException() throws IOException {
    FastByteArrayOutputStream os = new FastByteArrayOutputStream();
    os.write("Hello".getBytes());
    assertThatIllegalArgumentException().isThrownBy(() -> os.resize(2));
  }

  @Test
  void autoResizingWorksForLargeWrites() throws IOException {
    FastByteArrayOutputStream os = new FastByteArrayOutputStream(4);
    byte[] largeData = new byte[1000];
    os.write(largeData);
    assertThat(os.size()).isEqualTo(1000);
  }

  @Test
  void readingFromInputStreamMatchesWrittenData() throws IOException {
    FastByteArrayOutputStream os = new FastByteArrayOutputStream();
    os.write("Test Data".getBytes());

    InputStream is = os.getInputStream();
    byte[] readData = new byte[9];
    int bytesRead = is.read(readData);

    assertThat(bytesRead).isEqualTo(9);
    assertThat(new String(readData)).isEqualTo("Test Data");
  }

  @Test
  void writeToOutputStreamTransfersAllData() throws IOException {
    FastByteArrayOutputStream os = new FastByteArrayOutputStream();
    os.write("Test Data".getBytes());

    ByteArrayOutputStream target = new ByteArrayOutputStream();
    os.writeTo(target);

    assertThat(target.toString()).isEqualTo("Test Data");
  }

  @Test
  void getInputStreamSkipNegativeBytesThrowsException() throws IOException {
    os.write(helloBytes);
    InputStream inputStream = os.getInputStream();
    assertThatIllegalArgumentException()
            .isThrownBy(() -> inputStream.skip(-1));
  }

  @Test
  void getInputStreamSkipZeroReturnsZero() throws IOException {
    os.write(helloBytes);
    InputStream inputStream = os.getInputStream();
    assertThat(inputStream.skip(0)).isZero();
  }

  @Test
  void multipleBuffersAreHandledCorrectly() throws IOException {
    int size = 1000;
    byte[] data = new byte[size];
    for (int i = 0; i < size; i++) {
      data[i] = (byte) (i % 256);
    }

    os.write(data);
    assertThat(os.size()).isEqualTo(size);
    assertThat(os.toByteArray()).isEqualTo(data);
  }

  @Test
  void getInputStreamReadEmptyArray() throws IOException {
    os.write(helloBytes);
    InputStream is = os.getInputStream();
    byte[] empty = new byte[0];
    assertThat(is.read(empty)).isZero();
  }

  @Test
  void getInputStreamReadWithInvalidOffset() throws IOException {
    os.write(helloBytes);
    InputStream is = os.getInputStream();
    byte[] buffer = new byte[10];
    assertThatThrownBy(() -> is.read(buffer, -1, 5))
            .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void getInputStreamReadPartialBuffer() throws IOException {
    os.write(helloBytes);
    InputStream is = os.getInputStream();
    byte[] buffer = new byte[5];
    int read = is.read(buffer, 0, 3);
    assertThat(read).isEqualTo(3);
    assertThat(buffer[0]).isEqualTo(helloBytes[0]);
    assertThat(buffer[1]).isEqualTo(helloBytes[1]);
    assertThat(buffer[2]).isEqualTo(helloBytes[2]);
    assertThat(buffer[3]).isZero();
    assertThat(buffer[4]).isZero();
  }

  @Test
  void writeToClosedStreamThrowsException() {
    os.close();
    byte[] data = "test".getBytes();
    assertThatIOException()
            .isThrownBy(() -> os.write(data, 0, data.length));
  }

  @Test
  void inputStreamUpdateMessageDigestWithNegativeLengthThrowsException() throws IOException {
    os.write(helloBytes);
    FastByteArrayOutputStream.FastByteArrayInputStream is = (FastByteArrayOutputStream.FastByteArrayInputStream) os.getInputStream();
    assertThatIllegalArgumentException()
            .isThrownBy(() -> is.updateMessageDigest(MessageDigest.getInstance("MD5"), -1));
  }

  @Test
  void getInputStreamReadReturnsSameDataWithMultipleBuffers() throws IOException {
    FastByteArrayOutputStream os = new FastByteArrayOutputStream(4);
    byte[] data = "Hello World".getBytes();
    for (int i = 0; i < data.length; i++) {
      os.write(data[i]);
    }

    InputStream is = os.getInputStream();
    byte[] result = new byte[data.length];
    is.read(result);

    assertThat(result).isEqualTo(data);
  }

  @Test
  void getInputStreamReadPartialData() throws IOException {
    FastByteArrayOutputStream os = new FastByteArrayOutputStream();
    os.write("Hello World".getBytes());

    InputStream is = os.getInputStream();
    byte[] result = new byte[5];
    int read = is.read(result);

    assertThat(read).isEqualTo(5);
    assertThat(new String(result)).isEqualTo("Hello");
  }

  @Test
  void getInputStreamAvailableReturnsRemainingBytes() throws IOException {
    FastByteArrayOutputStream os = new FastByteArrayOutputStream();
    os.write("Hello".getBytes());

    InputStream is = os.getInputStream();
    is.read();
    assertThat(is.available()).isEqualTo(4);
  }

  @Test
  void resizeExpandsCapacityAndPreservesContent() throws IOException {
    FastByteArrayOutputStream os = new FastByteArrayOutputStream(4);
    os.write("Test".getBytes());
    os.resize(8);
    os.write("Data".getBytes());

    assertThat(os.toString()).isEqualTo("TestData");
  }

  @Test
  void writeByteArraySpanningMultipleBuffers() throws IOException {
    FastByteArrayOutputStream os = new FastByteArrayOutputStream(4);
    byte[] data = "Hello World".getBytes();
    os.write(data);

    assertThat(os.size()).isEqualTo(data.length);
    assertThat(os.toString()).isEqualTo("Hello World");
  }

  @Test
  void nextPowerOf2HandlesLargeValues() throws IOException {
    FastByteArrayOutputStream os = new FastByteArrayOutputStream(4);
    byte[] data = new byte[1025];
    os.write(data);

    assertThat(os.size()).isEqualTo(1025);
  }

  @Test
  void getInputStreamSkipMultipleBuffers() throws IOException {
    FastByteArrayOutputStream os = new FastByteArrayOutputStream(4);
    os.write("Hello World".getBytes());

    InputStream is = os.getInputStream();
    is.skip(6);
    byte[] result = new byte[5];
    is.read(result);

    assertThat(new String(result)).isEqualTo("World");
  }

  @Test
  void writeToTransfersAllDataBetweenBuffers() throws IOException {
    FastByteArrayOutputStream source = new FastByteArrayOutputStream(4);
    source.write("Hello World".getBytes());

    FastByteArrayOutputStream target = new FastByteArrayOutputStream();
    source.writeTo(target);

    assertThat(target.toString()).isEqualTo("Hello World");
    assertThat(target.size()).isEqualTo(source.size());
  }

  @Test
  void getInputStreamReadEmptyBuffer() throws IOException {
    FastByteArrayOutputStream os = new FastByteArrayOutputStream();
    InputStream is = os.getInputStream();

    byte[] result = new byte[10];
    int bytesRead = is.read(result);

    assertThat(bytesRead).isEqualTo(-1);
  }

  private void assertByteArrayEqualsString(FastByteArrayOutputStream actual) {
    assertThat(actual.toByteArray()).isEqualTo(this.helloBytes);
  }

}
