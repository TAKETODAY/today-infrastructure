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
class SettableListenableFutureTests {

  private final SettableListenableFuture<String> settableListenableFuture = new SettableListenableFuture<>();

  @Test
  void validateInitialValues() {
    assertThat(settableListenableFuture.isCancelled()).isFalse();
    assertThat(settableListenableFuture.isDone()).isFalse();
  }

  @Test
  void returnsSetValue() throws ExecutionException, InterruptedException {
    String string = "hello";
    assertThat(settableListenableFuture.set(string)).isTrue();
    assertThat(settableListenableFuture.get()).isEqualTo(string);
    assertThat(settableListenableFuture.isCancelled()).isFalse();
    assertThat(settableListenableFuture.isDone()).isTrue();
  }

  @Test
  void returnsSetValueFromCompletable() throws ExecutionException, InterruptedException {
    String string = "hello";
    assertThat(settableListenableFuture.set(string)).isTrue();
    Future<String> completable = settableListenableFuture.completable();
    assertThat(completable.get()).isEqualTo(string);
    assertThat(completable.isCancelled()).isFalse();
    assertThat(completable.isDone()).isTrue();
  }

  @Test
  void setValueUpdatesDoneStatus() {
    settableListenableFuture.set("hello");
    assertThat(settableListenableFuture.isCancelled()).isFalse();
    assertThat(settableListenableFuture.isDone()).isTrue();
  }

  @Test
  void throwsSetExceptionWrappedInExecutionException() throws Exception {
    Throwable exception = new RuntimeException();
    assertThat(settableListenableFuture.setException(exception)).isTrue();

    assertThatExceptionOfType(ExecutionException.class).isThrownBy(
                    settableListenableFuture::get)
            .withCause(exception);

    assertThat(settableListenableFuture.isCancelled()).isFalse();
    assertThat(settableListenableFuture.isDone()).isTrue();
  }

  @Test
  void throwsSetExceptionWrappedInExecutionExceptionFromCompletable() throws Exception {
    Throwable exception = new RuntimeException();
    assertThat(settableListenableFuture.setException(exception)).isTrue();
    Future<String> completable = settableListenableFuture.completable();

    assertThatExceptionOfType(ExecutionException.class).isThrownBy(
                    completable::get)
            .withCause(exception);

    assertThat(completable.isCancelled()).isFalse();
    assertThat(completable.isDone()).isTrue();
  }

  @Test
  void throwsSetErrorWrappedInExecutionException() throws Exception {
    Throwable exception = new OutOfMemoryError();
    assertThat(settableListenableFuture.setException(exception)).isTrue();

    assertThatExceptionOfType(ExecutionException.class).isThrownBy(
                    settableListenableFuture::get)
            .withCause(exception);

    assertThat(settableListenableFuture.isCancelled()).isFalse();
    assertThat(settableListenableFuture.isDone()).isTrue();
  }

  @Test
  void throwsSetErrorWrappedInExecutionExceptionFromCompletable() throws Exception {
    Throwable exception = new OutOfMemoryError();
    assertThat(settableListenableFuture.setException(exception)).isTrue();
    Future<String> completable = settableListenableFuture.completable();

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

    settableListenableFuture.addListener(new FutureListener<String>() {
      @Override
      public void onSuccess(String result) {
        callbackHolder[0] = result;
      }

      @Override
      public void onFailure(Throwable ex) {
        throw new AssertionError("Expected onSuccess() to be called", ex);
      }
    });

    settableListenableFuture.set(string);
    assertThat(callbackHolder[0]).isEqualTo(string);
    assertThat(settableListenableFuture.isCancelled()).isFalse();
    assertThat(settableListenableFuture.isDone()).isTrue();
  }

  @Test
  void setValueTriggersCallbackOnlyOnce() {
    String string = "hello";
    final String[] callbackHolder = new String[1];

    settableListenableFuture.addListener(new FutureListener<String>() {
      @Override
      public void onSuccess(String result) {
        callbackHolder[0] = result;
      }

      @Override
      public void onFailure(Throwable ex) {
        throw new AssertionError("Expected onSuccess() to be called", ex);
      }
    });

    settableListenableFuture.set(string);
    assertThat(settableListenableFuture.set("good bye")).isFalse();
    assertThat(callbackHolder[0]).isEqualTo(string);
    assertThat(settableListenableFuture.isCancelled()).isFalse();
    assertThat(settableListenableFuture.isDone()).isTrue();
  }

