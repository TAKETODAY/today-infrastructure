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

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.spi.mapper.MappingProvider;

import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.reactivestreams.Publisher;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

import cn.taketoday.core.ParameterizedTypeReference;
import cn.taketoday.core.io.ByteArrayResource;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.client.reactive.ClientHttpConnector;
import cn.taketoday.http.client.reactive.ClientHttpRequest;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.test.util.AssertionErrors;
import cn.taketoday.test.util.ExceptionCollector;
import cn.taketoday.test.util.JsonExpectationsHelper;
import cn.taketoday.test.util.XmlExpectationsHelper;
import cn.taketoday.util.LinkedMultiValueMap;
import cn.taketoday.util.MimeType;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.web.reactive.function.BodyInserter;
import cn.taketoday.web.reactive.function.BodyInserters;
import cn.taketoday.web.reactive.function.client.ClientRequest;
import cn.taketoday.web.reactive.function.client.ClientResponse;
import cn.taketoday.web.reactive.function.client.ExchangeFunction;
import cn.taketoday.web.reactive.function.client.ExchangeStrategies;
import cn.taketoday.web.util.UriBuilder;
import cn.taketoday.web.util.UriBuilderFactory;
import reactor.core.publisher.Flux;

/**
 * Default implementation of {@link WebTestClient}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author Michał Rowicki
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class DefaultWebTestClient implements WebTestClient {

  private final WiretapConnector wiretapConnector;

  @Nullable
  private final JsonEncoderDecoder jsonEncoderDecoder;

  private final ExchangeFunction exchangeFunction;

  private final UriBuilderFactory uriBuilderFactory;

  @Nullable
  private final HttpHeaders defaultHeaders;

  @Nullable
  private final MultiValueMap<String, String> defaultCookies;

  private final Consumer<EntityExchangeResult<?>> entityResultConsumer;

  private final Duration responseTimeout;

  private final DefaultWebTestClientBuilder builder;

  private final AtomicLong requestIndex = new AtomicLong();

  DefaultWebTestClient(ClientHttpConnector connector, ExchangeStrategies exchangeStrategies,
          Function<ClientHttpConnector, ExchangeFunction> exchangeFactory, UriBuilderFactory uriBuilderFactory,
          @Nullable HttpHeaders headers, @Nullable MultiValueMap<String, String> cookies,
          Consumer<EntityExchangeResult<?>> entityResultConsumer,
          @Nullable Duration responseTimeout, DefaultWebTestClientBuilder clientBuilder) {

    this.wiretapConnector = new WiretapConnector(connector);
    this.jsonEncoderDecoder = JsonEncoderDecoder.from(
            exchangeStrategies.messageWriters(), exchangeStrategies.messageReaders());
    this.exchangeFunction = exchangeFactory.apply(this.wiretapConnector);
    this.uriBuilderFactory = uriBuilderFactory;
    this.defaultHeaders = headers;
    this.defaultCookies = cookies;
    this.entityResultConsumer = entityResultConsumer;
    this.responseTimeout = (responseTimeout != null ? responseTimeout : Duration.ofSeconds(5));
    this.builder = clientBuilder;
  }

  private Duration getResponseTimeout() {
    return this.responseTimeout;
  }

  @Override
  public RequestHeadersUriSpec<?> get() {
    return methodInternal(HttpMethod.GET);
  }

  @Override
  public RequestHeadersUriSpec<?> head() {
    return methodInternal(HttpMethod.HEAD);
  }

  @Override
  public RequestBodyUriSpec post() {
    return methodInternal(HttpMethod.POST);
  }

  @Override
  public RequestBodyUriSpec put() {
    return methodInternal(HttpMethod.PUT);
  }

  @Override
  public RequestBodyUriSpec patch() {
    return methodInternal(HttpMethod.PATCH);
  }

  @Override
  public RequestHeadersUriSpec<?> delete() {
    return methodInternal(HttpMethod.DELETE);
  }

  @Override
  public RequestHeadersUriSpec<?> options() {
    return methodInternal(HttpMethod.OPTIONS);
  }

  @Override
  public RequestBodyUriSpec method(HttpMethod httpMethod) {
    return methodInternal(httpMethod);
  }

  private RequestBodyUriSpec methodInternal(HttpMethod httpMethod) {
    return new DefaultRequestBodyUriSpec(httpMethod);
  }

  @Override
  public Builder mutate() {
    return new DefaultWebTestClientBuilder(this.builder);
  }

  @Override
  public WebTestClient mutateWith(WebTestClientConfigurer configurer) {
    return mutate().apply(configurer).build();
  }

  private class DefaultRequestBodyUriSpec implements RequestBodyUriSpec {

    private final HttpMethod httpMethod;

    @Nullable
    private URI uri;

    private final HttpHeaders headers;

    @Nullable
    private MultiValueMap<String, String> cookies;

    @Nullable
    private BodyInserter<?, ? super ClientHttpRequest> inserter;

    private final Map<String, Object> attributes = new LinkedHashMap<>(4);

    @Nullable
    private Consumer<ClientHttpRequest> httpRequestConsumer;

    @Nullable
    private String uriTemplate;

    private final String requestId;

    DefaultRequestBodyUriSpec(HttpMethod httpMethod) {
      this.httpMethod = httpMethod;
      this.requestId = String.valueOf(requestIndex.incrementAndGet());
      this.headers = HttpHeaders.forWritable();
      this.headers.add(WebTestClient.WEBTESTCLIENT_REQUEST_ID, this.requestId);
    }

    @Override
    public RequestBodySpec uri(String uriTemplate, Object... uriVariables) {
      this.uriTemplate = uriTemplate;
      return uri(uriBuilderFactory.expand(uriTemplate, uriVariables));
    }

    @Override
    public RequestBodySpec uri(String uriTemplate, Map<String, ?> uriVariables) {
      this.uriTemplate = uriTemplate;
      return uri(uriBuilderFactory.expand(uriTemplate, uriVariables));
    }

    @Override
    public RequestBodySpec uri(Function<UriBuilder, URI> uriFunction) {
      this.uriTemplate = null;
      return uri(uriFunction.apply(uriBuilderFactory.builder()));
    }

    @Override
    public RequestBodySpec uri(URI uri) {
      this.uri = uri;
      return this;
    }

    private HttpHeaders getHeaders() {
      return this.headers;
    }

    private MultiValueMap<String, String> getCookies() {
      if (this.cookies == null) {
        this.cookies = new LinkedMultiValueMap<>(3);
      }
      return this.cookies;
    }

    @Override
    public RequestBodySpec header(String headerName, String... headerValues) {
      for (String headerValue : headerValues) {
        getHeaders().add(headerName, headerValue);
      }
      return this;
    }

    @Override
    public RequestBodySpec headers(Consumer<HttpHeaders> headersConsumer) {
      headersConsumer.accept(getHeaders());
      return this;
    }

    @Override
    public RequestBodySpec attribute(String name, Object value) {
      this.attributes.put(name, value);
      return this;
    }

    @Override
    public RequestBodySpec attributes(Consumer<Map<String, Object>> attributesConsumer) {
      attributesConsumer.accept(this.attributes);
      return this;
    }

    @Override
    public RequestBodySpec accept(MediaType... acceptableMediaTypes) {
      getHeaders().setAccept(Arrays.asList(acceptableMediaTypes));
      return this;
    }

    @Override
    public RequestBodySpec acceptCharset(Charset... acceptableCharsets) {
      getHeaders().setAcceptCharset(Arrays.asList(acceptableCharsets));
      return this;
    }

    @Override
    public RequestBodySpec contentType(MediaType contentType) {
      getHeaders().setContentType(contentType);
      return this;
    }

    @Override
    public RequestBodySpec contentLength(long contentLength) {
      getHeaders().setContentLength(contentLength);
      return this;
    }

    @Override
    public RequestBodySpec cookie(String name, String value) {
      getCookies().add(name, value);
      return this;
    }

    @Override
    public RequestBodySpec cookies(Consumer<MultiValueMap<String, String>> cookiesConsumer) {
      cookiesConsumer.accept(getCookies());
      return this;
    }

    @Override
    public RequestBodySpec ifModifiedSince(ZonedDateTime ifModifiedSince) {
      getHeaders().setIfModifiedSince(ifModifiedSince);
      return this;
    }

    @Override
    public RequestBodySpec ifNoneMatch(String... ifNoneMatches) {
      getHeaders().setIfNoneMatch(Arrays.asList(ifNoneMatches));
      return this;
    }

    @Override
    public RequestHeadersSpec<?> bodyValue(Object body) {
      this.inserter = BodyInserters.fromValue(body);
      return this;
    }

    @Override
    public <T, P extends Publisher<T>> RequestHeadersSpec<?> body(
            P publisher, ParameterizedTypeReference<T> elementTypeRef) {
      this.inserter = BodyInserters.fromPublisher(publisher, elementTypeRef);
      return this;
    }

    @Override
    public <T, P extends Publisher<T>> RequestHeadersSpec<?> body(P publisher, Class<T> elementClass) {
      this.inserter = BodyInserters.fromPublisher(publisher, elementClass);
      return this;
    }

    @Override
    public RequestHeadersSpec<?> body(Object producer, Class<?> elementClass) {
      this.inserter = BodyInserters.fromProducer(producer, elementClass);
      return this;
    }

    @Override
    public RequestHeadersSpec<?> body(Object producer, ParameterizedTypeReference<?> elementTypeRef) {
      this.inserter = BodyInserters.fromProducer(producer, elementTypeRef);
      return this;
    }

    @Override
    public RequestHeadersSpec<?> body(BodyInserter<?, ? super ClientHttpRequest> inserter) {
      this.inserter = inserter;
      return this;
    }

    @Override
    public ResponseSpec exchange() {
      ClientRequest request = initRequestBuilder().build();

      ClientResponse response = exchangeFunction.exchange(request)
              .block(getResponseTimeout());
      Assert.state(response != null, "No ClientResponse");

      ExchangeResult result = wiretapConnector.getExchangeResult(
              this.requestId, this.uriTemplate, getResponseTimeout());

      return new DefaultResponseSpec(result, response,
              DefaultWebTestClient.this.entityResultConsumer, getResponseTimeout(), jsonEncoderDecoder);
    }

    private ClientRequest.Builder initRequestBuilder() {
      ClientRequest.Builder builder = ClientRequest.create(this.httpMethod, initUri())
              .headers(defaultHeaders)
              .headers(headers)
              .cookies(defaultCookies)
              .cookies(cookies)
              .attributes(attributes);

      if (this.httpRequestConsumer != null) {
        builder.httpRequest(this.httpRequestConsumer);
      }

      if (this.inserter != null) {
        builder.body(this.inserter);
      }

      return builder;
    }

    private URI initUri() {
      return (this.uri != null ? this.uri : uriBuilderFactory.expand(""));
    }

  }

  private static class DefaultResponseSpec implements ResponseSpec {

    private final ExchangeResult exchangeResult;

    private final ClientResponse response;

    private final Consumer<EntityExchangeResult<?>> entityResultConsumer;

    private final Duration timeout;

    @Nullable
    private final JsonEncoderDecoder jsonEncoderDecoder;

    DefaultResponseSpec(ExchangeResult exchangeResult, ClientResponse response,
            Consumer<EntityExchangeResult<?>> entityResultConsumer,
            Duration timeout, @Nullable JsonEncoderDecoder jsonEncoderDecoder) {

      this.exchangeResult = exchangeResult;
      this.response = response;
      this.entityResultConsumer = entityResultConsumer;
      this.timeout = timeout;
      this.jsonEncoderDecoder = jsonEncoderDecoder;
    }

    @Override
    public StatusAssertions expectStatus() {
      return new StatusAssertions(this.exchangeResult, this);
    }

    @Override
    public HeaderAssertions expectHeader() {
      return new HeaderAssertions(this.exchangeResult, this);
    }

    @Override
    public CookieAssertions expectCookie() {
      return new CookieAssertions(this.exchangeResult, this);
    }

    @Override
    public <B> BodySpec<B, ?> expectBody(Class<B> bodyType) {
      B body = this.response.bodyToMono(bodyType).block(this.timeout);
      EntityExchangeResult<B> entityResult = initEntityExchangeResult(body);
      return new DefaultBodySpec<>(entityResult);
    }

    @Override
    public <B> BodySpec<B, ?> expectBody(ParameterizedTypeReference<B> bodyType) {
      B body = this.response.bodyToMono(bodyType).block(this.timeout);
      EntityExchangeResult<B> entityResult = initEntityExchangeResult(body);
      return new DefaultBodySpec<>(entityResult);
    }

    @Override
    public <E> ListBodySpec<E> expectBodyList(Class<E> elementType) {
      return getListBodySpec(this.response.bodyToFlux(elementType));
    }

    @Override
    public <E> ListBodySpec<E> expectBodyList(ParameterizedTypeReference<E> elementType) {
      Flux<E> flux = this.response.bodyToFlux(elementType);
      return getListBodySpec(flux);
    }

    private <E> ListBodySpec<E> getListBodySpec(Flux<E> flux) {
      List<E> body = flux.collectList().block(this.timeout);
      EntityExchangeResult<List<E>> entityResult = initEntityExchangeResult(body);
      return new DefaultListBodySpec<>(entityResult);
    }

    @Override
    public BodyContentSpec expectBody() {
      ByteArrayResource resource = this.response.bodyToMono(ByteArrayResource.class).block(this.timeout);
      byte[] body = (resource != null ? resource.getByteArray() : null);
      EntityExchangeResult<byte[]> entityResult = initEntityExchangeResult(body);
      return new DefaultBodyContentSpec(entityResult, jsonEncoderDecoder);
    }

    private <B> EntityExchangeResult<B> initEntityExchangeResult(@Nullable B body) {
      EntityExchangeResult<B> result = new EntityExchangeResult<>(this.exchangeResult, body);
      result.assertWithDiagnostics(() -> this.entityResultConsumer.accept(result));
      return result;
    }

    @Override
    public <T> FluxExchangeResult<T> returnResult(Class<T> elementClass) {
      Flux<T> body;
      if (elementClass.equals(Void.class)) {
        this.response.releaseBody().block();
        body = Flux.empty();
      }
      else {
        body = this.response.bodyToFlux(elementClass);
      }
      return new FluxExchangeResult<>(this.exchangeResult, body);
    }

    @Override
    public <T> FluxExchangeResult<T> returnResult(ParameterizedTypeReference<T> elementTypeRef) {
      Flux<T> body = this.response.bodyToFlux(elementTypeRef);
      return new FluxExchangeResult<>(this.exchangeResult, body);
    }

    @Override
    public ResponseSpec expectAll(ResponseSpecConsumer... consumers) {
      ExceptionCollector exceptionCollector = new ExceptionCollector();
      for (ResponseSpecConsumer consumer : consumers) {
        exceptionCollector.execute(() -> consumer.accept(this));
      }
      try {
        exceptionCollector.assertEmpty();
      }
      catch (RuntimeException ex) {
        throw ex;
      }
      catch (Exception ex) {
        // In theory, a ResponseSpecConsumer should never throw an Exception
        // that is not a RuntimeException, but since ExceptionCollector may
        // throw a checked Exception, we handle this to appease the compiler
        // and in case someone uses a "sneaky throws" technique.
        throw new AssertionError(ex.getMessage(), ex);
      }
      return this;
    }
  }

  private static class DefaultBodySpec<B, S extends BodySpec<B, S>> implements BodySpec<B, S> {

    private final EntityExchangeResult<B> result;

    DefaultBodySpec(EntityExchangeResult<B> result) {
      this.result = result;
    }

    protected EntityExchangeResult<B> getResult() {
      return this.result;
    }

    @Override
    public <T extends S> T isEqualTo(B expected) {
      this.result.assertWithDiagnostics(() ->
              AssertionErrors.assertEquals("Response body", expected, this.result.getResponseBody()));
      return self();
    }

    @Override
    public <T extends S> T value(Matcher<? super B> matcher) {
      this.result.assertWithDiagnostics(() -> MatcherAssert.assertThat(this.result.getResponseBody(), matcher));
      return self();
    }

    @Override
    public <T extends S, R> T value(Function<B, R> bodyMapper, Matcher<? super R> matcher) {
      this.result.assertWithDiagnostics(() -> {
        B body = this.result.getResponseBody();
        MatcherAssert.assertThat(bodyMapper.apply(body), matcher);
      });
      return self();
    }

    @Override
    public <T extends S> T value(Consumer<B> consumer) {
      this.result.assertWithDiagnostics(() -> consumer.accept(this.result.getResponseBody()));
      return self();
    }

    @Override
    public <T extends S> T consumeWith(Consumer<EntityExchangeResult<B>> consumer) {
      this.result.assertWithDiagnostics(() -> consumer.accept(this.result));
      return self();
    }

    @SuppressWarnings("unchecked")
    private <T extends S> T self() {
      return (T) this;
    }

    @Override
    public EntityExchangeResult<B> returnResult() {
      return this.result;
    }
  }

  private static class DefaultListBodySpec<E> extends DefaultBodySpec<List<E>, ListBodySpec<E>>
          implements ListBodySpec<E> {

    DefaultListBodySpec(EntityExchangeResult<List<E>> result) {
      super(result);
    }

    @Override
    public ListBodySpec<E> hasSize(int size) {
      List<E> actual = getResult().getResponseBody();
      String message = "Response body does not contain " + size + " elements";
      getResult().assertWithDiagnostics(() ->
              AssertionErrors.assertEquals(message, size, (actual != null ? actual.size() : 0)));
      return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ListBodySpec<E> contains(E... elements) {
      List<E> expected = Arrays.asList(elements);
      List<E> actual = getResult().getResponseBody();
      String message = "Response body does not contain " + expected;
      getResult().assertWithDiagnostics(() ->
              AssertionErrors.assertTrue(message, (actual != null && new HashSet<>(actual).containsAll(expected))));
      return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ListBodySpec<E> doesNotContain(E... elements) {
      List<E> expected = Arrays.asList(elements);
      List<E> actual = getResult().getResponseBody();
      String message = "Response body should not have contained " + expected;
      getResult().assertWithDiagnostics(() ->
              AssertionErrors.assertTrue(message, (actual == null || !new HashSet<>(actual).containsAll(expected))));
      return this;
    }

    @Override
    public EntityExchangeResult<List<E>> returnResult() {
      return getResult();
    }
  }

  private static class DefaultBodyContentSpec implements BodyContentSpec {

    private final EntityExchangeResult<byte[]> result;

    @Nullable
    private final JsonEncoderDecoder jsonEncoderDecoder;

    private final boolean isEmpty;

    DefaultBodyContentSpec(EntityExchangeResult<byte[]> result, @Nullable JsonEncoderDecoder jsonEncoderDecoder) {
      this.result = result;
      this.isEmpty = (result.getResponseBody() == null || result.getResponseBody().length == 0);
      this.jsonEncoderDecoder = jsonEncoderDecoder;
    }

    @Override
    public EntityExchangeResult<Void> isEmpty() {
      this.result.assertWithDiagnostics(() ->
              AssertionErrors.assertTrue("Expected empty body", this.isEmpty));
      return new EntityExchangeResult<>(this.result, null);
    }

    @Override
    public BodyContentSpec json(String json, boolean strict) {
      this.result.assertWithDiagnostics(() -> {
        try {
          new JsonExpectationsHelper().assertJsonEqual(json, getBodyAsString(), strict);
        }
        catch (Exception ex) {
          throw new AssertionError("JSON parsing error", ex);
        }
      });
      return this;
    }

    @Override
    public BodyContentSpec xml(String expectedXml) {
      this.result.assertWithDiagnostics(() -> {
        try {
          new XmlExpectationsHelper().assertXmlEqual(expectedXml, getBodyAsString());
        }
        catch (Exception ex) {
          throw new AssertionError("XML parsing error", ex);
        }
      });
      return this;
    }

    @Override
    public JsonPathAssertions jsonPath(String expression) {
      return new JsonPathAssertions(this, getBodyAsString(), expression,
              JsonPathConfigurationProvider.getConfiguration(this.jsonEncoderDecoder));
    }

    @Override
    @SuppressWarnings("removal")
    public JsonPathAssertions jsonPath(String expression, Object... args) {
      Assert.hasText(expression, "expression must not be null or empty");
      return jsonPath(expression.formatted(args));
    }

    @Override
    public XpathAssertions xpath(String expression, @Nullable Map<String, String> namespaces, Object... args) {
      return new XpathAssertions(this, expression, namespaces, args);
    }

    private String getBodyAsString() {
      byte[] body = this.result.getResponseBody();
      if (body == null || body.length == 0) {
        return "";
      }
      Charset charset = Optional.ofNullable(this.result.getResponseHeaders().getContentType())
              .map(MimeType::getCharset).orElse(StandardCharsets.UTF_8);
      return new String(body, charset);
    }

    @Override
    public BodyContentSpec consumeWith(Consumer<EntityExchangeResult<byte[]>> consumer) {
      this.result.assertWithDiagnostics(() -> consumer.accept(this.result));
      return this;
    }

    @Override
    public EntityExchangeResult<byte[]> returnResult() {
      return this.result;
    }
  }

  private static class JsonPathConfigurationProvider {

    static Configuration getConfiguration(@Nullable JsonEncoderDecoder jsonEncoderDecoder) {
      Configuration jsonPathConfiguration = Configuration.defaultConfiguration();
      if (jsonEncoderDecoder != null) {
        MappingProvider mappingProvider = new EncoderDecoderMappingProvider(
                jsonEncoderDecoder.encoder(), jsonEncoderDecoder.decoder());
        return jsonPathConfiguration.mappingProvider(mappingProvider);
      }
      return jsonPathConfiguration;
    }
  }
}
