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

package cn.taketoday.retry.support;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.lang.Nullable;
import cn.taketoday.retry.RetryCallback;
import cn.taketoday.retry.RetryContext;
import cn.taketoday.retry.RetryOperations;

/**
 * Global variable support for retry clients. Normally it is not necessary for clients to
 * be aware of the surrounding environment because a {@link RetryCallback} can always use
 * the context it is passed by the enclosing {@link RetryOperations}. But occasionally it
 * might be helpful to have lower level access to the ongoing {@link RetryContext} so we
 * provide a global accessor here. The mutator methods ({@link #clear()} and
 * {@link #register(RetryContext)} should not be used except internally by
 * {@link RetryOperations} implementations.
 *
 * @author Dave Syer
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class RetrySynchronizationManager {

  private RetrySynchronizationManager() { }

  private static final ThreadLocal<RetryContext> context = new ThreadLocal<>();

  private static final Map<Thread, RetryContext> contexts = new ConcurrentHashMap<>();

  private static boolean useThreadLocal = true;

  /**
   * Set to false to store the context in a map (keyed by the current thread) instead of
   * in a {@link ThreadLocal}. Recommended when using virtual threads.
   *
   * @param use true to use a {@link ThreadLocal} (default true).
   */
  public static void setUseThreadLocal(boolean use) {
    useThreadLocal = use;
  }

  /**
   * Return true if contexts are held in a ThreadLocal (default) rather than a Map.
   *
   * @return the useThreadLocal
   */
  public static boolean isUseThreadLocal() {
    return useThreadLocal;
  }

  /**
   * Public accessor for the locally enclosing {@link RetryContext}.
   *
   * @return the current retry context, or null if there isn't one
   */
  @Nullable
  public static RetryContext getContext() {
    if (useThreadLocal) {
      return context.get();
    }
    else {
      return contexts.get(Thread.currentThread());
    }
  }

  /**
   * Method for registering a context - should only be used by {@link RetryOperations}
   * implementations to ensure that {@link #getContext()} always returns the correct
   * value.
   *
   * @param context the new context to register
   * @return the old context if there was one
   */
  @Nullable
  public static RetryContext register(RetryContext context) {
    if (useThreadLocal) {
      RetryContext oldContext = getContext();
      RetrySynchronizationManager.context.set(context);
      return oldContext;
    }
    else {
      RetryContext oldContext = contexts.get(Thread.currentThread());
      contexts.put(Thread.currentThread(), context);
      return oldContext;
    }
  }

  /**
   * Clear the current context at the end of a batch - should only be used by
   * {@link RetryOperations} implementations.
   *
   * @return the old value if there was one.
   */
  @Nullable
  public static RetryContext clear() {
    RetryContext value = getContext();
    RetryContext parent = value == null ? null : value.getParent();
    if (useThreadLocal) {
      RetrySynchronizationManager.context.set(parent);
    }
    else {
      if (parent != null) {
        contexts.put(Thread.currentThread(), parent);
      }
      else {
        contexts.remove(Thread.currentThread());
      }
    }
    return value;
  }

}
