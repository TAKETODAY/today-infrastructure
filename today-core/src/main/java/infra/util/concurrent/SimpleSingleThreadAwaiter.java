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

package infra.util.concurrent;

import org.jspecify.annotations.Nullable;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import infra.lang.TodayStrategies;

/**
 * A simple implementation of {@link Awaiter} that supports only one thread waiting at a time.
 *
 * <p>This implementation uses {@link AtomicReference} to ensure thread-safety and
 * {@link LockSupport} for parking/unparking threads. If a second thread attempts to
 * await while another thread is already waiting, an {@link IllegalStateException} will
 * be thrown.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * Awaiter awaiter = new SimpleSingleThreadAwaiter();
 *
 * // Thread 1
 * awaiter.await(); // waits here
 *
 * // Thread 2
 * awaiter.resume(); // resumes Thread 1
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public class SimpleSingleThreadAwaiter implements Awaiter {

  protected static final long threadParkNanos = TodayStrategies.getLong("awaiter.thread.pack-nanos", TimeUnit.SECONDS.toNanos(3));

  protected static final Object READY = new Object();

  protected final AtomicReference<@Nullable Object> parkedThread = new AtomicReference<>();

  private final boolean parkNanosEnabled;

  public SimpleSingleThreadAwaiter() {
    this(false);
  }

  public SimpleSingleThreadAwaiter(boolean parkNanosEnabled) {
    this.parkNanosEnabled = parkNanosEnabled;
  }

  /**
   * 唤醒等待的线程
   *
   * @see LockSupport#unpark(Thread)
   */
  @Override
  public void resume() {
    Object last = parkedThread.getAndSet(READY);
    if (last != READY) {
      LockSupport.unpark((Thread) last);
    }
  }

  @Override
  public void await() {
    Thread currentThread = Thread.currentThread();

    for (; ; ) {
      Object current = parkedThread.get();
      if (current == READY) {
        break;
      }

      if (current != null && current != currentThread) {
        throw new IllegalStateException("Only one (Virtual)Thread can await!");
      }

      if (parkedThread.compareAndSet(null, currentThread)) {
        parkThread();
        // we don't just break here because park() can wake up spuriously
        // if we got a proper resume, get() == READY and the loop will quit above
      }
    }
    // clear the resume indicator so that the next await call will park without a resume()
    parkedThread.lazySet(null);
  }

  protected final void parkThread() {
    if (parkNanosEnabled) {
      // LockSupport.park() 在极小的情况下会出现永远阻塞的状态
      LockSupport.parkNanos(threadParkNanos);
    }
    else {
      LockSupport.park();
    }
  }

}
