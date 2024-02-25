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

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @author Mattias Severson
 * @author Juergen Hoeller
 */
class SettableFutureTests {

  private final SettableFuture<String> settableFuture = new SettableFuture<>();

  @Test
  void validateInitialValues() {
    assertThat(settableFuture.isCancelled()).isFalse();
    assertThat(settableFuture.isDone()).isFalse();
  }

  @Test
  void returnsSetValue() throws ExecutionException, InterruptedException {
    String string = "hello";
    assertThat(settableFuture.set(string)).isTrue();
    assertThat(settableFuture.get()).isEqualTo(string);
    assertThat(settableFuture.isCancelled()).isFalse();
    assertThat(settableFuture.isDone()).isTrue();
  }

  @Test
  void returnsSetValueFromCompletable() throws ExecutionException, InterruptedException {
    String string = "hello";
    assertThat(settableFuture.set(string)).isTrue();
    Future<String> completable = settableFuture.completable();
    assertThat(completable.get()).isEqualTo(string);
    assertThat(completable.isCancelled()).isFalse();
    assertThat(completable.isDone()).isTrue();
  }

  @Test
  void setValueUpdatesDoneStatus() {
    settableFuture.set("hello");
    assertThat(settableFuture.isCancelled()).isFalse();
    assertThat(settableFuture.isDone()).isTrue();
  }

  @Test
  void throwsSetExceptionWrappedInExecutionException() throws Exception {
    Throwable exception = new RuntimeException();
    assertThat(settableFuture.setException(exception)).isTrue();

    assertThatExceptionOfType(ExecutionException.class).isThrownBy(
                    settableFuture::get)
            .withCause(exception);

    assertThat(settableFuture.isCancelled()).isFalse();
    assertThat(settableFuture.isDone()).isTrue();
  }

  @Test
  void throwsSetExceptionWrappedInExecutionExceptionFromCompletable() throws Exception {
    Throwable exception = new RuntimeException();
    assertThat(settableFuture.setException(exception)).isTrue();
    Future<String> completable = settableFuture.completable();

    assertThatExceptionOfType(ExecutionException.class).isThrownBy(
                    completable::get)
            .withCause(exception);

    assertThat(completable.isCancelled()).isFalse();
    assertThat(completable.isDone()).isTrue();
  }

  @Test
  void throwsSetErrorWrappedInExecutionException() throws Exception {
    Throwable exception = new OutOfMemoryError();
    assertThat(settableFuture.setException(exception)).isTrue();

    assertThatExceptionOfType(ExecutionException.class).isThrownBy(
                    settableFuture::get)
            .withCause(exception);

    assertThat(settableFuture.isCancelled()).isFalse();
    assertThat(settableFuture.isDone()).isTrue();
  }

  @Test
  void throwsSetErrorWrappedInExecutionExceptionFromCompletable() throws Exception {
    Throwable exception = new OutOfMemoryError();
    assertThat(settableFuture.setException(exception)).isTrue();
    Future<String> completable = settableFuture.completable();

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

    settableFuture.addListener(new FutureListener<String>() {
      @Override
      public void onSuccess(String result) {
        callbackHolder[0] = result;
      }

      @Override
      public void onFailure(Throwable ex) {
        throw new AssertionError("Expected onSuccess() to be called", ex);
      }
    });

    settableFuture.set(string);
    assertThat(callbackHolder[0]).isEqualTo(string);
    assertThat(settableFuture.isCancelled()).isFalse();
    assertThat(settableFuture.isDone()).isTrue();
  }

  @Test
  void setValueTriggersCallbackOnlyOnce() {
    String string = "hello";
    final String[] callbackHolder = new String[1];

    settableFuture.addListener(new FutureListener<String>() {
      @Override
      public void onSuccess(String result) {
        callbackHolder[0] = result;
      }

      @Override
      public void onFailure(Throwable ex) {
        throw new AssertionError("Expected onSuccess() to be called", ex);
      }
    });

    settableFuture.set(string);
    assertThat(settableFuture.set("good bye")).isFalse();
    assertThat(callbackHolder[0]).isEqualTo(string);
    assertThat(settableFuture.isCancelled()).isFalse();
    assertThat(settableFuture.isDone()).isTrue();
  }

  @Test
  void setExceptionTriggersCallback() {
    Throwable exception = new RuntimeException();
    final Throwable[] callbackHolder = new Throwable[1];

    settableFuture.addListener(new FutureListener<String>() {
      @Override
      public void onSuccess(String result) {
        fail("Expected onFailure() to be called");
      }

      @Override
      public void onFailure(Throwable ex) {
        callbackHolder[0] = ex;
      }
    });

    settableFuture.setException(exception);
    assertThat(callbackHolder[0]).isEqualTo(exception);
    assertThat(settableFuture.isCancelled()).isFalse();
    assertThat(settableFuture.isDone()).isTrue();
  }

  @Test
  void setExceptionTriggersCallbackOnlyOnce() {
    Throwable exception = new RuntimeException();
    final Throwable[] callbackHolder = new Throwable[1];

    settableFuture.addListener(new FutureListener<String>() {
      @Override
      public void onSuccess(String result) {
        fail("Expected onFailure() to be called");
      }

      @Override
      public void onFailure(Throwable ex) {
        callbackHolder[0] = ex;
      }
    });

    settableFuture.setException(exception);
    assertThat(settableFuture.setException(new IllegalArgumentException())).isFalse();
    assertThat(callbackHolder[0]).isEqualTo(exception);
    assertThat(settableFuture.isCancelled()).isFalse();
    assertThat(settableFuture.isDone()).isTrue();
  }

