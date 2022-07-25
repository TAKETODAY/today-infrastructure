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

package cn.taketoday.transaction.jta;

import java.util.List;

import cn.taketoday.transaction.support.TransactionSynchronization;
import cn.taketoday.transaction.support.TransactionSynchronizationUtils;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;

/**
 * Adapter for a JTA Synchronization, invoking the {@code afterCommit} /
 * {@code afterCompletion} callbacks of Framework {@link TransactionSynchronization}
 * objects callbacks after the outer JTA transaction has completed.
 * Applied when participating in an existing (non-Framework) JTA transaction.
 *
 * @author Juergen Hoeller
 * @see TransactionSynchronization#afterCommit
 * @see TransactionSynchronization#afterCompletion
 * @since 4.0
 */
public class JtaAfterCompletionSynchronization implements Synchronization {

  private final List<TransactionSynchronization> synchronizations;

  /**
   * Create a new JtaAfterCompletionSynchronization for the given synchronization objects.
   *
   * @param synchronizations the List of TransactionSynchronization objects
   * @see cn.taketoday.transaction.support.TransactionSynchronization
   */
  public JtaAfterCompletionSynchronization(List<TransactionSynchronization> synchronizations) {
    this.synchronizations = synchronizations;
  }

  @Override
  public void beforeCompletion() { }

  @Override
  public void afterCompletion(int status) {
    switch (status) {
      case Status.STATUS_COMMITTED:
        try {
          TransactionSynchronizationUtils.invokeAfterCommit(this.synchronizations);
        }
        finally {
          TransactionSynchronizationUtils.invokeAfterCompletion(
                  this.synchronizations, TransactionSynchronization.STATUS_COMMITTED);
        }
        break;
      case Status.STATUS_ROLLEDBACK:
        TransactionSynchronizationUtils.invokeAfterCompletion(
                this.synchronizations, TransactionSynchronization.STATUS_ROLLED_BACK);
        break;
      default:
        TransactionSynchronizationUtils.invokeAfterCompletion(
                this.synchronizations, TransactionSynchronization.STATUS_UNKNOWN);
        break;
    }
  }
}
