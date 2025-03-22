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

package infra.util;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static infra.util.ConcurrencyThrottleSupport.NO_CONCURRENCY;
import static infra.util.ConcurrencyThrottleSupport.UNBOUNDED_CONCURRENCY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/22 19:22
 */
class ConcurrencyThrottleSupportTests {

  @Test
  void noConcurrencyPreventsAccess() {
    var support = new ConcurrencyThrottleSupportImpl(NO_CONCURRENCY);
    assertThatThrownBy(support::beforeAccess)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Currently no invocations allowed");
  }

  @Test
  void unboundedConcurrencyAllowsUnlimitedAccess() {
    var support = new ConcurrencyThrottleSupportImpl(UNBOUNDED_CONCURRENCY);
    for (int i = 0; i < 1000; i++) {
      support.beforeAccess();
    }
    for (int i = 0; i < 1000; i++) {
      support.afterAccess();
    }
  }

  @Test
  void afterAccessReleasesSlot() {
    var support = new ConcurrencyThrottleSupportImpl(1);

    support.beforeAccess();
    support.afterAccess();
    support.beforeAccess(); // Should not block
  }

  @Test
  void changingLimitAtRuntime() {
    var support = new ConcurrencyThrottleSupportImpl(1);
    support.beforeAccess();

    support.setConcurrencyLimit(2);
    support.beforeAccess(); // Should not block with new limit

    support.afterAccess();
    support.afterAccess();
  }

  @Test
  void throttleActiveStatus() {
    var support = new ConcurrencyThrottleSupportImpl(1);
    assertThat(support.isThrottleActive()).isTrue();

    support.setConcurrencyLimit(UNBOUNDED_CONCURRENCY);
    assertThat(support.isThrottleActive()).isFalse();

    support.setConcurrencyLimit(NO_CONCURRENCY);
    assertThat(support.isThrottleActive()).isTrue();
  }

  @Test
  void multipleThreadsShouldBlockUntilSlotAvailable() throws InterruptedException {
    var support = new ConcurrencyThrottleSupportImpl(1);
    var latch = new CountDownLatch(1);

    support.beforeAccess();

    Thread t = new Thread(() -> {
      support.beforeAccess();
      latch.countDown();
    });
    t.start();

    assertThat(latch.await(100, TimeUnit.MILLISECONDS)).isFalse();
    support.afterAccess();
    assertThat(latch.await(1000, TimeUnit.MILLISECONDS)).isTrue();
  }

  @Test
  void concurrentAccessWithinLimit() throws InterruptedException {
    var support = new ConcurrencyThrottleSupportImpl(3);
    var latch = new CountDownLatch(3);

    for (int i = 0; i < 3; i++) {
      new Thread(() -> {
        support.beforeAccess();
        latch.countDown();
      }).start();
    }

    assertThat(latch.await(1000, TimeUnit.MILLISECONDS)).isTrue();
  }

  @Test
  void negativeThrottleLimitShouldBeUnbounded() {
    var support = new ConcurrencyThrottleSupportImpl(-2);
    assertThat(support.isThrottleActive()).isFalse();
    for (int i = 0; i < 100; i++) {
      support.beforeAccess();
    }
  }

  @Test
  void afterAccessWithoutBeforeAccessShouldNotThrow() {
    var support = new ConcurrencyThrottleSupportImpl(1);
    assertThatCode(support::afterAccess).doesNotThrowAnyException();
  }

  @Test
  void concurrencyCountShouldNotGoBelowZero() {
    var support = new ConcurrencyThrottleSupportImpl(1);
    support.beforeAccess();
    support.afterAccess();
    support.afterAccess();
    support.beforeAccess(); // Should still work
  }

  @Test
  void concurrencyLimitRespected() {
    var support = new ConcurrencyThrottleSupportImpl(2);
    var latch = new CountDownLatch(1);
    var exceptionHolder = new AtomicReference<Exception>();

    support.beforeAccess();
    support.beforeAccess();

    Thread t = new Thread(() -> {
      try {
        support.beforeAccess();
      }
      catch (Exception e) {
        exceptionHolder.set(e);
      }
      finally {
        latch.countDown();
      }
    });
    t.start();

    try {
      Thread.sleep(100);
      t.interrupt();
      latch.await(1000, TimeUnit.MILLISECONDS);
    }
    catch (InterruptedException ignored) {
    }

    assertThat(exceptionHolder.get())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("interrupted while waiting");
  }

  @Test
  void interruptedThreadShouldNotAffectOtherThreads() throws InterruptedException {
    var support = new ConcurrencyThrottleSupportImpl(2);
    var latch = new CountDownLatch(1);

    support.beforeAccess();
    Thread t1 = new Thread(() -> {
      try {
        support.beforeAccess();
        Thread.currentThread().interrupt();
        support.afterAccess();
      }
      finally {
        latch.countDown();
      }
    });
    t1.start();
    latch.await();

    Thread t2 = new Thread(() -> support.beforeAccess());
    t2.start();
    t2.join(1000);
    assertThat(t2.isAlive()).isFalse();
  }

  @Test
  void serializedAccessShouldWork() throws InterruptedException {
    var support = new ConcurrencyThrottleSupportImpl(1);
    var counter = new AtomicInteger();
    var latch = new CountDownLatch(3);

    for (int i = 0; i < 3; i++) {
      new Thread(() -> {
        support.beforeAccess();
        counter.incrementAndGet();
        try {
          Thread.sleep(10);
        }
        catch (InterruptedException ignored) {
        }
        support.afterAccess();
        latch.countDown();
      }).start();
    }

    assertThat(latch.await(1000, TimeUnit.MILLISECONDS)).isTrue();
    assertThat(counter.get()).isEqualTo(3);
  }

  private static class ConcurrencyThrottleSupportImpl extends ConcurrencyThrottleSupport {
    ConcurrencyThrottleSupportImpl(int concurrencyLimit) {
      setConcurrencyLimit(concurrencyLimit);
    }
  }

}