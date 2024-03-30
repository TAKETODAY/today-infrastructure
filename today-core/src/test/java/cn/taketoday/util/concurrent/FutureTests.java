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

import junit.framework.AssertionFailedError;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import cn.taketoday.core.Pair;
import cn.taketoday.core.Triple;
import cn.taketoday.logging.LoggerFactory;
import lombok.SneakyThrows;

import static cn.taketoday.util.concurrent.Future.failed;
import static cn.taketoday.util.concurrent.Future.forSettable;
import static cn.taketoday.util.concurrent.Future.ok;
import static cn.taketoday.util.concurrent.Future.whenAllComplete;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/3/22 14:31
 */
class FutureTests {

  @Test
  void validateInitialValues() {
    SettableFuture<Object> future = Future.forSettable();
    assertThat(future.isCancelled()).isFalse();
    assertThat(future.isDone()).isFalse();
    assertThat(future.isSuccess()).isFalse();
    assertThat(future.isFailed()).isFalse();
    assertThat(future.getCause()).isNull();
    assertThat(future.getNow()).isNull();
    assertThatThrownBy(future::obtain)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Result is required");
  }

  @Test
  void validateValuesAfterCancel() {
    SettableFuture<Object> future = Future.forSettable();
    assertThat(future.cancel()).isTrue();
    assertThat(future.isCancelled()).isTrue();
    assertThat(future.isDone()).isTrue();
    assertThat(future.isSuccess()).isFalse();
    assertThat(future.isFailed()).isTrue();
    assertThat(future.getCause()).isNotNull().isInstanceOf(CancellationException.class);
    assertThat(future.getNow()).isNull();
    assertThatThrownBy(future::obtain)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Result is required");

    assertThatThrownBy(future::get)
            .isInstanceOf(CancellationException.class);
  }

  @Test
  void zip() throws ExecutionException, InterruptedException {
    Pair<String, Integer> pair = Future.ok("2")
            .zip(Future.ok(1))
            .onSuccess(result -> {
              assertThat(result).isNotNull();
              assertThat(result.first).isEqualTo("2");
              assertThat(result.second).isEqualTo(1);
            })
            .onFailure((e) -> fail("never"))
            .get();

    assertThat(pair).isNotNull();
    assertThat(pair.first).isEqualTo("2");
    assertThat(pair.second).isEqualTo(1);

  }

  @Test
  void zip_failed() {
    IllegalStateException exception = new IllegalStateException("result");

    Future<Pair<String, Integer>> future = ok("2")
            .zip(Future.<Integer>failed(exception))
            .onSuccess(result -> fail("never"))
            .awaitUninterruptibly();

    assertThat(future).isDone();
    assertThat(future).isNotCancelled();
    assertThat(future.getCause()).isSameAs(exception);

    Future<Pair<String, String>> pairFuture = Future.<String>failed(exception)
            .zip(ok("ok"))
            .onSuccess(result -> fail("never"));

    assertThat(pairFuture.awaitUninterruptibly()).isDone();
    assertThat(pairFuture).isNotCancelled();
    assertThat(pairFuture.getCause()).isSameAs(exception);
  }

  @Test
  void zip_triple() throws ExecutionException, InterruptedException {
    Triple<String, Integer, Boolean> triple = ok("2")
            .zip(ok(1), ok(true))
            .onSuccess(result -> {
              assertThat(result).isNotNull();
              assertThat(result.first).isEqualTo("2");
              assertThat(result.second).isEqualTo(1);
            })
            .onFailure((e) -> fail("never"))
            .get();

    assertThat(triple).isNotNull();
    assertThat(triple.first).isEqualTo("2");
    assertThat(triple.second).isEqualTo(1);
    assertThat(triple.third).isTrue();

  }

  @Test
  void zipWith() throws ExecutionException, InterruptedException {
    Pair<String, Integer> pair = Future.ok("2")
            .zipWith(Future.ok(1), (first, second) -> {
              assertThat(first).isEqualTo("2");
              assertThat(second).isEqualTo(1);
              return Pair.of(first, second);
            })
            .onSuccess(result -> {
              assertThat(result).isNotNull();
              assertThat(result.first).isEqualTo("2");
              assertThat(result.second).isEqualTo(1);
            })
            .onFailure((e) -> fail("never"))
            .get();

    assertThat(pair).isNotNull();
    assertThat(pair.first).isEqualTo("2");
    assertThat(pair.second).isEqualTo(1);
  }

