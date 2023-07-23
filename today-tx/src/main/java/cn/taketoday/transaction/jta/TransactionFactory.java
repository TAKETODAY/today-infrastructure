/*
 * Copyright 2017 - 2023 the original author or authors.
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

import cn.taketoday.lang.Nullable;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;

/**
 * Strategy interface for creating JTA {@link Transaction}
 * objects based on specified transactional characteristics.
 *
 * <p>The default implementation, {@link SimpleTransactionFactory}, simply
 * wraps a standard JTA {@link jakarta.transaction.TransactionManager}.
 * This strategy interface allows for more sophisticated implementations
 * that adapt to vendor-specific JTA extensions.
 *
 * @author Juergen Hoeller
 * @see jakarta.transaction.TransactionManager#getTransaction()
 * @see SimpleTransactionFactory
 * @see JtaTransactionManager
 * @since 4.0
 */
public interface TransactionFactory {

  /**
   * Create an active Transaction object based on the given name and timeout.
   *
   * @param name the transaction name (may be {@code null})
   * @param timeout the transaction timeout (may be -1 for the default timeout)
   * @return the active Transaction object (never {@code null})
   * @throws NotSupportedException if the transaction manager does not support
   * a transaction of the specified type
   * @throws SystemException if the transaction manager failed to create the
   * transaction
   */
  Transaction createTransaction(@Nullable String name, int timeout) throws NotSupportedException, SystemException;

  /**
   * Determine whether the underlying transaction manager supports XA transactions
   * managed by a resource adapter (i.e. without explicit XA resource enlistment).
   * <p>Typically {@code false}.
   *
   * @see jakarta.resource.spi.ResourceAdapter#endpointActivation
   * @see jakarta.resource.spi.endpoint.MessageEndpointFactory#isDeliveryTransacted
   */
  boolean supportsResourceAdapterManagedTransactions();

}
