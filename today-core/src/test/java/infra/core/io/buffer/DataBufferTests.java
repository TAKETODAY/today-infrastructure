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

package infra.core.io.buffer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import infra.core.testfixture.io.buffer.AbstractDataBufferAllocatingTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Arjen Poutsma
 * @author Sam Brannen
 */
class DataBufferTests extends AbstractDataBufferAllocatingTests {

  @ParameterizedDataBufferAllocatingTest
  void byteCountsAndPositions(DataBufferFactory bufferFactory) {
    super.bufferFactory = bufferFactory;

    DataBuffer buffer = createDataBuffer(2);

    assertThat(buffer.readPosition()).isEqualTo(0);
    assertThat(buffer.writePosition()).isEqualTo(0);
    assertThat(buffer.readableBytes()).isEqualTo(0);
    assertThat(buffer.writableBytes()).isEqualTo(2);
    assertThat(buffer.capacity()).isEqualTo(2);

    buffer.write((byte) 'a');
    assertThat(buffer.readPosition()).isEqualTo(0);
    assertThat(buffer.writePosition()).isEqualTo(1);
    assertThat(buffer.readableBytes()).isEqualTo(1);
    assertThat(buffer.writableBytes()).isEqualTo(1);
    assertThat(buffer.capacity()).isEqualTo(2);

    buffer.write((byte) 'b');
    assertThat(buffer.readPosition()).isEqualTo(0);
    assertThat(buffer.writePosition()).isEqualTo(2);
    assertThat(buffer.readableBytes()).isEqualTo(2);
    assertThat(buffer.writableBytes()).isEqualTo(0);
    assertThat(buffer.capacity()).isEqualTo(2);

    buffer.read();
    assertThat(buffer.readPosition()).isEqualTo(1);
    assertThat(buffer.writePosition()).isEqualTo(2);
    assertThat(buffer.readableBytes()).isEqualTo(1);
    assertThat(buffer.writableBytes()).isEqualTo(0);
    assertThat(buffer.capacity()).isEqualTo(2);

    buffer.read();
    assertThat(buffer.readPosition()).isEqualTo(2);
    assertThat(buffer.writePosition()).isEqualTo(2);
    assertThat(buffer.readableBytes()).isEqualTo(0);
    assertThat(buffer.writableBytes()).isEqualTo(0);
    assertThat(buffer.capacity()).isEqualTo(2);

    release(buffer);
  }

