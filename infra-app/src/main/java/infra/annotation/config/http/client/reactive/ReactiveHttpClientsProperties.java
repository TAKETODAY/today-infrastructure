/*
 * Copyright 2017 - 2026 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.annotation.config.http.client.reactive;

import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

import infra.annotation.config.http.client.HttpClientsProperties;
import infra.context.properties.ConfigurationProperties;
import infra.http.client.config.reactive.ClientHttpConnectorBuilder;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} to configure the defaults used
 * for reactive HTTP clients.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see HttpClientsProperties
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
