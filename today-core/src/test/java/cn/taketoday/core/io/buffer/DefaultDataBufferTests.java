/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.core.io.buffer;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static cn.taketoday.core.io.buffer.DataBufferUtils.release;
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

    release(dataBuffer);
  }

}