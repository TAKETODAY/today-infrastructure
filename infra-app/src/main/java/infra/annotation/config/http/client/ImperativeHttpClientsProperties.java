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

package infra.annotation.config.http.client;

import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

import infra.context.properties.ConfigurationProperties;
import infra.http.client.config.ClientHttpRequestFactoryBuilder;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} to configure the defaults used
 * for imperative HTTP clients.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see HttpClientsProperties
 * @since 5.0
 */
@ConfigurationProperties("http.clients.imperative")
public class ImperativeHttpClientsProperties {

  /**
   * Default factory used for a client HTTP request.
   */
  public @Nullable Factory factory;

  /**
   * Supported factory types.
   */
  public enum Factory {

    /**
     * Apache HttpComponents HttpClient.
     */
    HTTP_COMPONENTS(ClientHttpRequestFactoryBuilder::httpComponents),

    /**
     * Reactor-Netty.
     */
    REACTOR(ClientHttpRequestFactoryBuilder::reactor),

    /**
     * Java's HttpClient.
     */
    JDK(ClientHttpRequestFactoryBuilder::jdk);

    private final Supplier<ClientHttpRequestFactoryBuilder<?>> builderSupplier;

    Factory(Supplier<ClientHttpRequestFactoryBuilder<?>> builderSupplier) {
      this.builderSupplier = builderSupplier;
    }

    ClientHttpRequestFactoryBuilder<?> builder() {
      return this.builderSupplier.get();
    }

  }

}
