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

import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpOutputMessage;
import cn.taketoday.http.HttpRequest;

/**
 * Represents a client-side HTTP request.
 * Created via an implementation of the {@link ClientHttpRequestFactory}.
 *
 * <p>A {@code ClientHttpRequest} can be {@linkplain #execute() executed},
 * receiving a {@link ClientHttpResponse} which can be read from.
 *
 * @author Arjen Poutsma
 * @see ClientHttpRequestFactory#createRequest(java.net.URI, HttpMethod)
 * @since 4.0
 */
public interface ClientHttpRequest extends HttpRequest, HttpOutputMessage {

  /**
   * Execute this request, resulting in a {@link ClientHttpResponse} that can be read.
   *
   * @return the response result of the execution
   * @throws IOException in case of I/O errors
   */
  ClientHttpResponse execute() throws IOException;

}
