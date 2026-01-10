/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web;

import org.jspecify.annotations.Nullable;

import infra.core.NamedThreadLocal;
import infra.lang.TodayStrategies;
import infra.lang.VisibleForTesting;

/**
 * Abstract base class for managing request context in a thread-local manner.
 * Provides mechanisms to get, set, and remove the current request context.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.0 2021/4/1 19:28
 */
public abstract class RequestThreadLocal {

  /**
   * Remove the current request context from the thread local storage.
   */
  public abstract void remove();

  /**
   * Get the current request context from the thread local storage.
   *
   * @return the current request context, or {@code null} if none is bound
   */
  public abstract @Nullable RequestContext get();

  /**
   * Set the current request context in the thread local storage.
   *
   * @param context the request context to bind, or {@code null} to clear
   */
  public abstract void set(@Nullable RequestContext context);

  /**
   * Static factory method to lookup and create an appropriate RequestThreadLocal instance.
   * <p>
   * This method first attempts to find an implementation via {@link TodayStrategies}.
   * If none is found, it checks for the presence of Netty's FastThreadLocal class.
   * If available, it returns a Netty-based implementation; otherwise, it falls back
   * to a default implementation using NamedThreadLocal.
   *
   * @return an appropriate RequestThreadLocal instance
   */
  public static RequestThreadLocal lookup() {
    RequestThreadLocal ret = TodayStrategies.findFirst(RequestThreadLocal.class, null);
    if (ret == null) {
      return new Default();
    }
    return ret;
  }

  /**
   * Default implementation using {@link NamedThreadLocal} for storing request context.
   */
  @VisibleForTesting
  static final class Default extends RequestThreadLocal {
    private final NamedThreadLocal<RequestContext> threadLocal = new NamedThreadLocal<>("Current Request Context");

    @Override
    public void remove() {
      threadLocal.remove();
    }

    @Override
    public @Nullable RequestContext get() {
      return threadLocal.get();
    }

    @Override
    public void set(@Nullable RequestContext context) {
      if (context == null) {
        threadLocal.remove();
      }
      else {
        threadLocal.set(context);
      }
    }
  }

}
