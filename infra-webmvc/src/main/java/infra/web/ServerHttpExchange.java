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

package infra.web;

import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import infra.context.ApplicationContext;
import infra.context.MessageSource;
import infra.core.Conventions;
import infra.core.io.InputStreamSource;
import infra.core.io.OutputStreamSource;
import infra.http.HttpCookie;
import infra.http.HttpHeaders;
import infra.http.HttpInputMessage;
import infra.http.HttpMethod;
import infra.http.HttpRequest;
import infra.http.HttpStatusCode;
import infra.http.InvalidMediaTypeException;
import infra.http.MediaType;
import infra.http.ResponseCookie;
import infra.http.server.RequestPath;
import infra.http.server.ServerHttpResponse;
import infra.session.Session;
import infra.session.SessionManager;
import infra.util.Assert;
import infra.util.CollectionUtils;
import infra.util.MultiValueMap;
import infra.util.StringUtils;
import infra.web.async.WebAsyncManager;
import infra.web.async.WebAsyncRequest;
import infra.web.context.annotation.RequestScope;
import infra.web.context.annotation.SessionScope;
import infra.web.multipart.MultipartRequest;
import infra.web.util.WebUtils;

/**
 * Server-side exchange for an HTTP request and its associated response.
 * <p>Provides access to request metadata and the request body, as well as
 * methods for building the response and managing the exchange lifecycle.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see AbstractServerHttpExchange
 * @since 5.0 2026/7/12 23:08
 */
public interface ServerHttpExchange extends InputStreamSource, OutputStreamSource, HttpInputMessage, HttpRequest {

  /**
   * Scope identifier for request scope: "request".
   * Supported in addition to the standard scopes "singleton" and "prototype".
   */
  String SCOPE_REQUEST = RequestScope.NAME;

  /**
   * Scope identifier for session scope: "session".
   * Supported in addition to the standard scopes "singleton" and "prototype".
   */
  String SCOPE_SESSION = SessionScope.NAME;

  /**
   * Attribute name for the original request URI before forwarding.
   *
   * @see #forward(String)
   */
  String FORWARD_REQUEST_URI_ATTRIBUTE =
          Conventions.getQualifiedAttributeName(ServerHttpExchange.class, "forward.requestUri");

  /**
   * Attribute name for the forwarded request marker.
   *
   * @see #forward(String)
   */
  String FORWARD_ATTRIBUTE = Conventions.getQualifiedAttributeName(ServerHttpExchange.class, "forward");

  /**
   * Empty {@link HttpCookie} array constant used as a safe return value
   * when a request has no cookies.
   */
  HttpCookie[] EMPTY_COOKIES = {};

  /**
   * Lifecycle phases for request/response event callbacks.
   *
   * @since 5.0
   */
  enum Lifecycle {
    /** Before response headers are committed */
    COMMITTING,
    /** After response headers have been committed */
    COMMITTED,
    /** After request processing completes */
    COMPLETED
  }

  /**
   * Return the ApplicationContext that this request runs in.
   *
   * @since 4.0
   */
  ApplicationContext getApplicationContext();

  /**
   * Return the timestamp recorded at the start of request handling.
   *
   * @return the start time in milliseconds since the Unix epoch
   * @since 4.0
   */
  long getRequestTimeMillis();

  /**
   * Return the elapsed time since the start of request handling.
   * The elapsed time stops increasing once the request completes.
   *
   * @return the elapsed time in milliseconds
   * @since 4.0
   */
  long getRequestProcessingTime();

  /**
   * Returns a boolean indicating whether this request was
   * made using a secure channel, such as HTTPS.
   *
   * @return a boolean indicating if the request was made
   * using a secure channel
   * @since 5.0
   */
  boolean isSecure();

  /**
   * Returns the name of the scheme used to make this request, for example, <code>http</code>, <code>https</code>, or
   * <code>ftp</code>. Different schemes have different rules for constructing URLs, as noted in RFC 1738.
   *
   * @return a <code>String</code> containing the name
   * of the scheme used to make this request
   * @since 3.0.1
   */
  String getScheme();

  /**
   * Returns the host name of the server to which the request was sent. It may be derived from a protocol specific
   * mechanism, such as the <code>Host</code> header, or the HTTP/2 authority, or
   * <a href="https://tools.ietf.org/html/rfc7239">RFC 7239</a>, otherwise the resolved server name or the server IP
   * address.
   *
   * @return a <code>String</code> containing the name of the server
   * @since 4.0
   */
  String getServerName();

  /**
   * Returns the port number to which the request was sent. It may be derived from a protocol specific mechanism, such as
   * the <code>Host</code> header, or HTTP authority, or <a href="https://tools.ietf.org/html/rfc7239">RFC 7239</a>,
   * otherwise the server port where the client connection was accepted on.
   *
   * @return an integer specifying the port number
   * @since 4.0
   */
  int getServerPort();

  /**
   * Returns the Internet Protocol (IP) address of the client or last proxy that
   * sent the request.
   *
   * @return a <code>String</code> containing the IP address of the client that
   * sent the request
   */
  String getRemoteAddress();

