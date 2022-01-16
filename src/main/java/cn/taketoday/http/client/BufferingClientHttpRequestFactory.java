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
import java.net.URI;

import cn.taketoday.http.HttpMethod;

/**
 * Wrapper for a {@link ClientHttpRequestFactory} that buffers
 * all outgoing and incoming streams in memory.
 *
 * <p>Using this wrapper allows for multiple reads of the
 * {@linkplain ClientHttpResponse#getBody() response body}.
 *
 * @author Arjen Poutsma
 * @since 4.0
 */
public class BufferingClientHttpRequestFactory extends ClientHttpRequestFactoryWrapper {

  /**
   * Create a buffering wrapper for the given {@link ClientHttpRequestFactory}.
   *
   * @param requestFactory the target request factory to wrap
   */
  public BufferingClientHttpRequestFactory(ClientHttpRequestFactory requestFactory) {
    super(requestFactory);
  }

  @Override
  protected ClientHttpRequest createRequest(
          URI uri, HttpMethod httpMethod, ClientHttpRequestFactory requestFactory) throws IOException {
    ClientHttpRequest request = requestFactory.createRequest(uri, httpMethod);
    if (shouldBuffer(uri, httpMethod)) {
      return new BufferingClientHttpRequestWrapper(request);
    }
    else {
      return request;
    }
  }

  /**
   * Indicates whether the request/response exchange for the given URI and method
   * should be buffered in memory.
   * <p>The default implementation returns {@code true} for all URIs and methods.
   * Subclasses can override this method to change this behavior.
   *
   * @param uri the URI
   * @param httpMethod the method
   * @return {@code true} if the exchange should be buffered; {@code false} otherwise
   */
  protected boolean shouldBuffer(URI uri, HttpMethod httpMethod) {
    return true;
  }

}
