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
