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

package infra.http;

import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Enumeration of HTTP request methods. Intended for use with the
 * {@link infra.web.annotation.RequestMapping#method()} attribute of
 * the {@link infra.web.annotation.RequestMapping} annotation.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @version 2.0.0 2018-06-27 19:01:04
 */
public enum HttpMethod {

  GET, POST, PUT, DELETE, PATCH, TRACE, HEAD, OPTIONS, CONNECT;

  private static final Map<String, HttpMethod> mappings = Map.of(GET.name(), GET, POST.name(), POST, PUT.name(), PUT, DELETE.name(),
          DELETE, PATCH.name(), PATCH, TRACE.name(), TRACE, HEAD.name(), HEAD, OPTIONS.name(), OPTIONS, CONNECT.name(), CONNECT);

  /**
   * Determine whether this {@code HttpMethod} matches the given method value.
   *
   * @param method the method value as a String. <b>Must Upper Case</b>
   * @return {@code true} if it matches, {@code false} otherwise
   */
  public boolean matches(String method) {
    return name().equals(method);
  }

  /**
   * Resolve the given method value to an {@code HttpMethod}.
   *
   * @param method the method value as a String
   * @return the corresponding {@code HttpMethod}, or {@code null} if not found
   * @since 4.0
   */
  public static @Nullable HttpMethod resolve(@Nullable String method) {
    return method != null ? mappings.get(method) : null;
  }

}
