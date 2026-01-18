/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.client;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.ArrayList;
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
import infra.http.client.InterceptingClientHttpRequestFactory;
import infra.http.converter.HttpMessageConverter;
import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;
import infra.web.util.UriBuilderFactory;
import infra.web.util.UriTemplateHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/10 16:03
 */
class DefaultRestClientBuilderTests {

  @Test
  void copyConstructorCopiesAllProperties() {
    DefaultRestClientBuilder original = new DefaultRestClientBuilder();
    original.baseURI(URI.create("http://example.com"))
            .defaultHeader("X-Test", "value")
            .defaultCookie("testCookie", "cookieValue")
            .defaultApiVersion("1.0")
            .apiVersionInserter(ApiVersionInserter.forHeader("API-Version"))
            .ignoreStatus(true)
            .detectEmptyMessageBody(false);

    DefaultRestClientBuilder copy = new DefaultRestClientBuilder(original);

    // Build clients to compare configurations
    RestClient originalClient = original.build();
    RestClient copyClient = copy.build();

    assertThat(copyClient).isNotNull();
    assertThat(copy).isNotSameAs(original);
  }

  @Test
  void baseURISetterWithString() {
    DefaultRestClientBuilder builder = new DefaultRestClientBuilder();
    String baseURI = "http://example.com/api";

    builder.baseURI(baseURI);

    assertThat(builder).extracting("baseURI").isEqualTo(URI.create(baseURI));
  }

  @Test
  void baseURISetterWithURI() {
    DefaultRestClientBuilder builder = new DefaultRestClientBuilder();
    URI baseURI = URI.create("http://example.com/api");

    builder.baseURI(baseURI);

    assertThat(builder).extracting("baseURI").isSameAs(baseURI);
  }

  @Test
  void defaultUriVariablesSetter() {
    DefaultRestClientBuilder builder = new DefaultRestClientBuilder();
    Map<String, String> uriVariables = Map.of("id", "123", "version", "v1");

    builder.defaultUriVariables(uriVariables);

    assertThat(builder).extracting("defaultUriVariables").isEqualTo(uriVariables);
  }

  @Test
  void uriBuilderFactorySetter() {
    DefaultRestClientBuilder builder = new DefaultRestClientBuilder();
    UriBuilderFactory uriBuilderFactory = mock(UriBuilderFactory.class);

    builder.uriBuilderFactory(uriBuilderFactory);

    assertThat(builder).extracting("uriBuilderFactory").isSameAs(uriBuilderFactory);
  }

  @Test
  void defaultHeaderAddsHeader() {
    DefaultRestClientBuilder builder = new DefaultRestClientBuilder();
    String headerName = "X-Test";
    String[] headerValues = { "value1", "value2" };

    builder.defaultHeader(headerName, headerValues);

    assertThat(builder).extracting("defaultHeaders").isNotNull();
    assertThat(builder.defaultHeaders.get(headerName)).containsExactly(headerValues);
  }

  @Test
  void defaultHeadersWithConsumer() {
    DefaultRestClientBuilder builder = new DefaultRestClientBuilder();
    Consumer<HttpHeaders> headersConsumer = headers -> headers.set("X-Test", "value");

    builder.defaultHeaders(headersConsumer);

    assertThat(builder.defaultHeaders).isNotNull();
    assertThat(builder.defaultHeaders.getFirst("X-Test")).isEqualTo("value");
  }

  @Test
  void defaultHeadersWithHeadersObject() {
    DefaultRestClientBuilder builder = new DefaultRestClientBuilder();
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.set("X-Test", "value");

    builder.defaultHeaders(headers);

    assertThat(builder.defaultHeaders).isNotNull();
    assertThat(builder.defaultHeaders.getFirst("X-Test")).isEqualTo("value");
  }

  @Test
  void defaultCookieAddsCookie() {
    DefaultRestClientBuilder builder = new DefaultRestClientBuilder();
    String cookieName = "testCookie";
    String[] cookieValues = { "value1", "value2" };

    builder.defaultCookie(cookieName, cookieValues);

    assertThat(builder.defaultCookies).isNotNull();
    assertThat(builder.defaultCookies.get(cookieName)).containsExactly(cookieValues);
  }

