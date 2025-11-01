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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/25 18:42
 */
class SyncTaskExecutorTests {

  @Test
  void plainExecution() {
    SyncTaskExecutor taskExecutor = new SyncTaskExecutor();

    ConcurrentClass target = new ConcurrentClass();
    assertThatNoException().isThrownBy(() -> taskExecutor.execute(target::concurrentOperation));
    assertThat(taskExecutor.execute(target::concurrentOperationWithResult)).isEqualTo("result");
    assertThatIOException().isThrownBy(() -> taskExecutor.execute(target::concurrentOperationWithException));
  }

  @Test
  void withConcurrencyLimit() {
    SyncTaskExecutor taskExecutor = new SyncTaskExecutor();
    taskExecutor.setConcurrencyLimit(2);

    ConcurrentClass target = new ConcurrentClass();
    List<CompletableFuture<?>> futures = new ArrayList<>(10);
    for (int i = 0; i < 10; i++) {
      futures.add(CompletableFuture.runAsync(() -> taskExecutor.execute(target::concurrentOperation)));
    }
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    assertThat(target.current).hasValue(0);
    assertThat(target.counter).hasValue(10);
  }

  @Test
  void withConcurrencyLimitAndResult() {
    SyncTaskExecutor taskExecutor = new SyncTaskExecutor();
    taskExecutor.setConcurrencyLimit(2);

    ConcurrentClass target = new ConcurrentClass();
    List<CompletableFuture<?>> futures = new ArrayList<>(10);
    for (int i = 0; i < 10; i++) {
      futures.add(CompletableFuture.runAsync(() ->
              assertThat(taskExecutor.execute(target::concurrentOperationWithResult)).isEqualTo("result")));
    }
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    assertThat(target.current).hasValue(0);
    assertThat(target.counter).hasValue(10);
  }

  @Test
  void withConcurrencyLimitAndException() {
    SyncTaskExecutor taskExecutor = new SyncTaskExecutor();
    taskExecutor.setConcurrencyLimit(2);

    ConcurrentClass target = new ConcurrentClass();
    List<CompletableFuture<?>> futures = new ArrayList<>(10);
    for (int i = 0; i < 10; i++) {
      futures.add(CompletableFuture.runAsync(() ->
              assertThatIOException().isThrownBy(() -> taskExecutor.execute(target::concurrentOperationWithException))));
    }
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    assertThat(target.current).hasValue(0);
    assertThat(target.counter).hasValue(10);
  }


  static class ConcurrentClass {

    final AtomicInteger current = new AtomicInteger();

    final AtomicInteger counter = new AtomicInteger();

    public void concurrentOperation() {
      if (current.incrementAndGet() > 2) {
        throw new IllegalStateException();
      }
      try {
        Thread.sleep(10);
      }
      catch (InterruptedException ex) {
        throw new IllegalStateException(ex);
      }
      current.decrementAndGet();
      counter.incrementAndGet();
    }

    public String concurrentOperationWithResult() {
      concurrentOperation();
      return "result";
    }

    public String concurrentOperationWithException() throws IOException {
      concurrentOperation();
      throw new IOException();
    }
  }

}