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

package infra.core.task;

import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import infra.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/20 21:41
 */
class AsyncTaskExecutorTests {

  @Test
  void executeWithTimeoutDelegatesToExecute() {
    AsyncTaskExecutor executor = task -> {
      // Simple implementation that just runs the task
      task.run();
    };

    Runnable task = mock(Runnable.class);
    assertThatCode(() -> executor.execute(task, 1000L)).doesNotThrowAnyException();
    // Verify that the task was executed
    verify(task, times(1)).run();
  }

  @Test
  void submitRunnableReturnsFuture() {
    AsyncTaskExecutor executor = task -> {
      task.run();
    };

    Runnable task = mock(Runnable.class);
    Future<Void> future = executor.submit(task);

    assertThat(future).isNotNull();
    // Verify that the task was executed
    verify(task, times(1)).run();
  }

  @Test
  void submitCallableReturnsFuture() throws Exception {
    AsyncTaskExecutor executor = task -> {
      try {
        task.run();
      }
      catch (Exception ignored) {
      }
    };

    Callable<String> task = mock(Callable.class);
    when(task.call()).thenReturn("result");

    Future<String> future = executor.submit(task);

    assertThat(future).isNotNull();
    try {
      assertThat(future.get()).isEqualTo("result");
    }
    catch (Exception e) {
      fail("Future should complete successfully");
    }

    verify(task, times(1)).call();
  }

  @Test
  void submitCompletableRunnableReturnsCompletableFuture() {
    AsyncTaskExecutor executor = task -> {
      task.run();
    };

    Runnable task = mock(Runnable.class);
    CompletableFuture<Void> future = executor.submitCompletable(task);

    assertThat(future).isNotNull();
    assertThat(future.isDone()).isTrue();
    verify(task, times(1)).run();
  }

  @Test
  void submitCompletableCallableReturnsCompletableFuture() throws Exception {
    AsyncTaskExecutor executor = task -> {
      try {
        task.run();
      }
      catch (Exception ignored) {
      }
    };

    Callable<String> task = mock(Callable.class);
    when(task.call()).thenReturn("result");

    CompletableFuture<String> future = executor.submitCompletable(task);

    assertThat(future).isNotNull();
    future.thenAccept(result -> assertThat(result).isEqualTo("result"));
    verify(task, times(1)).call();
  }

  @Test
  void timeoutConstantsHaveCorrectValues() {
    assertThat(AsyncTaskExecutor.TIMEOUT_IMMEDIATE).isEqualTo(0L);
    assertThat(AsyncTaskExecutor.TIMEOUT_INDEFINITE).isEqualTo(Long.MAX_VALUE);
  }

}