  @Test
  void errorHandling() throws InterruptedException {
    String string = Future.<String>failed(new RuntimeException())
            .errorHandling(e -> "recover")
            .await()
            .getNow();

    assertThat(string).isNotNull().isEqualTo("recover");
  }

  @Test
  void errorHandling_cancel() throws InterruptedException {
    SettableFuture<String> settable = Future.forSettable();
    Future<String> stringFuture = settable.errorHandling(Throwable::getMessage);

    settable.cancel();
    assertThat(stringFuture.await()).isCancelled();

    settable = Future.forSettable();
    settable.cancel();

    stringFuture = settable.errorHandling(Throwable::getMessage);
    assertThat(stringFuture.await()).isCancelled();
  }

  @Test
  void errorHandling_failed() throws InterruptedException {
    IllegalStateException exception = new IllegalStateException("result");
    SettableFuture<String> settable = Future.forSettable();
    Future<String> stringFuture = settable.errorHandling(Throwable::getMessage);
    settable.tryFailure(exception);

    assertThat(stringFuture.await()).isDone();
    assertThat(stringFuture.getNow()).isEqualTo("result");
  }

  @Test
  void errorHandling_success() throws InterruptedException {
    SettableFuture<String> settable = Future.forSettable();
    Future<String> stringFuture = settable.errorHandling(Throwable::getMessage);
    settable.trySuccess("result");

    assertThat(stringFuture.await()).isDone();
    assertThat(stringFuture.getNow()).isEqualTo("result");
  }

  @Test
  void map_success() throws ExecutionException, InterruptedException {
    var length = Future.ok("ok")
            .map(String::length)
            .get();

    assertThat(length).isEqualTo(2);
  }

  @Test
  void map_failed() {
    assertThatThrownBy(() -> ok("ok").map(null)).hasMessage("mapper is required");

    IllegalStateException exception = new IllegalStateException();
    Future<Void> failedFuture = ok("ok")
            .map(s -> {
              throw exception;
            });

    assertThat(failedFuture.awaitUninterruptibly().getCause()).isSameAs(exception);
    assertThat(failed(exception).map((s) -> null).awaitUninterruptibly().getCause()).isSameAs(exception);

    //

    SettableFuture<Integer> settable = Future.forSettable();
    Future<Integer> map = settable.map(i -> i + 1);
    settable.tryFailure(exception);

    assertThat(map.awaitUninterruptibly().getCause()).isSameAs(exception);
    assertThat(settable.awaitUninterruptibly().getCause()).isSameAs(exception);
  }

  @Test
  void map_failedInMapper() {
    IllegalStateException exception = new IllegalStateException();
    SettableFuture<Void> settable = forSettable();
    Future<Void> failedFuture = settable
            .map(s -> {
              throw exception;
            });

    settable.setSuccess(null);

    assertThat(failedFuture.awaitUninterruptibly().getCause()).isSameAs(exception);
  }

  @Test
  void map_cancel() {
    SettableFuture<String> settableFuture = Future.forSettable();
    assertThat(settableFuture.toString()).endsWith("[Not completed]");

    Future<String> stringFuture = settableFuture.map(s -> (s + "1"));
    stringFuture.cancel();

    assertThat(settableFuture.awaitUninterruptibly()).isCancelled();
    assertThat(stringFuture.awaitUninterruptibly()).isCancelled();

    settableFuture = Future.forSettable();
    stringFuture = settableFuture.map(s -> (s + "1"));
    settableFuture.cancel();
    assertThat(settableFuture.awaitUninterruptibly()).isCancelled();
    assertThat(stringFuture.awaitUninterruptibly()).isCancelled();

    assertThat(settableFuture.toString()).endsWith("[Cancelled]");
  }

  @Test
  void mapCombiner() throws ExecutionException, InterruptedException {
    SettableFuture<Integer> futureInteger = Future.forSettable();
    SettableFuture<Boolean> futureBoolean = Future.forSettable();

    Callable<String> combiner = () -> {
      assertTrue(futureInteger.isDone());
      assertTrue(futureBoolean.isDone());
      return createCombinedResult(futureInteger.obtain(), futureBoolean.obtain());
    };

    Future<Integer> futureResult = whenAllComplete(List.of(futureInteger, futureBoolean))
            .call(combiner, directExecutor())
            .map(String::length);

    Integer integerPartial = 1;
    futureInteger.setSuccess(integerPartial);
    Boolean booleanPartial = true;
    futureBoolean.setSuccess(booleanPartial);

    assertThat(futureResult.get()).isEqualTo(createCombinedResult(integerPartial, booleanPartial).length());
  }

