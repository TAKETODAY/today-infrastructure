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

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.stream.Collectors;

import static infra.util.concurrent.FutureTests.directExecutor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/17 21:10
 */
class FutureCombinerTests {

  @Test
  void requireAllSucceedWithSuccessfulFuturesCompletesSuccessfully() {
    Future<String> future1 = Future.ok("result1");
    Future<String> future2 = Future.ok("result2");

    Future<List<Object>> combined = new FutureCombiner(false, List.of(future1, future2))
            .requireAllSucceed()
            .asList();

    assertThat(combined.join()).containsExactly("result1", "result2");
  }

  @Test
  void requireAllSucceedWithFailedFutureFailsImmediately() {
    Future<String> future1 = Future.ok("result1");
    Future<String> future2 = Future.failed(new RuntimeException("failed"));

    Future<List<Object>> combined = new FutureCombiner(false, List.of(future1, future2))
            .requireAllSucceed()
            .asList();

    assertThrows(RuntimeException.class, combined::join);
  }

  @Test
  void requireAllSucceedWithCancelledFutureFailsImmediately() {
    Future<String> future1 = Future.ok("result1");
    Promise<String> future2 = Future.forPromise();
    future2.cancel();

    Future<List<Object>> combined = new FutureCombiner(false, List.of(future1, future2))
            .requireAllSucceed()
            .asList();

    assertThrows(CancellationException.class, combined::join);
  }

  @Test
  void requireAllSucceedWithEmptyListCompletesWithEmptyList() {
    Future<List<Object>> combined = new FutureCombiner(false, List.of())
            .requireAllSucceed()
            .asList();

    assertThat(combined.join()).isEmpty();
  }

  @Test
  void requireAllSucceedMaintainsOriginalOrder() {
    Future<String> future1 = Future.ok("result1");
    Future<String> future2 = Future.ok("result2");
    Future<String> future3 = Future.ok("result3");

    Future<List<Object>> combined = new FutureCombiner(false, List.of(future1, future2, future3))
            .requireAllSucceed()
            .asList();

    assertThat(combined.join()).containsExactly("result1", "result2", "result3");
  }

  @Test
  void requireAllSucceedWithMultipleFailuresFailsWithFirstException() {
    RuntimeException ex1 = new RuntimeException("first");
    RuntimeException ex2 = new RuntimeException("second");

    Future<String> future1 = Future.failed(ex1, directExecutor());
    Future<String> future2 = Future.failed(ex2, directExecutor());

    Future<List<Object>> combined = new FutureCombiner(false, List.of(future1, future2))
            .requireAllSucceed()
            .asList(directExecutor());

    RuntimeException thrown = assertThrows(RuntimeException.class, combined::join);
    assertThat(thrown).isSameAs(ex1);
  }

  @Test
  void combineFuturesSuccessfully() {
    Promise<String> p1 = Future.forPromise(directExecutor());
    Promise<Integer> p2 = Future.forPromise(directExecutor());
    Promise<Boolean> p3 = Future.forPromise(directExecutor());

    Future<List<Object>> combined = Future.combine(p1, p2, p3).asList(directExecutor());

    p1.setSuccess("test");
    p2.setSuccess(42);
    p3.setSuccess(true);

    assertThat(combined.join()).containsExactly("test", 42, true);
  }

  @Test
  void combinedFutureFailsIfAnyInputFails() {
    Promise<String> p1 = Future.forPromise(directExecutor());
    Promise<Integer> p2 = Future.forPromise(directExecutor());

    Future<List<Object>> combined = Future.combine(p1, p2).asList(directExecutor());

    RuntimeException error = new RuntimeException("failed");
    p1.setSuccess("test");
    p2.setFailure(error);

    assertThat(combined.awaitUninterruptibly().getCause()).isEqualTo(error);
  }

  @Test
  void combinedFutureFailsIfAnyInputIsCancelled() {
    Promise<String> p1 = Future.forPromise(directExecutor());
    Promise<Integer> p2 = Future.forPromise(directExecutor());

    Future<List<Object>> combined = Future.combine(p1, p2)
            .asList(directExecutor());

    p1.setSuccess("test");
    p2.cancel();

    assertThat(combined.awaitUninterruptibly().isCancelled()).isTrue();
  }

  @Test
  void combinedEmptyListCompletesImmediately() {
    Future<List<Object>> combined = Future.combine(Collections.emptyList()).asList(directExecutor());
    assertThat(combined.join()).isEmpty();
  }

