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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import reactor.core.publisher.Flux;

/**
 * Custom {@link List} to collect data buffers with and enforce a
 * limit on the total number of bytes buffered. For use with "collect" or
 * other buffering operators in declarative APIs, e.g. {@link Flux}.
 *
 * <p>Adding elements increases the byte count and if the limit is exceeded,
 * {@link DataBufferLimitException} is raised.  {@link #clear()} resets the
 * count. Remove and set are not supported.
 *
 * <p><strong>Note:</strong> This class does not automatically release the
 * buffers it contains. It is usually preferable to use hooks such as
 * {@link Flux#doOnDiscard} that also take care of cancel and error signals,
 * or otherwise {@link #releaseAndClear()} can be used.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
@SuppressWarnings("serial")
public class LimitedDataBufferList extends ArrayList<DataBuffer> {

  private int byteCount;

  private final int maxByteCount;

  public LimitedDataBufferList(int maxByteCount) {
    this.maxByteCount = maxByteCount;
  }

  @Override
  public boolean add(DataBuffer buffer) {
    updateCount(buffer.readableBytes());
    return super.add(buffer);
  }

  @Override
  public void add(int index, DataBuffer buffer) {
    super.add(index, buffer);
    updateCount(buffer.readableBytes());
  }

  @Override
  public boolean addAll(Collection<? extends DataBuffer> collection) {
    boolean result = super.addAll(collection);
    for (DataBuffer buffer : collection) {
      updateCount(buffer.readableBytes());
    }
    return result;
  }

  @Override
  public boolean addAll(int index, Collection<? extends DataBuffer> collection) {
    boolean result = super.addAll(index, collection);
    for (DataBuffer buffer : collection) {
      updateCount(buffer.readableBytes());
    }
    return result;
  }

  private void updateCount(int bytesToAdd) {
    if (this.maxByteCount < 0) {
      return;
    }
    if (bytesToAdd > Integer.MAX_VALUE - this.byteCount) {
      raiseLimitException();
    }
    else {
      this.byteCount += bytesToAdd;
      if (this.byteCount > this.maxByteCount) {
        raiseLimitException();
      }
    }
  }

  private void raiseLimitException() {
    // Do not release here, it's likely down via doOnDiscard..
    throw new DataBufferLimitException(
            "Exceeded limit on max bytes to buffer : " + this.maxByteCount);
  }

  @Override
  public DataBuffer remove(int index) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean remove(Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void removeRange(int fromIndex, int toIndex) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean removeIf(Predicate<? super DataBuffer> filter) {
    throw new UnsupportedOperationException();
  }

  @Override
  public DataBuffer set(int index, DataBuffer element) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    this.byteCount = 0;
    super.clear();
  }

  /**
   * Shortcut to {@link DataBuffer#release release} all data buffers and
   * then {@link #clear()}.
   */
  public void releaseAndClear() {
    for (DataBuffer buf : this) {
      try {
        buf.release();
      }
      catch (Throwable ex) {
        // Keep going..
      }
    }
    clear();
  }

}
