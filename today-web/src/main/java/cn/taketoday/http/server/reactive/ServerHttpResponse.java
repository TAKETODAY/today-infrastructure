/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.http.server.reactive;

import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.ReactiveHttpOutputMessage;
import cn.taketoday.http.ResponseCookie;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MultiValueMap;

/**
 * Represents a reactive server-side HTTP response.
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface ServerHttpResponse extends ReactiveHttpOutputMessage {

  /**
   * Set the HTTP status code of the response.
   *
   * @param status the HTTP status as an {@link HttpStatus} enum value
   * @return {@code false} if the status code change wasn't processed because
   * the HTTP response is committed, {@code true} if successfully set.
   */
  boolean setStatusCode(@Nullable HttpStatus status);

  /**
   * Return the status code that has been set, or otherwise fall back on the
   * status of the response from the underlying server. The return value may
   * be {@code null} if the status code value is outside the
   * {@link HttpStatus} enum range, or if there is no default value from the
   * underlying server.
   */
  @Nullable
  HttpStatusCode getStatusCode();

  /**
   * Set the HTTP status code to the given value (potentially non-standard and
   * not resolvable through the {@link HttpStatus} enum) as an integer.
   *
   * @param value the status code value
   * @return {@code false} if the status code change wasn't processed because
   * the HTTP response is committed, {@code true} if successfully set.
   */
  default boolean setRawStatusCode(@Nullable Integer value) {
    if (value == null) {
      return setStatusCode(null);
    }
    else {
      HttpStatus httpStatus = HttpStatus.resolve(value);
      if (httpStatus == null) {
        throw new IllegalStateException(
                "Unresolvable HttpStatus for general ServerHttpResponse: " + value);
      }
      return setStatusCode(httpStatus);
    }
  }

  /**
   * Return the status code that has been set, or otherwise fall back on the
   * status of the response from the underlying server. The return value may
   * be {@code null} if there is no default value from the underlying server.
   */
  @Nullable
  default Integer getRawStatusCode() {
    HttpStatusCode httpStatus = getStatusCode();
    return httpStatus != null ? httpStatus.value() : null;
  }

  /**
   * Return a mutable map with the cookies to send to the server.
   */
  MultiValueMap<String, ResponseCookie> getCookies();

  /**
   * Add the given {@code ResponseCookie}.
   *
   * @param cookie the cookie to add
   * @throws IllegalStateException if the response has already been committed
   */
  void addCookie(ResponseCookie cookie);

}
