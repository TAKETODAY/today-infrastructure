/*
 * Copyright 2012-present the original author or authors.
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

package infra.transaction.config;

import java.util.List;

import infra.transaction.ConfigurableTransactionManager;
import infra.transaction.TransactionExecutionListener;

/**
 * {@link TransactionManagerCustomizer} that adds {@link TransactionExecutionListener
 * execution listeners} to any transaction manager that is
 * {@link ConfigurableTransactionManager configurable}.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
class ExecutionListenersTransactionManagerCustomizer implements TransactionManagerCustomizer<ConfigurableTransactionManager> {

  private final List<TransactionExecutionListener> listeners;

  ExecutionListenersTransactionManagerCustomizer(List<TransactionExecutionListener> listeners) {
    this.listeners = listeners;
  }

  @Override
  public void customize(ConfigurableTransactionManager txManager) {
    for (TransactionExecutionListener listener : listeners) {
      txManager.addListener(listener);
    }
  }

}
