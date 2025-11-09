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

import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.util.Collection;

import infra.transaction.support.DelegatingTransactionDefinition;

/**
 * {@link TransactionAttribute} implementation that delegates all calls to a given target
 * {@link TransactionAttribute} instance. Abstract because it is meant to be subclassed,
 * with subclasses overriding specific methods that are not supposed to simply delegate
 * to the target instance.
 *
 * @author Juergen Hoeller
 * @author Mark Paluch
 * @since 4.0
 */
@SuppressWarnings("serial")
public abstract class DelegatingTransactionAttribute extends DelegatingTransactionDefinition
        implements TransactionAttribute, Serializable {

  private final TransactionAttribute targetAttribute;

  /**
   * Create a DelegatingTransactionAttribute for the given target attribute.
   *
   * @param targetAttribute the target TransactionAttribute to delegate to
   */
  public DelegatingTransactionAttribute(TransactionAttribute targetAttribute) {
    super(targetAttribute);
    this.targetAttribute = targetAttribute;
  }

  @Override
  @Nullable
  public String getQualifier() {
    return this.targetAttribute.getQualifier();
  }

  @Override
  public Collection<String> getLabels() {
    return this.targetAttribute.getLabels();
  }

  @Override
  public boolean rollbackOn(Throwable ex) {
    return this.targetAttribute.rollbackOn(ex);
  }

}