  @Test
  void combineWithStreamSuccessfully() {
    List<Promise<Integer>> promises = Arrays.asList(
            Future.forPromise(directExecutor()),
            Future.forPromise(directExecutor()),
            Future.forPromise(directExecutor())
    );

    Future<List<Object>> combined = Future.combine(promises.stream()).asList(directExecutor());

    promises.forEach(p -> p.setSuccess(42));

    assertThat(combined.join()).containsOnly(42, 42, 42);
  }

  @Test
  void combineAsVoidSuccessfully() {
    Promise<String> p1 = Future.forPromise(directExecutor());
    Promise<Integer> p2 = Future.forPromise(directExecutor());

    Future<Void> combined = Future.combine(p1, p2).asVoid(directExecutor());

    p1.setSuccess("test");
    p2.setSuccess(42);

    assertThat(combined.join()).isNull();
  }

  @Test
  void combineAsVoidFailsIfAnyInputFails() {
    Promise<String> p1 = Future.forPromise(directExecutor());
    Promise<Integer> p2 = Future.forPromise(directExecutor());

    Future<Void> combined = Future.combine(p1, p2).asVoid(directExecutor());

    RuntimeException error = new RuntimeException("failed");
    p1.setSuccess("test");
    p2.setFailure(error);

    assertThat(combined.awaitUninterruptibly().getCause()).isEqualTo(error);
  }

  @Test
  void cancellingCombinedFutureCancelsAllInputFutures() {
    Promise<String> p1 = Future.forPromise(directExecutor());
    Promise<Integer> p2 = Future.forPromise(directExecutor());

    Future<?> combined = Future.combine(p1, p2).asVoid(directExecutor());

    combined.cancel();

    assertThat(p1.awaitUninterruptibly().isCancelled()).isTrue();
    assertThat(p2.awaitUninterruptibly().isCancelled()).isTrue();
  }

  @Test
  void combinedFutureCompletesAfterAllInputsComplete() {
    Promise<String> p1 = Future.forPromise(directExecutor());
    Promise<Integer> p2 = Future.forPromise(directExecutor());

    Future<List<Object>> combined = Future.combine(p1, p2).asList(directExecutor());

    assertThat(combined.isDone()).isFalse();

    p1.setSuccess("test");
    assertThat(combined.isDone()).isFalse();

    p2.setSuccess(42);
    assertThat(combined.awaitUninterruptibly().isDone()).isTrue();
  }

  @Test
  void combineEmptyCollectionCompletesImmediately() {
    Future<Void> combined = Future.combine(Collections.emptyList()).asVoid(directExecutor());
    assertThat(combined.isDone()).isTrue();
    assertThat(combined.isSuccess()).isTrue();
  }

  @Test
  void combineSuccessfulFuturesCompletesWithSuccess() {
    Promise<String> p1 = Future.forPromise(directExecutor());
    Promise<Integer> p2 = Future.forPromise(directExecutor());

    Future<Void> combined = Future.combine(p1, p2).asVoid(directExecutor());

    p1.setSuccess("test");
    p2.setSuccess(42);

    assertThat(combined.isDone()).isTrue();
    assertThat(combined.isSuccess()).isTrue();
  }

  @Test
  void combineFailedFutureCancelsOthersAndFailsCombined() {
    Promise<String> p1 = Future.forPromise(directExecutor());
    Promise<Integer> p2 = Future.forPromise(directExecutor());

    Future<Void> combined = Future.combine(p1, p2).asVoid(directExecutor());

    Exception error = new RuntimeException("failed");
    p1.setFailure(error);

    assertThat(combined.isDone()).isTrue();
    assertThat(combined.isFailure()).isTrue();
    assertThat(combined.getCause()).isEqualTo(error);
    assertThat(p2.isCancelled()).isTrue();
  }

  @Test
  void combineCancelledFutureCancelsOthersAndCancelsCombined() {
    Promise<String> p1 = Future.forPromise(directExecutor());
    Promise<Integer> p2 = Future.forPromise(directExecutor());

    Future<Void> combined = Future.combine(p1, p2).asVoid(directExecutor());

    p1.cancel();

    assertThat(combined.isDone()).isTrue();
    assertThat(combined.isCancelled()).isTrue();
    assertThat(p2.isCancelled()).isTrue();
  }

