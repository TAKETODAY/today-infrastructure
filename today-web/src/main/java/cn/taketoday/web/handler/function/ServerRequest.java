/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.web.handler.function;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Consumer;

import cn.taketoday.core.TypeReference;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpRange;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.server.RequestPath;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.multipart.Multipart;
import cn.taketoday.web.multipart.MultipartRequest;
import cn.taketoday.web.util.UriBuilder;

/**
 * Represents a server-side HTTP request, as handled by a {@code HandlerFunction}.
 * Access to headers and body is offered by {@link Headers} and
 * {@link #body(Class)}, respectively.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface ServerRequest {

  /**
   * Get the HTTP method.
   *
   * @return the HTTP method as an HttpMethod enum value, or {@code null}
   * if not resolvable (e.g. in case of a non-standard HTTP method)
   */
  HttpMethod method();

  /**
   * Get the name of the HTTP method.
   *
   * @return the HTTP method as a String
   */
  String methodName();

  /**
   * Get the request URI.
   */
  URI uri();

  /**
   * Get a {@code UriBuilderComponents} from the URI associated with this
   * {@code ServerRequest}.
   *
   * @return a URI builder
   */
  UriBuilder uriBuilder();

  /**
   * Get the request path.
   */
  default String path() {
    return requestContext().getLookupPath().value();
  }

  /**
   * Get the request path as a {@code PathContainer}.
   */
  default RequestPath requestPath() {
    return requestContext().getRequestPath();
  }

  /**
   * Get the headers of this request.
   */
  Headers headers();

  /**
   * Get the cookies of this request.
   */
  MultiValueMap<String, HttpCookie> cookies();

  /**
   * Get the remote address to which this request is connected, if available.
   */
  Optional<InetSocketAddress> remoteAddress();

  /**
   * Get the readers used to convert the body of this request.
   */
  List<HttpMessageConverter<?>> messageConverters();

  /**
   * Extract the body as an object of the given type.
   *
   * @param bodyType the type of return value
   * @param <T> the body type
   * @return the body
   */
  <T> T body(Class<T> bodyType) throws IOException;

  /**
   * Extract the body as an object of the given type.
   *
   * @param bodyType the type of return value
   * @param <T> the body type
   * @return the body
   */
  <T> T body(TypeReference<T> bodyType) throws IOException;

  /**
   * Get the request attribute value if present.
   *
   * @param name the attribute name
   * @return the attribute value
   */
  default Optional<Object> attribute(String name) {
    Map<String, Object> attributes = attributes();
    if (attributes.containsKey(name)) {
      return Optional.of(attributes.get(name));
    }
    else {
      return Optional.empty();
    }
  }

  /**
   * Get a mutable map of request attributes.
   *
   * @return the request attributes
   */
  Map<String, Object> attributes();

  /**
   * Get the first parameter with the given name, if present.
   * parameters are contained in the query string or posted form data.
   *
   * @param name the parameter name
   * @return the parameter value
   * @see RequestContext#getParameter(String)
   */
  default Optional<String> param(String name) {
    List<String> paramValues = params().get(name);
    if (CollectionUtils.isEmpty(paramValues)) {
      return Optional.empty();
    }
    else {
      String value = paramValues.get(0);
      if (value == null) {
        value = "";
      }
      return Optional.of(value);
    }
  }

  /**
   * Get the parameters with the given name.
   * <p>
   * parameters are contained in the query string or posted form data.
   *
   * @param name the parameter name
   * @return the parameter value
   * @see RequestContext#getParameter(String)
   */
  default List<String> params(String name) {
    List<String> paramValues = params().get(name);
    if (CollectionUtils.isEmpty(paramValues)) {
      return Collections.emptyList();
    }
    return paramValues;
  }

  /**
   * Get all parameters for this request. parameters are contained
   * in the query string or posted form data.
   *
   * @see RequestContext#getParameters()
   */
  MultiValueMap<String, String> params();

  /**
   * Get the parts of a multipart request, provided the Content-Type is
   * {@code "multipart/form-data"}, or an exception otherwise.
   *
   * @return the multipart data, mapping from name to part(s)
   * @throws IOException if an I/O error occurred during the retrieval
   * @throws cn.taketoday.web.bind.NotMultipartRequestException if this request is not of type {@code "multipart/form-data"}
   * @see RequestContext#getMultipartRequest()
   * @see MultipartRequest#multipartData()
   */
  MultiValueMap<String, Multipart> multipartData() throws IOException;

  /**
   * Get the path variable with the given name, if present.
   *
   * @param name the variable name
   * @return the variable value
   * @throws IllegalArgumentException if there is no path variable with the given name
   */
  default String pathVariable(String name) {
    Map<String, String> pathVariables = pathVariables();
    if (pathVariables.containsKey(name)) {
      return pathVariables.get(name);
    }
    else {
      throw new IllegalArgumentException("No path variable with name \"" + name + "\" available");
    }
  }

  /**
   * Get all path variables for this request.
   */
  Map<String, String> pathVariables();

  /**
   * Get the request that this request is based on.
   */
  RequestContext requestContext();

  /**
   * Check whether the requested resource has been modified given the
   * supplied last-modified timestamp (as determined by the application).
   * If not modified, this method returns a response with corresponding
   * status code and headers, otherwise an empty result.
   * <p>Typical usage:
   * <pre class="code">
   * public ServerResponse myHandleMethod(ServerRequest request) {
   *   Instant lastModified = // application-specific calculation
   * 	 return request.checkNotModified(lastModified)
   * 	   .orElseGet(() -&gt; {
   * 	     // further request processing, actually building content
   * 		 return ServerResponse.ok().body(...);
   *     });
   * }</pre>
   * <p>This method works with conditional GET/HEAD requests, but
   * also with conditional POST/PUT/DELETE requests.
   * <p><strong>Note:</strong> you can use either
   * this {@code #checkNotModified(Instant)} method; or
   * {@link #checkNotModified(String)}. If you want to enforce both
   * a strong entity tag and a Last-Modified value,
   * as recommended by the HTTP specification,
   * then you should use {@link #checkNotModified(Instant, String)}.
   *
   * @param lastModified the last-modified timestamp that the
   * application determined for the underlying resource
   * @return a corresponding response if the request qualifies as not
   * modified, or an empty result otherwise.
   */
  default Optional<ServerResponse> checkNotModified(Instant lastModified) {
    Assert.notNull(lastModified, "LastModified is required");
    return DefaultServerRequest.checkNotModified(requestContext(), lastModified, null);
  }

  /**
   * Check whether the requested resource has been modified given the
   * supplied {@code ETag} (entity tag), as determined by the application.
   * If not modified, this method returns a response with corresponding
   * status code and headers, otherwise an empty result.
   * <p>Typical usage:
   * <pre class="code">
   * public ServerResponse myHandleMethod(ServerRequest request) {
   *   String eTag = // application-specific calculation
   * 	 return request.checkNotModified(eTag)
   * 	   .orElseGet(() -&gt; {
   * 	     // further request processing, actually building content
   * 		 return ServerResponse.ok().body(...);
   *     });
   * }</pre>
   * <p>This method works with conditional GET/HEAD requests, but
   * also with conditional POST/PUT/DELETE requests.
   * <p><strong>Note:</strong> you can use either
   * this {@link #checkNotModified(Instant)} method; or
   * {@code #checkNotModified(String)}. If you want to enforce both
   * a strong entity tag and a Last-Modified value,
   * as recommended by the HTTP specification,
   * then you should use {@link #checkNotModified(Instant, String)}.
   *
   * @param etag the entity tag that the application determined
   * for the underlying resource. This parameter will be padded
   * with quotes (") if necessary.
   * @return a corresponding response if the request qualifies as not
   * modified, or an empty result otherwise.
   */
  default Optional<ServerResponse> checkNotModified(String etag) {
    Assert.notNull(etag, "Etag is required");
    return DefaultServerRequest.checkNotModified(requestContext(), null, etag);
  }

  /**
   * Check whether the requested resource has been modified given the
   * supplied {@code ETag} (entity tag) and last-modified timestamp,
   * as determined by the application.
   * If not modified, this method returns a response with corresponding
   * status code and headers, otherwise an empty result.
   * <p>Typical usage:
   * <pre class="code">
   * public ServerResponse myHandleMethod(ServerRequest request) {
   *   Instant lastModified = // application-specific calculation
   *   String eTag = // application-specific calculation
   * 	 return request.checkNotModified(lastModified, eTag)
   * 	   .orElseGet(() -&gt; {
   * 	     // further request processing, actually building content
   * 		 return ServerResponse.ok().body(...);
   *     });
   * }</pre>
   * <p>This method works with conditional GET/HEAD requests, but
   * also with conditional POST/PUT/DELETE requests.
   *
   * @param lastModified the last-modified timestamp that the
   * application determined for the underlying resource
   * @param etag the entity tag that the application determined
   * for the underlying resource. This parameter will be padded
   * with quotes (") if necessary.
   * @return a corresponding response if the request qualifies as not
   * modified, or an empty result otherwise.
   */
  default Optional<ServerResponse> checkNotModified(Instant lastModified, String etag) {
    Assert.notNull(etag, "Etag is required");
    Assert.notNull(lastModified, "LastModified is required");
    return DefaultServerRequest.checkNotModified(requestContext(), lastModified, etag);
  }

  // Static methods

  /**
   * Create a new {@code ServerRequest} based on the given {@code RequestContext} and
   * message converters.
   *
   * @param context the request
   * @param messageReaders the message readers
   * @return the created {@code ServerRequest}
   */
  static ServerRequest create(RequestContext context, List<HttpMessageConverter<?>> messageReaders) {
    return new DefaultServerRequest(context, messageReaders);
  }

  /**
   * Create a builder with the status, headers, and cookies of the given request.
   *
   * @param other the response to copy the status, headers, and cookies from
   * @return the created builder
   */
  static Builder from(ServerRequest other) {
    return new DefaultServerRequestBuilder(other);
  }

  @Nullable
  static ServerRequest find(RequestContext context) {
    Object attribute = context.getAttribute(RouterFunctions.REQUEST_ATTRIBUTE);
    if (attribute instanceof ServerRequest serverRequest) {
      return serverRequest;
    }
    return null;
  }

  static ServerRequest findRequired(RequestContext context) {
    ServerRequest serverRequest = find(context);
    if (serverRequest == null) {
      throw new IllegalStateException(
              "Required attribute '" + RouterFunctions.REQUEST_ATTRIBUTE + "' is missing");
    }
    return serverRequest;
  }

  /**
   * Represents the headers of the HTTP request.
   *
   * @see ServerRequest#headers()
   */
  interface Headers {

    /**
     * Get the list of acceptable media types, as specified by the {@code Accept}
     * header.
     * <p>Returns an empty list if the acceptable media types are unspecified.
     */
    List<MediaType> accept();

    /**
     * Get the list of acceptable charsets, as specified by the
     * {@code Accept-Charset} header.
     */
    List<Charset> acceptCharset();

    /**
     * Get the list of acceptable languages, as specified by the
     * {@code Accept-Language} header.
     */
    List<Locale.LanguageRange> acceptLanguage();

    /**
     * Get the length of the body in bytes, as specified by the
     * {@code Content-Length} header.
     */
    OptionalLong contentLength();

    /**
     * Get the media type of the body, as specified by the
     * {@code Content-Type} header.
     */
    Optional<MediaType> contentType();

    /**
     * Get the value of the {@code Host} header, if available.
     * <p>If the header value does not contain a port, the
     * {@linkplain InetSocketAddress#getPort() port} in the returned address will
     * be {@code 0}.
     */
    @Nullable
    InetSocketAddress host();

    /**
     * Get the value of the {@code Range} header.
     * <p>Returns an empty list when the range is unknown.
     */
    List<HttpRange> range();

    /**
     * Get the header value(s), if any, for the header of the given name.
     * <p>Returns an empty list if no header values are found.
     *
     * @param headerName the header name
     */
    List<String> header(String headerName);

    /**
     * Get the first header value, if any, for the header for the given name.
     * <p>Returns {@code null} if no header values are found.
     *
     * @param headerName the header name
     */
    @Nullable
    default String firstHeader(String headerName) {
      List<String> list = header(headerName);
      return list.isEmpty() ? null : list.get(0);
    }

    /**
     * Get the headers as an instance of {@link HttpHeaders}.
     */
    HttpHeaders asHttpHeaders();
  }

  /**
   * Defines a builder for a request.
   */
  interface Builder {

    /**
     * Set the method of the request.
     *
     * @param method the new method
     * @return this builder
     */
    Builder method(HttpMethod method);

    /**
     * Set the URI of the request.
     *
     * @param uri the new URI
     * @return this builder
     */
    Builder uri(URI uri);

    /**
     * Add the given header value(s) under the given name.
     *
     * @param headerName the header name
     * @param headerValues the header value(s)
     * @return this builder
     * @see HttpHeaders#add(String, String)
     */
    Builder header(String headerName, String... headerValues);

    /**
     * Manipulate this request's headers with the given consumer.
     * <p>The headers provided to the consumer are "live", so that the consumer can be used to
     * {@linkplain HttpHeaders#set(String, String) overwrite} existing header values,
     * {@linkplain HttpHeaders#remove(Object) remove} values, or use any of the other
     * {@link HttpHeaders} methods.
     *
     * @param headersConsumer a function that consumes the {@code HttpHeaders}
     * @return this builder
     */
    Builder headers(Consumer<HttpHeaders> headersConsumer);

    /**
     * Add a cookie with the given name and value(s).
     *
     * @param name the cookie name
     * @param values the cookie value(s)
     * @return this builder
     */
    Builder cookie(String name, String... values);

    /**
     * Manipulate this request's cookies with the given consumer.
     * <p>The map provided to the consumer is "live", so that the consumer can be used to
     * {@linkplain MultiValueMap#set(Object, Object) overwrite} existing cookies,
     * {@linkplain MultiValueMap#remove(Object) remove} cookies, or use any of the other
     * {@link MultiValueMap} methods.
     *
     * @param cookiesConsumer a function that consumes the cookies map
     * @return this builder
     */
    Builder cookies(Consumer<MultiValueMap<String, HttpCookie>> cookiesConsumer);

    /**
     * Set the body of the request.
     * <p>Calling this methods will
     * {@linkplain cn.taketoday.core.io.buffer.DataBufferUtils#release(DataBuffer) release}
     * the existing body of the builder.
     *
     * @param body the new body
     * @return this builder
     */
    Builder body(byte[] body);

    /**
     * Set the body of the request to the UTF-8 encoded bytes of the given string.
     * <p>Calling this methods will
     * {@linkplain cn.taketoday.core.io.buffer.DataBufferUtils#release(DataBuffer) release}
     * the existing body of the builder.
     *
     * @param body the new body
     * @return this builder
     */
    Builder body(String body);

    /**
     * Add an attribute with the given name and value.
     *
     * @param name the attribute name
     * @param value the attribute value
     * @return this builder
     */
    Builder attribute(String name, Object value);

    /**
     * Manipulate this request's attributes with the given consumer.
     * <p>The map provided to the consumer is "live", so that the consumer can be used
     * to {@linkplain Map#put(Object, Object) overwrite} existing attributes,
     * {@linkplain Map#remove(Object) remove} attributes, or use any of the other
     * {@link Map} methods.
     *
     * @param attributesConsumer a function that consumes the attributes map
     * @return this builder
     */
    Builder attributes(Consumer<Map<String, Object>> attributesConsumer);

    /**
     * Add a parameter with the given name and value.
     *
     * @param name the parameter name
     * @param values the parameter value(s)
     * @return this builder
     */
    Builder param(String name, String... values);

    /**
     * Manipulate this request's parameters with the given consumer.
     * <p>The map provided to the consumer is "live", so that the consumer can be used to
     * {@linkplain MultiValueMap#set(Object, Object) overwrite} existing cookies,
     * {@linkplain MultiValueMap#remove(Object) remove} cookies, or use any of the other
     * {@link MultiValueMap} methods.
     *
     * @param paramsConsumer a function that consumes the parameters map
     * @return this builder
     */
    Builder params(Consumer<MultiValueMap<String, String>> paramsConsumer);

    /**
     * Set the remote address of the request.
     *
     * @param remoteAddress the remote address
     * @return this builder
     */
    Builder remoteAddress(InetSocketAddress remoteAddress);

    /**
     * Build the request.
     *
     * @return the built request
     */
    ServerRequest build();
  }

}
