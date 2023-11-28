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

import java.net.URI;

import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.ReactiveHttpOutputMessage;
import cn.taketoday.util.MultiValueMap;

/**
 * Represents a client-side reactive HTTP request.
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @since 4.0
 */
public interface ClientHttpRequest extends ReactiveHttpOutputMessage {

  /**
   * Return the HTTP method of the request.
   */
  HttpMethod getMethod();

  /**
   * Return the URI of the request.
   */
  URI getURI();

  /**
   * Return a mutable map of request cookies to send to the server.
   */
  MultiValueMap<String, HttpCookie> getCookies();

  /**
   * Return the request from the underlying HTTP library.
   *
   * @param <T> the expected type of the request to cast to
   */
  <T> T getNativeRequest();

}
