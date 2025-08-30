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

import org.junit.jupiter.api.Test;

import java.util.concurrent.CancellationException;

import infra.util.concurrent.Future;
import infra.util.concurrent.Promise;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/8/12 20:59
 */
class FutureMonoTests {

  @Test
  void deferredFutureFailedCompletion() {
    RuntimeException exception = new RuntimeException("deferred boom");

    StepVerifier.create(FutureMono.deferFuture(() -> {
              throw exception;
            }))
            .verifyErrorMatches(e -> e == exception);
  }

  @Test
  void deferredFutureSupplierReturnsNull() {
    StepVerifier.create(FutureMono.deferFuture(() -> null))
            .expectError(NullPointerException.class)
            .verify();
  }

  @Test
  void nullFutureThrowsException() {
    assertThatThrownBy(() -> FutureMono.of(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("future is required");
  }

  @Test
  void nullDeferredSupplierThrowsException() {
    assertThatThrownBy(() -> FutureMono.deferFuture(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("deferredFuture is required");
  }

  @Test
  void failedImmediateFutureCompletion() {
    Promise<String> future = Future.forPromise();
    RuntimeException exception = new RuntimeException("boom");
    future.setFailure(exception);

    StepVerifier.create(FutureMono.of(future))
            .verifyErrorMatches(e -> e == exception);
  }

  @Test
  void deferredFutureSuccessfulCompletion() {
    Promise<String> future = Future.forPromise();

    StepVerifier.create(FutureMono.deferFuture(() -> {
              future.setSuccess("deferred success");
              return future;
            }))
            .expectNext("deferred success")
            .verifyComplete();
  }

  @Test
  void futureCompletesAfterSubscription() {
    Promise<String> future = Future.forPromise();

    StepVerifier.create(FutureMono.of(future))
            .then(() -> future.setSuccess("delayed"))
            .expectNext("delayed")
            .verifyComplete();
  }

  @Test
  void futureFailsAfterSubscription() {
    Promise<String> future = Future.forPromise();
    RuntimeException exception = new RuntimeException("delayed boom");

    StepVerifier.create(FutureMono.of(future))
            .then(() -> future.setFailure(exception))
            .verifyErrorMatches(e -> e == exception);
  }

  @Test
  void cancelFutureSubscription() {
    Promise<String> future = Future.forPromise();

    StepVerifier.create(FutureMono.of(future))
            .thenCancel()
            .verify();
  }

  @Test
  void concurrentCompletionAndCancellation() {
    Promise<String> future = Future.forPromise();

    StepVerifier.create(FutureMono.of(future))
            .then(() -> {
              future.setSuccess("success");
              future.cancel();
            })
            .expectNext("success")
            .verifyComplete();
  }

  @Test
  void multipleSubscribersToSameFuture() {
    Promise<String> future = Future.forPromise();

    StepVerifier first = StepVerifier.create(FutureMono.of(future))
            .expectNext("shared")
            .expectComplete()
            .verifyLater();

    StepVerifier second = StepVerifier.create(FutureMono.of(future))
            .expectNext("shared")
            .expectComplete()
            .verifyLater();

    future.setSuccess("shared");

    first.verify();
    second.verify();
  }

  @Test
  void deferredFutureSupplierThrowsRuntimeException() {
    RuntimeException exception = new IllegalStateException("boom");
    StepVerifier.create(FutureMono.deferFuture(() -> {
              throw exception;
            }))
            .verifyErrorMatches(e -> e == exception);
  }

  @Test
  void deferredFutureSupplierReturnsAlreadyCompletedFuture() {
    Promise<String> completed = Future.forPromise();
    completed.setSuccess("done");

    StepVerifier.create(FutureMono.deferFuture(() -> completed))
            .expectNext("done")
            .verifyComplete();
  }

  @Test
  void immediateEmptyFutureCompletion() {
    Promise<String> future = Future.forPromise();
    future.setSuccess(null);

    StepVerifier.create(FutureMono.of(future))
            .verifyComplete();
  }

  @Test
  void emptyCompletedFutureDoesNotEmitValue() {
    Promise<String> future = Future.forPromise();
    future.setSuccess(null);

    StepVerifier.create(FutureMono.of(future))
            .expectNextCount(0)
            .verifyComplete();
  }

  @Test
  void futureCompletesWithNullInDeferredCase() {
    Promise<String> future = Future.forPromise();

    StepVerifier.create(FutureMono.deferFuture(() -> {
              future.setSuccess(null);
              return future;
            }))
            .verifyComplete();
  }

  @Test
  void backpressureIsRespected() {
    Promise<String> future = Future.forPromise();
    future.setSuccess("value");

    StepVerifier.create(FutureMono.of(future), 0)
            .expectSubscription()
            .thenRequest(1)
            .expectNext("value")
            .verifyComplete();
  }

  @Test
  void deferredFutureWithImmediateSuccessCompletesCorrectly() {
    Promise<String> future = Future.forPromise();
    future.setSuccess("immediate");

    StepVerifier.create(FutureMono.deferFuture(() -> future))
            .expectNext("immediate")
            .verifyComplete();
  }

  @Test
  void multipleConcurrentSubscriptionsGetIndependentResults() {
    Promise<String> future1 = Future.forPromise();
    Promise<String> future2 = Future.forPromise();

    StepVerifier first = StepVerifier.create(FutureMono.of(future1))
            .expectNext("result1")
            .expectComplete()
            .verifyLater();

    StepVerifier second = StepVerifier.create(FutureMono.of(future2))
            .expectNext("result2")
            .expectComplete()
            .verifyLater();

    future1.setSuccess("result1");
    future2.setSuccess("result2");

    first.verify();
    second.verify();
  }

  @Test
  void successfulCompletionWithEmptyStringIsEmitted() {
    Promise<String> future = Future.forPromise();
    future.setSuccess("");

    StepVerifier.create(FutureMono.of(future))
            .expectNext("")
            .verifyComplete();
  }

  @Test
  void deferredFutureCompletesWithNullValue() {
    Promise<String> future = Future.forPromise();
    future.setSuccess(null);

    StepVerifier.create(FutureMono.deferFuture(() -> future))
            .expectNextCount(0)
            .verifyComplete();
  }

  @Test
  void completingWithNullAfterRequestDoesNotEmitValue() {
    Promise<String> future = Future.forPromise();

    StepVerifier.create(FutureMono.of(future))
            .then(() -> future.setSuccess(null))
            .expectNextCount(0)
            .verifyComplete();
  }

  @Test
  void nullCompletionWithMultipleSubscribers() {
    Promise<String> future = Future.forPromise();

    StepVerifier first = StepVerifier.create(FutureMono.of(future))
            .expectNextCount(0)
            .expectComplete()
            .verifyLater();

    StepVerifier second = StepVerifier.create(FutureMono.of(future))
            .expectNextCount(0)
            .expectComplete()
            .verifyLater();

    future.setSuccess(null);

    first.verify();
    second.verify();
  }

  @Test
  void multipleRequestsAfterFutureCompletionOnlyEmitsOnce() {
    Promise<String> future = Future.forPromise();
    future.setSuccess("value");

    StepVerifier.create(FutureMono.of(future))
            .thenRequest(1)
            .expectNext("value")
            .thenRequest(2)
            .verifyComplete();
  }

  @Test
  void concurrentRequestsAndCompletions() {
    Promise<String> future = Future.forPromise();

    StepVerifier.create(FutureMono.of(future))
            .then(() -> {
              future.setSuccess("result");
              future.trySuccess("ignored");
            })
            .thenRequest(1)
            .expectNext("result")
            .verifyComplete();
  }

  @Test
  void deferredFutureWithNullValueAndMultipleRequests() {
    Promise<String> future = Future.forPromise();

    StepVerifier.create(FutureMono.deferFuture(() -> {
              future.setSuccess(null);
              return future;
            }))
            .thenRequest(1)
            .thenRequest(2)
            .expectNextCount(0)
            .verifyComplete();
  }

  @Test
  void subscribeAfterCancellation() {
    Promise<String> future = Future.forPromise();
    future.cancel();

    StepVerifier.create(FutureMono.of(future))
            .verifyError(CancellationException.class);
  }

}