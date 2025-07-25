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

package infra.util.concurrent;

import junit.framework.AssertionFailedError;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

import infra.core.Pair;
import infra.core.Triple;
import infra.logging.LoggerFactory;
import infra.util.function.ThrowingRunnable;
import infra.util.function.ThrowingSupplier;
import lombok.SneakyThrows;

import static infra.util.concurrent.Future.combine;
import static infra.util.concurrent.Future.failed;
import static infra.util.concurrent.Future.forExecutor;
import static infra.util.concurrent.Future.forPromise;
import static infra.util.concurrent.Future.ok;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/3/22 14:31
 */
class FutureTests {

  @Test
  void validateInitialValues() {
    Promise<Object> future = Future.forPromise();
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
    Promise<Object> future = Future.forPromise();
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
  void zipWith_failed() {
    Future<Object> future = ok("2")
            .zipWith(ok(1), (first, second) -> {
              assertThat(first).isEqualTo("2");
              assertThat(second).isEqualTo(1);
              throw new IllegalStateException();
            })
            .onSuccess(result -> {
              fail("never");
            })
            .onFailure((e) -> fail("never"));

    assertThat(future.awaitUninterruptibly()).isDone();
    assertThat(future.getCause()).isInstanceOf(IllegalStateException.class);
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
    Promise<String> promise = Future.forPromise();
    Future<String> stringFuture = promise.errorHandling(Throwable::getMessage);

    promise.cancel();
    assertThat(stringFuture.await()).isCancelled();

    promise = Future.forPromise();
    promise.cancel();

    stringFuture = promise.errorHandling(Throwable::getMessage);
    assertThat(stringFuture.await()).isCancelled();
  }

  @Test
  void errorHandling_failed() throws InterruptedException {
    IllegalStateException exception = new IllegalStateException("result");
    Promise<String> promise = Future.forPromise();
    Future<String> stringFuture = promise.errorHandling(Throwable::getMessage);
    promise.tryFailure(exception);

    assertThat(stringFuture.await()).isDone();
    assertThat(stringFuture.getNow()).isEqualTo("result");
  }

  @Test
  void errorHandling_success() throws InterruptedException {
    Promise<String> promise = Future.forPromise();
    Future<String> stringFuture = promise.errorHandling(Throwable::getMessage);
    promise.trySuccess("result");

    assertThat(stringFuture.await()).isDone();
    assertThat(stringFuture.getNow()).isEqualTo("result");
  }

  @Test
  void errorHandling_notPropagate() throws InterruptedException {
    Promise<String> promise = Future.forPromise();
    Future<String> stringFuture = promise.errorHandling(s -> "notPropagate")
            .errorHandling(Throwable::getMessage);
    promise.tryFailure(new IllegalStateException());

    assertThat(stringFuture.await()).isDone();
    assertThat(stringFuture.getNow()).isEqualTo("notPropagate");
  }

  @Test
  void catchMostSpecific_propagate() {
    var stringFuture = Future.<String>run(() -> { throw new UnsupportedOperationException(new IllegalStateException()); })
            .catchSpecificCause(IllegalArgumentException.class, iae -> "iae")
            .catchSpecificCause(IllegalStateException.class, i -> "IllegalStateException");

    assertThat(stringFuture.awaitUninterruptibly()).isDone();
    assertThat(stringFuture.getNow()).isEqualTo("IllegalStateException");
  }

  @Test
  void catchMostSpecific_notPropagate() {
    Future<String> stringFuture = Future.<String>run(() -> { throw new UnsupportedOperationException(new IllegalArgumentException()); })
            .catchSpecificCause(IllegalArgumentException.class, iae -> "iae")
            .catchSpecificCause(IllegalStateException.class, i -> "IllegalStateException");

    assertThat(stringFuture.awaitUninterruptibly()).isDone();
    assertThat(stringFuture.getNow()).isEqualTo("iae");
  }

  @Test
  void catchRootCause_propagate() {
    Future<String> stringFuture = Future.<String>run(() -> { throw new UnsupportedOperationException(new IllegalStateException()); })
            .catchRootCause(IllegalArgumentException.class, iae -> "iae")
            .catchRootCause(IllegalStateException.class, i -> "IllegalStateException");

    assertThat(stringFuture.awaitUninterruptibly()).isDone();
    assertThat(stringFuture.getNow()).isEqualTo("IllegalStateException");
  }

  @Test
  void catchRootCause_notPropagate() {
    Future<String> stringFuture = Future.<String>run(() -> { throw new UnsupportedOperationException(new IllegalArgumentException()); })
            .catchRootCause(IllegalArgumentException.class, iae -> "iae")
            .catchRootCause(IllegalStateException.class, i -> "IllegalStateException");

    assertThat(stringFuture.awaitUninterruptibly()).isDone();
    assertThat(stringFuture.getNow()).isEqualTo("iae");
  }

  @Test
  void catching_propagate() {
    Future<String> stringFuture = Future.<String>run(() -> { throw new IllegalStateException(new IllegalArgumentException()); })
            .catching(IllegalArgumentException.class, iae -> "iae")
            .catching(IllegalStateException.class, i -> "IllegalStateException");

    assertThat(stringFuture.awaitUninterruptibly()).isDone();
    assertThat(stringFuture.getNow()).isEqualTo("IllegalStateException");
  }

  @Test
  void catching_notPropagate() {
    Future<String> stringFuture = Future.<String>run(() -> { throw new IllegalArgumentException(new IllegalStateException()); })
            .catching(IllegalArgumentException.class, iae -> "iae")
            .catching(IllegalStateException.class, i -> "IllegalStateException");

    assertThat(stringFuture.awaitUninterruptibly()).isDone();
    assertThat(stringFuture.getNow()).isEqualTo("iae");
  }

  @Test
  void catching_failed() {
    Promise<String> future = Future.forPromise();
    Future<String> stringFuture = future
            .catching(IllegalArgumentException.class, iae -> { throw new IllegalStateException("result"); })
            .catching(IllegalStateException.class, IllegalStateException::getMessage);

    future.tryFailure(new IllegalArgumentException(new IllegalStateException()));

    assertThat(stringFuture.awaitUninterruptibly()).isDone();
    assertThat(stringFuture.getNow()).isEqualTo("result");
  }

  @Test
  void catching_lost() {
    Promise<String> future = Future.forPromise();
    Future<String> stringFuture = future
            .catching(UnsupportedOperationException.class, UnsupportedOperationException::getMessage);

    IllegalArgumentException exception = new IllegalArgumentException(new IllegalStateException());
    future.tryFailure(exception);

    assertThat(stringFuture.awaitUninterruptibly()).isDone();
    assertThat(stringFuture.getNow()).isNull();
    assertThat(stringFuture.getCause()).isSameAs(exception);
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

    Promise<Integer> promise = Future.forPromise();
    Future<Integer> map = promise.map(i -> i + 1);
    promise.tryFailure(exception);

    assertThat(map.awaitUninterruptibly().getCause()).isSameAs(exception);
    assertThat(promise.awaitUninterruptibly().getCause()).isSameAs(exception);
  }

  @Test
  void map_failedInMapper() {
    IllegalStateException exception = new IllegalStateException();
    Promise<Void> promise = Future.forPromise();
    Future<Void> failedFuture = promise
            .map(s -> {
              throw exception;
            });

    promise.setSuccess(null);

    assertThat(failedFuture.awaitUninterruptibly().getCause()).isSameAs(exception);
  }

  @Test
  void map_cancel() {
    Promise<String> promise = Future.forPromise();
    assertThat(promise.toString()).endsWith("[Not completed]");

    Future<String> stringFuture = promise.map(s -> (s + "1"));
    stringFuture.cancel();

    assertThat(promise.awaitUninterruptibly()).isCancelled();
    assertThat(stringFuture.awaitUninterruptibly()).isCancelled();

    promise = Future.forPromise();
    stringFuture = promise.map(s -> (s + "1"));
    promise.cancel();
    assertThat(promise.awaitUninterruptibly()).isCancelled();
    assertThat(stringFuture.awaitUninterruptibly()).isCancelled();

    assertThat(promise.toString()).endsWith("[Cancelled]");
  }

  @Test
  void map_null() {
    assertThat(ok(1).mapNull()).succeedsWithin(1, TimeUnit.SECONDS)
            .isNull();
  }

  @Test
  void mapNull() {
    assertThat(ok(1).mapNull(s -> {

    })).succeedsWithin(1, TimeUnit.SECONDS)
            .isNull();

    assertThat(ok(1).mapNull(res -> {
      assertThat(res).isEqualTo(1);
      throw new IOException();
    })).failsWithin(1, TimeUnit.SECONDS)
            .withThrowableThat()
            .withRootCauseInstanceOf(IOException.class)
            .isNotNull();
  }

  @Test
  void switchIfEmpty() {
    assertThat(ok(2).switchIfEmpty(1))
            .succeedsWithin(1, TimeUnit.SECONDS)
            .isEqualTo(2);

    assertThat(ok().switchIfEmpty(1)).succeedsWithin(1, TimeUnit.SECONDS).isEqualTo(1);

    assertThat(failed(new RuntimeException()).switchIfEmpty(1)).failsWithin(Duration.ofSeconds(1))
            .withThrowableThat()
            .havingRootCause()
            .isInstanceOf(RuntimeException.class);

    assertThat(failed(new RuntimeException()).switchIfEmpty(() -> 1)).failsWithin(Duration.ofSeconds(1))
            .withThrowableThat()
            .havingRootCause()
            .isInstanceOf(RuntimeException.class);

    // cancel

    Promise<Integer> promise = forPromise();
    promise.cancel();
    assertThat(promise.switchIfEmpty(1).awaitUninterruptibly()).isCancelled();
    assertThat(promise.switchIfEmpty(() -> 1).awaitUninterruptibly()).isCancelled();
    assertThat(promise.switchIfEmpty(Future.ok(1)).awaitUninterruptibly()).isCancelled();
    assertThat(promise.switchIfEmpty(() -> Future.ok(1)).awaitUninterruptibly()).isCancelled();
  }

  ThrowingSupplier<Integer> nullThrowingSupplier() {
    return null;
  }

  <T> Supplier<T> nullSupplier() {
    return null;
  }

  @Test
  void switchIfEmptySupplier() {
    assertThatThrownBy(() -> Future.<Integer>ok().switchIfEmpty(nullThrowingSupplier()))
            .hasMessage("defaultValue Supplier is required");

    assertThat(ok(2).switchIfEmpty(() -> 1)).succeedsWithin(Duration.ofSeconds(1)).isEqualTo(2);
    assertThat(ok(null).switchIfEmpty(() -> 1)).succeedsWithin(Duration.ofSeconds(1)).isEqualTo(1);
    assertThat(Future.<Integer>ok(null).switchIfEmpty((ThrowingSupplier<Integer>) () -> {
      // throws from supplier
      throw new IOException("msg"); // not runtime exception
    })).failsWithin(Duration.ofSeconds(1))
            .withThrowableThat()
            .havingRootCause()
            .withMessage("msg");

  }

  @Test
  void switchIfEmptyFuture() {
    assertThat(ok(2).switchIfEmpty(Future.ok(1))).succeedsWithin(Duration.ofSeconds(1)).isEqualTo(2);
    assertThat(ok(null).switchIfEmpty(Future.ok(1))).succeedsWithin(Duration.ofSeconds(1)).isEqualTo(1);
    assertThat(Future.<Integer>ok(null).switchIfEmpty(Future.failed(new IOException("msg"))))
            .failsWithin(Duration.ofSeconds(1))
            .withThrowableThat()
            .havingRootCause()
            .withMessage("msg");

  }

  @Test
  void switchIfEmptyFutureSupplier() {
    assertThatThrownBy(() -> Future.<Integer>ok().switchIfEmpty(nullSupplier()))
            .hasMessage("defaultValue Supplier is required");

    assertThat(ok(2).switchIfEmpty(() -> Future.ok(1))).succeedsWithin(Duration.ofSeconds(1)).isEqualTo(2);
    assertThat(Future.<Integer>ok(null).switchIfEmpty(() -> Future.ok(1))).succeedsWithin(Duration.ofSeconds(1)).isEqualTo(1);
    assertThat(Future.<Integer>ok(null).switchIfEmpty(() -> Future.failed(new IOException("msg"))))
            .failsWithin(Duration.ofSeconds(1))
            .withThrowableThat()
            .havingRootCause()
            .withMessage("msg");
  }

  @Test
  void mapCombiner() throws ExecutionException, InterruptedException {
    Promise<Integer> futureInteger = Future.forPromise();
    Promise<Boolean> futureBoolean = Future.forPromise();

    Callable<String> combiner = () -> {
      assertThat(futureInteger.isDone()).isTrue();
      assertThat(futureBoolean.isDone()).isTrue();
      return createCombinedResult(futureInteger.obtain(), futureBoolean.obtain());
    };

    Future<Integer> futureResult = combine(List.of(futureInteger, futureBoolean))
            .acceptFailure()
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

    Promise<String> promise = Future.forPromise();

    Future<String> future = ok("ok")
            .flatMap(() -> promise);

    future.awaitUninterruptibly(500);

    promise.setSuccess("okk");
    assertThat(promise.awaitUninterruptibly().getNow()).isEqualTo("okk");
    assertThat(future.awaitUninterruptibly().getNow()).isEqualTo("okk");
  }

  @Test
  void flatMap_failedInMapper() {
    IllegalStateException exception = new IllegalStateException();
    Promise<Void> promise = Future.forPromise();
    Future<Void> failedFuture = promise
            .flatMap(() -> {
              throw exception;
            });

    promise.setSuccess(null);
    assertThat(failedFuture.awaitUninterruptibly().getCause()).isSameAs(exception);
  }

  @Test
  void flatMap_cancel() {
    Promise<String> promise = Future.forPromise();
    Future<String> stringFuture = promise.flatMap(s -> ok(s + "1"));
    stringFuture.cancel();
    assertThat(promise.awaitUninterruptibly()).isCancelled();
    assertThat(stringFuture).isCancelled();

    promise = Future.forPromise();
    stringFuture = promise.flatMap(s -> ok(s + "1"));
    promise.cancel();
    assertThat(promise.awaitUninterruptibly()).isCancelled();
    assertThat(stringFuture.awaitUninterruptibly()).isCancelled();

    //

    promise = Future.forPromise();
    Promise<String> finalPromise = promise;

    stringFuture = ok("ok")
            .flatMap(s -> finalPromise);
    stringFuture.cancel();

    assertThat(promise.awaitUninterruptibly()).isCancelled();
    assertThat(stringFuture.awaitUninterruptibly()).isCancelled();

    promise = Future.forPromise();
    stringFuture = promise
            .flatMap(() -> Future.forPromise());
    promise.cancel();

    assertThat(promise.awaitUninterruptibly()).isCancelled();
    assertThat(stringFuture.awaitUninterruptibly()).isCancelled();

  }

  @Test
  void cascadeTo_success() {
    Promise<String> promise = Future.forPromise();
    Promise stringFuture = (Promise) Future.<String>forPromise()
            .cascadeTo(promise);

    assertThat(stringFuture).isNotDone();
    assertThat(stringFuture).isNotCancelled();
    assertThat(promise).isNotDone();
    assertThat(promise).isNotCancelled();
    stringFuture.setSuccess("ok");

    assertThat(promise.awaitUninterruptibly()).isDone();
    assertThat(stringFuture).isNotCancelled();
    assertThat(promise).isDone();
    assertThat(promise).isNotCancelled();
    assertThat(promise.getNow()).isEqualTo("ok");
  }

  @Test
  void cascadeTo_failed() {
    RuntimeException exception = new RuntimeException();
    Promise<String> promise = Future.forPromise();
    Promise stringFuture = (Promise) Future.<String>forPromise()
            .cascadeTo(promise);

    assertThat(stringFuture).isNotDone();
    assertThat(stringFuture).isNotCancelled();
    assertThat(promise).isNotDone();
    assertThat(promise).isNotCancelled();
    stringFuture.tryFailure(exception);

    assertThat(promise.awaitUninterruptibly()).isDone();
    assertThat(stringFuture).isNotCancelled();
    assertThat(promise).isDone();
    assertThat(promise).isNotCancelled();
    assertThat(promise.getNow()).isNull();

    assertThat(promise.getCause()).isSameAs(exception).isSameAs(stringFuture.getCause());
  }

  @Test
  void cascadeTo_cancel() {
    Promise<String> promise = Future.forPromise();
    var stringFuture = Future.<String>forPromise()
            .cascadeTo(promise);

    assertThat(stringFuture).isNotDone();
    assertThat(stringFuture).isNotCancelled();
    assertThat(promise).isNotDone();
    assertThat(promise).isNotCancelled();
    stringFuture.cancel();

    assertThat(promise.awaitUninterruptibly()).isDone();
    assertThat(stringFuture).isCancelled();
    assertThat(promise).isDone();
    assertThat(promise).isCancelled();
    assertThat(promise.getNow()).isNull();

    assertThat(promise.getCause()).isInstanceOf(CancellationException.class);
    assertThat(stringFuture.getCause()).isInstanceOf(CancellationException.class);
  }

  // FutureCombiner

  @Test
  void whenAllComplete_noLeakInterruption() throws Exception {
    Callable<String> combiner = () -> "";

    Future<String> futureResult = combine().acceptFailure().call(combiner, directExecutor());

    assertThat(Thread.interrupted()).isFalse();
    futureResult.cancel(true);
    assertThat(Thread.interrupted()).isFalse();
  }

  @Test
  void whenAllComplete_wildcard() throws Exception {
    Future<?>[] futures = new Future<?>[0];
    Callable<String> combiner = () -> "hi";

    Future<String> future = combine(ok("a"), ok("b"))
            .acceptFailure()
            .call(combiner, directExecutor());

    assertThat(future.get()).isEqualTo("hi");
    future = combine(futures)
            .acceptFailure()
            .call(combiner, directExecutor());
    assertThat(future.get()).isEqualTo("hi");
  }

  @Test
  void whenAllComplete_cancelledNotInterrupted() throws Exception {
    Promise<String> stringFuture = Future.forPromise();
    Promise<Boolean> booleanFuture = Future.forPromise();
    final CountDownLatch inFunction = new CountDownLatch(1);
    final CountDownLatch shouldCompleteFunction = new CountDownLatch(1);
    Callable<String> combiner = () -> {
      inFunction.countDown();
      shouldCompleteFunction.await();
      System.out.println("no interrupt");
      return "result";
    };

    Future<String> futureResult = Future.combine(stringFuture, booleanFuture)
            .acceptFailure()
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
    Promise<String> stringFuture = Future.forPromise();
    Promise<Boolean> booleanFuture = Future.forPromise();
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

    Future<String> futureResult = combine(stringFuture, booleanFuture)
            .acceptFailure()
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
    final Promise<Integer> futureInteger = Future.forPromise();
    final Promise<Boolean> futureBoolean = Future.forPromise();
    final String[] result = new String[1];
    Runnable combiner = () -> {
      assertThat(futureInteger.isDone()).isTrue();
      assertThat(futureBoolean.isDone()).isTrue();
      result[0] = createCombinedResult(futureInteger.obtain(), futureBoolean.obtain());
    };

    Future<?> futureResult = combine(futureInteger, futureBoolean)
            .acceptFailure()
            .run(combiner, directExecutor());

    Integer integerPartial = 1;
    futureInteger.setSuccess(integerPartial);
    Boolean booleanPartial = true;
    futureBoolean.setSuccess(booleanPartial);
    futureResult.get();
    assertThat(createCombinedResult(integerPartial, booleanPartial)).isEqualTo(result[0]);
  }

  @Test
  void whenAllComplete_runnableError() throws Exception {
    final RuntimeException thrown = new RuntimeException("test");

    final Promise<Integer> futureInteger = Future.forPromise();
    final Promise<Boolean> futureBoolean = Future.forPromise();
    Runnable combiner = () -> {
      assertThat(futureInteger.isDone()).isTrue();
      assertThat(futureBoolean.isDone()).isTrue();
      throw thrown;
    };

    Future<?> futureResult =
            combine(futureInteger, futureBoolean).acceptFailure().run(combiner, directExecutor());
    Integer integerPartial = 1;
    futureInteger.setSuccess(integerPartial);
    Boolean booleanPartial = true;
    futureBoolean.setSuccess(booleanPartial);

    try {
      futureResult.get();
      Assertions.fail();
    }
    catch (ExecutionException expected) {
      assertThat(thrown).isSameAs(expected.getCause());
    }
  }

  @Test
  void whenAllCompleteRunnable_resultCanceledWithoutInterrupt_doesNotInterruptRunnable()
          throws Exception {
    Promise<String> stringFuture = Future.forPromise();
    Promise<Boolean> booleanFuture = Future.forPromise();
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
            combine(stringFuture, booleanFuture)
                    .acceptFailure()
                    .run(combiner, newSingleThreadExecutor());

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
    Promise<String> stringFuture = Future.forPromise();
    Promise<Boolean> booleanFuture = Future.forPromise();
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

    Future<?> futureResult = combine(stringFuture, booleanFuture)
            .acceptFailure()
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
  void whenAllSucceed_() throws Exception {
    class PartialResultException extends Exception { }

    final Promise<Integer> futureInteger = Future.forPromise();
    final Promise<Boolean> futureBoolean = Future.forPromise();
    Callable<String> combiner = new Callable<String>() {
      @Override
      public String call() throws Exception {
        throw new AssertionFailedError("Callable should not have been called.");
      }
    };

    Future<String> futureResult = Future.combine(futureInteger, futureBoolean)
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
      assertThat(partialResultException).isSameAs(expected.getCause());
    }
  }

  @Test
  void whenAllSucceed_combineSuccess() throws Exception {
    Promise<Long> future1 = Future.forPromise();
    Promise<Long> future2 = Future.forPromise();

    Future<Void> combine = Future.combine(future1, future2)
            .asVoid();

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
    Future<Void> combine = Future.combine(List.of())
            .asVoid();

    assertThat(combine).isDone();
    assertThat(combine).isNotCancelled();
    assertThat(combine.get()).isNull();
    assertThat(combine.getNow()).isNull();
  }

  @Test
  void whenAllSucceed_callEmpty() throws Exception {
    Future<Void> combine = Future.combine()
            .call(() -> null, null);

    assertThat(combine.awaitUninterruptibly()).isDone();
    assertThat(combine).isNotCancelled();
    assertThat(combine.get()).isNull();
    assertThat(combine.getNow()).isNull();
  }

  @Test
  void whenAllSucceed_combineFailed() throws Exception {
    Promise<Long> future1 = Future.forPromise();
    Promise<Long> future2 = Future.forPromise();

    Future<Void> combine = Future.combine(future1, future2)
            .asVoid();

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
    Promise<Long> future1 = Future.forPromise();
    Promise<Long> future2 = Future.forPromise();

    AtomicInteger counter = new AtomicInteger(0);
    Future<Void> combine = Future.combine(future1, future2)
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
    Promise<Long> future1 = Future.forPromise();
    Promise<Long> future2 = Future.forPromise();

    Future<Long> combine = Future.combine(future1, future2)
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
    Promise<Long> future1 = Future.forPromise();
    Promise<Long> future2 = Future.forPromise();

    Future<Long> combine = Future.combine(future1, future2)
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
    Promise<Long> future1 = Future.forPromise();
    Promise<Long> future2 = Future.forPromise();

    Future<Void> combine = Future.combine(future1, future2)
            .asVoid();

    assertThat(combine).isInstanceOf(Promise.class);
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
    Promise<Void> promise = Future.forPromise();
    assertThat(promise).isNotDone();
    Future.defaultScheduler.execute(new Runnable() {
      @SneakyThrows
      @Override
      public void run() {
        Thread.sleep(1000);
        promise.setSuccess(null);
      }
    });

    assertThat(promise.await(5, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  void awaitFalse() throws Exception {
    Promise<Void> promise = Future.forPromise();
    assertThat(promise).isNotDone();
    assertThat(promise.await(1, TimeUnit.SECONDS)).isFalse();
  }

  @Test
  void completable() {
    Promise<Void> promise = Future.forPromise();
    CompletableFuture<Void> completable = promise.completable();
    assertThat(completable).isNotDone();
    assertThat(completable).isNotCancelled();
    assertThat(completable).isNotCompleted();

    promise.setSuccess(null);
    promise.awaitUninterruptibly();

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

    Promise<Void> future = Future.forPromise();
    Futures.tryFailure(future, exception, null);

    assertThat(future).isDone();
    assertThat(future).isNotCancelled();
    assertThat(future.awaitUninterruptibly().getCause()).isSameAs(exception);
  }

  @Test
  void static_tryFailure_failed() {
    IllegalStateException exception = new IllegalStateException("result");

    Promise<Void> future = Future.forPromise();
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
  void join() {
    var res = ok(1)
            .join();

    assertThat(res).isEqualTo(1);

    assertThatThrownBy(() ->
            failed(new RuntimeException("msg")).join())
            .hasMessage("msg");
  }

  @Test
  void joinTimeout() throws TimeoutException {
    var res = ok(1).join(Duration.ofSeconds(1));
    assertThat(res).isEqualTo(1);

    assertThatThrownBy(() ->
            failed(new RuntimeException("msg")).join(Duration.ofSeconds(1))).hasMessage("msg");

    Promise<Object> promise = forPromise();
    assertThatThrownBy(() -> promise.join(Duration.ofMillis(50)))
            .isInstanceOf(TimeoutException.class)
            .hasMessage("Timeout on blocking read for 50 ms");
    //

    var futureTask = Future.run(() -> {
      try {
        Thread.sleep(10);
      }
      catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    });

    Void join = futureTask.join(Duration.ofMillis(100));
    assertThat(join).isNull();

    var task = Future.run(() -> {
      try {
        Thread.sleep(10000);
      }
      catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    });

    assertThatThrownBy(() -> task.join(Duration.ofMillis(100), true))
            .isInstanceOf(TimeoutException.class)
            .hasMessage("Timeout on blocking read for 100 ms");

    assertThat(task).isDone();
    assertThat(task).isCancelled();
  }

  @Test
  void joinInterrupted() throws InterruptedException {
    AtomicBoolean interrupted = new AtomicBoolean(false);
    var futureTask = Future.run(() -> {
      try {
        Thread.sleep(10000);
      }
      catch (InterruptedException e) {
        interrupted.set(true);
        Thread.currentThread().interrupt();
      }
    });

    CountDownLatch latch = new CountDownLatch(1);
    var joinTask = Future.run(() -> {
      try {
        latch.countDown();
        futureTask.join(Duration.ofMillis(1000), true);
      }
      catch (TimeoutException e) {
        fail();
      }
      catch (Exception e) {
        interrupted.set(true);
        assertThat(e).isInstanceOf(InterruptedException.class);
      }

    });
    latch.await();
    Thread.sleep(100);
    joinTask.cancel();

    assertThat(joinTask.isCancelled()).isTrue();

    assertThat(futureTask).failsWithin(Duration.ofSeconds(1))
            .withThrowableThat().isInstanceOf(CancellationException.class);
    assertThat(futureTask.isCancelled()).isTrue();
    Thread.sleep(100);
    assertThat(interrupted).isTrue();
  }

  @Test
  void block() throws TimeoutException {
    var res = ok(1).block();
    assertThat(res).hasValue(1);

    assertThatThrownBy(() ->
            failed(new RuntimeException("msg")).block()).hasMessage("msg");

    res = ok(1).block(Duration.ofSeconds(1));
    assertThat(res).hasValue(1);

    assertThatThrownBy(() ->
            failed(new RuntimeException("msg")).block(Duration.ofSeconds(1))).hasMessage("msg");

    Promise<Object> promise = forPromise();
    assertThatThrownBy(() -> promise.block(Duration.ofMillis(50)))
            .isInstanceOf(TimeoutException.class)
            .hasMessage("Timeout on blocking read for 50 ms");
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
  void forAdaption_cancel() throws InterruptedException {
    AtomicBoolean interrupted = new AtomicBoolean(false);

    CountDownLatch latch = new CountDownLatch(1);
    CompletableFuture<Void> future = CompletableFuture.runAsync(new Runnable() {

      @Override
      public void run() {
        try {
          latch.countDown();
          Thread.sleep(10_000L);
        }
        catch (InterruptedException e) {
          interrupted.set(true);
        }
      }
    });

    Thread.sleep(100L);
    Future<Void> adaption = Future.forAdaption(future, directExecutor());
    adaption.cancel();

    assertThat(latch.getCount()).isEqualTo(0L);
    assertThat(adaption).isCancelled();
    Thread.sleep(1_000L);
    assertThat(future).isCancelled();
    assertThat(interrupted).isFalse();
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

  @Test
  void onCancelled() {
    Future.ok().onCancelled(() -> Assertions.fail());
    Future.ok().onCancelled((w) -> Assertions.fail());

    AtomicBoolean flag = new AtomicBoolean(false);
    var promise = forPromise(directExecutor())
            .onCancelled(() -> flag.set(true));
    promise.cancel();
    assertThat(flag).isTrue();

    assertThatThrownBy(() -> Future.ok().onCancelled((ThrowingRunnable) null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("cancelledCallback is required");

    assertThatThrownBy(() -> Future.ok().onCancelled((FailureCallback) null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("cancelledCallback is required");
  }

  @Test
  void onFailed() {
    Future.ok().onFailed(Assertions::fail);

    AtomicBoolean flag = new AtomicBoolean(false);
    var promise = forPromise(directExecutor())
            .onFailed(e -> flag.set(true));

    promise.cancel();
    assertThat(promise.getCause()).isInstanceOf(CancellationException.class);

    assertThat(flag).isTrue();

    Promise<Object> promise1 = forPromise(directExecutor());
    promise1.onFailed(e -> flag.set(false));

    RuntimeException exception = new RuntimeException();
    promise1.tryFailure(exception);

    assertThat(flag).isFalse();
    assertThat(promise1.getCause()).isSameAs(exception);
  }

  @Test
  void onFailure() {
    Future.ok().onFailure(Assertions::fail);

    AtomicBoolean flag = new AtomicBoolean(false);
    var promise = forPromise(directExecutor())
            .onFailure(e -> flag.set(true));

    promise.cancel();
    assertThat(promise.getCause()).isInstanceOf(CancellationException.class);

    assertThat(flag).isFalse();
    assertThat(promise.isDone()).isTrue();
    assertThat(promise.isFailed()).isTrue();
    assertThat(promise.isCancelled()).isTrue();
    assertThat(promise.isSuccess()).isFalse();

    Promise<Object> promise1 = forPromise(directExecutor());
    promise1.onFailure(e -> flag.set(true));

    RuntimeException exception = new RuntimeException();
    promise1.tryFailure(exception);

    assertThat(flag).isTrue();
    assertThat(promise1.isDone()).isTrue();
    assertThat(promise1.isFailed()).isTrue();
    assertThat(promise1.isCancelled()).isFalse();
    assertThat(promise1.isSuccess()).isFalse();
    assertThat(promise1.getCause()).isSameAs(exception);

    assertThat(ok().isFailure()).isFalse();
    assertThat(failed(new Exception()).isFailure()).isTrue();

    failed(new IllegalStateException())
            .onFailure(IllegalArgumentException.class, e -> fail());

    assertThat(flag).isTrue();

    failed(new IllegalStateException(), directExecutor())
            .onFailure(IllegalStateException.class, e -> flag.set(false));

    assertThat(flag).isFalse();

    failed(new IllegalStateException(), directExecutor())
            .onFailure(throwable -> throwable instanceof IllegalStateException, e -> flag.set(true));

    assertThat(flag).isTrue();
  }

  @Test
  void onFinally() {
    AtomicBoolean flag = new AtomicBoolean(false);

    Future.forExecutor(directExecutor())
            .onFailed(Assertions::fail)
            .onFinally(() -> flag.set(true));

    assertThat(flag).isTrue();

    // cancel
    forPromise(directExecutor())
            .onFinally(() -> flag.set(false))
            .onSuccess(v -> Assertions.fail())
            .cancel();
    assertThat(flag).isFalse();

    // failed

    Future.failed(new RuntimeException(), directExecutor())
            .onSuccess(v -> Assertions.fail())
            .onFinally(() -> flag.set(true));

    assertThat(flag).isTrue();
  }

  @Test
  void timeout() throws InterruptedException {
    AtomicBoolean flag = new AtomicBoolean(false);

    ScheduledExecutorService scheduledService = Executors.newScheduledThreadPool(1);
    forExecutor(directExecutor())
            .timeout(Duration.ofSeconds(1), scheduledService)
            .onSuccess(v -> flag.set(true));

    assertThat(flag).isTrue();

    var timeout = forPromise(directExecutor()).timeout(Duration.ofSeconds(1), scheduledService,
            f -> f.cancel(true));

    assertThat(timeout.await()).isDone();
    assertThat(timeout).isCancelled();

    assertThat(forPromise(directExecutor()).timeout(Duration.ofMillis(500), scheduledService))
            .failsWithin(Duration.ofMillis(1000))
            .withThrowableOfType(ExecutionException.class)
            .withMessageEndingWith("Timeout, after 0 seconds");

    assertThat(Future.run(() -> { }).timeout(Duration.ofSeconds(1), scheduledService))
            .succeedsWithin(Duration.ofMillis(500))
            .isNull();

    assertThat(Future.run(() -> {
      throw new IllegalStateException("failed");
    }).timeout(Duration.ofSeconds(1), scheduledService))
            .failsWithin(Duration.ofMillis(500))
            .withThrowableThat()
            .withCauseInstanceOf(IllegalStateException.class)
            .withMessageEndingWith("failed");

    //

    forExecutor(directExecutor())
            .timeout(Duration.ofSeconds(1), future -> future.trySuccess(1))
            .onSuccess(() -> flag.set(false));

    assertThat(flag).isFalse();

    forExecutor(directExecutor())
            .timeout(100, TimeUnit.MILLISECONDS, scheduledService)
            .onSuccess(() -> flag.set(true));

    assertThat(flag).isTrue();

    scheduledService.shutdown();
  }

  @Test
  void timeoutWithScheduler() {
    Scheduler scheduler = new DefaultScheduler();
    var scheduledService = Executors.newScheduledThreadPool(1);

    Future<Object> objectFuture = forExecutor(scheduler)
            .timeout(Duration.ofSeconds(1))
            .onFailure(e -> fail());

    assertThat(objectFuture).succeedsWithin(Duration.ofSeconds(2));
    assertThat(objectFuture.executor()).isSameAs(scheduler);

    objectFuture = objectFuture.timeout(1, TimeUnit.SECONDS, scheduler)
            .onFailure(e -> fail());

    assertThat(objectFuture).succeedsWithin(Duration.ofSeconds(2));
    assertThat(objectFuture.executor()).isSameAs(scheduler);

    scheduler = new Scheduler() {

      @Override
      public void execute(Runnable command) {
        directExecutor().execute(command);
      }

      @Override
      public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return scheduledService.schedule(command, 0, unit);
      }
    };

    objectFuture = Future.forPromise()
            .timeout(1, TimeUnit.SECONDS, scheduler);

    assertThat(objectFuture).failsWithin(1, TimeUnit.SECONDS)
            .withThrowableThat()
            .havingRootCause()
            .isInstanceOf(TimeoutException.class)
            .withMessageEndingWith("Timeout, after 1 seconds");

    assertThat(objectFuture.executor()).isSameAs(scheduler);

    scheduledService.shutdown();
  }

  @Test
  void whenAllCompleteStream() throws InterruptedException {
    Future<Void> future = combine(Stream.of(ok(), failed(new RuntimeException("msg"))))
            .acceptFailure()
            .asVoid()
            .onSuccess(() -> fail("ok"));

    assertThat(future).succeedsWithin(Duration.ofSeconds(1));

    future.await();
    assertThat(future.isSuccess()).isTrue();

    assertThat(combine(Stream.of(1, 2, 3).map(Future::ok))
            .acceptFailure()
            .asVoid()
            .onFailure(e -> fail())).succeedsWithin(Duration.ofSeconds(1));
  }

  @Test
  void whenAllSucceedStream() {
    Future<Void> future = combine(Stream.of(ok(), failed(new RuntimeException("msg"))))
            .asVoid(directExecutor())
            .onSuccess(() -> fail());

    assertThat(future).failsWithin(Duration.ofSeconds(1));
    assertThat(future.isFailed()).isTrue();

    assertThat(combine(Stream.of(1, 2, "3").map(Future::ok)).asVoid()).succeedsWithin(Duration.ofSeconds(1));
  }

  @Test
  void onErrorResume() {
    RuntimeException exception = new IllegalStateException("msg");
    Future<Integer> future = Future.<Integer>failed(exception)
            .onErrorResume(e -> ok(1));
    assertThat(future).succeedsWithin(Duration.ofSeconds(1)).isEqualTo(1);
    future = future.onErrorResume(IllegalArgumentException.class, e -> {
      fail();
      return ok(2);
    });
    // already success
    assertThat(future).succeedsWithin(Duration.ofSeconds(1)).isEqualTo(1);

    future = Future.<Integer>failed(exception).onErrorResume(IllegalArgumentException.class, e -> {
      fail();
      return ok(2);
    });

    assertThat(future).failsWithin(Duration.ofSeconds(1))
            .withThrowableThat().withRootCauseInstanceOf(IllegalStateException.class)
            .withMessageEndingWith("msg");

    future = future.onErrorResume(ex -> true, e -> ok(2));
    assertThat(future).succeedsWithin(Duration.ofSeconds(1)).isEqualTo(2);

    future = Future.<Integer>failed(exception)
            .onErrorResume(ex -> false, e -> ok(3));
    assertThat(future).failsWithin(Duration.ofSeconds(1))
            .withThrowableThat().withRootCauseInstanceOf(IllegalStateException.class)
            .withMessageEndingWith("msg");
  }

  @Test
  void onErrorMap() {
    RuntimeException exception = new IllegalStateException("msg");
    Future<Integer> future = Future.<Integer>failed(exception)
            .onErrorMap(IllegalArgumentException::new);

    assertThat(future).failsWithin(Duration.ofSeconds(1))
            .withThrowableThat().withCauseInstanceOf(IllegalArgumentException.class)
            .withMessageEndingWith("msg")
            .withRootCauseInstanceOf(IllegalStateException.class)
            .withMessageEndingWith("msg");

    // Class
    future = Future.<Integer>failed(exception)
            .onErrorMap(IOException.class, IllegalArgumentException::new);

    assertThat(future).failsWithin(Duration.ofSeconds(1))
            .withThrowableThat()
            .withRootCauseInstanceOf(IllegalStateException.class)
            .withMessageEndingWith("msg");

    future = Future.<Integer>failed(exception)
            .onErrorMap(IllegalStateException.class, IllegalArgumentException::new);

    assertThat(future).failsWithin(Duration.ofSeconds(1))
            .withThrowableThat().withCauseInstanceOf(IllegalArgumentException.class)
            .withMessageEndingWith("msg")
            .withRootCauseInstanceOf(IllegalStateException.class)
            .withMessageEndingWith("msg");

    // Predicate

    future = Future.<Integer>failed(exception)
            .onErrorMap(IllegalStateException.class::isInstance, IllegalArgumentException::new);

    assertThat(future).failsWithin(Duration.ofSeconds(1))
            .withThrowableThat().withCauseInstanceOf(IllegalArgumentException.class)
            .withMessageEndingWith("msg")
            .withRootCauseInstanceOf(IllegalStateException.class)
            .withMessageEndingWith("msg");

  }

  @Test
  void onErrorComplete() {
    RuntimeException exception = new IllegalStateException("msg");

    Future<Integer> future = Future.<Integer>failed(exception).onErrorComplete();
    assertThat(future).succeedsWithin(Duration.ofSeconds(1));
    assertThat(future.isFailed()).isFalse();
    assertThat(future.isFailure()).isFalse();
    assertThat(future.isCancelled()).isFalse();
    assertThat(future.isDone()).isTrue();
    assertThat(future.isSuccess()).isTrue();
    assertThat(future.getNow()).isNull();

    future = Future.<Integer>failed(exception).onErrorComplete(IllegalStateException.class);
    assertThat(future).succeedsWithin(Duration.ofSeconds(1));
    assertThat(future.isFailed()).isFalse();
    assertThat(future.isFailure()).isFalse();
    assertThat(future.isCancelled()).isFalse();
    assertThat(future.isDone()).isTrue();
    assertThat(future.isSuccess()).isTrue();
    assertThat(future.getNow()).isNull();

    future = Future.<Integer>failed(exception).onErrorComplete(e -> true);
    assertThat(future).succeedsWithin(Duration.ofSeconds(1));
    assertThat(future.isFailed()).isFalse();
    assertThat(future.isFailure()).isFalse();
    assertThat(future.isCancelled()).isFalse();
    assertThat(future.isDone()).isTrue();
    assertThat(future.isSuccess()).isTrue();
    assertThat(future.getNow()).isNull();

    future = Future.<Integer>failed(exception).onErrorComplete(e -> false);
    assertThat(future).failsWithin(Duration.ofSeconds(1));
    assertThat(future.isFailed()).isTrue();
    assertThat(future.isFailure()).isTrue();
    assertThat(future.isCancelled()).isFalse();
    assertThat(future.isDone()).isTrue();
    assertThat(future.isSuccess()).isFalse();
    assertThat(future.getNow()).isNull();
    assertThat(future.getCause()).isSameAs(exception);

    future = Future.<Integer>failed(exception).onErrorComplete(e -> true);
    assertThat(future).succeedsWithin(Duration.ofSeconds(1));
    assertThat(future.isFailed()).isFalse();
    assertThat(future.isFailure()).isFalse();
    assertThat(future.isCancelled()).isFalse();
    assertThat(future.isDone()).isTrue();
    assertThat(future.isSuccess()).isTrue();
    assertThat(future.getNow()).isNull();

  }

  @Test
  void onErrorReturn() {
    RuntimeException exception = new IllegalStateException("msg");

    Future<Integer> future = Future.<Integer>failed(exception).onErrorReturn(1);
    assertThat(future).succeedsWithin(Duration.ofSeconds(1));
    assertThat(future.isFailed()).isFalse();
    assertThat(future.isFailure()).isFalse();
    assertThat(future.isCancelled()).isFalse();
    assertThat(future.isDone()).isTrue();
    assertThat(future.isSuccess()).isTrue();
    assertThat(future.getNow()).isEqualTo(1);

    future = Future.<Integer>failed(exception).onErrorReturn(IllegalStateException.class, 2);
    assertThat(future).succeedsWithin(Duration.ofSeconds(1));
    assertThat(future.isFailed()).isFalse();
    assertThat(future.isFailure()).isFalse();
    assertThat(future.isCancelled()).isFalse();
    assertThat(future.isDone()).isTrue();
    assertThat(future.isSuccess()).isTrue();
    assertThat(future.getNow()).isEqualTo(2);

    future = Future.<Integer>failed(exception).onErrorReturn(e -> true, 3);
    assertThat(future).succeedsWithin(Duration.ofSeconds(1));
    assertThat(future.isFailed()).isFalse();
    assertThat(future.isFailure()).isFalse();
    assertThat(future.isCancelled()).isFalse();
    assertThat(future.isDone()).isTrue();
    assertThat(future.isSuccess()).isTrue();
    assertThat(future.getNow()).isEqualTo(3);

    future = Future.<Integer>failed(exception).onErrorReturn(e -> false, 4);
    assertThat(future).failsWithin(Duration.ofSeconds(1));
    assertThat(future.isFailed()).isTrue();
    assertThat(future.isFailure()).isTrue();
    assertThat(future.isCancelled()).isFalse();
    assertThat(future.isDone()).isTrue();
    assertThat(future.isSuccess()).isFalse();
    assertThat(future.getNow()).isNull();
    assertThat(future.getCause()).isSameAs(exception);

  }

  @Test
  void switchIfCancelled() {
    RuntimeException exception = new IllegalStateException("msg");

    Future<Integer> future = Future.<Integer>failed(exception)
            .switchIfCancelled(() -> 1);

    assertThat(future).failsWithin(Duration.ofSeconds(1))
            .withThrowableThat().withMessageEndingWith("msg");

    assertThat(future.isFailed()).isTrue();
    assertThat(future.isFailure()).isTrue();
    assertThat(future.isCancelled()).isFalse();
    assertThat(future.isDone()).isTrue();
    assertThat(future.isSuccess()).isFalse();

    Promise<Integer> integerPromise = Future.forPromise();

    Future<Integer> ifCancelled = integerPromise.switchIfCancelled(2);
    ifCancelled.cancel();

    assertThat(ifCancelled).isCancelled();
    assertThat(integerPromise.awaitUninterruptibly()).isCancelled();

    integerPromise = Future.forPromise();

    ifCancelled = integerPromise.switchIfCancelled(ok(1));
    integerPromise.cancel();

    assertThat(integerPromise).isCancelled();
    assertThat(ifCancelled.join()).isEqualTo(1);

    //

    Promise<Integer> promise = Future.forPromise();
    ifCancelled = promise.switchIfCancelled(2);
    promise.trySuccess(1);

    assertThat(promise.join()).isEqualTo(1);
    assertThat(ifCancelled.awaitUninterruptibly()).isNotCancelled();
    assertThat(promise.switchIfCancelled(2).join()).isEqualTo(1);

    //

    integerPromise = Future.forPromise();
    ifCancelled = integerPromise.switchIfCancelled(2);
    integerPromise.cancel();

    assertThat(ifCancelled.join()).isEqualTo(2);
    assertThat(integerPromise).isCancelled();

    //
    integerPromise = Future.forPromise();
    ifCancelled = integerPromise.switchIfCancelled((ThrowingSupplier<Integer>) () -> {
      throw new IOException("msg");
    });
    integerPromise.cancel();

    assertThatThrownBy(ifCancelled::join)
            .isInstanceOf(IOException.class)
            .hasMessage("msg");

    assertThat(integerPromise).isCancelled();
    //

    integerPromise = Future.forPromise();
    ifCancelled = integerPromise.switchIfCancelled(2);
    integerPromise.tryFailure(new IOException("msg"));

    assertThatThrownBy(ifCancelled::join)
            .isInstanceOf(IOException.class)
            .hasMessage("msg");

    assertThat(ok(1).switchIfCancelled(2).join()).isEqualTo(1);
    assertThatThrownBy(() ->
            failed(new IOException("msg")).switchIfCancelled(2).join())
            .hasMessage("msg");
  }

  @Test
  void switchIfCancelledFuture() throws InterruptedException {
    RuntimeException exception = new IllegalStateException("msg");

    Future<Integer> future = Future.<Integer>failed(exception)
            .switchIfCancelled(() -> Future.ok(1));

    assertThat(future).failsWithin(Duration.ofSeconds(1))
            .withThrowableThat().withMessageEndingWith("msg");

    assertThat(future.isFailed()).isTrue();
    assertThat(future.isFailure()).isTrue();
    assertThat(future.isCancelled()).isFalse();
    assertThat(future.isDone()).isTrue();
    assertThat(future.isSuccess()).isFalse();

    Promise<Integer> integerPromise = Future.forPromise();

    Future<Integer> ifCancelled = integerPromise.switchIfCancelled(Future.ok(2));
    ifCancelled.cancel();

    assertThat(ifCancelled).isCancelled();
    assertThat(integerPromise.awaitUninterruptibly()).isCancelled();

    integerPromise = Future.forPromise();

    ifCancelled = integerPromise.switchIfCancelled(ok(1));
    integerPromise.cancel();

    assertThat(integerPromise).isCancelled();
    assertThat(ifCancelled.join()).isEqualTo(1);

    //

    Promise<Integer> promise = Future.forPromise();
    ifCancelled = promise.switchIfCancelled(Future.ok(2));
    promise.trySuccess(1);

    assertThat(promise.join()).isEqualTo(1);
    assertThat(ifCancelled.awaitUninterruptibly()).isNotCancelled();
    assertThat(promise.switchIfCancelled(2).join()).isEqualTo(1);

    //

    integerPromise = Future.forPromise();
    ifCancelled = integerPromise.switchIfCancelled(Future.ok(2));
    integerPromise.cancel();

    assertThat(ifCancelled.join()).isEqualTo(2);
    assertThat(integerPromise).isCancelled();

    //
    integerPromise = Future.forPromise();
    ifCancelled = integerPromise.switchIfCancelled(() -> Future.failed(new IOException("msg")));
    integerPromise.cancel();

    assertThatThrownBy(ifCancelled::join)
            .isInstanceOf(IOException.class)
            .hasMessage("msg");

    assertThat(integerPromise).isCancelled();
    //

    integerPromise = Future.forPromise();
    ifCancelled = integerPromise.switchIfCancelled(Future.ok(2));
    integerPromise.tryFailure(new IOException("msg"));

    assertThatThrownBy(ifCancelled::join)
            .hasMessage("msg");

    //
    integerPromise = Future.forPromise();
    Promise<Integer> cancelledFuture = forPromise();
    ifCancelled = integerPromise.switchIfCancelled(cancelledFuture);
    integerPromise.cancel();
    Thread.sleep(1000);

    cancelledFuture.tryFailure(new IOException("msg"));

    assertThatThrownBy(ifCancelled::join)
            .hasMessage("msg");

    assertThat(ok(1).switchIfCancelled(Future.ok(2)).join()).isEqualTo(1);
    assertThatThrownBy(() ->
            failed(new IOException("msg")).switchIfCancelled(Future.failed(new RuntimeException("msg1"))).join())
            .hasMessage("msg");

    //
    integerPromise = Future.forPromise();
    ifCancelled = integerPromise.switchIfCancelled((Supplier<Future<Integer>>) () -> {
      throw new RuntimeException("msg");
    });
    integerPromise.cancel();

    assertThatThrownBy(ifCancelled::join)
            .isInstanceOf(RuntimeException.class)
            .hasMessage("msg");

    assertThat(integerPromise).isCancelled();
  }

  @Test
  void cancel_behavior() throws Exception {
    // 测试基本的取消操作
    Promise<String> promise = Future.forPromise(directExecutor());
    assertThat(promise.cancel()).isTrue(); // 首次取消应成功
    assertThat(promise.cancel()).isFalse(); // 重复取消应返回false
    assertThat(promise).isDone();
    assertThat(promise).isCancelled();
    assertThat(promise.isSuccess()).isFalse();
    assertThat(promise.isFailed()).isTrue();
    assertThat(promise.getCause()).isInstanceOf(CancellationException.class);

    // 测试已完成的Future不能被取消
    Promise<String> completedPromise = Future.forPromise(directExecutor());
    completedPromise.setSuccess("done");
    assertThat(completedPromise.cancel()).isFalse();
    assertThat(completedPromise).isNotCancelled();
    assertThat(completedPromise.getNow()).isEqualTo("done");

    // 测试已失败的Future不能被取消
    Promise<String> failedPromise = Future.forPromise(directExecutor());
    failedPromise.setFailure(new RuntimeException());
    assertThat(failedPromise.cancel()).isFalse();
    assertThat(failedPromise).isNotCancelled();

    // 测试取消会传播到子Future
    Promise<String> parent = Future.forPromise(directExecutor());
    Future<Integer> child = parent.map(String::length);

    assertThat(parent.cancel()).isTrue();
    assertThat(parent.await()).isCancelled();
    assertThat(child.await()).isCancelled();

    // 测试带中断的取消
    CountDownLatch latch = new CountDownLatch(1);
    AtomicBoolean interrupted = new AtomicBoolean(false);

    CountDownLatch finalLatch1 = latch;
    Future<?> future = Future.run(() -> {
      try {
        finalLatch1.countDown();
        Thread.sleep(10000);
      }
      catch (InterruptedException e) {
        interrupted.set(true);
        Thread.currentThread().interrupt();
      }
    });

    latch.await();
    assertThat(future.cancel(true)).isTrue();
    assertThat(future).isCancelled();
    Thread.sleep(100);
    assertThat(interrupted).isTrue();

    // 测试不带中断的取消
    latch = new CountDownLatch(1);
    interrupted.set(false);

    CountDownLatch finalLatch = latch;
    future = Future.run(() -> {
      try {
        finalLatch.countDown();
        Thread.sleep(10000);
      }
      catch (InterruptedException e) {
        interrupted.set(true);
      }
    });

    latch.await();
    assertThat(future.cancel(false)).isTrue();
    assertThat(future).isCancelled();
    Thread.sleep(100);
    assertThat(interrupted).isFalse();
  }

  @Test
  void cancelWithoutCancellationShouldReturnTrue() {
    var future = Future.forPromise(directExecutor());
    assertThat(future.cancel()).isTrue();
    assertThat(future.isCancelled()).isTrue();
    assertThat(future.isDone()).isTrue();
  }

  @Test
  void cancelWithNoCancellationShouldSetNullAsCancellationCause() {
    var future = Future.forPromise(directExecutor());
    future.cancel();
    assertThat(future.getCause()).isInstanceOf(CancellationException.class);
  }

  @Test
  void cancelWithCancellationShouldSetSpecifiedCause() {
    var future = Future.forPromise(directExecutor());
    var cause = new IllegalStateException("Cancelled");
    future.cancel(cause);
    assertThat(future.getCause()).isSameAs(cause);
  }

  @Test
  void cancelCompletedFutureShouldReturnFalse() {
    var future = Future.forPromise(directExecutor());
    future.trySuccess("OK");
    assertThat(future.cancel()).isFalse();
    assertThat(future.isCancelled()).isFalse();
    assertThat(future.isSuccess()).isTrue();
  }

  @Test
  void cancelFailedFutureShouldReturnFalse() {
    var future = Future.forPromise(directExecutor());
    future.tryFailure(new RuntimeException());
    assertThat(future.cancel()).isFalse();
    assertThat(future.isCancelled()).isFalse();
    assertThat(future.isFailure()).isTrue();
  }

  @Test
  void cancelTwiceShouldReturnFalse() {
    var future = Future.forPromise(directExecutor());
    assertThat(future.cancel()).isTrue();
    assertThat(future.cancel()).isFalse();
  }

  @Test
  void cancelWithMayInterruptIfRunningShouldInterruptTask() {
    var future = Future.forPromise(directExecutor());
    assertThat(future.cancel(true)).isTrue();
    assertThat(future.isCancelled()).isTrue();
    // Check internal state to verify task was interrupted
    assertThat(future.state).isEqualTo(AbstractFuture.INTERRUPTED);
  }

  @Test
  void cancelWithoutMayInterruptIfRunningShouldNotInterruptTask() {
    var future = Future.forPromise(directExecutor());
    assertThat(future.cancel(false)).isTrue();
    assertThat(future.isCancelled()).isTrue();
    // Check internal state to verify task wasn't interrupted
    assertThat(future.state).isEqualTo(AbstractFuture.CANCELLED);
  }

  @Test
  void cancelShouldTriggerListeners() {
    var future = Future.forPromise(directExecutor());
    var completed = new AtomicBoolean();

    future.onCompleted((f) -> completed.set(true));
    future.cancel();
    future.awaitUninterruptibly();
    assertThat(completed).isTrue();
  }

  @Test
  void shouldCancelFutureWithCancellationCause() {
    Promise<String> promise = Future.forPromise(directExecutor());
    var cause = new IllegalStateException("Cancelled");
    assertTrue(promise.cancel(cause));
    assertTrue(promise.isCancelled());
    assertTrue(promise.isDone());
    assertSame(cause, promise.getCause());
  }

  @Test
  void shouldNotCancelCompletedFuture() {
    Promise<String> promise = Future.forPromise(directExecutor());
    promise.trySuccess("success");
    assertFalse(promise.cancel());
    assertFalse(promise.isCancelled());
    assertTrue(promise.isSuccess());
  }

  @Test
  void shouldNotCancelFailedFuture() {
    Promise<String> promise = Future.forPromise(directExecutor());
    promise.tryFailure(new RuntimeException());
    assertFalse(promise.cancel());
    assertFalse(promise.isCancelled());
    assertTrue(promise.isFailure());
  }

  @Test
  void shouldNotCancelAlreadyCancelledFuture() {
    Promise<String> promise = Future.forPromise(directExecutor());
    promise.cancel();
    assertFalse(promise.cancel());
    assertTrue(promise.isCancelled());
  }

  @Test
  void shouldInterruptFutureWhenCancellingWithMayInterruptIfRunning() {
    Promise<String> promise = Future.forPromise(directExecutor());
    assertTrue(promise.cancel(true));
    assertTrue(promise.isCancelled());
    assertTrue(promise.isDone());
  }

  @Test
  void shouldNotInterruptFutureWhenCancellingWithoutMayInterruptIfRunning() {
    Promise<String> promise = Future.forPromise(directExecutor());
    assertTrue(promise.cancel(false));
    assertTrue(promise.isCancelled());
    assertTrue(promise.isDone());
  }

  @Test
  void shouldThrowCancellationExceptionOnGetAfterCancel() {
    Promise<String> promise = Future.forPromise(directExecutor());
    promise.cancel();
    assertThrows(CancellationException.class, promise::join);
  }

  @Test
  void shouldThrowCancellationCauseOnGetAfterCancelWithCause() {
    Promise<String> promise = Future.forPromise(directExecutor());
    var cause = new IllegalStateException("Cancelled");
    promise.cancel(cause);
    var thrown = assertThrows(IllegalStateException.class, promise::join);
    assertSame(cause, thrown);
  }

  @Test
  void shouldNotifyListenersOnCancel() {
    Promise<String> promise = Future.forPromise(directExecutor());
    AtomicBoolean notified = new AtomicBoolean();
    promise.onCompleted((f) -> notified.set(true));
    promise.cancel();
    assertTrue(notified.get());
  }

  @Test
  void create() {
    Promise<String> promise = Future.create(this::handleAsync);

    assertThat(promise.awaitUninterruptibly(10000)).isTrue();
    assertThat(promise.isCancelled()).isFalse();
    assertThat(promise.isDone()).isTrue();
    assertThat(promise.isSuccess()).isTrue();
    assertThat(promise.getNow()).isEqualTo("success");

    assertThatThrownBy(() -> Future.create(null))
            .isInstanceOf(NullPointerException.class);
  }

  private void handleAsync(Promise<String> promise) {
    Future.run(() -> {
      try {
        Thread.sleep(500);
        promise.setSuccess("success");
      }
      catch (InterruptedException e) {
        promise.setFailure(e);
      }
    });
  }

  static Executor directExecutor() {
    return Runnable::run;
  }

  private static String createCombinedResult(Integer i, Boolean b) {
    return "-" + i + "-" + b;
  }

}
