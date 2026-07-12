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

package infra.web;

import org.jspecify.annotations.Nullable;

/**
 * Holder class to expose the web request in the form of a thread-bound
 * {@link HttpContext} object.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2.3.7 2019-03-23 10:29
 */
public abstract class HttpContextHolder {

  private static final RequestThreadLocal contextHolder = RequestThreadLocal.lookup();

  /**
   * Reset the {@link HttpContext} for the current thread.
   */
  public static void cleanup() {
    contextHolder.remove();
  }

  /**
   * Bind the given {@link HttpContext} to the current thread.
   *
   * @param httpContext the request context to bind, or {@code null} to reset the thread-local
   */
  public static void set(@Nullable HttpContext httpContext) {
    contextHolder.set(httpContext);
  }

  /**
   * Return the {@link HttpContext} currently bound to the thread.
   *
   * @return the current request context, or {@code null} if none bound
   */
  public static @Nullable HttpContext current() {
    return contextHolder.get();
  }

  /**
   * Return the {@link HttpContext} currently bound to the thread.
   *
   * @return the current request context (never {@code null})
   * @throws IllegalStateException if no request context is bound to the current thread
   */
  public static HttpContext required() {
    HttpContext context = contextHolder.get();
    if (context == null) {
      throw new IllegalStateException("No HttpContext set");
    }
    return context;
  }

}
