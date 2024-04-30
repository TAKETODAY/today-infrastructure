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

package cn.taketoday.web;

import cn.taketoday.lang.Nullable;

/**
 * Holder class to expose the web request in the form of a thread-bound
 * {@link RequestContext} object.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2.3.7 2019-03-23 10:29
 */
public abstract class RequestContextHolder {

  private static final RequestThreadLocal contextHolder = RequestThreadLocal.lookup();

  /**
   * cleanup request context
   */
  public static void cleanup() {
    contextHolder.remove();
  }

  public static void set(RequestContext requestContext) {
    contextHolder.set(requestContext);
  }

  /**
   * current context
   */
  @Nullable
  public static RequestContext get() {
    return contextHolder.get();
  }

  public static RequestContext getRequired() {
    RequestContext context = contextHolder.get();
    if (context == null) {
      throw new IllegalStateException("No RequestContext set");
    }
    return context;
  }

}
