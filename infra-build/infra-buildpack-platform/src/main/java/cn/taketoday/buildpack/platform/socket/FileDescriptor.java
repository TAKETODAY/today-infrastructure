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

package cn.taketoday.buildpack.platform.socket;

import java.io.Closeable;
import java.io.IOException;
import java.util.function.IntConsumer;

/**
 * Provides access to the underlying file system representation of an open file.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #acquire()
 * @since 4.0
 */
class FileDescriptor {

  private final Handle openHandle;

  private final Handle closedHandler;

  private final IntConsumer closer;

  private Status status = Status.OPEN;

  private int referenceCount;

  FileDescriptor(int handle, IntConsumer closer) {
    this.openHandle = new Handle(handle);
    this.closedHandler = new Handle(-1);
    this.closer = closer;
  }

  /**
   * Acquire an instance of the actual {@link Handle}. The caller must
   * {@link Handle#close() close} the resulting handle when done.
   *
   * @return the handle
   */
  synchronized Handle acquire() {
    this.referenceCount++;
    return (this.status != Status.OPEN) ? this.closedHandler : this.openHandle;
  }

  private synchronized void release() {
    this.referenceCount--;
    if (this.referenceCount == 0 && this.status == Status.CLOSE_PENDING) {
      this.closer.accept(this.openHandle.value);
      this.status = Status.CLOSED;
    }
  }

  /**
   * Close the underlying file when all handles have been released.
   */
  synchronized void close() {
    if (this.status == Status.OPEN) {
      if (this.referenceCount == 0) {
        this.closer.accept(this.openHandle.value);
        this.status = Status.CLOSED;
      }
      else {
        this.status = Status.CLOSE_PENDING;
      }
    }
  }

  /**
   * The status of the file descriptor.
   */
  private enum Status {

    OPEN, CLOSE_PENDING, CLOSED

  }

  /**
   * Provides access to the actual file descriptor handle.
   */
  final class Handle implements Closeable {

    private final int value;

    private Handle(int value) {
      this.value = value;
    }

    boolean isClosed() {
      return this.value == -1;
    }

    int intValue() {
      return this.value;
    }

    @Override
    public void close() throws IOException {
      if (!isClosed()) {
        release();
      }
    }

  }

}