  @Test
  void defaultCookiesWithConsumer() {
    DefaultRestClientBuilder builder = new DefaultRestClientBuilder();
    Consumer<MultiValueMap<String, String>> cookiesConsumer = cookies -> cookies.add("testCookie", "value");

    builder.defaultCookies(cookiesConsumer);

    assertThat(builder.defaultCookies).isNotNull();
    assertThat(builder.defaultCookies.getFirst("testCookie")).isEqualTo("value");
  }

  @Test
  void defaultCookiesWithCookiesObject() {
    DefaultRestClientBuilder builder = new DefaultRestClientBuilder();
    MultiValueMap<String, String> cookies = new LinkedMultiValueMap<>();
    cookies.add("testCookie", "value");

    builder.defaultCookies(cookies);

    assertThat(builder.defaultCookies).isNotNull();
    assertThat(builder.defaultCookies.getFirst("testCookie")).isEqualTo("value");
  }

  @Test
  void defaultApiVersionSetter() {
    DefaultRestClientBuilder builder = new DefaultRestClientBuilder();
    String version = "1.0";

    builder.defaultApiVersion(version);

    assertThat(builder.defaultApiVersion).isEqualTo(version);
  }

  @Test
  void apiVersionInserterSetter() {
    DefaultRestClientBuilder builder = new DefaultRestClientBuilder();
    ApiVersionInserter inserter = ApiVersionInserter.forHeader("API-Version");

    builder.apiVersionInserter(inserter);

    assertThat(builder.apiVersionInserter).isSameAs(inserter);
  }

  @Test
  void defaultRequestSetter() {
    DefaultRestClientBuilder builder = new DefaultRestClientBuilder();
    Consumer<RestClient.RequestHeadersSpec<?>> defaultRequest = spec -> spec.header("X-Default", "value");

    builder.defaultRequest(defaultRequest);

    assertThat(builder.defaultRequest).isNotNull();
  }

  @Test
  void defaultStatusHandlerWithPredicateAndErrorHandler() {
    DefaultRestClientBuilder builder = new DefaultRestClientBuilder();
    Predicate<HttpStatusCode> statusPredicate = status -> status.is4xxClientError();
    RestClient.ErrorHandler errorHandler = (request, response) -> { };

    builder.defaultStatusHandler(statusPredicate, errorHandler);

    assertThat(builder.statusHandlers).isNotNull();
    assertThat(builder.statusHandlers).hasSize(1);
  }

  @Test
  void defaultStatusHandlerWithResponseErrorHandler() {
    DefaultRestClientBuilder builder = new DefaultRestClientBuilder();
    ResponseErrorHandler errorHandler = mock(ResponseErrorHandler.class);

    builder.defaultStatusHandler(errorHandler);

    assertThat(builder.statusHandlers).isNotNull();
    assertThat(builder.statusHandlers).hasSize(1);
  }

  @Test
  void ignoreStatusSetter() {
    DefaultRestClientBuilder builder = new DefaultRestClientBuilder();

    builder.ignoreStatus(true);

    assertThat(builder.ignoreStatus).isTrue();
  }

  @Test
  void detectEmptyMessageBodySetter() {
    DefaultRestClientBuilder builder = new DefaultRestClientBuilder();

    builder.detectEmptyMessageBody(false);

    assertThat(builder.detectEmptyMessageBody).isFalse();
  }

  @Test
  void requestInterceptorAddsInterceptor() {
    DefaultRestClientBuilder builder = new DefaultRestClientBuilder();
    ClientHttpRequestInterceptor interceptor = mock(ClientHttpRequestInterceptor.class);

    builder.requestInterceptor(interceptor);

    assertThat(builder.interceptors).isNotNull();
    assertThat(builder.interceptors).containsExactly(interceptor);
  }

