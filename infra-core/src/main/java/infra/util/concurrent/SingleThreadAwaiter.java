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

import java.io.InterruptedIOException;
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
 * <p>Waiting is interruptible: if the current thread is interrupted while
 * waiting, {@link #await()} clears and restores the interrupt status and throws
 * {@link InterruptedIOException}, unless a concurrent {@link #resume()} already
 * published a notification for this thread.
 *
 * <p>By default, a waiting thread is parked with {@link LockSupport#park()}.
 * Set the {@code infra.awaiter.thread.park-nanos-enabled} system property to
 * {@code true} to use timed parking instead, which avoids the rare case where
 * {@code park()} blocks forever. The timed wait duration is configured through
 * {@code infra.awaiter.thread.park-nanos} in nanoseconds and defaults to three
 * seconds. Both properties are read once when this class is initialized and
 * only apply to instances created with the default constructor; callers that
 * must wait without a timeout can pass {@code false} explicitly.
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
   * Default timed-parking behavior, configured by
   * {@code infra.awaiter.thread.park-nanos-enabled}. Defaults to {@code false}.
   */
  private static final boolean defaultParkNanosEnabled = InfraStrategies.getFlag(
          "infra.awaiter.thread.park-nanos-enabled", false);

  private static final Object READY = new Object();

  private final AtomicReference<@Nullable Object> parkedThread = new AtomicReference<>();

  private final boolean parkNanosEnabled;

  /**
   * Creates an instance using the configured timed-parking behavior.
   *
   * @see #SingleThreadAwaiter(boolean)
   */
  public SingleThreadAwaiter() {
    this(defaultParkNanosEnabled);
  }

  /**
   * Creates an instance with explicit timed-parking behavior.
   *
   * @param parkNanosEnabled whether to use timed parking instead of
   * {@link LockSupport#park()}
   */
  public SingleThreadAwaiter(boolean parkNanosEnabled) {
    this.parkNanosEnabled = parkNanosEnabled;
  }

  /**
   * Consumes a pending notification, parking the current thread if necessary.
   *
   * @throws InterruptedIOException if the current thread is interrupted while
   * waiting and no notification was published concurrently
   */
  public void await() throws InterruptedIOException {
    Thread currentThread = Thread.currentThread();

    while (true) {
      Object current = parkedThread.get();
      if (current == READY) {
        break;
      }

      if (current != null && current != currentThread) {
        throw new IllegalStateException("Only one (Virtual)Thread can await!");
      }

      if (parkedThread.compareAndSet(null, currentThread)
              || parkedThread.get() == currentThread) {
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
      // Preserve a concurrent READY signal so data is delivered before interruption.
      if (Thread.interrupted()) {
        Thread.currentThread().interrupt();
        if (parkedThread.compareAndSet(currentThread, null)) {
          throw new InterruptedIOException("Interrupted while awaiting data");
        }
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
