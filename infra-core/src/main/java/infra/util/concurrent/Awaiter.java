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
 * Interface for thread waiting operations with at most one pending notification.
 *
 * <p>This interface is based on {@link LockSupport} to implement thread waiting and resuming mechanism,
 * which can be used in scenarios where a thread needs to wait for a specific condition to be met.</p>
 * <p>Notifications are retained until consumed by {@link #await()}, even if no thread
 * is waiting when {@link #resume()} is called. Multiple pending notifications are
 * coalesced into a single notification.</p>
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
   * Consumes a pending notification, waiting for {@link #resume()} if necessary.
   *
   * <p>If a notification is already pending, this method consumes it and returns
   * without waiting. Otherwise, it waits until a notification is available.
   * After the notification is consumed, a subsequent call waits for another notification.</p>
   * <p>This method is not interruptible: interrupting the current thread does not
   * cause it to return, and its interrupt status is preserved.</p>
   * <p>Callers should recheck the condition they are waiting for after this method returns.</p>
   *
   * @see LockSupport#park()
   */
  void await();

  /**
   * Publishes a notification and resumes the waiting thread, if any.
   *
   * <p>If a thread is waiting via {@link #await()}, this method unparks it.
   * Otherwise, the notification is retained for the next call to {@code await()}.</p>
   * <p>At most one notification may be pending. Further calls before that
   * notification is consumed are coalesced and do not accumulate additional notifications.</p>
   *
   * @see LockSupport#unpark(Thread)
   */
  void resume();

}