  @Test
  void requestInterceptorsWithConsumer() {
    DefaultRestClientBuilder builder = new DefaultRestClientBuilder();
    ClientHttpRequestInterceptor interceptor1 = mock(ClientHttpRequestInterceptor.class);
    ClientHttpRequestInterceptor interceptor2 = mock(ClientHttpRequestInterceptor.class);
    Consumer<List<ClientHttpRequestInterceptor>> interceptorsConsumer = interceptors -> {
      interceptors.add(interceptor1);
      interceptors.add(interceptor2);
    };

    builder.requestInterceptors(interceptorsConsumer);

    assertThat(builder.interceptors).isNotNull();
    assertThat(builder.interceptors).containsExactly(interceptor1, interceptor2);
  }

  @Test
  void bufferContentSetter() {
    DefaultRestClientBuilder builder = new DefaultRestClientBuilder();
    Predicate<HttpRequest> predicate = request -> true;

    builder.bufferContent(predicate);

    assertThat(builder.bufferingPredicate).isSameAs(predicate);
  }

  @Test
  void requestInitializerAddsInitializer() {
    DefaultRestClientBuilder builder = new DefaultRestClientBuilder();
    ClientHttpRequestInitializer initializer = mock(ClientHttpRequestInitializer.class);

    builder.requestInitializer(initializer);

    assertThat(builder.initializers).isNotNull();
    assertThat(builder.initializers).containsExactly(initializer);
  }

  @Test
  void requestInitializersWithConsumer() {
    DefaultRestClientBuilder builder = new DefaultRestClientBuilder();
    ClientHttpRequestInitializer initializer1 = mock(ClientHttpRequestInitializer.class);
    ClientHttpRequestInitializer initializer2 = mock(ClientHttpRequestInitializer.class);
    Consumer<List<ClientHttpRequestInitializer>> initializersConsumer = initializers -> {
      initializers.add(initializer1);
      initializers.add(initializer2);
    };

    builder.requestInitializers(initializersConsumer);

    assertThat(builder.initializers).isNotNull();
    assertThat(builder.initializers).containsExactly(initializer1, initializer2);
  }

  @Test
  void requestFactorySetter() {
    DefaultRestClientBuilder builder = new DefaultRestClientBuilder();
    ClientHttpRequestFactory requestFactory = mock(ClientHttpRequestFactory.class);

    builder.requestFactory(requestFactory);

    assertThat(builder.requestFactory).isSameAs(requestFactory);
  }

  @Test
  void messageConvertersWithConsumer() {
    DefaultRestClientBuilder builder = new DefaultRestClientBuilder();
    HttpMessageConverter<?> converter = mock(HttpMessageConverter.class);
    Consumer<List<HttpMessageConverter<?>>> configurer = converters -> converters.add(converter);

    builder.messageConverters(configurer);

    assertThat(builder.messageConverters).isNotNull();
    assertThat(builder.messageConverters).contains(converter);
  }

  @Test
  void messageConvertersWithList() {
    DefaultRestClientBuilder builder = new DefaultRestClientBuilder();
    HttpMessageConverter<?> converter = mock(HttpMessageConverter.class);
    List<HttpMessageConverter<?>> converters = List.of(converter);

    builder.messageConverters(converters);

    assertThat(builder.messageConverters).isNotNull();
    assertThat(builder.messageConverters).containsExactly(converter);
  }

  @Test
  void applyMethodAppliesConsumer() {
    DefaultRestClientBuilder builder = new DefaultRestClientBuilder();
    Consumer<RestClient.Builder> builderConsumer = b -> b.defaultHeader("X-Applied", "value");

    builder.apply(builderConsumer);

    assertThat(builder.defaultHeaders).isNotNull();
    assertThat(builder.defaultHeaders.getFirst("X-Applied")).isEqualTo("value");
  }

