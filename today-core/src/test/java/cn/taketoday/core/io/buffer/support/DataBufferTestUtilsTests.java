/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.core.io.buffer.support;

import java.nio.charset.StandardCharsets;

import cn.taketoday.core.io.buffer.AbstractDataBufferAllocatingTests;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.core.testfixture.io.buffer.DataBufferTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 * @author Sam Brannen
 */
class DataBufferTestUtilsTests extends AbstractDataBufferAllocatingTests {

  @ParameterizedDataBufferAllocatingTest
  void dumpBytes(String displayName, DataBufferFactory bufferFactory) {
    this.bufferFactory = bufferFactory;

    DataBuffer buffer = this.bufferFactory.allocateBuffer(4);
    byte[] source = { 'a', 'b', 'c', 'd' };
    buffer.write(source);

    byte[] result = DataBufferTestUtils.dumpBytes(buffer);

    assertThat(result).isEqualTo(source);

    release(buffer);
  }

  @ParameterizedDataBufferAllocatingTest
  void dumpString(String displayName, DataBufferFactory bufferFactory) {
    this.bufferFactory = bufferFactory;

    DataBuffer buffer = this.bufferFactory.allocateBuffer(4);
    String source = "abcd";
    buffer.write(source.getBytes(StandardCharsets.UTF_8));
    String result = buffer.toString(StandardCharsets.UTF_8);
    release(buffer);

    assertThat(result).isEqualTo(source);
  }

}
