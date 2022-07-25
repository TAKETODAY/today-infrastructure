/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.http;

import java.net.URI;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import cn.taketoday.core.MultiValueMap;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Extension of {@link HttpEntity} that adds a {@link HttpStatusCode} status code.
 * Used in {@code RestTemplate} as well {@code @Controller} methods.
 *
 * <p>Can also be used in TODAY Web MVC, as the return value from a @Controller method:
 * <pre class="code">
 * &#64;RequestMapping("/handle")
 * public ResponseEntity&lt;String&gt; handle() {
 *   URI location = ...;
 *   HttpHeaders responseHeaders = HttpHeaders.create();
 *   responseHeaders.setLocation(location);
 *   responseHeaders.set("MyResponseHeader", "MyValue");
 *   return new ResponseEntity&lt;String&gt;("Hello World", responseHeaders, HttpStatus.CREATED);
 * }
 * </pre>
 *
 * Or, by using a builder accessible via static methods:
 * <pre class="code">
 * &#64;RequestMapping("/handle")
 * public ResponseEntity&lt;String&gt; handle() {
 *   URI location = ...;
 *   return ResponseEntity.created(location).header("MyResponseHeader", "MyValue").body("Hello World");
 * }
 * </pre>
 *
 * From Spring
 *
 * @param <T> the body type
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @author TODAY 2020/12/6 17:06
 * @see #getStatusCode()
 * @since 3.0
 */
public class ResponseEntity<T> extends HttpEntity<T> {

  private final Object status;

  /**
   * Create a new {@code ResponseEntity} with the given status code, and no body nor headers.
   *
   * @param status the status code
   */
  public ResponseEntity(HttpStatusCode status) {
    this(null, null, status);
  }

  /**
   * Create a new {@code ResponseEntity} with the given body and status code, and no headers.
   *
   * @param body the entity body
   * @param status the status code
   */
  public ResponseEntity(T body, HttpStatusCode status) {
    this(body, null, status);
  }

  /**
   * Create a new {@code HttpEntity} with the given headers and status code, and no body.
   *
   * @param headers the entity headers
   * @param status the status code
   */
  public ResponseEntity(MultiValueMap<String, String> headers, HttpStatusCode status) {
    this(null, headers, status);
  }

  /**
   * Create a new {@code HttpEntity} with the given body, headers, and status code.
   *
   * @param body the entity body
   * @param headers the entity headers
   * @param status the status code
   */
  public ResponseEntity(@Nullable T body, MultiValueMap<String, String> headers, HttpStatusCode status) {
    super(body, headers);
    Assert.notNull(status, "HttpStatusCode must not be null");
    this.status = status;
  }

  /**
   * Create a {@code ResponseEntity} with a body, headers, and a raw status code.
   *
   * @param body the entity body
   * @param headers the entity headers
   * @param rawStatus the status code value
   * @since 4.0
   */
  public ResponseEntity(@Nullable T body, @Nullable MultiValueMap<String, String> headers, int rawStatus) {
    this(body, headers, (Object) rawStatus);
  }

  /**
   * Create a new {@code HttpEntity} with the given body, headers, and status code.
   * Just used behind the nested builder API.
   *
   * @param body the entity body
   * @param headers the entity headers
   * @param status the status code (as {@code HttpStatusCode} or as {@code Integer} value)
   */
  private ResponseEntity(T body, MultiValueMap<String, String> headers, Object status) {
    super(body, headers);
    Assert.notNull(status, "HttpStatusCode must not be null");
    this.status = status;
  }

  /**
   * Return the HTTP status code of the response.
   *
   * @return the HTTP status as an HttpStatusCode enum entry
   */
  public HttpStatusCode getStatusCode() {
    if (this.status instanceof HttpStatusCode) {
      return (HttpStatusCode) this.status;
    }
    else {
      return HttpStatusCode.valueOf((Integer) this.status);
    }
  }

