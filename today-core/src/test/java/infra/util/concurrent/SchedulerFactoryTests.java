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

import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/3/23 14:04
 */
class SchedulerFactoryTests {

  @Test
  void lookup() {
    assertThat(Scheduler.lookup()).isInstanceOf(MyExecutor.class);
    assertThat(Future.defaultScheduler).isInstanceOf(MyExecutor.class);
  }

  static class ExecutorFactory implements SchedulerFactory {

    @Override
    public Scheduler create() {
      return new MyExecutor();
    }

  }

  static class MyExecutor implements Scheduler {
    final ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();

    final ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(1);

    @Override
    public void execute(Runnable command) {
      forkJoinPool.execute(command);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
      return scheduledThreadPool.schedule(command, delay, unit);
    }
  }

}