  @Test
  void cloneMethodCreatesCopy() {
    DefaultRestClientBuilder original = new DefaultRestClientBuilder();
    original.defaultHeader("X-Test", "value");

    DefaultRestClientBuilder clone = (DefaultRestClientBuilder) original.clone();

    assertThat(clone).isNotNull();
    assertThat(clone).isNotSameAs(original);
    assertThat(clone.defaultHeaders).isNotNull();
    assertThat(clone.defaultHeaders.getFirst("X-Test")).isEqualTo("value");
  }

  @Test
  void buildMethodCreatesRestClient() {
    DefaultRestClientBuilder builder = new DefaultRestClientBuilder();

    RestClient client = builder.build();

    assertThat(client).isNotNull();
    assertThat(client).isInstanceOf(DefaultRestClient.class);
  }

  @Test
  void initMessageConvertersInitializesConverters() {
    DefaultRestClientBuilder builder = new DefaultRestClientBuilder();

    List<HttpMessageConverter<?>> converters = builder.initMessageConverters();

    assertThat(converters).isNotNull();
    assertThat(converters).isNotEmpty();
  }

  @Test
  void defaultConstructorCreatesBuilderWithDefaults() {
    DefaultRestClientBuilder builder = new DefaultRestClientBuilder();

    assertThat(builder.baseURI).isNull();
    assertThat(builder.defaultHeaders).isNull();
    assertThat(builder.defaultCookies).isNull();
    assertThat(builder.defaultRequest).isNull();
    assertThat(builder.statusHandlers).isNull();
    assertThat(builder.requestFactory).isNull();
    assertThat(builder.messageConverters).isNull();
    assertThat(builder.interceptors).isNull();
    assertThat(builder.initializers).isNull();
    assertThat(builder.bufferingPredicate).isNull();
    assertThat(builder.ignoreStatus).isFalse();
    assertThat(builder.detectEmptyMessageBody).isTrue();
    assertThat(builder.defaultApiVersion).isNull();
    assertThat(builder.apiVersionInserter).isNull();
  }

  @Test
  void restTemplateConstructorCopiesProperties() {
    RestTemplate restTemplate = new RestTemplate();
    ClientHttpRequestInterceptor interceptor = mock(ClientHttpRequestInterceptor.class);
    ClientHttpRequestInitializer initializer = mock(ClientHttpRequestInitializer.class);
    restTemplate.getInterceptors().add(interceptor);
    restTemplate.getHttpRequestInitializers().add(initializer);

    DefaultRestClientBuilder builder = new DefaultRestClientBuilder(restTemplate);

    assertThat(builder.statusHandlers).isNotNull();
    assertThat(builder.messageConverters).isNotNull();
    assertThat(builder.interceptors).containsExactly(interceptor);
    assertThat(builder.initializers).containsExactly(initializer);
  }

  @Test
  void baseURISetterWithNullString() {
    DefaultRestClientBuilder builder = new DefaultRestClientBuilder();

    builder.baseURI((String) null);

    assertThat(builder.baseURI).isNull();
  }

  @Test
  void baseURISetterWithNullURI() {
    DefaultRestClientBuilder builder = new DefaultRestClientBuilder();

    builder.baseURI((URI) null);

    assertThat(builder.baseURI).isNull();
  }

  @Test
  void initHeadersReturnsSameInstance() {
    DefaultRestClientBuilder builder = new DefaultRestClientBuilder();

    HttpHeaders headers1 = builder.initHeaders();
    HttpHeaders headers2 = builder.initHeaders();

    assertThat(headers1).isSameAs(headers2);
  }

  @Test
  void initCookiesReturnsSameInstance() {
    DefaultRestClientBuilder builder = new DefaultRestClientBuilder();

    MultiValueMap<String, String> cookies1 = builder.initCookies();
    MultiValueMap<String, String> cookies2 = builder.initCookies();

    assertThat(cookies1).isSameAs(cookies2);
  }

  @Test
  void initInterceptorsReturnsSameInstance() {
    DefaultRestClientBuilder builder = new DefaultRestClientBuilder();

    List<ClientHttpRequestInterceptor> interceptors1 = builder.initInterceptors();
    List<ClientHttpRequestInterceptor> interceptors2 = builder.initInterceptors();

    assertThat(interceptors1).isSameAs(interceptors2);
  }

