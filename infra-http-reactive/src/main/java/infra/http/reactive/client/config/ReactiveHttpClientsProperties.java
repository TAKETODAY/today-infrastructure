/*
 * Copyright 2012-present the original author or authors.
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

package infra.http.reactive.client.config;

import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

import infra.context.properties.ConfigurationProperties;
import infra.http.reactive.client.ClientHttpConnectorBuilder;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} to configure the defaults used
 * for reactive HTTP clients.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see infra.http.client.config.HttpClientsProperties
 * @since 5.0
 */
@ConfigurationProperties("http.clients.reactive")
public class ReactiveHttpClientsProperties {

  /**
   * Default connector used for a client HTTP request.
   */
  public @Nullable Connector connector;

  /**
   * Supported factory types.
   */
  public enum Connector {

    /**
     * Reactor-Netty.
     */
    REACTOR(ClientHttpConnectorBuilder::reactor),

    /**
     * Apache HttpComponents HttpClient.
     */
    HTTP_COMPONENTS(ClientHttpConnectorBuilder::httpComponents),

    /**
     * Java's HttpClient.
     */
    JDK(ClientHttpConnectorBuilder::jdk);

    private final Supplier<ClientHttpConnectorBuilder<?>> builderSupplier;

    Connector(Supplier<ClientHttpConnectorBuilder<?>> builderSupplier) {
      this.builderSupplier = builderSupplier;
    }

    ClientHttpConnectorBuilder<?> builder() {
      return this.builderSupplier.get();
    }

  }

}
