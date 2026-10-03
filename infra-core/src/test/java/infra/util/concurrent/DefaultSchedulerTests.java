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
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import infra.util.InfraStrategies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;

@ResourceLock(DefaultScheduler.PoolSize)
class DefaultSchedulerTests {

  private static final DefaultScheduler scheduler = new DefaultScheduler();

  @ParameterizedTest
  @ValueSource(ints = { 0, -1 })
  void rejectsNonPositivePoolSize(int poolSize) {
    String previous = InfraStrategies.getProperty(DefaultScheduler.PoolSize);
    try {
      InfraStrategies.setProperty(DefaultScheduler.PoolSize, Integer.toString(poolSize));
      assertThatIllegalArgumentException().isThrownBy(DefaultScheduler::new)
              .withMessage("Scheduler pool size must be greater than zero: " + poolSize);
    }
    finally {
      InfraStrategies.setProperty(DefaultScheduler.PoolSize, previous);
    }
  }

  @Test
  void lookupCreatesDefaultInstanceWhenNoStrategyIsAvailable() {
    try (var strategies = mockStatic(InfraStrategies.class)) {
      strategies.when(() -> InfraStrategies.getInt(DefaultScheduler.PoolSize, 1)).thenReturn(1);
      Scheduler first = Scheduler.lookup();
      assertThat(first).isInstanceOf(DefaultScheduler.class);
      assertThat(Scheduler.lookup()).isInstanceOf(DefaultScheduler.class).isNotSameAs(first);
    }
  }

  @Test
  void scheduledFutureTracksTaskCompletion() throws Exception {
    CountDownLatch started = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    var future = scheduler.schedule(() -> {
      started.countDown();
      try {
        if (!release.await(5, TimeUnit.SECONDS)) {
          throw new IllegalStateException("Task was not released");
        }
      }
      catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(ex);
      }
    }, 0, TimeUnit.NANOSECONDS);

    try {
      assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
      assertThat(future.isDone()).isFalse();
      release.countDown();
      assertThat(future.get(5, TimeUnit.SECONDS)).isNull();
    }
    finally {
      release.countDown();
      future.cancel(true);
    }
  }

  @Test
  void scheduledFutureReportsTaskFailure() {
    IllegalStateException failure = new IllegalStateException("Task failed");
    var future = scheduler.schedule(() -> {
      throw failure;
    }, 0, TimeUnit.NANOSECONDS);

    assertThatThrownBy(() -> future.get(5, TimeUnit.SECONDS))
            .isInstanceOf(ExecutionException.class).hasCause(failure);
  }

  @Test
  void cancellationPreventsDelayedTaskExecution() throws Exception {
    AtomicBoolean executed = new AtomicBoolean();
    var future = scheduler.schedule(() -> executed.set(true), 1, TimeUnit.DAYS);

    assertThat(future.cancel(false)).isTrue();
    assertThat(future.isCancelled()).isTrue();
    scheduler.schedule(() -> { }, 0, TimeUnit.NANOSECONDS).get(5, TimeUnit.SECONDS);
    assertThat(executed).isFalse();
  }

}
