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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author TODAY 2021/8/21 01:28
 */
class ResizableByteArrayOutputStreamTests {

  private static final int INITIAL_CAPACITY = 256;

  private ResizableByteArrayOutputStream baos;

  private byte[] helloBytes;

  @BeforeEach
  void setUp() throws Exception {
    this.baos = new ResizableByteArrayOutputStream(INITIAL_CAPACITY);
    this.helloBytes = "Hello World".getBytes(StandardCharsets.UTF_8);
  }

  @Test
  void resize() throws Exception {
    assertThat(this.baos.capacity()).isEqualTo(INITIAL_CAPACITY);
    this.baos.write(helloBytes);
    int size = 64;
    this.baos.resize(size);
    assertThat(this.baos.capacity()).isEqualTo(size);
    assertByteArrayEqualsString(this.baos);
  }

  @Test
  void autoGrow() {
    assertThat(this.baos.capacity()).isEqualTo(INITIAL_CAPACITY);
    for (int i = 0; i < 129; i++) {
      this.baos.write(0);
    }
    assertThat(this.baos.capacity()).isEqualTo(256);
  }

  @Test
  void grow() throws Exception {
    assertThat(this.baos.capacity()).isEqualTo(INITIAL_CAPACITY);
    this.baos.write(helloBytes);
    this.baos.grow(1000);
    assertThat(this.baos.capacity()).isEqualTo((this.helloBytes.length + 1000));
    assertByteArrayEqualsString(this.baos);
  }

  @Test
  void write() throws Exception {
    this.baos.write(helloBytes);
    assertByteArrayEqualsString(this.baos);
  }