  @Test
  void flatMap_success() throws ExecutionException, InterruptedException {
    var length = Future.ok("ok")
            .flatMap(s -> Future.ok(s + "1"))
            .get();

    assertThat(length).isEqualTo("ok1");
  }

  @Test
  void flatMap_failed() {
    assertThat(Future.failed(new RuntimeException())
            .flatMap(s -> Future.ok(s + "1")).awaitUninterruptibly().getCause()).isInstanceOf(RuntimeException.class);

    assertThat(Future.ok("ok")
            .flatMap(s -> Future.failed(new RuntimeException())).awaitUninterruptibly().getCause()).isInstanceOf(RuntimeException.class);

    SettableFuture<String> settableFuture = Future.forSettable();

    Future<String> future = ok("ok")
            .flatMap(s -> settableFuture);

    future.awaitUninterruptibly(500);

    settableFuture.setSuccess("okk");
    assertThat(settableFuture.awaitUninterruptibly().getNow()).isEqualTo("okk");
    assertThat(future.awaitUninterruptibly().getNow()).isEqualTo("okk");
  }

  @Test
  void flatMap_failedInMapper() {
    IllegalStateException exception = new IllegalStateException();
    SettableFuture<Void> settable = forSettable();
    Future<Void> failedFuture = settable
            .flatMap(s -> {
              throw exception;
            });

    settable.setSuccess(null);
    assertThat(failedFuture.awaitUninterruptibly().getCause()).isSameAs(exception);
  }

  @Test
  void flatMap_cancel() {
    SettableFuture<String> settableFuture = Future.forSettable();
    Future<String> stringFuture = settableFuture.flatMap(s -> ok(s + "1"));
    stringFuture.cancel();
    assertThat(settableFuture.awaitUninterruptibly()).isCancelled();
    assertThat(stringFuture).isCancelled();

    settableFuture = Future.forSettable();
    stringFuture = settableFuture.flatMap(s -> ok(s + "1"));
    settableFuture.cancel();
    assertThat(settableFuture.awaitUninterruptibly()).isCancelled();
    assertThat(stringFuture.awaitUninterruptibly()).isCancelled();

    //

    settableFuture = Future.forSettable();
    SettableFuture<String> finalSettableFuture = settableFuture;

    stringFuture = ok("ok")
            .flatMap(s -> finalSettableFuture);
    stringFuture.cancel();

    assertThat(settableFuture.awaitUninterruptibly()).isCancelled();
    assertThat(stringFuture.awaitUninterruptibly()).isCancelled();

    settableFuture = Future.forSettable();
    stringFuture = settableFuture
            .flatMap(s -> Future.forSettable());
    settableFuture.cancel();

    assertThat(settableFuture.awaitUninterruptibly()).isCancelled();
    assertThat(stringFuture.awaitUninterruptibly()).isCancelled();

  }

  @Test
  void cascadeTo_success() {
    SettableFuture<String> settableFuture = Future.forSettable();
    var stringFuture = Future.<String>forSettable()
            .cascadeTo(settableFuture);

    assertThat(stringFuture).isNotDone();
    assertThat(stringFuture).isNotCancelled();
    assertThat(settableFuture).isNotDone();
    assertThat(settableFuture).isNotCancelled();
    stringFuture.setSuccess("ok");

    assertThat(settableFuture.awaitUninterruptibly()).isDone();
    assertThat(stringFuture).isNotCancelled();
    assertThat(settableFuture).isDone();
    assertThat(settableFuture).isNotCancelled();
    assertThat(settableFuture.getNow()).isEqualTo("ok");
  }

  @Test
  void cascadeTo_failed() {
    RuntimeException exception = new RuntimeException();
    SettableFuture<String> settableFuture = Future.forSettable();
    var stringFuture = Future.<String>forSettable()
            .cascadeTo(settableFuture);

    assertThat(stringFuture).isNotDone();
    assertThat(stringFuture).isNotCancelled();
    assertThat(settableFuture).isNotDone();
    assertThat(settableFuture).isNotCancelled();
    stringFuture.tryFailure(exception);

    assertThat(settableFuture.awaitUninterruptibly()).isDone();
    assertThat(stringFuture).isNotCancelled();
    assertThat(settableFuture).isDone();
    assertThat(settableFuture).isNotCancelled();
    assertThat(settableFuture.getNow()).isNull();

    assertThat(settableFuture.getCause()).isSameAs(exception).isSameAs(stringFuture.getCause());
  }

