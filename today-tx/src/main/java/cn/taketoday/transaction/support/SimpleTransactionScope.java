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

package cn.taketoday.transaction.support;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import cn.taketoday.beans.factory.config.Scope;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.config.CustomScopeConfigurer;
import cn.taketoday.context.support.SimpleThreadScope;
import cn.taketoday.lang.Nullable;

/**
 * A simple transaction-backed {@link Scope} implementation, delegating to
 * {@link TransactionSynchronizationManager}'s resource binding mechanism.
 *
 * <p><b>NOTE:</b> Like {@link SimpleThreadScope},
 * this transaction scope is not registered by default in common contexts. Instead,
 * you need to explicitly assign it to a scope key in your setup, either through
 * {@link ConfigurableBeanFactory#registerScope}
 * or through a {@link CustomScopeConfigurer} bean.
 *
 * @author Juergen Hoeller
 * @see SimpleThreadScope
 * @see ConfigurableBeanFactory#registerScope
 * @see CustomScopeConfigurer
 * @since 4.0
 */
public class SimpleTransactionScope implements Scope {

  @Override
  public Object get(String name, Supplier<?> objectFactory) {
    SynchronizationInfo info = TransactionSynchronizationManager.getSynchronizationInfo();

    ScopedObjectsHolder scopedObjects = info.getResource(this);
    if (scopedObjects == null) {
      scopedObjects = new ScopedObjectsHolder();
      info.registerSynchronization(new CleanupSynchronization(scopedObjects));
      info.bindResource(this, scopedObjects);
    }
    // NOTE: Do NOT modify the following to use Map::computeIfAbsent. For details,
    // see https://github.com/spring-projects/spring-framework/issues/25801.
    Object scopedObject = scopedObjects.scopedInstances.get(name);
    if (scopedObject == null) {
      scopedObject = objectFactory.get();
      scopedObjects.scopedInstances.put(name, scopedObject);
    }
    return scopedObject;
  }

  @Override
  @Nullable
  public Object remove(String name) {
    ScopedObjectsHolder scopedObjects = TransactionSynchronizationManager.getResource(this);
    if (scopedObjects != null) {
      scopedObjects.destructionCallbacks.remove(name);
      return scopedObjects.scopedInstances.remove(name);
    }
    else {
      return null;
    }
  }

  @Override
  public void registerDestructionCallback(String name, Runnable callback) {
    ScopedObjectsHolder scopedObjects = TransactionSynchronizationManager.getResource(this);
    if (scopedObjects != null) {
      scopedObjects.destructionCallbacks.put(name, callback);
    }
  }

  @Nullable
  @Override
  public Object resolveContextualObject(String key) {
    return null;
  }

  @Override
  @Nullable
  public String getConversationId() {
    return TransactionSynchronizationManager.getCurrentTransactionName();
  }

  /**
   * Holder for scoped objects.
   */
  static class ScopedObjectsHolder {

    final Map<String, Object> scopedInstances = new HashMap<>();

    final Map<String, Runnable> destructionCallbacks = new LinkedHashMap<>();
  }

  private class CleanupSynchronization implements TransactionSynchronization {

    private final ScopedObjectsHolder scopedObjects;

    public CleanupSynchronization(ScopedObjectsHolder scopedObjects) {
      this.scopedObjects = scopedObjects;
    }

    @Override
    public void suspend() {
      TransactionSynchronizationManager.unbindResource(SimpleTransactionScope.this);
    }

    @Override
    public void resume() {
      TransactionSynchronizationManager.bindResource(SimpleTransactionScope.this, this.scopedObjects);
    }

    @Override
    public void afterCompletion(int status) {
      TransactionSynchronizationManager.unbindResourceIfPossible(SimpleTransactionScope.this);
      for (Runnable callback : this.scopedObjects.destructionCallbacks.values()) {
        callback.run();
      }
      this.scopedObjects.destructionCallbacks.clear();
      this.scopedObjects.scopedInstances.clear();
    }
  }

}
