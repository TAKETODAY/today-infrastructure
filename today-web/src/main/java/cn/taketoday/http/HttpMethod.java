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

package cn.taketoday.http;

import java.util.Map;

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.annotation.RequestMapping;

/**
 * Enumeration of HTTP request methods. Intended for use with the
 * {@link RequestMapping#method()} attribute of the {@link RequestMapping} annotation.
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
  @Nullable
  public static HttpMethod resolve(@Nullable String method) {
    return method != null ? mappings.get(method) : null;
  }

}
