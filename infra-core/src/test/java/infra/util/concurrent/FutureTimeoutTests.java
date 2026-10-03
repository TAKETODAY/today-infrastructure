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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class FutureTimeoutTests {

  @Test
  void durationTimeoutSaturatesAtNanosecondLimits() {
    assertScheduledDelay(Duration.ofSeconds(Long.MAX_VALUE), Long.MAX_VALUE);
    assertScheduledDelay(Duration.ofSeconds(Long.MIN_VALUE), Long.MIN_VALUE);
    assertScheduledDelay(Duration.ofNanos(Long.MAX_VALUE), Long.MAX_VALUE);
    assertScheduledDelay(Duration.ofNanos(Long.MIN_VALUE), Long.MIN_VALUE);
    assertScheduledDelay(Duration.ofNanos(Long.MAX_VALUE).plusNanos(1), Long.MAX_VALUE);
    assertScheduledDelay(Duration.ofNanos(Long.MIN_VALUE).minusNanos(1), Long.MIN_VALUE);
    assertScheduledDelay(Duration.ofNanos(1501), 1501);
    assertScheduledDelay(Duration.ofNanos(-1501), -1501);
    assertScheduledDelay(Duration.ZERO, 0);
  }

  private static void assertScheduledDelay(Duration duration, long expectedDelay) {
    int[] scheduled = { 0 };
    Scheduler scheduler = new Scheduler() {
      @Override
      public void execute(Runnable command) {
        command.run();
      }

      @Override
      public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        assertThat(delay).isEqualTo(expectedDelay);
        assertThat(unit).isSameAs(TimeUnit.NANOSECONDS);
        scheduled[0]++;
        return mock(ScheduledFuture.class);
      }
    };
    Future.forPromise().timeout(duration, scheduler);
    Future.forPromise().timeout(duration, scheduler, promise -> promise.trySuccess(null));
    assertThat(scheduled[0]).isEqualTo(2);
  }

  @Test
  void timeoutFormattingPreservesPrecisionAndNormalizesUnits() {
    assertThat(Futures.formatTimeout(500_000_000, TimeUnit.NANOSECONDS)).isEqualTo("500 milliseconds");
    assertThat(Futures.formatTimeout(500, TimeUnit.MILLISECONDS)).isEqualTo("500 milliseconds");
    assertThat(Futures.formatTimeout(1500, TimeUnit.MILLISECONDS)).isEqualTo("1.5 seconds");
    assertThat(Futures.formatTimeout(1_000_000_001, TimeUnit.NANOSECONDS)).isEqualTo("1.000000001 seconds");
    assertThat(Futures.formatTimeout(1501, TimeUnit.NANOSECONDS)).isEqualTo("1.501 microseconds");
    assertThat(Futures.formatTimeout(1, TimeUnit.SECONDS)).isEqualTo("1 second");
    assertThat(Futures.formatTimeout(0, TimeUnit.SECONDS)).isEqualTo("0 nanoseconds");
    assertThat(Futures.formatTimeout(-1500, TimeUnit.MILLISECONDS)).isEqualTo("-1.5 seconds");
    assertThat(Futures.formatTimeout(Long.MIN_VALUE, TimeUnit.NANOSECONDS))
            .isEqualTo("-9223372036.854775808 seconds");
    assertThat(Futures.formatTimeout(Long.MAX_VALUE, TimeUnit.DAYS))
            .isEqualTo("796899343984252629724800 seconds");
  }

  @Test
  void listenerExceptionFailsReturnedFuture() throws Exception {
    Promise<String> source = Future.forPromise();
    IOException failure = new IOException("fallback failed");
    Future<String> result = source.timeout(Duration.ZERO, promise -> {
      throw failure;
    });

    assertThat(result.await(5, TimeUnit.SECONDS)).isTrue();
    assertThat(result.getCause()).isSameAs(failure);
    assertThat(source.await(5, TimeUnit.SECONDS)).isTrue();
    assertThat(source.isCancelled()).isTrue();
  }

  @Test
  void blockingListenerDoesNotBlockTimerThread() throws Exception {
    ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();
    ExecutorService callbacks = Executors.newSingleThreadExecutor();
    CountDownLatch started = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    try {
      Promise<String> source = Future.forPromise();
      Future<String> result = source.timeout(Duration.ZERO, scheduler(callbacks, timer), promise -> {
        started.countDown();
        if (!release.await(5, TimeUnit.SECONDS)) {
          throw new IllegalStateException("Listener was not released");
        }
        promise.setSuccess("fallback");
      });

      assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
      assertThat(timer.schedule(() -> "timer available", 0, TimeUnit.NANOSECONDS)
              .get(5, TimeUnit.SECONDS)).isEqualTo("timer available");
      assertThat(result.isDone()).isFalse();
      release.countDown();
      assertThat(result.get(5, TimeUnit.SECONDS)).isEqualTo("fallback");
      assertThat(source.await(5, TimeUnit.SECONDS)).isTrue();
      assertThat(source.isCancelled()).isTrue();
    }
    finally {
      release.countDown();
      callbacks.shutdownNow();
      timer.shutdownNow();
    }
  }

  @Test
  void rejectedListenerSubmissionFailsReturnedFuture() throws Exception {
    ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();
    ExecutorService callbacks = Executors.newSingleThreadExecutor();
    callbacks.shutdown();
    try {
      Promise<String> source = Future.forPromise();
      Future<String> result = source.timeout(Duration.ZERO, scheduler(callbacks, timer),
              promise -> promise.setSuccess("fallback"));

      assertThat(result.await(5, TimeUnit.SECONDS)).isTrue();
      assertThat(result.getCause()).isInstanceOf(RejectedExecutionException.class);
      assertThat(source.await(5, TimeUnit.SECONDS)).isTrue();
      assertThat(source.isCancelled()).isTrue();
    }
    finally {
      timer.shutdownNow();
    }
  }

  private static Scheduler scheduler(ExecutorService callbacks, ScheduledExecutorService timer) {
    return new Scheduler() {
      @Override
      public void execute(Runnable command) {
        callbacks.execute(command);
      }

      @Override
      public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return timer.schedule(command, delay, unit);
      }
    };
  }

}
