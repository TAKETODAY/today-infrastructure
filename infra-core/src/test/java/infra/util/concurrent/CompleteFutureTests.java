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