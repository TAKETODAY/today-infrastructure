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

package infra.core.io.buffer;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/3/10 21:15
 */
class DefaultDataBufferTests {

  private final DefaultDataBufferFactory bufferFactory = new DefaultDataBufferFactory();

  @Test
  void getNativeBuffer() {
    DefaultDataBuffer dataBuffer = this.bufferFactory.allocateBuffer(256);
    dataBuffer.write("0123456789", StandardCharsets.UTF_8);

    byte[] result = new byte[7];
    dataBuffer.read(result);
    assertThat(result).isEqualTo("0123456".getBytes(StandardCharsets.UTF_8));

    ByteBuffer nativeBuffer = dataBuffer.getNativeBuffer();
    assertThat(nativeBuffer.position()).isEqualTo(7);
    assertThat(dataBuffer.readPosition()).isEqualTo(7);
    assertThat(nativeBuffer.limit()).isEqualTo(10);
    assertThat(dataBuffer.writePosition()).isEqualTo(10);
    assertThat(nativeBuffer.capacity()).isEqualTo(256);
    assertThat(dataBuffer.capacity()).isEqualTo(256);

    dataBuffer.release();
  }

}