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

package infra.web.client;

import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

import infra.http.HttpHeaders;
import infra.http.HttpRequest;
import infra.http.HttpStatusCode;
import infra.http.client.ClientHttpRequestFactory;
import infra.http.client.ClientHttpRequestInitializer;
import infra.http.client.ClientHttpRequestInterceptor;
import infra.http.client.HttpComponentsClientHttpRequestFactory;
import infra.http.client.InterceptingClientHttpRequestFactory;
import infra.http.client.JdkClientHttpRequestFactory;
import infra.http.client.ReactorClientHttpRequestFactory;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.HttpMessageConverters;
import infra.lang.Assert;
import infra.util.ClassUtils;
import infra.util.CollectionUtils;
import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;
import infra.web.util.DefaultUriBuilderFactory;
import infra.web.util.UriBuilderFactory;
import infra.web.util.UriComponentsBuilder;
import infra.web.util.UriTemplateHandler;

/**
 * Default implementation of {@link RestClient.Builder}.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class DefaultRestClientBuilder implements RestClient.Builder {

  // request factories

  private static final boolean httpComponentsClientPresent;

  private static final boolean reactorNettyClientPresent;

  static {
    ClassLoader loader = DefaultRestClientBuilder.class.getClassLoader();

    httpComponentsClientPresent = ClassUtils.isPresent("org.apache.hc.client5.http.classic.HttpClient", loader);
    reactorNettyClientPresent = ClassUtils.isPresent("reactor.netty.http.client.HttpClient", loader);
  }

  public @Nullable URI baseURI;

  public @Nullable Map<String, ?> defaultUriVariables;

  public @Nullable UriBuilderFactory uriBuilderFactory;

  public @Nullable HttpHeaders defaultHeaders;

  /** @since 5.0 */
  public @Nullable MultiValueMap<String, String> defaultCookies;

  public @Nullable Consumer<RestClient.RequestHeadersSpec<?>> defaultRequest;

  public @Nullable List<ResponseErrorHandler> statusHandlers;

  public @Nullable ClientHttpRequestFactory requestFactory;

  public @Nullable List<HttpMessageConverter<?>> messageConverters;

  public @Nullable List<ClientHttpRequestInterceptor> interceptors;

  public @Nullable List<ClientHttpRequestInitializer> initializers;

  public @Nullable Predicate<HttpRequest> bufferingPredicate;

  public boolean ignoreStatus = false;

  public boolean detectEmptyMessageBody = true;

  public @Nullable Object defaultApiVersion;

  public @Nullable ApiVersionInserter apiVersionInserter;

  private @Nullable Consumer<HttpMessageConverters.ClientBuilder> convertersConfigurer;

  public DefaultRestClientBuilder() {
  }

  public DefaultRestClientBuilder(DefaultRestClientBuilder other) {
    Assert.notNull(other, "Other is required");
    this.baseURI = other.baseURI;
    this.ignoreStatus = other.ignoreStatus;
    this.defaultRequest = other.defaultRequest;
    this.requestFactory = other.requestFactory;
    this.uriBuilderFactory = other.uriBuilderFactory;
    this.bufferingPredicate = other.bufferingPredicate;
    this.detectEmptyMessageBody = other.detectEmptyMessageBody;
    this.apiVersionInserter = other.apiVersionInserter;
    this.defaultApiVersion = other.defaultApiVersion;
    this.convertersConfigurer = other.convertersConfigurer;

    this.interceptors = other.interceptors != null ? new ArrayList<>(other.interceptors) : null;
    this.initializers = other.initializers != null ? new ArrayList<>(other.initializers) : null;
    this.defaultHeaders = other.defaultHeaders != null ? HttpHeaders.copyOf(other.defaultHeaders) : null;
    this.defaultCookies = other.defaultCookies != null ? MultiValueMap.copyOf(other.defaultCookies) : null;
    this.statusHandlers = other.statusHandlers != null ? new ArrayList<>(other.statusHandlers) : null;
    this.messageConverters = other.messageConverters != null ? new ArrayList<>(other.messageConverters) : null;
    this.defaultUriVariables = other.defaultUriVariables != null ? new LinkedHashMap<>(other.defaultUriVariables) : null;
  }

  public DefaultRestClientBuilder(RestTemplate restTemplate) {
    Assert.notNull(restTemplate, "RestTemplate is required");

    this.uriBuilderFactory = getUriBuilderFactory(restTemplate);
    this.statusHandlers = new ArrayList<>();
    this.statusHandlers.add(restTemplate.getErrorHandler());

    this.requestFactory = getRequestFactory(restTemplate);
    this.messageConverters = new ArrayList<>(restTemplate.getMessageConverters());
    this.bufferingPredicate = restTemplate.getBufferingPredicate();

    if (CollectionUtils.isNotEmpty(restTemplate.getInterceptors())) {
      this.interceptors = new ArrayList<>(restTemplate.getInterceptors());
    }
    if (CollectionUtils.isNotEmpty(restTemplate.getHttpRequestInitializers())) {
      this.initializers = new ArrayList<>(restTemplate.getHttpRequestInitializers());
    }
  }

  @Nullable
  static UriBuilderFactory getUriBuilderFactory(RestTemplate restTemplate) {
    UriTemplateHandler uriTemplateHandler = restTemplate.getUriTemplateHandler();
    if (uriTemplateHandler instanceof DefaultUriBuilderFactory builderFactory) {
      // only reuse the DefaultUriBuilderFactory if it has been customized
      if (hasRestTemplateDefaults(builderFactory)) {
        return null;
      }
      else {
        return builderFactory;
      }
    }
    else if (uriTemplateHandler instanceof UriBuilderFactory builderFactory) {
      return builderFactory;
    }
    else {
      return null;
    }
  }

  /**
   * Indicate whether this {@code DefaultUriBuilderFactory} uses the default
   * {@link infra.web.client.RestTemplate RestTemplate} settings.
   */
  static boolean hasRestTemplateDefaults(DefaultUriBuilderFactory factory) {
    // see RestTemplate::initUriTemplateHandler
    return (!factory.hasBaseUri() &&
            factory.getEncodingMode() == DefaultUriBuilderFactory.EncodingMode.URI_COMPONENT &&
            CollectionUtils.isEmpty(factory.getDefaultUriVariables()) &&
            factory.shouldParsePath());
  }

  static ClientHttpRequestFactory getRequestFactory(RestTemplate restTemplate) {
    ClientHttpRequestFactory requestFactory = restTemplate.getRequestFactory();
    if (requestFactory instanceof InterceptingClientHttpRequestFactory factory) {
      return factory.getRequestFactory();
    }
    else {
      return requestFactory;
    }
  }

  @Override
  public RestClient.Builder baseURI(@Nullable String baseURI) {
    if (baseURI != null) {
      this.baseURI = URI.create(baseURI);
    }
    return this;
  }

  @Override
  public RestClient.Builder baseURI(@Nullable URI baseURI) {
    this.baseURI = baseURI;
    return this;
  }

  @Override
  public RestClient.Builder defaultUriVariables(@Nullable Map<String, ?> defaultUriVariables) {
    this.defaultUriVariables = defaultUriVariables;
    return this;
  }

  @Override
  public RestClient.Builder uriBuilderFactory(@Nullable UriBuilderFactory uriBuilderFactory) {
    this.uriBuilderFactory = uriBuilderFactory;
    return this;
  }

  @Override
  public RestClient.Builder defaultHeader(String header, String... values) {
    initHeaders().setOrRemove(header, values);
    return this;
  }

  @Override
  public RestClient.Builder defaultHeaders(Consumer<HttpHeaders> headersConsumer) {
    headersConsumer.accept(initHeaders());
    return this;
  }

  @Override
  public RestClient.Builder defaultHeaders(HttpHeaders headers) {
    initHeaders().setAll(headers);
    return this;
  }

  HttpHeaders initHeaders() {
    if (this.defaultHeaders == null) {
      this.defaultHeaders = HttpHeaders.forWritable();
    }
    return this.defaultHeaders;
  }

  @Override
  public RestClient.Builder defaultCookie(String cookie, String @Nullable ... values) {
    initCookies().setOrRemove(cookie, values);
    return this;
  }

  @Override
  public RestClient.Builder defaultCookies(Consumer<MultiValueMap<String, String>> cookiesConsumer) {
    cookiesConsumer.accept(initCookies());
    return this;
  }

  @Override
  public RestClient.Builder defaultCookies(MultiValueMap<String, String> cookies) {
    initCookies().setAll(cookies);
    return this;
  }

  @Override
  public RestClient.Builder defaultApiVersion(@Nullable Object version) {
    this.defaultApiVersion = version;
    return this;
  }

  @Override
  public RestClient.Builder apiVersionInserter(@Nullable ApiVersionInserter apiVersionInserter) {
    this.apiVersionInserter = apiVersionInserter;
    return this;
  }

  MultiValueMap<String, String> initCookies() {
    if (this.defaultCookies == null) {
      this.defaultCookies = new LinkedMultiValueMap<>(3);
    }
    return this.defaultCookies;
  }

  @Override
  public RestClient.Builder defaultRequest(Consumer<RestClient.RequestHeadersSpec<?>> defaultRequest) {
    this.defaultRequest = this.defaultRequest != null ?
            this.defaultRequest.andThen(defaultRequest) : defaultRequest;
    return this;
  }

  @Override
  public RestClient.Builder defaultStatusHandler(Predicate<HttpStatusCode> statusPredicate, RestClient.ErrorHandler errorHandler) {
    return defaultStatusHandlerInternal(StatusHandler.of(statusPredicate, errorHandler));
  }

  @Override
  public RestClient.Builder defaultStatusHandler(ResponseErrorHandler errorHandler) {
    return defaultStatusHandlerInternal(errorHandler);
  }

  @Override
  public RestClient.Builder ignoreStatus(boolean ignoreStatus) {
    this.ignoreStatus = ignoreStatus;
    return this;
  }

  @Override
  public RestClient.Builder detectEmptyMessageBody(boolean detectEmptyBody) {
    this.detectEmptyMessageBody = detectEmptyBody;
    return this;
  }

  private RestClient.Builder defaultStatusHandlerInternal(ResponseErrorHandler statusHandler) {
    if (this.statusHandlers == null) {
      this.statusHandlers = new ArrayList<>();
    }
    this.statusHandlers.add(statusHandler);
    return this;
  }

  @Override
  public RestClient.Builder requestInterceptor(ClientHttpRequestInterceptor interceptor) {
    Assert.notNull(interceptor, "Interceptor is required");
    initInterceptors().add(interceptor);
    return this;
  }

  @Override
  public RestClient.Builder requestInterceptors(Consumer<List<ClientHttpRequestInterceptor>> interceptorsConsumer) {
    interceptorsConsumer.accept(initInterceptors());
    return this;
  }

  @Override
  public RestClient.Builder bufferContent(Predicate<HttpRequest> predicate) {
    bufferingPredicate = predicate;
    return this;
  }

  List<ClientHttpRequestInterceptor> initInterceptors() {
    if (this.interceptors == null) {
      this.interceptors = new ArrayList<>();
    }
    return this.interceptors;
  }

  @Override
  public RestClient.Builder requestInitializer(ClientHttpRequestInitializer initializer) {
    Assert.notNull(initializer, "Initializer is required");
    initInitializers().add(initializer);
    return this;
  }

  @Override
  public RestClient.Builder requestInitializers(Consumer<List<ClientHttpRequestInitializer>> initializersConsumer) {
    initializersConsumer.accept(initInitializers());
    return this;
  }

  List<ClientHttpRequestInitializer> initInitializers() {
    if (this.initializers == null) {
      this.initializers = new ArrayList<>();
    }
    return this.initializers;
  }

  @Override
  public RestClient.Builder requestFactory(ClientHttpRequestFactory requestFactory) {
    this.requestFactory = requestFactory;
    return this;
  }

  @Override
  public RestClient.Builder configureMessageConverters(Consumer<HttpMessageConverters.ClientBuilder> configurer) {
    this.convertersConfigurer = (this.convertersConfigurer != null ?
            this.convertersConfigurer.andThen(configurer) : configurer);
    return this;
  }

  @Override
  public RestClient.Builder messageConverters(Consumer<List<HttpMessageConverter<?>>> configurer) {
    if (this.messageConverters == null) {
      this.messageConverters = new ArrayList<>();
      this.messageConverters.addAll(HttpMessageConverters.forClient().registerDefaults().asList());
    }
    configurer.accept(messageConverters);
    validateConverters(messageConverters);
    return this;
  }

  @Override
  public RestClient.Builder messageConverters(List<HttpMessageConverter<?>> messageConverters) {
    validateConverters(messageConverters);
    this.messageConverters = Collections.unmodifiableList(messageConverters);
    return this;
  }

  @Override
  public RestClient.Builder apply(Consumer<RestClient.Builder> builderConsumer) {
    builderConsumer.accept(this);
    return this;
  }

  List<HttpMessageConverter<?>> initMessageConverters() {
    HttpMessageConverters.ClientBuilder builder = HttpMessageConverters.forClient();
    if (this.messageConverters == null && this.convertersConfigurer == null) {
      builder.registerDefaults();
    }
    else {
      builder.addCustomConverters(messageConverters);

      if (this.convertersConfigurer != null) {
        this.convertersConfigurer.accept(builder);
      }
    }
    List<HttpMessageConverter<?>> result = new ArrayList<>();
    builder.build().forEach(result::add);
    return result;
  }

  @Override
  public RestClient.Builder clone() {
    return new DefaultRestClientBuilder(this);
  }

  private void validateConverters(@Nullable List<HttpMessageConverter<?>> messageConverters) {
    Assert.notEmpty(messageConverters, "At least one HttpMessageConverter is required");
    Assert.noNullElements(messageConverters, "The HttpMessageConverter list must not contain null elements");
  }

  @Override
  public RestClient build() {
    ClientHttpRequestFactory requestFactory = initRequestFactory();
    UriBuilderFactory uriBuilderFactory = initUriBuilderFactory();

    var defaultHeaders = this.defaultHeaders != null ? HttpHeaders.copyOf(this.defaultHeaders).asReadOnly() : null;
    var defaultCookies = this.defaultCookies != null ? MultiValueMap.copyOf(this.defaultCookies).asReadOnly() : null;

    List<HttpMessageConverter<?>> messageConverters = initMessageConverters();

    return new DefaultRestClient(requestFactory, this.interceptors, this.initializers, uriBuilderFactory,
            defaultHeaders, defaultCookies, this.defaultRequest, this.statusHandlers, bufferingPredicate,
            messageConverters, new DefaultRestClientBuilder(this), ignoreStatus, detectEmptyMessageBody,
            apiVersionInserter, defaultApiVersion);
  }

  private ClientHttpRequestFactory initRequestFactory() {
    if (this.requestFactory != null) {
      return this.requestFactory;
    }
    else if (httpComponentsClientPresent) {
      return new HttpComponentsClientHttpRequestFactory();
    }
    else if (reactorNettyClientPresent) {
      return new ReactorClientHttpRequestFactory();
    }
    return new JdkClientHttpRequestFactory();
  }

  private UriBuilderFactory initUriBuilderFactory() {
    if (this.uriBuilderFactory != null) {
      return this.uriBuilderFactory;
    }

    DefaultUriBuilderFactory factory = this.baseURI != null
            ? new DefaultUriBuilderFactory(UriComponentsBuilder.forURI(baseURI))
            : new DefaultUriBuilderFactory();
    factory.setDefaultUriVariables(this.defaultUriVariables);
    return factory;
  }

}
