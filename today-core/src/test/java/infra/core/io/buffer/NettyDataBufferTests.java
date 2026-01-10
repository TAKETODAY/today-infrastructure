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

package infra.core.io.buffer;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

import infra.core.io.buffer.DataBuffer.ByteBufferIterator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/22 16:54
 */
class NettyDataBufferTests {

  @Test
  void newCapacityAdjustsUnderlyingBuffer() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    buffer.write(new byte[] { 1, 2, 3 });
    buffer.capacity(10);

    assertThat(buffer.capacity()).isEqualTo(10);
    assertThat(buffer.readableBytes()).isEqualTo(3);
  }

  @Test
  void ensureWritableExpandsCapacity() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    buffer.ensureWritable(10);

    assertThat(buffer.writableBytes()).isGreaterThanOrEqualTo(10);
  }

  @Test
  void readWriteOperations() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    buffer.write((byte) 1);
    buffer.write(new byte[] { 2, 3, 4 });

    assertThat(buffer.readableBytes()).isEqualTo(4);
    assertThat(buffer.read()).isEqualTo((byte) 1);

    byte[] bytes = new byte[3];
    buffer.read(bytes);
    assertThat(bytes).containsExactly(2, 3, 4);
  }

  @Test
  void sliceCreatesIndependentView() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    buffer.write(new byte[] { 1, 2, 3, 4 });

    NettyDataBuffer slice = buffer.slice(1, 2);
    assertThat(slice.readableBytes()).isEqualTo(2);
    assertThat(slice.read()).isEqualTo((byte) 2);
    assertThat(slice.read()).isEqualTo((byte) 3);
  }

  @Test
  void splitDividesBuffer() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    buffer.write(new byte[] { 1, 2, 3, 4 });

    NettyDataBuffer split = buffer.split(2);
    assertThat(split.readableBytes()).isEqualTo(2);
    assertThat(buffer.readableBytes()).isEqualTo(2);

    assertThat(split.read()).isEqualTo((byte) 1);
    assertThat(split.read()).isEqualTo((byte) 2);
    assertThat(buffer.read()).isEqualTo((byte) 3);
    assertThat(buffer.read()).isEqualTo((byte) 4);
  }

  @Test
  void retainAndReleaseManageResources() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    buffer.retain();
    assertThat(buffer.isAllocated()).isTrue();

    buffer.release();
    assertThat(buffer.isAllocated()).isTrue();

    buffer.release();
    assertThat(buffer.isAllocated()).isFalse();
  }

  @Test
  void byteBufferOperations() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    buffer.write(new byte[] { 1, 2, 3, 4 });

    ByteBuffer byteBuffer = buffer.asByteBuffer();
    assertThat(byteBuffer.remaining()).isEqualTo(4);
    assertThat(byteBuffer.get()).isEqualTo((byte) 1);

    ByteBuffer slice = buffer.asByteBuffer(1, 2);
    assertThat(slice.remaining()).isEqualTo(2);
    assertThat(slice.get()).isEqualTo((byte) 2);
  }

  @Test
  void iteratorProvidesAccessToByteBuffers() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    buffer.write(new byte[] { 1, 2, 3, 4 });

    ByteBufferIterator iterator = buffer.readableByteBuffers();
    assertThat(iterator.hasNext()).isTrue();

    ByteBuffer first = iterator.next();
    assertThat(first.remaining()).isGreaterThan(0);
  }

  @Test
  void duplicateCreatesIndependentCopy() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    buffer.write(new byte[] { 1, 2, 3, 4 });

    NettyDataBuffer duplicate = buffer.duplicate();
    duplicate.write((byte) 5);

    assertThat(duplicate.readableBytes()).isEqualTo(5);
    assertThat(buffer.readableBytes()).isEqualTo(4);
  }

  @Test
  void writeStringEncodesWithCharset() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    String test = "Test";

    buffer.write(test, StandardCharsets.UTF_8);
    assertThat(buffer.toString(StandardCharsets.UTF_8)).isEqualTo(test);
  }

  @Test
  void writeEmptyByteBufferDoesNothing() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    buffer.write((ByteBuffer) null);
    assertThat(buffer.readableBytes()).isEqualTo(0);
  }

  @Test
  void writeEmptyDataBufferDoesNothing() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    buffer.write((DataBuffer) null);
    assertThat(buffer.readableBytes()).isEqualTo(0);
  }

  @Test
  void touchOperationReturnsBuffer() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    NettyDataBuffer touched = buffer.touch("test hint");
    assertThat(touched).isSameAs(buffer);
  }

  @Test
  void readOutOfBoundsThrowsException() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    buffer.write(new byte[] { 1 });
    buffer.read();
    assertThatThrownBy(buffer::read)
            .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void writeUtf8StringOptimizesEncoding() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    String test = "Hello 世界";
    buffer.write(test, StandardCharsets.UTF_8);
    assertThat(buffer.toString(StandardCharsets.UTF_8)).isEqualTo(test);
  }

  @Test
  void writeAsciiStringOptimizesEncoding() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    String test = "Hello ASCII";
    buffer.write(test, StandardCharsets.US_ASCII);
    assertThat(buffer.toString(StandardCharsets.US_ASCII)).isEqualTo(test);
  }

  @Test
  void retainedDuplicateCreatesIndependentCopy() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    buffer.write(new byte[] { 1, 2, 3 });

    DataBuffer duplicate = buffer.retainedDuplicate();
    duplicate.write((byte) 4);

    assertThat(duplicate.readableBytes()).isEqualTo(4);
    assertThat(buffer.readableBytes()).isEqualTo(3);
  }

  @Test
  void toByteBufferCopiesDataForDirectBuffer() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    byte[] data = { 1, 2, 3 };
    buffer.write(data);

    ByteBuffer result = buffer.toByteBuffer(0, 3);
    assertThat(result.remaining()).isEqualTo(3);
    assertThat(result.get(0)).isEqualTo((byte) 1);
  }

  @Test
  void writableByteBuffersProvideIterator() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    buffer.capacity(10);

    ByteBufferIterator iterator = buffer.writableByteBuffers();
    assertThat(iterator.hasNext()).isTrue();

    ByteBuffer writable = iterator.next();
    assertThat(writable.remaining()).isGreaterThan(0);
    assertThat(writable.isReadOnly()).isFalse();
  }

  @Test
  void readableByteBufferIteratorProvidesReadOnlyBuffers() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    buffer.write(new byte[] { 1, 2, 3 });

    ByteBufferIterator iterator = buffer.readableByteBuffers();
    ByteBuffer readable = iterator.next();
    assertThat(readable.isReadOnly()).isTrue();
  }

  @Test
  void releaseWithIllegalStateReturnsFailure() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    buffer.release();
    assertThat(buffer.release()).isFalse();
  }

  @Test
  void indexOfFindsFirstMatchingByte() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    buffer.write(new byte[] { 1, 2, 3, 2, 1 });

    int index = buffer.indexOf(b -> b == 2, 0);
    assertThat(index).isEqualTo(1);
  }

  @Test
  void lastIndexOfFindsLastMatchingByte() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    buffer.write(new byte[] { 1, 2, 3, 2, 1 });

    int index = buffer.lastIndexOf(b -> b == 2, 4);
    assertThat(index).isEqualTo(3);
  }

  @Test
  void readEmptyBufferThrowsException() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    assertThatThrownBy(buffer::read)
            .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void negativeCapacityThrowsException() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    assertThatThrownBy(() -> buffer.capacity(-1))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void writeStringWithNullCharsetThrowsException() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    assertThatThrownBy(() -> buffer.write("test", null))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void readNegativePositionThrowsException() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    assertThatThrownBy(() -> buffer.readPosition(-1))
            .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void writeOverCapacityAutoExpands() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    int initialCapacity = buffer.capacity();
    byte[] largeData = new byte[initialCapacity * 2];

    buffer.write(largeData);
    assertThat(buffer.capacity()).isGreaterThan(initialCapacity);
  }

  @Test
  void sliceWithInvalidIndexThrowsException() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    assertThatThrownBy(() -> buffer.slice(-1, 1))
            .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void toStringWithInvalidIndexThrowsException() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    assertThatThrownBy(() -> buffer.toString(-1, 1, StandardCharsets.UTF_8))
            .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void splitWithZeroIndexReturnsEmptyBuffer() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    buffer.write(new byte[] { 1, 2, 3 });

    NettyDataBuffer split = buffer.split(0);
    assertThat(split.readableBytes()).isZero();
    assertThat(buffer.readableBytes()).isEqualTo(3);
  }

  @Test
  void getByteOutOfBoundsThrowsException() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    buffer.capacity(1);
    buffer.write(new byte[] { 1 });
    assertThatThrownBy(() -> buffer.getByte(2))
            .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void writePositionExceedsCapacityThrowsException() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    int initialCapacity = buffer.capacity();
    assertThatThrownBy(() -> buffer.writePosition(initialCapacity + 10))
            .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void writePositionSetsCorrectIndex() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    buffer.write(new byte[] { 1, 2, 3 });
    buffer.writePosition(1);
    assertThat(buffer.writePosition()).isEqualTo(1);
  }

  @Test
  void ensureWritableWithZeroDoesNotModifyCapacity() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    int initialCapacity = buffer.capacity();
    buffer.ensureWritable(0);
    assertThat(buffer.capacity()).isEqualTo(initialCapacity);
  }

  @Test
  void indexOfWithNegativeFromIndexStartsFromZero() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    buffer.write(new byte[] { 1, 2, 3 });
    int index = buffer.indexOf(b -> b == 2, -1);
    assertThat(index).isEqualTo(1);
  }

  @Test
  void writeByteBufferWithExactCapacitySucceeds() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    int initialCapacity = buffer.capacity();
    ByteBuffer source = ByteBuffer.wrap(new byte[initialCapacity]);
    buffer.write(source);
    assertThat(buffer.readableBytes()).isEqualTo(initialCapacity);
  }

  @Test
  void duplicateWithEmptyBufferHasZeroReadableBytes() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    NettyDataBuffer duplicate = buffer.duplicate();
    assertThat(duplicate.readableBytes()).isZero();
  }

  @Test
  void retainedSliceWithFullLengthCreatesCopy() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    buffer.write(new byte[] { 1, 2, 3 });
    NettyDataBuffer slice = buffer.retainedSlice(0, 3);
    assertThat(slice.readableBytes()).isEqualTo(3);
  }

  @Test
  void splitAtEndIndexCreatesEmptySecondBuffer() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    buffer.write(new byte[] { 1, 2, 3 });
    NettyDataBuffer split = buffer.split(3);
    assertThat(split.readableBytes()).isEqualTo(3);
    assertThat(buffer.readableBytes()).isZero();
  }

  @Test
  void writeToByteBufferWithInvalidPositionThrowsException() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    ByteBuffer dest = ByteBuffer.allocate(10);
    assertThatThrownBy(() -> buffer.toByteBuffer(-1, dest, 0, 1))
            .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void writeToByteBufferWithInvalidDestPositionThrowsException() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    buffer.write(new byte[] { 1 });
    ByteBuffer dest = ByteBuffer.allocate(1);
    assertThatThrownBy(() -> buffer.toByteBuffer(0, dest, 1, 1))
            .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void multipleRetainIncreasesReferenceCount() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    buffer.retain().retain();
    buffer.release();
    assertThat(buffer.isAllocated()).isTrue();
  }

  @Test
  void writableByteBuffersWithNoSpaceReturnsEmptyIterator() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    buffer.write(new byte[buffer.capacity()]);
    ByteBufferIterator iterator = buffer.writableByteBuffers();
    iterator.next();
    assertThat(iterator.hasNext()).isFalse();
  }

  @Test
  void readableByteBuffersWithNoDataReturnsEmptyIterator() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    buffer.capacity(0);
    ByteBufferIterator iterator = buffer.readableByteBuffers();
    iterator.next();
    assertThat(iterator.hasNext()).isFalse();
  }

  @Test
  void byteBufferIteratorNextWithoutHasNextThrowsException() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    ByteBufferIterator iterator = buffer.readableByteBuffers();
    iterator.next();
    assertThatThrownBy(iterator::next)
            .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  void equalityWithDifferentBufferContents() {
    NettyDataBuffer buffer1 = createNettyDataBuffer();
    NettyDataBuffer buffer2 = createNettyDataBuffer();
    buffer1.write((byte) 1);
    buffer2.write((byte) 2);
    assertThat(buffer1).isNotEqualTo(buffer2);
  }

  @Test
  void hashCodeGeneratesSameValueForEqualBuffers() {
    NettyDataBuffer buffer1 = createNettyDataBuffer();
    NettyDataBuffer buffer2 = createNettyDataBuffer();
    buffer1.write((byte) 1);
    buffer2.write((byte) 1);
    assertThat(buffer1.hashCode()).isEqualTo(buffer2.hashCode());
  }

  @Test
  void toStringReturnsReadableRepresentation() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    buffer.write(new byte[] { 1, 2, 3 });
    assertThat(buffer.toString()).contains("ridx:", "widx:", "cap:");
  }

  @Test
  void writeEmptySequenceDoesNotModifyBuffer() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    buffer.write("", StandardCharsets.UTF_8);
    assertThat(buffer.readableBytes()).isZero();
  }

  @Test
  void nettyDataBufferReturnsSameNativeBuffer() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    buffer.write(new byte[] { 1, 2, 3 });
    ByteBuf result = NettyDataBuffer.toByteBuf(buffer);
    assertThat(result).isSameAs(buffer.getNativeBuffer());
  }

  @Test
  void emptyDataBufferReturnsEmptyBuffer() {
    DataBuffer buffer = new DefaultDataBuffer();
    ByteBuf result = NettyDataBuffer.toByteBuf(buffer);
    assertThat(result).isSameAs(Unpooled.EMPTY_BUFFER);
  }

  @Test
  void nonNettyDataBufferConvertsToWrappedByteBuf() {
    DataBuffer buffer = new DefaultDataBuffer();
    buffer.write(new byte[] { 1, 2, 3 });
    ByteBuf result = NettyDataBuffer.toByteBuf(buffer);
    assertThat(result.readableBytes()).isEqualTo(3);
    assertThat(result.getByte(0)).isEqualTo((byte) 1);
  }

  @Test
  void nullDataBufferThrowsException() {
    assertThatThrownBy(() -> NettyDataBuffer.toByteBuf(null))
            .isInstanceOf(NullPointerException.class);
  }

  @Test
  void writeToNullBufferThrowsException() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    assertThatThrownBy(() -> buffer.toByteBuffer(0, null, 0, 1))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void readFromNegativeIndexThrowsException() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    buffer.write(new byte[] { 1 });
    assertThatThrownBy(() -> buffer.read(new byte[1], -1, 1))
            .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void writeToInvalidDestinationLengthThrowsException() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    buffer.write(new byte[] { 1 });
    assertThatThrownBy(() -> buffer.toByteBuffer(0, ByteBuffer.allocate(1), 0, 2))
            .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void writeEmptyIteratorClosesWithoutError() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    ByteBufferIterator iterator = buffer.writableByteBuffers();
    iterator.close();
    assertThat(iterator.hasNext()).isTrue();
  }

  @Test
  void lastIndexOfWithEmptyBufferReturnsNegative() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    assertThat(buffer.lastIndexOf(b -> true, 0)).isEqualTo(-1);
  }

  @Test
  void indexOfWithFromIndexAtEndReturnsNegative() {
    NettyDataBuffer buffer = createNettyDataBuffer();
    buffer.write(new byte[] { 1, 2, 3 });
    assertThat(buffer.indexOf(b -> true, 3)).isEqualTo(-1);
  }

  private NettyDataBuffer createNettyDataBuffer() {
    return new NettyDataBuffer(
            Unpooled.buffer(),
            new NettyDataBufferFactory(ByteBufAllocator.DEFAULT)
    );
  }

}