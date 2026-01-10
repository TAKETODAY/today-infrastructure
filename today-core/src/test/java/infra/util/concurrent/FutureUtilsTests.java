/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.util.concurrent;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import infra.core.task.SimpleAsyncTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/7/28 22:10
 */
class FutureUtilsTests {

  @Test
  void callAsyncNormal() throws ExecutionException, InterruptedException {
    String foo = "Foo";
    CompletableFuture<String> future = FutureUtils.callAsync(() -> foo);

    assertThat(future.get()).isEqualTo(foo);
    assertThat(future.isCancelled()).isFalse();
    assertThat(future.isDone()).isTrue();

    CountDownLatch latch = new CountDownLatch(1);
    future.whenComplete((s, throwable) -> {
      assertThat(s).isEqualTo(foo);
      assertThat(throwable).isNull();
      latch.countDown();
    });
    latch.await();
  }

  @Test
  void callAsyncException() throws InterruptedException {
    RuntimeException ex = new RuntimeException("Foo");
    CompletableFuture<String> future = FutureUtils.callAsync(() -> {
      throw ex;
    });
    assertThatExceptionOfType(ExecutionException.class)
            .isThrownBy(future::get)
            .withCause(ex);
    assertThat(future.isCancelled()).isFalse();
    assertThat(future.isDone()).isTrue();

    CountDownLatch latch = new CountDownLatch(1);
    future.whenComplete((s, throwable) -> {
      assertThat(s).isNull();
      assertThat(throwable).isInstanceOf(CompletionException.class)
              .hasCause(ex);
      latch.countDown();
    });
    latch.await();
  }

  @Test
  void callAsyncNormalExecutor() throws ExecutionException, InterruptedException {
    String foo = "Foo";
    CompletableFuture<String> future = FutureUtils.callAsync(() -> foo, new SimpleAsyncTaskExecutor());

    assertThat(future.get()).isEqualTo(foo);
    assertThat(future.isCancelled()).isFalse();
    assertThat(future.isDone()).isTrue();

    CountDownLatch latch = new CountDownLatch(1);
    future.whenComplete((s, throwable) -> {
      assertThat(s).isEqualTo(foo);
      assertThat(throwable).isNull();
      latch.countDown();
    });
    latch.await();
  }

  @Test
  void callAsyncExceptionExecutor() throws InterruptedException {
    RuntimeException ex = new RuntimeException("Foo");
    CompletableFuture<String> future = FutureUtils.callAsync(() -> {
      throw ex;
    }, new SimpleAsyncTaskExecutor());
    assertThatExceptionOfType(ExecutionException.class)
            .isThrownBy(future::get)
            .withCause(ex);
    assertThat(future.isCancelled()).isFalse();
    assertThat(future.isDone()).isTrue();

    CountDownLatch latch = new CountDownLatch(1);
    future.whenComplete((s, throwable) -> {
      assertThat(s).isNull();
      assertThat(throwable).isInstanceOf(CompletionException.class)
              .hasCause(ex);
      latch.countDown();
    });
    latch.await();
  }

}
