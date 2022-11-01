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
import cn.taketoday.lang.Assert;

/**
 * Abstract base class for {@link ClientHttpRequestFactory} implementations
 * that decorate another request factory.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Arjen Poutsma
 * @since 4.0
 */
public class ClientHttpRequestFactoryWrapper implements ClientHttpRequestFactory {

  private final ClientHttpRequestFactory requestFactory;

  /**
   * Create a {@code AbstractClientHttpRequestFactoryWrapper} wrapping the given request factory.
   *
   * @param requestFactory the request factory to be wrapped
   */
  public ClientHttpRequestFactoryWrapper(ClientHttpRequestFactory requestFactory) {
    Assert.notNull(requestFactory, "ClientHttpRequestFactory must not be null");
    this.requestFactory = requestFactory;
  }

  /**
   * This implementation simply calls {@link #createRequest(URI, HttpMethod, ClientHttpRequestFactory)}
   * with the wrapped request factory provided to the
   * {@linkplain #ClientHttpRequestFactoryWrapper(ClientHttpRequestFactory) constructor}.
   */
  @Override
  public final ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
    return createRequest(uri, httpMethod, requestFactory);
  }

  /**
   * Create a new {@link ClientHttpRequest} for the specified URI and HTTP method
   * by using the passed-on request factory.
   * <p>Called from {@link #createRequest(URI, HttpMethod)}.
   *
   * @param uri the URI to create a request for
   * @param httpMethod the HTTP method to execute
   * @param requestFactory the wrapped request factory
   * @return the created request
   * @throws IOException in case of I/O errors
   */
  protected ClientHttpRequest createRequest(
          URI uri, HttpMethod httpMethod, ClientHttpRequestFactory requestFactory) throws IOException {
    return requestFactory.createRequest(uri, httpMethod);
  }

  public ClientHttpRequestFactory getRequestFactory() {
    return requestFactory;
  }

}
