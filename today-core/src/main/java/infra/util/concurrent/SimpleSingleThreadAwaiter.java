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

package infra.util.concurrent;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

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

  protected static final Object READY = new Object();

  protected final AtomicReference<Object> parkedThread = new AtomicReference<>();

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

}
