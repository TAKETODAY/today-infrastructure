/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.util.concurrent;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/3/30 20:30
 */
class CompleteFutureTests {

  @Test
  void await() throws InterruptedException, ExecutionException, TimeoutException {
    Future<Integer> ok = Future.ok(1);
    assertThat(ok.await()).isNotCancelled();
    assertThat(ok.getNow()).isEqualTo(1);
    assertThat(ok.get()).isEqualTo(1);
    assertThat(ok.get(1, TimeUnit.SECONDS)).isEqualTo(1);
    assertThat(ok.isSuccess()).isTrue();
    assertThat(ok.isFailed()).isFalse();
    assertThat(ok.isCancelled()).isFalse();
    assertThat(ok.isDone()).isTrue();
    assertThat(ok.awaitUninterruptibly().getNow()).isEqualTo(1);
  }

  @Test
  void awaitTimeout() throws InterruptedException {
    assertThat(Future.ok(1).await(1)).isTrue();
    assertThat(Future.ok(1).await(1, TimeUnit.SECONDS)).isTrue();
    assertThat(Future.ok(1).awaitUninterruptibly(1)).isTrue();
    assertThat(Future.ok(1).awaitUninterruptibly(1, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  void sync() {
    IllegalStateException cause = new IllegalStateException();
    assertThat(Future.failed(cause)).isDone();
    assertThat(Future.failed(cause)).isNotCancelled();
    assertThatThrownBy(() -> Future.failed(cause).sync())
            .isSameAs(cause);
  }

  @Test
  void syncUninterruptibly() {
    IllegalStateException cause = new IllegalStateException();
    assertThatThrownBy(() -> Future.failed(cause).syncUninterruptibly())
            .isSameAs(cause);
  }

  @Test
  void completable_success() throws ExecutionException, InterruptedException {
    Future<Integer> future = Future.ok(1);
    CompletableFuture<Integer> completable = future.completable();
    assertThat(completable.join()).isEqualTo(1);
    assertThat(completable).isDone();
    assertThat(completable).isNotCancelled();
    assertThat(completable).isCompleted();

    assertThat(completable.get()).isEqualTo(1);
  }

  @Test
  void completable_failed() {
    IllegalStateException cause = new IllegalStateException();
    Future<Integer> future = Future.failed(cause);
    CompletableFuture<Integer> completable = future.completable();
    assertThat(completable).isDone();
    assertThat(completable).isNotCancelled();
    assertThat(completable).isCompletedExceptionally();

    assertThatThrownBy(future::get)
            .isInstanceOf(ExecutionException.class)
            .satisfies(throwable -> assertThat(throwable.getCause()).isEqualTo(cause));

    assertThatThrownBy(completable::get)
            .isInstanceOf(ExecutionException.class)
            .satisfies(throwable -> assertThat(throwable.getCause()).isEqualTo(cause));
  }

  @Test
  void failedUnwrap() {
    IllegalStateException exception = new IllegalStateException();
    ExecutionException cause = new ExecutionException(exception);
    Future<Integer> adaption = Future.failed(cause);

    assertThat(adaption.awaitUninterruptibly()).isDone();
    assertThat(adaption).isNotCancelled();
    assertThat(adaption.getNow()).isNull();
    assertThat(adaption.getCause()).isSameAs(exception);
    assertThatThrownBy(adaption::get).isSameAs(cause);
  }

  @Test
  void cancel() {
    Future<Integer> future = Future.failed(new IllegalStateException());
    assertThat(future.cancel()).isFalse();
  }
}