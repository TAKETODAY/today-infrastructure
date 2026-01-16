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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    Future<String> future1 = Future.ok("result1", directExecutor());
    Promise<String> future2 = Future.forPromise(directExecutor());
    future2.cancel();

    Future<List<Object>> combined = new FutureCombiner(false, List.of(future1, future2))
            .requireAllSucceed()
            .asList(directExecutor());

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
  void withStreamOfFuturesAddsFuturesToCollection() {
    Future<Integer> future1 = Future.ok(1);
    Stream<Future<?>> futures = Stream.of(
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
  void withEmptyFutureStreamDoesNotModifyCombiner() {
    Future<Integer> future1 = Future.ok(1);
    Stream<Future<?>> emptyFutures = Collections.<Future<?>>emptyList().stream();

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

  @Test
  void withNullFutureStreamThrowsException() {
    Future<Integer> future1 = Future.ok(1);
    Stream<Future<?>> futureStream = null;
    assertThrows(IllegalArgumentException.class, () -> Future.combine(future1).with(futureStream));
  }

}