/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.util.concurrent;

import java.io.Serial;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import cn.taketoday.lang.Assert;

/**
 * <p>Reentrant lock that can be used in a try-with-resources statement.</p>
 * <p>Typical usage:</p>
 * <pre>
 * try (SmartLock lock = this.lock.lock())
 * {
 *     // Something
 * }
 * </pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/30 12:48
 */
public class SmartLock implements AutoCloseable, Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private final ReentrantLock lock;

  public SmartLock(ReentrantLock lock) {
    Assert.notNull(lock, "ReentrantLock is required");
    this.lock = lock;
  }

  /**
   * <p>Acquires the lock.</p>
   *
   * @return this AutoLock for unlocking
   */
  public SmartLock lock() {
    lock.lock();
    return this;
  }

  /**
   * @return whether this lock is held by the current thread
   * @see ReentrantLock#isHeldByCurrentThread()
   */
  public boolean isHeldByCurrentThread() {
    return lock.isHeldByCurrentThread();
  }

  /**
   * @return a {@link Condition} associated with this lock
   */
  public Condition newCondition() {
    return lock.newCondition();
  }

  // Package-private for testing only.
  boolean isLocked() {
    return lock.isLocked();
  }

  @Override
  public void close() {
    lock.unlock();
  }

  // Static Factory Methods

  public static SmartLock of() {
    return of(new ReentrantLock());
  }

  public static SmartLock of(boolean fair) {
    return of(new ReentrantLock(fair));
  }

  public static SmartLock of(ReentrantLock lock) {
    return new SmartLock(lock);
  }

  public static WithCondition forCondition() {
    return forCondition(new ReentrantLock());
  }

  public static WithCondition forCondition(boolean fair) {
    return forCondition(new ReentrantLock(fair));
  }

  public static WithCondition forCondition(ReentrantLock lock) {
    return new WithCondition(lock);
  }

  /**
   * <p>A reentrant lock with a condition that can be used in a try-with-resources statement.</p>
   * <p>Typical usage:</p>
   * <pre>
   * // Waiting
   * try (SmartLock lock = lock.lock())
   * {
   *     lock.await();
   * }
   *
   * // Signaling
   * try (SmartLock lock = lock.lock())
   * {
   *     lock.signalAll();
   * }
   * </pre>
   */
  public static class WithCondition extends SmartLock {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Condition condition = newCondition();

    public WithCondition(ReentrantLock lock) {
      super(lock);
    }

    @Override
    public WithCondition lock() {
      return (WithCondition) super.lock();
    }

    /**
     * @see Condition#signal()
     */
    public void signal() {
      condition.signal();
    }

    /**
     * @see Condition#signalAll()
     */
    public void signalAll() {
      condition.signalAll();
    }

    /**
     * @throws InterruptedException if the current thread is interrupted
     * @see Condition#await()
     */
    public void await() throws InterruptedException {
      condition.await();
    }

    /**
     * @param time the time to wait
     * @param unit the time unit
     * @return false if the waiting time elapsed
     * @throws InterruptedException if the current thread is interrupted
     * @see Condition#await(long, TimeUnit)
     */
    public boolean await(long time, TimeUnit unit) throws InterruptedException {
      return condition.await(time, unit);
    }

  }
}
