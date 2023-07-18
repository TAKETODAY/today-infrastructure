/*
 * Copyright 2017 - 2023 the original author or authors.
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

import org.junit.jupiter.api.Test;

import cn.taketoday.core.testfixture.io.buffer.LeakAwareDataBufferFactory;

import static cn.taketoday.core.io.buffer.DataBufferUtils.release;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Arjen Poutsma
 */
class LeakAwareDataBufferFactoryTests {
  private final LeakAwareDataBufferFactory bufferFactory = new LeakAwareDataBufferFactory();

  @Test
  void leak() {
    DataBuffer dataBuffer = this.bufferFactory.allocateBuffer();
    try {
      assertThatExceptionOfType(AssertionError.class).isThrownBy(
              this.bufferFactory::checkForLeaks);
    }
    finally {
      release(dataBuffer);
    }
  }

  @Test
  void noLeak() {
    DataBuffer dataBuffer = this.bufferFactory.allocateBuffer();
    release(dataBuffer);
    this.bufferFactory.checkForLeaks();
  }

}
