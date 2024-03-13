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

package cn.taketoday.transaction.interceptor;

import java.io.Serializable;
import java.lang.reflect.Method;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Composite {@link TransactionAttributeSource} implementation that iterates
 * over a given array of {@link TransactionAttributeSource} instances.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@SuppressWarnings("serial")
public class CompositeTransactionAttributeSource implements TransactionAttributeSource, Serializable {

  private final TransactionAttributeSource[] transactionAttributeSources;

  /**
   * Create a new CompositeTransactionAttributeSource for the given sources.
   *
   * @param transactionAttributeSources the TransactionAttributeSource instances to combine
   */
  public CompositeTransactionAttributeSource(TransactionAttributeSource... transactionAttributeSources) {
    Assert.notNull(transactionAttributeSources, "TransactionAttributeSource array is required");
    this.transactionAttributeSources = transactionAttributeSources;
  }

  /**
   * Return the TransactionAttributeSource instances that this
   * CompositeTransactionAttributeSource combines.
   */
  public final TransactionAttributeSource[] getTransactionAttributeSources() {
    return this.transactionAttributeSources;
  }

  @Override
  public boolean isCandidateClass(Class<?> targetClass) {
    for (TransactionAttributeSource source : this.transactionAttributeSources) {
      if (source.isCandidateClass(targetClass)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean hasTransactionAttribute(Method method, @Nullable Class<?> targetClass) {
    for (TransactionAttributeSource source : this.transactionAttributeSources) {
      if (source.hasTransactionAttribute(method, targetClass)) {
        return true;
      }
    }
    return false;
  }

  @Override
  @Nullable
  public TransactionAttribute getTransactionAttribute(Method method, @Nullable Class<?> targetClass) {
    for (TransactionAttributeSource source : this.transactionAttributeSources) {
      TransactionAttribute attr = source.getTransactionAttribute(method, targetClass);
      if (attr != null) {
        return attr;
      }
    }
    return null;
  }

}
