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

import cn.taketoday.transaction.IllegalTransactionStateException;
import cn.taketoday.transaction.ReactiveTransaction;
import cn.taketoday.transaction.ReactiveTransactionManager;
import cn.taketoday.transaction.TransactionDefinition;
import cn.taketoday.transaction.support.DefaultTransactionDefinition;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for transactional support through {@link ReactiveTestTransactionManager}.
 *
 * @author Mark Paluch
 */
public class ReactiveTransactionSupportTests {

  @Test
  public void noExistingTransaction() {
    ReactiveTransactionManager tm = new ReactiveTestTransactionManager(false, true);

    tm.getReactiveTransaction(new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_SUPPORTS))
            .contextWrite(TransactionContextManager.createTransactionContext()).cast(GenericReactiveTransaction.class)
            .as(StepVerifier::create).consumeNextWith(actual -> assertThat(actual.hasTransaction()).isFalse()
            ).verifyComplete();

    tm.getReactiveTransaction(new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED))
            .cast(GenericReactiveTransaction.class).contextWrite(TransactionContextManager.createTransactionContext())
            .as(StepVerifier::create).consumeNextWith(actual -> {
              assertThat(actual.hasTransaction()).isTrue();
              assertThat(actual.isNewTransaction()).isTrue();
            }).verifyComplete();

    tm.getReactiveTransaction(new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_MANDATORY))
            .contextWrite(TransactionContextManager.createTransactionContext()).cast(GenericReactiveTransaction.class)
            .as(StepVerifier::create).expectError(IllegalTransactionStateException.class).verify();
  }

  @Test
  public void existingTransaction() {
    ReactiveTransactionManager tm = new ReactiveTestTransactionManager(true, true);

    tm.getReactiveTransaction(new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_SUPPORTS))
            .contextWrite(TransactionContextManager.createTransactionContext()).cast(GenericReactiveTransaction.class)
            .as(StepVerifier::create).consumeNextWith(actual -> {
              assertThat(actual.getTransaction()).isNotNull();
              assertThat(actual.isNewTransaction()).isFalse();
            }).verifyComplete();

    tm.getReactiveTransaction(new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED))
            .contextWrite(TransactionContextManager.createTransactionContext()).cast(GenericReactiveTransaction.class)
            .as(StepVerifier::create).consumeNextWith(actual -> {
              assertThat(actual.getTransaction()).isNotNull();
              assertThat(actual.isNewTransaction()).isFalse();
            }).verifyComplete();

    tm.getReactiveTransaction(new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_MANDATORY))
            .contextWrite(TransactionContextManager.createTransactionContext()).cast(GenericReactiveTransaction.class)
            .as(StepVerifier::create).consumeNextWith(actual -> {
              assertThat(actual.getTransaction()).isNotNull();
              assertThat(actual.isNewTransaction()).isFalse();
            }).verifyComplete();
  }

  @Test
  public void commitWithoutExistingTransaction() {
    ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(false, true);
    tm.getReactiveTransaction(new DefaultTransactionDefinition()).flatMap(tm::commit)
            .contextWrite(TransactionContextManager.createTransactionContext())
            .as(StepVerifier::create).verifyComplete();

    assertHasBegan(tm);
    assertHasCommitted(tm);
    assertHasNoRollback(tm);
    assertHasNotSetRollbackOnly(tm);
    assertHasCleanedUp(tm);
  }

  @Test
  public void rollbackWithoutExistingTransaction() {
    ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(false, true);
    tm.getReactiveTransaction(new DefaultTransactionDefinition()).flatMap(tm::rollback)
            .contextWrite(TransactionContextManager.createTransactionContext()).as(StepVerifier::create)
            .verifyComplete();

    assertHasBegan(tm);
    assertHasNotCommitted(tm);
    assertHasRolledBack(tm);
    assertHasNotSetRollbackOnly(tm);
    assertHasCleanedUp(tm);
  }

  @Test
  public void rollbackOnlyWithoutExistingTransaction() {
    ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(false, true);
    tm.getReactiveTransaction(new DefaultTransactionDefinition()).doOnNext(ReactiveTransaction::setRollbackOnly)
            .flatMap(tm::commit)
            .contextWrite(TransactionContextManager.createTransactionContext()).as(StepVerifier::create)
            .verifyComplete();

    assertHasBegan(tm);
    assertHasNotCommitted(tm);
    assertHasRolledBack(tm);
    assertHasNotSetRollbackOnly(tm);
    assertHasCleanedUp(tm);
  }

  @Test
  public void commitWithExistingTransaction() {
    ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(true, true);
    tm.getReactiveTransaction(new DefaultTransactionDefinition()).flatMap(tm::commit)
            .contextWrite(TransactionContextManager.createTransactionContext())
            .as(StepVerifier::create).verifyComplete();

    assertHasNotBegan(tm);
    assertHasNotCommitted(tm);
    assertHasNoRollback(tm);
    assertHasNotSetRollbackOnly(tm);
    assertHasNotCleanedUp(tm);
  }

  @Test
  public void rollbackWithExistingTransaction() {
    ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(true, true);
    tm.getReactiveTransaction(new DefaultTransactionDefinition()).flatMap(tm::rollback)
            .contextWrite(TransactionContextManager.createTransactionContext()).as(StepVerifier::create)
            .verifyComplete();

    assertHasNotBegan(tm);
    assertHasNotCommitted(tm);
    assertHasNoRollback(tm);
    assertHasSetRollbackOnly(tm);
    assertHasNotCleanedUp(tm);
  }

  @Test
  public void rollbackOnlyWithExistingTransaction() {
    ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(true, true);
    tm.getReactiveTransaction(new DefaultTransactionDefinition()).doOnNext(ReactiveTransaction::setRollbackOnly).flatMap(tm::commit)
            .contextWrite(TransactionContextManager.createTransactionContext()).as(StepVerifier::create)
            .verifyComplete();

    assertHasNotBegan(tm);
    assertHasNotCommitted(tm);
    assertHasNoRollback(tm);
    assertHasSetRollbackOnly(tm);
    assertHasNotCleanedUp(tm);
  }

  @Test
  public void transactionTemplate() {
    ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(false, true);
    TransactionalOperator operator = TransactionalOperator.create(tm, new DefaultTransactionDefinition());

    Flux.just("Walter").as(operator::transactional)
            .as(StepVerifier::create)
            .expectNextCount(1)
            .verifyComplete();

    assertHasBegan(tm);
    assertHasCommitted(tm);
    assertHasNoRollback(tm);
    assertHasNotSetRollbackOnly(tm);
    assertHasCleanedUp(tm);
  }

  @Test
  public void transactionTemplateWithException() {
    ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(false, true);
    TransactionalOperator operator = TransactionalOperator.create(tm, new DefaultTransactionDefinition());
    RuntimeException ex = new RuntimeException("Some application exception");

    Mono.error(ex).as(operator::transactional)
            .as(StepVerifier::create)
            .expectError(RuntimeException.class)
            .verify();

    assertHasBegan(tm);
    assertHasNotCommitted(tm);
    assertHasRolledBack(tm);
    assertHasNotSetRollbackOnly(tm);
    assertHasCleanedUp(tm);
  }

  @Test
  void errorInCommitDoesInitiateRollbackAfterCommit() {
    ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(false, true, true);
    TransactionalOperator rxtx = TransactionalOperator.create(tm);

    StepVerifier.create(rxtx.transactional(Mono.just("bar")))
            .verifyErrorMessage("Forced failure on commit");

    assertHasBegan(tm);
    assertHasCommitted(tm);
    assertHasRolledBack(tm);
    assertHasNotSetRollbackOnly(tm);
    assertHasCleanedUp(tm);
  }

  private void assertHasBegan(ReactiveTestTransactionManager actual) {
    assertThat(actual.begin).as("Expected <ReactiveTransactionManager.begin()> but was <begin()> was not invoked").isTrue();
  }

  private void assertHasNotBegan(ReactiveTestTransactionManager actual) {
    assertThat(actual.begin).as("Expected to not call <ReactiveTransactionManager.begin()> but was <begin()> was called").isFalse();
  }

  private void assertHasCommitted(ReactiveTestTransactionManager actual) {
    assertThat(actual.commit).as("Expected <ReactiveTransactionManager.commit()> but was <commit()> was not invoked").isTrue();
  }

  private void assertHasNotCommitted(ReactiveTestTransactionManager actual) {
    assertThat(actual.commit).as("Expected to not call <ReactiveTransactionManager.commit()> but was <commit()> was called").isFalse();
  }

  private void assertHasRolledBack(ReactiveTestTransactionManager actual) {
    assertThat(actual.rollback).as("Expected <ReactiveTransactionManager.rollback()> but was <rollback()> was not invoked").isTrue();
  }

  private void assertHasNoRollback(ReactiveTestTransactionManager actual) {
    assertThat(actual.rollback).as("Expected to not call <ReactiveTransactionManager.rollback()> but was <rollback()> was called").isFalse();
  }

  private void assertHasSetRollbackOnly(ReactiveTestTransactionManager actual) {
    assertThat(actual.rollbackOnly).as("Expected <ReactiveTransactionManager.setRollbackOnly()> but was <setRollbackOnly()> was not invoked").isTrue();
  }

  private void assertHasNotSetRollbackOnly(ReactiveTestTransactionManager actual) {
    assertThat(actual.rollbackOnly).as("Expected to not call <ReactiveTransactionManager.setRollbackOnly()> but was <setRollbackOnly()> was called").isFalse();
  }

  private void assertHasCleanedUp(ReactiveTestTransactionManager actual) {
    assertThat(actual.cleanup).as("Expected <ReactiveTransactionManager.doCleanupAfterCompletion()> but was <doCleanupAfterCompletion()> was not invoked").isTrue();
  }

  private void assertHasNotCleanedUp(ReactiveTestTransactionManager actual) {
    assertThat(actual.cleanup).as("Expected to not call <ReactiveTransactionManager.doCleanupAfterCompletion()> but was <doCleanupAfterCompletion()> was called").isFalse();
  }

}
