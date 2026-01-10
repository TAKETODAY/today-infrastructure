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

package infra.core;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import infra.core.task.TaskRejectedException;
import infra.core.task.TaskTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/20 21:37
 */
class ExceptionTests {

  @Nested
  class TaskTimeoutExceptionTests {

    @Test
    void constructorWithMessageCreatesException() {
      String message = "Task execution timed out";
      TaskTimeoutException exception = new TaskTimeoutException(message);

      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isNull();
    }

    @Test
    void constructorWithMessageAndCauseCreatesException() {
      String message = "Task execution timed out";
      Throwable cause = new RuntimeException("Root cause");
      TaskTimeoutException exception = new TaskTimeoutException(message, cause);

      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void exceptionIsInstanceOfTaskRejectedException() {
      TaskTimeoutException exception = new TaskTimeoutException("Test");
      assertThat(exception).isInstanceOf(TaskRejectedException.class);
    }

    @Test
    void exceptionIsInstanceOfRuntimeException() {
      TaskTimeoutException exception = new TaskTimeoutException("Test");
      assertThat(exception).isInstanceOf(RuntimeException.class);
    }

  }

  @Nested
  class TaskRejectedExceptionTests {
    @Test
    void constructorWithMessageCreatesException() {
      String message = "Task rejected";
      TaskRejectedException exception = new TaskRejectedException(message);

      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isNull();
    }

    @Test
    void constructorWithMessageAndCauseCreatesException() {
      String message = "Task rejected";
      Throwable cause = new RuntimeException("Root cause");
      TaskRejectedException exception = new TaskRejectedException(message, cause);

      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void constructorWithExecutorTaskAndCauseCreatesException() {
      Executor executor = Runnable::run;
      Object task = new Object();
      RejectedExecutionException cause = new RejectedExecutionException("Rejected");
      TaskRejectedException exception = new TaskRejectedException(executor, task, cause);

      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).contains("did not accept task:");
      assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void constructorWithShutdownExecutorCreatesExceptionWithShutdownState() {
      ExecutorService executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
      executor.shutdown();
      Object task = new Object();
      RejectedExecutionException cause = new RejectedExecutionException("Rejected");
      TaskRejectedException exception = new TaskRejectedException(executor, task, cause);

      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).contains("ExecutorService in shutdown state did not accept task:");
      assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void exceptionIsInstanceOfRejectedExecutionException() {
      TaskRejectedException exception = new TaskRejectedException("Test");
      assertThat(exception).isInstanceOf(RejectedExecutionException.class);
    }

    @Test
    void exceptionIsInstanceOfRuntimeException() {
      TaskRejectedException exception = new TaskRejectedException("Test");
      assertThat(exception).isInstanceOf(RuntimeException.class);
    }

  }

}