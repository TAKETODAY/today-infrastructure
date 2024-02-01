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

package cn.taketoday.web.reactive.function.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.client.reactive.ClientHttpConnector;
import cn.taketoday.http.client.reactive.HttpComponentsClientHttpConnector;
import cn.taketoday.http.client.reactive.JdkClientHttpConnector;
import cn.taketoday.http.client.reactive.JettyClientHttpConnector;
import cn.taketoday.http.client.reactive.ReactorClientHttpConnector;
import cn.taketoday.http.codec.ClientCodecConfigurer;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.LinkedMultiValueMap;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.web.util.DefaultUriBuilderFactory;
import cn.taketoday.web.util.UriBuilderFactory;
import reactor.core.publisher.Mono;

/**
 * Default implementation of {@link WebClient.Builder}.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 4.0
 */
final class DefaultWebClientBuilder implements WebClient.Builder {

  private static final boolean reactorClientPresent;

  private static final boolean jettyClientPresent;

  private static final boolean httpComponentsClientPresent;

  static {
    ClassLoader loader = DefaultWebClientBuilder.class.getClassLoader();
    reactorClientPresent = ClassUtils.isPresent("reactor.netty.http.client.HttpClient", loader);
    jettyClientPresent = ClassUtils.isPresent("org.eclipse.jetty.client.HttpClient", loader);
    httpComponentsClientPresent =
            ClassUtils.isPresent("org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient", loader) &&
                    ClassUtils.isPresent("org.apache.hc.core5.reactive.ReactiveDataConsumer", loader);
  }

  @Nullable
  private String baseUrl;

  @Nullable
  private Map<String, ?> defaultUriVariables;

  @Nullable
  private UriBuilderFactory uriBuilderFactory;

  @Nullable
  private HttpHeaders defaultHeaders;

  @Nullable
  private MultiValueMap<String, String> defaultCookies;

  @Nullable
  private Consumer<WebClient.RequestHeadersSpec<?>> defaultRequest;

  @Nullable
  private Map<Predicate<HttpStatusCode>, Function<ClientResponse, Mono<? extends Throwable>>> statusHandlers;

  @Nullable
  private List<ExchangeFilterFunction> filters;

  @Nullable
  private ClientHttpConnector connector;

  @Nullable
  private ExchangeStrategies strategies;

  @Nullable
  private List<Consumer<ExchangeStrategies.Builder>> strategiesConfigurers;

  @Nullable
  private ExchangeFunction exchangeFunction;

  public DefaultWebClientBuilder() {
  }

  public DefaultWebClientBuilder(DefaultWebClientBuilder other) {
    Assert.notNull(other, "DefaultWebClientBuilder is required");

    this.baseUrl = other.baseUrl;
    this.defaultUriVariables = (other.defaultUriVariables != null ?
                                new LinkedHashMap<>(other.defaultUriVariables) : null);
    this.uriBuilderFactory = other.uriBuilderFactory;

    if (other.defaultHeaders != null) {
      this.defaultHeaders = HttpHeaders.create();
      this.defaultHeaders.putAll(other.defaultHeaders);
    }
    else {
      this.defaultHeaders = null;
    }

    this.defaultRequest = other.defaultRequest;
    this.defaultCookies = (other.defaultCookies != null ? new LinkedMultiValueMap<>(other.defaultCookies) : null);
    this.statusHandlers = (other.statusHandlers != null ? new LinkedHashMap<>(other.statusHandlers) : null);
    this.filters = (other.filters != null ? new ArrayList<>(other.filters) : null);

    this.connector = other.connector;
    this.strategies = other.strategies;
    this.strategiesConfigurers = (other.strategiesConfigurers != null ?
                                  new ArrayList<>(other.strategiesConfigurers) : null);
    this.exchangeFunction = other.exchangeFunction;
  }

