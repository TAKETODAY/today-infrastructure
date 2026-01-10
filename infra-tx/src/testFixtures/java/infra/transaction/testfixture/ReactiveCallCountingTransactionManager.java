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

package infra.transaction.testfixture;

import infra.transaction.TransactionDefinition;
import infra.transaction.TransactionException;
import infra.transaction.reactive.AbstractReactiveTransactionManager;
import infra.transaction.reactive.GenericReactiveTransaction;
import infra.transaction.reactive.TransactionSynchronizationManager;
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
