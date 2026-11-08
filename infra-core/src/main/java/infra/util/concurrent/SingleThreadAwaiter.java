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

import infra.util.InfraStrategies;

/**
 * Internal helper for suspending and resuming a single thread.
 *
 * <p>This class is an implementation detail shared across modules and is not
 * intended to be used as public API. It supports at most one waiting thread at
 * a time and keeps at most one pending notification: calling {@link #resume()}
 * before {@link #await()} makes the next {@code await} return immediately.
 *
 * <p>Waiting is not interruptible; an interrupt does not cause {@link #await()}
 * to return, and the interrupt status is preserved.
 *
 * <p>By default, a waiting thread is parked with {@link LockSupport#park()}.
 * Set the {@code infra.awaiter.thread.park-nanos-enabled} system property to
 * {@code true} to use timed parking instead, which avoids the rare case where
 * {@code park()} blocks forever. The timed wait duration is configured through
 * {@code infra.awaiter.thread.park-nanos} in nanoseconds and defaults to three
 * seconds. Both properties are read once when this class is initialized.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public final class SingleThreadAwaiter {

  /**
   * Timed park duration in nanoseconds, configured by
   * {@code infra.awaiter.thread.park-nanos}. Defaults to three seconds.
   */
  private static final long threadParkNanos = InfraStrategies.getLong(
          "infra.awaiter.thread.park-nanos", TimeUnit.SECONDS.toNanos(3));

  /**
   * Whether to use timed parking instead of {@link LockSupport#park()},
   * configured by {@code infra.awaiter.thread.park-nanos-enabled}.
   * Defaults to {@code false}.
   */
  private static final boolean parkNanosEnabled = InfraStrategies.getFlag(
          "infra.awaiter.thread.park-nanos-enabled", false);

  private static final Object READY = new Object();

  private final AtomicReference<@Nullable Object> parkedThread = new AtomicReference<>();

  /**
   * Consumes a pending notification, parking the current thread if necessary.
   */
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
        if (parkNanosEnabled) {
          // LockSupport.park() 在极小的情况下会出现永远阻塞的状态
          LockSupport.parkNanos(threadParkNanos);
        }
        else {
          LockSupport.park();
        }
        // we don't just break here because park() can wake up spuriously
        // if we got a proper resume, get() == READY and the loop will quit above
      }
    }
    // clear the resume indicator so that the next await call will park without a resume()
    parkedThread.lazySet(null);
  }

  /**
   * Publishes a notification and resumes the waiting thread, if any.
   */
  public void resume() {
    Object last = parkedThread.getAndSet(READY);
    if (last != READY) {
      LockSupport.unpark((Thread) last);
    }
  }

}