  @Override
  public WebClient.Builder baseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
    return this;
  }

  @Override
  public WebClient.Builder defaultUriVariables(Map<String, ?> defaultUriVariables) {
    this.defaultUriVariables = defaultUriVariables;
    return this;
  }

  @Override
  public WebClient.Builder uriBuilderFactory(UriBuilderFactory uriBuilderFactory) {
    this.uriBuilderFactory = uriBuilderFactory;
    return this;
  }

  @Override
  public WebClient.Builder defaultHeader(String header, String... values) {
    initHeaders().put(header, Arrays.asList(values));
    return this;
  }

  @Override
  public WebClient.Builder defaultHeaders(Consumer<HttpHeaders> headersConsumer) {
    headersConsumer.accept(initHeaders());
    return this;
  }

  private HttpHeaders initHeaders() {
    if (this.defaultHeaders == null) {
      this.defaultHeaders = HttpHeaders.create();
    }
    return this.defaultHeaders;
  }

  @Override
  public WebClient.Builder defaultCookie(String cookie, String... values) {
    initCookies().addAll(cookie, Arrays.asList(values));
    return this;
  }

  @Override
  public WebClient.Builder defaultCookies(Consumer<MultiValueMap<String, String>> cookiesConsumer) {
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
  public WebClient.Builder defaultRequest(Consumer<WebClient.RequestHeadersSpec<?>> defaultRequest) {
    if (this.defaultRequest != null) {
      this.defaultRequest = this.defaultRequest.andThen(defaultRequest);
    }
    else {
      this.defaultRequest = defaultRequest;
    }
    return this;
  }

  @Override
  public WebClient.Builder defaultStatusHandler(Predicate<HttpStatusCode> statusPredicate,
          Function<ClientResponse, Mono<? extends Throwable>> exceptionFunction) {
    if (statusHandlers == null) {
      this.statusHandlers = new LinkedHashMap<>();
    }
    statusHandlers.put(statusPredicate, exceptionFunction);
    return this;
  }

  @Override
  public WebClient.Builder filter(ExchangeFilterFunction filter) {
    Assert.notNull(filter, "ExchangeFilterFunction is required");
    initFilters().add(filter);
    return this;
  }

  @Override
  public WebClient.Builder filters(Consumer<List<ExchangeFilterFunction>> filtersConsumer) {
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
  public WebClient.Builder clientConnector(ClientHttpConnector connector) {
    this.connector = connector;
    return this;
  }

  @Override
  public WebClient.Builder codecs(Consumer<ClientCodecConfigurer> configurer) {
    if (this.strategiesConfigurers == null) {
      this.strategiesConfigurers = new ArrayList<>(4);
    }
    this.strategiesConfigurers.add(builder -> builder.codecs(configurer));
    return this;
  }

  @Override
  public WebClient.Builder exchangeStrategies(ExchangeStrategies strategies) {
    this.strategies = strategies;
    return this;
  }

  @Override
  public WebClient.Builder exchangeStrategies(Consumer<ExchangeStrategies.Builder> configurer) {
    if (this.strategiesConfigurers == null) {
      this.strategiesConfigurers = new ArrayList<>(4);
    }
    this.strategiesConfigurers.add(configurer);
    return this;
  }

  @Override
  public WebClient.Builder exchangeFunction(ExchangeFunction exchangeFunction) {
    this.exchangeFunction = exchangeFunction;
    return this;
  }

  @Override
  public WebClient.Builder apply(Consumer<WebClient.Builder> builderConsumer) {
    builderConsumer.accept(this);
    return this;
  }

  @Override
  public WebClient.Builder clone() {
    return new DefaultWebClientBuilder(this);
  }

  @Override
  public WebClient build() {
    ExchangeFunction exchange = exchangeFunction;
    if (exchange == null) {
      ClientHttpConnector connectorToUse = connector;
      if (connectorToUse == null) {
        connectorToUse = initConnector();
      }

      exchange = ExchangeFunctions.create(connectorToUse, initExchangeStrategies());
    }

    exchange = filterExchangeFunction(exchange);

    HttpHeaders defaultHeaders = copyDefaultHeaders();
    MultiValueMap<String, String> defaultCookies = copyDefaultCookies();
    return new DefaultWebClient(exchange, initUriBuilderFactory(),
            defaultHeaders,
            defaultCookies,
            this.defaultRequest,
            this.statusHandlers,
            new DefaultWebClientBuilder(this)
    );
  }

  private ExchangeFunction filterExchangeFunction(ExchangeFunction exchange) {
    if (filters != null) {
      return filters.stream()
              .reduce(ExchangeFilterFunction::andThen)
              .map(filter -> filter.apply(exchange))
              .orElse(exchange);
    }
    return exchange;
  }

  private ClientHttpConnector initConnector() {
    if (reactorClientPresent) {
      return new ReactorClientHttpConnector();
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
      return strategies != null ? strategies : ExchangeStrategies.withDefaults();
    }

    ExchangeStrategies.Builder builder = getOrCreateBuilder();
    for (Consumer<ExchangeStrategies.Builder> configurer : strategiesConfigurers) {
      configurer.accept(builder);
    }
    return builder.build();
  }

  private ExchangeStrategies.Builder getOrCreateBuilder() {
    if (strategies != null) {
      return strategies.mutate();
    }
    return ExchangeStrategies.builder();
  }

  private UriBuilderFactory initUriBuilderFactory() {
    if (this.uriBuilderFactory != null) {
      return this.uriBuilderFactory;
    }

    DefaultUriBuilderFactory factory = (this.baseUrl != null ?
                                        new DefaultUriBuilderFactory(this.baseUrl) : new DefaultUriBuilderFactory());
    factory.setDefaultUriVariables(this.defaultUriVariables);
    return factory;
  }

  @Nullable
  private HttpHeaders copyDefaultHeaders() {
    if (defaultHeaders != null) {
      HttpHeaders copy = HttpHeaders.create();
      doCopyMultiValueMap(defaultHeaders, copy);
      return HttpHeaders.readOnlyHttpHeaders(copy);
    }
    else {
      return null;
    }
  }

  @Nullable
  private MultiValueMap<String, String> copyDefaultCookies() {
    if (this.defaultCookies != null) {
      MultiValueMap<String, String> copy = new LinkedMultiValueMap<>(this.defaultCookies.size());
      doCopyMultiValueMap(defaultCookies, copy);
      return MultiValueMap.forUnmodifiable(copy);
    }
    else {
      return null;
    }
  }

  private void doCopyMultiValueMap(MultiValueMap<String, String> source, MultiValueMap<String, String> copy) {
    for (var entry : source.entrySet()) {
      copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
    }
  }

}