  /**
   * Returns the Internet Protocol (IP) source port the remote end of the connection on which the request was received. By
   * default, this is either the port of the client or last proxy that sent the request. In some cases, protocol specific
   * mechanisms such as <a href="https://tools.ietf.org/html/rfc7239">RFC 7239</a> may be used to obtain a port different
   * to that of the actual TCP/IP connection.
   *
   * @return an integer specifying the port number
   * @since 5.0
   */
  int getRemotePort();

  /**
   * Returns the local address where this server is bound to. The returned
   * {@link SocketAddress} is supposed to be down-cast into more concrete
   * type such as {@link InetSocketAddress} to retrieve the detailed
   * information.
   *
   * @return the local address of this server.
   * @since 5.0
   */
  SocketAddress localAddress();

  /**
   * Get the remote address to which this request is connected, if available.
   *
   * @return the remote address of this request.
   * @since 5.0
   */
  InetSocketAddress remoteAddress();

  /**
   * Return the HTTP method of the request.
   *
   * @return the HTTP method as an HttpMethod value
   * @see HttpMethod#resolve(String)
   */
  @Override
  HttpMethod getMethod();

  /**
   * Return the URI of the request (including a query string if any,
   * but only if it is well-formed for a URI representation).
   *
   * @return the URI of the request (never {@code null})
   * @throws IllegalStateException if the request URL cannot be represented as a URI
   */
  @Override
  URI getURI();

  /**
   * Return the request URI string without the query component or percent decoding.
   * For example, a request target of {@code /files/a%20b?download=true}
   * yields {@code /files/a%20b}.
   *
   * @return the request URI without the query component
   * @see #getQueryString()
   */
  String getRequestURI();

  /**
   * Return the parsed path of the current request.
   *
   * @return the current request path
   */
  RequestPath getRequestPath();

  /**
   * Reconstruct the request URL from the scheme, server name, port, and request URI.
   * The query string and the default port for HTTP or HTTPS are omitted.
   *
   * @return the request URL without the query string
   */
  String getRequestURL();

  /**
   * Returns the query string that is contained in the request URL after the path.
   * This method returns <code>null</code> if the URL does not have a query
   * string. Same as the value of the CGI variable QUERY_STRING.
   *
   * @return a <code>String</code> containing the query string or
   * <code>null</code> if the URL contains no query string. The value is
   * not decoded by the container.
   */
  @Nullable
  String getQueryString();

  /**
   * Return the cookies sent by the client with this request.
   *
   * @return the request cookies, or an empty array if none were sent
   */
  HttpCookie[] getCookies();

  /**
   * Return the first request cookie with the given name.
   *
   * @param name the cookie name
   * @return the matching cookie, or {@code null} if none was sent
   * @since 2.3.7
   */
  @Nullable
  HttpCookie getCookie(String name);

  /**
   * Return the first request cookie with the given name, if present.
   *
   * @param name the cookie name
   * @return the matching cookie, or an empty {@link Optional}
   * @see #getCookie(String)
   * @since 5.0
   */
  default Optional<HttpCookie> cookie(String name) {
    return Optional.ofNullable(getCookie(name));
  }

  /**
   * Adds the specified cookie to the response. This method can be called multiple
   * times to set more than one cookie.
   *
   * @param cookie the {@link ResponseCookie} to return to the client
   */
  void addCookie(ResponseCookie cookie);

  /**
   * Adds the specified cookie to the response by building it from the provided builder.
   * This method can be called multiple times to set more than one cookie.
   *
   * @param cookie the {@link ResponseCookie.Builder} used to construct the cookie to return to the client
   * @since 5.0
   */
  default void addCookie(ResponseCookie.Builder cookie) {
    addCookie(cookie.build());
  }

  /**
   * Adds a cookie to the response by configuring its properties using the provided consumer.
   * <p>
   * This method creates a {@link ResponseCookie.Builder} with the specified name and applies
   * the configuration defined in the {@code consumer}. The configured cookie is then added
   * to the response.
   *
   * <p><b>Example Usage:</b>
   * <pre>{@code
   *   exchange.addCookie("sessionId", builder -> {
   *     builder.value("12345")
   *            .httpOnly(true)
   *            .secure(true)
   *            .maxAge(Duration.ofHours(1));
   *   });
   * }</pre>
   *
   * @param name the name of the cookie; must not be null or empty
   * @param consumer a {@link Consumer} that configures the {@link ResponseCookie.Builder};
   * must not be null
   * @throws NullPointerException if the consumer is {@code null}
   * @throws IllegalArgumentException if the configured cookie is invalid
   * @since 5.0
   */
  default void addCookie(String name, Consumer<ResponseCookie.Builder> consumer) {
    var builder = ResponseCookie.builder(name);
    consumer.accept(builder);
    addCookie(builder);
  }

  /**
   * Adds the specified cookie to the response. This method can be called multiple
   * times to set more than one cookie.
   *
   * @param name the Cookie name to return to the client
   * @param value the Cookie value to return to the client
   */
  default void addCookie(String name, @Nullable String value) {
    addCookie(ResponseCookie.builder(name, value).build());
  }

