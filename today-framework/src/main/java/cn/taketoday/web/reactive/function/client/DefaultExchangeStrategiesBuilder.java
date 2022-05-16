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
 * Default implementation of {@link ExchangeStrategies.Builder}.
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class DefaultExchangeStrategiesBuilder implements ExchangeStrategies.Builder {

  final static ExchangeStrategies DEFAULT_EXCHANGE_STRATEGIES;

  static {
    DefaultExchangeStrategiesBuilder builder = new DefaultExchangeStrategiesBuilder();
    builder.defaultConfiguration();
    DEFAULT_EXCHANGE_STRATEGIES = builder.build();
  }

  private final ClientCodecConfigurer codecConfigurer;

  public DefaultExchangeStrategiesBuilder() {
    this.codecConfigurer = ClientCodecConfigurer.create();
    this.codecConfigurer.registerDefaults(false);
  }

  private DefaultExchangeStrategiesBuilder(DefaultExchangeStrategies other) {
    this.codecConfigurer = other.codecConfigurer.clone();
  }

  public void defaultConfiguration() {
    this.codecConfigurer.registerDefaults(true);
  }

  @Override
  public ExchangeStrategies.Builder codecs(Consumer<ClientCodecConfigurer> consumer) {
    consumer.accept(this.codecConfigurer);
    return this;
  }

  @Override
  public ExchangeStrategies build() {
    return new DefaultExchangeStrategies(this.codecConfigurer);
  }

  private static class DefaultExchangeStrategies implements ExchangeStrategies {

    private final ClientCodecConfigurer codecConfigurer;

    private final List<HttpMessageReader<?>> readers;

    private final List<HttpMessageWriter<?>> writers;

    public DefaultExchangeStrategies(ClientCodecConfigurer codecConfigurer) {
      this.codecConfigurer = codecConfigurer;
      this.readers = List.copyOf(codecConfigurer.getReaders());
      this.writers = List.copyOf(codecConfigurer.getWriters());
    }

    @Override
    public List<HttpMessageReader<?>> messageReaders() {
      return this.readers;
    }

    @Override
    public List<HttpMessageWriter<?>> messageWriters() {
      return this.writers;
    }

    @Override
    public Builder mutate() {
      return new DefaultExchangeStrategiesBuilder(this);
    }
  }

}
