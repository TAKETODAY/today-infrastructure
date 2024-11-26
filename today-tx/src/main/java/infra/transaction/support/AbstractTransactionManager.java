/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.transaction.support;

import java.util.Collection;
import java.util.Collections;

import infra.lang.Nullable;
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
