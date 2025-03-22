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

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.List;

import infra.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/22 17:22
 */
class DefaultDataBufferFactoryTests {

  @Test
  void defaultConstructorCreatesNonDirectBufferFactory() {
    var factory = new DefaultDataBufferFactory();
    assertThat(factory.isDirect()).isFalse();
  }

  @Test
  void preferDirectConstructorCreatesDirectBufferFactory() {
    var factory = new DefaultDataBufferFactory(true);
    assertThat(factory.isDirect()).isTrue();
  }

  @Test
  void negativeInitialCapacityThrowsException() {
    assertThatThrownBy(() -> new DefaultDataBufferFactory(false, -1))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void zeroInitialCapacityThrowsException() {
    assertThatThrownBy(() -> new DefaultDataBufferFactory(false, 0))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void allocatedBufferHasDefaultCapacity() {
    var factory = new DefaultDataBufferFactory();
    var buffer = factory.allocateBuffer();
    assertThat(buffer.capacity()).isEqualTo(DefaultDataBufferFactory.DEFAULT_INITIAL_CAPACITY);
  }

  @Test
  void allocatedBufferWithCustomCapacityHasSpecifiedSize() {
    var factory = new DefaultDataBufferFactory();
    var buffer = factory.allocateBuffer(10);
    assertThat(buffer.capacity()).isEqualTo(10);
  }

  @Test
  void wrappedByteBufferMaintainsContent() {
    var factory = new DefaultDataBufferFactory();
    var original = ByteBuffer.wrap(new byte[] { 1, 2, 3 });
    var wrapped = factory.wrap(original);
    assertThat(wrapped.readableBytes()).isEqualTo(3);
    assertThat(wrapped.getByte(0)).isEqualTo((byte) 1);
  }

  @Test
  void wrappedByteArrayMaintainsContent() {
    var factory = new DefaultDataBufferFactory();
    var wrapped = factory.wrap(new byte[] { 1, 2, 3 }, 1, 2);
    assertThat(wrapped.readableBytes()).isEqualTo(2);
    assertThat(wrapped.getByte(0)).isEqualTo((byte) 2);
  }

  @Test
  void joinedBuffersContainCombinedContent() {
    var factory = new DefaultDataBufferFactory();
    var buffer1 = factory.wrap(new byte[] { 1 });
    var buffer2 = factory.wrap(new byte[] { 2 });
    var joined = factory.join(buffer1, buffer2);
    assertThat(joined.readableBytes()).isEqualTo(2);
    assertThat(joined.getByte(0)).isEqualTo((byte) 1);
    assertThat(joined.getByte(1)).isEqualTo((byte) 2);
  }

  @Test
  void sharedInstanceIsNonDirect() {
    assertThat(DefaultDataBufferFactory.sharedInstance.isDirect()).isFalse();
  }

  @Test
  void toStringContainsPreferDirectSetting() {
    var factory = new DefaultDataBufferFactory(true);
    assertThat(factory.toString()).contains("preferDirect=true");
  }

  @Test
  void wrapNullByteBufferThrowsException() {
    var factory = new DefaultDataBufferFactory();
    assertThatThrownBy(() -> factory.wrap((ByteBuffer) null))
            .isInstanceOf(NullPointerException.class);
  }

  @Test
  void wrapByteArrayWithInvalidOffsetThrowsException() {
    var factory = new DefaultDataBufferFactory();
    assertThatThrownBy(() -> factory.wrap(new byte[] { 1 }, -1, 1))
            .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void wrapByteArrayWithInvalidLengthThrowsException() {
    var factory = new DefaultDataBufferFactory();
    assertThatThrownBy(() -> factory.wrap(new byte[] { 1 }, 0, 2))
            .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void joinEmptyBufferListThrowsException() {
    var factory = new DefaultDataBufferFactory();
    assertThatThrownBy(() -> factory.join(List.of()))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void joinEmptyBufferArrayThrowsException() {
    var factory = new DefaultDataBufferFactory();
    assertThatThrownBy(() -> factory.join(new DataBuffer[0]))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void joinSingleBufferReturnsSameContent() {
    var factory = new DefaultDataBufferFactory();
    var buffer = factory.wrap(new byte[] { 1, 2, 3 });
    var joined = factory.join(List.of(buffer));
    assertThat(joined.readableBytes()).isEqualTo(3);
    assertThat(joined.getByte(0)).isEqualTo((byte) 1);
  }

  @Test
  void joinReleasesInputBuffers() {
    var factory = new DefaultDataBufferFactory();
    var buffer1 = factory.allocateBuffer();
    var buffer2 = factory.allocateBuffer();
    factory.join(buffer1, buffer2);
    assertThat(buffer1.isAllocated()).isFalse();
    assertThat(buffer2.isAllocated()).isFalse();
  }

  @Test
  void defaultInitialCapacityIsUsedWhenNotSpecified() {
    var factory = new DefaultDataBufferFactory();
    var buffer = factory.allocateBuffer();
    assertThat(buffer.capacity()).isEqualTo(DefaultDataBufferFactory.DEFAULT_INITIAL_CAPACITY);
  }

  @Test
  void directBufferIsAllocatedWhenPreferred() {
    var factory = new DefaultDataBufferFactory(true);
    var buffer = factory.allocateBuffer();

    assertThat(buffer.byteBuffer.isDirect()).isTrue();
  }

}