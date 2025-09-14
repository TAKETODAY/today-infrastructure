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

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

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

}
