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

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 */
@SuppressWarnings("unchecked")
class ListenableFutureTaskTests {

  @Test
  void success() throws Exception {
    final String s = "Hello World";
    Callable<String> callable = () -> s;

    ListenableFutureTask<String> task = Future.forFutureTask(callable);
    task.onCompleted(future -> {
      if (future.isSuccess()) {
        assertThat(future.getNow()).isEqualTo(s);
      }
      else {
        throw new AssertionError(future.getCause().getMessage(), future.getCause());
      }
    });

    task.run();

    assertThat(task.get()).isSameAs(s);
    assertThat(task.completable().get()).isSameAs(s);
    task.completable().thenAccept(v -> assertThat(v).isSameAs(s));
  }

  @Test
  void failure() throws Exception {
    final String s = "Hello World";
    Callable<String> callable = () -> {
      throw new IOException(s);
    };

    ListenableFutureTask<String> task = Future.forFutureTask(callable);

    task.onCompleted(future -> {
      if (future.isSuccess()) {
        fail("onSuccess not expected");
      }
      else {
        assertThat(future.getCause().getMessage()).isEqualTo(s);
      }
    });

    task.run();

    assertThatExceptionOfType(ExecutionException.class)
            .isThrownBy(task::get)
            .havingCause()
            .withMessage(s);
    assertThatExceptionOfType(ExecutionException.class)
            .isThrownBy(task.completable()::get)
            .havingCause()
            .withMessage(s);
  }

  @Test
  void successWithLambdas() throws Throwable {
    final String s = "Hello World";
    Callable<String> callable = () -> s;

    SuccessCallback<String> successCallback = mock(SuccessCallback.class);
    FailureCallback failureCallback = mock(FailureCallback.class);
    ListenableFutureTask<String> task = Future.forFutureTask(callable, Runnable::run);
    task.onCompleted(successCallback, failureCallback);
    task.run();
    verify(successCallback).onSuccess(s);
    verifyNoInteractions(failureCallback);

    assertThat(task.get()).isSameAs(s);
    assertThat(task.completable().get()).isSameAs(s);
    task.completable().thenAccept(v -> assertThat(v).isSameAs(s));
  }

  @Test
  void failureWithLambdas() throws Throwable {
    final String s = "Hello World";
    IOException ex = new IOException(s);
    Callable<String> callable = () -> {
      throw ex;
    };

    SuccessCallback<String> successCallback = mock(SuccessCallback.class);
    FailureCallback failureCallback = mock(FailureCallback.class);
    ListenableFutureTask<String> task = Future.forFutureTask(callable, Runnable::run);
    task.onCompleted(successCallback, failureCallback);
    task.run();
    verify(failureCallback).onFailure(ex);
    verifyNoInteractions(successCallback);

    assertThatExceptionOfType(ExecutionException.class).isThrownBy(task::get)
            .satisfies(e -> assertThat(e.getCause().getMessage()).isEqualTo(s));

    assertThatExceptionOfType(ExecutionException.class).isThrownBy(task.completable()::get)
            .satisfies(e -> assertThat(e.getCause().getMessage()).isEqualTo(s));
  }

  @Test
  void cascadeTo() throws InterruptedException {
    ListenableFutureTask<Integer> futureTask = Future.run(() -> 1);
    assertThat(futureTask.await().getNow()).isEqualTo(1);

    Promise<Integer> promise = Future.forPromise();
    futureTask.cascadeTo(promise);

    assertThat(promise.await().getNow()).isEqualTo(1);
  }

  @Test
  void cancelBeforeExecution() {
    ListenableFutureTask<String> task = Future.forFutureTask(() -> "test", Runnable::run);
    task.cancel(true);

    assertThat(task.isCancelled()).isTrue();
    assertThat(task.isDone()).isTrue();
    assertThatExceptionOfType(CancellationException.class).isThrownBy(task::get);
  }

  @Test
  void nullCallableThrowsException() {
    assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> Future.forFutureTask((Callable<? extends Object>) null));
  }

  @Test
  void interruptedExecution() throws Exception {
    ListenableFutureTask<String> task = Future.forFutureTask(() -> {
      Thread.sleep(1000);
      return "test";
    }, Runnable::run);

    Thread thread = new Thread(task);
    thread.start();
    thread.interrupt();

    assertThatExceptionOfType(ExecutionException.class)
            .isThrownBy(task::get)
            .withCauseInstanceOf(InterruptedException.class);
  }

  @Test
  void multipleListenersNotified() {
    AtomicInteger counter = new AtomicInteger();
    ListenableFutureTask<Integer> task = Future.forFutureTask(() -> 42, Runnable::run);

    task.onCompleted(f -> counter.incrementAndGet());
    task.onCompleted(f -> counter.incrementAndGet());
    task.onCompleted(f -> counter.incrementAndGet());

    task.run();

    assertThat(counter.get()).isEqualTo(3);
  }

  @Test
  void completedBeforeListenerAdded() {
    ListenableFutureTask<String> task = Future.forFutureTask(() -> "test", Runnable::run);
    task.run();

    AtomicBoolean listenerCalled = new AtomicBoolean();
    task.onCompleted(f -> listenerCalled.set(true));

    assertThat(listenerCalled.get()).isTrue();
  }

  @Test
  void taskReturnsNullSuccessfully() {
    ListenableFutureTask<String> task = Future.forFutureTask(() -> null, Runnable::run);
    task.run();
    assertThat(task.getNow()).isNull();
    assertThat(task.isSuccess()).isTrue();
  }

  @Test
  void runtimeExceptionPropagatedToListeners() throws Throwable {
    RuntimeException expected = new RuntimeException("test");
    ListenableFutureTask<String> task = Future.forFutureTask(() -> {
      throw expected;
    }, Runnable::run);

    FailureCallback failureCallback = mock(FailureCallback.class);
    task.onCompleted(s -> fail("Should not succeed"), failureCallback);

    task.run();

    verify(failureCallback).onFailure(expected);
  }

  @Test
  void listenerThrowingExceptionDoesNotAffectOtherListeners() {
    AtomicInteger callCount = new AtomicInteger();
    ListenableFutureTask<String> task = Future.forFutureTask(() -> "test", Runnable::run);

    task.onCompleted(f -> { throw new RuntimeException("oops"); });
    task.onCompleted(f -> callCount.incrementAndGet());
    task.onCompleted(f -> callCount.incrementAndGet());

    task.run();

    assertThat(callCount.get()).isEqualTo(2);
  }

  @Test
  void cancelWithInterruptFalseDoesNotInterruptThread() throws Exception {
    AtomicBoolean interrupted = new AtomicBoolean();
    ListenableFutureTask<String> task = Future.forFutureTask(() -> {
      Thread.sleep(1000);
      if (Thread.interrupted()) {
        interrupted.set(true);
      }
      return "test";
    });

    Thread thread = new Thread(task);
    thread.start();
    task.cancel(false);
    thread.join();

    assertThat(interrupted.get()).isFalse();
    assertThat(task.awaitUninterruptibly().isCancelled()).isTrue();
  }

  @Test
  void notCompletedStringContainsTaskInfo() {
    ListenableFutureTask<String> task = Future.forFutureTask(() -> "test");
    assertThat(task.notCompletedString()).contains("task =");
  }

}
