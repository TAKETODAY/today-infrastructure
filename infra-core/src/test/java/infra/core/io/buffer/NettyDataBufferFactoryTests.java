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
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/22 17:14
 */
class NettyDataBufferFactoryTests {

  @Test
  void allocateBufferCreatesEmptyBuffer() {
    var factory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
    var buffer = factory.allocateBuffer();
    assertThat(buffer.readableBytes()).isZero();
  }

  @Test
  void allocateBufferWithCapacityCreatesBufferWithSize() {
    var factory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
    var buffer = factory.allocateBuffer(10);
    assertThat(buffer.capacity()).isEqualTo(10);
  }

  @Test
  void wrapByteBufferCreatesBufferWithContent() {
    var factory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
    var wrapped = factory.wrap(ByteBuffer.wrap(new byte[] { 1, 2, 3 }));
    assertThat(wrapped.readableBytes()).isEqualTo(3);
  }

  @Test
  void wrapByteBufRetainsBuffer() {
    var factory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
    var byteBuf = Unpooled.buffer();
    var wrapped = factory.wrap(byteBuf);
    assertThat(byteBuf.refCnt()).isEqualTo(1);
  }

  @Test
  void joinSingleBufferReturnsSameBuffer() {
    var factory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
    var buffer = factory.allocateBuffer();
    var joined = factory.join(List.of(buffer));
    assertThat(joined).isSameAs(buffer);
  }

  @Test
  void joinMultipleBuffersCombinesContent() {
    var factory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
    var buffer1 = factory.wrap(new byte[] { 1 });
    var buffer2 = factory.wrap(new byte[] { 2 });
    var joined = factory.join(buffer1, buffer2);
    assertThat(joined.readableBytes()).isEqualTo(2);
  }

  @Test
  void joinEmptyListThrowsException() {
    var factory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
    assertThatThrownBy(() -> factory.join(List.of()))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void joinEmptyArrayThrowsException() {
    var factory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
    assertThatThrownBy(() -> factory.join(new DataBuffer[0]))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void joinNonNettyBufferThrowsException() {
    var factory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
    var buffer = new DefaultDataBuffer();

    assertThat(factory.join(List.of(buffer))).isSameAs(buffer);

    assertThatThrownBy(() -> factory.join(List.of(buffer, buffer)))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void copyCharSequenceCreatesBufferWithContent() {
    var factory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
    var buffer = factory.copiedBuffer("test", StandardCharsets.UTF_8);
    assertThat(buffer.toString(StandardCharsets.UTF_8)).isEqualTo("test");
  }

  @Test
  void isDirectMatchesAllocatorSetting() {
    var directAllocator = ByteBufAllocator.DEFAULT;
    var factory = new NettyDataBufferFactory(directAllocator);
    assertThat(factory.isDirect()).isEqualTo(directAllocator.isDirectBufferPooled());
  }

  @Test
  void toStringContainsAllocatorInfo() {
    var factory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
    assertThat(factory.toString()).contains("NettyDataBufferFactory");
  }

  @Test
  void wrapByteArrayWithOffsetAndLengthCreatesBuffer() {
    var factory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
    var buffer = factory.wrap(new byte[] { 1, 2, 3, 4 }, 1, 2);
    assertThat(buffer.readableBytes()).isEqualTo(2);
    assertThat(buffer.getByte(0)).isEqualTo((byte) 2);
  }

  @Test
  void wrapByteArrayCreatesBuffer() {
    var factory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
    var buffer = factory.wrap(new byte[] { 1, 2, 3 });
    assertThat(buffer.readableBytes()).isEqualTo(3);
    assertThat(buffer.getByte(0)).isEqualTo((byte) 1);
  }

  @Test
  void nullAllocatorThrowsException() {
    assertThatThrownBy(() -> new NettyDataBufferFactory(null))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void getByteBufAllocatorReturnsSameAllocator() {
    var allocator = ByteBufAllocator.DEFAULT;
    var factory = new NettyDataBufferFactory(allocator);
    assertThat(factory.getByteBufAllocator()).isSameAs(allocator);
  }

  @Test
  void joinDifferentTypesOfBuffersThrowsException() {
    var factory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
    var nettyBuffer = factory.allocateBuffer();
    var defaultBuffer = new DefaultDataBuffer();
    assertThatThrownBy(() -> factory.join(nettyBuffer, defaultBuffer))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void joinWithNullBufferThrowsException() {
    var factory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
    var buffer = factory.allocateBuffer();
    assertThatThrownBy(() -> factory.join(buffer, null))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void copiedBufferWithEmptyStringCreatesEmptyBuffer() {
    var factory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
    var buffer = factory.copiedBuffer("", StandardCharsets.UTF_8);
    assertThat(buffer.readableBytes()).isZero();
  }

  @Test
  void wrapNullByteArrayThrowsException() {
    var factory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
    assertThatThrownBy(() -> factory.wrap((byte[]) null))
            .isInstanceOf(NullPointerException.class);
  }

  @Test
  void wrapNullByteBufferThrowsException() {
    var factory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
    assertThatThrownBy(() -> factory.wrap((ByteBuffer) null))
            .isInstanceOf(NullPointerException.class);
  }

  @Test
  void wrapNullByteBufThrowsException() {
    var factory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
    assertThatThrownBy(() -> factory.wrap((ByteBuf) null))
            .isInstanceOf(NullPointerException.class);
  }

  @Test
  void wrapByteArrayWithInvalidOffsetThrowsException() {
    var factory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
    assertThatThrownBy(() -> factory.wrap(new byte[] { 1 }, -1, 1))
            .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void wrapByteArrayWithInvalidLengthThrowsException() {
    var factory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
    assertThatThrownBy(() -> factory.wrap(new byte[] { 1 }, 0, 2))
            .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void copiedBufferWithNullCharsetThrowsException() {
    var factory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
    assertThatThrownBy(() -> factory.copiedBuffer("test", null))
            .isInstanceOf(NullPointerException.class);
  }

  @Test
  void copiedBufferWithNullStringThrowsException() {
    var factory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
    assertThatThrownBy(() -> factory.copiedBuffer(null, StandardCharsets.UTF_8))
            .isInstanceOf(NullPointerException.class);
  }

  @Test
  void allocateBufferWithNegativeCapacityThrowsException() {
    var factory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
    assertThatThrownBy(() -> factory.allocateBuffer(-1))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void joinEmptyBuffersResultsInEmptyBuffer() {
    var factory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
    var buffer1 = factory.allocateBuffer();
    var buffer2 = factory.allocateBuffer();
    var joined = factory.join(List.of(buffer1, buffer2));
    assertThat(joined.readableBytes()).isZero();
  }

  @Test
  void joinRetainsInputBuffers() {
    var factory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
    var buffer1 = factory.allocateBuffer();
    var buffer2 = factory.allocateBuffer();
    factory.join(List.of(buffer1, buffer2));
    assertThat(buffer1.isAllocated()).isTrue();
    assertThat(buffer2.isAllocated()).isTrue();
  }

  @Test
  void joinedBufferContainsAllContent() {
    var factory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
    var buffer1 = factory.wrap(new byte[] { 1 });
    var buffer2 = factory.wrap(new byte[] { 2 });
    var buffer3 = factory.wrap(new byte[] { 3 });
    var joined = factory.join(List.of(buffer1, buffer2, buffer3));
    assertThat(joined.readableBytes()).isEqualTo(3);
    assertThat(joined.getByte(0)).isEqualTo((byte) 1);
    assertThat(joined.getByte(1)).isEqualTo((byte) 2);
    assertThat(joined.getByte(2)).isEqualTo((byte) 3);
  }

}