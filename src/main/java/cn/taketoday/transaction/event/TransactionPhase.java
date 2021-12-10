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

package cn.taketoday.transaction.event;

import java.util.function.Consumer;

import cn.taketoday.transaction.support.TransactionSynchronization;

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
