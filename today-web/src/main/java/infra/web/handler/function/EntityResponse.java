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

package infra.web.handler.function;

import java.net.URI;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import infra.core.ParameterizedTypeReference;
import infra.http.CacheControl;
import infra.http.HttpCookie;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpStatusCode;
import infra.http.MediaType;
import infra.lang.Nullable;
import infra.util.MultiValueMap;

/**
 * Entity-specific subtype of {@link ServerResponse} that exposes entity data.
 *
 * @param <T> the entity type
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface EntityResponse<T> extends ServerResponse {

  /**
   * Return the entity that makes up this response.
   */
  T entity();

  // Static builder methods

  /**
   * Create a builder with the given object.
   *
   * @param t the object that represents the body of the response
   * @param <T> the type of element contained in the entity
   * @return the created builder
   */
  static <T> Builder<T> fromObject(T t) {
    return new DefaultEntityResponseBuilder<>(t, null);
  }

  /**
   * Create a builder with the given object and type reference.
   *
   * @param t the object that represents the body of the response
   * @param entityType the type of the entity, used to capture the generic type
   * @param <T> the type of element contained in the entity
   * @return the created builder
   */
  static <T> Builder<T> fromObject(T t, ParameterizedTypeReference<T> entityType) {
    return new DefaultEntityResponseBuilder<>(t, entityType.getType());
  }

  /**
   * Defines a builder for {@code EntityResponse}.
   *
   * @param <T> the entity type
   */
  interface Builder<T> {

    /**
     * Add the given header value(s) under the given name.
     *
     * @param headerName the header name
     * @param headerValues the header value(s)
     * @return this builder
     * @see HttpHeaders#add(String, String)
     */
    Builder<T> header(String headerName, String... headerValues);

    /**
     * Manipulate this response's headers with the given consumer. The
     * headers provided to the consumer are "live", so that the consumer can be used to
     * {@linkplain HttpHeaders#setOrRemove(String, String) overwrite} existing header values,
     * {@linkplain HttpHeaders#remove(Object) remove} values, or use any of the other
     * {@link HttpHeaders} methods.
     *
     * @param headersConsumer a function that consumes the {@code HttpHeaders}
     * @return this builder
     */
    Builder<T> headers(Consumer<HttpHeaders> headersConsumer);

    /**
     * Add the given HttpHeaders.
     *
     * @param headers the headers
     * @return this builder
     * @see MultiValueMap#setAll(Map)
     * @since 5.0
     */
    Builder<T> headers(@Nullable HttpHeaders headers);

    /**
     * Set the HTTP status.
     *
     * @param status the response status
     * @return this builder
     */
    Builder<T> status(HttpStatusCode status);

    /**
     * Set the HTTP status.
     *
     * @param status the response status
     * @return this builder
     */
    Builder<T> status(int status);

    /**
     * Add the given cookie to the response.
     *
     * @param cookie the cookie to add
     * @return this builder
     */
    Builder<T> cookie(HttpCookie cookie);

    /**
     * Add a cookie with the given name and value(s).
     *
     * @param name the cookie name
     * @param values the cookie value(s)
     * @return this builder
     * @since 5.0
     */
    Builder<T> cookie(String name, String... values);

    /**
     * Manipulate this response's cookies with the given consumer. The
     * cookies provided to the consumer are "live", so that the consumer can be used to
     * {@linkplain MultiValueMap#setOrRemove(Object, Object) overwrite} existing cookies,
     * {@linkplain MultiValueMap#remove(Object) remove} cookies, or use any of the other
     * {@link MultiValueMap} methods.
     *
     * @param cookiesConsumer a function that consumes the cookies
     * @return this builder
     */
    Builder<T> cookies(Consumer<MultiValueMap<String, HttpCookie>> cookiesConsumer);

    /**
     * Add a cookies with the given name and values.
     *
     * @param cookies the cookies
     * @return this builder
     * @see MultiValueMap#setAll(Map)
     * @since 5.0
     */
    Builder<T> cookies(@Nullable Collection<HttpCookie> cookies);

    /**
     * Add a cookies with the given name and values.
     *
     * @param cookies the cookies
     * @return this builder
     * @see MultiValueMap#setAll(Map)
     * @since 5.0
     */
    Builder<T> cookies(@Nullable MultiValueMap<String, HttpCookie> cookies);

    /**
     * Set the set of allowed {@link HttpMethod HTTP methods}, as specified
     * by the {@code Allow} header.
     *
     * @param allowedMethods the allowed methods
     * @return this builder
     * @see HttpHeaders#setAllow(Collection)
     */
    Builder<T> allow(HttpMethod... allowedMethods);

    /**
     * Set the set of allowed {@link HttpMethod HTTP methods}, as specified
     * by the {@code Allow} header.
     *
     * @param allowedMethods the allowed methods
     * @return this builder
     * @see HttpHeaders#setAllow(Collection)
     */
    Builder<T> allow(Set<HttpMethod> allowedMethods);

    /**
     * Set the entity tag of the body, as specified by the {@code ETag} header.
     *
     * @param etag the new entity tag
     * @return this builder
     * @see HttpHeaders#setETag(String)
     */
    Builder<T> eTag(@Nullable String etag);

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
    Builder<T> lastModified(ZonedDateTime lastModified);

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
    Builder<T> lastModified(Instant lastModified);

    /**
     * Set the location of a resource, as specified by the {@code Location} header.
     *
     * @param location the location
     * @return this builder
     * @see HttpHeaders#setLocation(URI)
     */
    Builder<T> location(URI location);

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
    Builder<T> cacheControl(CacheControl cacheControl);

    /**
     * Configure one or more request header names (e.g. "Accept-Language") to
     * add to the "Vary" response header to inform clients that the response is
     * subject to content negotiation and variances based on the value of the
     * given request headers. The configured request header names are added only
     * if not already present in the response "Vary" header.
     *
     * @param requestHeaders request header names
     * @return this builder
     */
    Builder<T> varyBy(String... requestHeaders);

    /**
     * Set the length of the body in bytes, as specified by the
     * {@code Content-Length} header.
     *
     * @param contentLength the content length
     * @return this builder
     * @see HttpHeaders#setContentLength(long)
     */
    Builder<T> contentLength(long contentLength);

    /**
     * Set the {@linkplain MediaType media type} of the body, as specified by the
     * {@code Content-Type} header.
     *
     * @param contentType the content type
     * @return this builder
     * @see HttpHeaders#setContentType(MediaType)
     */
    Builder<T> contentType(MediaType contentType);

    /**
     * Build the response.
     *
     * @return the built response
     */
    EntityResponse<T> build();
  }

}
