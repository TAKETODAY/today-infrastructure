/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.transaction.reactive;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import cn.taketoday.lang.Nullable;

/**
 * Mutable transaction context that encapsulates transactional synchronizations and
 * resources in the scope of a single transaction. Transaction context is typically
 * held by an outer {@link TransactionContextHolder} or referenced directly within
 * from the subscriber context.
 *
 * @author Mark Paluch
 * @author Juergen Hoeller
 * @see TransactionContextManager
 * @see reactor.util.context.Context
 * @since 4.0
 */
public class TransactionContext {

  @Nullable
  private final TransactionContext parent;

  private final LinkedHashMap<Object, Object> resources = new LinkedHashMap<>();

  @Nullable
  private Set<TransactionSynchronization> synchronizations;

  @Nullable
  private volatile String currentTransactionName;

  private volatile boolean currentTransactionReadOnly;

  @Nullable
  private volatile Integer currentTransactionIsolationLevel;

  private volatile boolean actualTransactionActive;

  TransactionContext() {
    this(null);
  }

  TransactionContext(@Nullable TransactionContext parent) {
    this.parent = parent;
  }

  @Nullable
  public TransactionContext getParent() {
    return this.parent;
  }

  public Map<Object, Object> getResources() {
    return this.resources;
  }

  public void setSynchronizations(@Nullable Set<TransactionSynchronization> synchronizations) {
    this.synchronizations = synchronizations;
  }

  @Nullable
  public Set<TransactionSynchronization> getSynchronizations() {
    return this.synchronizations;
  }

  public void setCurrentTransactionName(@Nullable String currentTransactionName) {
    this.currentTransactionName = currentTransactionName;
  }

  @Nullable
  public String getCurrentTransactionName() {
    return this.currentTransactionName;
  }

  public void setCurrentTransactionReadOnly(boolean currentTransactionReadOnly) {
    this.currentTransactionReadOnly = currentTransactionReadOnly;
  }

  public boolean isCurrentTransactionReadOnly() {
    return this.currentTransactionReadOnly;
  }

  public void setCurrentTransactionIsolationLevel(@Nullable Integer currentTransactionIsolationLevel) {
    this.currentTransactionIsolationLevel = currentTransactionIsolationLevel;
  }

  @Nullable
  public Integer getCurrentTransactionIsolationLevel() {
    return this.currentTransactionIsolationLevel;
  }

  public void setActualTransactionActive(boolean actualTransactionActive) {
    this.actualTransactionActive = actualTransactionActive;
  }

  public boolean isActualTransactionActive() {
    return this.actualTransactionActive;
  }

  public void clear() {
    this.synchronizations = null;
    this.currentTransactionName = null;
    this.currentTransactionReadOnly = false;
    this.currentTransactionIsolationLevel = null;
    this.actualTransactionActive = false;
  }

}