  /**
   * Adds the specified {@link HttpCookie} to the response by extracting its name and value.
   * This method can be called multiple times to set more than one cookie.
   *
   * @param cookie the {@link HttpCookie} to add to the response; must not be null
   * @since 5.0
   */
  default void addCookie(HttpCookie cookie) {
    addCookie(cookie.getName(), cookie.getValue());
  }

  /**
   * Remove response cookies with the given name from this response.
   * This does not instruct the client to delete a previously stored cookie.
   *
   * @param name the cookie name
   * @return the removed response cookies, or {@code null} if none matched
   */
  @Nullable
  List<ResponseCookie> removeCookies(String name);

  /**
   * Determine whether the response cookie list is non-empty.
   *
   * @return {@code true} if the list contains at least one response cookie
   * @see #responseCookies()
   */
  boolean hasResponseCookies();

  /**
   * Return the live list of response cookies. Add or remove cookies before
   * the response is committed.
   *
   * @return the modifiable list of {@link ResponseCookie} instances, possibly empty
   */
  List<ResponseCookie> responseCookies();

  /**
   * Returns a {@link MultiValueMap} of the parameters of this request.
   *
   * <p>Request parameters are extra information sent with the request. Parameters
   * are contained in the query string or posted form data.
   *
   * @return a {@link MultiValueMap} containing parameter names as keys and parameter values
   * as map values. The keys in the parameter map are of type {@code String}. The
   * values in the parameter map are of type {@code List<String>}.
   */
  MultiValueMap<String, String> getParameters();

  /**
   * Returns an <code>Set</code> of <code>String</code> objects containing
   * the names of the parameters contained in this request. If the request has no
   * parameters, the method returns an empty {@link Collections#emptySet() Set}.
   *
   * @return an <code>Set</code> of <code>String</code> objects, each
   * <code>String</code> containing the name of a request parameter; or an
   * empty {@link Collections#emptySet() Set} if the request has no parameters
   */
  default Set<String> getParameterNames() {
    return new LinkedHashSet<>(getParameters().keySet());
  }

  /**
   * Returns an array of <code>String</code> objects containing all of the values
   * the given request parameter has, or <code>null</code> if the parameter does
   * not exist.
   *
   * <p>
   * If the parameter has a single value, the array has a length of 1.
   *
   * @param name a <code>String</code> containing the name of the parameter whose
   * value is requested
   * @return the parameter values, or {@code null} if there are none
   * @see #getParameters()
   */
  default String @Nullable [] getParameterValues(String name) {
    List<String> list = getParameters().get(name);
    if (CollectionUtils.isEmpty(list)) {
      return null;
    }
    return StringUtils.toStringArray(list);
  }

  /**
   * Returns the value of a request parameter as a <code>String</code>, or
   * <code>null</code> if the parameter does not exist. Request parameters are
   * extra information sent with the request. Parameters are contained in the
   * query string or posted form data.
   *
   * <p>
   * You should only use this method when you are sure the parameter has only one
   * value. If the parameter might have more than one value, use
   * {@link #getParameterValues(String)}.
   *
   * <p>For a multivalued parameter, this method returns the first value
   * returned by {@link #getParameterValues(String)}.
   *
   * <p>
   * If the parameter data was sent in the request body, such as occurs with an
   * HTTP POST request, then reading the body directly via {@link #getInputStream}
   * or {@link #getReader} can interfere with the execution of this method.
   *
   * @param name a <code>String</code> specifying the name of the parameter
   * @return the first parameter value, or {@code null} if there are none
   * @see #getParameterValues(String)
   */
  default @Nullable String getParameter(String name) {
    List<String> list = getParameters().get(name);
    return CollectionUtils.firstElement(list);
  }

  /**
   * Return the known length of the request body, not the number of bytes
   * currently available to read.
   *
   * @return the content length in bytes, or {@code -1} if unknown
   */
  @Override
  long getContentLength();

  /**
   * Return the body of the message as an input stream.
   *
   * @return the input stream body (never {@code null})
   * @throws IOException in case of I/O errors
   */
  @Override
  default InputStream getBody() throws IOException {
    return getInputStream();
  }

  /**
   * Return the headers of this message.
   *
   * @return a corresponding HttpHeaders object (never {@code null})
   */
  @Override
  default HttpHeaders getHeaders() {
    return requestHeaders();
  }

  /**
   * Return the request body as a byte stream.
   * <p>Use either this stream or {@link #getReader()} to consume the body;
   * they are not independent reads of the same data.
   *
   * @return the request body stream
   * @throws IOException if opening the stream fails
   */
  @Override
  InputStream getInputStream() throws IOException;

  /**
   * Return a reader that decodes the request body as characters.
   * <p>Do not interleave reads from this reader and {@link #getInputStream()}.
   *
   * @return a reader for the request body
   * @throws IOException if opening the reader fails
   * @see #getInputStream()
   */
  @Override
  BufferedReader getReader() throws IOException;

  // -----------------------------------------------------

  /**
   * Determine whether the request declares a {@code multipart/*} content type.
   * GET and HEAD requests are not treated as multipart. This check does not
   * parse or validate the request body.
   *
   * @return {@code true} if the request is treated as multipart
   * @throws InvalidMediaTypeException if the content type cannot be parsed
   */
  boolean isMultipart();

