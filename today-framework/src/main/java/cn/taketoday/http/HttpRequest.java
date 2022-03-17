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

import java.net.URI;

import cn.taketoday.lang.Nullable;

/**
 * Represents an HTTP request message, consisting of
 * {@linkplain #getMethod() method} and {@linkplain #getURI() uri}.
 *
 * @author Arjen Poutsma
 * @since 4.0
 */
public interface HttpRequest extends HttpMessage {

  /**
   * Return the HTTP method of the request.
   *
   * @return the HTTP method as an HttpMethod enum value, or {@code null}
   * if not resolvable (e.g. in case of a non-standard HTTP method)
   * @see #getMethodValue()
   * @see HttpMethod#from(String)
   */
  @Nullable
  default HttpMethod getMethod() {
    return HttpMethod.from(getMethodValue());
  }

  /**
   * Return the HTTP method of the request as a String value.
   *
   * @return the HTTP method as a plain String
   * @see #getMethod()
   */
  String getMethodValue();

  /**
   * Return the URI of the request (including a query string if any,
   * but only if it is well-formed for a URI representation).
   *
   * @return the URI of the request (never {@code null})
   */
  URI getURI();

}