  @Test
  void setExceptionTriggersCallback() {
    Throwable exception = new RuntimeException();
    final Throwable[] callbackHolder = new Throwable[1];

    settableListenableFuture.addListener(new FutureListener<String>() {
      @Override
      public void onSuccess(String result) {
        fail("Expected onFailure() to be called");
      }

      @Override
      public void onFailure(Throwable ex) {
        callbackHolder[0] = ex;
      }
    });

    settableListenableFuture.setException(exception);
    assertThat(callbackHolder[0]).isEqualTo(exception);
    assertThat(settableListenableFuture.isCancelled()).isFalse();
    assertThat(settableListenableFuture.isDone()).isTrue();
  }

  @Test
  void setExceptionTriggersCallbackOnlyOnce() {
    Throwable exception = new RuntimeException();
    final Throwable[] callbackHolder = new Throwable[1];

    settableListenableFuture.addListener(new FutureListener<String>() {
      @Override
      public void onSuccess(String result) {
        fail("Expected onFailure() to be called");
      }

      @Override
      public void onFailure(Throwable ex) {
        callbackHolder[0] = ex;
      }
    });

    settableListenableFuture.setException(exception);
    assertThat(settableListenableFuture.setException(new IllegalArgumentException())).isFalse();
    assertThat(callbackHolder[0]).isEqualTo(exception);
    assertThat(settableListenableFuture.isCancelled()).isFalse();
    assertThat(settableListenableFuture.isDone()).isTrue();
  }

  @Test
  void nullIsAcceptedAsValueToSet() throws ExecutionException, InterruptedException {
    settableListenableFuture.set(null);
    assertThat((Object) settableListenableFuture.get()).isNull();
    assertThat(settableListenableFuture.isCancelled()).isFalse();
    assertThat(settableListenableFuture.isDone()).isTrue();
  }

  @Test
  void getWaitsForCompletion() throws ExecutionException, InterruptedException {
    final String string = "hello";

    new Thread(() -> {
      try {
        Thread.sleep(20L);
        settableListenableFuture.set(string);
      }
      catch (InterruptedException ex) {
        throw new RuntimeException(ex);
      }
    }).start();

    String value = settableListenableFuture.get();
    assertThat(value).isEqualTo(string);
    assertThat(settableListenableFuture.isCancelled()).isFalse();
    assertThat(settableListenableFuture.isDone()).isTrue();
  }

  @Test
  void getWithTimeoutThrowsTimeoutException() throws ExecutionException, InterruptedException {
    assertThatExceptionOfType(TimeoutException.class).isThrownBy(() ->
            settableListenableFuture.get(1L, TimeUnit.MILLISECONDS));
  }

  @Test
  void getWithTimeoutWaitsForCompletion() throws ExecutionException, InterruptedException, TimeoutException {
    final String string = "hello";

    new Thread(() -> {
      try {
        Thread.sleep(20L);
        settableListenableFuture.set(string);
      }
      catch (InterruptedException ex) {
        throw new RuntimeException(ex);
      }
    }).start();

    String value = settableListenableFuture.get(500L, TimeUnit.MILLISECONDS);
    assertThat(value).isEqualTo(string);
    assertThat(settableListenableFuture.isCancelled()).isFalse();
    assertThat(settableListenableFuture.isDone()).isTrue();
  }

  @Test
  void cancelPreventsValueFromBeingSet() {
    assertThat(settableListenableFuture.cancel(true)).isTrue();
    assertThat(settableListenableFuture.set("hello")).isFalse();
    assertThat(settableListenableFuture.isCancelled()).isTrue();
    assertThat(settableListenableFuture.isDone()).isTrue();
  }

  @Test
  void cancelSetsFutureToDone() {
    settableListenableFuture.cancel(true);
    assertThat(settableListenableFuture.isCancelled()).isTrue();
    assertThat(settableListenableFuture.isDone()).isTrue();
  }

