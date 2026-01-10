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

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.IntPredicate;

import infra.core.io.buffer.DataBuffer.ByteBufferIterator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/22 16:32
 */
class DataBufferWrapperTests {

  @Test
  void wrappingNullDelegateThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new DataBufferWrapper(null));
  }

  @Test
  void delegateMethodsForwardToWrappedBuffer() {
    DataBuffer delegate = mock(DataBuffer.class);
    DataBufferWrapper wrapper = new DataBufferWrapper(delegate);

    when(delegate.readPosition()).thenReturn(42);
    when(delegate.writePosition()).thenReturn(100);
    when(delegate.readableBytes()).thenReturn(58);
    when(delegate.capacity()).thenReturn(1024);

    assertThat(wrapper.readPosition()).isEqualTo(42);
    assertThat(wrapper.writePosition()).isEqualTo(100);
    assertThat(wrapper.readableBytes()).isEqualTo(58);
    assertThat(wrapper.capacity()).isEqualTo(1024);

    verify(delegate).readPosition();
    verify(delegate).writePosition();
    verify(delegate).readableBytes();
    verify(delegate).capacity();
  }

  @Test
  void readWriteOperationsForwardToDelegate() throws Exception {
    DataBuffer delegate = mock(DataBuffer.class);
    DataBufferWrapper wrapper = new DataBufferWrapper(delegate);

    byte[] data = "test".getBytes();
    wrapper.write(data);
    verify(delegate).write(data);

    when(delegate.read()).thenReturn((byte) 42);
    assertThat(wrapper.read()).isEqualTo((byte) 42);
  }

  @Test
  void sliceOperationsReturnDelegateResults() {
    DataBuffer delegate = mock(DataBuffer.class);
    DataBufferWrapper wrapper = new DataBufferWrapper(delegate);
    DataBuffer sliced = mock(DataBuffer.class);

    when(delegate.slice(0, 10)).thenReturn(sliced);
    when(delegate.retainedSlice(5, 15)).thenReturn(sliced);
    when(delegate.split(20)).thenReturn(sliced);

    assertThat(wrapper.slice(0, 10)).isSameAs(sliced);
    assertThat(wrapper.retainedSlice(5, 15)).isSameAs(sliced);
    assertThat(wrapper.split(20)).isSameAs(sliced);
  }

  @Test
  void byteBufferOperationsForwardToDelegate() {
    DataBuffer delegate = mock(DataBuffer.class);
    DataBufferWrapper wrapper = new DataBufferWrapper(delegate);
    ByteBuffer byteBuffer = ByteBuffer.allocate(10);

    when(delegate.asByteBuffer()).thenReturn(byteBuffer);
    when(delegate.toByteBuffer()).thenReturn(byteBuffer);

    assertThat(wrapper.asByteBuffer()).isSameAs(byteBuffer);
    assertThat(wrapper.toByteBuffer()).isSameAs(byteBuffer);
  }

  @Test
  void releaseRetainOperationsForwardToDelegate() {
    DataBuffer delegate = mock(DataBuffer.class);
    DataBufferWrapper wrapper = new DataBufferWrapper(delegate);

    when(delegate.release()).thenReturn(true);
    when(delegate.retain()).thenReturn(delegate);
    when(delegate.touch("hint")).thenReturn(delegate);

    assertThat(wrapper.release()).isTrue();
    assertThat(wrapper.retain()).isSameAs(delegate);
    assertThat(wrapper.touch("hint")).isSameAs(delegate);
  }

  @Test
  void stringConversionOperationsForwardToDelegate() {
    DataBuffer delegate = mock(DataBuffer.class);
    DataBufferWrapper wrapper = new DataBufferWrapper(delegate);
    Charset charset = StandardCharsets.UTF_8;

    when(delegate.toString(charset)).thenReturn("test");
    when(delegate.toString(0, 4, charset)).thenReturn("test");

    assertThat(wrapper.toString(charset)).isEqualTo("test");
    assertThat(wrapper.toString(0, 4, charset)).isEqualTo("test");
  }

  @Test
  void writePositionChangesAreForwardedToDelegate() {
    DataBuffer delegate = mock(DataBuffer.class);
    DataBufferWrapper wrapper = new DataBufferWrapper(delegate);
    wrapper.writePosition(50);
    verify(delegate).writePosition(50);
  }

  @Test
  void ensureWritableForwardsToDelegate() {
    DataBuffer delegate = mock(DataBuffer.class);
    DataBufferWrapper wrapper = new DataBufferWrapper(delegate);
    wrapper.ensureWritable(100);
    verify(delegate).ensureWritable(100);
  }

  @Test
  void capacityChangeIsForwardedToDelegate() {
    DataBuffer delegate = mock(DataBuffer.class);
    DataBufferWrapper wrapper = new DataBufferWrapper(delegate);
    wrapper.capacity(200);
    verify(delegate).capacity(200);
  }

  @Test
  void byteOperationsForwardToDelegate() {
    DataBuffer delegate = mock(DataBuffer.class);
    DataBufferWrapper wrapper = new DataBufferWrapper(delegate);

    when(delegate.getByte(5)).thenReturn((byte)123);
    assertThat(wrapper.getByte(5)).isEqualTo((byte)123);

    wrapper.write((byte)45);
    verify(delegate).write((byte)45);
  }

  @Test
  void inputStreamOperationsForwardToDelegate() {
    DataBuffer delegate = mock(DataBuffer.class);
    DataBufferWrapper wrapper = new DataBufferWrapper(delegate);
    InputStream mockStream = mock(InputStream.class);

    when(delegate.asInputStream()).thenReturn(mockStream);
    when(delegate.asInputStream(true)).thenReturn(mockStream);

    assertThat(wrapper.asInputStream()).isSameAs(mockStream);
    assertThat(wrapper.asInputStream(true)).isSameAs(mockStream);
  }

  @Test
  void outputStreamOperationsForwardToDelegate() {
    DataBuffer delegate = mock(DataBuffer.class);
    DataBufferWrapper wrapper = new DataBufferWrapper(delegate);
    OutputStream mockStream = mock(OutputStream.class);

    when(delegate.asOutputStream()).thenReturn(mockStream);
    assertThat(wrapper.asOutputStream()).isSameAs(mockStream);
  }

  @Test
  void byteBufferIteratorOperationsForwardToDelegate() {
    DataBuffer delegate = mock(DataBuffer.class);
    DataBufferWrapper wrapper = new DataBufferWrapper(delegate);
    ByteBufferIterator mockIterator = mock(ByteBufferIterator.class);

    when(delegate.readableByteBuffers()).thenReturn(mockIterator);
    when(delegate.writableByteBuffers()).thenReturn(mockIterator);

    assertThat(wrapper.readableByteBuffers()).isSameAs(mockIterator);
    assertThat(wrapper.writableByteBuffers()).isSameAs(mockIterator);
  }

  @Test
  void factoryForwardsToDelegate() {
    DataBuffer delegate = mock(DataBuffer.class);
    DataBufferWrapper wrapper = new DataBufferWrapper(delegate);
    DataBufferFactory mockFactory = mock(DataBufferFactory.class);

    when(delegate.factory()).thenReturn(mockFactory);
    assertThat(wrapper.factory()).isSameAs(mockFactory);
  }

  @Test
  void indexOfOperationsForwardToDelegate() {
    DataBuffer delegate = mock(DataBuffer.class);
    DataBufferWrapper wrapper = new DataBufferWrapper(delegate);
    IntPredicate predicate = i -> i == 0;

    when(delegate.indexOf(predicate, 0)).thenReturn(5);
    when(delegate.lastIndexOf(predicate, 10)).thenReturn(8);

    assertThat(wrapper.indexOf(predicate, 0)).isEqualTo(5);
    assertThat(wrapper.lastIndexOf(predicate, 10)).isEqualTo(8);
  }

  @Test
  void writableByteCountForwardsToDelegate() {
    DataBuffer delegate = mock(DataBuffer.class);
    DataBufferWrapper wrapper = new DataBufferWrapper(delegate);

    when(delegate.writableBytes()).thenReturn(100);
    assertThat(wrapper.writableBytes()).isEqualTo(100);
  }

  @Test
  void toByteBufferOperationsForwardToDelegate() {
    DataBuffer delegate = mock(DataBuffer.class);
    DataBufferWrapper wrapper = new DataBufferWrapper(delegate);
    ByteBuffer destBuffer = ByteBuffer.allocate(10);

    wrapper.toByteBuffer(destBuffer);
    verify(delegate).toByteBuffer(destBuffer);

    wrapper.toByteBuffer(0, destBuffer, 0, 10);
    verify(delegate).toByteBuffer(0, destBuffer, 0, 10);
  }

  @Test
  void byteArrayWriteOperationsForwardToDelegate() {
    DataBuffer delegate = mock(DataBuffer.class);
    DataBufferWrapper wrapper = new DataBufferWrapper(delegate);
    byte[] source = "test data".getBytes();

    wrapper.write(source, 1, 4);
    verify(delegate).write(source, 1, 4);

    wrapper.write(new DataBuffer[]{ mock(DataBuffer.class) });
    verify(delegate).write(any(DataBuffer[].class));

    wrapper.write(new ByteBuffer[]{ ByteBuffer.allocate(10) });
    verify(delegate).write(any(ByteBuffer[].class));

    wrapper.write("test", StandardCharsets.UTF_8);
    verify(delegate).write(eq("test"), eq(StandardCharsets.UTF_8));
  }

  @Test
  void nullSourceWriteOperationsForwardToDelegate() {
    DataBuffer delegate = mock(DataBuffer.class);
    DataBufferWrapper wrapper = new DataBufferWrapper(delegate);

    wrapper.write((DataBuffer)null);
    verify(delegate).write((DataBuffer)null);

    wrapper.write((ByteBuffer)null);
    verify(delegate).write((ByteBuffer)null);
  }

  @Test
  void toByteBufferOverloadsCoverAllVariants() {
    DataBuffer delegate = mock(DataBuffer.class);
    DataBufferWrapper wrapper = new DataBufferWrapper(delegate);
    ByteBuffer byteBuffer = ByteBuffer.allocate(10);

    when(delegate.toByteBuffer(0, 5)).thenReturn(byteBuffer);
    assertThat(wrapper.toByteBuffer(0, 5)).isSameAs(byteBuffer);
    verify(delegate).toByteBuffer(0, 5);

    when(delegate.asByteBuffer(2, 8)).thenReturn(byteBuffer);
    assertThat(wrapper.asByteBuffer(2, 8)).isSameAs(byteBuffer);
    verify(delegate).asByteBuffer(2, 8);

    wrapper.toByteBuffer(1, byteBuffer, 2, 3);
    verify(delegate).toByteBuffer(1, byteBuffer, 2, 3);
  }

  @Test
  void multipleToByteBufferCallsForwardCorrectly() {
    DataBuffer delegate = mock(DataBuffer.class);
    DataBufferWrapper wrapper = new DataBufferWrapper(delegate);
    ByteBuffer buffer1 = ByteBuffer.allocate(5);
    ByteBuffer buffer2 = ByteBuffer.allocate(10);

    when(delegate.toByteBuffer()).thenReturn(buffer1);
    when(delegate.toByteBuffer(5, 10)).thenReturn(buffer2);

    assertThat(wrapper.toByteBuffer()).isSameAs(buffer1);
    assertThat(wrapper.toByteBuffer(5, 10)).isSameAs(buffer2);

    verify(delegate).toByteBuffer();
    verify(delegate).toByteBuffer(5, 10);
  }

  @Test
  void isAllocatedForwardsToDelegate() {
    DataBuffer delegate = mock(DataBuffer.class);
    DataBufferWrapper wrapper = new DataBufferWrapper(delegate);

    when(delegate.isAllocated()).thenReturn(true);
    assertThat(wrapper.isAllocated()).isTrue();

    when(delegate.isAllocated()).thenReturn(false);
    assertThat(wrapper.isAllocated()).isFalse();

    verify(delegate, times(2)).isAllocated();
  }

  @Test
  void readOperationsForwardToDelegate() {
    DataBuffer delegate = mock(DataBuffer.class);
    DataBufferWrapper wrapper = new DataBufferWrapper(delegate);
    byte[] destination = new byte[10];

    wrapper.read(destination);
    verify(delegate).read(destination);

    wrapper.read(destination, 2, 5);
    verify(delegate).read(destination, 2, 5);
  }

}