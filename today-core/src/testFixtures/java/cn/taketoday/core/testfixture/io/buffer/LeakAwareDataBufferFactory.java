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

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.core.io.buffer.DefaultDataBufferFactory;
import cn.taketoday.core.io.buffer.NettyDataBufferFactory;
import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import io.netty.buffer.PooledByteBufAllocator;

/**
 * Implementation of the {@code DataBufferFactory} interface that keeps track of
 * memory leaks.
 * <p>Useful for unit tests that handle data buffers. Simply inherit from
 * {@link AbstractLeakCheckingTests} or call {@link #checkForLeaks()} in
 * a JUnit <em>after</em> method yourself, and any buffers that have not been
 * released will result in an {@link AssertionError}.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see LeakAwareDataBufferFactory
 */
public class LeakAwareDataBufferFactory implements DataBufferFactory {

  private static final Logger logger = LoggerFactory.getLogger(LeakAwareDataBufferFactory.class);

  private final DataBufferFactory delegate;

  private final List<LeakAwareDataBuffer> created = new ArrayList<>();

  private final AtomicBoolean trackCreated = new AtomicBoolean(true);

  /**
   * Creates a new {@code LeakAwareDataBufferFactory} by wrapping a
   * {@link DefaultDataBufferFactory}.
   */
  public LeakAwareDataBufferFactory() {
    this(new NettyDataBufferFactory(PooledByteBufAllocator.DEFAULT));
  }

  /**
   * Creates a new {@code LeakAwareDataBufferFactory} by wrapping the given delegate.
   *
   * @param delegate the delegate buffer factory to wrap.
   */
  public LeakAwareDataBufferFactory(DataBufferFactory delegate) {
    Assert.notNull(delegate, "Delegate is required");
    this.delegate = delegate;
  }

  /**
   * Checks whether all the data buffers allocated by this factory have also been released.
   * If not, then an {@link AssertionError} is thrown. Typically used from a JUnit <em>after</em>
   * method.
   */
  public void checkForLeaks() {
    checkForLeaks(Duration.ofSeconds(0));
  }

  /**
   * Variant of {@link #checkForLeaks()} with the option to wait for buffer release.
   *
   * @param timeout how long to wait for buffers to be released; 0 for no waiting
   */
  public void checkForLeaks(Duration timeout) {
    this.trackCreated.set(false);
    Instant start = Instant.now();
    while (true) {
      if (this.created.stream().noneMatch(LeakAwareDataBuffer::isAllocated)) {
        return;
      }
      if (Instant.now().isBefore(start.plus(timeout))) {
        try {
          Thread.sleep(50);
        }
        catch (InterruptedException ex) {
          // ignore
        }
        continue;
      }
      List<AssertionError> errors = this.created.stream()
              .filter(LeakAwareDataBuffer::isAllocated)
              .map(LeakAwareDataBuffer::leakError)
              .toList();

      errors.forEach(it -> logger.error("Leaked error: ", it));
      throw new AssertionError(errors.size() + " buffer leaks detected (see logs above)");
    }
  }

  @Override
  public DataBuffer allocateBuffer() {
    return createLeakAwareDataBuffer(this.delegate.allocateBuffer());
  }

  @Override
  public DataBuffer allocateBuffer(int initialCapacity) {
    return createLeakAwareDataBuffer(this.delegate.allocateBuffer(initialCapacity));
  }

  private DataBuffer createLeakAwareDataBuffer(DataBuffer delegateBuffer) {
    LeakAwareDataBuffer dataBuffer = new LeakAwareDataBuffer(delegateBuffer, this);
    if (this.trackCreated.get()) {
      this.created.add(dataBuffer);
    }
    return dataBuffer;
  }

  @Override
  public DataBuffer wrap(ByteBuffer byteBuffer) {
    return this.delegate.wrap(byteBuffer);
  }

  @Override
  public DataBuffer wrap(byte[] bytes) {
    return this.delegate.wrap(bytes);
  }

  @Override
  public DataBuffer join(List<? extends DataBuffer> dataBuffers) {
    // Remove LeakAwareDataBuffer wrapper so delegate can find native buffers
    dataBuffers = dataBuffers.stream()
            .map(o -> o instanceof LeakAwareDataBuffer ? ((LeakAwareDataBuffer) o).dataBuffer() : o)
            .toList();
    return new LeakAwareDataBuffer(this.delegate.join(dataBuffers), this);
  }

  @Override
  public boolean isDirect() {
    return this.delegate.isDirect();
  }

}
