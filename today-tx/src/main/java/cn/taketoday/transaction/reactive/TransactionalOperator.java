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

import cn.taketoday.transaction.ReactiveTransactionManager;
import cn.taketoday.transaction.TransactionDefinition;
import cn.taketoday.transaction.TransactionException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Operator class that simplifies programmatic transaction demarcation and
 * transaction exception handling.
 *
 * <p>The central method is {@link #transactional}, supporting transactional wrapping
 * of functional sequences code that. This operator handles the transaction lifecycle
 * and possible exceptions such that neither the ReactiveTransactionCallback
 * implementation nor the calling code needs to explicitly handle transactions.
 *
 * <p>Typical usage: Allows for writing low-level data access objects that use
 * resources such as database connections but are not transaction-aware themselves.
 * Instead, they can implicitly participate in transactions handled by higher-level
 * application services utilizing this class, making calls to the low-level
 * services via an inner-class callback object.
 *
 * <p><strong>Note:</strong> Transactional Publishers should avoid Subscription
 * cancellation. See the
 * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/data-access.html#tx-prog-operator-cancel">Cancel Signals</a>
 * section of the Framework reference for more details.
 *
 * @author Mark Paluch
 * @author Juergen Hoeller
 * @see #execute
 * @see ReactiveTransactionManager
 * @since 4.0
 */
public interface TransactionalOperator {

  /**
   * Wrap the functional sequence specified by the given Flux within a transaction.
   *
   * @param flux the Flux that should be executed within the transaction
   * @return a result publisher returned by the callback, or {@code null} if none
   * @throws TransactionException in case of initialization, rollback, or system errors
   * @throws RuntimeException if thrown by the TransactionCallback
   */
  default <T> Flux<T> transactional(Flux<T> flux) {
    return execute(it -> flux);
  }

  /**
   * Wrap the functional sequence specified by the given Mono within a transaction.
   *
   * @param mono the Mono that should be executed within the transaction
   * @return a result publisher returned by the callback
   * @throws TransactionException in case of initialization, rollback, or system errors
   * @throws RuntimeException if thrown by the TransactionCallback
   */
  default <T> Mono<T> transactional(Mono<T> mono) {
    return execute(it -> mono).singleOrEmpty();
  }

  /**
   * Execute the action specified by the given callback object within a transaction.
   * <p>Allows for returning a result object created within the transaction, that is,
   * a domain object or a collection of domain objects. A RuntimeException thrown
   * by the callback is treated as a fatal exception that enforces a rollback.
   * Such an exception gets propagated to the caller of the template.
   *
   * @param action the callback object that specifies the transactional action
   * @return a result object returned by the callback
   * @throws TransactionException in case of initialization, rollback, or system errors
   * @throws RuntimeException if thrown by the TransactionCallback
   */
  <T> Flux<T> execute(TransactionCallback<T> action) throws TransactionException;

  // Static builder methods

  /**
   * Create a new {@link TransactionalOperator} using {@link ReactiveTransactionManager},
   * using a default transaction.
   *
   * @param transactionManager the transaction management strategy to be used
   * @return the transactional operator
   */
  static TransactionalOperator create(ReactiveTransactionManager transactionManager) {
    return create(transactionManager, TransactionDefinition.withDefaults());
  }

  /**
   * Create a new {@link TransactionalOperator} using {@link ReactiveTransactionManager}
   * and {@link TransactionDefinition}.
   *
   * @param transactionManager the transaction management strategy to be used
   * @param transactionDefinition the transaction definition to apply
   * @return the transactional operator
   */
  static TransactionalOperator create(
          ReactiveTransactionManager transactionManager, TransactionDefinition transactionDefinition) {

    return new TransactionalOperatorImpl(transactionManager, transactionDefinition);
  }

}
