/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package infra.transaction.testfixture;

import infra.transaction.TransactionDefinition;
import infra.transaction.support.AbstractPlatformTransactionManager;
import infra.transaction.support.DefaultTransactionStatus;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class CallCountingTransactionManager extends AbstractPlatformTransactionManager {

  public TransactionDefinition lastDefinition;
  public int begun;
  public int commits;
  public int rollbacks;
  public int inflight;

  @Override
  protected Object doGetTransaction() {
    return new Object();
  }

  @Override
  protected void doBegin(Object transaction, TransactionDefinition definition) {
    this.lastDefinition = definition;
    ++begun;
    ++inflight;
  }

  @Override
  protected void doCommit(DefaultTransactionStatus status) {
    ++commits;
    --inflight;
  }

  @Override
  protected void doRollback(DefaultTransactionStatus status) {
    ++rollbacks;
    --inflight;
  }

  public void clear() {
    begun = commits = rollbacks = inflight = 0;
  }

}