  @Test
  void cancelWithMayInterruptIfRunningTrueCallsOverriddenMethod() {
    InterruptibleSettableListenableFuture interruptibleFuture = new InterruptibleSettableListenableFuture();
    assertThat(interruptibleFuture.cancel(true)).isTrue();
    assertThat(interruptibleFuture.calledInterruptTask()).isTrue();
    assertThat(interruptibleFuture.isCancelled()).isTrue();
    assertThat(interruptibleFuture.isDone()).isTrue();
  }

  @Test
  void cancelWithMayInterruptIfRunningFalseDoesNotCallOverriddenMethod() {
    InterruptibleSettableListenableFuture interruptibleFuture = new InterruptibleSettableListenableFuture();
    assertThat(interruptibleFuture.cancel(false)).isTrue();
    assertThat(interruptibleFuture.calledInterruptTask()).isFalse();
    assertThat(interruptibleFuture.isCancelled()).isTrue();
    assertThat(interruptibleFuture.isDone()).isTrue();
  }

  @Test
  void setPreventsCancel() {
    assertThat(settableListenableFuture.set("hello")).isTrue();
    assertThat(settableListenableFuture.cancel(true)).isFalse();
    assertThat(settableListenableFuture.isCancelled()).isFalse();
    assertThat(settableListenableFuture.isDone()).isTrue();
  }

  @Test
  void cancelPreventsExceptionFromBeingSet() {
    assertThat(settableListenableFuture.cancel(true)).isTrue();
    assertThat(settableListenableFuture.setException(new RuntimeException())).isFalse();
    assertThat(settableListenableFuture.isCancelled()).isTrue();
    assertThat(settableListenableFuture.isDone()).isTrue();
  }

  @Test
  void setExceptionPreventsCancel() {
    assertThat(settableListenableFuture.setException(new RuntimeException())).isTrue();
    assertThat(settableListenableFuture.cancel(true)).isFalse();
    assertThat(settableListenableFuture.isCancelled()).isFalse();
    assertThat(settableListenableFuture.isDone()).isTrue();
  }

  @Test
  void cancelStateThrowsExceptionWhenCallingGet() throws ExecutionException, InterruptedException {
    settableListenableFuture.cancel(true);

    assertThatExceptionOfType(CancellationException.class).isThrownBy(() ->
            settableListenableFuture.get());

    assertThat(settableListenableFuture.isCancelled()).isTrue();
    assertThat(settableListenableFuture.isDone()).isTrue();
  }

  @Test
  void cancelStateThrowsExceptionWhenCallingGetWithTimeout() throws ExecutionException, TimeoutException, InterruptedException {
    new Thread(() -> {
      try {
        Thread.sleep(20L);
        settableListenableFuture.cancel(true);
      }
      catch (InterruptedException ex) {
        throw new RuntimeException(ex);
      }
    }).start();

    assertThatExceptionOfType(CancellationException.class)
            .isThrownBy(() -> settableListenableFuture.get(500L, TimeUnit.MILLISECONDS));

    assertThat(settableListenableFuture.isCancelled()).isTrue();
    assertThat(settableListenableFuture.isDone()).isTrue();
  }

  @Test
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void cancelDoesNotNotifyCallbacksOnSet() {
    FutureListener callback = mock(FutureListener.class);
    settableListenableFuture.addListener(callback);
    settableListenableFuture.cancel(true);

    verify(callback).onFailure(any(CancellationException.class));
    verifyNoMoreInteractions(callback);

    settableListenableFuture.set("hello");
    verifyNoMoreInteractions(callback);

    assertThat(settableListenableFuture.isCancelled()).isTrue();
    assertThat(settableListenableFuture.isDone()).isTrue();
  }

  @Test
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void cancelDoesNotNotifyCallbacksOnSetException() {
    FutureListener callback = mock(FutureListener.class);
    settableListenableFuture.addListener(callback);
    settableListenableFuture.cancel(true);

    verify(callback).onFailure(any(CancellationException.class));
    verifyNoMoreInteractions(callback);

    settableListenableFuture.setException(new RuntimeException());
    verifyNoMoreInteractions(callback);

    assertThat(settableListenableFuture.isCancelled()).isTrue();
    assertThat(settableListenableFuture.isDone()).isTrue();
  }

  private static class InterruptibleSettableListenableFuture extends SettableListenableFuture<String> {

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
