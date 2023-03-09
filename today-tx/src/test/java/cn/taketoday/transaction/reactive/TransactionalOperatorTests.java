/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.transaction.reactive;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import cn.taketoday.transaction.ReactiveTransaction;
import cn.taketoday.transaction.ReactiveTransactionManager;
import cn.taketoday.transaction.support.DefaultTransactionDefinition;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.publisher.PublisherProbe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TransactionalOperator}.
 *
 * @author Mark Paluch
 */
public class TransactionalOperatorTests {

  ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(false, true);

  @Test
  public void commitWithMono() {
    TransactionalOperator operator = TransactionalOperator.create(tm, new DefaultTransactionDefinition());
    Mono.just(true).as(operator::transactional)
            .as(StepVerifier::create)
            .expectNext(true)
            .verifyComplete();
    assertThat(tm.commit).isTrue();
    assertThat(tm.rollback).isFalse();
  }

  @Test
  public void monoSubscriptionNotCancelled() {
    AtomicBoolean cancelled = new AtomicBoolean();
    TransactionalOperator operator = TransactionalOperator.create(tm, new DefaultTransactionDefinition());
    Mono.just(true).doOnCancel(() -> cancelled.set(true)).as(operator::transactional)
            .as(StepVerifier::create)
            .expectNext(true)
            .verifyComplete();
    assertThat(tm.commit).isTrue();
    assertThat(tm.rollback).isFalse();
    assertThat(cancelled).isFalse();
  }

  @Test
  public void cancellationPropagatedToMono() {
    AtomicBoolean cancelled = new AtomicBoolean();
    TransactionalOperator operator = TransactionalOperator.create(tm, new DefaultTransactionDefinition());
    Mono.create(sink -> sink.onCancel(() -> cancelled.set(true))).as(operator::transactional)
            .as(StepVerifier::create)
            .thenAwait()
            .thenCancel()
            .verify();
    assertThat(tm.commit).isFalse();
    assertThat(tm.rollback).isTrue();
    assertThat(cancelled).isTrue();
  }

  @Test
  public void cancellationPropagatedToFlux() {
    AtomicBoolean cancelled = new AtomicBoolean();
    TransactionalOperator operator = TransactionalOperator.create(tm, new DefaultTransactionDefinition());
    Flux.create(sink -> sink.onCancel(() -> cancelled.set(true))).as(operator::transactional)
            .as(StepVerifier::create)
            .thenAwait()
            .thenCancel()
            .verify();
    assertThat(tm.commit).isFalse();
    assertThat(tm.rollback).isTrue();
    assertThat(cancelled).isTrue();
  }

  @Test
  public void rollbackWithMono() {
    TransactionalOperator operator = TransactionalOperator.create(tm, new DefaultTransactionDefinition());
    Mono.error(new IllegalStateException()).as(operator::transactional)
            .as(StepVerifier::create)
            .verifyError(IllegalStateException.class);
    assertThat(tm.commit).isFalse();
    assertThat(tm.rollback).isTrue();
  }

  @Test
  public void commitFailureWithMono() {
    ReactiveTransactionManager tm = mock(ReactiveTransactionManager.class);
    given(tm.getReactiveTransaction(any())).willReturn(Mono.just(mock(ReactiveTransaction.class)));
    PublisherProbe<Void> commit = PublisherProbe.of(Mono.error(IOException::new));
    given(tm.commit(any())).willReturn(commit.mono());
    PublisherProbe<Void> rollback = PublisherProbe.empty();
    given(tm.rollback(any())).willReturn(rollback.mono());

    TransactionalOperator operator = TransactionalOperator.create(tm, new DefaultTransactionDefinition());
    Mono.just(true).as(operator::transactional)
            .as(StepVerifier::create)
            .verifyError(IOException.class);
    assertThat(commit.subscribeCount()).isEqualTo(1);
    rollback.assertWasNotSubscribed();
  }