  /**
   * Return the HTTP status code of the response.
   *
   * @return the HTTP status as an int value
   */
  public int getStatusCodeValue() {
    if (this.status instanceof HttpStatusCode) {
      return ((HttpStatusCode) this.status).value();
    }
    else {
      return (Integer) this.status;
    }
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!super.equals(other)) {
      return false;
    }
    ResponseEntity<?> otherEntity = (ResponseEntity<?>) other;
    return Objects.equals(this.status, otherEntity.status);
  }

  @Override
  public int hashCode() {
    return (29 * super.hashCode() + Objects.hashCode(this.status));
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder("<");
    builder.append(status.toString());
    if (status instanceof HttpStatus status) {
      builder.append(' ');
      builder.append(status.getReasonPhrase());
    }
    builder.append(',');
    T body = getBody();
    HttpHeaders headers = getHeaders();
    if (body != null) {
      builder.append(body);
      builder.append(',');
    }
    builder.append(headers);
    builder.append('>');
    return builder.toString();
  }

  // Static builder methods

  /**
   * Create a builder with the given status.
   *
   * @param status the response status
   * @return the created builder
   */
  public static BodyBuilder status(HttpStatusCode status) {
    Assert.notNull(status, "HttpStatusCode must not be null");
    return new DefaultBuilder(status);
  }

  /**
   * Create a builder with the given status.
   *
   * @param status the response status
   * @return the created builder
   */
  public static BodyBuilder status(int status) {
    return new DefaultBuilder(status);
  }

  /**
   * Create a builder with the status set to {@linkplain HttpStatus#OK OK}.
   *
   * @return the created builder
   */
  public static BodyBuilder ok() {
    return status(HttpStatus.OK);
  }

  /**
   * A shortcut for creating a {@code ResponseEntity} with the given body and
   * the status set to {@linkplain HttpStatus#OK OK}.
   *
   * @return the created {@code ResponseEntity}
   */
  public static <T> ResponseEntity<T> ok(T body) {
    return ok().body(body);
  }

  /**
   * A shortcut for creating a {@code ResponseEntity} with the given body
   * and the {@linkplain HttpStatus#OK OK} status, or an empty body and a
   * {@linkplain HttpStatus#NOT_FOUND NOT FOUND} status in case of an
   * {@linkplain Optional#empty()} parameter.
   *
   * @return the created {@code ResponseEntity}
   */
  public static <T> ResponseEntity<T> of(Optional<T> body) {
    Assert.notNull(body, "Body must not be null");
    return body.map(ResponseEntity::ok).orElseGet(() -> notFound().build());
  }

  /**
   * Create a builder for a {@code ResponseEntity} with the given
   * {@link ProblemDetail} as the body, and its
   * {@link ProblemDetail#getStatus() status} as the status.
   * <p>Note that {@code ProblemDetail} is supported as a return value from
   * controller methods and from {@code @ExceptionHandler} methods. The method
   * here is convenient to also add response headers.
   *
   * @param body the details for an HTTP error response
   * @return the created builder
   * @since 4.0
   */
  public static HeadersBuilder<?> of(ProblemDetail body) {
    return new DefaultBuilder(body.getStatus()) {

      @SuppressWarnings("unchecked")
      @Override
      public <T> ResponseEntity<T> build() {
        return (ResponseEntity<T>) body(body);
      }
    };
  }

  /**
   * Create a new builder with a {@linkplain HttpStatus#CREATED CREATED} status
   * and a location header set to the given URI.
   *
   * @param location the location URI
   * @return the created builder
   */
  public static BodyBuilder created(URI location) {
    return status(HttpStatus.CREATED).location(location);
  }

  /**
   * Create a builder with an {@linkplain HttpStatus#ACCEPTED ACCEPTED} status.
   *
   * @return the created builder
   */
  public static BodyBuilder accepted() {
    return status(HttpStatus.ACCEPTED);
  }

  /**
   * Create a builder with a {@linkplain HttpStatus#NO_CONTENT NO_CONTENT} status.
   *
   * @return the created builder
   */
  public static HeadersBuilder<?> noContent() {
    return status(HttpStatus.NO_CONTENT);
  }

  /**
   * Create a builder with a {@linkplain HttpStatus#BAD_REQUEST BAD_REQUEST} status.
   *
   * @return the created builder
   */
  public static BodyBuilder badRequest() {
    return status(HttpStatus.BAD_REQUEST);
  }

  /**
   * Create a builder with a {@linkplain HttpStatus#NOT_FOUND NOT_FOUND} status.
   *
   * @return the created builder
   */
  public static HeadersBuilder<?> notFound() {
    return status(HttpStatus.NOT_FOUND);
  }

  /**
   * Create a builder with an
   * {@linkplain HttpStatus#UNPROCESSABLE_ENTITY UNPROCESSABLE_ENTITY} status.
   *
   * @return the created builder
   */
  public static BodyBuilder unprocessableEntity() {
    return status(HttpStatus.UNPROCESSABLE_ENTITY);
  }

  /**
   * Defines a builder that adds headers to the response entity.
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
    B headers(HttpHeaders headers);

    /**
     * Manipulate this entity's headers with the given consumer. The
     * headers provided to the consumer are "live", so that the consumer can be used to
     * {@linkplain HttpHeaders#set(String, String) overwrite} existing header values,
     * {@linkplain HttpHeaders#remove(Object) remove} values, or use any of the other
     * {@link HttpHeaders} methods.
     *
     * @param headersConsumer a function that consumes the {@code HttpHeaders}
     * @return this builder
     */
    B headers(Consumer<HttpHeaders> headersConsumer);

    /**
     * Set the set of allowed {@link jodd.net.HttpMethod HTTP methods}, as specified
     * by the {@code Allow} header.
     *
     * @param allowedMethods the allowed methods
     * @return this builder
     * @see HttpHeaders#setAllow(java.util.Collection)
     */
    B allow(HttpMethod... allowedMethods);

    /**
     * Set the entity tag of the body, as specified by the {@code ETag} header.
     *
     * @param etag the new entity tag
     * @return this builder
     * @see HttpHeaders#setETag(String)
     */
    B eTag(String etag);

    /**
     * Set the time the resource was last changed, as specified by the
     * {@code Last-Modified} header.
     *
     * @param lastModified the last modified date
     * @return this builder
     * @see HttpHeaders#setLastModified(ZonedDateTime)
     */
    B lastModified(ZonedDateTime lastModified);

    /**
     * Set the time the resource was last changed, as specified by the
     * {@code Last-Modified} header.
     *
     * @param lastModified the last modified date
     * @return this builder
     * @see HttpHeaders#setLastModified(Instant)
     */
    B lastModified(Instant lastModified);

    /**
     * Set the time the resource was last changed, as specified by the
     * {@code Last-Modified} header.
     * <p>The date should be specified as the number of milliseconds since
     * January 1, 1970 GMT.
     *
     * @param lastModified the last modified date
     * @return this builder
     * @see HttpHeaders#setLastModified(long)
     */
    B lastModified(long lastModified);

    /**
     * Set the location of a resource, as specified by the {@code Location} header.
     *
     * @param location the location
     * @return this builder
     * @see HttpHeaders#setLocation(URI)
     */
    B location(URI location);

    /**
     * Set the caching directives for the resource, as specified by the HTTP 1.1
     * {@code Cache-Control} header.
     * <p>A {@code CacheControl} instance can be built like
     * {@code CacheControl.maxAge(3600).cachePublic().noTransform()}.
     *
     * @param cacheControl a builder for cache-related HTTP response headers
     * @return this builder
     * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2">RFC-7234 Section 5.2</a>
     */
    B cacheControl(CacheControl cacheControl);

    /**
     * Configure one or more request header names (e.g. "Accept-Language") to
     * add to the "Vary" response header to inform clients that the response is
     * subject to content negotiation and variances based on the value of the
     * given request headers. The configured request header names are added only
     * if not already present in the response "Vary" header.
     *
     * @param requestHeaders request header names
     */
    B varyBy(String... requestHeaders);

    /**
     * Build the response entity with no body.
     *
     * @return the response entity
     * @see BodyBuilder#body(Object)
     */
    <T> ResponseEntity<T> build();
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
     * Set the {@linkplain MediaType media type} of the body, as specified by the
     * {@code Content-Type} header.
     *
     * @param contentType the content type
     * @return this builder
     * @see HttpHeaders#setContentType(MediaType)
     */
    BodyBuilder contentType(MediaType contentType);

    /**
     * Set the body of the response entity and returns it.
     *
     * @param <T> the type of the body
     * @param body the body of the response entity
     * @return the built response entity
     */
    <T> ResponseEntity<T> body(T body);
  }

  private static class DefaultBuilder implements BodyBuilder {

    private final Object statusCode;

    private final DefaultHttpHeaders headers = new DefaultHttpHeaders();

    public DefaultBuilder(Object statusCode) {
      this.statusCode = statusCode;
    }

    @Override
    public BodyBuilder header(String headerName, String... headerValues) {
      for (String headerValue : headerValues) {
        this.headers.add(headerName, headerValue);
      }
      return this;
    }

    @Override
    public BodyBuilder headers(HttpHeaders headers) {
      if (headers != null) {
        this.headers.addAll(headers);
      }
      return this;
    }

    @Override
    public BodyBuilder headers(Consumer<HttpHeaders> headersConsumer) {
      headersConsumer.accept(this.headers);
      return this;
    }

    @Override
    public BodyBuilder allow(HttpMethod... allowedMethods) {
      this.headers.setAllow(new LinkedHashSet<>(Arrays.asList(allowedMethods)));
      return this;
    }

    @Override
    public BodyBuilder contentLength(long contentLength) {
      this.headers.setContentLength(contentLength);
      return this;
    }

    @Override
    public BodyBuilder contentType(MediaType contentType) {
      this.headers.setContentType(contentType);
      return this;
    }

    @Override
    public BodyBuilder eTag(String etag) {
      if (!etag.startsWith("\"") && !etag.startsWith("W/\"")) {
        etag = "\"" + etag;
      }
      if (!etag.endsWith("\"")) {
        etag = etag + "\"";
      }
      this.headers.setETag(etag);
      return this;
    }

    @Override
    public BodyBuilder lastModified(ZonedDateTime date) {
      this.headers.setLastModified(date);
      return this;
    }

    @Override
    public BodyBuilder lastModified(Instant date) {
      this.headers.setLastModified(date);
      return this;
    }

    @Override
    public BodyBuilder lastModified(long date) {
      this.headers.setLastModified(date);
      return this;
    }

    @Override
    public BodyBuilder location(URI location) {
      this.headers.setLocation(location);
      return this;
    }

    @Override
    public BodyBuilder cacheControl(CacheControl cacheControl) {
      this.headers.setCacheControl(cacheControl);
      return this;
    }

    @Override
    public BodyBuilder varyBy(String... requestHeaders) {
      this.headers.setVary(Arrays.asList(requestHeaders));
      return this;
    }

    @Override
    public <T> ResponseEntity<T> build() {
      return body(null);
    }

    @Override
    public <T> ResponseEntity<T> body(T body) {
      return new ResponseEntity<>(body, this.headers, this.statusCode);
    }
  }

}

