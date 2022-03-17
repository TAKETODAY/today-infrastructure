/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.util.concurrent;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MonoToListenableFutureAdapter}.
 *
 * @author Rossen Stoyanchev
 */
class MonoToListenableFutureAdapterTests {

  @Test
  void success() {
    String expected = "one";
    AtomicReference<Object> actual = new AtomicReference<>();
    ListenableFuture<String> future = new MonoToListenableFutureAdapter<>(Mono.just(expected));
    future.addCallback(actual::set, actual::set);

    assertThat(actual.get()).isEqualTo(expected);
  }

  @Test
  void failure() {
    Throwable expected = new IllegalStateException("oops");
    AtomicReference<Object> actual = new AtomicReference<>();
    ListenableFuture<String> future = new MonoToListenableFutureAdapter<>(Mono.error(expected));
    future.addCallback(actual::set, actual::set);

    assertThat(actual.get()).isEqualTo(expected);
  }

  @Test
  void cancellation() {
    Mono<Long> mono = Mono.delay(Duration.ofSeconds(60));
    Future<Long> future = new MonoToListenableFutureAdapter<>(mono);

    assertThat(future.cancel(true)).isTrue();
    assertThat(future.isCancelled()).isTrue();
  }

  @Test
  void cancellationAfterTerminated() {
    Future<Void> future = new MonoToListenableFutureAdapter<>(Mono.empty());

    assertThat(future.cancel(true)).as("Should return false if task already completed").isFalse();
    assertThat(future.isCancelled()).isFalse();
  }

}
