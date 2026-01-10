/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.test.web.reactive.server;

import org.jspecify.annotations.Nullable;

import infra.http.client.reactive.ClientHttpConnector;
import infra.test.web.mock.client.MockMvcHttpConnector;

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
