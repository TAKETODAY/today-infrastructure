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

package infra.web.client;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import infra.core.Pair;
import infra.core.ParameterizedTypeReference;
import infra.core.ResolvableType;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpRequest;
import infra.http.HttpStatusCode;
import infra.http.MediaType;
import infra.http.ResponseEntity;
import infra.http.StreamingHttpOutputMessage;
import infra.http.client.BufferingClientHttpRequestFactory;
import infra.http.client.ClientHttpRequest;
import infra.http.client.ClientHttpRequestFactory;
import infra.http.client.ClientHttpRequestInitializer;
import infra.http.client.ClientHttpRequestInterceptor;
import infra.http.client.ClientHttpResponse;
import infra.http.client.InterceptingClientHttpRequestFactory;
import infra.http.converter.GenericHttpMessageConverter;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.HttpMessageNotReadableException;
import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.CollectionUtils;
import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;
import infra.util.concurrent.Future;
import infra.web.util.UriBuilder;
import infra.web.util.UriBuilderFactory;

/**
 * Default implementation of {@link RestClient}.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class DefaultRestClient implements RestClient {

  private static final Logger logger = LoggerFactory.getLogger(DefaultRestClient.class);

  private final ClientHttpRequestFactory clientRequestFactory;

  @Nullable
  private volatile ClientHttpRequestFactory interceptingRequestFactory;

  @Nullable
  private final List<ClientHttpRequestInitializer> initializers;

  @Nullable
  private final List<ClientHttpRequestInterceptor> interceptors;

  final UriBuilderFactory uriBuilderFactory;

  @Nullable
  private final HttpHeaders defaultHeaders;

  @Nullable
  private final MultiValueMap<String, String> defaultCookies;

  @Nullable
  private final List<ResponseErrorHandler> defaultStatusHandlers;

  private final ResponseErrorHandler defaultStatusHandler;

  final DefaultRestClientBuilder builder;

  private final List<HttpMessageConverter<?>> messageConverters;

  @Nullable
  private final Consumer<RequestHeadersSpec<?>> defaultRequest;

  private final boolean ignoreStatusHandlers;

  private final boolean detectEmptyMessageBody;

  @Nullable
  private final Predicate<HttpRequest> bufferingPredicate;

  @Nullable
  private final ApiVersionInserter apiVersionInserter;

  @Nullable
  private final Object defaultApiVersion;

  DefaultRestClient(ClientHttpRequestFactory clientRequestFactory,
          @Nullable List<ClientHttpRequestInterceptor> interceptors,
          @Nullable List<ClientHttpRequestInitializer> initializers,
          UriBuilderFactory uriBuilderFactory, @Nullable HttpHeaders defaultHeaders,
          @Nullable MultiValueMap<String, String> defaultCookies,
          @Nullable Consumer<RequestHeadersSpec<?>> defaultRequest,
          @Nullable List<ResponseErrorHandler> statusHandlers,
          @Nullable Predicate<HttpRequest> bufferingPredicate,
          List<HttpMessageConverter<?>> messageConverters, DefaultRestClientBuilder builder,
          boolean ignoreStatusHandlers, boolean detectEmptyMessageBody,
          @Nullable ApiVersionInserter apiVersionInserter, @Nullable Object defaultApiVersion) {

    this.clientRequestFactory = clientRequestFactory;
    this.initializers = initializers;
    this.interceptors = interceptors;
    this.uriBuilderFactory = uriBuilderFactory;
    this.defaultHeaders = defaultHeaders;
    this.defaultCookies = defaultCookies;
    this.defaultRequest = defaultRequest;
    this.defaultStatusHandlers = statusHandlers;
    this.bufferingPredicate = bufferingPredicate;
    this.messageConverters = messageConverters;
    this.builder = builder;
    this.defaultStatusHandler = StatusHandler.createDefaultStatusHandler(messageConverters);
    this.ignoreStatusHandlers = ignoreStatusHandlers;
    this.detectEmptyMessageBody = detectEmptyMessageBody;
    this.apiVersionInserter = apiVersionInserter;
    this.defaultApiVersion = defaultApiVersion;
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
  public RequestBodyUriSpec delete() {
    return methodInternal(HttpMethod.DELETE);
  }

  @Override
  public RequestHeadersUriSpec<?> options() {
    return methodInternal(HttpMethod.OPTIONS);
  }

  @Override
  public RequestBodyUriSpec method(HttpMethod method) {
    Assert.notNull(method, "HttpMethod is required");
    return methodInternal(method);
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
    return new DefaultRestClientBuilder(this.builder);
  }

  @Nullable
  @SuppressWarnings({ "rawtypes", "unchecked" })
  private <T> T readWithMessageConverters(ClientHttpResponse clientResponse, @Nullable ResponseConsumer callback, Type bodyType, Class<T> bodyClass) {
    MediaType contentType = getContentType(clientResponse);

    try {
      if (callback != null) {
        callback.accept(clientResponse);
      }

      if (detectEmptyMessageBody) {
        var responseWrapper = new IntrospectingClientHttpResponse(clientResponse);
        if (!responseWrapper.hasMessageBody() || responseWrapper.hasEmptyMessageBody()) {
          return null;
        }
        clientResponse = responseWrapper;
      }

      for (HttpMessageConverter<?> hmc : this.messageConverters) {
        if (hmc instanceof GenericHttpMessageConverter ghmc) {
          if (ghmc.canRead(bodyType, null, contentType)) {
            if (logger.isDebugEnabled()) {
              logger.debug("Reading to [{}]", ResolvableType.forType(bodyType));
            }
            return (T) ghmc.read(bodyType, null, clientResponse);
          }
        }
        else if (hmc.canRead(bodyClass, contentType)) {
          if (logger.isDebugEnabled()) {
            logger.debug("Reading to [{}] as \"{}\"", bodyClass.getName(), contentType);
          }
          return (T) hmc.read((Class) bodyClass, clientResponse);
        }
      }
      throw new UnknownContentTypeException(bodyType, contentType,
              clientResponse.getStatusCode(), clientResponse.getStatusText(),
              clientResponse.getHeaders(), RestClientUtils.getBody(clientResponse));
    }
    catch (IOException | HttpMessageNotReadableException ex) {
      throw new RestClientException("Error while extracting response for type [%s] and content type [%s]"
              .formatted(ResolvableType.forType(bodyType), contentType), ex);
    }
    finally {
      clientResponse.close();
    }
  }

  static MediaType getContentType(ClientHttpResponse clientResponse) {
    MediaType contentType = clientResponse.getHeaders().getContentType();
    if (contentType == null) {
      contentType = MediaType.APPLICATION_OCTET_STREAM;
    }
    return contentType;
  }

  @SuppressWarnings("unchecked")
  static <T> Class<T> bodyClass(Type type) {
    if (type instanceof Class<?> clazz) {
      return (Class<T>) clazz;
    }
    if (type instanceof ParameterizedType parameterizedType &&
            parameterizedType.getRawType() instanceof Class<?> rawType) {
      return (Class<T>) rawType;
    }
    return (Class<T>) Object.class;
  }

  private void applyStatusHandlers(HttpRequest request, ClientHttpResponse response,
          @Nullable List<ResponseErrorHandler> statusHandlers) throws IOException {

    if (response instanceof DefaultClientResponse cr) {
      response = cr.delegate;
    }

    if (defaultStatusHandlers != null) {
      for (ResponseErrorHandler handler : defaultStatusHandlers) {
        if (handler.hasError(response)) {
          handler.handleError(request, response);
          return;
        }
      }
    }

    if (statusHandlers != null) {
      for (ResponseErrorHandler handler : statusHandlers) {
        if (handler.hasError(response)) {
          handler.handleError(request, response);
          return;
        }
      }
    }

    if (defaultStatusHandler.hasError(response)) {
      defaultStatusHandler.handleError(request, response);
    }
  }

  class DefaultRequestBodyUriSpec implements RequestBodyUriSpec {

    public final HttpMethod httpMethod;

    public @Nullable URI uri;

    public @Nullable HttpHeaders headers;

    public @Nullable MultiValueMap<String, String> cookies;

    public @Nullable InternalBody body;

    public @Nullable Consumer<ClientHttpRequest> httpRequestConsumer;

    public @Nullable Map<String, Object> attributes;

    public @Nullable Object apiVersion;

    public DefaultRequestBodyUriSpec(HttpMethod httpMethod) {
      this.httpMethod = httpMethod;
    }

    @Override
    public RequestBodySpec uri(String uriTemplate, Object... uriVariables) {
      return uri(DefaultRestClient.this.uriBuilderFactory.expand(uriTemplate, uriVariables));
    }

    @Override
    public RequestBodySpec uri(String uriTemplate, Map<String, ?> uriVariables) {
      return uri(DefaultRestClient.this.uriBuilderFactory.expand(uriTemplate, uriVariables));
    }

    @Override
    public RequestBodySpec uri(String uriTemplate, Function<UriBuilder, URI> uriFunction) {
      return uri(uriFunction.apply(DefaultRestClient.this.uriBuilderFactory.uriString(uriTemplate)));
    }

    @Override
    public RequestBodySpec uri(Function<UriBuilder, URI> uriFunction) {
      return uri(uriFunction.apply(DefaultRestClient.this.uriBuilderFactory.builder()));
    }

    @Override
    public RequestBodySpec uri(URI uri) {
      if (uri.isAbsolute()) {
        this.uri = uri;
      }
      else {
        URI baseUri = DefaultRestClient.this.uriBuilderFactory.expand("");
        this.uri = baseUri.resolve(uri);
      }
      return this;
    }

    @Override
    public RequestBodySpec apiVersion(@Nullable Object version) {
      this.apiVersion = version;
      return this;
    }

    private HttpHeaders httpHeaders() {
      if (this.headers == null) {
        this.headers = HttpHeaders.forWritable();
      }
      return this.headers;
    }

    private MultiValueMap<String, String> cookies() {
      if (this.cookies == null) {
        this.cookies = new LinkedMultiValueMap<>(3);
      }
      return this.cookies;
    }

    @Override
    public DefaultRequestBodyUriSpec header(String headerName, String... headerValues) {
      httpHeaders().setOrRemove(headerName, headerValues);
      return this;
    }

    @Override
    public DefaultRequestBodyUriSpec headers(Consumer<HttpHeaders> headersConsumer) {
      headersConsumer.accept(httpHeaders());
      return this;
    }

    @Override
    public RequestBodySpec headers(@Nullable HttpHeaders headers) {
      httpHeaders().setAll(headers);
      return this;
    }

    @Override
    public RequestBodySpec cookie(String name, String value) {
      cookies().add(name, value);
      return this;
    }

    @Override
    public RequestBodySpec cookies(Consumer<MultiValueMap<String, String>> cookiesConsumer) {
      cookiesConsumer.accept(cookies());
      return this;
    }

    @Override
    public RequestBodySpec cookies(MultiValueMap<String, String> cookies) {
      cookies().setAll(cookies);
      return this;
    }

    @Override
    public RequestBodySpec attribute(String name, Object value) {
      attributes().put(name, value);
      return this;
    }

    @Override
    public RequestBodySpec attributes(Consumer<Map<String, Object>> attributesConsumer) {
      attributesConsumer.accept(attributes());
      return this;
    }

    @Override
    public RequestBodySpec attributes(@Nullable Map<String, Object> attributes) {
      if (CollectionUtils.isNotEmpty(attributes)) {
        attributes().putAll(attributes);
      }
      return this;
    }

    private Map<String, Object> attributes() {
      Map<String, Object> attributes = this.attributes;
      if (attributes == null) {
        attributes = new LinkedHashMap<>(4);
        this.attributes = attributes;
      }
      return attributes;
    }

    @Override
    public DefaultRequestBodyUriSpec accept(MediaType... acceptableMediaTypes) {
      httpHeaders().setAccept(Arrays.asList(acceptableMediaTypes));
      return this;
    }

    @Override
    public DefaultRequestBodyUriSpec acceptCharset(Charset... acceptableCharsets) {
      httpHeaders().setAcceptCharset(Arrays.asList(acceptableCharsets));
      return this;
    }

    @Override
    public DefaultRequestBodyUriSpec contentType(MediaType contentType) {
      httpHeaders().setContentType(contentType);
      return this;
    }

    @Override
    public DefaultRequestBodyUriSpec contentLength(long contentLength) {
      httpHeaders().setContentLength(contentLength);
      return this;
    }

    @Override
    public DefaultRequestBodyUriSpec ifModifiedSince(ZonedDateTime ifModifiedSince) {
      httpHeaders().setIfModifiedSince(ifModifiedSince);
      return this;
    }

    @Override
    public DefaultRequestBodyUriSpec ifNoneMatch(String... ifNoneMatches) {
      httpHeaders().setIfNoneMatch(Arrays.asList(ifNoneMatches));
      return this;
    }

    @Override
    public RequestBodySpec httpRequest(Consumer<ClientHttpRequest> requestConsumer) {
      this.httpRequestConsumer = (this.httpRequestConsumer != null ?
              this.httpRequestConsumer.andThen(requestConsumer) : requestConsumer);
      return this;
    }

    @Override
    public RequestBodySpec body(Object body) {
      this.body = request -> writeWithMessageConverters(body, body.getClass(), request);
      return this;
    }

    @Override
    public <T> RequestBodySpec body(T body, ParameterizedTypeReference<T> bodyType) {
      this.body = request -> writeWithMessageConverters(body, bodyType.getType(), request);
      return this;
    }

    @Override
    public RequestBodySpec body(StreamingHttpOutputMessage.Body body) {
      this.body = request -> {
        if (request instanceof StreamingHttpOutputMessage shom) {
          shom.setBody(body);
        }
        else {
          body.writeTo(request.getBody());
        }
      };
      return this;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void writeWithMessageConverters(Object body, Type bodyType, ClientHttpRequest clientRequest)
            throws IOException {

      MediaType contentType = clientRequest.getHeaders().getContentType();
      Class<?> bodyClass = body.getClass();

      for (HttpMessageConverter hmc : DefaultRestClient.this.messageConverters) {
        if (hmc instanceof GenericHttpMessageConverter ghmc) {
          if (ghmc.canWrite(bodyType, bodyClass, contentType)) {
            logBody(body, contentType, ghmc);
            ghmc.write(body, bodyType, contentType, clientRequest);
            return;
          }
        }
        else if (hmc.canWrite(bodyClass, contentType)) {
          logBody(body, contentType, hmc);
          hmc.write(body, contentType, clientRequest);
          return;
        }
      }
      String message = "No HttpMessageConverter for " + bodyClass.getName();
      if (contentType != null) {
        message += " and content type \"%s\"".formatted(contentType);
      }
      throw new RestClientException(message);
    }

    private void logBody(Object body, @Nullable MediaType mediaType, HttpMessageConverter<?> converter) {
      if (logger.isDebugEnabled()) {
        StringBuilder msg = new StringBuilder("Writing [");
        msg.append(body);
        msg.append("] ");
        if (mediaType != null) {
          msg.append("as \"");
          msg.append(mediaType);
          msg.append("\" ");
        }
        msg.append("with ");
        msg.append(converter.getClass().getName());
        logger.debug(msg.toString());
      }
    }

    @Override
    public ResponseSpec retrieve() {
      return exchangeInternal(DefaultResponseSpec::new, false);
    }

    @Override
    public AsyncSpec async(@Nullable Executor executor) {
      var pair = asyncInternal(executor);
      return new DefaultAsyncSpec(pair.first, pair.second);
    }

    @Override
    public Future<ClientResponse> send(@Nullable Executor executor) {
      return asyncInternal(executor).second
              .map(DefaultClientResponse::new);
    }

    private Pair<ClientHttpRequest, Future<ClientHttpResponse>> asyncInternal(@Nullable Executor executor) {
      ClientHttpRequest clientRequest = null;
      try {
        URI uri = initURI();
        clientRequest = createRequest(uri);
        return Pair.of(clientRequest, clientRequest.async(executor)
                .onErrorMap(IOException.class, ex -> createResourceAccessException(uri, this.httpMethod, ex)));
      }
      catch (Throwable e) {
        return Pair.of(clientRequest, Future.failed(e, executor));
      }
    }

    @Override
    public ClientResponse execute() {
      return execute(true);
    }

    @Override
    public ClientResponse execute(boolean close) {
      return exchangeInternal((clientRequest, clientResponse) -> clientResponse, close);
    }

    @Override
    public <T> T exchange(ExchangeFunction<T> exchangeFunction, boolean close) {
      return exchangeInternal(exchangeFunction, close);
    }

    private <T> T exchangeInternal(ExchangeFunction<T> exchangeFunction, boolean close) {
      Assert.notNull(exchangeFunction, "ExchangeFunction is required");

      URI uri = initURI();
      var clientRequest = createRequest(uri);
      ClientHttpResponse clientResponse = null;
      try {
        clientResponse = clientRequest.execute();
        var convertibleWrapper = new DefaultClientResponse(clientResponse);
        return exchangeFunction.exchange(clientRequest, convertibleWrapper);
      }
      catch (IOException ex) {
        throw createResourceAccessException(uri, this.httpMethod, ex);
      }
      finally {
        if (close && clientResponse != null) {
          clientResponse.close();
        }
      }
    }

    private URI initURI() {
      URI uriToUse = this.uri != null ? this.uri : uriBuilderFactory.expand("");
      Object version = getApiVersionOrDefault();
      if (version != null) {
        Assert.state(apiVersionInserter != null, "No ApiVersionInserter configured");
        uriToUse = apiVersionInserter.insertVersion(version, uriToUse);
      }
      return uriToUse;
    }

    private ClientHttpRequest createRequest(URI uri) throws ResourceAccessException {
      ClientHttpRequestFactory factory;
      if (interceptors != null) {
        factory = interceptingRequestFactory;
        if (factory == null) {
          factory = new InterceptingClientHttpRequestFactory(clientRequestFactory, interceptors, bufferingPredicate);
          interceptingRequestFactory = factory;
        }
      }
      else if (bufferingPredicate != null) {
        factory = new BufferingClientHttpRequestFactory(clientRequestFactory, bufferingPredicate);
      }
      else {
        factory = clientRequestFactory;
      }

      try {
        ClientHttpRequest request = factory.createRequest(uri, this.httpMethod);
        HttpHeaders headers = request.getHeaders();
        headers.setAll(defaultHeaders);
        headers.setAll(this.headers);

        Object version = getApiVersionOrDefault();
        if (version != null) {
          Assert.state(apiVersionInserter != null, "No ApiVersionInserter configured");
          apiVersionInserter.insertVersion(version, headers);
        }

        String serializedCookies = serializeCookies();
        if (serializedCookies != null) {
          headers.add(HttpHeaders.COOKIE, serializedCookies);
        }

        request.setAttributes(attributes);

        if (initializers != null) {
          for (ClientHttpRequestInitializer initializer : initializers) {
            initializer.initialize(request);
          }
        }

        if (this.body != null) {
          this.body.writeTo(request);
        }

        if (httpRequestConsumer != null) {
          httpRequestConsumer.accept(request);
        }
        return request;
      }
      catch (IOException ex) {
        throw createResourceAccessException(uri, this.httpMethod, ex);
      }
    }

    @Nullable
    private Object getApiVersionOrDefault() {
      return this.apiVersion != null ? this.apiVersion : DefaultRestClient.this.defaultApiVersion;
    }

    @Nullable
    private String serializeCookies() {
      MultiValueMap<String, String> map;
      MultiValueMap<String, String> defaultCookies = DefaultRestClient.this.defaultCookies;
      if (CollectionUtils.isEmpty(this.cookies)) {
        map = defaultCookies;
      }
      else if (CollectionUtils.isEmpty(defaultCookies)) {
        map = this.cookies;
      }
      else {
        map = new LinkedMultiValueMap<>(defaultCookies.size() + this.cookies.size());
        map.putAll(defaultCookies);
        map.putAll(this.cookies);
      }
      return CollectionUtils.isNotEmpty(map) ? serializeCookies(map) : null;
    }

    private static String serializeCookies(MultiValueMap<String, String> map) {
      boolean first = true;
      StringBuilder sb = new StringBuilder();
      for (Map.Entry<String, List<String>> entry : map.entrySet()) {
        for (String value : entry.getValue()) {
          if (!first) {
            sb.append("; ");
          }
          else {
            first = false;
          }
          sb.append(entry.getKey()).append('=').append(value);
        }
      }
      return sb.toString();
    }

    private static ResourceAccessException createResourceAccessException(URI url, HttpMethod method, IOException ex) {
      StringBuilder msg = new StringBuilder("I/O error on ");
      msg.append(method.name());
      msg.append(" request for \"");
      String urlString = url.toString();
      int idx = urlString.indexOf('?');
      if (idx != -1) {
        msg.append(urlString, 0, idx);
      }
      else {
        msg.append(urlString);
      }
      msg.append("\": ");
      msg.append(ex.getMessage());
      return new ResourceAccessException(msg.toString(), ex);
    }

    @FunctionalInterface
    private interface InternalBody {

      void writeTo(ClientHttpRequest request) throws IOException;
    }
  }

  abstract class AbstractResponseSpec<T extends AbstractResponseSpec<T>> {

    public final HttpRequest clientRequest;

    public final ArrayList<ResponseErrorHandler> statusHandlers = new ArrayList<>();

    protected boolean ignoreStatus;

    public AbstractResponseSpec(HttpRequest clientRequest) {
      this.clientRequest = clientRequest;
      this.ignoreStatus = DefaultRestClient.this.ignoreStatusHandlers;
    }

    public T onStatus(Predicate<HttpStatusCode> statusPredicate, ErrorHandler errorHandler) {
      return onStatusInternal(StatusHandler.of(statusPredicate, errorHandler));
    }

    public T onStatus(ResponseErrorHandler errorHandler) {
      return onStatusInternal(errorHandler);
    }

    @SuppressWarnings("unchecked")
    public T ignoreStatus(boolean ignoreStatus) {
      this.ignoreStatus = ignoreStatus;
      return (T) this;
    }

    @SuppressWarnings("unchecked")
    private T onStatusInternal(ResponseErrorHandler statusHandler) {
      statusHandlers.add(statusHandler);
      return (T) this;
    }

    final ResponseEntity<Void> toBodilessEntity(ClientHttpResponse response) throws ResourceAccessException {
      toBodiless(response);
      return ResponseEntity.status(response.getStatusCode())
              .headers(response.getHeaders())
              .build();
    }

    final Void toBodiless(ClientHttpResponse response) throws ResourceAccessException {
      try (response) {
        if (!ignoreStatus) {
          applyStatusHandlers(clientRequest, response, statusHandlers);
        }
        return null;
      }
      catch (IOException ex) {
        throw new ResourceAccessException("Could not retrieve response status code: " + ex.getMessage(), ex);
      }
    }

    final <R> ResponseEntity<R> toEntityInternal(ClientHttpResponse response, Type bodyType, Class<R> bodyClass) {
      R body = readBody(response, bodyType, bodyClass);
      return ResponseEntity.status(response.getStatusCode())
              .headers(response.getHeaders())
              .body(body);
    }

    @Nullable
    final <R> R readBody(ClientHttpResponse clientResponse, Type bodyType, Class<R> bodyClass) {
      return readWithMessageConverters(clientResponse, ignoreStatus ? null : response ->
              DefaultRestClient.this.applyStatusHandlers(clientRequest, response, statusHandlers), bodyType, bodyClass);
    }

  }

  private class DefaultResponseSpec extends AbstractResponseSpec<DefaultResponseSpec> implements ResponseSpec {

    private final ClientHttpResponse clientResponse;

    DefaultResponseSpec(HttpRequest clientRequest, ClientHttpResponse clientResponse) {
      super(clientRequest);
      this.clientResponse = clientResponse;
    }

    @Nullable
    @Override
    public <T> T body(Class<T> bodyType) {
      return ignoreStatus(false).readBody(clientResponse, bodyType, bodyType);
    }

    @Nullable
    @Override
    public <T> T body(ParameterizedTypeReference<T> bodyType) {
      Type type = bodyType.getType();
      Class<T> bodyClass = bodyClass(type);
      return ignoreStatus(false).readBody(clientResponse, type, bodyClass);
    }

    @Override
    public <T> ResponseEntity<T> toEntity(Class<T> bodyType) {
      return toEntityInternal(clientResponse, bodyType, bodyType);
    }

    @Override
    public <T> ResponseEntity<T> toEntity(ParameterizedTypeReference<T> bodyType) {
      Type type = bodyType.getType();
      Class<T> bodyClass = bodyClass(type);
      return toEntityInternal(clientResponse, type, bodyClass);
    }

    @Override
    public ResponseEntity<Void> toBodilessEntity() {
      return toBodilessEntity(clientResponse);
    }

    @Override
    public void toBodiless() throws RestClientException {
      toBodiless(clientResponse);
    }
  }

  private class DefaultAsyncSpec extends AbstractResponseSpec<DefaultAsyncSpec> implements AsyncSpec {

    private final Future<ClientHttpResponse> clientResponse;

    DefaultAsyncSpec(HttpRequest clientRequest, Future<ClientHttpResponse> clientResponse) {
      super(clientRequest);
      this.clientResponse = clientResponse;
    }

    @Override
    @SuppressWarnings("NullAway")
    public <T> Future<T> body(Class<T> bodyType) {
      return clientResponse.map(response ->
              ignoreStatus(false).readBody(response, bodyType, bodyType));
    }

    @Override
    @SuppressWarnings("NullAway")
    public <T> Future<T> body(ParameterizedTypeReference<T> bodyType) {
      return clientResponse.map(response -> {
        Type type = bodyType.getType();
        Class<T> bodyClass = bodyClass(type);
        return ignoreStatus(false).readBody(response, type, bodyClass);
      });
    }

    @Override
    public <T> Future<ResponseEntity<T>> toEntity(Class<T> bodyType) {
      return toEntityInternal(bodyType, bodyType);
    }

    @Override
    public <T> Future<ResponseEntity<T>> toEntity(ParameterizedTypeReference<T> bodyType) {
      Type type = bodyType.getType();
      Class<T> bodyClass = bodyClass(type);
      return toEntityInternal(type, bodyClass);
    }

    @Override
    public Future<ResponseEntity<Void>> toBodilessEntity() {
      return clientResponse.map(this::toBodilessEntity);
    }

    @Override
    public Future<Void> toBodiless() {
      return clientResponse.map(this::toBodiless);
    }

    private <T> Future<ResponseEntity<T>> toEntityInternal(Type bodyType, Class<T> bodyClass) {
      return clientResponse.map(response -> toEntityInternal(response, bodyType, bodyClass));
    }

  }

  class DefaultClientResponse implements ClientResponse {

    public final ClientHttpResponse delegate;

    public DefaultClientResponse(ClientHttpResponse delegate) {
      this.delegate = delegate;
    }

    @Nullable
    @Override
    public <T> T bodyTo(Class<T> bodyType) {
      return readWithMessageConverters(this.delegate, null, bodyType, bodyType);
    }

    @Nullable
    @Override
    public <T> T bodyTo(ParameterizedTypeReference<T> bodyType) {
      Type type = bodyType.getType();
      Class<T> bodyClass = bodyClass(type);
      return readWithMessageConverters(this.delegate, null, type, bodyClass);
    }

    @Override
    public InputStream getBody() throws IOException {
      return this.delegate.getBody();
    }

    @Override
    public HttpHeaders getHeaders() {
      return this.delegate.getHeaders();
    }

    @Override
    public HttpStatusCode getStatusCode() {
      return this.delegate.getStatusCode();
    }

    @Override
    public int getRawStatusCode() {
      return delegate.getRawStatusCode();
    }

    @Override
    public String getStatusText() {
      return this.delegate.getStatusText();
    }

    @Override
    public void close() {
      this.delegate.close();
    }

  }

  /**
   * @since 5.0
   */
  interface ResponseConsumer {

    void accept(ClientHttpResponse response) throws IOException;
  }
}
