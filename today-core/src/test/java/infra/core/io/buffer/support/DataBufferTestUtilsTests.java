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

package infra.core.io.buffer.support;

import java.nio.charset.StandardCharsets;

import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DataBufferFactory;
import infra.core.testfixture.io.buffer.AbstractDataBufferAllocatingTests;
import infra.core.testfixture.io.buffer.DataBufferTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 * @author Sam Brannen
 */
class DataBufferTestUtilsTests extends AbstractDataBufferAllocatingTests {

  @ParameterizedDataBufferAllocatingTest
  void dumpBytes(DataBufferFactory bufferFactory) {
    this.bufferFactory = bufferFactory;

    DataBuffer buffer = this.bufferFactory.allocateBuffer(4);
    byte[] source = { 'a', 'b', 'c', 'd' };
    buffer.write(source);

    byte[] result = DataBufferTestUtils.dumpBytes(buffer);

    assertThat(result).isEqualTo(source);

    release(buffer);
  }

  @ParameterizedDataBufferAllocatingTest
  void dumpString(DataBufferFactory bufferFactory) {
    this.bufferFactory = bufferFactory;

    DataBuffer buffer = this.bufferFactory.allocateBuffer(4);
    String source = "abcd";
    buffer.write(source.getBytes(StandardCharsets.UTF_8));
    String result = buffer.toString(StandardCharsets.UTF_8);
    release(buffer);

    assertThat(result).isEqualTo(source);
  }

}
