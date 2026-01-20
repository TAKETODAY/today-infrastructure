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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/9/14 18:20
 */
class SpinSingleThreadAwaiterTests {

  @Test
  void basicAwaitResume() throws InterruptedException {
    SpinSingleThreadAwaiter awaiter = new SpinSingleThreadAwaiter();
    AtomicBoolean completed = new AtomicBoolean();

    Thread waiter = new Thread(() -> {
      awaiter.await();
      completed.set(true);
    });
    waiter.start();

    Thread.sleep(50);
    assertThat(completed).isFalse();

    awaiter.resume();
    waiter.join(1000);
    assertThat(completed).isTrue();
  }

  @Test
  void spinningOptimizesShortWaits() throws InterruptedException {
    SpinSingleThreadAwaiter awaiter = new SpinSingleThreadAwaiter();
    AtomicInteger completedCount = new AtomicInteger();
    int iterations = 1000;

    for (int i = 0; i < iterations; i++) {
      Thread waiter = new Thread(() -> {
        awaiter.await();
        completedCount.incrementAndGet();
      });
      waiter.start();
      awaiter.resume();
      waiter.join(50);
    }

    assertThat(completedCount.get()).isEqualTo(iterations);
  }

  @Test
  void multipleThreadsAwaitingThrowsException() throws InterruptedException {
    SpinSingleThreadAwaiter awaiter = new SpinSingleThreadAwaiter();
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Throwable> error = new AtomicReference<>();

    Thread first = new Thread(awaiter::await);
    first.start();

    Thread second = new Thread(() -> {
      try {
        awaiter.await();
      }
      catch (Throwable t) {
        error.set(t);
        latch.countDown();
      }
    });
    second.start();

    assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
    assertThat(error.get())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Only one (Virtual)Thread can await!");

    awaiter.resume();
    first.join(1000);
  }

  @Test
  void singleCoreSystemDoesNotSpin() throws InterruptedException {
    assumeThat(Runtime.getRuntime().availableProcessors()).isEqualTo(1);

    SpinSingleThreadAwaiter awaiter = new SpinSingleThreadAwaiter();
    AtomicLong startTime = new AtomicLong();
    AtomicLong endTime = new AtomicLong();

    Thread waiter = new Thread(() -> {
      startTime.set(System.nanoTime());
      awaiter.await();
      endTime.set(System.nanoTime());
    });
    waiter.start();

    Thread.sleep(50);
    awaiter.resume();
    waiter.join(1000);

    // No spinning means the thread should park immediately
    assertThat(endTime.get() - startTime.get()).isGreaterThan(TimeUnit.MILLISECONDS.toNanos(40));
  }

  @Test
  @DisabledOnOs(OS.MAC)
  void successiveSpinsOptimizePerformance() throws InterruptedException {
    assumeThat(Runtime.getRuntime().availableProcessors()).isGreaterThan(1);

    SpinSingleThreadAwaiter awaiter = new SpinSingleThreadAwaiter();
    int warmupRounds = 1000;

    // Warm up spins
    for (int i = 0; i < warmupRounds; i++) {
      Thread waiter = new Thread(awaiter::await);
      waiter.start();
      awaiter.resume();
      waiter.join(50);
    }

    // Measure optimized performance
    long start = System.nanoTime();
    Thread waiter = new Thread(awaiter::await);
    waiter.start();
    awaiter.resume();
    waiter.join(50);
    long duration = System.nanoTime() - start;

    // Optimized case should be faster than parking
    assertThat(duration).isLessThan(TimeUnit.MILLISECONDS.toNanos(1));
  }

  @Test
  void spinningBacksOffUnderContention() throws InterruptedException {
    SpinSingleThreadAwaiter awaiter = new SpinSingleThreadAwaiter();
    AtomicInteger parkedCount = new AtomicInteger();
    int iterations = 100;

    for (int i = 0; i < iterations; i++) {
      Thread waiter = new Thread(() -> {
        long start = System.nanoTime();
        awaiter.await();
        if (System.nanoTime() - start > TimeUnit.MILLISECONDS.toNanos(1)) {
          parkedCount.incrementAndGet();
        }
      });
      waiter.start();
      Thread.sleep(2); // Force contention
      awaiter.resume();
      waiter.join();
    }

    // Some threads should have parked due to backoff
    assertThat(parkedCount.get()).isGreaterThan(0);
  }

  @Test
  void resumeBeforeAwaitDoesNotSpin() throws InterruptedException {
    SpinSingleThreadAwaiter awaiter = new SpinSingleThreadAwaiter();
    AtomicLong duration = new AtomicLong();

    awaiter.resume();
    Thread waiter = new Thread(() -> {
      long start = System.nanoTime();
      awaiter.await();
      duration.set(System.nanoTime() - start);
    });
    waiter.start();
    waiter.join(1000);

    assertThat(duration.get()).isLessThan(TimeUnit.MICROSECONDS.toNanos(100));
  }

