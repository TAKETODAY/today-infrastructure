/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.web.reactive.function.client;

import java.util.List;
import java.util.function.Consumer;

import cn.taketoday.http.codec.ClientCodecConfigurer;
import cn.taketoday.http.codec.HttpMessageReader;
import cn.taketoday.http.codec.HttpMessageWriter;

/**
 * Provides strategies for use in an {@link ExchangeFunction}.
 *
 * <p>To create an instance, see the static methods {@link #withDefaults()},
 * {@link #builder()}, and {@link #empty()}.
 *
 * @author Brian Clozel
 * @author Arjen Poutsma
 * @since 4.0
 */
public interface ExchangeStrategies {

  /**
   * Return {@link HttpMessageReader HttpMessageReaders} to read and decode the response body with.
   *
   * @return the message readers
   */
  List<HttpMessageReader<?>> messageReaders();

  /**
   * Return {@link HttpMessageWriter HttpMessageWriters} to write and encode the request body with.
   *
   * @return the message writers
   */
  List<HttpMessageWriter<?>> messageWriters();

  /**
   * Return a builder to create a new {@link ExchangeStrategies} instance
   * replicated from the current instance.
   */
  default Builder mutate() {
    throw new UnsupportedOperationException();
  }

  // Static builder methods

  /**
   * Return an {@code ExchangeStrategies} instance with default configuration
   * provided by {@link ClientCodecConfigurer}.
   */
  static ExchangeStrategies withDefaults() {
    return DefaultExchangeStrategiesBuilder.DEFAULT_EXCHANGE_STRATEGIES;
  }

  /**
   * Return a builder pre-configured with default configuration to start.
   * This is the same as {@link #withDefaults()} but returns a mutable builder
   * for further customizations.
   */
  static Builder builder() {
    DefaultExchangeStrategiesBuilder builder = new DefaultExchangeStrategiesBuilder();
    builder.defaultConfiguration();
    return builder;
  }

  /**
   * Return a builder with empty configuration to start.
   */
  static Builder empty() {
    return new DefaultExchangeStrategiesBuilder();
  }

  /**
   * A mutable builder for an {@link ExchangeStrategies}.
   */
  interface Builder {

    /**
     * Customize the list of client-side HTTP message readers and writers.
     *
     * @param consumer the consumer to customize the codecs
     * @return this builder
     */
    Builder codecs(Consumer<ClientCodecConfigurer> consumer);

    /**
     * Builds the {@link ExchangeStrategies}.
     *
     * @return the built strategies
     */
    ExchangeStrategies build();
  }

}
