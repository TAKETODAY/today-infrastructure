/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.server.support;

import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.util.ConcurrentModificationException;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import infra.util.ExceptionUtils;
import infra.util.concurrent.Awaiter;
import io.netty.buffer.ByteBuf;

/**
 * An {@link InputStream} backed by {@link Flow.Subscriber Flow.Subscriber}
 * receiving byte buffers from a source.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class BodyInputStream extends InputStream {

  private final ReentrantLock lock;

  private final int capacity;

  private final Awaiter awaiter;

  private final AtomicInteger workAmount = new AtomicInteger();

  private final AtomicBoolean cancelled = new AtomicBoolean();

  private final Queue<ByteBuf> queue;

  private volatile boolean closed;

  private boolean done;

  private @Nullable ByteBuf available;

  private @Nullable Throwable error;

  BodyInputStream(Awaiter awaiter) {
    this(awaiter, 128);
  }

  BodyInputStream(Awaiter awaiter, int capacity) {
    this.awaiter = awaiter;
    this.capacity = capacity;
    this.queue = new ConcurrentLinkedQueue<>();
    this.lock = new ReentrantLock(false);
  }

  public void onDataReceived(ByteBuf buffer) {
    if (this.done || this.cancelled.get()) {
      discard(buffer);
      return;
    }

    if (queue.size() >= capacity) {
      discard(buffer);
      this.error = new RuntimeException("Buffer overflow");
      this.done = true;
      return;
    }

    queue.offer(buffer);

    int previousWorkState = addWork();
    if (previousWorkState == Integer.MIN_VALUE) {
      ByteBuf value = queue.poll();
      if (value != null) {
        discard(value);
      }
      return;
    }

    if (previousWorkState == 0) {
      resume();
    }
  }

  public void onError(Throwable throwable) {
    if (this.done) {
      return;
    }
    this.error = throwable;
    this.done = true;

    if (addWork() == 0) {
      resume();
    }
  }

  public void onComplete() {
    if (this.done) {
      return;
    }

    this.done = true;

    if (addWork() == 0) {
      resume();
    }
  }

  int addWork() {
    for (; ; ) {
      int produced = this.workAmount.getPlain();

      if (produced == Integer.MIN_VALUE) {
        return Integer.MIN_VALUE;
      }

      int nextProduced = (produced == Integer.MAX_VALUE ? 1 : produced + 1);

      if (this.workAmount.weakCompareAndSetRelease(produced, nextProduced)) {
        return produced;
      }
    }
  }

  private void resume() {
    awaiter.resume();
  }

  /* InputStream implementation */

  @Override
  public int read() {
    if (!this.lock.tryLock()) {
      if (this.closed) {
        return -1;
      }
      throw new ConcurrentModificationException("Concurrent access is not allowed");
    }

    try {
      ByteBuf next = getNextOrAwait();

      if (next == null) {
        if (done) {
          this.closed = true;
          cleanAndFinalize();
          if (this.error == null) {
            return -1;
          }
          else {
            throw ExceptionUtils.sneakyThrow(this.error);
          }
        }
        else if (closed) {
          cleanAndFinalize();
          return -1;
        }
        else {
          return -1;
        }
      }

      return next.readByte() & 0xFF;
    }
    catch (Throwable ex) {
      this.closed = true;
      cancel();
      cleanAndFinalize();
      throw ExceptionUtils.sneakyThrow(ex);
    }
    finally {
      this.lock.unlock();
    }
  }

  @Override
  public int read(byte[] b, int off, int len) {
    Objects.checkFromIndexSize(off, len, b.length);
    if (len == 0) {
      return 0;
    }

    if (!this.lock.tryLock()) {
      if (this.closed) {
        return -1;
      }
      throw new ConcurrentModificationException("concurrent access is disallowed");
    }

    try {
      for (int j = 0; j < len; ) {
        ByteBuf next = getNextOrAwait();

        if (next == null) {
          if (done) {
            cleanAndFinalize();
            if (this.error == null) {
              this.closed = true;
              return j == 0 ? -1 : j;
            }
            else {
              if (j == 0) {
                this.closed = true;
                throw ExceptionUtils.sneakyThrow(this.error);
              }
              return j;
            }
          }
          else if (closed) {
            cancel();
            cleanAndFinalize();
            return -1;
          }
          else {
            return j;
          }
        }
        int initialReadPosition = next.readerIndex();
        next.readBytes(b, off + j, Math.min(len - j, next.readableBytes()));
        j += next.readerIndex() - initialReadPosition;
      }

      return len;
    }
    catch (Throwable ex) {
      this.closed = true;
      cancel();
      cleanAndFinalize();
      throw ExceptionUtils.sneakyThrow(ex);
    }
    finally {
      this.lock.unlock();
    }
  }

  @Nullable
  private ByteBuf getNextOrAwait() {
    if (this.available == null || this.available.readableBytes() == 0) {
      discard(this.available);
      this.available = null;

      int actualWorkAmount = this.workAmount.getAcquire();
      for (; ; ) {
        if (this.closed || this.cancelled.get()) {
          return null;
        }

        boolean done = this.done;
        ByteBuf buffer = queue.poll();
        if (buffer != null) {
          this.available = buffer;
          break;
        }

        if (done) {
          return null;
        }

        actualWorkAmount = this.workAmount.addAndGet(-actualWorkAmount);
        if (actualWorkAmount == 0) {
          requestNext();
          awaiter.await();
        }
      }
    }

    return this.available;
  }

  private void cleanAndFinalize() {
    discard(this.available);
    this.available = null;

    for (; ; ) {
      int workAmount = this.workAmount.getPlain();
      ByteBuf value;
      while ((value = queue.poll()) != null) {
        discard(value);
      }

      if (this.workAmount.weakCompareAndSetPlain(workAmount, Integer.MIN_VALUE)) {
        return;
      }
    }
  }

  @Override
  public void close() {
    if (this.closed) {
      return;
    }

    this.closed = true;

    if (!this.lock.tryLock()) {
      if (addWork() == 0) {
        resume();
      }
      return;
    }

    try {
      cancel();
      cleanAndFinalize();
    }
    finally {
      this.lock.unlock();
    }
  }

  private void cancel() {
    cancelled.set(true);
  }

  private void requestNext() {
//    channel.read();
  }

  private void discard(@Nullable ByteBuf buffer) {
    if (buffer != null) {
      buffer.release();
    }
  }

}
