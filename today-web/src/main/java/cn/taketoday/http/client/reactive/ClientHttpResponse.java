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

package cn.taketoday.http.client.reactive;

import cn.taketoday.core.MultiValueMap;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.ReactiveHttpInputMessage;
import cn.taketoday.http.ResponseCookie;
import cn.taketoday.util.ObjectUtils;

/**
 * Represents a client-side reactive HTTP response.
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @since 4.0
 */
public interface ClientHttpResponse extends ReactiveHttpInputMessage {

  /**
   * Return an id that represents the underlying connection, if available,
   * or the request for the purpose of correlating log messages.
   */
  default String getId() {
    return ObjectUtils.getIdentityHexString(this);
  }

  /**
   * Return the HTTP status code as an {@link HttpStatus} enum value.
   *
   * @return the HTTP status as an HttpStatus enum value (never {@code null})
   * @throws IllegalArgumentException in case of an unknown HTTP status code
   * @see HttpStatus#valueOf(int)
   * @since #getRawStatusCode()
   */
  default HttpStatusCode getStatusCode() {
    return HttpStatusCode.valueOf(getRawStatusCode());
  }

  /**
   * Return the HTTP status code (potentially non-standard and not
   * resolvable through the {@link HttpStatus} enum) as an integer.
   *
   * @return the HTTP status as an integer value
   * @see #getStatusCode()
   * @see HttpStatus#resolve(int)
   */
  int getRawStatusCode();

  /**
   * Return a read-only map of response cookies received from the server.
   */
  MultiValueMap<String, ResponseCookie> getCookies();

}
