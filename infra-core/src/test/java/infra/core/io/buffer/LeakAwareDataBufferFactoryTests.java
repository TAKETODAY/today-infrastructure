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

import infra.core.testfixture.io.buffer.LeakAwareDataBufferFactory;

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
      dataBuffer.release();
    }
  }

  @Test
  void noLeak() {
    DataBuffer dataBuffer = this.bufferFactory.allocateBuffer();
    dataBuffer.release();
    this.bufferFactory.checkForLeaks();
  }

}
