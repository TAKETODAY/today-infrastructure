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
