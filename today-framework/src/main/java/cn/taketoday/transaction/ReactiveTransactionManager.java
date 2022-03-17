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

package cn.taketoday.transaction;

import cn.taketoday.lang.Nullable;
import reactor.core.publisher.Mono;

/**
 * This is the central interface in Framework's reactive transaction infrastructure.
 * Applications can use this directly, but it is not primarily meant as an API:
 * Typically, applications will work with either transactional operators or
 * declarative transaction demarcation through AOP.
 *
 * @author Mark Paluch
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see cn.taketoday.transaction.reactive.TransactionalOperator
 * @see cn.taketoday.transaction.interceptor.TransactionInterceptor
 * @see PlatformTransactionManager
 * @since 4.0 2021/12/10 21:00
 */
public interface ReactiveTransactionManager extends TransactionManager {

  /**
   * Emit a currently active reactive transaction or create a new one, according to
   * the specified propagation behavior.
   * <p>Note that parameters like isolation level or timeout will only be applied
   * to new transactions, and thus be ignored when participating in active ones.
   * <p>Furthermore, not all transaction definition settings will be supported
   * by every transaction manager: A proper transaction manager implementation
   * should throw an exception when unsupported settings are encountered.
   * <p>An exception to the above rule is the read-only flag, which should be
   * ignored if no explicit read-only mode is supported. Essentially, the
   * read-only flag is just a hint for potential optimization.
   *
   * @param definition the TransactionDefinition instance,
   * describing propagation behavior, isolation level, timeout etc.
   * @return transaction status object representing the new or current transaction
   * @throws TransactionException in case of lookup, creation, or system errors
   * @throws IllegalTransactionStateException if the given transaction definition
   * cannot be executed (for example, if a currently active transaction is in
   * conflict with the specified propagation behavior)
   * @see TransactionDefinition#getPropagationBehavior
   * @see TransactionDefinition#getIsolationLevel
   * @see TransactionDefinition#getTimeout
   * @see TransactionDefinition#isReadOnly
   */
  Mono<ReactiveTransaction> getReactiveTransaction(@Nullable TransactionDefinition definition)
          throws TransactionException;

  /**
   * Commit the given transaction, with regard to its status. If the transaction
   * has been marked rollback-only programmatically, perform a rollback.
   * <p>If the transaction wasn't a new one, omit the commit for proper
   * participation in the surrounding transaction. If a previous transaction
   * has been suspended to be able to create a new one, resume the previous
   * transaction after committing the new one.
   * <p>Note that when the commit call completes, no matter if normally or
   * throwing an exception, the transaction must be fully completed and
   * cleaned up. No rollback call should be expected in such a case.
   * <p>If this method throws an exception other than a TransactionException,
   * then some before-commit error caused the commit attempt to fail. For
   * example, an O/R Mapping tool might have tried to flush changes to the
   * database right before commit, with the resulting DataAccessException
   * causing the transaction to fail. The original exception will be
   * propagated to the caller of this commit method in such a case.
   *
   * @param transaction object returned by the {@code getTransaction} method
   * @throws UnexpectedRollbackException in case of an unexpected rollback
   * that the transaction coordinator initiated
   * @throws HeuristicCompletionException in case of a transaction failure
   * caused by a heuristic decision on the side of the transaction coordinator
   * @throws TransactionSystemException in case of commit or system errors
   * (typically caused by fundamental resource failures)
   * @throws IllegalTransactionStateException if the given transaction
   * is already completed (that is, committed or rolled back)
   * @see ReactiveTransaction#setRollbackOnly
   */
  Mono<Void> commit(ReactiveTransaction transaction) throws TransactionException;

  /**
   * Perform a rollback of the given transaction.
   * <p>If the transaction wasn't a new one, just set it rollback-only for proper
   * participation in the surrounding transaction. If a previous transaction
   * has been suspended to be able to create a new one, resume the previous
   * transaction after rolling back the new one.
   * <p><b>Do not call rollback on a transaction if commit threw an exception.</b>
   * The transaction will already have been completed and cleaned up when commit
   * returns, even in case of a commit exception. Consequently, a rollback call
   * after commit failure will lead to an IllegalTransactionStateException.
   *
   * @param transaction object returned by the {@code getTransaction} method
   * @throws TransactionSystemException in case of rollback or system errors
   * (typically caused by fundamental resource failures)
   * @throws IllegalTransactionStateException if the given transaction
   * is already completed (that is, committed or rolled back)
   */
  Mono<Void> rollback(ReactiveTransaction transaction) throws TransactionException;

}

