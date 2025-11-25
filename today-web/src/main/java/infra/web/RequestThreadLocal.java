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

    @Nullable
    @Override
    public RequestContext get() {
      return threadLocal.get();
    }

    @Override
    public void set(@Nullable RequestContext context) {
      threadLocal.set(context);
    }
  }

}
