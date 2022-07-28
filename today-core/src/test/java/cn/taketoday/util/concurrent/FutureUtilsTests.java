/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.util.concurrent;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import cn.taketoday.core.task.SimpleAsyncTaskExecutor;

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
