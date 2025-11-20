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