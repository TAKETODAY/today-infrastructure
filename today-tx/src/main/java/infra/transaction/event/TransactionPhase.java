/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.transaction.event;

import java.util.function.Consumer;

import infra.transaction.support.TransactionSynchronization;

/**
 * The phase in which a transactional event listener applies.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see TransactionalEventListener#phase()
 * @see TransactionalApplicationListener#getTransactionPhase()
 * @see TransactionalApplicationListener#forPayload(TransactionPhase, Consumer)
 * @since 4.0
 */
public enum TransactionPhase {

  /**
   * Handle the event before transaction commit.
   *
   * @see TransactionSynchronization#beforeCommit(boolean)
   */
  BEFORE_COMMIT,

  /**
   * Handle the event after the commit has completed successfully.
   * <p>Note: This is a specialization of {@link #AFTER_COMPLETION} and therefore
   * executes in the same sequence of events as {@code AFTER_COMPLETION}
   * (and not in {@link TransactionSynchronization#afterCommit()}).
   * <p>Interactions with the underlying transactional resource will not be
   * committed in this phase. See
   * {@link TransactionSynchronization#afterCompletion(int)} for details.
   *
   * @see TransactionSynchronization#afterCompletion(int)
   * @see TransactionSynchronization#STATUS_COMMITTED
   */
  AFTER_COMMIT,

  /**
   * Handle the event if the transaction has rolled back.
   * <p>Note: This is a specialization of {@link #AFTER_COMPLETION} and therefore
   * executes in the same sequence of events as {@code AFTER_COMPLETION}.
   * <p>Interactions with the underlying transactional resource will not be
   * committed in this phase. See
   * {@link TransactionSynchronization#afterCompletion(int)} for details.
   *
   * @see TransactionSynchronization#afterCompletion(int)
   * @see TransactionSynchronization#STATUS_ROLLED_BACK
   */
  AFTER_ROLLBACK,

  /**
   * Handle the event after the transaction has completed.
   * <p>For more fine-grained events, use {@link #AFTER_COMMIT} or
   * {@link #AFTER_ROLLBACK} to intercept transaction commit
   * or rollback, respectively.
   * <p>Interactions with the underlying transactional resource will not be
   * committed in this phase. See
   * {@link TransactionSynchronization#afterCompletion(int)} for details.
   *
   * @see TransactionSynchronization#afterCompletion(int)
   */
  AFTER_COMPLETION

}