  @Test
  void failResize() throws Exception {
    this.baos.write(helloBytes);
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.baos.resize(5));
  }

  @Test
  void constructorWithDefaultCapacity() {
    ResizableByteArrayOutputStream stream = new ResizableByteArrayOutputStream();
    assertThat(stream.capacity()).isEqualTo(256);
  }

  @Test
  void constructorWithCustomCapacity() {
    ResizableByteArrayOutputStream stream = new ResizableByteArrayOutputStream(512);
    assertThat(stream.capacity()).isEqualTo(512);
  }

  @Test
  void constructorWithZeroCapacity() {
    ResizableByteArrayOutputStream stream = new ResizableByteArrayOutputStream(0);
    assertThat(stream.capacity()).isEqualTo(0);
  }

  @Test
  void resizeToSmallerThanContentSizeThrowsException() throws Exception {
    ResizableByteArrayOutputStream stream = new ResizableByteArrayOutputStream(10);
    byte[] data = "Hello World".getBytes();
    stream.write(data);

    assertThatIllegalArgumentException()
            .isThrownBy(() -> stream.resize(5))
            .withMessage("New capacity must not be smaller than current size");
  }

  @Test
  void resizeToExactContentSize() throws Exception {
    ResizableByteArrayOutputStream stream = new ResizableByteArrayOutputStream(10);
    byte[] data = "Hello".getBytes();
    stream.write(data);

    stream.resize(5);
    assertThat(stream.capacity()).isEqualTo(5);
    assertThat(stream.toByteArray()).isEqualTo(data);
  }

  @Test
  void growWithNegativeAdditionalCapacityThrowsException() {
    ResizableByteArrayOutputStream stream = new ResizableByteArrayOutputStream(10);

    assertThatIllegalArgumentException()
            .isThrownBy(() -> stream.grow(-1))
            .withMessage("Additional capacity must be 0 or higher");
  }

  @Test
  void growWhenNoAdditionalCapacityNeeded() throws Exception {
    ResizableByteArrayOutputStream stream = new ResizableByteArrayOutputStream(10);
    byte[] data = "Hello".getBytes();
    stream.write(data);

    int originalCapacity = stream.capacity();
    stream.grow(0);
    assertThat(stream.capacity()).isEqualTo(originalCapacity);
  }

  @Test
  void growExpandsBufferWhenNeeded() throws Exception {
    ResizableByteArrayOutputStream stream = new ResizableByteArrayOutputStream(10);
    byte[] data = "Hello".getBytes();
    stream.write(data);

    stream.grow(20);
    assertThat(stream.capacity()).isGreaterThanOrEqualTo(25); // 5 (current size) + 20
  }

  @Test
  void growDoublesBufferWhenNeeded() throws Exception {
    ResizableByteArrayOutputStream stream = new ResizableByteArrayOutputStream(10);
    byte[] data = "Hello World More Data".getBytes();
    stream.write(data); // This should be more than 10 bytes

    // The buffer should have been automatically doubled
    assertThat(stream.capacity()).isEqualTo(21);
  }

  @Test
  void capacityReturnsCorrectValueAfterMultipleOperations() throws Exception {
    ResizableByteArrayOutputStream stream = new ResizableByteArrayOutputStream(5);
    assertThat(stream.capacity()).isEqualTo(5);

    stream.write("Hello".getBytes());
    assertThat(stream.capacity()).isEqualTo(5);

    stream.write(" World".getBytes());
    assertThat(stream.capacity()).isEqualTo(11); // Doubled from 5

    stream.resize(30);
    assertThat(stream.capacity()).isEqualTo(30);

    stream.grow(5);
    assertThat(stream.capacity()).isEqualTo(30); // No growth needed
  }

  @Test
  void writeByteArrayPartially() throws Exception {
    ResizableByteArrayOutputStream stream = new ResizableByteArrayOutputStream(10);
    byte[] data = "Hello World".getBytes();

    stream.write(data, 0, 5);
    assertThat(stream.toByteArray()).isEqualTo("Hello".getBytes());

    stream.write(data, 5, 6);
    assertThat(stream.toByteArray()).isEqualTo("Hello World".getBytes());
  }

  @Test
  void writeSingleByte() throws Exception {
    ResizableByteArrayOutputStream stream = new ResizableByteArrayOutputStream(10);

    stream.write(65); // 'A'
    stream.write(66); // 'B'
    stream.write(67); // 'C'

    assertThat(stream.toByteArray()).isEqualTo("ABC".getBytes());
  }

  @Test
  void sizeReturnsCorrectValue() throws Exception {
    ResizableByteArrayOutputStream stream = new ResizableByteArrayOutputStream(10);
    assertThat(stream.size()).isEqualTo(0);

    stream.write("Hello".getBytes());
    assertThat(stream.size()).isEqualTo(5);

    stream.write(" World".getBytes());
    assertThat(stream.size()).isEqualTo(11);
  }

  @Test
  void resetClearsContentButPreservesCapacity() throws Exception {
    ResizableByteArrayOutputStream stream = new ResizableByteArrayOutputStream(10);
    stream.write("Hello".getBytes());

    int capacityBeforeReset = stream.capacity();
    stream.reset();

    assertThat(stream.size()).isEqualTo(0);
    assertThat(stream.capacity()).isEqualTo(capacityBeforeReset);
    assertThat(stream.toByteArray()).isEmpty();
  }

  @Test
  void resizeToMuchLargerCapacity() throws Exception {
    ResizableByteArrayOutputStream stream = new ResizableByteArrayOutputStream(5);
    byte[] data = "Hi".getBytes();
    stream.write(data);

    stream.resize(1000);
    assertThat(stream.capacity()).isEqualTo(1000);
    assertThat(stream.toByteArray()).isEqualTo(data);
  }

  @Test
  void growWithLargeAdditionalCapacity() throws Exception {
    ResizableByteArrayOutputStream stream = new ResizableByteArrayOutputStream(5);
    byte[] data = "Hello".getBytes();
    stream.write(data);

    stream.grow(1000);
    assertThat(stream.capacity()).isEqualTo(1005); // 5 (current size) + 1000
    assertThat(stream.toByteArray()).isEqualTo(data);
  }

  private void assertByteArrayEqualsString(ResizableByteArrayOutputStream actual) {
    assertThat(actual.toByteArray()).isEqualTo(helloBytes);
  }

}
