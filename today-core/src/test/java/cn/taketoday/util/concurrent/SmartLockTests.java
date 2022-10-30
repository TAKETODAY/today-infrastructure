/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/30 12:49
 */
class SmartLockTests {

  @Test
  void notNull() {
    assertThatThrownBy(() ->
            SmartLock.of(null))
            .hasMessage("ReentrantLock is required")
            .isInstanceOf(IllegalArgumentException.class);

    assertThatThrownBy(() ->
            SmartLock.forCondition(null))
            .hasMessage("ReentrantLock is required")
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void testLocked() {
    SmartLock lock = SmartLock.of();
    assertFalse(lock.isLocked());

    try (SmartLock l = lock.lock()) {
      assertTrue(lock.isLocked());
    }
    finally {
      assertFalse(lock.isLocked());
    }

    assertFalse(lock.isLocked());
  }

  @Test
  public void testLockedException() {
    SmartLock lock = SmartLock.of();
    assertFalse(lock.isLocked());

    try (SmartLock l = lock.lock()) {
      assertTrue(lock.isLocked());
      throw new Exception();
    }
    catch (Exception e) {
      assertFalse(lock.isLocked());
    }
    finally {
      assertFalse(lock.isLocked());
    }

    assertFalse(lock.isLocked());
  }

  @Test
  public void testContend() throws Exception {
    SmartLock lock = SmartLock.of();

    final CountDownLatch held0 = new CountDownLatch(1);
    final CountDownLatch hold0 = new CountDownLatch(1);

    Thread thread0 = new Thread(() ->
    {
      try (SmartLock l = lock.lock()) {
        held0.countDown();
        hold0.await();
      }
      catch (InterruptedException e) {
        e.printStackTrace();
      }
    });
    thread0.start();
    held0.await();

    assertTrue(lock.isLocked());

    final CountDownLatch held1 = new CountDownLatch(1);
    final CountDownLatch hold1 = new CountDownLatch(1);
    Thread thread1 = new Thread(() ->
    {
      try (SmartLock l = lock.lock()) {
        held1.countDown();
        hold1.await();
      }
      catch (InterruptedException e) {
        e.printStackTrace();
      }
    });
    thread1.start();
    // thread1 will be spinning here
    assertFalse(held1.await(100, TimeUnit.MILLISECONDS));

    // Let thread0 complete
    hold0.countDown();
    thread0.join();

    // thread1 can progress
    held1.await();

    // let thread1 complete
    hold1.countDown();
    thread1.join();

    assertFalse(lock.isLocked());
  }
}