  @Test
  void cascadeTo_cancel() {
    SettableFuture<String> settableFuture = Future.forSettable();
    var stringFuture = Future.<String>forSettable()
            .cascadeTo(settableFuture);

    assertThat(stringFuture).isNotDone();
    assertThat(stringFuture).isNotCancelled();
    assertThat(settableFuture).isNotDone();
    assertThat(settableFuture).isNotCancelled();
    stringFuture.cancel();

    assertThat(settableFuture.awaitUninterruptibly()).isDone();
    assertThat(stringFuture).isCancelled();
    assertThat(settableFuture).isDone();
    assertThat(settableFuture).isCancelled();
    assertThat(settableFuture.getNow()).isNull();

    assertThat(settableFuture.getCause()).isInstanceOf(CancellationException.class);
    assertThat(stringFuture.getCause()).isInstanceOf(CancellationException.class);
  }

  // FutureCombiner

  @Test
  void whenAllComplete_noLeakInterruption() throws Exception {
    Callable<String> combiner = () -> "";

    Future<String> futureResult = whenAllComplete().call(combiner, directExecutor());

    assertThat(Thread.interrupted()).isFalse();
    futureResult.cancel(true);
    assertThat(Thread.interrupted()).isFalse();
  }

  @Test
  void whenAllComplete_wildcard() throws Exception {
    Future<?>[] futures = new Future<?>[0];
    Callable<String> combiner = () -> "hi";

    Future<String> future = whenAllComplete(ok("a"), ok("b"))
            .call(combiner, directExecutor());

    assertThat(future.get()).isEqualTo("hi");
    future = whenAllComplete(futures).call(combiner, directExecutor());
    assertThat(future.get()).isEqualTo("hi");
  }

  @Test
  void whenAllComplete_cancelledNotInterrupted() throws Exception {
    SettableFuture<String> stringFuture = Future.forSettable();
    SettableFuture<Boolean> booleanFuture = Future.forSettable();
    final CountDownLatch inFunction = new CountDownLatch(1);
    final CountDownLatch shouldCompleteFunction = new CountDownLatch(1);
    Callable<String> combiner = () -> {
      inFunction.countDown();
      shouldCompleteFunction.await();
      System.out.println("no interrupt");
      return "result";
    };

    Future<String> futureResult = Future.whenAllComplete(stringFuture, booleanFuture)
            .call(combiner, newSingleThreadExecutor());

    stringFuture.setSuccess("value");
    booleanFuture.setSuccess(true);
    inFunction.await();

    assertThat(futureResult.cancel(false)).isTrue();
    shouldCompleteFunction.countDown();
    try {
      futureResult.get();
      Assertions.fail();
    }
    catch (CancellationException expected) {
    }

  }

  @Test
  void whenAllComplete_interrupted() throws Exception {
    SettableFuture<String> stringFuture = Future.forSettable();
    SettableFuture<Boolean> booleanFuture = Future.forSettable();
    final CountDownLatch inFunction = new CountDownLatch(1);
    final CountDownLatch gotException = new CountDownLatch(1);
    Callable<String> combiner = () -> {
      inFunction.countDown();
      try {
        new CountDownLatch(1).await(); // wait for interrupt
      }
      catch (InterruptedException expected) {
        gotException.countDown();
        throw expected;
      }
      return "a";
    };

    Future<String> futureResult = whenAllComplete(stringFuture, booleanFuture)
            .call(combiner, newSingleThreadExecutor());

    stringFuture.setSuccess("value");
    booleanFuture.setSuccess(true);
    inFunction.await();
    futureResult.cancel(true);
    try {
      futureResult.get();
      Assertions.fail();
    }
    catch (CancellationException expected) {
    }
    gotException.await();
  }

  @Test
  void whenAllComplete_runnableResult() throws Exception {
    final SettableFuture<Integer> futureInteger = Future.forSettable();
    final SettableFuture<Boolean> futureBoolean = Future.forSettable();
    final String[] result = new String[1];
    Runnable combiner = () -> {
      assertTrue(futureInteger.isDone());
      assertTrue(futureBoolean.isDone());
      result[0] = createCombinedResult(futureInteger.obtain(), futureBoolean.obtain());
    };

    Future<?> futureResult = whenAllComplete(futureInteger, futureBoolean)
            .run(combiner, directExecutor());

    Integer integerPartial = 1;
    futureInteger.setSuccess(integerPartial);
    Boolean booleanPartial = true;
    futureBoolean.setSuccess(booleanPartial);
    futureResult.get();
    assertEquals(createCombinedResult(integerPartial, booleanPartial), result[0]);
  }

