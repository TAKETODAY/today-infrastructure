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

package cn.taketoday.scheduling.annotation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import cn.taketoday.util.concurrent.ListenableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Juergen Hoeller
 */
public class AsyncResultTests {

  @Test
  public void asyncResultWithCallbackAndValue() throws Exception {
    String value = "val";
    final Set<String> values = new HashSet<>(1);
    ListenableFuture<String> future = AsyncResult.forValue(value);

    future.addListener(values::add, ex -> {
      throw new AssertionError("Failure callback not expected: " + ex, ex);
    });

    assertThat(values.iterator().next()).isSameAs(value);
    assertThat(future.get()).isSameAs(value);
    assertThat(future.completable().get()).isSameAs(value);
    future.completable().thenAccept(v -> assertThat(v).isSameAs(value));
  }

  @Test
  public void asyncResultWithCallbackAndException() throws Exception {
    IOException ex = new IOException();
    final Set<Throwable> values = new HashSet<>(1);
    ListenableFuture<String> future = AsyncResult.forExecutionException(ex);

    future.addListener(result -> {
      throw new AssertionError("Success callback not expected: " + result);
    }, values::add);

    assertThat(values.iterator().next()).isSameAs(ex);
    assertThatExceptionOfType(ExecutionException.class).isThrownBy(
                    future::get)
            .withCause(ex);
    assertThatExceptionOfType(ExecutionException.class).isThrownBy(
                    future.completable()::get)
            .withCause(ex);
  }

  @Test
  public void asyncResultWithSeparateCallbacksAndValue() throws Exception {
    String value = "val";
    final Set<String> values = new HashSet<>(1);
    ListenableFuture<String> future = AsyncResult.forValue(value);
    future.addListener(values::add, ex -> new AssertionError("Failure callback not expected: " + ex));
    assertThat(values.iterator().next()).isSameAs(value);
    assertThat(future.get()).isSameAs(value);
    assertThat(future.completable().get()).isSameAs(value);
    future.completable().thenAccept(v -> assertThat(v).isSameAs(value));
  }

  @Test
  public void asyncResultWithSeparateCallbacksAndException() throws Exception {
    IOException ex = new IOException();
    final Set<Throwable> values = new HashSet<>(1);
    ListenableFuture<String> future = AsyncResult.forExecutionException(ex);
    future.addListener(result -> new AssertionError("Success callback not expected: " + result), values::add);
    assertThat(values.iterator().next()).isSameAs(ex);
    assertThatExceptionOfType(ExecutionException.class).isThrownBy(
                    future::get)
            .withCause(ex);
    assertThatExceptionOfType(ExecutionException.class).isThrownBy(
                    future.completable()::get)
            .withCause(ex);
  }

}
