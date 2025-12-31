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

import java.util.concurrent.locks.LockSupport;

/**
 * Interface for thread waiting operations, providing basic thread park and unpark functionality.
 *
 * <p>This interface is based on {@link LockSupport} to implement thread waiting and resuming mechanism,
 * which can be used in scenarios where a thread needs to wait for a specific condition to be met.</p>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * Awaiter awaiter = // get Awaiter instance
 * // Wait in one thread
 * awaiter.await();
 *
 * // Resume in another thread
 * awaiter.resume();
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see LockSupport
 * @since 5.0
 */
public interface Awaiter {

  /**
   * Parks the current thread until it is resumed by another thread via {@link #resume()}.
   *
   * <p>This method will cause the current thread to enter a waiting state.
   * The thread will continue to wait until one of the following occurs:</p>
   * <ul>
   *   <li>Some other thread invokes {@link #resume()} for this object</li>
   *   <li>Some other thread interrupts the current thread</li>
   * </ul>
   *
   * @see LockSupport#park()
   */
  void await();

  /**
   * Resumes the waiting thread.
   *
   * <p>If there is a thread waiting (via {@link #await()}), this method will unpark that thread.
   * If no thread is waiting, this call will be ignored.</p>
   *
   * @see LockSupport#unpark(Thread)
   */
  void resume();

}