  /**
   * Adapt this request to a {@link MultipartRequest} for accessing its parts.
   * <p>Use {@link #isMultipart()} to check whether the request is multipart.
   * The default multipart parser defers reading and parsing the body until
   * part data is accessed. Do not consume the raw body before accessing the parts.
   * <pre>{@code
   * if (exchange.isMultipart()) {
   *   var file = exchange.asMultipartRequest().getPart("file");
   *   // A missing part is returned as null.
   * }
   * }</pre>
   *
   * @return a multipart view of the current request
   * @throws MultipartException if an eager parser fails during adaptation;
   * with lazy parsing, the failure occurs when accessing part data
   * @see #isMultipart()
   * @since 4.0
   */
  MultipartRequest asMultipartRequest();

  // ---------------------------------------------------------------------
  // Async
  // ---------------------------------------------------------------------

  /**
   * Determine whether asynchronous handling is currently active.
   * A {@code false} result does not distinguish between handling that never
   * started and handling that has already ended.
   *
   * @return {@code true} if concurrent handling is under way
   * @see WebAsyncRequest#isAsyncStarted()
   * @since 4.0
   */
  boolean isConcurrentHandlingStarted();

  /**
   * Return the {@link WebAsyncRequest} associated with this exchange, creating
   * it on first access. Accessing it does not start asynchronous processing.
   *
   * @return the async request for this exchange
   * @see WebAsyncRequest#startAsync()
   */
  WebAsyncRequest asyncRequest();

  /**
   * Return the {@link WebAsyncManager} for this exchange, creating it on first access.
   * Accessing the manager does not start asynchronous processing.
   *
   * @return the manager for asynchronous handler results
   * @see #asyncRequest()
   */
  WebAsyncManager asyncManager();

  // ---------------------------------------------------------------------
  // lifecycle callback
  // ---------------------------------------------------------------------

  /**
   * Register a lifecycle callback under the given name for the specified phase.
   * A registration with the same phase and name replaces the previous callback.
   *
   * @param phase lifecycle phase
   * @param name the name of the callback
   * @param callback the callback to execute
   * @throws IllegalArgumentException if any argument is {@code null}
   * @since 5.0
   */
  void registerCallback(Lifecycle phase, String name, Runnable callback);

  /**
   * Remove a lifecycle callback for the given phase and name.
   *
   * @param phase lifecycle phase
   * @param name the name of the callback to remove
   * @since 5.0
   */
  void removeCallback(Lifecycle phase, String name);

  /**
   * Register the given callback to be executed after request completion.
   * <p>Each invocation registers the callback under a newly generated name.
   * Treat the returned name as an opaque registration identifier for use with
   * {@link #removeCallback(Lifecycle, String)}.
   *
   * @param callback the callback to run after request completion; must not be {@code null}
   * @return the name under which the callback was registered
   * @throws IllegalArgumentException if the callback is {@code null}
   * @since 5.0
   */
  default String registerCompletedCallback(Runnable callback) {
    return registerCallback(Lifecycle.COMPLETED, callback);
  }

  /**
   * Register a one-shot callback to be invoked just before the response
   * headers are written.
   *
   * <p>The callback runs while the response is being committed: the status
   * code, headers, content type, content length, and cookies may still be
   * changed. Do not register trailers, {@linkplain #reset() reset} the
   * response, or attempt to send a {@linkplain #sendRedirect(String) redirect}
   * or an {@linkplain #sendError(int, String) error response} from this callback.
   *
   * <p>The callback is removed before it runs and is invoked at most once. It
   * executes on the thread that commits the response: it must not block,
   * perform significant work, write large body chunks, or call {@link #flush()}.
   * If it throws, the commit fails and the exception is propagated to the
   * caller. If the response is never committed, the callback is never invoked;
   * callbacks registered after the response has been committed never run.
   *
   * <p>Each invocation registers the callback under a newly generated name.
   * Treat the returned name as an opaque registration identifier for use with
   * {@link #removeCallback(Lifecycle, String)}.
   *
   * @param callback the callback to execute; must not be {@code null}
   * @return the name under which the callback was registered
   * @throws IllegalArgumentException if the callback is {@code null}
   * @see Lifecycle#COMMITTING
   * @since 5.0
   */
  default String registerCommittingCallback(Runnable callback) {
    return registerCallback(Lifecycle.COMMITTING, callback);
  }

  /**
   * Register a callback to run after the response is committed.
   * At that point it is too late to change the response headers.
   * <p>Each invocation registers the callback under a newly generated name.
   * Treat the returned name as an opaque registration identifier for use with
   * {@link #removeCallback(Lifecycle, String)}.
   *
   * @param callback the callback to execute; must not be {@code null}
   * @return the name under which the callback was registered
   * @throws IllegalArgumentException if the callback is {@code null}
   * @see Lifecycle#COMMITTED
   * @since 5.0
   */
  default String registerCommittedCallback(Runnable callback) {
    return registerCallback(Lifecycle.COMMITTED, callback);
  }

