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