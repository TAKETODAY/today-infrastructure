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
package cn.taketoday.http;

import java.util.HashMap;

import cn.taketoday.lang.Nullable;

/**
 * HTTP Request Methods
 *
 * @author TODAY 2018-06-27 19:01:04
 * @version 2.0.0
 */
public enum HttpMethod {

  GET, POST, PUT, DELETE, PATCH, TRACE, HEAD, OPTIONS, CONNECT;

  private static final HashMap<String, HttpMethod> mappings = new HashMap<>(16);

  static {
    for (HttpMethod httpMethod : values()) {
      mappings.put(httpMethod.name(), httpMethod);
    }
  }

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
  public static HttpMethod from(@Nullable String method) {
    return method != null ? mappings.get(method) : null;
  }

}