  /**
   * Register a lifecycle callback under a newly generated name.
   * <p>Each invocation creates an independent registration, even when the same
   * callback object is supplied more than once. To replace an existing callback,
   * use {@link #registerCallback(Lifecycle, String, Runnable)} with an explicit name.
   * <p>Treat the returned name as an opaque registration identifier for use with
   * {@link #removeCallback(Lifecycle, String)}.
   *
   * @param phase lifecycle phase
   * @param callback the callback to execute
   * @return the name under which the callback was registered
   * @throws IllegalArgumentException if the phase or callback is {@code null}
   * @since 5.0
   */
  default String registerCallback(Lifecycle phase, Runnable callback) {
    Assert.notNull(phase, "Phase is required");
    Assert.notNull(callback, "Callback is required");
    String name = CallbackNameGenerator.nextName();
    registerCallback(phase, name, callback);
    return name;
  }

  //

  /**
   * Determine whether this is an {@code OPTIONS} request with both an
   * {@code Origin} and an {@code Access-Control-Request-Method} header.
   * This check does not determine whether the request is allowed by the CORS policy.
   *
   * @since 4.0
   */
  boolean isPreFlightRequest();

  /**
   * Determine whether the request has an {@code Origin} header that differs
   * from the request's own origin. This is not a CORS policy check.
   *
   * @since 4.0
   */
  boolean isCorsRequest();

  /**
   * Return the parsed {@code Content-Type} of the request body.
   *
   * @return the request media type, or {@code null} if the header is absent
   * @throws InvalidMediaTypeException if the header value cannot be parsed
   * @see HttpHeaders#getContentType()
   * @since 5.0
   */
  @Override
  @Nullable
  MediaType getContentType();

  /**
   * Return the request's {@code Content-Type} header without parsing it.
   *
   * @return the header value, including any media type parameters, or {@code null} if absent
   */
  @Override
  @Nullable
  String getContentTypeAsString();

  /**
   * Returns the HTTP headers associated with the current request. If the headers
   * have not been initialized yet, they will be created and cached for subsequent
   * calls to this method.
   *
   * <p>This method ensures that the headers are only created once, improving
   * performance by avoiding redundant operations.
   *
   * <p><strong>Example Usage:</strong>
   * <pre>{@code
   * HttpHeaders headers = requestHeaders();
   * headers.forEach((key, value) -> {
   *   System.out.println(key + ": " + value);
   * });
   * }</pre>
   *
   * @return the {@link HttpHeaders} object containing the request headers
   */
  HttpHeaders requestHeaders();

  /**
   * Return the client's preferred locale from the {@code Accept-Language}
   * header, without applying a default locale or consulting a {@link LocaleResolver}.
   * <p>Language ranges are ordered by descending quality value. Wildcard
   * ranges and ranges with zero quality are ignored. For full language
   * negotiation, use {@link HttpHeaders#getAcceptLanguage()} instead.
   * <p>This is a request preference, not the application's effective locale or
   * a guarantee that the application supports the language. Cookie and session
   * locale selections do not change this value. For message lookup, rendering,
   * and formatting, use {@link WebUtils#resolveLocale(ServerHttpExchange)}.
   *
   * @return the preferred locale, or {@code null} if the header is absent,
   * blank, or contains no acceptable concrete language range
   * @throws IllegalArgumentException if the header cannot be parsed
   * @see WebUtils#resolveLocale(ServerHttpExchange)
   * @since 4.0
   */
  @Nullable Locale getPreferredLocale();

  // checkNotModified

  /**
   * Return the outcome of a previous conditional request check without
   * re-evaluating the request headers.
   *
   * @return {@code true} if the check determined that normal processing should stop
   * @see #checkNotModified(String, long)
   */
  boolean isNotModified();

  /**
   * Evaluate request preconditions with a last-modified timestamp and no entity tag.
   *
   * @param lastModifiedTimestamp the resource's last-modified time in milliseconds since the Unix epoch
   * @return {@code true} if normal request processing should stop
   * @see #checkNotModified(String, long)
   * @since 4.0
   */
  default boolean checkNotModified(long lastModifiedTimestamp) {
    return checkNotModified(null, lastModifiedTimestamp);
  }

  /**
   * Evaluate request preconditions with an entity tag and no last-modified timestamp.
   *
   * @param etag the resource's entity tag, quoted if necessary by this method
   * @return {@code true} if normal request processing should stop
   * @see #checkNotModified(String, long)
   * @since 4.0
   */
  default boolean checkNotModified(String etag) {
    return checkNotModified(etag, -1);
  }

  /**
   * Evaluate conditional request headers against the resource's entity tag
   * and last-modified timestamp before generating a response or changing the resource.
   * <p>If the check sets {@code 304 Not Modified} or {@code 412 Precondition Failed},
   * stop normal processing. For GET and HEAD, this method may also populate the
   * {@code ETag} and {@code Last-Modified} response headers. Unparseable date
   * conditions are ignored.
   *
   * @param eTag the resource's entity tag, quoted if necessary, or {@code null} if unavailable
   * @param lastModifiedTimestamp the last-modified time in milliseconds since the
   * Unix epoch, or {@code -1} if unavailable
   * @return {@code true} if normal request processing should stop
   * @see #isNotModified()
   * @since 4.0
   */
  boolean checkNotModified(@Nullable String eTag, long lastModifiedTimestamp);