  @ParameterizedDataBufferAllocatingTest
  void readPositionSmallerThanZero(DataBufferFactory bufferFactory) {
    super.bufferFactory = bufferFactory;

    DataBuffer buffer = createDataBuffer(1);
    try {
      assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> buffer.readPosition(-1));
    }
    finally {
      release(buffer);
    }
  }

  @ParameterizedDataBufferAllocatingTest
  void readPositionGreaterThanWritePosition(DataBufferFactory bufferFactory) {
    super.bufferFactory = bufferFactory;

    DataBuffer buffer = createDataBuffer(1);
    try {
      assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> buffer.readPosition(1));
    }
    finally {
      release(buffer);
    }
  }

  @ParameterizedDataBufferAllocatingTest
  void writePositionSmallerThanReadPosition(DataBufferFactory bufferFactory) {
    super.bufferFactory = bufferFactory;

    DataBuffer buffer = createDataBuffer(2);
    try {
      buffer.write((byte) 'a');
      buffer.read();
      assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> buffer.writePosition(0));
    }
    finally {
      release(buffer);
    }
  }

  @ParameterizedDataBufferAllocatingTest
  void writePositionGreaterThanCapacity(DataBufferFactory bufferFactory) {
    super.bufferFactory = bufferFactory;

    DataBuffer buffer = createDataBuffer(1);
    try {
      assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> buffer.writePosition(2));
    }
    finally {
      release(buffer);
    }
  }

  @ParameterizedDataBufferAllocatingTest
  void writeAndRead(DataBufferFactory bufferFactory) {
    super.bufferFactory = bufferFactory;

    DataBuffer buffer = createDataBuffer(5);
    buffer.write(new byte[] { 'a', 'b', 'c' });

    int ch = buffer.read();
    assertThat(ch).isEqualTo((byte) 'a');

    buffer.write((byte) 'd');
    buffer.write((byte) 'e');

    byte[] result = new byte[4];
    buffer.read(result);

    assertThat(result).isEqualTo(new byte[] { 'b', 'c', 'd', 'e' });

    release(buffer);
  }

  @ParameterizedDataBufferAllocatingTest
  void writeNullString(DataBufferFactory bufferFactory) {
    super.bufferFactory = bufferFactory;

    DataBuffer buffer = createDataBuffer(1);
    try {
      assertThatIllegalArgumentException().isThrownBy(() ->
              buffer.write(null, StandardCharsets.UTF_8));
    }
    finally {
      release(buffer);
    }
  }

  @ParameterizedDataBufferAllocatingTest
  void writeNullCharset(DataBufferFactory bufferFactory) {
    super.bufferFactory = bufferFactory;

    DataBuffer buffer = createDataBuffer(1);
    try {
      assertThatIllegalArgumentException().isThrownBy(() ->
              buffer.write("test", null));
    }
    finally {
      release(buffer);
    }
  }

  @ParameterizedDataBufferAllocatingTest
  void writeEmptyString(DataBufferFactory bufferFactory) {
    super.bufferFactory = bufferFactory;

    DataBuffer buffer = createDataBuffer(1);
    buffer.write("", StandardCharsets.UTF_8);

    assertThat(buffer.readableBytes()).isEqualTo(0);

    release(buffer);
  }

  @ParameterizedDataBufferAllocatingTest
  void writeUtf8String(DataBufferFactory bufferFactory) {
    super.bufferFactory = bufferFactory;

    DataBuffer buffer = createDataBuffer(6);
    buffer.write("Spring", StandardCharsets.UTF_8);

    byte[] result = new byte[6];
    buffer.read(result);

    assertThat(result).isEqualTo("Spring".getBytes(StandardCharsets.UTF_8));
    release(buffer);
  }

  @ParameterizedDataBufferAllocatingTest
  void writeUtf8StringOutGrowsCapacity(DataBufferFactory bufferFactory) {
    super.bufferFactory = bufferFactory;

    DataBuffer buffer = createDataBuffer(5);
    buffer.write("Spring €", StandardCharsets.UTF_8);

    byte[] result = new byte[10];
    buffer.read(result);

    assertThat(result).isEqualTo("Spring €".getBytes(StandardCharsets.UTF_8));
    release(buffer);
  }

  @ParameterizedDataBufferAllocatingTest
  void writeIsoString(DataBufferFactory bufferFactory) {
    super.bufferFactory = bufferFactory;

    DataBuffer buffer = createDataBuffer(3);
    buffer.write("\u00A3", StandardCharsets.ISO_8859_1);

    byte[] result = new byte[1];
    buffer.read(result);

    assertThat(result).isEqualTo("\u00A3".getBytes(StandardCharsets.ISO_8859_1));
    release(buffer);
  }

  @ParameterizedDataBufferAllocatingTest
  void writeMultipleUtf8String(DataBufferFactory bufferFactory) {
    super.bufferFactory = bufferFactory;

    DataBuffer buffer = createDataBuffer(1);
    buffer.write("abc", StandardCharsets.UTF_8);
    assertThat(buffer.readableBytes()).isEqualTo(3);

    buffer.write("def", StandardCharsets.UTF_8);
    assertThat(buffer.readableBytes()).isEqualTo(6);

    buffer.write("ghi", StandardCharsets.UTF_8);
    assertThat(buffer.readableBytes()).isEqualTo(9);

    byte[] result = new byte[9];
    buffer.read(result);

    assertThat(result).isEqualTo("abcdefghi".getBytes());

    release(buffer);
  }

  @ParameterizedDataBufferAllocatingTest
  void toStringNullCharset(DataBufferFactory bufferFactory) {
    super.bufferFactory = bufferFactory;

    DataBuffer buffer = createDataBuffer(1);
    try {
      assertThatIllegalArgumentException().isThrownBy(() ->
              buffer.toString(null));
    }
    finally {
      release(buffer);
    }
  }

  @ParameterizedDataBufferAllocatingTest
  void toStringUtf8(DataBufferFactory bufferFactory) {
    super.bufferFactory = bufferFactory;

    String spring = "Spring";
    byte[] bytes = spring.getBytes(StandardCharsets.UTF_8);
    DataBuffer buffer = createDataBuffer(bytes.length);
    buffer.write(bytes);

    String result = buffer.toString(StandardCharsets.UTF_8);

    assertThat(result).isEqualTo(spring);
    release(buffer);
  }

  @ParameterizedDataBufferAllocatingTest
  void toStringSection(DataBufferFactory bufferFactory) {
    super.bufferFactory = bufferFactory;

    String spring = "Spring";
    byte[] bytes = spring.getBytes(StandardCharsets.UTF_8);
    DataBuffer buffer = createDataBuffer(bytes.length);
    buffer.write(bytes);

    String result = buffer.toString(1, 3, StandardCharsets.UTF_8);

    assertThat(result).isEqualTo("pri");
    release(buffer);
  }

  @ParameterizedDataBufferAllocatingTest
  void inputStream(DataBufferFactory bufferFactory) throws Exception {
    super.bufferFactory = bufferFactory;

    DataBuffer buffer = createDataBuffer(4);
    buffer.write(new byte[] { 'a', 'b', 'c', 'd', 'e' });
    buffer.readPosition(1);

    InputStream inputStream = buffer.asInputStream();

    assertThat(inputStream.available()).isEqualTo(4);

    int result = inputStream.read();
    assertThat(result).isEqualTo((byte) 'b');
    assertThat(inputStream.available()).isEqualTo(3);

    assertThat(inputStream.markSupported()).isTrue();
    inputStream.mark(2);

    byte[] bytes = new byte[2];
    int len = inputStream.read(bytes);
    assertThat(len).isEqualTo(2);
    assertThat(bytes).isEqualTo(new byte[] { 'c', 'd' });
    assertThat(inputStream.available()).isEqualTo(1);

    Arrays.fill(bytes, (byte) 0);
    len = inputStream.read(bytes);
    assertThat(len).isEqualTo(1);
    assertThat(bytes).isEqualTo(new byte[] { 'e', (byte) 0 });
    assertThat(inputStream.available()).isEqualTo(0);

    assertThat(inputStream.read()).isEqualTo(-1);
    assertThat(inputStream.read(bytes)).isEqualTo(-1);

    inputStream.reset();
    bytes = new byte[3];
    len = inputStream.read(bytes);
    assertThat(len).isEqualTo(3);
    assertThat(bytes).containsExactly('c', 'd', 'e');

    buffer.readPosition(0);
    inputStream = buffer.asInputStream();
    assertThat(inputStream.readAllBytes()).asString().isEqualTo("abcde");
    assertThat(inputStream.available()).isEqualTo(0);
    assertThat(inputStream.readAllBytes()).isEmpty();

    buffer.readPosition(0);
    inputStream = buffer.asInputStream();
    inputStream.mark(5);
    assertThat(inputStream.readNBytes(0)).isEmpty();
    assertThat(inputStream.readNBytes(1000)).asString().isEqualTo("abcde");
    inputStream.reset();
    assertThat(inputStream.readNBytes(3)).asString().isEqualTo("abc");
    assertThat(inputStream.readNBytes(2)).asString().isEqualTo("de");
    assertThat(inputStream.readNBytes(10)).isEmpty();

    buffer.readPosition(0);
    inputStream = buffer.asInputStream();
    inputStream.mark(5);
    assertThat(inputStream.skip(1)).isEqualTo(1);
    assertThat(inputStream.readAllBytes()).asString().isEqualTo("bcde");
    assertThat(inputStream.skip(10)).isEqualTo(0);
    assertThat(inputStream.available()).isEqualTo(0);
    inputStream.reset();
    assertThat(inputStream.skip(100)).isEqualTo(5);
    assertThat(inputStream.available()).isEqualTo(0);

    buffer.readPosition(0);
    inputStream = buffer.asInputStream();
    inputStream.mark(5);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    assertThat(inputStream.transferTo(out)).isEqualTo(5);
    assertThat(out.toByteArray()).asString().isEqualTo("abcde");
    assertThat(inputStream.available()).isEqualTo(0);
    out.reset();
    inputStream.reset();
    assertThat(inputStream.read()).isEqualTo('a');
    assertThat(inputStream.transferTo(out)).isEqualTo(4);
    assertThat(out.toByteArray()).asString().isEqualTo("bcde");
    assertThat(inputStream.available()).isEqualTo(0);
    assertThat(inputStream.transferTo(OutputStream.nullOutputStream())).isEqualTo(0);

    release(buffer);
  }

  @ParameterizedDataBufferAllocatingTest
  void inputStreamReleaseOnClose(DataBufferFactory bufferFactory) throws Exception {
    super.bufferFactory = bufferFactory;

    DataBuffer buffer = createDataBuffer(3);
    byte[] bytes = { 'a', 'b', 'c' };
    buffer.write(bytes);

    try (InputStream inputStream = buffer.asInputStream(true)) {
      byte[] result = new byte[3];
      int len = inputStream.read(result);
      assertThat(len).isEqualTo(3);
      assertThat(result).isEqualTo(bytes);
    }

    // AbstractDataBufferAllocatingTests.leakDetector will verify the buffer's release
  }

  @ParameterizedDataBufferAllocatingTest
  void outputStream(DataBufferFactory bufferFactory) throws Exception {
    super.bufferFactory = bufferFactory;

    DataBuffer buffer = createDataBuffer(4);
    buffer.write((byte) 'a');

    OutputStream outputStream = buffer.asOutputStream();
    outputStream.write('b');
    outputStream.write(new byte[] { 'c', 'd' });

    buffer.write((byte) 'e');

    byte[] bytes = new byte[5];
    buffer.read(bytes);
    assertThat(bytes).isEqualTo(new byte[] { 'a', 'b', 'c', 'd', 'e' });

    release(buffer);
  }

  @ParameterizedDataBufferAllocatingTest
  void expand(DataBufferFactory bufferFactory) {
    super.bufferFactory = bufferFactory;

    DataBuffer buffer = createDataBuffer(1);
    buffer.write((byte) 'a');
    assertThat(buffer.capacity()).isEqualTo(1);
    buffer.write((byte) 'b');

    assertThat(buffer.capacity()).isGreaterThan(1);

    release(buffer);
  }

  @ParameterizedDataBufferAllocatingTest
  void increaseCapacity(DataBufferFactory bufferFactory) {
    super.bufferFactory = bufferFactory;

    DataBuffer buffer = createDataBuffer(1);
    assertThat(buffer.capacity()).isEqualTo(1);

    buffer.capacity(2);
    assertThat(buffer.capacity()).isEqualTo(2);

    release(buffer);
  }

  @ParameterizedDataBufferAllocatingTest
  void decreaseCapacityLowReadPosition(DataBufferFactory bufferFactory) {

    super.bufferFactory = bufferFactory;

    DataBuffer buffer = createDataBuffer(2);
    buffer.writePosition(2);
    buffer.capacity(1);
    assertThat(buffer.capacity()).isEqualTo(1);

    release(buffer);
  }

  @ParameterizedDataBufferAllocatingTest
  void decreaseCapacityHighReadPosition(DataBufferFactory bufferFactory) {

    super.bufferFactory = bufferFactory;

    DataBuffer buffer = createDataBuffer(2);
    buffer.writePosition(2);
    buffer.readPosition(2);
    buffer.capacity(1);
    assertThat(buffer.capacity()).isEqualTo(1);

    release(buffer);
  }

  @ParameterizedDataBufferAllocatingTest
  void capacityLessThanZero(DataBufferFactory bufferFactory) {
    super.bufferFactory = bufferFactory;

    DataBuffer buffer = createDataBuffer(1);
    try {
      assertThatIllegalArgumentException().isThrownBy(() -> buffer.capacity(-1));
    }
    finally {
      release(buffer);
    }
  }

  @ParameterizedDataBufferAllocatingTest
  void writeByteBuffer(DataBufferFactory bufferFactory) {
    super.bufferFactory = bufferFactory;

    DataBuffer buffer1 = createDataBuffer(1);
    buffer1.write((byte) 'a');
    ByteBuffer buffer2 = createByteBuffer(2);
    buffer2.put((byte) 'b');
    buffer2.flip();
    ByteBuffer buffer3 = createByteBuffer(3);
    buffer3.put((byte) 'c');
    buffer3.flip();

    buffer1.write(buffer2, buffer3);
    buffer1.write((byte) 'd'); // make sure the write index is correctly set

    assertThat(buffer1.readableBytes()).isEqualTo(4);
    byte[] result = new byte[4];
    buffer1.read(result);

    assertThat(result).isEqualTo(new byte[] { 'a', 'b', 'c', 'd' });

    release(buffer1);
  }

  private ByteBuffer createByteBuffer(int capacity) {
    return ByteBuffer.allocate(capacity);
  }

  @ParameterizedDataBufferAllocatingTest
  void writeDataBuffer(DataBufferFactory bufferFactory) {
    super.bufferFactory = bufferFactory;

    DataBuffer buffer1 = createDataBuffer(1);
    buffer1.write((byte) 'a');
    DataBuffer buffer2 = createDataBuffer(2);
    buffer2.write((byte) 'b');
    DataBuffer buffer3 = createDataBuffer(3);
    buffer3.write((byte) 'c');

    buffer1.write(buffer2, buffer3);
    buffer1.write((byte) 'd'); // make sure the write index is correctly set

    assertThat(buffer1.readableBytes()).isEqualTo(4);
    byte[] result = new byte[4];
    buffer1.read(result);

    assertThat(result).isEqualTo(new byte[] { 'a', 'b', 'c', 'd' });

    release(buffer1, buffer2, buffer3);
  }

  @ParameterizedDataBufferAllocatingTest
  void asByteBuffer(DataBufferFactory bufferFactory) {
    super.bufferFactory = bufferFactory;

    DataBuffer buffer = createDataBuffer(4);
    buffer.write(new byte[] { 'a', 'b', 'c' });
    buffer.read(); // skip a

    ByteBuffer result = buffer.asByteBuffer();
    assertThat(result.capacity()).isEqualTo(2);

    buffer.write((byte) 'd');
    assertThat(result.remaining()).isEqualTo(2);

    byte[] resultBytes = new byte[2];
    result.get(resultBytes);
    assertThat(resultBytes).isEqualTo(new byte[] { 'b', 'c' });

    release(buffer);
  }

  @ParameterizedDataBufferAllocatingTest
  void asByteBufferIndexLength(DataBufferFactory bufferFactory) {
    super.bufferFactory = bufferFactory;

    DataBuffer buffer = createDataBuffer(3);
    buffer.write(new byte[] { 'a', 'b' });

    ByteBuffer result = buffer.asByteBuffer(1, 2);
    assertThat(result.capacity()).isEqualTo(2);

    buffer.write((byte) 'c');
    assertThat(result.remaining()).isEqualTo(2);

    byte[] resultBytes = new byte[2];
    result.get(resultBytes);
    assertThat(resultBytes).isEqualTo(new byte[] { 'b', 'c' });

    release(buffer);
  }

  @ParameterizedDataBufferAllocatingTest
  void byteBufferContainsDataBufferChanges(DataBufferFactory bufferFactory) {

    super.bufferFactory = bufferFactory;

    DataBuffer dataBuffer = createDataBuffer(1);
    ByteBuffer byteBuffer = dataBuffer.asByteBuffer(0, 1);

    dataBuffer.write((byte) 'a');

    assertThat(byteBuffer.limit()).isEqualTo(1);
    byte b = byteBuffer.get();
    assertThat(b).isEqualTo((byte) 'a');

    release(dataBuffer);
  }

  @ParameterizedDataBufferAllocatingTest
  void dataBufferContainsByteBufferChanges(DataBufferFactory bufferFactory) {

    super.bufferFactory = bufferFactory;

    DataBuffer dataBuffer = createDataBuffer(1);
    ByteBuffer byteBuffer = dataBuffer.asByteBuffer(0, 1);

    byteBuffer.put((byte) 'a');
    dataBuffer.writePosition(1);

    byte b = dataBuffer.read();
    assertThat(b).isEqualTo((byte) 'a');

    release(dataBuffer);
  }

  @ParameterizedDataBufferAllocatingTest
  void emptyAsByteBuffer(DataBufferFactory bufferFactory) {
    super.bufferFactory = bufferFactory;

    DataBuffer buffer = createDataBuffer(1);

    ByteBuffer result = buffer.asByteBuffer();
    assertThat(result.capacity()).isEqualTo(0);

    release(buffer);
  }

  @ParameterizedDataBufferAllocatingTest
  void toByteBuffer(DataBufferFactory bufferFactory) {
    super.bufferFactory = bufferFactory;

    DataBuffer buffer = createDataBuffer(4);
    buffer.write(new byte[] { 'a', 'b', 'c' });
    buffer.read(); // skip a

    ByteBuffer result = buffer.toByteBuffer();
    assertThat(result.capacity()).isEqualTo(2);
    assertThat(result.remaining()).isEqualTo(2);

    byte[] resultBytes = new byte[2];
    result.get(resultBytes);
    assertThat(resultBytes).isEqualTo(new byte[] { 'b', 'c' });

    release(buffer);
  }

  @ParameterizedDataBufferAllocatingTest
  void toByteBufferIndexLength(DataBufferFactory bufferFactory) {
    super.bufferFactory = bufferFactory;

    DataBuffer buffer = createDataBuffer(3);
    buffer.write(new byte[] { 'a', 'b', 'c' });

    ByteBuffer result = buffer.toByteBuffer(1, 2);
    assertThat(result.capacity()).isEqualTo(2);
    assertThat(result.remaining()).isEqualTo(2);

    byte[] resultBytes = new byte[2];
    result.get(resultBytes);
    assertThat(resultBytes).isEqualTo(new byte[] { 'b', 'c' });

    release(buffer);
  }

  @ParameterizedDataBufferAllocatingTest
  void toByteBufferDestination(DataBufferFactory bufferFactory) {
    super.bufferFactory = bufferFactory;

    DataBuffer buffer = createDataBuffer(4);
    buffer.write(new byte[] { 'a', 'b', 'c' });

    ByteBuffer byteBuffer = createByteBuffer(2);
    buffer.toByteBuffer(1, byteBuffer, 0, 2);
    assertThat(byteBuffer.capacity()).isEqualTo(2);
    assertThat(byteBuffer.remaining()).isEqualTo(2);

    byte[] resultBytes = new byte[2];
    byteBuffer.get(resultBytes);
    assertThat(resultBytes).isEqualTo(new byte[] { 'b', 'c' });

    assertThatExceptionOfType(IndexOutOfBoundsException.class)
            .isThrownBy(() -> buffer.toByteBuffer(0, byteBuffer, 0, 3));

    release(buffer);
  }

  @ParameterizedDataBufferAllocatingTest
  void readableByteBuffers(DataBufferFactory bufferFactory) throws IOException {
    super.bufferFactory = bufferFactory;

    DataBuffer dataBuffer = this.bufferFactory.join(Arrays.asList(stringBuffer("a"),
            stringBuffer("b"), stringBuffer("c")));

    byte[] result = new byte[3];
    try (var iterator = dataBuffer.readableByteBuffers()) {
      assertThat(iterator).hasNext();
      int i = 0;
      while (iterator.hasNext()) {
        ByteBuffer byteBuffer = iterator.next();
        int len = byteBuffer.remaining();
        byteBuffer.get(result, i, len);
        i += len;
        assertThatException().isThrownBy(() -> byteBuffer.put((byte) 'd'));
      }
    }

    assertThat(result).containsExactly('a', 'b', 'c');

    release(dataBuffer);
  }

  @ParameterizedDataBufferAllocatingTest
  void writableByteBuffers(DataBufferFactory bufferFactory) {
    super.bufferFactory = bufferFactory;

    DataBuffer dataBuffer = this.bufferFactory.allocateBuffer(1);

    try (DataBuffer.ByteBufferIterator iterator = dataBuffer.writableByteBuffers()) {
      assertThat(iterator).hasNext();
      ByteBuffer byteBuffer = iterator.next();
      byteBuffer.put((byte) 'a');
      dataBuffer.writePosition(1);

      assertThat(iterator).isExhausted();
    }
    assertThat(dataBuffer.read()).isEqualTo((byte) 'a');

    release(dataBuffer);
  }

  @ParameterizedDataBufferAllocatingTest
  void indexOf(DataBufferFactory bufferFactory) {
    super.bufferFactory = bufferFactory;

    DataBuffer buffer = createDataBuffer(3);
    buffer.write(new byte[] { 'a', 'b', 'c' });

    int result = buffer.indexOf(b -> b == 'c', 0);
    assertThat(result).isEqualTo(2);

    result = buffer.indexOf(b -> b == 'c', Integer.MIN_VALUE);
    assertThat(result).isEqualTo(2);

    result = buffer.indexOf(b -> b == 'c', Integer.MAX_VALUE);
    assertThat(result).isEqualTo(-1);

    result = buffer.indexOf(b -> b == 'z', 0);
    assertThat(result).isEqualTo(-1);

    release(buffer);
  }

  @ParameterizedDataBufferAllocatingTest
  void lastIndexOf(DataBufferFactory bufferFactory) {
    super.bufferFactory = bufferFactory;

    DataBuffer buffer = createDataBuffer(3);
    buffer.write(new byte[] { 'a', 'b', 'c' });

    int result = buffer.lastIndexOf(b -> b == 'b', 2);
    assertThat(result).isEqualTo(1);

    result = buffer.lastIndexOf(b -> b == 'c', 2);
    assertThat(result).isEqualTo(2);

    result = buffer.lastIndexOf(b -> b == 'b', Integer.MAX_VALUE);
    assertThat(result).isEqualTo(1);

    result = buffer.lastIndexOf(b -> b == 'c', Integer.MAX_VALUE);
    assertThat(result).isEqualTo(2);

    result = buffer.lastIndexOf(b -> b == 'b', Integer.MIN_VALUE);
    assertThat(result).isEqualTo(-1);

    result = buffer.lastIndexOf(b -> b == 'c', Integer.MIN_VALUE);
    assertThat(result).isEqualTo(-1);

    result = buffer.lastIndexOf(b -> b == 'z', 0);
    assertThat(result).isEqualTo(-1);

    release(buffer);
  }

  @ParameterizedDataBufferAllocatingTest
  void slice(DataBufferFactory bufferFactory) {
    super.bufferFactory = bufferFactory;

    DataBuffer buffer = createDataBuffer(3);
    buffer.write(new byte[] { 'a', 'b' });

    DataBuffer slice = buffer.slice(1, 2);
    assertThat(slice.readableBytes()).isEqualTo(2);
    buffer.write((byte) 'c');

    assertThat(buffer.readableBytes()).isEqualTo(3);
    byte[] result = new byte[3];
    buffer.read(result);

    assertThat(result).isEqualTo(new byte[] { 'a', 'b', 'c' });

    assertThat(slice.readableBytes()).isEqualTo(2);
    result = new byte[2];
    slice.read(result);

    assertThat(result).isEqualTo(new byte[] { 'b', 'c' });
    release(buffer);
  }

  @ParameterizedDataBufferAllocatingTest
  void retainedSlice(DataBufferFactory bufferFactory) {

    super.bufferFactory = bufferFactory;

    DataBuffer buffer = createDataBuffer(3);
    buffer.write(new byte[] { 'a', 'b' });

    DataBuffer slice = buffer.retainedSlice(1, 2);
    assertThat(slice.readableBytes()).isEqualTo(2);
    buffer.write((byte) 'c');

    assertThat(buffer.readableBytes()).isEqualTo(3);
    byte[] result = new byte[3];
    buffer.read(result);

    assertThat(result).isEqualTo(new byte[] { 'a', 'b', 'c' });

    assertThat(slice.readableBytes()).isEqualTo(2);
    result = new byte[2];
    slice.read(result);

    assertThat(result).isEqualTo(new byte[] { 'b', 'c' });

    release(buffer, slice);
  }

  @ParameterizedDataBufferAllocatingTest
  void spr16351(DataBufferFactory bufferFactory) {
    super.bufferFactory = bufferFactory;

    DataBuffer buffer = createDataBuffer(6);
    byte[] bytes = { 'a', 'b', 'c', 'd', 'e', 'f' };
    buffer.write(bytes);
    DataBuffer slice = buffer.slice(3, 3);
    buffer.writePosition(3);
    buffer.write(slice);

    assertThat(buffer.readableBytes()).isEqualTo(6);
    byte[] result = new byte[6];
    buffer.read(result);

    assertThat(result).isEqualTo(bytes);

    release(buffer);
  }

  @ParameterizedDataBufferAllocatingTest
  void split(DataBufferFactory bufferFactory) {
    super.bufferFactory = bufferFactory;

    DataBuffer buffer = createDataBuffer(3);
    buffer.write(new byte[] { 'a', 'b' });

    assertThatException().isThrownBy(() -> buffer.split(-1));
    assertThatException().isThrownBy(() -> buffer.split(4));

    DataBuffer split = buffer.split(1);

    assertThat(split.readPosition()).isEqualTo(0);
    assertThat(split.writePosition()).isEqualTo(1);
    assertThat(split.capacity()).isEqualTo(1);
    assertThat(split.readableBytes()).isEqualTo(1);
    byte[] bytes = new byte[1];
    split.read(bytes);
    assertThat(bytes).containsExactly('a');

    assertThat(buffer.readPosition()).isEqualTo(0);
    assertThat(buffer.writePosition()).isEqualTo(1);
    assertThat(buffer.capacity()).isEqualTo(2);

    buffer.write((byte) 'c');
    assertThat(buffer.readableBytes()).isEqualTo(2);
    bytes = new byte[2];
    buffer.read(bytes);

    assertThat(bytes).isEqualTo(new byte[] { 'b', 'c' });

    DataBuffer buffer2 = createDataBuffer(1);
    buffer2.write(new byte[] { 'a' });
    DataBuffer split2 = buffer2.split(1);

    assertThat(split2.readPosition()).isEqualTo(0);
    assertThat(split2.writePosition()).isEqualTo(1);
    assertThat(split2.capacity()).isEqualTo(1);
    assertThat(split2.readableBytes()).isEqualTo(1);
    bytes = new byte[1];
    split2.read(bytes);
    assertThat(bytes).containsExactly('a');

    assertThat(buffer2.readPosition()).isEqualTo(0);
    assertThat(buffer2.writePosition()).isEqualTo(0);
    assertThat(buffer2.capacity()).isEqualTo(0);
    assertThat(buffer.readableBytes()).isEqualTo(0);

    release(buffer, buffer2, split, split2);
  }

  @ParameterizedDataBufferAllocatingTest
  void join(DataBufferFactory bufferFactory) {
    super.bufferFactory = bufferFactory;

    DataBuffer composite = this.bufferFactory.join(Arrays.asList(stringBuffer("a"),
            stringBuffer("b"), stringBuffer("c")));
    assertThat(composite.readableBytes()).isEqualTo(3);
    byte[] bytes = new byte[3];
    composite.read(bytes);

    assertThat(bytes).isEqualTo(new byte[] { 'a', 'b', 'c' });

    release(composite);
  }

  @ParameterizedDataBufferAllocatingTest
  void getByte(DataBufferFactory bufferFactory) {
    super.bufferFactory = bufferFactory;

    DataBuffer buffer = stringBuffer("abc");

    assertThat(buffer.getByte(0)).isEqualTo((byte) 'a');
    assertThat(buffer.getByte(1)).isEqualTo((byte) 'b');
    assertThat(buffer.getByte(2)).isEqualTo((byte) 'c');
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> buffer.getByte(-1));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> buffer.getByte(3));

    release(buffer);
  }

  @ParameterizedDataBufferAllocatingTest
  void shouldHonorSourceBuffersReadPosition(DataBufferFactory bufferFactory) {
    DataBuffer dataBuffer = bufferFactory.wrap("ab".getBytes(StandardCharsets.UTF_8));
    dataBuffer.readPosition(1);

    ByteBuffer byteBuffer = ByteBuffer.allocate(dataBuffer.readableBytes());
    dataBuffer.toByteBuffer(byteBuffer);

    assertThat(StandardCharsets.UTF_8.decode(byteBuffer).toString()).isEqualTo("b");
  }

  @ParameterizedDataBufferAllocatingTest
  void readBytes(DataBufferFactory factory) {
    assertThat(factory.copiedBuffer("hello").readBytes()).isEqualTo("hello".getBytes(StandardCharsets.UTF_8));
    assertThat(factory.copiedBuffer("hello", StandardCharsets.UTF_8).readBytes()).isEqualTo("hello".getBytes(StandardCharsets.UTF_8));
  }

  @ParameterizedDataBufferAllocatingTest
  void duplicate(DataBufferFactory factory) {
    DataBuffer hello = factory.copiedBuffer("hello");
    DataBuffer duplicate = hello.duplicate();

    assertThat(hello.toString(StandardCharsets.UTF_8)).isEqualTo("hello");
    assertThat(duplicate.toString(StandardCharsets.UTF_8)).isEqualTo("hello");
    release(hello, duplicate);
  }

  @ParameterizedDataBufferAllocatingTest
  void retainedDuplicate(DataBufferFactory factory) {
    this.bufferFactory = factory;

    DataBuffer hello = factory.copiedBuffer("hello");
    DataBuffer duplicate = hello.retainedDuplicate();

    assertThat(hello.toString(StandardCharsets.UTF_8)).isEqualTo("hello");
    assertThat(duplicate.toString(StandardCharsets.UTF_8)).isEqualTo("hello");

    DataBuffer buffer = factory.allocateBuffer(3);
    buffer.write(new byte[] { 'a', 'b' });

    DataBuffer retainedDuplicate = buffer.retainedDuplicate();
    assertThat(retainedDuplicate.readableBytes()).isEqualTo(2);
    buffer.write((byte) 'c');

    assertThat(buffer.readableBytes()).isEqualTo(3);
    assertThat(retainedDuplicate.readableBytes()).isEqualTo(2);

    byte[] result = new byte[3];
    buffer.read(result);

    assertThat(result).isEqualTo(new byte[] { 'a', 'b', 'c' });
    assertThat(retainedDuplicate.readableBytes()).isEqualTo(2);

    result = new byte[2];
    retainedDuplicate.read(result);

    assertThat(result).isEqualTo(new byte[] { 'a', 'b' });

    release(buffer, retainedDuplicate);
    release(hello, duplicate);
  }

  @ParameterizedDataBufferAllocatingTest
  void forEachByteProcessAll(DataBufferFactory bufferFactory) {
    super.bufferFactory = bufferFactory;

    List<Byte> result = new ArrayList<>();
    DataBuffer buffer = byteBuffer(new byte[] { 'a', 'b', 'c', 'd' });
    int index = buffer.forEach(0, 4, b -> {
      result.add(b);
      return true;
    });
    assertThat(index).isEqualTo(-1);
    assertThat(result).containsExactly((byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd');
    release(buffer);
  }

  @ParameterizedDataBufferAllocatingTest
  void forEachByteProcessSome(DataBufferFactory bufferFactory) {
    super.bufferFactory = bufferFactory;

    List<Byte> result = new ArrayList<>();
    DataBuffer buffer = byteBuffer(new byte[] { 'a', 'b', 'c', 'd' });
    int index = buffer.forEach(0, 4, b -> {
      result.add(b);
      return (b != 'c');
    });
    assertThat(index).isEqualTo(2);
    assertThat(result).containsExactly((byte) 'a', (byte) 'b', (byte) 'c');
    release(buffer);
  }

}
