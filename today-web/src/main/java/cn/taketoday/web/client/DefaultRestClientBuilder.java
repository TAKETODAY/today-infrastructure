/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.web.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.client.ClientHttpRequestFactory;
import cn.taketoday.http.client.ClientHttpRequestInitializer;
import cn.taketoday.http.client.ClientHttpRequestInterceptor;
import cn.taketoday.http.client.HttpComponentsClientHttpRequestFactory;
import cn.taketoday.http.client.JdkClientHttpRequestFactory;
import cn.taketoday.http.client.JettyClientHttpRequestFactory;
import cn.taketoday.http.client.OkHttp3ClientHttpRequestFactory;
import cn.taketoday.http.client.SimpleClientHttpRequestFactory;
import cn.taketoday.http.converter.AllEncompassingFormHttpMessageConverter;
import cn.taketoday.http.converter.ByteArrayHttpMessageConverter;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.converter.ResourceHttpMessageConverter;
import cn.taketoday.http.converter.StringHttpMessageConverter;
import cn.taketoday.http.converter.cbor.MappingJackson2CborHttpMessageConverter;
import cn.taketoday.http.converter.json.GsonHttpMessageConverter;
import cn.taketoday.http.converter.json.JsonbHttpMessageConverter;
import cn.taketoday.http.converter.json.MappingJackson2HttpMessageConverter;
import cn.taketoday.http.converter.smile.MappingJackson2SmileHttpMessageConverter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.util.DefaultUriBuilderFactory;
import cn.taketoday.web.util.UriBuilderFactory;

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

  private static final boolean okHttpClientPresent;

  private static final boolean jettyClientPresent;

  private static final boolean jdkClientPresent;

  // message factories

  private static final boolean jackson2Present;

  private static final boolean gsonPresent;

  private static final boolean jsonbPresent;

  private static final boolean jackson2SmilePresent;

  private static final boolean jackson2CborPresent;

  static {
    ClassLoader loader = DefaultRestClientBuilder.class.getClassLoader();

    httpComponentsClientPresent = ClassUtils.isPresent("org.apache.hc.client5.http.classic.HttpClient", loader);
    okHttpClientPresent = ClassUtils.isPresent("okhttp3.OkHttpClient", loader);
    jettyClientPresent = ClassUtils.isPresent("org.eclipse.jetty.client.HttpClient", loader);
    jdkClientPresent = ClassUtils.isPresent("java.net.http.HttpClient", loader);

    jackson2Present = ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", loader) &&
            ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", loader);
    gsonPresent = ClassUtils.isPresent("com.google.gson.Gson", loader);
    jsonbPresent = ClassUtils.isPresent("jakarta.json.bind.Jsonb", loader);
    jackson2SmilePresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.smile.SmileFactory", loader);
    jackson2CborPresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.cbor.CBORFactory", loader);
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
  private Consumer<RestClient.RequestHeadersSpec<?>> defaultRequest;

  @Nullable
  private List<StatusHandler> statusHandlers;

  @Nullable
  private ClientHttpRequestFactory requestFactory;

  @Nullable
  private List<HttpMessageConverter<?>> messageConverters;

  @Nullable
  private List<ClientHttpRequestInterceptor> interceptors;

  @Nullable
  private List<ClientHttpRequestInitializer> initializers;

  public DefaultRestClientBuilder() {
  }

  public DefaultRestClientBuilder(DefaultRestClientBuilder other) {
    Assert.notNull(other, "Other must not be null");

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
    this.statusHandlers = (other.statusHandlers != null ? new ArrayList<>(other.statusHandlers) : null);

    this.requestFactory = other.requestFactory;
    this.messageConverters = (other.messageConverters != null ?
                              new ArrayList<>(other.messageConverters) : null);

    this.interceptors = (other.interceptors != null) ? new ArrayList<>(other.interceptors) : null;
    this.initializers = (other.initializers != null) ? new ArrayList<>(other.initializers) : null;
  }

  public DefaultRestClientBuilder(RestTemplate restTemplate) {
    Assert.notNull(restTemplate, "RestTemplate must not be null");

    if (restTemplate.getUriTemplateHandler() instanceof UriBuilderFactory builderFactory) {
      this.uriBuilderFactory = builderFactory;
    }
    this.statusHandlers = new ArrayList<>();
    this.statusHandlers.add(StatusHandler.fromErrorHandler(restTemplate.getErrorHandler()));

    this.requestFactory = restTemplate.getRequestFactory();
    this.messageConverters = new ArrayList<>(restTemplate.getMessageConverters());

    if (CollectionUtils.isNotEmpty(restTemplate.getInterceptors())) {
      this.interceptors = new ArrayList<>(restTemplate.getInterceptors());
    }
    if (CollectionUtils.isNotEmpty(restTemplate.getHttpRequestInitializers())) {
      this.initializers = new ArrayList<>(restTemplate.getHttpRequestInitializers());
    }
  }

  @Override
  public RestClient.Builder baseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
    return this;
  }

  @Override
  public RestClient.Builder defaultUriVariables(Map<String, ?> defaultUriVariables) {
    this.defaultUriVariables = defaultUriVariables;
    return this;
  }

  @Override
  public RestClient.Builder uriBuilderFactory(UriBuilderFactory uriBuilderFactory) {
    this.uriBuilderFactory = uriBuilderFactory;
    return this;
  }

  @Override
  public RestClient.Builder defaultHeader(String header, String... values) {
    initHeaders().put(header, Arrays.asList(values));
    return this;
  }

  @Override
  public RestClient.Builder defaultHeaders(Consumer<HttpHeaders> headersConsumer) {
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
  public RestClient.Builder defaultRequest(Consumer<RestClient.RequestHeadersSpec<?>> defaultRequest) {
    this.defaultRequest = this.defaultRequest != null ?
                          this.defaultRequest.andThen(defaultRequest) : defaultRequest;
    return this;
  }

  @Override
  public RestClient.Builder defaultStatusHandler(Predicate<HttpStatusCode> statusPredicate, RestClient.ResponseSpec.ErrorHandler errorHandler) {
    return defaultStatusHandlerInternal(StatusHandler.of(statusPredicate, errorHandler));
  }

  @Override
  public RestClient.Builder defaultStatusHandler(ResponseErrorHandler errorHandler) {
    return defaultStatusHandlerInternal(StatusHandler.fromErrorHandler(errorHandler));
  }

  private RestClient.Builder defaultStatusHandlerInternal(StatusHandler statusHandler) {
    if (this.statusHandlers == null) {
      this.statusHandlers = new ArrayList<>();
    }
    this.statusHandlers.add(statusHandler);
    return this;
  }

  @Override
  public RestClient.Builder requestInterceptor(ClientHttpRequestInterceptor interceptor) {
    Assert.notNull(interceptor, "Interceptor must not be null");
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
    Assert.notNull(initializer, "Initializer must not be null");
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
    }
    return this.messageConverters;
  }

  @Override
  public RestClient.Builder clone() {
    return new DefaultRestClientBuilder(this);
  }

  @Override
  public RestClient build() {
    ClientHttpRequestFactory requestFactory = initRequestFactory();
    UriBuilderFactory uriBuilderFactory = initUriBuilderFactory();
    HttpHeaders defaultHeaders = copyDefaultHeaders();
    List<HttpMessageConverter<?>> messageConverters = (this.messageConverters != null ?
                                                       this.messageConverters : initMessageConverters());
    return new DefaultRestClient(requestFactory,
            this.interceptors, this.initializers, uriBuilderFactory,
            defaultHeaders,
            this.statusHandlers,
            messageConverters,
            new DefaultRestClientBuilder(this)
    );
  }

  private ClientHttpRequestFactory initRequestFactory() {
    if (this.requestFactory != null) {
      return this.requestFactory;
    }
    else if (httpComponentsClientPresent) {
      return new HttpComponentsClientHttpRequestFactory();
    }
    else if (okHttpClientPresent) {
      return new OkHttp3ClientHttpRequestFactory();
    }
    else if (jettyClientPresent) {
      return new JettyClientHttpRequestFactory();
    }
    else if (jdkClientPresent) {
      // java.net.http module might not be loaded, so we can't default to the JDK HttpClient
      return new JdkClientHttpRequestFactory();
    }
    else {
      return new SimpleClientHttpRequestFactory();
    }
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
    if (this.defaultHeaders != null) {
      HttpHeaders copy = HttpHeaders.create();
      this.defaultHeaders.forEach((key, values) -> copy.put(key, new ArrayList<>(values)));
      return HttpHeaders.readOnlyHttpHeaders(copy);
    }
    else {
      return null;
    }
  }

}