  @Test
  void initInitializersReturnsSameInstance() {
    DefaultRestClientBuilder builder = new DefaultRestClientBuilder();

    List<ClientHttpRequestInitializer> initializers1 = builder.initInitializers();
    List<ClientHttpRequestInitializer> initializers2 = builder.initInitializers();

    assertThat(initializers1).isSameAs(initializers2);
  }

  @Test
  void requestInterceptorWithNullThrowsException() {
    DefaultRestClientBuilder builder = new DefaultRestClientBuilder();

    assertThatThrownBy(() -> builder.requestInterceptor(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Interceptor is required");
  }

  @Test
  void requestInitializerWithNullThrowsException() {
    DefaultRestClientBuilder builder = new DefaultRestClientBuilder();

    assertThatThrownBy(() -> builder.requestInitializer(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Initializer is required");
  }

  @Test
  void messageConvertersWithNullListThrowsException() {
    DefaultRestClientBuilder builder = new DefaultRestClientBuilder();

    assertThatThrownBy(() -> builder.messageConverters((List<HttpMessageConverter<?>>) null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("At least one HttpMessageConverter is required");
  }

  @Test
  void messageConvertersWithEmptyListThrowsException() {
    DefaultRestClientBuilder builder = new DefaultRestClientBuilder();

    assertThatThrownBy(() -> builder.messageConverters(List.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("At least one HttpMessageConverter is required");
  }

  @Test
  void messageConvertersWithNullElementThrowsException() {
    DefaultRestClientBuilder builder = new DefaultRestClientBuilder();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    converters.add(null);

    assertThatThrownBy(() -> builder.messageConverters(converters))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("The HttpMessageConverter list must not contain null elements");
  }

  @Test
  void buildWithCustomRequestFactory() {
    DefaultRestClientBuilder builder = new DefaultRestClientBuilder();
    ClientHttpRequestFactory customRequestFactory = mock(ClientHttpRequestFactory.class);
    builder.requestFactory(customRequestFactory);

    RestClient client = builder.build();

    assertThat(client).isNotNull();
  }

  @Test
  void buildWithCustomUriBuilderFactory() {
    DefaultRestClientBuilder builder = new DefaultRestClientBuilder();
    UriBuilderFactory customUriBuilderFactory = mock(UriBuilderFactory.class);
    builder.uriBuilderFactory(customUriBuilderFactory);

    RestClient client = builder.build();

    assertThat(client).isNotNull();
  }

  @Test
  void buildWithBaseURIString() {
    DefaultRestClientBuilder builder = new DefaultRestClientBuilder();
    builder.baseURI("http://example.com/api");

    RestClient client = builder.build();

    assertThat(client).isNotNull();
  }

  @Test
  void buildWithBaseURI() {
    DefaultRestClientBuilder builder = new DefaultRestClientBuilder();
    builder.baseURI(URI.create("http://example.com/api"));

    RestClient client = builder.build();

    assertThat(client).isNotNull();
  }

  @Test
  void getUriBuilderFactoryReturnsNullForNonDefaultUriTemplateHandler() {
    RestTemplate restTemplate = new RestTemplate();
    restTemplate.setUriTemplateHandler(mock(UriTemplateHandler.class));

    UriBuilderFactory result = DefaultRestClientBuilder.getUriBuilderFactory(restTemplate);

    assertThat(result).isNull();
  }

  @Test
  void getRequestFactoryExtractsInnerFactoryFromInterceptingFactory() {
    ClientHttpRequestFactory innerFactory = mock(ClientHttpRequestFactory.class);
    InterceptingClientHttpRequestFactory interceptingFactory = new InterceptingClientHttpRequestFactory(innerFactory, List.of());
    RestTemplate restTemplate = new RestTemplate();
    restTemplate.setRequestFactory(interceptingFactory);

    ClientHttpRequestFactory result = DefaultRestClientBuilder.getRequestFactory(restTemplate);

    assertThat(result).isSameAs(innerFactory);
  }

}