  /**
   * Set the {@code Content-Length} response header.
   *
   * @param length the response content length in bytes
   */
  default void setContentLength(long length) {
    responseHeaders().setContentLength(length);
  }

  /**
   * Returns a boolean indicating if the response has been committed. A committed
   * response has already had its status code and headers written.
   *
   * @return a boolean indicating if the response has been committed
   * @see #reset
   */
  boolean isCommitted();

  /**
   * Whether the request lifecycle has already been completed.
   * <p>Completion includes request-scoped cleanup, response finalization and
   * {@link Lifecycle#COMPLETED} callbacks, and happens at most once per request.
   *
   * @since 5.0
   */
  boolean isRequestCompleted();

  /**
   * Reset the uncommitted response, discarding its buffered body, headers,
   * cookies, and trailer registration and restoring the default status.
   * Obtain a new writer with {@link #getWriter()} if needed; do not reuse a
   * writer obtained before the reset.
   *
   * @throws IllegalStateException if the response has already been committed
   */
  void reset();

  /**
   * Send a redirect to the given location. The standard response uses
   * {@code 302 Found} and a {@code Location} header. A relative location need
   * not be converted to an absolute URL.
   * <p>Call this before committing the response and do not write more body data afterward.
   *
   * @param location the URI reference for the {@code Location} header
   * @throws IOException if writing the redirect fails
   * @throws IllegalStateException if the response is already committed
   */
  void sendRedirect(String location) throws IOException;

  /**
   * Send the given result using the configured response value handlers and
   * message converters, without requiring a mapped controller method.
   * <p>The result is passed unchanged to the configured return value handlers,
   * following their normal selection rules, including view-name handling for
   * strings. To send a string as a response body, wrap it in an
   * {@link infra.http.HttpEntity}. An {@code HttpEntity} supplies response headers
   * and a {@link infra.http.ResponseEntity} also supplies the status code.
   * A {@code null} result leaves the response unchanged.
   * <p>This method does not complete the request lifecycle or continue a filter
   * chain. A filter sending a short-circuit response should return without
   * invoking {@link FilterChain#doFilter(ServerHttpExchange)}. Returning from this
   * method does not guarantee that the client has received the response.
   *
   * @param result the response result, or {@code null} for no body
   * @throws IOException if writing the response fails with an I/O error
   * @throws IllegalStateException if the response is committed, the request is
   * completed, or a return value handler fails with another checked exception
   * @since 5.0
   */
  void send(@Nullable Object result) throws IOException;

  /**
   * Forward the request to a new path, re-dispatching through the handler
   * pipeline without involving filters.
   * <p>Similar to Servlet's {@code RequestDispatcher.forward()}.
   * The response buffer is cleared and the request path is updated before
   * re-dispatching.
   * <p>This is a convenience that delegates to
   * {@link DispatcherHandler#forward(AbstractServerHttpExchange, String)}.
   *
   * @param path the new path to forward to
   * @throws Exception if forwarding fails
   * @throws IllegalStateException if the response has already been committed
   * @see DispatcherHandler#forward(AbstractServerHttpExchange, String)
   * @since 5.0
   */
  void forward(String path) throws Exception;

  /**
   * Set the response status without invoking error handling or clearing the
   * response headers or cookies.
   *
   * @param sc the status code
   */
  void setStatus(int sc);

  /**
   * Set the numeric status code for this response.
   *
   * @param status the status
   */
  default void setStatus(HttpStatusCode status) {
    setStatus(status.value());
  }

  /**
   * Gets the current status code of this response.
   *
   * @return the current status code of this response
   */
  int getStatus();

  /**
   * Send an error response with the specified status code and no explicit message.
   *
   * @param code the error status code
   * @throws IOException if writing the error response fails
   * @throws IllegalStateException if the response is already committed
   * @see #sendError(int, String)
   * @since 4.0
   */
  default void sendError(HttpStatusCode code) throws IOException {
    sendError(code.value());
  }

  /**
   * Send an error response with the specified status code and optional message.
   *
   * @param code the error status code
   * @param msg the error message, or {@code null} to omit it
   * @throws IOException if writing the error response fails
   * @throws IllegalStateException if the response is already committed
   * @see #sendError(int, String)
   * @since 4.0
   */
  default void sendError(HttpStatusCode code, @Nullable String msg) throws IOException {
    sendError(code.value(), msg);
  }

  /**
   * Send an error response with the specified status code and no explicit message.
   *
   * @param sc the error status code
   * @throws IOException if writing the error response fails
   * @throws IllegalStateException if the response is already committed
   * @see #sendError(int, String)
   */
  void sendError(int sc) throws IOException;

  /**
   * Send an error response with the specified status code and optional message.
   * The server's error handling determines the body and whether to expose the
   * message. Do not assume the response is HTML, that existing headers or
   * cookies will be preserved, or that the response is committed on return.
   * <p>After calling this method, leave response rendering to the error handler.
   *
   * @param sc the error status code
   * @param msg the error message, or {@code null} to omit it
   * @throws IOException if writing the error response fails
   * @throws IllegalStateException if the response is already committed
   */
  void sendError(int sc, @Nullable String msg) throws IOException;

