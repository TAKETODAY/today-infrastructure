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

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LimitedDataBufferList}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
class LimitedDataBufferListTests {

  @Test
  void limitEnforced() {
    LimitedDataBufferList list = new LimitedDataBufferList(5);

    assertThatThrownBy(() -> list.add(toDataBuffer("123456"))).isInstanceOf(DataBufferLimitException.class);
    assertThat(list).isEmpty();
  }

  @Test
  void limitIgnored() {
    new LimitedDataBufferList(-1).add(toDataBuffer("123456"));
  }

  @Test
  void clearResetsCount() {
    LimitedDataBufferList list = new LimitedDataBufferList(5);
    list.add(toDataBuffer("12345"));
    list.clear();
    list.add(toDataBuffer("12345"));
  }

  @Test
  void addAllEnforcesLimit() {
    LimitedDataBufferList list = new LimitedDataBufferList(10);

    List<DataBuffer> buffers = List.of(
            toDataBuffer("123"),
            toDataBuffer("456"),
            toDataBuffer("789")
    );

    list.addAll(buffers);
    assertThat(list).hasSize(3);
  }

  @Test
  void addAllAtIndexEnforcesLimit() {
    LimitedDataBufferList list = new LimitedDataBufferList(10);
    list.add(toDataBuffer("12"));

    List<DataBuffer> buffers = List.of(
            toDataBuffer("345"),
            toDataBuffer("678"),
            toDataBuffer("901")
    );

    assertThatThrownBy(() -> list.addAll(0, buffers))
            .isInstanceOf(DataBufferLimitException.class);
    assertThat(list).hasSize(4);
  }

  @Test
  void addAtIndexEnforcesLimit() {
    LimitedDataBufferList list = new LimitedDataBufferList(5);
    list.add(toDataBuffer("12"));

    assertThatThrownBy(() -> list.add(0, toDataBuffer("3456")))
            .isInstanceOf(DataBufferLimitException.class);
    assertThat(list).hasSize(2);
  }

  @Test
  void removesAreNotSupported() {
    LimitedDataBufferList list = new LimitedDataBufferList(10);
    list.add(toDataBuffer("test"));

    assertThatThrownBy(() -> list.remove(0))
            .isInstanceOf(UnsupportedOperationException.class);

    assertThatThrownBy(() -> list.remove(toDataBuffer("test")))
            .isInstanceOf(UnsupportedOperationException.class);

    assertThatThrownBy(() -> list.removeRange(0, 1))
            .isInstanceOf(UnsupportedOperationException.class);

    assertThatThrownBy(() -> list.removeAll(List.of()))
            .isInstanceOf(UnsupportedOperationException.class);

    assertThatThrownBy(() -> list.removeIf(b -> true))
            .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void setIsNotSupported() {
    LimitedDataBufferList list = new LimitedDataBufferList(10);
    list.add(toDataBuffer("test"));

    assertThatThrownBy(() -> list.set(0, toDataBuffer("new")))
            .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void releaseAndClearFreesBuffers() {
    LimitedDataBufferList list = new LimitedDataBufferList(10);
    DataBuffer buffer1 = toDataBuffer("123");
    DataBuffer buffer2 = toDataBuffer("456");
    list.add(buffer1);
    list.add(buffer2);

    list.releaseAndClear();

    assertThat(list).isEmpty();
    assertThat(buffer1.isAllocated()).isFalse();
    assertThat(buffer2.isAllocated()).isFalse();
  }

  @Test
  void releaseAndClearHandlesFailures() {
    LimitedDataBufferList list = new LimitedDataBufferList(10);
    DataBuffer buffer1 = toDataBuffer("123");
    DataBuffer buffer2 = toDataBuffer("456");
    buffer1.release(); // Pre-release to cause failure
    list.add(buffer1);
    list.add(buffer2);

    list.releaseAndClear(); // Should not throw

    assertThat(list).isEmpty();
    assertThat(buffer2.isAllocated()).isFalse();
  }

  @Test
  void overflowingIntegerMaxValueRaisesException() {
    LimitedDataBufferList list = new LimitedDataBufferList(10);
    DataBuffer huge = mock(DataBuffer.class);
    when(huge.readableBytes()).thenReturn(Integer.MAX_VALUE);

    list.add(toDataBuffer("123")); // Add some bytes first

    assertThatThrownBy(() -> list.add(huge))
            .isInstanceOf(DataBufferLimitException.class);
  }

  @Test
  void successfulAddAllAtIndex() {
    LimitedDataBufferList list = new LimitedDataBufferList(10);
    list.add(toDataBuffer("12"));

    List<DataBuffer> buffers = List.of(toDataBuffer("34"), toDataBuffer("56"));
    list.addAll(0, buffers);

    assertThat(list).hasSize(3);
  }

  @Test
  void successfulAddAll() {
    LimitedDataBufferList list = new LimitedDataBufferList(10);
    List<DataBuffer> buffers = List.of(toDataBuffer("12"), toDataBuffer("34"));

    assertThat(list.addAll(buffers)).isTrue();
    assertThat(list).hasSize(2);
  }

  private static DataBuffer toDataBuffer(String value) {
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    return DefaultDataBufferFactory.sharedInstance.wrap(bytes);
  }

}
