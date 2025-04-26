/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.client.reactive;

import org.reactivestreams.Publisher;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import infra.core.ParameterizedTypeReference;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpStatusCode;
import infra.http.MediaType;
import infra.http.ResponseEntity;
import infra.http.client.reactive.ClientHttpRequest;
import infra.http.client.reactive.ClientHttpResponse;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.CollectionUtils;
import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;
import infra.util.StringUtils;
import infra.web.client.ApiVersionInserter;
import infra.web.reactive.function.BodyExtractor;
import infra.web.reactive.function.BodyInserter;
import infra.web.reactive.function.BodyInserters;
import infra.web.util.UriBuilder;
import infra.web.util.UriBuilderFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

/**
 * Default implementation of {@link WebClient}.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class DefaultWebClient implements WebClient {

  private static final String URI_TEMPLATE_ATTRIBUTE = WebClient.class.getName() + ".uriTemplate";

  private static final Mono<ClientResponse> NO_HTTP_CLIENT_RESPONSE_ERROR = Mono.error(
          () -> new IllegalStateException("The underlying HTTP client completed without emitting a response."));

  private final ExchangeFunction exchangeFunction;

  private final UriBuilderFactory uriBuilderFactory;

  @Nullable
  private final HttpHeaders defaultHeaders;

  @Nullable
  private final MultiValueMap<String, String> defaultCookies;

  @Nullable
  private final Consumer<RequestHeadersSpec<?>> defaultRequest;

  private final DefaultWebClientBuilder builder;

  private final List<DefaultResponseSpec.StatusHandler> defaultStatusHandlers;

  @Nullable
  private final ApiVersionInserter apiVersionInserter;

  DefaultWebClient(ExchangeFunction exchangeFunction, UriBuilderFactory uriBuilderFactory,
          @Nullable HttpHeaders defaultHeaders, @Nullable MultiValueMap<String, String> defaultCookies,
          @Nullable Consumer<RequestHeadersSpec<?>> defaultRequest,
          @Nullable Map<Predicate<HttpStatusCode>, Function<ClientResponse, Mono<? extends Throwable>>> statusHandlerMap,
          DefaultWebClientBuilder builder, @Nullable ApiVersionInserter apiVersionInserter) {

    this.builder = builder;
    this.exchangeFunction = exchangeFunction;
    this.uriBuilderFactory = uriBuilderFactory;
    this.defaultHeaders = defaultHeaders;
    this.defaultCookies = defaultCookies;
    this.defaultRequest = defaultRequest;
    this.apiVersionInserter = apiVersionInserter;
    this.defaultStatusHandlers = initStatusHandlers(statusHandlerMap);
  }

  private static List<DefaultResponseSpec.StatusHandler> initStatusHandlers(
          @Nullable Map<Predicate<HttpStatusCode>, Function<ClientResponse, Mono<? extends Throwable>>> handlerMap) {

    return (CollectionUtils.isEmpty(handlerMap) ? Collections.emptyList() :
            handlerMap.entrySet().stream()
                    .map(entry -> new DefaultResponseSpec.StatusHandler(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList()));
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
    DefaultRequestBodyUriSpec spec = new DefaultRequestBodyUriSpec(httpMethod);
    if (this.defaultRequest != null) {
      this.defaultRequest.accept(spec);
    }
    return spec;
  }

  @Override
  public Builder mutate() {
    return new DefaultWebClientBuilder(this.builder);
  }

  private static Mono<Void> releaseIfNotConsumed(ClientResponse response) {
    return response.releaseBody().onErrorComplete();
  }

  private static <T> Mono<T> releaseIfNotConsumed(ClientResponse response, Throwable ex) {
    return response.releaseBody().onErrorComplete().then(Mono.error(ex));
  }

  private class DefaultRequestBodyUriSpec implements RequestBodyUriSpec, Consumer<HttpHeaders> {

    private final HttpMethod httpMethod;

    @Nullable
    private URI uri;

    @Nullable
    private HttpHeaders headers;

    @Nullable
    private MultiValueMap<String, String> cookies;

    @Nullable
    private BodyInserter<?, ? super ClientHttpRequest> inserter;

    private final LinkedHashMap<String, Object> attributes = new LinkedHashMap<>(4);

    @Nullable
    private Function<Context, Context> contextModifier;

    @Nullable
    private Consumer<ClientHttpRequest> httpRequestConsumer;

    @Nullable
    private Object apiVersion;

    DefaultRequestBodyUriSpec(HttpMethod httpMethod) {
      this.httpMethod = httpMethod;
    }

    @Override
    public RequestBodySpec uri(String uriTemplate, Object... uriVariables) {
      UriBuilder uriBuilder = uriBuilderFactory.uriString(uriTemplate);
      attribute(URI_TEMPLATE_ATTRIBUTE, uriBuilder.toUriString());
      return uri(uriBuilder.build(uriVariables));
    }

    @Override
    public RequestBodySpec uri(String uriTemplate, Map<String, ?> uriVariables) {
      attribute(URI_TEMPLATE_ATTRIBUTE, uriTemplate);
      return uri(uriBuilderFactory.expand(uriTemplate, uriVariables));
    }

    @Override
    public RequestBodySpec uri(String uriTemplate, Function<UriBuilder, URI> uriFunction) {
      attribute(URI_TEMPLATE_ATTRIBUTE, uriTemplate);
      return uri(uriFunction.apply(uriBuilderFactory.uriString(uriTemplate)));
    }

    @Override
    public RequestBodySpec uri(Function<UriBuilder, URI> uriFunction) {
      return uri(uriFunction.apply(uriBuilderFactory.builder()));
    }

    @Override
    public RequestBodySpec uri(URI uri) {
      this.uri = uri;
      return this;
    }

    private HttpHeaders headers() {
      if (this.headers == null) {
        this.headers = HttpHeaders.forWritable();
      }
      return this.headers;
    }

    private MultiValueMap<String, String> getCookies() {
      if (this.cookies == null) {
        this.cookies = new LinkedMultiValueMap<>(3);
      }
      return this.cookies;
    }

    @Override
    public DefaultRequestBodyUriSpec header(String headerName, String... headerValues) {
      headers().setOrRemove(headerName, headerValues);
      return this;
    }

    @Override
    public DefaultRequestBodyUriSpec headers(Consumer<HttpHeaders> headersConsumer) {
      headersConsumer.accept(headers());
      return this;
    }

    @Override
    public DefaultRequestBodyUriSpec headers(@Nullable HttpHeaders headers) {
      headers().setAll(headers);
      return this;
    }

    @Override
    public DefaultRequestBodyUriSpec apiVersion(@Nullable Object version) {
      this.apiVersion = version;
      return this;
    }

    @Override
    public DefaultRequestBodyUriSpec accept(MediaType... acceptableMediaTypes) {
      headers().setAccept(Arrays.asList(acceptableMediaTypes));
      return this;
    }

    @Override
    public DefaultRequestBodyUriSpec acceptCharset(Charset... acceptableCharsets) {
      headers().setAcceptCharset(Arrays.asList(acceptableCharsets));
      return this;
    }

    @Override
    public DefaultRequestBodyUriSpec contentType(MediaType contentType) {
      headers().setContentType(contentType);
      return this;
    }

    @Override
    public DefaultRequestBodyUriSpec contentLength(long contentLength) {
      headers().setContentLength(contentLength);
      return this;
    }

    @Override
    public DefaultRequestBodyUriSpec cookie(String name, String value) {
      getCookies().setOrRemove(name, value);
      return this;
    }

    @Override
    public DefaultRequestBodyUriSpec cookies(Consumer<MultiValueMap<String, String>> cookiesConsumer) {
      cookiesConsumer.accept(getCookies());
      return this;
    }

    @Override
    public RequestBodySpec cookies(@Nullable MultiValueMap<String, String> cookies) {
      getCookies().setAll(cookies);
      return this;
    }

    @Override
    public DefaultRequestBodyUriSpec ifModifiedSince(ZonedDateTime ifModifiedSince) {
      headers().setIfModifiedSince(ifModifiedSince);
      return this;
    }

    @Override
    public DefaultRequestBodyUriSpec ifNoneMatch(String... ifNoneMatches) {
      headers().setIfNoneMatch(Arrays.asList(ifNoneMatches));
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
    public RequestBodySpec attributes(@Nullable Map<String, Object> attributes) {
      if (attributes != null) {
        this.attributes.putAll(attributes);
      }
      return this;
    }

    @Override
    public RequestBodySpec context(Function<Context, Context> contextModifier) {
      this.contextModifier = (this.contextModifier != null ?
              this.contextModifier.andThen(contextModifier) : contextModifier);
      return this;
    }

    @Override
    public RequestBodySpec httpRequest(Consumer<ClientHttpRequest> requestConsumer) {
      this.httpRequestConsumer = (this.httpRequestConsumer != null ?
              this.httpRequestConsumer.andThen(requestConsumer) : requestConsumer);
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
    public ResponseSpec retrieve() {
      return new DefaultResponseSpec(
              this.httpMethod, initURI(), exchange(), DefaultWebClient.this.defaultStatusHandlers);
    }

    @Override
    public <V> Mono<V> exchangeToMono(Function<ClientResponse, ? extends Mono<V>> responseHandler) {
      return exchange().flatMap(response -> {
        try {
          return responseHandler.apply(response)
                  .flatMap(value -> releaseIfNotConsumed(response).thenReturn(value))
                  .switchIfEmpty(Mono.defer(() -> releaseIfNotConsumed(response).then(Mono.empty())))
                  .onErrorResume(ex -> releaseIfNotConsumed(response, ex));
        }
        catch (Throwable ex) {
          return releaseIfNotConsumed(response, ex);
        }
      });
    }

    @Override
    public <V> Flux<V> exchangeToFlux(Function<ClientResponse, ? extends Flux<V>> responseHandler) {
      return exchange().flatMapMany(response -> {
        try {
          return responseHandler.apply(response)
                  .concatWith(Flux.defer(() -> releaseIfNotConsumed(response).then(Mono.empty())))
                  .onErrorResume(ex -> releaseIfNotConsumed(response, ex));
        }
        catch (Throwable ex) {
          return releaseIfNotConsumed(response, ex);
        }
      });
    }

    @Override
    public Mono<ClientResponse> exchange() {
      ClientRequest.Builder requestBuilder = initRequestBuilder();

      return Mono.defer(() -> {
        ClientRequest request = requestBuilder.build();
        Mono<ClientResponse> responseMono = exchangeFunction.exchange(request)
                .checkpoint("Request to %s %s [DefaultWebClient]".formatted(httpMethod.name(), this.uri))
                .switchIfEmpty(NO_HTTP_CLIENT_RESPONSE_ERROR);
        if (this.contextModifier != null) {
          responseMono = responseMono.contextWrite(this.contextModifier);
        }
        return responseMono;
      });
    }

    private ClientRequest.Builder initRequestBuilder() {
      var builder = ClientRequest.create(this.httpMethod, initURI())
              .headers(this)
              .cookies(defaultCookies)
              .cookies(cookies)
              .attributes(attributes);

      if (httpRequestConsumer != null) {
        builder.httpRequest(httpRequestConsumer);
      }
      if (inserter != null) {
        builder.body(inserter);
      }
      return builder;
    }

    private URI initURI() {
      URI uriToUse = this.uri != null ? this.uri : uriBuilderFactory.expand("");
      if (this.apiVersion != null) {
        Assert.state(apiVersionInserter != null, "No ApiVersionInserter configured");
        uriToUse = apiVersionInserter.insertVersion(this.apiVersion, uriToUse);
      }
      return uriToUse;
    }

    @Override
    public void accept(HttpHeaders headers) {
      if (defaultHeaders != null && !defaultHeaders.isEmpty()) {
        headers.putAll(defaultHeaders);
      }
      if (this.headers != null && !this.headers.isEmpty()) {
        headers.putAll(this.headers);
      }
      if (apiVersion != null) {
        Assert.state(apiVersionInserter != null, "No ApiVersionInserter configured");
        apiVersionInserter.insertVersion(apiVersion, headers);
      }
    }

  }

  private static class DefaultResponseSpec implements ResponseSpec {

    private static final StatusHandler DEFAULT_STATUS_HANDLER =
            new StatusHandler(code -> code.value() >= 400, ClientResponse::createException);

    private final HttpMethod httpMethod;

    private final URI uri;

    private final Mono<ClientResponse> responseMono;

    private final ArrayList<StatusHandler> statusHandlers = new ArrayList<>(1);

    private final int defaultStatusHandlerCount;

    DefaultResponseSpec(HttpMethod httpMethod, URI uri,
            Mono<ClientResponse> responseMono, List<StatusHandler> defaultStatusHandlers) {

      this.uri = uri;
      this.httpMethod = httpMethod;
      this.responseMono = responseMono;
      this.statusHandlers.addAll(defaultStatusHandlers);
      this.statusHandlers.add(DEFAULT_STATUS_HANDLER);
      this.defaultStatusHandlerCount = this.statusHandlers.size();
    }

    @Override
    public ResponseSpec onStatus(Predicate<HttpStatusCode> statusCodePredicate,
            Function<ClientResponse, Mono<? extends Throwable>> exceptionFunction) {

      Assert.notNull(statusCodePredicate, "StatusCodePredicate is required");
      Assert.notNull(exceptionFunction, "Function is required");
      int index = this.statusHandlers.size() - this.defaultStatusHandlerCount;  // Default handlers always last
      this.statusHandlers.add(index, new StatusHandler(statusCodePredicate, exceptionFunction));
      return this;
    }

    @Override
    public ResponseSpec onRawStatus(IntPredicate statusCodePredicate,
            Function<ClientResponse, Mono<? extends Throwable>> exceptionFunction) {

      return onStatus(toStatusCodePredicate(statusCodePredicate), exceptionFunction);
    }

    private static Predicate<HttpStatusCode> toStatusCodePredicate(IntPredicate predicate) {
      return value -> predicate.test(value.value());
    }

    @Override
    public <T> Mono<T> bodyToMono(Class<T> elementClass) {
      Assert.notNull(elementClass, "Class is required");
      return this.responseMono.flatMap(response ->
              handleBodyMono(response, response.bodyToMono(elementClass)));
    }

    @Override
    public <T> Mono<T> bodyToMono(ParameterizedTypeReference<T> elementTypeRef) {
      Assert.notNull(elementTypeRef, "ParameterizedTypeReference is required");
      return this.responseMono.flatMap(response ->
              handleBodyMono(response, response.bodyToMono(elementTypeRef)));
    }

    @Override
    public <T> Flux<T> bodyToFlux(Class<T> elementClass) {
      Assert.notNull(elementClass, "Class is required");
      return this.responseMono.flatMapMany(response ->
              handleBodyFlux(response, response.bodyToFlux(elementClass)));
    }

    @Override
    public <T> Flux<T> bodyToFlux(ParameterizedTypeReference<T> elementTypeRef) {
      Assert.notNull(elementTypeRef, "ParameterizedTypeReference is required");
      return this.responseMono.flatMapMany(response ->
              handleBodyFlux(response, response.bodyToFlux(elementTypeRef)));
    }

    @Override
    public <T> Mono<ResponseEntity<T>> toEntity(Class<T> bodyClass) {
      return this.responseMono.flatMap(response ->
              WebClientUtils.mapToEntity(response,
                      handleBodyMono(response, response.bodyToMono(bodyClass))));
    }

    @Override
    public <T> Mono<ResponseEntity<T>> toEntity(ParameterizedTypeReference<T> bodyTypeRef) {
      return this.responseMono.flatMap(response ->
              WebClientUtils.mapToEntity(response,
                      handleBodyMono(response, response.bodyToMono(bodyTypeRef))));
    }

    @Override
    public <T> Mono<ResponseEntity<List<T>>> toEntityList(Class<T> elementClass) {
      return this.responseMono.flatMap(response ->
              WebClientUtils.mapToEntityList(response,
                      handleBodyFlux(response, response.bodyToFlux(elementClass))));
    }

    @Override
    public <T> Mono<ResponseEntity<List<T>>> toEntityList(ParameterizedTypeReference<T> elementTypeRef) {
      return this.responseMono.flatMap(response ->
              WebClientUtils.mapToEntityList(response,
                      handleBodyFlux(response, response.bodyToFlux(elementTypeRef))));
    }

    @Override
    public <T> Mono<ResponseEntity<Flux<T>>> toEntityFlux(Class<T> elementType) {
      return this.responseMono.flatMap(response ->
              handlerEntityFlux(response, response.bodyToFlux(elementType)));
    }

    @Override
    public <T> Mono<ResponseEntity<Flux<T>>> toEntityFlux(ParameterizedTypeReference<T> elementTypeRef) {
      return this.responseMono.flatMap(response ->
              handlerEntityFlux(response, response.bodyToFlux(elementTypeRef)));
    }

    @Override
    public <T> Mono<ResponseEntity<Flux<T>>> toEntityFlux(BodyExtractor<Flux<T>, ? super ClientHttpResponse> bodyExtractor) {
      return this.responseMono.flatMap(response ->
              handlerEntityFlux(response, response.body(bodyExtractor)));
    }

    @Override
    public Mono<ResponseEntity<Void>> toBodilessEntity() {
      return this.responseMono.flatMap(response ->
              WebClientUtils.mapToEntity(response, handleBodyMono(response, Mono.<Void>empty()))
                      .flatMap(entity -> response.releaseBody()
                              .onErrorResume(WebClientUtils.WRAP_EXCEPTION_PREDICATE, exceptionWrappingFunction(response))
                              .thenReturn(entity))
      );
    }

    @Override
    public Mono<Void> toBodiless() {
      return this.responseMono.flatMap(response ->
              handleBodyMono(response, Mono.<Void>empty())
                      .flatMap(empty -> response.releaseBody()
                              .onErrorResume(WebClientUtils.WRAP_EXCEPTION_PREDICATE, exceptionWrappingFunction(response))
                              .thenReturn(empty))
      );
    }

    private <T> Mono<T> handleBodyMono(ClientResponse response, Mono<T> body) {
      body = body.onErrorResume(WebClientUtils.WRAP_EXCEPTION_PREDICATE, exceptionWrappingFunction(response));
      Mono<T> result = applyStatusHandlers(response);
      return (result != null ? result.switchIfEmpty(body) : body);
    }

    private <T> Publisher<T> handleBodyFlux(ClientResponse response, Flux<T> body) {
      body = body.onErrorResume(WebClientUtils.WRAP_EXCEPTION_PREDICATE, exceptionWrappingFunction(response));
      Mono<T> result = applyStatusHandlers(response);
      return (result != null ? result.flux().switchIfEmpty(body) : body);
    }

    private <T> Mono<? extends ResponseEntity<Flux<T>>> handlerEntityFlux(ClientResponse response, Flux<T> body) {
      ResponseEntity<Flux<T>> entity = new ResponseEntity<>(
              body.onErrorResume(WebClientUtils.WRAP_EXCEPTION_PREDICATE, exceptionWrappingFunction(response)),
              response.headers().asHttpHeaders(),
              response.statusCode());

      Mono<ResponseEntity<Flux<T>>> result = applyStatusHandlers(response);
      return (result != null ? result.defaultIfEmpty(entity) : Mono.just(entity));
    }

    private <T> Function<Throwable, Mono<? extends T>> exceptionWrappingFunction(ClientResponse response) {
      return t -> response.createException().flatMap(ex -> Mono.error(ex.initCause(t)));
    }

    @Nullable
    private <T> Mono<T> applyStatusHandlers(ClientResponse response) {
      HttpStatusCode statusCode = response.statusCode();
      for (StatusHandler handler : this.statusHandlers) {
        if (handler.test(statusCode)) {
          Mono<? extends Throwable> exMono;
          try {
            exMono = handler.apply(response);
            exMono = exMono.flatMap(ex -> releaseIfNotConsumed(response, ex));
            exMono = exMono.onErrorResume(ex -> releaseIfNotConsumed(response, ex));
          }
          catch (Throwable ex2) {
            exMono = releaseIfNotConsumed(response, ex2);
          }
          Mono<T> result = exMono.flatMap(Mono::error);
          return result.checkpoint("%s from %s %s [DefaultWebClient]"
                  .formatted(statusCode, this.httpMethod, getUriToLog(this.uri)));
        }
      }
      return null;
    }

    private static URI getUriToLog(URI uri) {
      if (StringUtils.hasText(uri.getQuery())) {
        try {
          uri = new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), uri.getPath(), null, null);
        }
        catch (URISyntaxException ex) {
          // ignore
        }
      }
      return uri;
    }

    private static class StatusHandler {

      private final Predicate<HttpStatusCode> predicate;

      private final Function<ClientResponse, Mono<? extends Throwable>> exceptionFunction;

      public StatusHandler(Predicate<HttpStatusCode> predicate,
              Function<ClientResponse, Mono<? extends Throwable>> exceptionFunction) {

        this.predicate = predicate;
        this.exceptionFunction = exceptionFunction;
      }

      public boolean test(HttpStatusCode status) {
        return this.predicate.test(status);
      }

      public Mono<? extends Throwable> apply(ClientResponse response) {
        return this.exceptionFunction.apply(response);
      }
    }
  }

}