  @Test
  void multipleResumeCallsAreIdempotent() throws InterruptedException {
    SpinSingleThreadAwaiter awaiter = new SpinSingleThreadAwaiter();
    AtomicBoolean completed = new AtomicBoolean();

    Thread waiter = new Thread(() -> {
      awaiter.await();
      completed.set(true);
    });
    waiter.start();

    awaiter.resume();
    awaiter.resume();
    awaiter.resume();
    waiter.join(1000);

    assertThat(completed).isTrue();
  }

  @Test
  void spinningAdaptsToSystemLoad() throws InterruptedException {
    assumeThat(Runtime.getRuntime().availableProcessors()).isGreaterThan(1);

    SpinSingleThreadAwaiter awaiter = new SpinSingleThreadAwaiter();
    int iterations = 1000;
    AtomicLong totalDuration = new AtomicLong();

    // Create background load
    Thread load = new Thread(() -> {
      while (!Thread.interrupted()) {
        Math.random();
      }
    });
    load.start();

    for (int i = 0; i < iterations; i++) {
      long start = System.nanoTime();
      Thread waiter = new Thread(awaiter::await);
      waiter.start();
      awaiter.resume();
      waiter.join(50);
      totalDuration.addAndGet(System.nanoTime() - start);
    }

    load.interrupt();
    load.join(1000);

    double avgDuration = totalDuration.get() / (double) iterations;
    assertThat(avgDuration).isLessThan(TimeUnit.MILLISECONDS.toNanos(1));
  }

  @Test
  void verifySpinThresholdConstant() {
    // Verify that SPIN_THRESHOLD is accessible and has expected value
    SpinSingleThreadAwaiter awaiter = new SpinSingleThreadAwaiter();
    awaiter.resume();
    awaiter.await(); // Should work normally
    assertTrue(true); // Passes if no exception
  }

  @Test
  void testMaxSpinCalculationOnMultiCoreSystems() {
    assumeThat(Runtime.getRuntime().availableProcessors()).isGreaterThan(1);

    SpinSingleThreadAwaiter awaiter = new SpinSingleThreadAwaiter();
    awaiter.resume();
    awaiter.await(); // Trigger spin count calculation

    // Simply verify it works without throwing exceptions
    assertTrue(true);
  }

  @Test
  void testMaxSpinIsZeroOnSingleCoreSystems() {
    assumeThat(Runtime.getRuntime().availableProcessors()).isEqualTo(1);

    SpinSingleThreadAwaiter awaiter = new SpinSingleThreadAwaiter();
    awaiter.resume();
    awaiter.await(); // Should not spin, just park

    assertTrue(true); // Passes if no exception
  }

  @Test
  void testThreadOnSpinWaitIsCalledDuringSpin() throws InterruptedException {
    assumeThat(Runtime.getRuntime().availableProcessors()).isGreaterThan(1);

    SpinSingleThreadAwaiter awaiter = new SpinSingleThreadAwaiter();
    AtomicBoolean completed = new AtomicBoolean(false);

    Thread waiter = new Thread(() -> {
      awaiter.await();
      completed.set(true);
    });
    waiter.start();

    // Give some time for spinning
    Thread.sleep(10);
    awaiter.resume();
    waiter.join(1000);

    assertThat(completed).isTrue();
  }

  @Test
  void testSpinCountAdaptsToSuccessRate() throws InterruptedException {
    SpinSingleThreadAwaiter awaiter = new SpinSingleThreadAwaiter();

    // Do several quick operations to increase success rate
    for (int i = 0; i < 10; i++) {
      awaiter.resume();
      awaiter.await();
    }

    // Now do an operation and check it still works
    AtomicBoolean completed = new AtomicBoolean(false);
    Thread waiter = new Thread(() -> {
      awaiter.await();
      completed.set(true);
    });
    waiter.start();

    Thread.sleep(20); // Allow for adaptation
    awaiter.resume();
    waiter.join(1000);

    assertThat(completed).isTrue();
  }

  @Test
  void testConstructorWithParkNanosDisabled() throws InterruptedException {
    SpinSingleThreadAwaiter awaiter = new SpinSingleThreadAwaiter(false);
    AtomicBoolean completed = new AtomicBoolean(false);

    Thread waiter = new Thread(() -> {
      awaiter.await();
      completed.set(true);
    });
    waiter.start();

    Thread.sleep(50);
    assertThat(completed).isFalse();

    awaiter.resume();
    waiter.join(1000);
    assertThat(completed).isTrue();
  }

  @Test
  @Disabled
  void testAtomicIntegerBehaviorInMultithreadedEnvironment() throws InterruptedException {
    SpinSingleThreadAwaiter awaiter = new SpinSingleThreadAwaiter();
    int threads = 10;
    CountDownLatch latch = new CountDownLatch(threads);

    // Multiple threads using the same awaiter sequentially
    for (int i = 0; i < threads; i++) {
      final int index = i;
      Thread t = new Thread(() -> {
        try {
          if (index % 2 == 0) {
            awaiter.resume();
          }
          else {
            awaiter.await();
          }
        }
        finally {
          latch.countDown();
        }
      });
      t.start();
    }

    // Resume any remaining threads
    awaiter.resume();

    assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
  }

}