  /**
   * Returns a {@link OutputStream} suitable for writing binary data in the
   * response. The Server container does not encode the binary data.
   * <p>
   * Calling flush() on the {@link OutputStream} commits the response.
   * <p>
   * Use either this stream or {@link #getWriter()} to write the body. Do not
   * interleave byte writes with writes to a buffered character writer.
   *
   * @return a {@link OutputStream} for writing binary data
   * @throws IOException if an input or output exception occurred
   * @see #getWriter
   * @see #reset
   */
  @Override
  OutputStream getOutputStream() throws IOException;

  /**
   * Returns a <code>PrintWriter</code> object that can send character text to the
   * client. The writer uses the charset declared by the response
   * {@code Content-Type}, or UTF-8 when no charset is declared. Set the final
   * {@code Content-Type} before calling this method, since the charset is
   * resolved once when the writer is created.
   * <p>
   * Calling flush() on the <code>PrintWriter</code> commits the response.
   * <p>
   * Do not interleave writes to this writer with writes to {@link #getOutputStream()}.
   *
   * @return a <code>PrintWriter</code> object that can return character data to
   * the client
   * @throws IOException if an input or output exception occurred
   * @see #reset
   * @see PrintWriter#PrintWriter(OutputStream, boolean, Charset)
   * @see PrintWriter#flush()
   */
  @Override
  PrintWriter getWriter() throws IOException;

  /**
   * Set or remove the {@code Content-Type} response header before committing.
   * Set the final charset before obtaining the {@link #getWriter() writer};
   * changing the header does not change the charset of an existing writer.
   *
   * @param contentType the media type, optionally with a charset, or {@code null} to remove it
   */
  void setContentType(@Nullable String contentType);

  /**
   * Sets the content type of the response from a {@link MediaType}.
   * This is a convenience overload that delegates to
   * {@link #setContentType(String)} with the string representation
   * of the given media type.
   *
   * @param contentType the media type, or {@code null} to remove the header
   */
  default void setContentType(@Nullable MediaType contentType) {
    setContentType(contentType == null ? null : contentType.toString());
  }

  /**
   * Return the current {@code Content-Type} response header value.
   *
   * @return the header value, or {@code null} if absent
   * @see #getContentTypeAsString()
   */
  @Nullable String getResponseContentType();

  /**
   * Replace all values of the named response header with a single value,
   * or remove the header if the value is {@code null}.
   *
   * @param name the name of the header
   * @param value the header value, or {@code null} to remove the header
   * @see HttpHeaders#setOrRemove(String, String)
   * @since 4.0
   */
  default void setHeader(String name, @Nullable String value) {
    responseHeaders().setOrRemove(name, value);
  }

  /**
   * Set the supplied response headers, replacing existing values only for
   * the names present in {@code headers}.
   *
   * @param headers the headers to set; {@code null} or empty leaves the response unchanged
   * @since 5.0
   */
  default void setHeaders(@Nullable HttpHeaders headers) {
    responseHeaders().setAll(headers);
  }

  /**
   * Append a value to the named response header without replacing existing values.
   *
   * @param name the name of the header
   * @param value the header value
   * @see HttpHeaders#add(String, String)
   * @since 4.0
   */
  default void addHeader(String name, @Nullable String value) {
    responseHeaders().add(name, value);
  }

  /**
   * Append the supplied header values to the response headers.
   *
   * @param headers the headers to add; {@code null} or empty leaves the response unchanged
   * @since 3.0
   */
  default void addHeaders(@Nullable HttpHeaders headers) {
    if (HttpHeaders.isNotEmpty(headers)) {
      responseHeaders().addAll(headers);
    }
  }

  /**
   * Remove the named response header and all its values.
   *
   * @param name the header name
   * @return {@code true} if the header was present and removed
   */
  boolean removeHeader(String name);

  /**
   * Returns a boolean indicating whether the named
   * response header has already been set.
   *
   * @param name the header name
   * @return <code>true</code> if the named response header
   * has already been set; <code>false</code> otherwise
   * @since 4.0
   */
  boolean containsResponseHeader(String name);

  /**
   * Return the live response headers, not a copy. Make any changes before
   * the response is committed. The returned headers are not guaranteed to
   * be safe for concurrent access.
   *
   * @return the response headers (never {@code null})
   */
  HttpHeaders responseHeaders();

