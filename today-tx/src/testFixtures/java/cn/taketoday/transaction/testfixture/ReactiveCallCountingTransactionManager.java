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

package cn.taketoday.transaction.testfixture;

import cn.taketoday.transaction.TransactionDefinition;
import cn.taketoday.transaction.TransactionException;
import cn.taketoday.transaction.reactive.AbstractReactiveTransactionManager;
import cn.taketoday.transaction.reactive.GenericReactiveTransaction;
import cn.taketoday.transaction.reactive.TransactionSynchronizationManager;
import reactor.core.publisher.Mono;

/**
 * @author Mark Paluch
 */
@SuppressWarnings("serial")
public class ReactiveCallCountingTransactionManager extends AbstractReactiveTransactionManager {

  public TransactionDefinition lastDefinition;
  public int begun;
  public int commits;
  public int rollbacks;
  public int inflight;

  @Override
  protected Object doGetTransaction(TransactionSynchronizationManager synchronizationManager) throws TransactionException {
    return new Object();
  }

  @Override
  protected Mono<Void> doBegin(TransactionSynchronizationManager synchronizationManager, Object transaction, TransactionDefinition definition) throws TransactionException {
    this.lastDefinition = definition;
    ++begun;
    ++inflight;
    return Mono.empty();
  }

  @Override
  protected Mono<Void> doCommit(TransactionSynchronizationManager synchronizationManager, GenericReactiveTransaction status) throws TransactionException {
    ++commits;
    --inflight;
    return Mono.empty();
  }

  @Override
  protected Mono<Void> doRollback(TransactionSynchronizationManager synchronizationManager, GenericReactiveTransaction status) throws TransactionException {
    ++rollbacks;
    --inflight;
    return Mono.empty();
  }

  public void clear() {
    begun = commits = rollbacks = inflight = 0;
  }

}
