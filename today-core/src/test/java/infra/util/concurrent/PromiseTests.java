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

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @author Mattias Severson
 * @author Juergen Hoeller
 */
class PromiseTests {

  private final Promise<String> promise = Future.forPromise(Runnable::run);

  @Test
  void validateInitialValues() {
    assertThat(promise.isCancelled()).isFalse();
    assertThat(promise.isDone()).isFalse();
  }

  @Test
  void returnsSetValue() throws ExecutionException, InterruptedException {
    String string = "hello";
    assertThat(promise.trySuccess(string)).isTrue();
    assertThat(promise.get()).isEqualTo(string);
    assertThat(promise.isCancelled()).isFalse();
    assertThat(promise.isDone()).isTrue();
  }

  @Test
  void returnsSetValueFromCompletable() throws ExecutionException, InterruptedException {
    String string = "hello";
    assertThat(promise.trySuccess(string)).isTrue();
    var completable = promise.completable();
    assertThat(completable.get()).isEqualTo(string);
    assertThat(completable.isCancelled()).isFalse();
    assertThat(completable.isDone()).isTrue();
  }

  @Test
  void setValueUpdatesDoneStatus() {
    promise.trySuccess("hello");
    assertThat(promise.isCancelled()).isFalse();
    assertThat(promise.isDone()).isTrue();
  }

  @Test
  void throwsSetExceptionWrappedInExecutionException() throws Exception {
    Throwable exception = new RuntimeException();
    assertThat(promise.tryFailure(exception)).isTrue();

    assertThatExceptionOfType(ExecutionException.class).isThrownBy(
                    promise::get)
            .withCause(exception);

    assertThat(promise.isCancelled()).isFalse();
    assertThat(promise.isDone()).isTrue();
  }

  @Test
  void throwsSetExceptionWrappedInExecutionExceptionFromCompletable() throws Exception {
    Throwable exception = new RuntimeException();
    assertThat(promise.tryFailure(exception)).isTrue();
    CompletableFuture<String> completable = promise.completable();

    assertThatExceptionOfType(ExecutionException.class).isThrownBy(
                    completable::get)
            .withCause(exception);

    assertThat(completable.isCancelled()).isFalse();
    assertThat(completable.isDone()).isTrue();
  }

  @Test
  void throwsSetErrorWrappedInExecutionException() throws Exception {
    Throwable exception = new OutOfMemoryError();
    assertThat(promise.tryFailure(exception)).isTrue();

    assertThatExceptionOfType(ExecutionException.class).isThrownBy(
                    promise::get)
            .withCause(exception);

    assertThat(promise.isCancelled()).isFalse();
    assertThat(promise.isDone()).isTrue();
  }

  @Test
  void throwsSetErrorWrappedInExecutionExceptionFromCompletable() throws Exception {
    Throwable exception = new OutOfMemoryError();
    assertThat(promise.tryFailure(exception)).isTrue();
    CompletableFuture<String> completable = promise.completable();

    assertThatExceptionOfType(ExecutionException.class).isThrownBy(
                    completable::get)
            .withCause(exception);

    assertThat(completable.isCancelled()).isFalse();
    assertThat(completable.isDone()).isTrue();
  }

  @Test
  void setValueTriggersCallback() {
    String string = "hello";
    final String[] callbackHolder = new String[1];

    promise.onCompleted(future -> {
      if (future.isSuccess()) {
        callbackHolder[0] = future.getNow();
        fail("Expected onFailure() to be called");
      }
      else {
        throw new AssertionError("Expected onSuccess() to be called", future.getCause());
      }
    });

    promise.trySuccess(string);
    assertThat(callbackHolder[0]).isEqualTo(string);
    assertThat(promise.isCancelled()).isFalse();
    assertThat(promise.isDone()).isTrue();
  }

  @Test
  void setValueTriggersCallbackOnlyOnce() {
    String string = "hello";
    final String[] callbackHolder = new String[1];

    promise.onCompleted(future -> {
      if (future.isSuccess()) {
        callbackHolder[0] = future.getNow();
        fail("Expected onFailure() to be called");
      }
      else {
        throw new AssertionError("Expected onSuccess() to be called", future.getCause());
      }
    });

    promise.trySuccess(string);
    assertThat(promise.trySuccess("good bye")).isFalse();
    assertThat(callbackHolder[0]).isEqualTo(string);
    assertThat(promise.isCancelled()).isFalse();
    assertThat(promise.isDone()).isTrue();
  }