  @Test
  void nullIsAcceptedAsValueToSet() throws ExecutionException, InterruptedException {
    settableFuture.set(null);
    assertThat((Object) settableFuture.get()).isNull();
    assertThat(settableFuture.isCancelled()).isFalse();
    assertThat(settableFuture.isDone()).isTrue();
  }

  @Test
  void getWaitsForCompletion() throws ExecutionException, InterruptedException {
    final String string = "hello";

    new Thread(() -> {
      try {
        Thread.sleep(20L);
        settableFuture.set(string);
      }
      catch (InterruptedException ex) {
        throw new RuntimeException(ex);
      }
    }).start();

    String value = settableFuture.get();
    assertThat(value).isEqualTo(string);
    assertThat(settableFuture.isCancelled()).isFalse();
    assertThat(settableFuture.isDone()).isTrue();
  }

  @Test
  void getWithTimeoutThrowsTimeoutException() throws ExecutionException, InterruptedException {
    assertThatExceptionOfType(TimeoutException.class).isThrownBy(() ->
            settableFuture.get(1L, TimeUnit.MILLISECONDS));
  }

  @Test
  void getWithTimeoutWaitsForCompletion() throws ExecutionException, InterruptedException, TimeoutException {
    final String string = "hello";

    new Thread(() -> {
      try {
        Thread.sleep(20L);
        settableFuture.set(string);
      }
      catch (InterruptedException ex) {
        throw new RuntimeException(ex);
      }
    }).start();

    String value = settableFuture.get(500L, TimeUnit.MILLISECONDS);
    assertThat(value).isEqualTo(string);
    assertThat(settableFuture.isCancelled()).isFalse();
    assertThat(settableFuture.isDone()).isTrue();
  }

  @Test
  void cancelPreventsValueFromBeingSet() {
    assertThat(settableFuture.cancel(true)).isTrue();
    assertThat(settableFuture.set("hello")).isFalse();
    assertThat(settableFuture.isCancelled()).isTrue();
    assertThat(settableFuture.isDone()).isTrue();
  }

  @Test
  void cancelSetsFutureToDone() {
    settableFuture.cancel(true);
    assertThat(settableFuture.isCancelled()).isTrue();
    assertThat(settableFuture.isDone()).isTrue();
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
    assertThat(settableFuture.set("hello")).isTrue();
    assertThat(settableFuture.cancel(true)).isFalse();
    assertThat(settableFuture.isCancelled()).isFalse();
    assertThat(settableFuture.isDone()).isTrue();
  }

  @Test
  void cancelPreventsExceptionFromBeingSet() {
    assertThat(settableFuture.cancel(true)).isTrue();
    assertThat(settableFuture.setException(new RuntimeException())).isFalse();
    assertThat(settableFuture.isCancelled()).isTrue();
    assertThat(settableFuture.isDone()).isTrue();
  }

  @Test
  void setExceptionPreventsCancel() {
    assertThat(settableFuture.setException(new RuntimeException())).isTrue();
    assertThat(settableFuture.cancel(true)).isFalse();
    assertThat(settableFuture.isCancelled()).isFalse();
    assertThat(settableFuture.isDone()).isTrue();
  }

  @Test
  void cancelStateThrowsExceptionWhenCallingGet() throws ExecutionException, InterruptedException {
    settableFuture.cancel(true);

    assertThatExceptionOfType(CancellationException.class).isThrownBy(() ->
            settableFuture.get());

    assertThat(settableFuture.isCancelled()).isTrue();
    assertThat(settableFuture.isDone()).isTrue();
  }

  @Test
  void cancelStateThrowsExceptionWhenCallingGetWithTimeout() throws ExecutionException, TimeoutException, InterruptedException {
    new Thread(() -> {
      try {
        Thread.sleep(20L);
        settableFuture.cancel(true);
      }
      catch (InterruptedException ex) {
        throw new RuntimeException(ex);
      }
    }).start();

    assertThatExceptionOfType(CancellationException.class)
            .isThrownBy(() -> settableFuture.get(500L, TimeUnit.MILLISECONDS));

    assertThat(settableFuture.isCancelled()).isTrue();
    assertThat(settableFuture.isDone()).isTrue();
  }

  @Test
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void cancelDoesNotNotifyCallbacksOnSet() {
    FutureListener callback = mock(FutureListener.class);
    settableFuture.addListener(callback);
    settableFuture.cancel(true);

    verify(callback).onFailure(any(CancellationException.class));
    verifyNoMoreInteractions(callback);

    settableFuture.set("hello");
    verifyNoMoreInteractions(callback);

    assertThat(settableFuture.isCancelled()).isTrue();
    assertThat(settableFuture.isDone()).isTrue();
  }

  @Test
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void cancelDoesNotNotifyCallbacksOnSetException() {
    FutureListener callback = mock(FutureListener.class);
    settableFuture.addListener(callback);
    settableFuture.cancel(true);

    verify(callback).onFailure(any(CancellationException.class));
    verifyNoMoreInteractions(callback);

    settableFuture.setException(new RuntimeException());
    verifyNoMoreInteractions(callback);

    assertThat(settableFuture.isCancelled()).isTrue();
    assertThat(settableFuture.isDone()).isTrue();
  }

  private static class InterruptibleSettableFuture extends SettableFuture<String> {

    private boolean interrupted = false;

    @Override
    protected void interruptTask() {
      interrupted = true;
    }

    boolean calledInterruptTask() {
      return interrupted;
    }
  }

}
