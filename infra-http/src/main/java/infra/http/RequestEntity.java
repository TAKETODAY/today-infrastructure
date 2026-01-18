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

package infra.http;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import infra.util.CollectionUtils;
import infra.util.DataSize;
import infra.util.MultiValueMap;
import infra.util.ObjectUtils;

/**
 * Extension of {@link HttpEntity} that also exposes the HTTP method and the
 * target URL. For use in the {@code RestTemplate} to prepare requests with
 * and in {@code @Controller} methods to represent request input.
 *
 * <p>Example use with the {@code RestTemplate}:
 * <pre>{@code
 * MyRequest body = ...
 * RequestEntity<MyRequest> request = RequestEntity
 *     .post("https://example.com/{foo}", "bar")
 *     .accept(MediaType.APPLICATION_JSON)
 *     .body(body);
 * ResponseEntity<MyResponse> response = template.exchange(request, MyResponse.class);
 * }</pre>
 *
 * <p>Example use in an {@code @Controller}:
 * <pre>{@code
 * @RequestMapping("/handle")
 * public void handle(RequestEntity<String> request) {
 *   HttpMethod method = request.getMethod();
 *   URI url = request.getUrl();
 *   String body = request.getBody();
 * }
 * }</pre>
 *
 * @param <T> the body type
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Parviz Rozikov
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #getMethod()
 * @see #getURI()
 * @see infra.web.client.RestOperations#exchange(RequestEntity, Class)
 * @see ResponseEntity
 * @since 4.0 2021/11/5 21:46
 */
public class RequestEntity<T extends @Nullable Object> extends HttpEntity<T> {

  private final @Nullable HttpMethod method;

  private final @Nullable URI uri;

  private final @Nullable Type type;

  /**
   * Constructor with method and URL but without body nor headers.
   *
   * @param method the method
   * @param uri the URL
   */
  public RequestEntity(HttpMethod method, URI uri) {
    this(null, null, method, uri, null);
  }

  /**
   * Constructor with method, URL and body but without headers.
   *
   * @param body the body
   * @param method the method
   * @param uri the URL
   */
  public RequestEntity(@Nullable T body, HttpMethod method, URI uri) {
    this(body, null, method, uri, null);
  }

  /**
   * Constructor with method, URL, body and type but without headers.
   *
   * @param body the body
   * @param method the method
   * @param uri the URL
   * @param type the type used for generic type resolution
   */
  public RequestEntity(@Nullable T body, HttpMethod method, URI uri, Type type) {
    this(body, null, method, uri, type);
  }

  /**
   * Constructor with method, URL and headers but without body.
   *
   * @param headers the headers
   * @param method the method
   * @param uri the URL
   */
  public RequestEntity(MultiValueMap<String, String> headers, HttpMethod method, URI uri) {
    this(null, headers, method, uri, null);
  }

  /**
   * Constructor with method, URL, headers and body.
   *
   * @param body the body
   * @param headers the headers
   * @param method the method
   * @param uri the URL
   */
  public RequestEntity(@Nullable T body, @Nullable MultiValueMap<String, String> headers, @Nullable HttpMethod method, URI uri) {
    this(body, headers, method, uri, null);
  }

  /**
   * Constructor with method, URL, headers, body and type.
   *
   * @param body the body
   * @param headers the headers
   * @param method the method
   * @param uri the URL
   * @param type the type used for generic type resolution
   */
  public RequestEntity(@Nullable T body, @Nullable MultiValueMap<String, String> headers,
          @Nullable HttpMethod method, @Nullable URI uri, @Nullable Type type) {
    super(body, headers);
    this.method = method;
    this.uri = uri;
    this.type = type;
  }

  /**
   * Return the HTTP method of the request.
   *
   * @return the HTTP method as an {@code HttpMethod} enum value
   */
  public @Nullable HttpMethod getMethod() {
    return method;
  }

  /**
   * Return the {@link URI} for the target HTTP endpoint.
   * <p><strong>Note:</strong> This method raises
   * {@link UnsupportedOperationException} if the {@code RequestEntity} was
   * created with a URI template and variables rather than with a {@link URI}
   * instance. This is because a URI cannot be created without further input
   * on how to expand template and encode the URI. In such cases, the
   * {@code URI} is prepared by the
   * {@link infra.web.client.RestTemplate} with the help of the
   * {@link infra.web.util.UriTemplateHandler} it is configured with.
   */
  public URI getURI() {
    if (this.uri == null) {
      throw new UnsupportedOperationException(
              "The RequestEntity was created with a URI template and variables, " +
                      "and there is not enough information on how to correctly expand and " +
                      "encode the URI template. This will be done by the RestTemplate instead " +
                      "with help from the UriTemplateHandler it is configured with.");
    }
    return this.uri;
  }