  @Test
  void asListCombinesResultsInOrder() {
    Promise<String> p1 = Future.forPromise(directExecutor());
    Promise<String> p2 = Future.forPromise(directExecutor());

    Future<List<String>> combined = Future.combine(p1, p2).asList();

    p1.setSuccess("first");
    p2.setSuccess("second");

    assertThat(combined.join())
            .containsExactly("first", "second");
  }

  @Test
  void asListFailsIfAnyFutureFails() {
    Promise<String> p1 = Future.forPromise(directExecutor());
    Promise<String> p2 = Future.forPromise(directExecutor());

    Future<List<String>> combined = Future.combine(p1, p2).asList(directExecutor());

    Exception error = new RuntimeException("failed");
    p1.setFailure(error);

    assertThat(combined.isDone()).isTrue();
    assertThat(combined.isFailure()).isTrue();
    assertThat(combined.getCause()).isEqualTo(error);
  }

  @Test
  void callWithMapperCombinesResults() {
    Promise<String> p1 = Future.forPromise(directExecutor());
    Promise<Integer> p2 = Future.forPromise(directExecutor());

    Future<String> combined = Future.combine(p1, p2)
            .call(fs -> fs.stream()
                    .map(Future::getNow)
                    .map(String::valueOf)
                    .collect(Collectors.joining(",")));

    p1.setSuccess("test");
    p2.setSuccess(42);

    assertThat(combined.join()).isEqualTo("test,42");
  }

  @Test
  void acceptFailureAllowsFailuresWithoutPropagating() {
    Promise<String> p1 = Future.forPromise(directExecutor());
    Promise<String> p2 = Future.forPromise(directExecutor());

    Future<Void> combined = Future.combine(p1, p2)
            .acceptFailure()
            .asVoid(directExecutor());

    p1.setSuccess("test");
    p2.setFailure(new RuntimeException());

    assertThat(combined.isDone()).isTrue();
    assertThat(combined.isSuccess()).isTrue();
  }

  @Test
  void withAddsFuturesToCombiner() {
    Promise<String> p1 = Future.forPromise(directExecutor());
    Promise<String> p2 = Future.forPromise(directExecutor());
    Promise<String> p3 = Future.forPromise(directExecutor());

    Future<Void> combined = Future.combine(p1)
            .with(p2)
            .with(p3)
            .asVoid(directExecutor());

    p1.setSuccess("1");
    p2.setSuccess("2");
    p3.setSuccess("3");

    assertThat(combined.isDone()).isTrue();
    assertThat(combined.isSuccess()).isTrue();
  }

  @Test
  void withSingleFutureAddsFutureToCollection() {
    Future<Integer> future1 = Future.ok(1);
    Future<Integer> future2 = Future.ok(2);

    var combiner = Future.combine(future1)
            .with(future2);

    assertThat(combiner.asList().join()).containsExactly(1, 2);
  }

  @Test
  void withMultipleFuturesAddsFuturesToCollection() {
    Future<Integer> future1 = Future.ok(1);
    Future<Integer> future2 = Future.ok(2);
    Future<Integer> future3 = Future.ok(3);
    Future<Integer> future4 = Future.ok(4);

    var combiner = Future.combine(future1)
            .with(future2, future3, future4);

    assertThat(combiner.asList().join()).containsExactly(1, 2, 3, 4);
  }

  @Test
  void withCollectionOfFuturesAddsFuturesToCollection() {
    Future<Integer> future1 = Future.ok(1);
    List<Future<?>> futures = Arrays.asList(
            Future.ok(2),
            Future.ok(3),
            Future.ok(4)
    );

    var combiner = Future.combine(future1)
            .with(futures);

    assertThat(combiner.asList().join()).containsExactly(1, 2, 3, 4);
  }

  @Test
  void withEmptyFutureCollectionDoesNotModifyCombiner() {
    Future<Integer> future1 = Future.ok(1);
    List<Future<?>> emptyFutures = Collections.emptyList();

    var combiner = Future.combine(future1)
            .with(emptyFutures);

    assertThat(combiner.asList(null).join()).containsExactly(1);
  }

  @Test
  void withNullFutureThrowsException() {
    Future<Integer> future1 = Future.ok(1);
    Future<Integer> nullFuture = null;

    assertThrows(IllegalArgumentException.class, () -> Future.combine(future1).with(nullFuture));
  }

  @Test
  void withNullFutureCollectionThrowsException() {
    Future<Integer> future1 = Future.ok(1);
    Collection<Future<?>> nullCollection = null;

    assertThrows(IllegalArgumentException.class, () -> Future.combine(future1).with(nullCollection));
  }

}