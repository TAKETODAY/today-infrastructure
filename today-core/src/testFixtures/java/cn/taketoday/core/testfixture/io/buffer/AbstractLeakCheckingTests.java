/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.core.testfixture.io.buffer;

import org.junit.jupiter.api.AfterEach;

import java.time.Duration;

import cn.taketoday.core.io.buffer.DataBufferFactory;

/**
 * Abstract base class for unit tests that allocate data buffers via a {@link DataBufferFactory}.
 * After each unit test, this base class checks whether all created buffers have been released,
 * throwing an {@link AssertionError} if not.
 *
 * @author Arjen Poutsma
 * @see LeakAwareDataBufferFactory
 */
public abstract class AbstractLeakCheckingTests {

  /**
   * The data buffer factory.
   */
  protected final LeakAwareDataBufferFactory bufferFactory = new LeakAwareDataBufferFactory();

  /**
   * Checks whether any of the data buffers created by {@link #bufferFactory} have not been
   * released, throwing an assertion error if so.
   */
  @AfterEach
  final void checkForLeaks() {
    this.bufferFactory.checkForLeaks(Duration.ofSeconds(1));
  }

}