  /**
   * Register trailer fields to be evaluated after the response body has been
   * produced and before the terminating message is sent.
   *
   * <p>Each {@link Trailer} pairs a field name with a supplier for its value.
   * Names must be registered before the response headers are committed, are
   * copied defensively, and are compared case-insensitively. A later
   * registration replaces the previous one instead of merging with it;
   * multiple trailers with the same name produce multiple values for that
   * field. An empty array clears the registration without resetting other
   * response state.
   *
   * <p>Registration is rejected for HEAD requests, informational (1xx)
   * responses, status 204 or 304, and successful CONNECT responses. If the
   * final response changes to one of these states (including a protocol
   * upgrade), the registration is discarded without evaluating its suppliers.
   *
   * <p>Suppliers are invoked at most once, after the body has been produced
   * and only when the exchange completes normally. They run on the
   * response-completion or body-write-completion thread and must not perform
   * blocking I/O. Returning {@code null} omits that field for the current
   * response; an invalid value or an exception aborts the response instead of
   * sending partial trailers. Values may contain HTAB but no other control
   * characters, and must not contain DEL (0x7F).
   *
   * <p>Trailers are sent on a best-effort basis: proxies may strip them and
   * clients may ignore them. This is not a general-purpose streaming
   * completion signal.
   *
   * @param trailers the trailer fields to register, or an empty array to clear them
   * @throws IllegalStateException if the response has already been committed,
   * or a nonempty registration is attempted for HEAD, an informational (1xx)
   * response, status 204 or 304, or a successful CONNECT response
   * @throws IllegalArgumentException if the array or an element is {@code null},
   * or a name is empty, invalid, or forbidden
   * @throws UnsupportedOperationException if this response does not support trailers
   * @since 5.0
   */
  default void setTrailerFields(Trailer... trailers) {
    throw new UnsupportedOperationException("Response trailers are not supported");
  }

  /**
   * Adapt the response side of this exchange to a {@link ServerHttpResponse}
   * for message converters and other response writers. Writes affect the
   * same response, not a copy.
   *
   * @return a {@link ServerHttpResponse} view of this exchange
   */
  ServerHttpResponse asHttpOutputMessage();

  /**
   * Commit the response if necessary and flush buffered response data.
   * Returning from this method does not guarantee that the client has received it.
   * <p>Choose response framing before the first flush if more body data will
   * follow. With Netty HTTP/1.1, set {@code Transfer-Encoding: chunked} for
   * streaming when the total length is unknown; otherwise the first flush may
   * infer a {@code Content-Length} from the data buffered at that point. Do not
   * set {@code Transfer-Encoding: chunked} for HTTP/2.
   *
   * @throws IOException if flushing fails
   * @see #isCommitted
   * @see #reset
   */
  void flush() throws IOException;

  // ---------------------------------------------------------------------
  // MessageSource API
  // ---------------------------------------------------------------------

  /**
   * Return the MessageSource to use (typically the current ApplicationContext).
   *
   * @since 5.0
   */
  default MessageSource getMessageSource() {
    return getApplicationContext();
  }

  // ---------------------------------------------------------------------
  // Session API
  // ---------------------------------------------------------------------

  /**
   * Returns the current {@link Session} associated with this request,
   * or if the request does not have a session, creates one.
   *
   * @return the {@code Session} associated with this request
   * @see #getSession(boolean)
   * @see SessionManager
   * @since 5.0
   */
  default Session getSession() {
    return getSession(true);
  }

  /**
   * Returns the current {@link Session} associated with this request or,
   * if there is no current session and {@code create} is {@code true}, returns a new session.
   *
   * <p>If {@code create} is {@code false} and the request has no valid
   * {@link Session}, this method returns {@code null}.
   *
   * @param create {@code true} to create a new session for this request if
   * necessary; {@code false} to return {@code null} if there's no current session
   * @return the {@link Session} associated with this request or
   * {@code null} if {@code create} is {@code false} and the request has no valid session
   * @see #getSession()
   * @see SessionManager
   * @since 5.0
   */
  @Nullable Session getSession(boolean create);

  /**
   * Change the session id of the current session associated with this request and return the new session id.
   *
   * @return the new session id
   * @throws IllegalStateException if there is no session associated with the request
   * @see SessionManager
   * @since 5.0
   */
  default String changeSessionId() {
    Session session = getSession(false);
    if (session != null) {
      return session.changeSessionId();
    }
    throw new IllegalStateException("there is no session associated with the request");
  }

  /**
   * Return the ID of the current session without creating a session.
   *
   * @return the session id, or {@code null} if none
   */
  default @Nullable String getSessionId() {
    Session session = getSession(false);
    return session != null ? session.getId() : null;
  }

  /**
   * Return a session attribute without creating a session.
   *
   * @param name the name of the session attribute
   * @return the attribute value, or {@code null} if no session or attribute exists
   * @since 5.0
   */
  default @Nullable Object getSessionAttribute(String name) {
    Session session = getSession(false);
    return session != null ? session.getAttribute(name) : null;
  }

  /**
   * Return a required session attribute without creating a session.
   *
   * @param name the name of the session attribute
   * @return the attribute value (never {@code null})
   * @throws IllegalStateException if no session or attribute exists
   * @since 5.0
   */
  default Object getRequiredSessionAttribute(String name) {
    Object value = getSessionAttribute(name);
    if (value == null) {
      throw new IllegalStateException("No session attribute '" + name + "' found");
    }
    return value;
  }

  /**
   * Set a session attribute, creating a session if necessary for a non-null value.
   * <p>A {@code null} value removes the attribute from an existing session
   * without creating a session.
   *
   * @param name the name of the session attribute
   * @param value the attribute value, or {@code null} to remove it
   * @since 5.0
   */
  default void setSessionAttribute(String name, @Nullable Object value) {
    if (value != null) {
      getSession().setAttribute(name, value);
    }
    else {
      Session session = getSession(false);
      if (session != null) {
        session.removeAttribute(name);
      }
    }
  }

}