  @Test
  void whenAllComplete_runnableError() throws Exception {
    final RuntimeException thrown = new RuntimeException("test");

    final SettableFuture<Integer> futureInteger = Future.forSettable();
    final SettableFuture<Boolean> futureBoolean = Future.forSettable();
    Runnable combiner = () -> {
      assertTrue(futureInteger.isDone());
      assertTrue(futureBoolean.isDone());
      throw thrown;
    };

    Future<?> futureResult =
            whenAllComplete(futureInteger, futureBoolean).run(combiner, directExecutor());
    Integer integerPartial = 1;
    futureInteger.setSuccess(integerPartial);
    Boolean booleanPartial = true;
    futureBoolean.setSuccess(booleanPartial);

    try {
      futureResult.get();
      Assertions.fail();
    }
    catch (ExecutionException expected) {
      assertSame(thrown, expected.getCause());
    }
  }

  @Test
  void whenAllCompleteRunnable_resultCanceledWithoutInterrupt_doesNotInterruptRunnable()
          throws Exception {
    SettableFuture<String> stringFuture = Future.forSettable();
    SettableFuture<Boolean> booleanFuture = Future.forSettable();
    final CountDownLatch inFunction = new CountDownLatch(1);
    final CountDownLatch shouldCompleteFunction = new CountDownLatch(1);
    final CountDownLatch combinerCompletedWithoutInterrupt = new CountDownLatch(1);
    Runnable combiner = () -> {
      inFunction.countDown();
      try {
        shouldCompleteFunction.await();
        combinerCompletedWithoutInterrupt.countDown();
      }
      catch (InterruptedException e) {
        // Ensure the thread's interrupt status is preserved.
        Thread.currentThread().interrupt();
        throw new RuntimeException(e);
      }
    };

    Future<?> futureResult =
            whenAllComplete(stringFuture, booleanFuture).run(combiner, newSingleThreadExecutor());

    stringFuture.setSuccess("value");
    booleanFuture.setSuccess(true);
    inFunction.await();
    futureResult.cancel(false);
    shouldCompleteFunction.countDown();
    try {
      futureResult.get();
      Assertions.fail();
    }
    catch (CancellationException expected) {
    }
    combinerCompletedWithoutInterrupt.await();
  }

  @Test
  void whenAllCompleteRunnable_resultCanceledWithInterrupt_InterruptsRunnable() throws Exception {
    SettableFuture<String> stringFuture = Future.forSettable();
    SettableFuture<Boolean> booleanFuture = Future.forSettable();
    final CountDownLatch inFunction = new CountDownLatch(1);
    final CountDownLatch gotException = new CountDownLatch(1);
    Runnable combiner = () -> {
      inFunction.countDown();
      try {
        new CountDownLatch(1).await(); // wait for interrupt
      }
      catch (InterruptedException expected) {
        // Ensure the thread's interrupt status is preserved.
        Thread.currentThread().interrupt();
        gotException.countDown();
      }
    };

    Future<?> futureResult = whenAllComplete(stringFuture, booleanFuture)
            .run(combiner, newSingleThreadExecutor());

    stringFuture.setSuccess("value");
    booleanFuture.setSuccess(true);
    inFunction.await();
    futureResult.cancel(true);
    try {
      futureResult.get();
      Assertions.fail();
    }
    catch (CancellationException expected) {
    }
    gotException.await();
  }

  @Test
  void whenAllSucceed() throws Exception {
    class PartialResultException extends Exception { }

    final SettableFuture<Integer> futureInteger = Future.forSettable();
    final SettableFuture<Boolean> futureBoolean = Future.forSettable();
    Callable<String> combiner = new Callable<String>() {
      @Override
      public String call() throws Exception {
        throw new AssertionFailedError("Callable should not have been called.");
      }
    };

    Future<String> futureResult = Future.whenAllSucceed(futureInteger, futureBoolean)
            .call(combiner, directExecutor());

    PartialResultException partialResultException = new PartialResultException();
    futureInteger.setFailure(partialResultException);
    Boolean booleanPartial = true;
    futureBoolean.trySuccess(booleanPartial);
    try {
      futureResult.get();
      Assertions.fail();
    }
    catch (ExecutionException expected) {
      assertSame(partialResultException, expected.getCause());
    }
  }

