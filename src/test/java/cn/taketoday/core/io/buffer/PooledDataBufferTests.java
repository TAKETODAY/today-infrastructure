/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.core.io.buffer;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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

		default PooledDataBuffer createDataBuffer(int capacity) {
			return (PooledDataBuffer) createDataBufferFactory().allocateBuffer(capacity);
		}

		@Test
		default void retainAndRelease() {
			PooledDataBuffer buffer = createDataBuffer(1);
			buffer.write((byte) 'a');

			buffer.retain();
			assertThat(buffer.release()).isFalse();
			assertThat(buffer.release()).isTrue();
		}

		@Test
		default void tooManyReleases() {
			PooledDataBuffer buffer = createDataBuffer(1);
			buffer.write((byte) 'a');

			buffer.release();
			assertThatIllegalStateException().isThrownBy(buffer::release);
		}

	}

}
