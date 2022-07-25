/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.scheduling.concurrent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import cn.taketoday.core.task.AsyncListenableTaskExecutor;

import static org.assertj.core.api.Assertions.assertThatCode;

class NoOpRunnable implements Runnable {

  @Override
  public void run() {
    // explicit no-op
  }

}

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 */
class ConcurrentTaskExecutorTests extends AbstractSchedulingTaskExecutorTests {

  private final ThreadPoolExecutor concurrentExecutor =
          new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

  @Override
  protected AsyncListenableTaskExecutor buildExecutor() {
    concurrentExecutor.setThreadFactory(new CustomizableThreadFactory(this.threadNamePrefix));
    return new ConcurrentTaskExecutor(concurrentExecutor);
  }

  @Override
  @AfterEach
  void shutdownExecutor() {
    for (Runnable task : concurrentExecutor.shutdownNow()) {
      if (task instanceof RunnableFuture) {
        ((RunnableFuture<?>) task).cancel(true);
      }
    }
  }

  @Test
  void zeroArgCtorResultsInDefaultTaskExecutorBeingUsed() {
    ConcurrentTaskExecutor executor = new ConcurrentTaskExecutor();
    assertThatCode(() -> executor.execute(new NoOpRunnable())).doesNotThrowAnyException();
  }

  @Test
  void passingNullExecutorToCtorResultsInDefaultTaskExecutorBeingUsed() {
    ConcurrentTaskExecutor executor = new ConcurrentTaskExecutor(null);
    assertThatCode(() -> executor.execute(new NoOpRunnable())).doesNotThrowAnyException();
  }

  @Test
  void passingNullExecutorToSetterResultsInDefaultTaskExecutorBeingUsed() {
    ConcurrentTaskExecutor executor = new ConcurrentTaskExecutor();
    executor.setConcurrentExecutor(null);
    assertThatCode(() -> executor.execute(new NoOpRunnable())).doesNotThrowAnyException();
  }

}
