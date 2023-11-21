/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.annotation.config.web.reactive.client;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

import cn.taketoday.http.client.reactive.ReactorClientHttpConnector;
import cn.taketoday.lang.Assert;
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
