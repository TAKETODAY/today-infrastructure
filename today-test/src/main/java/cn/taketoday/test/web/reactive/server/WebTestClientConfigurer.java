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

package cn.taketoday.test.web.reactive.server;

import cn.taketoday.http.client.reactive.ClientHttpConnector;
import cn.taketoday.lang.Nullable;
import cn.taketoday.test.web.mock.client.MockMvcHttpConnector;

/**
 * Contract to encapsulate customizations to a {@link WebTestClient.Builder}.
 * Typically used by frameworks that wish to provide a shortcut for common
 * initialization.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see MockServerConfigurer
 * @since 4.0
 */
public interface WebTestClientConfigurer {

  /**
   * Use methods on {@link WebTestClient.Builder} to modify test client
   * settings. For a mock WebFlux server, For a MockMvc server, mutate the
   * {@link MockMvcHttpConnector} and set it on {@link WebTestClient.Builder}.
   *
   * @param builder the WebTestClient builder for test client changes
   * @param connector the connector in use
   */
  void afterConfigurerAdded(WebTestClient.Builder builder, @Nullable ClientHttpConnector connector);

}
