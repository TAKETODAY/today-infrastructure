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

package cn.taketoday.core.testfixture.io.buffer;

import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferUtils;
import cn.taketoday.core.io.buffer.DataBufferWrapper;
import cn.taketoday.core.io.buffer.PooledDataBuffer;
import cn.taketoday.lang.Assert;

/**
 * DataBuffer implementation created by {@link LeakAwareDataBufferFactory}.
 *
 * @author Arjen Poutsma
 */
class LeakAwareDataBuffer extends DataBufferWrapper implements PooledDataBuffer {

  private final AssertionError leakError;

  private final LeakAwareDataBufferFactory dataBufferFactory;

  LeakAwareDataBuffer(DataBuffer delegate, LeakAwareDataBufferFactory dataBufferFactory) {
    super(delegate);
    Assert.notNull(dataBufferFactory, "DataBufferFactory is required");
    this.dataBufferFactory = dataBufferFactory;
    this.leakError = createLeakError(delegate);
  }

  private static AssertionError createLeakError(DataBuffer delegate) {
    String message = String.format("DataBuffer leak detected: {%s} has not been released.%n" +
                    "Stack trace of buffer allocation statement follows:",
            delegate);
    AssertionError result = new AssertionError(message);
    // remove first four irrelevant stack trace elements
    StackTraceElement[] oldTrace = result.getStackTrace();
    StackTraceElement[] newTrace = new StackTraceElement[oldTrace.length - 4];
    System.arraycopy(oldTrace, 4, newTrace, 0, oldTrace.length - 4);
    result.setStackTrace(newTrace);
    return result;
  }

  AssertionError leakError() {
    return this.leakError;
  }

  @Override
  public boolean isAllocated() {
    DataBuffer delegate = dataBuffer();
    return delegate instanceof PooledDataBuffer &&
            ((PooledDataBuffer) delegate).isAllocated();
  }

  @Override
  public PooledDataBuffer retain() {
    DataBufferUtils.retain(dataBuffer());
    return this;
  }

  @Override
  public PooledDataBuffer touch(Object hint) {
    DataBufferUtils.touch(dataBuffer(), hint);
    return this;
  }

  @Override
  public boolean release() {
    DataBufferUtils.release(dataBuffer());
    return isAllocated();
  }

  @Override
  public LeakAwareDataBufferFactory factory() {
    return this.dataBufferFactory;
  }

  @Override
  public String toString() {
    return String.format("LeakAwareDataBuffer (%s)", dataBuffer());
  }
}
