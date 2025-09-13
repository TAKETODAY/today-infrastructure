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

import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ConcurrentModificationException;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

import infra.lang.Nullable;
import infra.util.ExceptionUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.TooLongHttpContentException;

/**
 * An {@link InputStream} backed by {@link Flow.Subscriber Flow.Subscriber}
 * receiving byte buffers from a source.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
final class SubscriberInputStream extends InputStream {

  private static final Object READY = new Object();

  private final ReentrantLock lock;

  private final Channel channel;

  private final int capacity;

  private final long maxContentLength = 0;

  private final AtomicReference<Object> parkedThread = new AtomicReference<>();

  private final AtomicInteger workAmount = new AtomicInteger();

  private final AtomicBoolean cancelled = new AtomicBoolean();

  // 跟踪已接收的总字节数
  private final AtomicLong receivedBytes = new AtomicLong(0);

  private volatile boolean closed;

  @Nullable
  private ByteBuf available;

  @Nullable
  private Queue<ByteBuf> queue;

  private boolean done;

  @Nullable
  private Throwable error;

  SubscriberInputStream(Channel channel) {
    this.channel = channel;
    this.capacity = 32;
    this.lock = new ReentrantLock(false);
  }

  /**
   * @param capacity the buffer capacity
   */
  SubscriberInputStream(Channel channel, int capacity) {
    this.channel = channel;
    this.capacity = capacity;
    this.lock = new ReentrantLock(false);
  }

  public void onNext(ByteBuf buffer) {
    if (this.done || this.cancelled.get()) {
      discard(buffer);
      return;
    }

    // 检查内容长度限制
    long currentBytes = receivedBytes.get();
    int bufferSize = buffer.readableBytes();
    if (currentBytes + bufferSize > maxContentLength) {
      discard(buffer);
      this.error = new TooLongHttpContentException(String.format("Content length exceeded %d bytes", maxContentLength));
      this.done = true;
      return;
    }

    Queue<ByteBuf> queue = getQueue();
    if (queue.size() >= capacity) {
      discard(buffer);
      this.error = new RuntimeException("Buffer overflow");
      this.done = true;
      return;
    }

    queue.offer(buffer);
    receivedBytes.addAndGet(bufferSize);

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

  private Queue<ByteBuf> getQueue() {
    Queue<ByteBuf> queue = this.queue;
    if (queue == null) {
      queue = new ArrayDeque<>(capacity);
      this.queue = queue;
    }
    return queue;
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
    Object last = parkedThread.getAndSet(READY);
    if (last != READY) {
      LockSupport.unpark((Thread) last);
    }
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
        Queue<ByteBuf> queue = this.queue;
        ByteBuf buffer = queue != null ? queue.poll() : null;
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
          await();
        }
      }
    }

    return this.available;
  }

  private void cleanAndFinalize() {
    discard(this.available);
    this.available = null;

    receivedBytes.set(0);

    for (; ; ) {
      int workAmount = this.workAmount.getPlain();
      Queue<ByteBuf> queue = this.queue;
      if (queue != null) {
        ByteBuf value;
        while ((value = queue.poll()) != null) {
          discard(value);
        }
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

  public long getReceivedBytes() {
    return receivedBytes.get();
  }

  private void cancel() {
    cancelled.set(true);
  }

  private void requestNext() {
    channel.read();
  }

  private void discard(@Nullable ByteBuf buffer) {
    if (buffer != null) {
      buffer.release();
    }
  }

  private void await() {
    Thread toUnpark = Thread.currentThread();

    while (true) {
      Object current = this.parkedThread.get();
      if (current == READY) {
        break;
      }

      if (current != null && current != toUnpark) {
        throw new IllegalStateException("Only one (Virtual)Thread can await!");
      }

      if (this.parkedThread.compareAndSet(null, toUnpark)) {
        LockSupport.park();
        // we don't just break here because park() can wake up spuriously
        // if we got a proper resume, get() == READY and the loop will quit above
      }
    }
    // clear the resume indicator so that the next await call will park without a resume()
    this.parkedThread.lazySet(null);
  }

  // 添加自适应自旋配置
  private static final int SPIN_THRESHOLD = 1000;
  private static final int CPU_CORES = Runtime.getRuntime().availableProcessors();
  private static final int MAX_SPIN = CPU_CORES > 1 ? 1000 : 0;

  private final AtomicInteger successiveSpins = new AtomicInteger();

  private void adaptiveWait() {
    Thread toUnpark = Thread.currentThread();
    int currentSpinCount = calculateSpinCount();

    while (true) {
      Object current = this.parkedThread.get();
      if (current == READY) {
        // 自旋成功，增加下次自旋次数
        successiveSpins.incrementAndGet();
        break;
      }

      if (current != null && current != toUnpark) {
        throw new IllegalStateException("Only one (Virtual)Thread can await!");
      }

      // 先尝试自旋
      if (currentSpinCount > 0) {
        currentSpinCount--;
        if (currentSpinCount % 10 == 0) { // 降低 CPU 压力
          Thread.onSpinWait();
        }
        continue;
      }

      // 自旋失败，降低下次自旋次数
      successiveSpins.decrementAndGet();

      // 切换到 park 模式
      if (this.parkedThread.compareAndSet(null, toUnpark)) {
        LockSupport.park();
      }
    }

    this.parkedThread.lazySet(null);
  }

  private int calculateSpinCount() {
    if (MAX_SPIN <= 0) {
      return 0; // 单核系统直接放弃自旋
    }

    int successSpins = successiveSpins.get();
    // 根据历史成功率动态调整自旋次数
    return Math.min(MAX_SPIN, Math.max(0,
            SPIN_THRESHOLD + (successSpins * 10)));
  }

}
