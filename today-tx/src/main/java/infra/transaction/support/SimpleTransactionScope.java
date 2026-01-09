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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.function.Supplier;

import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.config.CustomScopeConfigurer;
import infra.beans.factory.config.Scope;
import infra.context.support.SimpleThreadScope;

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
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
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

    public final HashMap<String, Object> scopedInstances = new HashMap<>();

    public final LinkedHashMap<String, Runnable> destructionCallbacks = new LinkedHashMap<>();

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
