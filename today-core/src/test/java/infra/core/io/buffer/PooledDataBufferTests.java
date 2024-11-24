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

package infra.core.io.buffer;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DataBufferFactory;
import infra.core.io.buffer.NettyDataBufferFactory;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Arjen Poutsma
 * @author Sam Brannen
 */
class PooledDataBufferTests {

  @Nested
  class UnpooledByteBufAllocatorWithPreferDirectTrueTests implements PooledDataBufferTestingTrait {

    @Override
    public DataBufferFactory createDataBufferFactory() {
      return new NettyDataBufferFactory(new UnpooledByteBufAllocator(true));
    }
  }

  @Nested
  class UnpooledByteBufAllocatorWithPreferDirectFalseTests implements PooledDataBufferTestingTrait {

    @Override
    public DataBufferFactory createDataBufferFactory() {
      return new NettyDataBufferFactory(new UnpooledByteBufAllocator(true));
    }
  }

  @Nested
  class PooledByteBufAllocatorWithPreferDirectTrueTests implements PooledDataBufferTestingTrait {

    @Override
    public DataBufferFactory createDataBufferFactory() {
      return new NettyDataBufferFactory(new PooledByteBufAllocator(true));
    }
  }

  @Nested
  class PooledByteBufAllocatorWithPreferDirectFalseTests implements PooledDataBufferTestingTrait {

    @Override
    public DataBufferFactory createDataBufferFactory() {
      return new NettyDataBufferFactory(new PooledByteBufAllocator(true));
    }
  }

  interface PooledDataBufferTestingTrait {

    DataBufferFactory createDataBufferFactory();

    default DataBuffer createDataBuffer(int capacity) {
      return createDataBufferFactory().allocateBuffer(capacity);
    }

    @Test
    default void retainAndRelease() {
      DataBuffer buffer = createDataBuffer(1);
      buffer.write((byte) 'a');

      buffer.retain();
      assertThat(buffer.release()).isFalse();
      assertThat(buffer.release()).isTrue();
    }

    @Test
    default void tooManyReleases() {
      DataBuffer buffer = createDataBuffer(1);
      buffer.write((byte) 'a');

      buffer.release();
      assertThatIllegalStateException().isThrownBy(buffer::release);
    }

  }

}
