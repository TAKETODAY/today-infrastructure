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

package infra.transaction.support;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

import infra.transaction.ConfigurableTransactionManager;
import infra.transaction.TransactionExecutionListener;
import infra.util.CollectionUtils;

/**
 * for managing TransactionExecutionListeners
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2024/7/22 21:53
 */
public abstract class AbstractTransactionManager implements ConfigurableTransactionManager {

  protected TransactionExecutionListener executionListeners = TransactionExecutionListener.empty();

  @Override
  public final void setTransactionExecutionListeners(@Nullable Collection<TransactionExecutionListener> listeners) {
    this.executionListeners = CollectionUtils.isEmpty(listeners) ? TransactionExecutionListener.empty()
            : new CompositeTransactionExecutionListener(listeners);
  }

  @Override
  public final Collection<TransactionExecutionListener> getTransactionExecutionListeners() {
    if (executionListeners instanceof CompositeTransactionExecutionListener composite) {
      return Collections.unmodifiableCollection(composite.listeners);
    }
    return Collections.emptyList();
  }

  @Override
  public void addListener(TransactionExecutionListener listener) {
    CompositeTransactionExecutionListener composite;
    if (executionListeners instanceof CompositeTransactionExecutionListener) {
      composite = (CompositeTransactionExecutionListener) executionListeners;
    }
    else {
      composite = new CompositeTransactionExecutionListener();
      executionListeners = composite;
    }
    composite.listeners.add(listener);
  }

}
