/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.annotation.config.web.reactive.client;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

import infra.http.client.reactive.ReactorClientHttpConnector;
import infra.lang.Assert;
import reactor.netty.http.client.HttpClient;

/**
 * Mapper that allows for custom modification of a {@link HttpClient} before it is used as
 * the basis for a {@link ReactorClientHttpConnector}.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@FunctionalInterface
public interface ReactorNettyHttpClientMapper extends Function<HttpClient, HttpClient> {

  /**
   * Configure the given {@link HttpClient} and return the newly created instance.
   *
   * @param httpClient the client to configure
   * @return the new client instance
   */
  @Override
  HttpClient apply(HttpClient httpClient);

  /**
   * Return a new {@link ReactorNettyHttpClientMapper} composed of the given mappers.
   *
   * @param mappers the mappers to compose
   * @return a composed {@link ReactorNettyHttpClientMapper} instance
   */
  static ReactorNettyHttpClientMapper forCompose(ReactorNettyHttpClientMapper... mappers) {
    Assert.notNull(mappers, "Mappers is required");
    return forCompose(Arrays.asList(mappers));
  }

  /**
   * Return a new {@link ReactorNettyHttpClientMapper} composed of the given mappers.
   *
   * @param mappers the mappers to compose
   * @return a composed {@link ReactorNettyHttpClientMapper} instance
   */
  static ReactorNettyHttpClientMapper forCompose(Collection<ReactorNettyHttpClientMapper> mappers) {
    Assert.notNull(mappers, "Mappers is required");
    return httpClient -> {
      for (ReactorNettyHttpClientMapper mapper : mappers) {
        httpClient = mapper.apply(httpClient);
      }
      return httpClient;
    };
  }

}
