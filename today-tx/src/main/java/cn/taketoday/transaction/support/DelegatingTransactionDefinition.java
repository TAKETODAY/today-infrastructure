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

package cn.taketoday.transaction.support;

import java.io.Serializable;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.transaction.TransactionDefinition;

/**
 * {@link TransactionDefinition} implementation that delegates all calls to a given target
 * {@link TransactionDefinition} instance. Abstract because it is meant to be subclassed,
 * with subclasses overriding specific methods that are not supposed to simply delegate
 * to the target instance.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
@SuppressWarnings("serial")
public abstract class DelegatingTransactionDefinition implements TransactionDefinition, Serializable {

  private final TransactionDefinition targetDefinition;

  /**
   * Create a DelegatingTransactionAttribute for the given target attribute.
   *
   * @param targetDefinition the target TransactionAttribute to delegate to
   */
  public DelegatingTransactionDefinition(TransactionDefinition targetDefinition) {
    Assert.notNull(targetDefinition, "Target definition is required");
    this.targetDefinition = targetDefinition;
  }

  @Override
  public int getPropagationBehavior() {
    return this.targetDefinition.getPropagationBehavior();
  }

  @Override
  public int getIsolationLevel() {
    return this.targetDefinition.getIsolationLevel();
  }

  @Override
  public int getTimeout() {
    return this.targetDefinition.getTimeout();
  }

  @Override
  public boolean isReadOnly() {
    return this.targetDefinition.isReadOnly();
  }

  @Override
  @Nullable
  public String getName() {
    return this.targetDefinition.getName();
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return this.targetDefinition.equals(other);
  }

  @Override
  public int hashCode() {
    return this.targetDefinition.hashCode();
  }

  @Override
  public String toString() {
    return this.targetDefinition.toString();
  }

}