  @Test
  void setExceptionTriggersCallback() {
    Throwable exception = new RuntimeException();
    final Throwable[] callbackHolder = new Throwable[1];

    promise.onCompleted(future -> {
      if (future.isSuccess()) {
        fail("Expected onFailure() to be called");
      }
      else {
        callbackHolder[0] = future.getCause();
      }
    });

    promise.tryFailure(exception);
    assertThat(callbackHolder[0]).isEqualTo(exception);
    assertThat(promise.isCancelled()).isFalse();
    assertThat(promise.isDone()).isTrue();
  }

  @Test
  void setExceptionTriggersCallbackOnlyOnce() {
    Throwable exception = new RuntimeException();
    final Throwable[] callbackHolder = new Throwable[1];

    promise.onCompleted(future -> {
      if (future.isSuccess()) {
        fail("Expected onFailure() to be called");
      }
      else {
        callbackHolder[0] = future.getCause();
      }
    });

    promise.tryFailure(exception);
    assertThat(promise.tryFailure(new IllegalArgumentException())).isFalse();
    assertThat(callbackHolder[0]).isEqualTo(exception);
    assertThat(promise.isCancelled()).isFalse();
    assertThat(promise.isDone()).isTrue();
  }

  @Test
  void nullIsAcceptedAsValueToSet() throws ExecutionException, InterruptedException {
    promise.trySuccess(null);
    assertThat((Object) promise.get()).isNull();
    assertThat(promise.isCancelled()).isFalse();
    assertThat(promise.isDone()).isTrue();
  }

  @Test
  void getWaitsForCompletion() throws ExecutionException, InterruptedException {
    final String string = "hello";

    new Thread(() -> {
      try {
        Thread.sleep(20L);
        promise.trySuccess(string);
      }
      catch (InterruptedException ex) {
        throw new RuntimeException(ex);
      }
    }).start();

    String value = promise.get();
    assertThat(value).isEqualTo(string);
    assertThat(promise.isCancelled()).isFalse();
    assertThat(promise.isDone()).isTrue();
  }

  @Test
  void getWithTimeoutThrowsTimeoutException() throws ExecutionException, InterruptedException {
    assertThatExceptionOfType(TimeoutException.class).isThrownBy(() ->
            promise.get(1L, TimeUnit.MILLISECONDS));
  }

  @Test
  void getWithTimeoutWaitsForCompletion() throws ExecutionException, InterruptedException, TimeoutException {
    final String string = "hello";

    new Thread(() -> {
      try {
        Thread.sleep(20L);
        promise.trySuccess(string);
      }
      catch (InterruptedException ex) {
        throw new RuntimeException(ex);
      }
    }).start();

    String value = promise.get(500L, TimeUnit.MILLISECONDS);
    assertThat(value).isEqualTo(string);
    assertThat(promise.isCancelled()).isFalse();
    assertThat(promise.isDone()).isTrue();
  }

  @Test
  void cancelPreventsValueFromBeingSet() {
    assertThat(promise.cancel(true)).isTrue();
    assertThat(promise.trySuccess("hello")).isFalse();
    assertThat(promise.isCancelled()).isTrue();
    assertThat(promise.isDone()).isTrue();
  }

  @Test
  void cancelSetsFutureToDone() {
    promise.cancel(true);
    assertThat(promise.isCancelled()).isTrue();
    assertThat(promise.isDone()).isTrue();
  }

  @Test
  void cancelWithMayInterruptIfRunningTrueCallsOverriddenMethod() {
    InterruptibleSettableFuture interruptibleFuture = new InterruptibleSettableFuture();
    assertThat(interruptibleFuture.cancel(true)).isTrue();
    assertThat(interruptibleFuture.calledInterruptTask()).isTrue();
    assertThat(interruptibleFuture.isCancelled()).isTrue();
    assertThat(interruptibleFuture.isDone()).isTrue();
  }