  @Test
  public void rollbackFailureWithMono() {
    ReactiveTransactionManager tm = mock(ReactiveTransactionManager.class);
    given(tm.getReactiveTransaction(any())).willReturn(Mono.just(mock(ReactiveTransaction.class)));
    PublisherProbe<Void> commit = PublisherProbe.empty();
    given(tm.commit(any())).willReturn(commit.mono());
    PublisherProbe<Void> rollback = PublisherProbe.of(Mono.error(IOException::new));
    given(tm.rollback(any())).willReturn(rollback.mono());

    TransactionalOperator operator = TransactionalOperator.create(tm, new DefaultTransactionDefinition());
    IllegalStateException actionFailure = new IllegalStateException();
    Mono.error(actionFailure).as(operator::transactional)
            .as(StepVerifier::create)
            .verifyErrorSatisfies(ex -> assertThat(ex)
                    .isInstanceOf(IOException.class)
                    .hasSuppressedException(actionFailure));
    commit.assertWasNotSubscribed();
    assertThat(rollback.subscribeCount()).isEqualTo(1);
  }

  @Test
  public void commitWithFlux() {
    TransactionalOperator operator = TransactionalOperator.create(tm, new DefaultTransactionDefinition());
    Flux.just(1, 2, 3, 4).as(operator::transactional)
            .as(StepVerifier::create)
            .expectNextCount(4)
            .verifyComplete();
    assertThat(tm.commit).isTrue();
    assertThat(tm.rollback).isFalse();
  }

  @Test
  public void rollbackWithFlux() {
    TransactionalOperator operator = TransactionalOperator.create(tm, new DefaultTransactionDefinition());
    Flux.error(new IllegalStateException()).as(operator::transactional)
            .as(StepVerifier::create)
            .verifyError(IllegalStateException.class);
    assertThat(tm.commit).isFalse();
    assertThat(tm.rollback).isTrue();
  }

  @Test
  public void commitFailureWithFlux() {
    ReactiveTransactionManager tm = mock(ReactiveTransactionManager.class);
    given(tm.getReactiveTransaction(any())).willReturn(Mono.just(mock(ReactiveTransaction.class)));
    PublisherProbe<Void> commit = PublisherProbe.of(Mono.error(IOException::new));
    given(tm.commit(any())).willReturn(commit.mono());
    PublisherProbe<Void> rollback = PublisherProbe.empty();
    given(tm.rollback(any())).willReturn(rollback.mono());

    TransactionalOperator operator = TransactionalOperator.create(tm, new DefaultTransactionDefinition());
    Flux.just(1, 2, 3, 4).as(operator::transactional)
            .as(StepVerifier::create)
            .expectNextCount(4)
            .verifyError(IOException.class);
    assertThat(commit.subscribeCount()).isEqualTo(1);
    rollback.assertWasNotSubscribed();
  }

  @Test
  public void rollbackFailureWithFlux() {
    ReactiveTransactionManager tm = mock(ReactiveTransactionManager.class);
    given(tm.getReactiveTransaction(any())).willReturn(Mono.just(mock(ReactiveTransaction.class)));
    PublisherProbe<Void> commit = PublisherProbe.empty();
    given(tm.commit(any())).willReturn(commit.mono());
    PublisherProbe<Void> rollback = PublisherProbe.of(Mono.error(IOException::new));
    given(tm.rollback(any())).willReturn(rollback.mono());

    TransactionalOperator operator = TransactionalOperator.create(tm, new DefaultTransactionDefinition());
    IllegalStateException actionFailure = new IllegalStateException();
    Flux.just(1, 2, 3).concatWith(Flux.error(actionFailure)).as(operator::transactional)
            .as(StepVerifier::create)
            .expectNextCount(3)
            .verifyErrorSatisfies(ex -> assertThat(ex)
                    .isInstanceOf(IOException.class)
                    .hasSuppressedException(actionFailure));
    commit.assertWasNotSubscribed();
    assertThat(rollback.subscribeCount()).isEqualTo(1);
  }

}
