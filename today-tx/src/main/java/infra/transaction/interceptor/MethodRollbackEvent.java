/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.transaction.interceptor;

import org.aopalliance.intercept.MethodInvocation;

import infra.context.event.MethodFailureEvent;
import infra.transaction.TransactionExecution;

/**
 * Event published for every exception encountered that triggers a transaction rollback
 * through a proxy-triggered method invocation or a reactive publisher returned from it.
 * Can be listened to via an {@code ApplicationListener<MethodRollbackEvent>} bean or
 * an {@code @EventListener(MethodRollbackEvent.class)} method.
 *
 * <p>Note: This event gets published right <i>before</i> the actual transaction rollback.
 * As a consequence, the exposed {@link #getTransaction() transaction} reflects the state
 * of the transaction right before the rollback.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see TransactionInterceptor
 * @see infra.transaction.annotation.Transactional
 * @see infra.context.ApplicationListener
 * @see infra.context.event.EventListener
 * @since 5.0
 */
@SuppressWarnings("serial")
public class MethodRollbackEvent extends MethodFailureEvent {

  private final TransactionExecution transaction;

  /**
   * Create a new event for the given rolled-back method invocation.
   *
   * @param invocation the transactional method invocation
   * @param failure the exception encountered that triggered a rollback
   * @param transaction the transaction status right before the rollback
   */
  public MethodRollbackEvent(MethodInvocation invocation, Throwable failure, TransactionExecution transaction) {
    super(invocation, failure);
    this.transaction = transaction;
  }

  /**
   * Return the exception encountered.
   * <p>This may be an exception thrown by the method or emitted by the
   * reactive publisher returned from the method.
   */
  @Override
  public Throwable getFailure() {
    return super.getFailure();
  }

  /**
   * Return the corresponding transaction status.
   */
  public TransactionExecution getTransaction() {
    return this.transaction;
  }

}
