/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.test.web.reactive.server;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.client.reactive.ClientHttpConnector;
import cn.taketoday.http.client.reactive.HttpComponentsClientHttpConnector;
import cn.taketoday.http.client.reactive.JdkClientHttpConnector;
import cn.taketoday.http.client.reactive.JettyClientHttpConnector;
import cn.taketoday.http.client.reactive.ReactorClientHttpConnector;
import cn.taketoday.http.client.reactive.ReactorNetty2ClientHttpConnector;
import cn.taketoday.http.codec.ClientCodecConfigurer;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.LinkedMultiValueMap;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.web.reactive.function.client.ExchangeFilterFunction;
import cn.taketoday.web.reactive.function.client.ExchangeFunction;
import cn.taketoday.web.reactive.function.client.ExchangeFunctions;
import cn.taketoday.web.reactive.function.client.ExchangeStrategies;
import cn.taketoday.web.util.DefaultUriBuilderFactory;
import cn.taketoday.web.util.UriBuilderFactory;

/**
 * Default implementation of {@link WebTestClient.Builder}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class DefaultWebTestClientBuilder implements WebTestClient.Builder {

  private static final boolean reactorNettyClientPresent;

  private static final boolean reactorNetty2ClientPresent;

  private static final boolean jettyClientPresent;

  private static final boolean httpComponentsClientPresent;

  static {
    ClassLoader loader = DefaultWebTestClientBuilder.class.getClassLoader();
    reactorNettyClientPresent = ClassUtils.isPresent("reactor.netty.http.client.HttpClient", loader);
    reactorNetty2ClientPresent = ClassUtils.isPresent("reactor.netty5.http.client.HttpClient", loader);
    jettyClientPresent = ClassUtils.isPresent("org.eclipse.jetty.client.HttpClient", loader);
    httpComponentsClientPresent =
            ClassUtils.isPresent("org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient", loader) &&
                    ClassUtils.isPresent("org.apache.hc.core5.reactive.ReactiveDataConsumer", loader);
  }

  @Nullable
  private ClientHttpConnector connector;

  @Nullable
  private String baseUrl;

  @Nullable
  private UriBuilderFactory uriBuilderFactory;

  @Nullable
  private HttpHeaders defaultHeaders;

  @Nullable
  private MultiValueMap<String, String> defaultCookies;

  @Nullable
  private List<ExchangeFilterFunction> filters;

  private Consumer<EntityExchangeResult<?>> entityResultConsumer = result -> { };

  @Nullable
  private ExchangeStrategies strategies;

  @Nullable
  private List<Consumer<ExchangeStrategies.Builder>> strategiesConfigurers;

  @Nullable
  private Duration responseTimeout;

  DefaultWebTestClientBuilder() {
    this((ClientHttpConnector) null);
  }

  DefaultWebTestClientBuilder(@Nullable ClientHttpConnector connector) {
    this.connector = connector;
  }

  /** Copy constructor. */
  DefaultWebTestClientBuilder(DefaultWebTestClientBuilder other) {
    this.connector = other.connector;
    this.responseTimeout = other.responseTimeout;

    this.baseUrl = other.baseUrl;
    this.uriBuilderFactory = other.uriBuilderFactory;
    if (other.defaultHeaders != null) {
      this.defaultHeaders = HttpHeaders.forWritable();
      this.defaultHeaders.putAll(other.defaultHeaders);
    }
    else {
      this.defaultHeaders = null;
    }
    this.defaultCookies = (other.defaultCookies != null ?
                           new LinkedMultiValueMap<>(other.defaultCookies) : null);
    this.filters = (other.filters != null ? new ArrayList<>(other.filters) : null);
    this.entityResultConsumer = other.entityResultConsumer;
    this.strategies = other.strategies;
    this.strategiesConfigurers = (other.strategiesConfigurers != null ?
                                  new ArrayList<>(other.strategiesConfigurers) : null);
  }

  @Override
  public WebTestClient.Builder baseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
    return this;
  }

  @Override
  public WebTestClient.Builder uriBuilderFactory(UriBuilderFactory uriBuilderFactory) {
    this.uriBuilderFactory = uriBuilderFactory;
    return this;
  }

  @Override
  public WebTestClient.Builder defaultHeader(String header, String... values) {
    initHeaders().put(header, Arrays.asList(values));
    return this;
  }

  @Override
  public WebTestClient.Builder defaultHeaders(Consumer<HttpHeaders> headersConsumer) {
    headersConsumer.accept(initHeaders());
    return this;
  }

  private HttpHeaders initHeaders() {
    if (this.defaultHeaders == null) {
      this.defaultHeaders = HttpHeaders.forWritable();
    }
    return this.defaultHeaders;
  }

  @Override
  public WebTestClient.Builder defaultCookie(String cookie, String... values) {
    initCookies().addAll(cookie, Arrays.asList(values));
    return this;
  }

  @Override
  public WebTestClient.Builder defaultCookies(Consumer<MultiValueMap<String, String>> cookiesConsumer) {
    cookiesConsumer.accept(initCookies());
    return this;
  }

  private MultiValueMap<String, String> initCookies() {
    if (this.defaultCookies == null) {
      this.defaultCookies = new LinkedMultiValueMap<>(3);
    }
    return this.defaultCookies;
  }

  @Override
  public WebTestClient.Builder filter(ExchangeFilterFunction filter) {
    Assert.notNull(filter, "ExchangeFilterFunction is required");
    initFilters().add(filter);
    return this;
  }

  @Override
  public WebTestClient.Builder filters(Consumer<List<ExchangeFilterFunction>> filtersConsumer) {
    filtersConsumer.accept(initFilters());
    return this;
  }

  private List<ExchangeFilterFunction> initFilters() {
    if (this.filters == null) {
      this.filters = new ArrayList<>();
    }
    return this.filters;
  }

  @Override
  public WebTestClient.Builder entityExchangeResultConsumer(Consumer<EntityExchangeResult<?>> entityResultConsumer) {
    Assert.notNull(entityResultConsumer, "'entityResultConsumer' is required");
    this.entityResultConsumer = this.entityResultConsumer.andThen(entityResultConsumer);
    return this;
  }

  @Override
  public WebTestClient.Builder codecs(Consumer<ClientCodecConfigurer> configurer) {
    if (this.strategiesConfigurers == null) {
      this.strategiesConfigurers = new ArrayList<>(4);
    }
    this.strategiesConfigurers.add(builder -> builder.codecs(configurer));
    return this;
  }

  @Override
  public WebTestClient.Builder exchangeStrategies(ExchangeStrategies strategies) {
    this.strategies = strategies;
    return this;
  }

  @Override
  public WebTestClient.Builder exchangeStrategies(Consumer<ExchangeStrategies.Builder> configurer) {
    if (this.strategiesConfigurers == null) {
      this.strategiesConfigurers = new ArrayList<>(4);
    }
    this.strategiesConfigurers.add(configurer);
    return this;
  }

  @Override
  public WebTestClient.Builder apply(WebTestClientConfigurer configurer) {
    configurer.afterConfigurerAdded(this, this.connector);
    return this;
  }

  @Override
  public WebTestClient.Builder responseTimeout(Duration timeout) {
    this.responseTimeout = timeout;
    return this;
  }

  @Override
  public WebTestClient.Builder clientConnector(ClientHttpConnector connector) {
    this.connector = connector;
    return this;
  }

  @Override
  public WebTestClient build() {
    ClientHttpConnector connectorToUse = this.connector;
    if (connectorToUse == null) {
      connectorToUse = initConnector();
    }
    Function<ClientHttpConnector, ExchangeFunction> exchangeFactory = connector -> {
      ExchangeFunction exchange = ExchangeFunctions.create(connector, initExchangeStrategies());
      if (CollectionUtils.isEmpty(this.filters)) {
        return exchange;
      }
      return this.filters.stream()
              .reduce(ExchangeFilterFunction::andThen)
              .map(filter -> filter.apply(exchange))
              .orElse(exchange);

    };
    return new DefaultWebTestClient(connectorToUse, exchangeFactory, initUriBuilderFactory(),
            this.defaultHeaders != null ? defaultHeaders.asReadOnly() : null,
            this.defaultCookies != null ? defaultCookies.asReadOnly() : null,
            this.entityResultConsumer, this.responseTimeout, new DefaultWebTestClientBuilder(this));
  }

  private static ClientHttpConnector initConnector() {
    if (reactorNettyClientPresent) {
      return new ReactorClientHttpConnector();
    }
    else if (reactorNetty2ClientPresent) {
      return new ReactorNetty2ClientHttpConnector();
    }
    else if (jettyClientPresent) {
      return new JettyClientHttpConnector();
    }
    else if (httpComponentsClientPresent) {
      return new HttpComponentsClientHttpConnector();
    }
    else {
      return new JdkClientHttpConnector();
    }
  }

  private ExchangeStrategies initExchangeStrategies() {
    if (CollectionUtils.isEmpty(this.strategiesConfigurers)) {
      return (this.strategies != null ? this.strategies : ExchangeStrategies.withDefaults());
    }
    ExchangeStrategies.Builder builder =
            (this.strategies != null ? this.strategies.mutate() : ExchangeStrategies.builder());
    this.strategiesConfigurers.forEach(configurer -> configurer.accept(builder));
    return builder.build();
  }

  private UriBuilderFactory initUriBuilderFactory() {
    if (this.uriBuilderFactory != null) {
      return this.uriBuilderFactory;
    }
    return (this.baseUrl != null ?
            new DefaultUriBuilderFactory(this.baseUrl) : new DefaultUriBuilderFactory());
  }
}
