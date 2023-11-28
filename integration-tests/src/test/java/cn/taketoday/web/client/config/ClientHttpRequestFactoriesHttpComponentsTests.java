/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.web.client.config;

import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.core5.function.Resolver;
import org.apache.hc.core5.http.io.SocketConfig;

import cn.taketoday.http.client.HttpComponentsClientHttpRequestFactory;
import cn.taketoday.test.util.ReflectionTestUtils;

/**
 * Tests for {@link ClientHttpRequestFactories} when Apache Http Components is the
 * predominant HTTP client.
 *
 * @author Andy Wilkinson
 */
class ClientHttpRequestFactoriesHttpComponentsTests
        extends AbstractClientHttpRequestFactoriesTests<HttpComponentsClientHttpRequestFactory> {

  ClientHttpRequestFactoriesHttpComponentsTests() {
    super(HttpComponentsClientHttpRequestFactory.class);
  }

  @Override
  protected long connectTimeout(HttpComponentsClientHttpRequestFactory requestFactory) {
    return (long) ReflectionTestUtils.getField(requestFactory, "connectTimeout");
  }

  @Override
  @SuppressWarnings("unchecked")
  protected long readTimeout(HttpComponentsClientHttpRequestFactory requestFactory) {
    HttpClient httpClient = requestFactory.getHttpClient();
    Object connectionManager = ReflectionTestUtils.getField(httpClient, "connManager");
    SocketConfig socketConfig = ((Resolver<HttpRoute, SocketConfig>) ReflectionTestUtils.getField(connectionManager,
            "socketConfigResolver"))
            .resolve(null);
    return socketConfig.getSoTimeout().toMilliseconds();
  }

}
