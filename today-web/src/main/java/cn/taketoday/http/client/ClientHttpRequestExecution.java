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
package cn.taketoday.http.client;

import java.io.IOException;

import cn.taketoday.http.HttpRequest;

/**
 * Represents the context of a client-side HTTP request execution.
 *
 * <p>Used to invoke the next interceptor in the interceptor chain,
 * or - if the calling interceptor is last - execute the request itself.
 *
 * @author Arjen Poutsma
 * @see ClientHttpRequestInterceptor
 * @since 4.0
 */
@FunctionalInterface
public interface ClientHttpRequestExecution {

  /**
   * Execute the request with the given request attributes and body,
   * and return the response.
   *
   * @param request the request, containing method, URI, and headers
   * @param body the body of the request to execute
   * @return the response
   * @throws IOException in case of I/O errors
   */
  ClientHttpResponse execute(HttpRequest request, byte[] body) throws IOException;

}
