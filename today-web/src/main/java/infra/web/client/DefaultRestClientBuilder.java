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

package infra.web.client;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

import infra.http.HttpHeaders;
import infra.http.HttpStatusCode;
import infra.http.client.ClientHttpRequestFactory;
import infra.http.client.ClientHttpRequestInitializer;
import infra.http.client.ClientHttpRequestInterceptor;
import infra.http.client.HttpComponentsClientHttpRequestFactory;
import infra.http.client.InterceptingClientHttpRequestFactory;
import infra.http.client.JdkClientHttpRequestFactory;
import infra.http.client.ReactorClientHttpRequestFactory;
import infra.http.converter.AllEncompassingFormHttpMessageConverter;
import infra.http.converter.ByteArrayHttpMessageConverter;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.ResourceHttpMessageConverter;
import infra.http.converter.StringHttpMessageConverter;
import infra.http.converter.cbor.MappingJackson2CborHttpMessageConverter;
import infra.http.converter.json.GsonHttpMessageConverter;
import infra.http.converter.json.JsonbHttpMessageConverter;
import infra.http.converter.json.MappingJackson2HttpMessageConverter;
import infra.http.converter.smile.MappingJackson2SmileHttpMessageConverter;
import infra.http.converter.yaml.MappingJackson2YamlHttpMessageConverter;
import infra.lang.Assert;
import infra.lang.Nullable;
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

  // message factories

  private static final boolean jackson2Present;

  private static final boolean gsonPresent;

  private static final boolean jsonbPresent;

  private static final boolean jackson2SmilePresent;

  private static final boolean jackson2CborPresent;

  private static final boolean jackson2YamlPresent;

  static {
    ClassLoader loader = DefaultRestClientBuilder.class.getClassLoader();

    reactorNettyClientPresent = ClassUtils.isPresent("reactor.netty.http.client.HttpClient", loader);
    httpComponentsClientPresent = ClassUtils.isPresent("org.apache.hc.client5.http.classic.HttpClient", loader);

    jackson2Present = ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", loader)
            && ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", loader);
    gsonPresent = ClassUtils.isPresent("com.google.gson.Gson", loader);
    jsonbPresent = ClassUtils.isPresent("jakarta.json.bind.Jsonb", loader);
    jackson2SmilePresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.smile.SmileFactory", loader);
    jackson2CborPresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.cbor.CBORFactory", loader);
    jackson2YamlPresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.yaml.YAMLFactory", loader);
  }

  @Nullable
  private URI baseURI;

  @Nullable
  private Map<String, ?> defaultUriVariables;

  @Nullable
  private UriBuilderFactory uriBuilderFactory;

  @Nullable
  private HttpHeaders defaultHeaders;

  /** @since 5.0 */
  @Nullable
  private MultiValueMap<String, String> defaultCookies;

  @Nullable
  private Consumer<RestClient.RequestHeadersSpec<?>> defaultRequest;

  @Nullable
  private List<ResponseErrorHandler> statusHandlers;

  @Nullable
  private ClientHttpRequestFactory requestFactory;

  @Nullable
  private List<HttpMessageConverter<?>> messageConverters;

  @Nullable
  private List<ClientHttpRequestInterceptor> interceptors;

  @Nullable
  private List<ClientHttpRequestInitializer> initializers;

  private boolean ignoreStatus = false;

  private boolean detectEmptyMessageBody = true;

  public DefaultRestClientBuilder() {

  }

  public DefaultRestClientBuilder(DefaultRestClientBuilder other) {
    Assert.notNull(other, "Other is required");
    this.baseURI = other.baseURI;
    this.ignoreStatus = other.ignoreStatus;
    this.defaultRequest = other.defaultRequest;
    this.requestFactory = other.requestFactory;
    this.uriBuilderFactory = other.uriBuilderFactory;

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

    if (CollectionUtils.isNotEmpty(restTemplate.getInterceptors())) {
      this.interceptors = new ArrayList<>(restTemplate.getInterceptors());
    }
    if (CollectionUtils.isNotEmpty(restTemplate.getHttpRequestInitializers())) {
      this.initializers = new ArrayList<>(restTemplate.getHttpRequestInitializers());
    }
  }

  @Nullable
  private static UriBuilderFactory getUriBuilderFactory(RestTemplate restTemplate) {
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
  private static boolean hasRestTemplateDefaults(DefaultUriBuilderFactory factory) {
    // see RestTemplate::initUriTemplateHandler
    return (!factory.hasBaseUri() &&
            factory.getEncodingMode() == DefaultUriBuilderFactory.EncodingMode.URI_COMPONENT &&
            CollectionUtils.isEmpty(factory.getDefaultUriVariables()) &&
            factory.shouldParsePath());
  }

  private static ClientHttpRequestFactory getRequestFactory(RestTemplate restTemplate) {
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

  private HttpHeaders initHeaders() {
    if (this.defaultHeaders == null) {
      this.defaultHeaders = HttpHeaders.forWritable();
    }
    return this.defaultHeaders;
  }

  @Override
  public RestClient.Builder defaultCookie(String cookie, @Nullable String... values) {
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

  private MultiValueMap<String, String> initCookies() {
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

  private List<ClientHttpRequestInterceptor> initInterceptors() {
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

  private List<ClientHttpRequestInitializer> initInitializers() {
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
  public RestClient.Builder messageConverters(Consumer<List<HttpMessageConverter<?>>> configurer) {
    configurer.accept(initMessageConverters());
    validateConverters(this.messageConverters);
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

  private List<HttpMessageConverter<?>> initMessageConverters() {
    if (this.messageConverters == null) {
      this.messageConverters = new ArrayList<>();
      this.messageConverters.add(new ByteArrayHttpMessageConverter());
      this.messageConverters.add(new StringHttpMessageConverter());
      this.messageConverters.add(new ResourceHttpMessageConverter(false));
      this.messageConverters.add(new AllEncompassingFormHttpMessageConverter());

      if (jackson2Present) {
        this.messageConverters.add(new MappingJackson2HttpMessageConverter());
      }
      else if (gsonPresent) {
        this.messageConverters.add(new GsonHttpMessageConverter());
      }
      else if (jsonbPresent) {
        this.messageConverters.add(new JsonbHttpMessageConverter());
      }
      if (jackson2SmilePresent) {
        this.messageConverters.add(new MappingJackson2SmileHttpMessageConverter());
      }
      if (jackson2CborPresent) {
        this.messageConverters.add(new MappingJackson2CborHttpMessageConverter());
      }
      if (jackson2YamlPresent) {
        this.messageConverters.add(new MappingJackson2YamlHttpMessageConverter());
      }
    }
    return this.messageConverters;
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

    List<HttpMessageConverter<?>> messageConverters =
            this.messageConverters != null ? this.messageConverters : initMessageConverters();

    return new DefaultRestClient(requestFactory,
            this.interceptors, this.initializers, uriBuilderFactory,
            defaultHeaders, defaultCookies, this.defaultRequest, this.statusHandlers,
            messageConverters, new DefaultRestClientBuilder(this), ignoreStatus, detectEmptyMessageBody);
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
            ? new DefaultUriBuilderFactory(UriComponentsBuilder.fromUri(baseURI))
            : new DefaultUriBuilderFactory();
    factory.setDefaultUriVariables(this.defaultUriVariables);
    return factory;
  }

}