  @Test
  void cancelWithMayInterruptIfRunningFalseDoesNotCallOverriddenMethod() {
    InterruptibleSettableFuture interruptibleFuture = new InterruptibleSettableFuture();
    assertThat(interruptibleFuture.cancel(false)).isTrue();
    assertThat(interruptibleFuture.calledInterruptTask()).isFalse();
    assertThat(interruptibleFuture.isCancelled()).isTrue();
    assertThat(interruptibleFuture.isDone()).isTrue();
  }

  @Test
  void setPreventsCancel() {
    assertThat(promise.trySuccess("hello")).isTrue();
    assertThat(promise.cancel(true)).isFalse();
    assertThat(promise.isCancelled()).isFalse();
    assertThat(promise.isDone()).isTrue();
  }

  @Test
  void cancelPreventsExceptionFromBeingSet() {
    assertThat(promise.cancel(true)).isTrue();
    assertThat(promise.tryFailure(new RuntimeException())).isFalse();
    assertThat(promise.isCancelled()).isTrue();
    assertThat(promise.isDone()).isTrue();
  }

  @Test
  void setExceptionPreventsCancel() {
    assertThat(promise.tryFailure(new RuntimeException())).isTrue();
    assertThat(promise.cancel(true)).isFalse();
    assertThat(promise.isCancelled()).isFalse();
    assertThat(promise.isDone()).isTrue();
  }

  @Test
  void cancelStateThrowsExceptionWhenCallingGet() throws ExecutionException, InterruptedException {
    promise.cancel(true);

    assertThatExceptionOfType(CancellationException.class).isThrownBy(promise::get);

    assertThat(promise.isCancelled()).isTrue();
    assertThat(promise.isDone()).isTrue();
  }

  @Test
  void cancelStateThrowsExceptionWhenCallingGetWithTimeout() throws ExecutionException, TimeoutException, InterruptedException {
    new Thread(() -> {
      try {
        Thread.sleep(20L);
        promise.cancel(true);
      }
      catch (InterruptedException ex) {
        throw new RuntimeException(ex);
      }
    }).start();

    assertThatExceptionOfType(CancellationException.class)
            .isThrownBy(() -> promise.get(500L, TimeUnit.MILLISECONDS));

    assertThat(promise.isCancelled()).isTrue();
    assertThat(promise.isDone()).isTrue();
  }

  @Test
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void cancelDoesNotNotifyCallbacksOnSet() throws Throwable {
    FutureListener callback = mock(FutureListener.class);
    promise.onCompleted(callback);
    promise.cancel(true);

    verify(callback).operationComplete(promise);
    verifyNoMoreInteractions(callback);

    promise.trySuccess("hello");
    verifyNoMoreInteractions(callback);

    assertThat(promise.isCancelled()).isTrue();
    assertThat(promise.isDone()).isTrue();
  }

  @Test
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void cancelDoesNotNotifyCallbacksOnSetException() throws Throwable {
    FutureListener callback = mock(FutureListener.class);
    promise.onCompleted(callback);
    promise.cancel(true);

    verify(callback).operationComplete(promise);

    verifyNoMoreInteractions(callback);

    promise.tryFailure(new RuntimeException());
    verifyNoMoreInteractions(callback);

    assertThat(promise.isCancelled()).isTrue();
    assertThat(promise.isDone()).isTrue();
  }

  @Test
  void setSuccess() {
    Promise<Void> settable = Future.create(promise -> promise.setSuccess(null));
    assertThatThrownBy(() -> settable.setSuccess(null))
            .hasMessageStartingWith("complete already:");
  }

  @Test
  void setFailure() {
    Promise<Void> settable = Future.create(promise -> promise.setSuccess(null));
    assertThatThrownBy(() -> settable.setFailure(new RuntimeException()))
            .hasMessageStartingWith("complete already:");
  }

  private static class InterruptibleSettableFuture extends Promise<String> {

    private boolean interrupted = false;

    /**
     * Creates a new instance.
     */
    InterruptibleSettableFuture() {
      super(defaultScheduler);
    }

    @Override
    protected void interruptTask() {
      super.interruptTask();
      interrupted = true;
    }

    boolean calledInterruptTask() {
      return interrupted;
    }
  }

}
