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

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import infra.core.ParameterizedTypeReference;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpRequest;
import infra.http.HttpStatusCode;
import infra.http.MediaType;
import infra.http.ResponseEntity;
import infra.http.StreamingHttpOutputMessage;
import infra.http.client.ClientHttpRequest;
import infra.http.client.ClientHttpRequestFactory;
import infra.http.client.ClientHttpRequestInitializer;
import infra.http.client.ClientHttpRequestInterceptor;
import infra.http.client.ClientHttpResponse;
import infra.http.converter.HttpMessageConverter;
import infra.lang.CheckReturnValue;
import infra.lang.Nullable;
import infra.util.MultiValueMap;
import infra.util.concurrent.Future;
import infra.web.util.DefaultUriBuilderFactory;
import infra.web.util.UriBuilder;
import infra.web.util.UriBuilderFactory;

/**
 * Client to perform HTTP requests, exposing a fluent, synchronous API over
 * underlying HTTP client libraries such the JDK {@code HttpClient}, Apache
 * HttpComponents, and others.
 *
 * <p>Use static factory methods {@link #create()}, {@link #create(String)},
 * or {@link RestClient#builder()} to prepare an instance. To use the same
 * configuration as a {@link RestTemplate}, use {@link #create(RestTemplate)} or
 * {@link #builder(RestTemplate)}.
 *
 * <p>For examples with a response body see:
 * <ul>
 * <li>{@link RequestHeadersSpec#retrieve() retrieve()}
 * <li>{@link RequestHeadersSpec#exchange(RequestHeadersSpec.ExchangeFunction) exchange(Function&lt;ClientHttpRequest, T&gt;)}
 * </ul>
 *
 * <p>For examples with a request body see:
 * <ul>
 * <li>{@link RequestBodySpec#body(Object) body(Object)}
 * <li>{@link RequestBodySpec#body(Object, ParameterizedTypeReference) body(Object, ParameterizedTypeReference)}
 * <li>{@link RequestBodySpec#body(StreamingHttpOutputMessage.Body) body(Consumer&lt;OutputStream&gt;)}
 * </ul>
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface RestClient {

  /**
   * Start building an HTTP GET request.
   *
   * @return a spec for specifying the target URL
   */
  RequestHeadersUriSpec<?> get();

  /**
   * Start building an HTTP GET request.
   * <p> Specify the URI using a fully constructed {@link URI}.
   * <p>If the given URI is absolute, it is used as given. If it is
   * a relative URI, the {@link UriBuilderFactory} configured for
   * the client (e.g. with a base URI) will be used to
   * {@linkplain URI#resolve(URI) resolve} the given URI against.
   *
   * @param uri fully constructed {@link URI}.
   * @return a spec for specifying the target URL
   * @since 5.0
   */
  default RequestHeadersSpec<?> get(URI uri) {
    return get().uri(uri);
  }

  /**
   * Start building an HTTP GET request.
   *
   * <p> Specify the URI for the request using a URI template and URI variables.
   * <p>If a {@link UriBuilderFactory} was configured for the client (e.g.
   * with a base URI) it will be used to expand the URI template.
   *
   * @param uri URI template.
   * @param uriVariables URI variables
   * @return a spec for specifying the target URL
   * @since 5.0
   */
  default RequestHeadersSpec<?> get(String uri, Object... uriVariables) {
    return get().uri(uri, uriVariables);
  }

  /**
   * Start building an HTTP HEAD request.
   *
   * @return a spec for specifying the target URL
   */
  RequestHeadersUriSpec<?> head();

  /**
   * Start building an HTTP POST request.
   *
   * @return a spec for specifying the target URL
   */
  RequestBodyUriSpec post();

  /**
   * Start building an HTTP PUT request.
   *
   * @return a spec for specifying the target URL
   */
  RequestBodyUriSpec put();

  /**
   * Start building an HTTP PATCH request.
   *
   * @return a spec for specifying the target URL
   */
  RequestBodyUriSpec patch();

  /**
   * Start building an HTTP DELETE request.
   *
   * @return a spec for specifying the target URL
   */
  RequestBodyUriSpec delete();

  /**
   * Start building an HTTP OPTIONS request.
   *
   * @return a spec for specifying the target URL
   */
  RequestHeadersUriSpec<?> options();

  /**
   * Start building a request for the given {@code HttpMethod}.
   *
   * @return a spec for specifying the target URL
   */
  RequestBodyUriSpec method(HttpMethod method);

  /**
   * Return a builder to create a new {@code RestClient} whose settings are
   * replicated from the current {@code RestClient}.
   */
  Builder mutate();

  // Static, factory methods

  /**
   * Create a new {@code RestClient}.
   *
   * @see #create(String)
   * @see #builder()
   */
  static RestClient create() {
    return new DefaultRestClientBuilder().build();
  }

  /**
   * Variant of {@link #create()} that accepts a default base URL. For more
   * details see {@link Builder#baseURI(String) Builder.baseUrl(String)}.
   *
   * @param baseURI the base URI for all requests
   * @see #builder()
   */
  static RestClient create(String baseURI) {
    return new DefaultRestClientBuilder().baseURI(baseURI).build();
  }

  /**
   * Variant of {@link #create()} that accepts a default base {@code URI}. For more
   * details see {@link Builder#baseURI(URI) Builder.baseUrl(URI)}.
   *
   * @param baseURI the base URI for all requests
   * @see #builder()
   * @since 5.0
   */
  static RestClient create(URI baseURI) {
    return new DefaultRestClientBuilder().baseURI(baseURI).build();
  }

  /**
   * Create a new {@code RestClient} based on the configuration of the
   * given {@code RestTemplate}. The returned builder is configured with the
   * template's
   * <ul>
   * <li>{@link RestTemplate#getRequestFactory() ClientHttpRequestFactory},</li>
   * <li>{@link RestTemplate#getMessageConverters() HttpMessageConverters},</li>
   * <li>{@link RestTemplate#getInterceptors() ClientHttpRequestInterceptors},</li>
   * <li>{@link RestTemplate#getHttpRequestInitializers() ClientHttpRequestInitializers},</li>
   * <li>{@link RestTemplate#getUriTemplateHandler() UriBuilderFactory}, and</li>
   * <li>{@linkplain RestTemplate#getErrorHandler() error handler}.</li>
   * </ul>
   *
   * @param restTemplate the rest template to base the returned client's
   * configuration on
   * @return a {@code RestClient} initialized with the {@code restTemplate}'s
   * configuration
   */
  static RestClient create(RestTemplate restTemplate) {
    return new DefaultRestClientBuilder(restTemplate).build();
  }

  /**
   * Obtain a {@code RestClient} builder.
   */
  static RestClient.Builder builder() {
    return new DefaultRestClientBuilder();
  }

  /**
   * Obtain a {@code RestClient} builder based on the configuration of the
   * given {@code RestTemplate}. The returned builder is configured with the
   * template's
   * <ul>
   * <li>{@link RestTemplate#getRequestFactory() ClientHttpRequestFactory},</li>
   * <li>{@link RestTemplate#getMessageConverters() HttpMessageConverters},</li>
   * <li>{@link RestTemplate#getInterceptors() ClientHttpRequestInterceptors},</li>
   * <li>{@link RestTemplate#getHttpRequestInitializers() ClientHttpRequestInitializers},</li>
   * <li>{@link RestTemplate#getUriTemplateHandler() UriBuilderFactory}, and</li>
   * <li>{@linkplain RestTemplate#getErrorHandler() error handler}.</li>
   * </ul>
   *
   * @param restTemplate the rest template to base the returned builder's
   * configuration on
   * @return a {@code RestClient} builder initialized with {@code restTemplate}'s
   * configuration
   */
  static RestClient.Builder builder(RestTemplate restTemplate) {
    return new DefaultRestClientBuilder(restTemplate);
  }

  /**
   * A mutable builder for creating a {@link RestClient}.
   */
  interface Builder {

    /**
     * Configure a base URL for requests. Effectively a shortcut for:
     * <p>
     * <pre>{@code
     * String baseUrl = "https://abc.go.com/v1";
     * DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(baseUrl);
     * RestClient client = RestClient.builder().uriBuilderFactory(factory).build();
     * }</pre>
     * <p>The {@code DefaultUriBuilderFactory} is used to prepare the URL
     * for every request with the given base URL, unless the URL request
     * for a given URL is absolute in which case the base URL is ignored.
     * <p><strong>Note:</strong> this method is mutually exclusive with
     * {@link #uriBuilderFactory(UriBuilderFactory)}. If both are used, the
     * {@code baseUrl} value provided here will be ignored.
     *
     * @return this builder
     * @see DefaultUriBuilderFactory#DefaultUriBuilderFactory(String)
     * @see #uriBuilderFactory(UriBuilderFactory)
     */
    Builder baseURI(@Nullable String baseURI);

    /**
     * Configure a base {@code URI} for requests. Effectively a shortcut for:
     * <pre>{@code
     * URI baseUri = URI.create("https://abc.go.com/v1");
     * var factory = new DefaultUriBuilderFactory(baseUri.toString());
     * RestClient client = RestClient.builder().uriBuilderFactory(factory).build();
     * }</pre>
     * <p>The {@code DefaultUriBuilderFactory} is used to prepare the URL
     * for every request with the given base URL, unless the URL request
     * for a given URL is absolute in which case the base URL is ignored.
     * <p><strong>Note:</strong> this method is mutually exclusive with
     * {@link #uriBuilderFactory(UriBuilderFactory)}. If both are used, the
     * {@code baseUrl} value provided here will be ignored.
     *
     * @return this builder
     * @see DefaultUriBuilderFactory#DefaultUriBuilderFactory(String)
     * @see #uriBuilderFactory(UriBuilderFactory)
     * @since 5.0
     */
    Builder baseURI(@Nullable URI baseURI);

    /**
     * Configure default URL variable values to use when expanding URI
     * templates with a {@link Map}. Effectively a shortcut for:
     * <p>
     * <pre>{@code
     * Map<String, ?> defaultVars = ...;
     * DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory();
     * factory.setDefaultVariables(defaultVars);
     * RestClient client = RestClient.builder().uriBuilderFactory(factory).build();
     * }</pre>
     * <p><strong>Note:</strong> this method is mutually exclusive with
     * {@link #uriBuilderFactory(UriBuilderFactory)}. If both are used, the
     * {@code defaultUriVariables} value provided here will be ignored.
     *
     * @return this builder
     * @see DefaultUriBuilderFactory#setDefaultUriVariables(Map)
     * @see #uriBuilderFactory(UriBuilderFactory)
     */
    Builder defaultUriVariables(@Nullable Map<String, ?> defaultUriVariables);

    /**
     * Provide a pre-configured {@link UriBuilderFactory} instance. This is
     * an alternative to, and effectively overrides the following shortcut
     * properties:
     * <ul>
     * <li>{@link #baseURI(String)}
     * <li>{@link #defaultUriVariables(Map)}.
     * </ul>
     *
     * @param uriBuilderFactory the URI builder factory to use
     * @return this builder
     * @see #baseURI(String)
     * @see #defaultUriVariables(Map)
     */
    Builder uriBuilderFactory(@Nullable UriBuilderFactory uriBuilderFactory);

    /**
     * Global option to specify a header to be added to every request,
     * if the request does not already contain such a header.
     *
     * @param header the header name
     * @param values the header values
     * @return this builder
     */
    Builder defaultHeader(String header, String... values);

    /**
     * Provide a consumer to access to every {@linkplain #defaultHeader(String, String...)
     * default header} declared so far, with the possibility to add, replace, or remove.
     *
     * @param headersConsumer the consumer
     * @return this builder
     */
    Builder defaultHeaders(Consumer<HttpHeaders> headersConsumer);

    /**
     * Global option to specify headers to be added to every request,
     * if the request does not already contain such a header.
     *
     * @param headers the headers
     * @return this builder
     * @since 5.0
     */
    Builder defaultHeaders(HttpHeaders headers);

    /**
     * Global option to specify a cookie to be added to every request,
     * if the request does not already contain such a cookie.
     *
     * @param cookie the cookie name
     * @param values the cookie values
     * @since 5.0
     */
    Builder defaultCookie(String cookie, @Nullable String... values);

    /**
     * Provides access to every {@link #defaultCookie(String, String...)}
     * declared so far with the possibility to add, replace, or remove.
     *
     * @param cookiesConsumer a function that consumes the cookies map
     * @since 5.0
     */
    Builder defaultCookies(Consumer<MultiValueMap<String, String>> cookiesConsumer);

    /**
     * Global option to specify cookies to be added to every request,
     * if the request does not already contain such a cookie.
     *
     * @param cookies the cookies map
     * @since 5.0
     */
    Builder defaultCookies(MultiValueMap<String, String> cookies);

    /**
     * Global option to specify an API version to be added to every request,
     * if not explicitly set.
     *
     * @param version the version to use
     * @return this builder
     * @since 5.0
     */
    Builder defaultApiVersion(Object version);

    /**
     * Configure an {@link ApiVersionInserter} to abstract how an API version
     * specified via {@link RequestHeadersSpec#apiVersion(Object)}
     * is inserted into the request.
     *
     * @param apiVersionInserter the inserter to use
     * @since 5.0
     */
    Builder apiVersionInserter(@Nullable ApiVersionInserter apiVersionInserter);

    /**
     * Provide a consumer to customize every request being built.
     *
     * @param defaultRequest the consumer to use for modifying requests
     * @return this builder
     */
    Builder defaultRequest(Consumer<RequestHeadersSpec<?>> defaultRequest);

    /**
     * Register a default
     * {@linkplain ResponseSpec#onStatus(Predicate, ErrorHandler) status handler}
     * to apply to every response. Such default handlers are applied in the
     * order in which they are registered, and after any others that are
     * registered for a specific response.
     *
     * @param statusPredicate to match responses with
     * @param errorHandler handler that typically, though not necessarily,
     * throws an exception
     * @return this builder
     */
    Builder defaultStatusHandler(Predicate<HttpStatusCode> statusPredicate, ErrorHandler errorHandler);

    /**
     * Register a default
     * {@linkplain ResponseSpec#onStatus(ResponseErrorHandler) status handler}
     * to apply to every response. Such default handlers are applied in the
     * order in which they are registered, and after any others that are
     * registered for a specific response.
     * <p>The first status handler who claims that a response has an
     * error is invoked. If you want to disable other defaults, consider
     * using {@link #defaultStatusHandler(Predicate, ErrorHandler)}
     * with a predicate that matches all status codes.
     *
     * @param errorHandler handler that typically, though not necessarily,
     * throws an exception
     * @return this builder
     */
    Builder defaultStatusHandler(ResponseErrorHandler errorHandler);

    /**
     * Ignore {@link ResponseErrorHandler status handler} throws exception when
     * using {@link ResponseSpec#toEntity(Class) ResponseEntity result}
     *
     * @return this builder
     * @see HttpStatusCodeException
     * @see ResponseErrorHandler#hasError(ClientHttpResponse)
     * @see ResponseSpec#toEntity(Class)
     * @see ResponseSpec#toEntity(ParameterizedTypeReference)
     * @since 5.0
     */
    default Builder ignoreStatus() {
      return ignoreStatus(true);
    }

    /**
     * Ignore {@link ResponseErrorHandler status handler} throws exception when
     * using {@link ResponseSpec#toEntity(Class) ResponseEntity result}
     *
     * @param ignoreStatus ignore or not
     * @return this builder
     * @see HttpStatusCodeException
     * @see ResponseErrorHandler#hasError(ClientHttpResponse)
     * @see ResponseSpec#toEntity(Class)
     * @see ResponseSpec#toEntity(ParameterizedTypeReference)
     * @since 5.0
     */
    Builder ignoreStatus(boolean ignoreStatus);

    /**
     * Detect empty message body
     *
     * @param detectEmptyBody detect empty message body
     * @return this builder
     * @see IntrospectingClientHttpResponse#hasMessageBody()
     * @since 5.0
     */
    Builder detectEmptyMessageBody(boolean detectEmptyBody);

    /**
     * Add the given request interceptor to the end of the interceptor chain.
     *
     * @param interceptor the interceptor to be added to the chain
     * @return this builder
     */
    Builder requestInterceptor(ClientHttpRequestInterceptor interceptor);

    /**
     * Manipulate the interceptors with the given consumer. The list provided to
     * the consumer is "live", so that the consumer can be used to remove
     * interceptors, change ordering, etc.
     *
     * @param interceptorsConsumer a function that consumes the interceptors list
     * @return this builder
     */
    Builder requestInterceptors(Consumer<List<ClientHttpRequestInterceptor>> interceptorsConsumer);

    /**
     * Enable buffering of request and response, aggregating all content before
     * it is sent, and making it possible to read the response body repeatedly.
     *
     * @param predicate to determine whether to buffer for the given request
     * @return this builder
     * @since 5.0
     */
    Builder bufferContent(Predicate<HttpRequest> predicate);

    /**
     * Add the given request initializer to the end of the initializer chain.
     *
     * @param initializer the initializer to be added to the chain
     * @return this builder
     */
    Builder requestInitializer(ClientHttpRequestInitializer initializer);

    /**
     * Manipulate the initializers with the given consumer. The list provided to
     * the consumer is "live", so that the consumer can be used to remove
     * initializers, change ordering, etc.
     *
     * @param initializersConsumer a function that consumes the initializers list
     * @return this builder
     */
    Builder requestInitializers(Consumer<List<ClientHttpRequestInitializer>> initializersConsumer);

    /**
     * Configure the {@link ClientHttpRequestFactory} to use. This is useful
     * for plugging in and/or customizing options of the underlying HTTP
     * client library (e.g. SSL).
     * <p>If no request factory is specified, {@code RestClient} uses
     * {@linkplain infra.http.client.HttpComponentsClientHttpRequestFactory Apache Http Client},
     * if available on the classpath, and defaults to the
     * {@linkplain infra.http.client.JdkClientHttpRequestFactory JDK HttpClient}
     * if the {@code java.net.http} module is loaded
     *
     * @param requestFactory the request factory to use
     * @return this builder
     */
    Builder requestFactory(ClientHttpRequestFactory requestFactory);

    /**
     * Configure the message converters for the {@code RestClient} to use.
     *
     * @param configurer the configurer to apply
     * @return this builder
     */
    Builder messageConverters(Consumer<List<HttpMessageConverter<?>>> configurer);

    /**
     * Set the message converters for the {@code RestClient} to use.
     *
     * @param messageConverters the list of {@link HttpMessageConverter} to use
     * @return this builder
     * @see #messageConverters(Consumer)
     * @since 5.0
     */
    Builder messageConverters(List<HttpMessageConverter<?>> messageConverters);

    /**
     * Apply the given {@code Consumer} to this builder instance.
     * <p>This can be useful for applying pre-packaged customizations.
     *
     * @param builderConsumer the consumer to apply
     * @return this builder
     */
    Builder apply(Consumer<Builder> builderConsumer);

    /**
     * Clone this {@code RestClient.Builder}.
     */
    Builder clone();

    /**
     * Build the {@link RestClient} instance.
     */
    RestClient build();
  }

  /**
   * Contract for specifying the URI for a request.
   *
   * @param <S> a self reference to the spec type
   */
  interface UriSpec<S extends RequestHeadersSpec<?>> {

    /**
     * Specify the URI using a fully constructed {@link URI}.
     * <p>If the given URI is absolute, it is used as given. If it is
     * a relative URI, the {@link UriBuilderFactory} configured for
     * the client (e.g. with a base URI) will be used to
     * {@linkplain URI#resolve(URI) resolve} the given URI against.
     */
    S uri(URI uri);

    /**
     * Specify the URI for the request using a URI template and URI variables.
     * <p>If a {@link UriBuilderFactory} was configured for the client (e.g.
     * with a base URI) it will be used to expand the URI template.
     */
    S uri(String uri, Object... uriVariables);

    /**
     * Specify the URI for the request using a URI template and URI variables.
     * <p>If a {@link UriBuilderFactory} was configured for the client (e.g.
     * with a base URI) it will be used to expand the URI template.
     */
    S uri(String uri, Map<String, ?> uriVariables);

    /**
     * Specify the URI starting with a URI template and finishing off with a
     * {@link UriBuilder} created from the template.
     */
    S uri(String uri, Function<UriBuilder, URI> uriFunction);

    /**
     * Specify the URI by through a {@link UriBuilder}.
     *
     * @see #uri(String, Function)
     */
    S uri(Function<UriBuilder, URI> uriFunction);
  }

  /**
   * Contract for specifying request headers leading up to the exchange.
   *
   * @param <S> a self reference to the spec type
   */
  interface RequestHeadersSpec<S extends RequestHeadersSpec<S>> {

    /**
     * Set the list of acceptable {@linkplain MediaType media types}, as
     * specified by the {@code Accept} header.
     *
     * @param acceptableMediaTypes the acceptable media types
     * @return this builder
     */
    S accept(MediaType... acceptableMediaTypes);

    /**
     * Set the list of acceptable {@linkplain Charset charsets}, as specified
     * by the {@code Accept-Charset} header.
     *
     * @param acceptableCharsets the acceptable charsets
     * @return this builder
     */
    S acceptCharset(Charset... acceptableCharsets);

    /**
     * Set the value of the {@code If-Modified-Since} header.
     * <p>The date should be specified as the number of milliseconds since
     * January 1, 1970 GMT.
     *
     * @param ifModifiedSince the new value of the header
     * @return this builder
     */
    S ifModifiedSince(ZonedDateTime ifModifiedSince);

    /**
     * Set the values of the {@code If-None-Match} header.
     *
     * @param ifNoneMatches the new value of the header
     * @return this builder
     */
    S ifNoneMatch(String... ifNoneMatches);

    /**
     * Add the given, single header value under the given name.
     *
     * @param headerName the header name
     * @param headerValues the header value(s)
     * @return this builder
     */
    S header(String headerName, String... headerValues);

    /**
     * Provides access to every header declared so far with the possibility
     * to add, replace, or remove values.
     *
     * @param headersConsumer the consumer to provide access to
     * @return this builder
     */
    S headers(Consumer<HttpHeaders> headersConsumer);

    /**
     * Add the given HttpHeaders.
     * <p>
     * Duplicate key-value pairs will be overwritten
     *
     * @param headers the headers
     * @return this builder
     * @see MultiValueMap#setAll(Map)
     * @since 5.0
     */
    S headers(@Nullable HttpHeaders headers);

    /**
     * Add a cookie with the given name and value.
     *
     * @param name the cookie name
     * @param value the cookie value
     * @return this builder
     * @since 5.0
     */
    S cookie(String name, String value);

    /**
     * Provides access to every cookie declared so far with the possibility
     * to add, replace, or remove values.
     *
     * @param cookiesConsumer the consumer to provide access to
     * @return this builder
     * @since 5.0
     */
    S cookies(Consumer<MultiValueMap<String, String>> cookiesConsumer);

    /**
     * Add cookies with the given map.
     * <p>
     * Duplicate key-value pairs will be overwritten
     *
     * @param cookies the cookies map
     * @since 5.0
     */
    S cookies(MultiValueMap<String, String> cookies);

    /**
     * Set an API version for the request. The version is inserted into the
     * request by the {@link Builder#apiVersionInserter(ApiVersionInserter)
     * configured} {@code ApiVersionInserter}.
     *
     * @param version the API version of the request; this can be a String or
     * some Object that can be formatted the inserter, e.g. through an
     * {@link ApiVersionFormatter}.
     * @since 5.0
     */
    S apiVersion(@Nullable Object version);

    /**
     * Set the attribute with the given name to the given value.
     *
     * @param name the name of the attribute to add
     * @param value the value of the attribute to add
     * @return this builder
     * @since 5.0
     */
    S attribute(String name, Object value);

    /**
     * Provides access to every attribute declared so far with the
     * possibility to add, replace, or remove values.
     *
     * @param attributesConsumer the consumer to provide access to
     * @return this builder
     * @since 5.0
     */
    S attributes(Consumer<Map<String, Object>> attributesConsumer);

    /**
     * Add the attributes with the given name to the given value.
     *
     * @param attributes the attributes to add
     * @return this builder
     * @see Map#putAll(Map)
     * @since 5.0
     */
    S attributes(@Nullable Map<String, Object> attributes);

    /**
     * Callback for access to the {@link ClientHttpRequest} that in turn
     * provides access to the native request of the underlying HTTP library.
     * <p>This could be useful for setting advanced, per-request options that
     * are exposed by the underlying library.
     *
     * @param requestConsumer a consumer to access the
     * {@code ClientHttpRequest} with
     * @return this builder
     */
    S httpRequest(Consumer<ClientHttpRequest> requestConsumer);

    /**
     * Proceed to declare how to extract the response. For example to extract
     * a {@link ResponseEntity} with status, headers, and body:
     * <pre>{@code
     * ResponseEntity<Person> entity = client.get()
     *     .uri("/persons/1")
     *     .accept(MediaType.APPLICATION_JSON)
     *     .retrieve()
     *     .toEntity(Person.class);
     * }</pre>
     * <p>Or if interested only in the body:
     * <pre>{@code
     * Person person = client.get()
     *     .uri("/persons/1")
     *     .accept(MediaType.APPLICATION_JSON)
     *     .retrieve()
     *     .body(Person.class);
     * }</pre>
     * <p>By default, 4xx response code result in a
     * {@link HttpClientErrorException} and 5xx response codes in a
     * {@link HttpServerErrorException}. To customize error handling, use
     * {@link ResponseSpec#onStatus(Predicate, ErrorHandler) onStatus} handlers.
     *
     * @return {@code ResponseSpec} to specify how to decode the body
     */
    @CheckReturnValue
    ResponseSpec retrieve();

    /**
     * Proceed to declare how to extract the response. For example to extract
     * a {@link ResponseEntity} with status, headers, and body:
     * <pre>{@code
     * Future<ResponseEntity<Person>> entity = client.get()
     *     .uri("/persons/1")
     *     .accept(MediaType.APPLICATION_JSON)
     *     .async()
     *     .toEntity(Person.class);
     * }</pre>
     * <p>Or if interested only in the body:
     * <pre>{@code
     * Future<Person> person = client.get()
     *     .uri("/persons/1")
     *     .accept(MediaType.APPLICATION_JSON)
     *     .async()
     *     .body(Person.class);
     * }</pre>
     * <p>By default, 4xx response code result in a
     * {@link HttpClientErrorException} and 5xx response codes in a
     * {@link HttpServerErrorException}. To customize error handling, use
     * {@link ResponseSpec#onStatus(Predicate, ErrorHandler) onStatus} handlers.
     *
     * @return {@code AsyncSpec} to specify how to decode the body
     * @see ClientHttpRequest#async(Executor)
     * @since 5.0
     */
    default AsyncSpec async() {
      return async(null);
    }

    /**
     * Proceed to declare how to extract the response. For example to extract
     * a {@link ResponseEntity} with status, headers, and body:
     * <pre>{@code
     * Future<ResponseEntity<Person>> entity = client.get()
     *     .uri("/persons/1")
     *     .accept(MediaType.APPLICATION_JSON)
     *     .async()
     *     .toEntity(Person.class);
     * }</pre>
     * <p>Or if interested only in the body:
     * <pre>{@code
     * Future<Person> person = client.get()
     *     .uri("/persons/1")
     *     .accept(MediaType.APPLICATION_JSON)
     *     .async()
     *     .body(Person.class);
     * }</pre>
     * <p>By default, 4xx response code result in a
     * {@link HttpClientErrorException} and 5xx response codes in a
     * {@link HttpServerErrorException}. To customize error handling, use
     * {@link ResponseSpec#onStatus(Predicate, ErrorHandler) onStatus} handlers.
     *
     * @return {@code AsyncSpec} to specify how to decode the body
     * @see ClientHttpRequest#async(Executor)
     * @since 5.0
     */
    AsyncSpec async(@Nullable Executor executor);

    /**
     * Send the HTTP request:
     * <pre>{@code
     * client.delete()
     *     .uri("/persons/1")
     *     .send();
     * }</pre>
     * <p>Or if interested only in the body:
     * <pre>{@code
     *  client.put()
     *     .uri("/persons/1")
     *     .body(persons)
     *     .send();
     * }</pre>
     * <p> The returned future completes exceptionally with:
     * <ul>
     * <li> By default, 4xx response code result in a
     * {@link HttpClientErrorException} and 5xx response codes in a
     * {@link HttpServerErrorException}. To customize error handling, use
     * {@link AsyncSpec#onStatus(Predicate, ErrorHandler) onStatus} handlers.
     * </li>
     * </ul>
     *
     * @return the Future Response
     * @see ClientHttpRequest#async(Executor)
     * @since 5.0
     */
    default Future<ClientResponse> send() {
      return send(null);
    }

    /**
     * Send the HTTP request:
     * <pre>{@code
     * client.delete()
     *     .uri("/persons/1")
     *     .send();
     * }</pre>
     * <p>Or if interested only in the body:
     * <pre>{@code
     *  client.put()
     *     .uri("/persons/1")
     *     .body(persons)
     *     .send();
     * }</pre>
     * <p> The returned future completes exceptionally with:
     * <ul>
     * <li> By default, 4xx response code result in a
     * {@link HttpClientErrorException} and 5xx response codes in a
     * {@link HttpServerErrorException}. To customize error handling, use
     * {@link AsyncSpec#onStatus(Predicate, ErrorHandler) onStatus} handlers.
     * </li>
     * </ul>
     *
     * @return the Future Response
     * @see ClientHttpRequest#async(Executor)
     * @since 5.0
     */
    Future<ClientResponse> send(@Nullable Executor executor);

    /**
     * Execute the HTTP request:
     * <pre>{@code
     * client.delete()
     *     .uri("/persons/1")
     *     .execute();
     * }</pre>
     * <p>Or if interested only in the body:
     * <pre>{@code
     *  client.put()
     *     .uri("/persons/1")
     *     .body(persons)
     *     .execute();
     * }</pre>
     * <p>By default, 4xx response code result in a
     * {@link HttpClientErrorException} and 5xx response codes in a
     * {@link HttpServerErrorException}. To customize error handling, use
     * {@link ResponseSpec#onStatus(Predicate, ErrorHandler) onStatus} handlers.
     * <p> This ClientHttpResponse body is closed
     */
    default ClientResponse execute() {
      return execute(true);
    }

    /**
     * Execute the HTTP request:
     * <pre>{@code
     * client.delete()
     *     .uri("/persons/1")
     *     .execute();
     * }</pre>
     * <p>Or if interested only in the body:
     * <pre>{@code
     *  client.put()
     *     .uri("/persons/1")
     *     .body(persons)
     *     .execute();
     * }</pre>
     * <p>By default, 4xx response code result in a
     * {@link HttpClientErrorException} and 5xx response codes in a
     * {@link HttpServerErrorException}. To customize error handling, use
     * {@link ResponseSpec#onStatus(Predicate, ErrorHandler) onStatus} handlers.
     *
     * @param close {@code true} to close the response
     */
    ClientResponse execute(boolean close);

    /**
     * Exchange the {@link ClientHttpResponse} for a type {@code T}. This
     * can be useful for advanced scenarios, for example to decode the
     * response differently depending on the response status:
     * <pre>{@code
     * Person person = client.get()
     *     .uri("/people/1")
     *     .accept(MediaType.APPLICATION_JSON)
     *     .exchange((request, response) -> {
     *         if (response.getStatusCode().equals(HttpStatus.OK)) {
     *             return deserialize(response.getBody());
     *         }
     *         else {
     *             throw new BusinessException();
     *         }
     *     });
     * }</pre>
     * <p><strong>Note:</strong> The response is
     * {@linkplain ClientHttpResponse#close() closed} after the exchange
     * function has been invoked.
     *
     * @param exchangeFunction the function to handle the response with
     * @param <T> the type the response will be transformed to
     * @return the value returned from the exchange function
     */
    default <T> T exchange(ExchangeFunction<T> exchangeFunction) {
      return exchange(exchangeFunction, true);
    }

    /**
     * Exchange the {@link ClientHttpResponse} for a type {@code T}. This
     * can be useful for advanced scenarios, for example to decode the
     * response differently depending on the response status:
     * <pre>{@code
     * Person person = client.get()
     *     .uri("/people/1")
     *     .accept(MediaType.APPLICATION_JSON)
     *     .exchange((request, response) -> {
     *         if (response.getStatusCode().equals(HttpStatus.OK)) {
     *             return deserialize(response.getBody());
     *         }
     *         else {
     *             throw new BusinessException();
     *         }
     *     });
     * }</pre>
     * <p><strong>Note:</strong> If {@code close} is {@code true},
     * then the response is {@linkplain ClientHttpResponse#close() closed}
     * after the exchange function has been invoked. When set to
     * {@code false}, the caller is responsible for closing the response.
     *
     * @param exchangeFunction the function to handle the response with
     * @param close {@code true} to close the response after
     * {@code exchangeFunction} is invoked, {@code false} to keep it open
     * @param <T> the type the response will be transformed to
     * @return the value returned from the exchange function
     */
    <T> T exchange(ExchangeFunction<T> exchangeFunction, boolean close);

    /**
     * Defines the contract for {@link #exchange(ExchangeFunction)}.
     *
     * @param <T> the type the response will be transformed to
     */
    @FunctionalInterface
    interface ExchangeFunction<T> {

      /**
       * Exchange the given response into a type {@code T}.
       *
       * @param clientRequest the request
       * @param clientResponse the response
       * @return the exchanged type
       * @throws IOException in case of I/O errors
       */
      T exchange(HttpRequest clientRequest, ClientResponse clientResponse) throws IOException;

    }

  }

  /**
   * Contract for specifying request headers and body leading up to the exchange.
   */
  interface RequestBodySpec extends RequestHeadersSpec<RequestBodySpec> {

    /**
     * Set the length of the body in bytes, as specified by the
     * {@code Content-Length} header.
     *
     * @param contentLength the content length
     * @return this builder
     * @see HttpHeaders#setContentLength(long)
     */
    RequestBodySpec contentLength(long contentLength);

    /**
     * Set the {@linkplain MediaType media type} of the body, as specified
     * by the {@code Content-Type} header.
     *
     * @param contentType the content type
     * @return this builder
     * @see HttpHeaders#setContentType(MediaType)
     */
    RequestBodySpec contentType(MediaType contentType);

    /**
     * Set the body of the request to the given {@code Object}.
     * For example:
     * <pre>{@code
     * Person person = ... ;
     * ResponseEntity<Void> response = client.post()
     *     .uri("/persons/{id}", id)
     *     .contentType(MediaType.APPLICATION_JSON)
     *     .body(person)
     *     .retrieve()
     *     .toBodilessEntity();
     * }</pre>
     *
     * @param body the body of the response
     * @return the built response
     */
    RequestBodySpec body(Object body);

    /**
     * Set the body of the response to the given {@code Object}. The parameter
     * {@code bodyType} is used to capture the generic type.
     *
     * @param body the body of the response
     * @param bodyType the type of the body, used to capture the generic type
     * @return the built response
     */
    <T> RequestBodySpec body(T body, ParameterizedTypeReference<T> bodyType);

    /**
     * Set the body of the response to the given function that writes to
     * an {@link OutputStream}.
     *
     * @param body a function that takes an {@code OutputStream} and can
     * throw an {@code IOException}
     * @return the built response
     */
    RequestBodySpec body(StreamingHttpOutputMessage.Body body);

  }

  /**
   * Contract for specifying response operations following the exchange.
   */
  interface ResponseSpec {

    /**
     * Provide a function to map specific error status codes to an error handler.
     * <p>By default, if there are no matching status handlers, responses with
     * status codes &gt;= 400 wil throw a {@link RestClientResponseException}.
     * <p>Note that {@link IOException IOExceptions},
     * {@link java.io.UncheckedIOException UncheckedIOExceptions}, and
     * {@link infra.http.converter.HttpMessageNotReadableException HttpMessageNotReadableExceptions}
     * thrown from {@code errorHandler} will be wrapped in a
     * {@link RestClientException}.
     *
     * @param statusPredicate to match responses with
     * @param errorHandler handler that typically, though not necessarily,
     * throws an exception
     * @return this builder
     */
    ResponseSpec onStatus(Predicate<HttpStatusCode> statusPredicate, ErrorHandler errorHandler);

    /**
     * Provide a function to map specific error status codes to an error handler.
     * <p>By default, if there are no matching status handlers, responses with
     * status codes &gt;= 400 wil throw a {@link RestClientResponseException}.
     * <p>Note that {@link IOException IOExceptions},
     * {@link java.io.UncheckedIOException UncheckedIOExceptions}, and
     * {@link infra.http.converter.HttpMessageNotReadableException HttpMessageNotReadableExceptions}
     * thrown from {@code errorHandler} will be wrapped in a
     * {@link RestClientException}.
     *
     * @param errorHandler the error handler
     * @return this builder
     */
    ResponseSpec onStatus(ResponseErrorHandler errorHandler);

    /**
     * Ignore {@link ResponseErrorHandler status handler} throws exception when
     * using {@link #toEntity(Class) ResponseEntity result}
     *
     * @return this builder
     * @see HttpStatusCodeException
     * @see ResponseErrorHandler#hasError(ClientHttpResponse)
     * @see #toEntity(Class)
     * @see #toEntity(ParameterizedTypeReference)
     * @see #toBodilessEntity()
     * @since 5.0
     */
    default ResponseSpec ignoreStatus() {
      return ignoreStatus(true);
    }

    /**
     * Ignore {@link ResponseErrorHandler status handler} throws exception when
     * using {@link #toEntity(Class) ResponseEntity result}
     *
     * @param ignoreStatus ignore or not
     * @return this builder
     * @see HttpStatusCodeException
     * @see ResponseErrorHandler#hasError(ClientHttpResponse)
     * @see #toEntity(Class)
     * @see #toEntity(ParameterizedTypeReference)
     * @see #toBodilessEntity()
     * @since 5.0
     */
    ResponseSpec ignoreStatus(boolean ignoreStatus);

    /**
     * Extract the body as an object of the given type.
     *
     * @param bodyType the type of return value
     * @param <T> the body type
     * @return the body, or {@code null} if no response body was available
     * @throws RestClientResponseException by default when receiving a
     * response with a status code of 4xx or 5xx. Use
     * {@link #onStatus(Predicate, ErrorHandler)} to customize error response
     * handling.
     */
    @Nullable
    <T> T body(Class<T> bodyType);

    /**
     * Extract the body as an object of the given type.
     *
     * @param bodyType the type of return value
     * @param <T> the body type
     * @return the body, or {@code null} if no response body was available
     * @throws RestClientResponseException by default when receiving a
     * response with a status code of 4xx or 5xx. Use
     * {@link #onStatus(Predicate, ErrorHandler)} to customize error response
     * handling.
     */
    @Nullable
    <T> T body(ParameterizedTypeReference<T> bodyType);

    /**
     * Return a {@code ResponseEntity} with the body decoded to an Object of
     * the given type.
     *
     * @param bodyType the expected response body type
     * @param <T> response body type
     * @return the {@code ResponseEntity} with the decoded body
     * @throws RestClientResponseException by default when receiving a
     * response with a status code of 4xx or 5xx. Use
     * {@link #onStatus(Predicate, ErrorHandler)} to customize error response
     * handling.
     */
    <T> ResponseEntity<T> toEntity(Class<T> bodyType);

    /**
     * Return a {@code ResponseEntity} with the body decoded to an Object of
     * the given type.
     *
     * @param bodyType the expected response body type
     * @param <T> response body type
     * @return the {@code ResponseEntity} with the decoded body
     * @throws RestClientResponseException by default when receiving a
     * response with a status code of 4xx or 5xx. Use
     * {@link #onStatus(Predicate, ErrorHandler)} to customize error response
     * handling.
     */
    <T> ResponseEntity<T> toEntity(ParameterizedTypeReference<T> bodyType);

    /**
     * Return a {@code ResponseEntity} without a body.
     *
     * @return the {@code ResponseEntity}
     * @throws RestClientResponseException by default when receiving a
     * response with a status code of 4xx or 5xx. Use
     * {@link #onStatus(Predicate, ErrorHandler)} to customize error response
     * handling.
     */
    ResponseEntity<Void> toBodilessEntity();

    /**
     * Execute without a body.
     * <p>
     * This API is like {@link #toBodilessEntity}
     *
     * @throws RestClientResponseException by default when receiving a
     * response with a status code of 4xx or 5xx. Use
     * {@link #onStatus(Predicate, ErrorHandler)} to customize error response
     * handling.
     * @see #toBodilessEntity()
     * @since 5.0
     */
    void toBodiless() throws RestClientException;

  }

  /**
   * Contract for specifying response operations following the exchange.
   */
  interface AsyncSpec {

    /**
     * Provide a function to map specific error status codes to an error handler.
     * <p>By default, if there are no matching status handlers, responses with
     * status codes &gt;= 400 wil throw a {@link RestClientResponseException}.
     * <p>Note that {@link IOException IOExceptions},
     * {@link java.io.UncheckedIOException UncheckedIOExceptions}, and
     * {@link infra.http.converter.HttpMessageNotReadableException HttpMessageNotReadableExceptions}
     * thrown from {@code errorHandler} will be wrapped in a
     * {@link RestClientException}.
     *
     * @param statusPredicate to match responses with
     * @param errorHandler handler that typically, though not necessarily, throws an exception
     * @return this builder
     */
    AsyncSpec onStatus(Predicate<HttpStatusCode> statusPredicate, ErrorHandler errorHandler);

    /**
     * Provide a function to map specific error status codes to an error handler.
     * <p>By default, if there are no matching status handlers, responses with
     * status codes &gt;= 400 wil throw a {@link RestClientResponseException}.
     * <p>Note that {@link IOException IOExceptions},
     * {@link java.io.UncheckedIOException UncheckedIOExceptions}, and
     * {@link infra.http.converter.HttpMessageNotReadableException HttpMessageNotReadableExceptions}
     * thrown from {@code errorHandler} will be wrapped in a
     * {@link RestClientException}.
     *
     * @param errorHandler the error handler
     * @return this builder
     */
    AsyncSpec onStatus(ResponseErrorHandler errorHandler);

    /**
     * Ignore {@link ResponseErrorHandler status handler} throws exception when
     * using {@link #toEntity(Class) ResponseEntity result}
     *
     * @return this builder
     * @see HttpStatusCodeException
     * @see ResponseErrorHandler#hasError(ClientHttpResponse)
     * @see #toEntity(Class)
     * @see #toEntity(ParameterizedTypeReference)
     * @see #toBodilessEntity()
     * @since 5.0
     */
    default AsyncSpec ignoreStatus() {
      return ignoreStatus(true);
    }

    /**
     * Ignore {@link ResponseErrorHandler status handler} throws exception when
     * using {@link #toEntity(Class) ResponseEntity result}
     *
     * @param ignoreStatus ignore or not
     * @return this builder
     * @see HttpStatusCodeException
     * @see ResponseErrorHandler#hasError(ClientHttpResponse)
     * @see #toEntity(Class)
     * @see #toEntity(ParameterizedTypeReference)
     * @see #toBodilessEntity()
     * @since 5.0
     */
    AsyncSpec ignoreStatus(boolean ignoreStatus);

    /**
     * Extract the body as an object of the given type.
     *
     * <p> The returned future completes exceptionally with:
     * <ul>
     * <li>{@link RestClientResponseException} - by default when receiving a
     * response with a status code of 4xx or 5xx. Use
     * {@link #onStatus(Predicate, ErrorHandler)} to customize error response
     * handling.
     *
     * </li>
     * </ul>
     *
     * <p>
     * NOT Fully async {@link ClientHttpResponse#getBody()}
     *
     * @param bodyType the type of return value
     * @param <T> the body type
     * @return the Future body
     */
    <T> Future<T> body(Class<T> bodyType);

    /**
     * Extract the body as an object of the given type.
     *
     * <p> The returned future completes exceptionally with:
     * <ul>
     * <li>{@link RestClientResponseException} - by default when receiving a
     * response with a status code of 4xx or 5xx. Use
     * {@link #onStatus(Predicate, ErrorHandler)} to customize error response
     * handling.
     * </li>
     * </ul>
     *
     * <p>
     * NOT Fully async {@link ClientHttpResponse#getBody()}
     *
     * @param bodyType the type of return value
     * @param <T> the body type
     * @return the Future body
     */
    <T> Future<T> body(ParameterizedTypeReference<T> bodyType);

    /**
     * Return a {@code ResponseEntity} with the body decoded to an Object of
     * the given type.
     *
     * <p> The returned future completes exceptionally with:
     * <ul>
     * <li>{@link RestClientResponseException} - by default when receiving a
     * response with a status code of 4xx or 5xx. Use
     * {@link #onStatus(Predicate, ErrorHandler)} to customize error response
     * handling.
     * </li>
     * </ul>
     *
     * @param bodyType the expected response body type
     * @param <T> response body type
     * @return the {@code Future<ResponseEntity>} with the decoded body
     * @see #onStatus(ResponseErrorHandler)
     */
    <T> Future<ResponseEntity<T>> toEntity(Class<T> bodyType);

    /**
     * Return a {@code ResponseEntity} with the body decoded to an Object of
     * the given type.
     *
     * <p> The returned future completes exceptionally with:
     * <ul>
     * <li>{@link RestClientResponseException} - by default when receiving a
     * response with a status code of 4xx or 5xx. Use
     * {@link #onStatus(Predicate, ErrorHandler)} to customize error response
     * handling.
     * </li>
     * </ul>
     *
     * @param bodyType the expected response body type
     * @param <T> response body type
     * @return the {@code Future<ResponseEntity>} with the decoded body
     * @see #onStatus(ResponseErrorHandler)
     */
    <T> Future<ResponseEntity<T>> toEntity(ParameterizedTypeReference<T> bodyType);

    /**
     * Return a {@code ResponseEntity} without a body.
     *
     * <p> The returned future completes exceptionally with:
     * <ul>
     * <li>{@link RestClientResponseException} - by default when receiving a
     * response with a status code of 4xx or 5xx. Use
     * {@link #onStatus(Predicate, ErrorHandler)} to customize error response
     * handling.
     * </li>
     * </ul>
     *
     * @return the {@code Future<ResponseEntity>}
     */
    Future<ResponseEntity<Void>> toBodilessEntity();

    /**
     * Return an async result without a body.
     * <p>
     * This API is like {@link #toBodilessEntity}
     *
     * <p> The returned future completes exceptionally with:
     * <ul>
     * <li>{@link RestClientResponseException} - by default when receiving a
     * response with a status code of 4xx or 5xx. Use
     * {@link #onStatus(Predicate, ErrorHandler)} to customize error response
     * handling.
     * </li>
     * </ul>
     *
     * @return {@code Future<Void>}
     * @see #toBodilessEntity()
     * @since 5.0
     */
    Future<Void> toBodiless();

  }

  /**
   * Used in {@link ResponseSpec#onStatus(Predicate, ErrorHandler)}
   * and {@link AsyncSpec#onStatus(Predicate, ErrorHandler)}.
   */
  @FunctionalInterface
  interface ErrorHandler {

    /**
     * Handle the error in the given response.
     *
     * @param response the response with the error
     * @throws IOException in case of I/O errors
     */
    void handle(HttpRequest request, ClientHttpResponse response) throws IOException;

  }

  /**
   * Contract for specifying request headers and URI for a request.
   *
   * @param <S> a self reference to the spec type
   */
  interface RequestHeadersUriSpec<S extends RequestHeadersSpec<S>> extends UriSpec<S>, RequestHeadersSpec<S> {

  }

  /**
   * Contract for specifying request headers, body and URI for a request.
   */
  interface RequestBodyUriSpec extends RequestBodySpec, RequestHeadersUriSpec<RequestBodySpec> {

  }

}