  @Test
  void whenAllSucceed_combineSuccess() throws Exception {
    SettableFuture<Long> future1 = Future.forSettable();
    SettableFuture<Long> future2 = Future.forSettable();

    Future<Void> combine = Future.whenAllSucceed(future1, future2)
            .combine();

    assertThat(combine).isNotDone();

    future1.setSuccess(1L);
    future2.setSuccess(2L);

    assertThat(combine.get()).isNull();
    assertThat(combine.getNow()).isNull();
    assertThat(combine.getCause()).isNull();
    assertThat(combine.isSuccess()).isTrue();
    assertThat(combine.isFailed()).isFalse();
    assertThat(combine).isDone();
    assertThat(combine).isNotCancelled();
    assertThat(combine.await(1)).isTrue();
    assertThat(combine.await(1, TimeUnit.SECONDS)).isTrue();
    assertThat(combine.awaitUninterruptibly(1, TimeUnit.SECONDS)).isTrue();
    assertThat(combine.awaitUninterruptibly(1)).isTrue();
    assertThat(combine.awaitUninterruptibly()).isSameAs(combine);
    assertThat(combine.sync()).isSameAs(combine);
    assertThat(combine.syncUninterruptibly()).isSameAs(combine);

    assertThatThrownBy(combine::obtain).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void whenAllSucceed_combineEmpty() throws Exception {
    Future<Void> combine = Future.whenAllSucceed(List.of())
            .combine();

    assertThat(combine).isDone();
    assertThat(combine).isNotCancelled();
    assertThat(combine.get()).isNull();
    assertThat(combine.getNow()).isNull();
  }

  @Test
  void whenAllSucceed_callEmpty() throws Exception {
    Future<Void> combine = Future.whenAllSucceed()
            .call(() -> null, null);

    assertThat(combine.awaitUninterruptibly()).isDone();
    assertThat(combine).isNotCancelled();
    assertThat(combine.get()).isNull();
    assertThat(combine.getNow()).isNull();
  }

  @Test
  void whenAllSucceed_combineFailed() throws Exception {
    SettableFuture<Long> future1 = Future.forSettable();
    SettableFuture<Long> future2 = Future.forSettable();

    Future<Void> combine = Future.whenAllSucceed(future1, future2)
            .combine();

    assertThat(combine).isNotDone();

    future1.setSuccess(1L);
    future2.setFailure(new RuntimeException());

    assertThat(combine.await().getNow()).isNull();
    assertThat(combine.getCause()).isInstanceOf(RuntimeException.class);
    assertThat(combine.isSuccess()).isFalse();
    assertThat(combine.isFailed()).isTrue();
    assertThat(combine).isDone();
    assertThat(combine).isNotCancelled();
    assertThat(combine.await(1)).isTrue();
    assertThat(combine.await(1, TimeUnit.SECONDS)).isTrue();
    assertThat(combine.awaitUninterruptibly(1, TimeUnit.SECONDS)).isTrue();
    assertThat(combine.awaitUninterruptibly(1)).isTrue();
    assertThat(combine.awaitUninterruptibly()).isSameAs(combine);

    assertThatThrownBy(combine::get).isInstanceOf(ExecutionException.class);
    assertThatThrownBy(combine::obtain).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(combine::sync).isInstanceOf(RuntimeException.class);
    assertThatThrownBy(combine::syncUninterruptibly).isInstanceOf(RuntimeException.class);
  }

  @Test
  void whenAllSucceed_runSucceed() throws Exception {
    SettableFuture<Long> future1 = Future.forSettable();
    SettableFuture<Long> future2 = Future.forSettable();

    AtomicInteger counter = new AtomicInteger(0);
    Future<Void> combine = Future.whenAllSucceed(future1, future2)
            .run(counter::incrementAndGet);

    assertThat(combine).isNotDone();

    future1.setSuccess(1L);
    assertThat(combine).isNotDone();
    future2.setSuccess(1L);

    assertThat(combine.await().getNow()).isNull();
    assertThat(combine).isDone();
    assertThat(counter.get()).isEqualTo(1);

    assertThat(combine.getCause()).isNull();
    assertThat(combine.isSuccess()).isTrue();
    assertThat(combine.isFailed()).isFalse();
    assertThat(combine).isDone();
    assertThat(combine).isNotCancelled();
    assertThat(combine.await(1)).isTrue();
    assertThat(combine.await(1, TimeUnit.SECONDS)).isTrue();
    assertThat(combine.awaitUninterruptibly(1, TimeUnit.SECONDS)).isTrue();
    assertThat(combine.awaitUninterruptibly(1)).isTrue();
    assertThat(combine.awaitUninterruptibly()).isSameAs(combine);
    assertThat(combine.sync()).isSameAs(combine);
    assertThat(combine.syncUninterruptibly()).isSameAs(combine);
  }

  @Test
  void whenAllSucceed_callSucceed() throws Exception {
    SettableFuture<Long> future1 = Future.forSettable();
    SettableFuture<Long> future2 = Future.forSettable();

    Future<Long> combine = Future.whenAllSucceed(future1, future2)
            .call(() -> future1.obtain() + future2.obtain());

    assertThat(combine).isNotDone();

    future1.setSuccess(1L);
    assertThat(combine).isNotDone();
    future2.setSuccess(1L);

    assertThat(combine.await().getNow()).isEqualTo(2);
    assertThat(combine.obtain()).isEqualTo(2);
    assertThat(combine.get()).isEqualTo(2);
    assertThat(combine.get(1, TimeUnit.SECONDS)).isEqualTo(2);

    assertThat(combine).isDone();
    assertThat(combine.getCause()).isNull();
    assertThat(combine.isSuccess()).isTrue();
    assertThat(combine.isFailed()).isFalse();
    assertThat(combine).isDone();
    assertThat(combine).isNotCancelled();
    assertThat(combine.await(1)).isTrue();
    assertThat(combine.await(1, TimeUnit.SECONDS)).isTrue();
    assertThat(combine.awaitUninterruptibly(1, TimeUnit.SECONDS)).isTrue();
    assertThat(combine.awaitUninterruptibly(1)).isTrue();
    assertThat(combine.awaitUninterruptibly()).isSameAs(combine);
    assertThat(combine.sync()).isSameAs(combine);
    assertThat(combine.syncUninterruptibly()).isSameAs(combine);
  }

  @Test
  void whenAllSucceed_callCancel() {
    SettableFuture<Long> future1 = Future.forSettable();
    SettableFuture<Long> future2 = Future.forSettable();

    Future<Long> combine = Future.whenAllSucceed(future1, future2)
            .call(() -> future1.obtain() + future2.obtain(), null);

    assertThat(combine).isInstanceOf(ListenableFutureTask.class);
    assertThat(combine).isNotDone();
    assertThat(combine).isNotCancelled();
    assertThat(future1).isNotCancelled();
    assertThat(future2).isNotCancelled();

    future1.cancel();

    assertThat(future1.awaitUninterruptibly()).isCancelled();
    assertThat(future2.awaitUninterruptibly()).isCancelled();
    assertThat(combine.awaitUninterruptibly()).isCancelled();
  }

  @Test
  void whenAllSucceed_combineCancel() {
    SettableFuture<Long> future1 = Future.forSettable();
    SettableFuture<Long> future2 = Future.forSettable();

    Future<Void> combine = Future.whenAllSucceed(future1, future2)
            .combine();

    assertThat(combine).isInstanceOf(SettableFuture.class);
    assertThat(combine).isNotDone();
    assertThat(combine).isNotCancelled();
    assertThat(future1).isNotCancelled();
    assertThat(future2).isNotCancelled();

    future1.cancel();

    assertThat(future1.awaitUninterruptibly()).isCancelled();
    assertThat(future2.awaitUninterruptibly()).isCancelled();
    assertThat(combine.awaitUninterruptibly()).isCancelled();
  }

  @Test
  void awaitOk() throws Exception {
    SettableFuture<Void> settable = Future.forSettable();
    assertThat(settable).isNotDone();
    Future.defaultExecutor.execute(new Runnable() {
      @SneakyThrows
      @Override
      public void run() {
        Thread.sleep(1000);
        settable.setSuccess(null);
      }
    });

    assertThat(settable.await(5, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  void awaitFalse() throws Exception {
    SettableFuture<Void> settable = Future.forSettable();
    assertThat(settable).isNotDone();
    assertThat(settable.await(1, TimeUnit.SECONDS)).isFalse();
  }

  @Test
  void completable() {
    SettableFuture<Void> settable = Future.forSettable();
    CompletableFuture<Void> completable = settable.completable();
    assertThat(completable).isNotDone();
    assertThat(completable).isNotCancelled();
    assertThat(completable).isNotCompleted();

    settable.setSuccess(null);
    settable.awaitUninterruptibly();

    assertThat(completable.join()).isNull();
    assertThat(completable).isDone();
    assertThat(completable).isNotCancelled();
    assertThat(completable).isCompleted();
  }

  @Test
  void futureTaskToString() {
    var futureTask = Future.<Void>forFutureTask(() -> { });
    assertThat(futureTask.toString()).containsSequence("[Not completed, task = ");
  }

  @Test
  void static_tryFailure() {
    IllegalStateException exception = new IllegalStateException("result");

    SettableFuture<Void> future = Future.forSettable();
    Futures.tryFailure(future, exception, null);

    assertThat(future).isDone();
    assertThat(future).isNotCancelled();
    assertThat(future.awaitUninterruptibly().getCause()).isSameAs(exception);
  }

  @Test
  void static_tryFailure_failed() {
    IllegalStateException exception = new IllegalStateException("result");

    SettableFuture<Void> future = Future.forSettable();
    future.tryFailure(exception);
    Futures.tryFailure(future, new RuntimeException(), LoggerFactory.getLogger(getClass()));

    assertThat(future).isDone();
    assertThat(future).isNotCancelled();
    assertThat(future.awaitUninterruptibly().getCause()).isSameAs(exception);
  }

  @Test
  void join_success() throws Throwable {
    int res = ok(1)
            .zip(ok(2))
            .join((pair, cause) -> pair.first + pair.second);

    assertThat(res).isEqualTo(3);
  }

  @Test
  void join_failed() throws Throwable {
    RuntimeException cause1 = new RuntimeException();
    var res = ok(1)
            .zip(failed(cause1))
            .join((pair, cause) -> cause);

    assertThat(res).isEqualTo(cause1);
  }

  @Test
  void operationComplete_failed() {
    Future.ok(1)
            .onCompleted(future -> {
              throw new RuntimeException();
            });
  }

  @Test
  void safeExecute_failed() {
    Future.ok(1, command -> {
              throw new RuntimeException();
            })
            .onCompleted(future -> {
              throw new RuntimeException();
            });
  }

  @Test
  void forAdaption_success() {
    Future<Integer> adaption = Future.forAdaption(CompletableFuture.completedFuture(1));
    assertThat(adaption.awaitUninterruptibly()).isDone();
    assertThat(adaption).isNotCancelled();
    assertThat(adaption.getNow()).isEqualTo(1);
  }

  @Test
  void forAdaption_failed() {
    IllegalStateException exception = new IllegalStateException();
    Future<Integer> adaption = Future.forAdaption(CompletableFuture.failedFuture(exception));
    assertThat(adaption.awaitUninterruptibly()).isDone();
    assertThat(adaption).isNotCancelled();
    assertThat(adaption.getNow()).isNull();
    assertThat(adaption.getCause()).isSameAs(exception);
  }

  @Test
  void forFutureTask() throws ExecutionException, InterruptedException {
    AtomicInteger counter = new AtomicInteger();

    assertThat(Future.forFutureTask(counter::incrementAndGet, 2).execute().get()).isEqualTo(2);
    assertThat(counter.get()).isEqualTo(1);
    assertThat(Future.forFutureTask(counter::incrementAndGet).execute().get()).isEqualTo(2);
    assertThat(counter.get()).isEqualTo(2);
    assertThat(Future.forFutureTask(counter::incrementAndGet, directExecutor()).execute().get()).isEqualTo(3);
    assertThat(counter.get()).isEqualTo(3);
    assertThat(Future.forFutureTask(counter::incrementAndGet, 2, directExecutor()).execute().get()).isEqualTo(2);
    assertThat(counter.get()).isEqualTo(4);
  }

  @Test
  void run() throws ExecutionException, InterruptedException {
    AtomicInteger counter = new AtomicInteger();

    assertThat(Future.run(counter::incrementAndGet, 2).get()).isEqualTo(2);
    assertThat(counter.get()).isEqualTo(1);
    assertThat(Future.run(counter::incrementAndGet).get()).isEqualTo(2);
    assertThat(counter.get()).isEqualTo(2);
    assertThat(Future.run(counter::incrementAndGet, directExecutor()).get()).isEqualTo(3);
    assertThat(counter.get()).isEqualTo(3);
    assertThat(Future.run(counter::incrementAndGet, 2, directExecutor()).get()).isEqualTo(2);
    assertThat(counter.get()).isEqualTo(4);
  }

  static Executor directExecutor() {
    return Runnable::run;
  }

  private static String createCombinedResult(Integer i, Boolean b) {
    return "-" + i + "-" + b;
  }

}
