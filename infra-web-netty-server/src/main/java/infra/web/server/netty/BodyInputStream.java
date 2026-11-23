/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.server.netty;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;

import infra.util.concurrent.SingleThreadAwaiter;
import io.netty.buffer.ByteBuf;

/**
 * A blocking {@link InputStream} for a single application reader, fed with
 * request-body buffers by the Netty event loop.
 * <p>In automatic read mode, unread bytes at or above the high-water mark
 * pause transport reads. Reads resume at or below the low-water mark.
 * In manual read mode, the reader requests more data when the buffer is empty.
 * The byte and queue limits are enforced in both modes.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class BodyInputStream extends InputStream {

  private static final VarHandle WORK_AMOUNT;

  private static final VarHandle STATE;

  private static final int READING = 1 << 0;
  private static final int CLOSED = 1 << 1;
  private static final int CANCELLED = 1 << 2;
  private static final int DONE = 1 << 3;
  private static final int PAUSED = 1 << 4;

  private final SingleThreadAwaiter awaiter = new SingleThreadAwaiter();

  private final ArrayBlockingQueue<ByteBuf> queue;

  private final @Nullable RequestBodyReadController readController;

  private final NettyRequestConfig.BufferLimits limits;

  private volatile long bufferedBytes;

  private int workAmount;

  private int state;

  private @Nullable ByteBuf available;

  private @Nullable IOException error;

  BodyInputStream() {
    this(128);
  }

  /**
   * Create a stream with a bounded queue and no transport read control.
   *
   * @param capacity the maximum number of buffers that can be queued
   */
  BodyInputStream(int capacity) {
    this.queue = new ArrayBlockingQueue<>(capacity);
    this.limits = new NettyRequestConfig.BufferLimits(Long.MAX_VALUE, 0, Long.MAX_VALUE, capacity);
    this.readController = null;
  }

  /**
   * Create a stream using the shared request configuration limits.
   *
   * @param limits the byte watermarks and hard buffer limits
   * @param readController the controller for this exchange, if present
   */
  BodyInputStream(NettyRequestConfig.BufferLimits limits,
          @Nullable RequestBodyReadController readController) {
    this.queue = new ArrayBlockingQueue<>(limits.maxBufferedChunks());
    this.limits = limits;
    this.readController = readController;
  }

  /**
   * Take ownership of an incoming buffer. Release it immediately if the
   * stream cannot accept it; otherwise release it after consumption or cleanup.
   *
   * @param buffer the incoming request-body buffer
   */
  public void onDataReceived(ByteBuf buffer) {
    boolean accepted;
    boolean limitExceeded = false;
    boolean wakeReader = false;
    int readableBytes = buffer.readableBytes();

    synchronized(this) {
      int state = getState();
      if ((state & (DONE | CLOSED | CANCELLED)) != 0) {
        discard(buffer);
        return;
      }
      accepted = (state & (DONE | CLOSED | CANCELLED)) == 0
              && bufferedBytes <= limits.maxBufferedBytes() - readableBytes
              && queue.offer(buffer);
      if (accepted) {
        bufferedBytes += readableBytes;
        updateReadPressure();
        int previousWorkState = addWork();
        if (previousWorkState == 0) {
          resume();
        }
      }
      else if ((state & (DONE | CLOSED | CANCELLED)) == 0) {
        limitExceeded = queue.remainingCapacity() == 0
                || bufferedBytes > limits.maxBufferedBytes() - readableBytes;
        if (limitExceeded) {
          this.error = readController == null
                  ? new IOException("Buffer overflow")
                  : new RequestBodyBufferLimitExceededException();
          updateState(DONE, 0);
          wakeReader = addWork() == 0;
        }
      }
    }

    if (!accepted) {
      discard(buffer);
      if (limitExceeded) {
        if (readController != null) {
          readController.abort();
        }
        if (wakeReader) {
          resume();
        }
      }
      else if (readController == null && (getState() & (DONE | CLOSED | CANCELLED)) == 0) {
        this.error = new IOException("Buffer overflow");
        updateState(DONE, 0);
        if (addWork() == 0) {
          resume();
        }
      }
    }
  }

  public void onError(IOException io) {
    synchronized(this) {
      int state = getState();
      if ((state & DONE) != 0) {
        return;
      }
      this.error = io;
      updateState(DONE, 0);
      if (addWork() == 0) {
        resume();
      }
    }
  }

  public void onComplete() {
    synchronized(this) {
      int state = getState();
      if ((state & DONE) != 0) {
        return;
      }

      updateState(DONE, 0);

      if (readController != null) {
        readController.bodyComplete();
      }

      if (addWork() == 0) {
        resume();
      }
    }
  }

  int addWork() {
    for (; ; ) {
      int produced = this.workAmount;

      if (produced == Integer.MIN_VALUE) {
        return Integer.MIN_VALUE;
      }

      int nextProduced = (produced == Integer.MAX_VALUE ? 1 : produced + 1);

      if (WORK_AMOUNT.weakCompareAndSetRelease(this, produced, nextProduced)) {
        return produced;
      }
    }
  }

  protected void resume() {
    awaiter.resume();
  }

  /**
   * Return the unread byte count across the queue and the current read buffer.
   *
   * @return currently buffered, unread bytes
   */
  final long bufferedBytes() {
    return bufferedBytes;
  }

  /* InputStream implementation */

  @Override
  public int read() throws IOException {
    if (!trySetState(READING, READING)) {
      if ((getState() & CLOSED) != 0) {
        return -1;
      }
      throw new IOException("Concurrent access is not allowed");
    }

    try {
      ByteBuf next = getNextOrAwait();

      if (next == null) {
        int state = getState();
        if ((state & DONE) != 0) {
          updateState(CLOSED, 0);
          cleanAndFinalize();
          if (this.error == null) {
            return -1;
          }
          else {
            throw this.error;
          }
        }
        else if ((state & CLOSED) != 0) {
          cleanAndFinalize();
          return -1;
        }
        else {
          return -1;
        }
      }

      int value = next.readByte() & 0xFF;
      consume(1);
      return value;
    }
    catch (Throwable ex) {
      throw readFailed(ex);
    }
    finally {
      updateState(0, READING);
    }
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    Objects.checkFromIndexSize(off, len, b.length);
    if (len == 0) {
      return 0;
    }

    if (!trySetState(READING, READING)) {
      if ((getState() & CLOSED) != 0) {
        return -1;
      }
      throw new IOException("Concurrent access is not allowed");
    }

    try {
      for (int j = 0; j < len; ) {
        ByteBuf next = getNextOrAwait();

        if (next == null) {
          int state = getState();
          if ((state & DONE) != 0) {
            cleanAndFinalize();
            if (this.error == null) {
              updateState(CLOSED, 0);
              return j == 0 ? -1 : j;
            }
            else {
              if (j == 0) {
                updateState(CLOSED, 0);
                throw this.error;
              }
              return j;
            }
          }
          else if ((state & CLOSED) != 0) {
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
        int consumed = next.readerIndex() - initialReadPosition;
        j += consumed;
        consume(consumed);
      }

      return len;
    }
    catch (Throwable ex) {
      throw readFailed(ex);
    }
    finally {
      updateState(0, READING);
    }
  }

  private IOException readFailed(Throwable ex) {
    updateState(CLOSED, 0);
    cancel();
    cleanAndFinalize();
    if (ex instanceof IOException e) {
      return e;
    }
    return new IOException("Read failed", ex);
  }

  private @Nullable ByteBuf getNextOrAwait() throws IOException {
    if (this.available == null || this.available.readableBytes() == 0) {
      discardAvailable();

      int actualWorkAmount = (int) WORK_AMOUNT.getAcquire(this);
      for (; ; ) {
        int state = getState();
        if ((state & CLOSED) != 0 || (state & CANCELLED) != 0) {
          return null;
        }

        boolean isDone = (state & DONE) != 0;
        ByteBuf buffer;
        synchronized(this) {
          buffer = queue.poll();
        }
        if (buffer != null) {
          this.available = buffer;
          break;
        }

        if (isDone) {
          return null;
        }

        actualWorkAmount = (int) WORK_AMOUNT.getAndAdd(this, -actualWorkAmount) - actualWorkAmount;
        if (actualWorkAmount == 0) {
          requestNext();
          awaiter.await();
        }
      }
    }

    return this.available;
  }

  private void cleanAndFinalize() {
    synchronized(this) {
      discardAvailable();

      for (; ; ) {
        int currentWorkAmount = workAmount;
        ByteBuf value;
        while ((value = queue.poll()) != null) {
          bufferedBytes -= value.readableBytes();
          discard(value);
        }

        if (WORK_AMOUNT.weakCompareAndSetPlain(this, currentWorkAmount, Integer.MIN_VALUE)) {
          break;
        }
      }
      bufferedBytes = 0;
    }
  }

  @Override
  public void close() {
    if (!trySetState(CLOSED, CLOSED)) {
      return;
    }

    if ((getState() & READING) != 0) {
      if (addWork() == 0) {
        resume();
      }
      return;
    }

    cancel();
    cleanAndFinalize();
  }

  private void cancel() {
    updateState(CANCELLED, 0);
  }

  /**
   * Atomically set {@code setBits} if none of the {@code guardBits} are set.
   */
  private boolean trySetState(int setBits, int guardBits) {
    int current = getState();
    while ((current & guardBits) == 0) {
      if (STATE.compareAndSet(this, current, current | setBits)) {
        return true;
      }
      current = getState();
    }
    return false;
  }

  /**
   * Atomically set {@code setBits} and clear {@code clearBits}.
   */
  private void updateState(int setBits, int clearBits) {
    int current = getState();
    while (!STATE.compareAndSet(this, current, (current | setBits) & ~clearBits)) {
      current = getState();
    }
  }

  private int getState() {
    return (int) STATE.getAcquire(this);
  }

  private void discard(@Nullable ByteBuf buffer) {
    if (buffer != null) {
      buffer.release();
    }
  }

  protected void requestNext() {
    if (readController != null) {
      readController.requestRead();
    }
  }

  private void consume(int bytes) {
    if (bytes == 0) {
      return;
    }
    synchronized(this) {
      bufferedBytes -= bytes;
      updateReadPressure();
    }
  }

  private void discardAvailable() {
    ByteBuf value = this.available;
    this.available = null;
    if (value != null) {
      synchronized(this) {
        bufferedBytes -= value.readableBytes();
      }
      discard(value);
      synchronized(this) {
        updateReadPressure();
      }
    }
  }

  private void updateReadPressure() {
    if (readController == null || limits.highWaterMark() == Long.MAX_VALUE) {
      return;
    }
    int state = getState();
    if ((state & PAUSED) == 0 && bufferedBytes >= limits.highWaterMark()) {
      updateState(PAUSED, 0);
      readController.pause();
    }
    else if ((state & PAUSED) != 0 && bufferedBytes <= limits.lowWaterMark()) {
      updateState(0, PAUSED);
      readController.resume();
    }
  }

  static {
    try {
      MethodHandles.Lookup lookup = MethodHandles.lookup();
      WORK_AMOUNT = lookup.findVarHandle(BodyInputStream.class, "workAmount", int.class);
      STATE = lookup.findVarHandle(BodyInputStream.class, "state", int.class);
    }
    catch (Exception e) {
      throw new ExceptionInInitializerError(e);
    }
  }

}