  /**
   * Return the type of the request's body.
   *
   * @return the request's body type, or {@code null} if not known
   */
  public @Nullable Type getType() {
    if (this.type == null) {
      T body = getBody();
      if (body != null) {
        return body.getClass();
      }
    }
    return this.type;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!super.equals(other)) {
      return false;
    }
    RequestEntity<?> otherEntity = (RequestEntity<?>) other;
    return Objects.equals(this.method, otherEntity.method)
            && Objects.equals(this.uri, otherEntity.uri);
  }

  @Override
  public int hashCode() {
    int hashCode = super.hashCode();
    hashCode = 29 * hashCode + Objects.hashCode(this.method);
    hashCode = 29 * hashCode + Objects.hashCode(this.uri);
    return hashCode;
  }

  @Override
  public String toString() {
    return format(getMethod(), getURI().toString(), getBody(), getHeaders());
  }

  static <T> String format(@Nullable HttpMethod httpMethod,
          String url, @Nullable T body, @Nullable HttpHeaders headers) {
    StringBuilder builder = new StringBuilder("<");
    builder.append(httpMethod);
    builder.append(' ');
    builder.append(url);
    builder.append(',');
    if (body != null) {
      builder.append(body);
      builder.append(',');
    }
    builder.append(toString(headers));
    builder.append('>');
    return builder.toString();
  }

  // Static builder methods

  /**
   * Create a builder with the given method and url.
   *
   * @param method the HTTP method (GET, POST, etc)
   * @param url the URL
   * @return the created builder
   */
  public static BodyBuilder method(HttpMethod method, URI url) {
    return new DefaultBodyBuilder(method, url);
  }

  /**
   * Create a builder with the given HTTP method, URI template, and variables.
   *
   * @param method the HTTP method (GET, POST, etc)
   * @param uriTemplate the uri template to use
   * @param uriVariables variables to expand the URI template with
   * @return the created builder
   */
  public static BodyBuilder method(HttpMethod method, String uriTemplate, Object... uriVariables) {
    return new DefaultBodyBuilder(method, uriTemplate, uriVariables);
  }

  /**
   * Create a builder with the given HTTP method, URI template, and variables.
   *
   * @param method the HTTP method (GET, POST, etc)
   * @param uriTemplate the uri template to use
   * @return the created builder
   */
  public static BodyBuilder method(HttpMethod method, String uriTemplate, Map<String, ?> uriVariables) {
    return new DefaultBodyBuilder(method, uriTemplate, uriVariables);
  }

  /**
   * Create an HTTP GET builder with the given url.
   *
   * @param url the URL
   * @return the created builder
   */
  public static HeadersBuilder<?> get(URI url) {
    return method(HttpMethod.GET, url);
  }

  /**
   * Create an HTTP GET builder with the given string base uri template.
   *
   * @param uriTemplate the uri template to use
   * @param uriVariables variables to expand the URI template with
   * @return the created builder
   */
  public static HeadersBuilder<?> get(String uriTemplate, Object... uriVariables) {
    return method(HttpMethod.GET, uriTemplate, uriVariables);
  }

  /**
   * Create an HTTP HEAD builder with the given url.
   *
   * @param url the URL
   * @return the created builder
   */
  public static HeadersBuilder<?> head(URI url) {
    return method(HttpMethod.HEAD, url);
  }

  /**
   * Create an HTTP HEAD builder with the given string base uri template.
   *
   * @param uriTemplate the uri template to use
   * @param uriVariables variables to expand the URI template with
   * @return the created builder
   */
  public static HeadersBuilder<?> head(String uriTemplate, Object... uriVariables) {
    return method(HttpMethod.HEAD, uriTemplate, uriVariables);
  }

  /**
   * Create an HTTP POST builder with the given url.
   *
   * @param url the URL
   * @return the created builder
   */
  public static BodyBuilder post(URI url) {
    return method(HttpMethod.POST, url);
  }

  /**
   * Create an HTTP POST builder with the given string base uri template.
   *
   * @param uriTemplate the uri template to use
   * @param uriVariables variables to expand the URI template with
   * @return the created builder
   */
  public static BodyBuilder post(String uriTemplate, Object... uriVariables) {
    return method(HttpMethod.POST, uriTemplate, uriVariables);
  }

  /**
   * Create an HTTP PUT builder with the given url.
   *
   * @param url the URL
   * @return the created builder
   */
  public static BodyBuilder put(URI url) {
    return method(HttpMethod.PUT, url);
  }

  /**
   * Create an HTTP PUT builder with the given string base uri template.
   *
   * @param uriTemplate the uri template to use
   * @param uriVariables variables to expand the URI template with
   * @return the created builder
   */
  public static BodyBuilder put(String uriTemplate, Object... uriVariables) {
    return method(HttpMethod.PUT, uriTemplate, uriVariables);
  }

  /**
   * Create an HTTP PATCH builder with the given url.
   *
   * @param url the URL
   * @return the created builder
   */
  public static BodyBuilder patch(URI url) {
    return method(HttpMethod.PATCH, url);
  }

  /**
   * Create an HTTP PATCH builder with the given string base uri template.
   *
   * @param uriTemplate the uri template to use
   * @param uriVariables variables to expand the URI template with
   * @return the created builder
   */
  public static BodyBuilder patch(String uriTemplate, Object... uriVariables) {
    return method(HttpMethod.PATCH, uriTemplate, uriVariables);
  }

  /**
   * Create an HTTP DELETE builder with the given url.
   *
   * @param url the URL
   * @return the created builder
   */
  public static HeadersBuilder<?> delete(URI url) {
    return method(HttpMethod.DELETE, url);
  }

  /**
   * Create an HTTP DELETE builder with the given string base uri template.
   *
   * @param uriTemplate the uri template to use
   * @param uriVariables variables to expand the URI template with
   * @return the created builder
   */
  public static HeadersBuilder<?> delete(String uriTemplate, Object... uriVariables) {
    return method(HttpMethod.DELETE, uriTemplate, uriVariables);
  }

  /**
   * Creates an HTTP OPTIONS builder with the given url.
   *
   * @param url the URL
   * @return the created builder
   */
  public static HeadersBuilder<?> options(URI url) {
    return method(HttpMethod.OPTIONS, url);
  }

  /**
   * Creates an HTTP OPTIONS builder with the given string base uri template.
   *
   * @param uriTemplate the uri template to use
   * @param uriVariables variables to expand the URI template with
   * @return the created builder
   * @since 4.0
   */
  public static HeadersBuilder<?> options(String uriTemplate, Object... uriVariables) {
    return method(HttpMethod.OPTIONS, uriTemplate, uriVariables);
  }

  /**
   * Defines a builder that adds headers to the request entity.
   *
   * @param <B> the builder subclass
   */
  public interface HeadersBuilder<B extends HeadersBuilder<B>> {

    /**
     * Add the given, single header value under the given name.
     *
     * @param headerName the header name
     * @param headerValues the header value(s)
     * @return this builder
     * @see HttpHeaders#add(String, String)
     */
    B header(String headerName, String... headerValues);

    /**
     * Copy the given headers into the entity's headers map.
     *
     * @param headers the existing HttpHeaders to copy from
     * @return this builder
     * @see HttpHeaders#add(String, String)
     */
    B headers(@Nullable HttpHeaders headers);

    /**
     * Manipulate this entity's headers with the given consumer. The
     * headers provided to the consumer are "live", so that the consumer can be used to
     * {@linkplain HttpHeaders#setOrRemove(String, String) overwrite} existing header values,
     * {@linkplain HttpHeaders#remove(Object) remove} values, or use any of the other
     * {@link HttpHeaders} methods.
     *
     * @param headersConsumer a function that consumes the {@code HttpHeaders}
     * @return this builder
     */
    B headers(Consumer<HttpHeaders> headersConsumer);

    /**
     * Set the list of acceptable {@linkplain MediaType media types}, as
     * specified by the {@code Accept} header.
     *
     * @param acceptableMediaTypes the acceptable media types
     */
    B accept(MediaType... acceptableMediaTypes);

    /**
     * Set the list of acceptable {@linkplain Charset charsets}, as specified
     * by the {@code Accept-Charset} header.
     *
     * @param acceptableCharsets the acceptable charsets
     */
    B acceptCharset(Charset... acceptableCharsets);

    /**
     * Set the value of the {@code If-Modified-Since} header.
     *
     * @param ifModifiedSince the new value of the header
     */
    B ifModifiedSince(ZonedDateTime ifModifiedSince);

    /**
     * Set the value of the {@code If-Modified-Since} header.
     *
     * @param ifModifiedSince the new value of the header
     */
    B ifModifiedSince(Instant ifModifiedSince);

    /**
     * Set the value of the {@code If-Modified-Since} header.
     * <p>The date should be specified as the number of milliseconds since
     * January 1, 1970 GMT.
     *
     * @param ifModifiedSince the new value of the header
     */
    B ifModifiedSince(long ifModifiedSince);

    /**
     * Set the values of the {@code If-None-Match} header.
     *
     * @param ifNoneMatches the new value of the header
     */
    B ifNoneMatch(String... ifNoneMatches);

    /**
     * Builds the request entity with no body.
     *
     * @return the request entity
     * @see BodyBuilder#body(Object)
     */
    RequestEntity<Void> build();
  }

  /**
   * Defines a builder that adds a body to the response entity.
   */
  public interface BodyBuilder extends HeadersBuilder<BodyBuilder> {

    /**
     * Set the length of the body in bytes, as specified by the
     * {@code Content-Length} header.
     *
     * @param contentLength the content length
     * @return this builder
     * @see HttpHeaders#setContentLength(long)
     */
    BodyBuilder contentLength(long contentLength);

    /**
     * Set the length of the body in bytes, as specified by the
     * {@code Content-Length} header.
     *
     * @param contentLength the content length
     * @return this builder
     * @see HttpHeaders#setContentLength(long)
     * @since 5.0
     */
    BodyBuilder contentLength(DataSize contentLength);

    /**
     * Set the {@linkplain MediaType media type} of the body, as specified
     * by the {@code Content-Type} header.
     *
     * @param contentType the content type
     * @return this builder
     * @see HttpHeaders#setContentType(MediaType)
     */
    BodyBuilder contentType(@Nullable MediaType contentType);

    /**
     * Set the {@linkplain MediaType media type} of the body, as specified
     * by the {@code Content-Type} header.
     *
     * @param contentType the content type
     * @return this builder
     * @see HttpHeaders#setContentType(String)
     * @since 5.0
     */
    BodyBuilder contentType(@Nullable String contentType);

    /**
     * Set the body of the request entity and build the RequestEntity.
     *
     * @param <T> the type of the body
     * @param body the body of the request entity
     * @return the built request entity
     */
    <T> RequestEntity<T> body(@Nullable T body);

    /**
     * Set the body and type of the request entity and build the RequestEntity.
     *
     * @param <T> the type of the body
     * @param body the body of the request entity
     * @param type the type of the body, useful for generic type resolution
     * @return the built request entity
     */
    <T> RequestEntity<T> body(@Nullable T body, Type type);
  }

  static class DefaultBodyBuilder implements BodyBuilder {

    private final HttpMethod method;

    @Nullable
    private final URI uri;

    @Nullable
    private final String uriTemplate;

    private final Object @Nullable [] uriVarsArray;

    @Nullable
    private final Map<String, ?> uriVarsMap;

    @Nullable
    private HttpHeaders headers;

    DefaultBodyBuilder(HttpMethod method, @Nullable URI url) {
      this.method = method;
      this.uri = url;
      this.uriTemplate = null;
      this.uriVarsArray = null;
      this.uriVarsMap = null;
    }

    DefaultBodyBuilder(HttpMethod method, @Nullable String uriTemplate, Object @Nullable ... uriVars) {
      this.method = method;
      this.uri = null;
      this.uriTemplate = uriTemplate;
      this.uriVarsArray = uriVars;
      this.uriVarsMap = null;
    }

    DefaultBodyBuilder(HttpMethod method, @Nullable String uriTemplate, @Nullable Map<String, ?> uriVars) {
      this.method = method;
      this.uri = null;
      this.uriTemplate = uriTemplate;
      this.uriVarsArray = null;
      this.uriVarsMap = uriVars;
    }

    @Override
    public BodyBuilder header(String headerName, String... headerValues) {
      headers().setOrRemove(headerName, headerValues);
      return this;
    }

    @Override
    public BodyBuilder headers(@Nullable HttpHeaders headers) {
      if (CollectionUtils.isNotEmpty(headers)) {
        headers().setAll(headers);
      }
      return this;
    }

    @Override
    public BodyBuilder headers(Consumer<HttpHeaders> headersConsumer) {
      headersConsumer.accept(headers());
      return this;
    }

    @Override
    public BodyBuilder accept(MediaType... acceptableMediaTypes) {
      headers().setAccept(Arrays.asList(acceptableMediaTypes));
      return this;
    }

    @Override
    public BodyBuilder acceptCharset(Charset... acceptableCharsets) {
      headers().setAcceptCharset(Arrays.asList(acceptableCharsets));
      return this;
    }

    @Override
    public BodyBuilder contentLength(long contentLength) {
      headers().setContentLength(contentLength);
      return this;
    }

    @Override
    public BodyBuilder contentLength(DataSize contentLength) {
      headers().setContentLength(contentLength.toBytes());
      return this;
    }

    @Override
    public BodyBuilder contentType(@Nullable MediaType contentType) {
      headers().setContentType(contentType);
      return this;
    }

    @Override
    public BodyBuilder contentType(@Nullable String contentType) {
      headers().setContentType(contentType);
      return this;
    }

    @Override
    public BodyBuilder ifModifiedSince(ZonedDateTime ifModifiedSince) {
      headers().setIfModifiedSince(ifModifiedSince);
      return this;
    }

    @Override
    public BodyBuilder ifModifiedSince(Instant ifModifiedSince) {
      headers().setIfModifiedSince(ifModifiedSince);
      return this;
    }

    @Override
    public BodyBuilder ifModifiedSince(long ifModifiedSince) {
      headers().setIfModifiedSince(ifModifiedSince);
      return this;
    }

    @Override
    public BodyBuilder ifNoneMatch(String... ifNoneMatches) {
      headers().setIfNoneMatch(Arrays.asList(ifNoneMatches));
      return this;
    }

    private HttpHeaders headers() {
      HttpHeaders headers = this.headers;
      if (headers == null) {
        headers = HttpHeaders.forWritable();
        this.headers = headers;
      }
      return headers;
    }

    @Override
    public RequestEntity<Void> build() {
      return buildInternal(null, null);
    }

    @Override
    public <T> RequestEntity<T> body(@Nullable T body) {
      return buildInternal(body, null);
    }

    @Override
    public <T> RequestEntity<T> body(@Nullable T body, Type type) {
      return buildInternal(body, type);
    }

    private <T> RequestEntity<T> buildInternal(@Nullable T body, @Nullable Type type) {
      if (this.uri != null) {
        return new RequestEntity<>(body, this.headers, this.method, this.uri, type);
      }
      else if (this.uriTemplate != null) {
        return new UriTemplateRequestEntity<>(body, this.headers, this.method,
                type, this.uriTemplate, this.uriVarsArray, this.uriVarsMap);
      }
      else {
        throw new IllegalStateException("Neither URI nor URI template");
      }
    }
  }

  /**
   * RequestEntity initialized with a URI template and variables instead of a {@link URI}.
   *
   * @param <T> the body type
   */
  public static class UriTemplateRequestEntity<T extends @Nullable Object> extends RequestEntity<T> {

    private final String uriTemplate;

    private final Object @Nullable [] uriVarsArray;

    private final @Nullable Map<String, ?> uriVarsMap;

    UriTemplateRequestEntity(@Nullable T body, @Nullable MultiValueMap<String, String> headers,
            @Nullable HttpMethod method, @Nullable Type type, String uriTemplate,
            Object @Nullable [] uriVarsArray, @Nullable Map<String, ?> uriVarsMap) {

      super(body, headers, method, null, type);
      this.uriTemplate = uriTemplate;
      this.uriVarsArray = uriVarsArray;
      this.uriVarsMap = uriVarsMap;
    }

    public String getUriTemplate() {
      return this.uriTemplate;
    }

    public Object @Nullable [] getVars() {
      return this.uriVarsArray;
    }

    @Nullable
    public Map<String, ?> getVarsMap() {
      return this.uriVarsMap;
    }

    @Override
    public boolean equals(@Nullable Object other) {
      if (this == other) {
        return true;
      }
      if (!super.equals(other)) {
        return false;
      }
      UriTemplateRequestEntity<?> otherEntity = (UriTemplateRequestEntity<?>) other;
      return Objects.equals(this.uriTemplate, otherEntity.uriTemplate)
              && Objects.equals(this.uriVarsMap, otherEntity.uriVarsMap)
              && ObjectUtils.nullSafeEquals(this.uriVarsArray, otherEntity.uriVarsArray);
    }

    @Override
    public int hashCode() {
      return 29 * super.hashCode() + Objects.hashCode(this.uriTemplate);
    }

    @Override
    public String toString() {
      return format(getMethod(), getUriTemplate(), getBody(), getHeaders());
    }
  }

}
