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
import infra.context.MessageSourceResolvable;
import infra.context.NoSuchMessageException;
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
import infra.lang.Assert;
import infra.lang.TodayStrategies;
import infra.session.Session;
import infra.session.SessionManager;
import infra.util.CollectionUtils;
import infra.util.MultiValueMap;
import infra.util.StringUtils;
import infra.web.async.AsyncWebRequest;
import infra.web.async.WebAsyncManager;
import infra.web.context.annotation.RequestScope;
import infra.web.context.annotation.SessionScope;
import infra.web.multipart.MultipartRequest;
import infra.web.util.HtmlUtils;
import infra.web.util.WebUtils;

/**
 * Central API interface for an HTTP request/response context, providing access to request-related
 * information such as headers, cookies, query parameters, and other metadata, along with
 * methods for managing the response.
 *
 * <p>This interface defines the contract for handling HTTP requests and responses in a structured
 * manner, supporting features such as CORS, multipart requests, asynchronous processing,
 * conditional {@code checkNotModified} handling, and lifecycle callbacks.</p>
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 *   <li>Access to request and response headers, cookies, and query parameters.</li>
 *   <li>Support for multipart and asynchronous requests.</li>
 *   <li>Tracking of request processing time and completion status.</li>
 *   <li>Management of CORS and pre-flight requests.</li>
 *   <li>Integration with application context and dispatcher handler.</li>
 * </ul>
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see AbstractHttpContext
 * @since 5.0 2026/7/12 23:08
 */
public interface HttpContext extends InputStreamSource, OutputStreamSource, HttpInputMessage, HttpRequest {

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
  String FORWARD_REQUEST_URI_ATTRIBUTE = Conventions.getQualifiedAttributeName(HttpContext.class, "forward.requestUri");

  /**
   * Attribute name for the forwarded request marker.
   *
   * @see #forward(String)
   */
  String FORWARD_ATTRIBUTE = Conventions.getQualifiedAttributeName(HttpContext.class, "forward");

  /**
   * Flag indicating whether HTML escaping is enabled by default for message resolution.
   * This value is retrieved from the application's configuration using the key
   * "infra.web.default-html-escape". If the key is not found, the default value is {@code true}.
   *
   * @since 5.0
   */
  boolean defaultHtmlEscape = TodayStrategies.getFlag("infra.web.default-html-escape", false);

  HttpCookie[] EMPTY_COOKIES = {};

  /**
   * Lifecycle phases for response event callbacks.
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
   * Get start handling this request time millis
   *
   * @return start handling this request time millis
   * @since 4.0
   */
  long getRequestTimeMillis();

  /**
   * Get this request processing time millis
   *
   * @return this request processing time millis
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
   * Return the HTTP method of the request as a String value.
   *
   * @return the HTTP method as a plain String
   * @see #getMethod()
   */
  default String getMethodAsString() {
    return getMethod().name();
  }

  /**
   * Return the URI of the request (including a query string if any,
   * but only if it is well-formed for a URI representation).
   *
   * @return the URI of the request (never {@code null})
   */
  @Override
  URI getURI();

  /**
   * Returns the part of this request's URL from the protocol name up to the query
   * string in the first line of the HTTP request. The web container does not
   * decode this String. For example:
   *
   * <table summary="Examples of Returned Values">
   * <tr align=left>
   * <th>First line of HTTP request</th>
   * <th>Returned Value</th>
   * <tr>
   * <td>POST /some/path.html HTTP/1.1
   * <td>
   * <td>/some/path.html
   * <tr>
   * <td>GET http://foo.bar/a.html HTTP/1.0
   * <td>
   * <td>/a.html
   * <tr>
   * <td>HEAD /xyz?a=b HTTP/1.1
   * <td>
   * <td>/xyz
   * </table>
   *
   * @return a <code>String</code> containing the part of the URL from the
   * protocol name up to the query string
   */
  String getRequestURI();

  /**
   * Returns the {@code RequestPath} associated with the current request.
   * <p>
   * This method retrieves the {@code RequestPath} instance, initializing it if necessary.
   * If the {@code RequestPath} has not been previously set, it will be read using
   * the {@code readRequestPath()} method and cached for future use.
   *
   * @return the {@code RequestPath} instance associated with the current request.
   */
  RequestPath getRequestPath();

  /**
   * The returned URL contains a protocol, server name, port number, and server
   * path, but it does not include query string parameters.
   *
   * @return A URL
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
  String getQueryString();

  /**
   * Returns an array containing all of the <code>Cookie</code> objects the client
   * sent with this request. This method returns <code>null</code> if no cookies
   * were sent.
   *
   * @return an array of all the <code>Cookies</code> included with this request,
   * or {@link #EMPTY_COOKIES} if the request has no cookies
   */
  HttpCookie[] getCookies();

  /**
   * Returns a {@link HttpCookie} object the client sent with this request. This
   * method returns <code>null</code> if no target cookie were sent.
   *
   * @param name Cookie name
   * @return a {@link HttpCookie} object the client sent with this request. This
   * method returns <code>null</code> if no target cookie were sent.
   * @since 2.3.7
   */
  @Nullable
  HttpCookie getCookie(String name);

  /**
   * Returns a {@link Optional} HttpCookie the client sent with this request. This
   * method returns <code>Optional.empty</code> if no target cookie were sent.
   *
   * @param name Cookie name
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
   *   context.addCookie("sessionId", builder -> {
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
   * @throws IllegalArgumentException if the name is null or empty, or if the consumer is null
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
   * Removes all cookies with the specified name from the internal list of response cookies.
   * If no cookies match the given name, or if the internal cookie list is null,
   * the method will return null. Otherwise, it returns a list of removed cookies.
   *
   * <p>Example usage:
   * <pre>{@code
   *   // Assuming responseCookies contains: [cookie1(name="id"), cookie2(name="session")]
   *   List<HttpCookie> removed = removeCookie("id");
   *
   *   // After execution:
   *   // removed contains: [cookie1(name="id")]
   *   // responseCookies now contains: [cookie2(name="session")]
   * }</pre>
   *
   * @param name the name of the cookies to be removed; must not be null
   * @return a list of {@link HttpCookie} objects that were removed, or null
   * if no cookies were found or the internal cookie list is null
   */
  @Nullable
  List<ResponseCookie> removeCookie(String name);

  /**
   * Checks if there are any response cookies available.
   *
   * <p>This method returns true if the {@code responseCookies} object is not null,
   * indicating that there are cookies present in the response. Otherwise, it returns false.
   *
   * <p><b>Example Usage:</b>
   * <pre>{@code
   * if (httpRequest.hasResponseCookie()) {
   *   // Process the response cookies
   *   System.out.println("Response contains cookies.");
   * }
   * else {
   *   // Handle the case where no cookies are present
   *   System.out.println("No cookies in the response.");
   * }
   * }</pre>
   *
   * @return true if response cookies are present, false otherwise
   */
  boolean hasResponseCookie();

  /**
   * Returns the list of response cookies associated with this object. If no
   * cookies have been set previously, an empty list is initialized and returned.
   *
   * <p>This method ensures that a non-null list is always available for use.
   * The returned list can be modified directly to add or remove cookies.
   *
   * <p><strong>Example Usage:</strong>
   * <pre>{@code
   *   // Retrieve the response cookies
   *   List<HttpCookie> cookies = responseCookies();
   *
   *   // Add a new cookie to the list
   *   cookies.add(new HttpCookie("sessionId", "12345"));
   *
   *   // Iterate through the cookies
   *   for (HttpCookie cookie : responseCookies()) {
   *     System.out.println("Cookie Name: " + cookie.getName());
   *     System.out.println("Cookie Value: " + cookie.getValue());
   *   }
   * }</pre>
   *
   * @return a modifiable list of {@link HttpCookie} objects representing the
   * response cookies. If no cookies exist, an empty list is returned.
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
   * @return an array of <code>String</code> objects containing the parameter's
   * values
   * @see #getParameters()
   */
  default String @Nullable [] getParameters(String name) {
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
   * {@link #getParameters(String)}.
   *
   * <p>
   * If you use this method with a multivalued parameter, the value returned is
   * equal to the first value in the array returned by
   * <code>parameters(String)</code>.
   *
   * <p>
   * If the parameter data was sent in the request body, such as occurs with an
   * HTTP POST request, then reading the body directly via {@link #getInputStream}
   * or {@link #getReader} can interfere with the execution of this method.
   *
   * @param name a <code>String</code> specifying the name of the parameter
   * @return a <code>String</code> representing the single value of the parameter
   * @see #getParameters(String)
   */
  default @Nullable String getParameter(String name) {
    List<String> list = getParameters().get(name);
    return CollectionUtils.firstElement(list);
  }

  /**
   * Returns the length, in bytes, of the request body and made available by the
   * input stream, or -1 if the length is not known.
   *
   * @return a long containing the length of the request body or -1L if the length
   * is not known
   */
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
   * Retrieves the body of the request as binary data using a {@link InputStream}.
   * Either this method or {@link #getReader} may be called to read the body, not
   * both.
   *
   * @return a {@link InputStream} object containing the body of the request
   * @throws IllegalStateException If the {@link #getReader} method has
   * already been called for this request
   * @throws IOException if an input or output exception occurred
   */
  @Override
  InputStream getInputStream() throws IOException;

  /**
   * Retrieves the body of the request as character data using a
   * <code>BufferedReader</code>. The reader translates the character data
   * according to the character encoding used on the body. Either this method or
   * {@link #getInputStream} may be called to read the body, not both.
   *
   * @return a <code>BufferedReader</code> containing the body of the request
   * @throws IllegalStateException if {@link #getInputStream} method has
   * been called on this request
   * @throws IOException if an input or output exception occurred
   * @see #getInputStream
   */
  @Override
  BufferedReader getReader() throws IOException;

  // -----------------------------------------------------

  /**
   * Determines whether the current request is a multipart request.
   * <p>
   * This method checks if the request's content type starts with "multipart/".
   * For GET and HEAD requests, it automatically returns false as these methods
   * do not support multipart content. The result is cached internally to avoid
   * redundant computations for subsequent calls.
   *
   * @return true if the request is a multipart request, false otherwise.
   */
  boolean isMultipart();

  /**
   * Returns the {@code MultipartRequest} instance associated with the current object.
   * If no instance exists, it creates one using the {@code createMultipartRequest()}
   * method and caches it for future use.
   *
   * <p>This method ensures that only one {@code MultipartRequest} instance is created
   * and reused, following the lazy initialization pattern.
   *
   * <p><b>Example Usage:</b>
   *
   * <pre>{@code
   *   // Assuming 'handler' is an instance of the class containing this method
   *   MultipartRequest request = multipartRequest();
   *
   *   // Use the request object to process multipart data
   *   String fileData = request.get*("fileKey");
   * }</pre>
   *
   * @return the cached or newly created {@code MultipartRequest} instance
   * @throws MultipartException if parsing fails
   * @see #isMultipart()
   * @since 4.0
   */
  MultipartRequest asMultipartRequest();

  // ---------------------------------------------------------------------
  // Async
  // ---------------------------------------------------------------------

  /**
   * Whether the selected handler for the current request chose to handle the
   * request asynchronously. A return value of "true" indicates concurrent
   * handling is under way and the response will remain open. A return value
   * of "false" means concurrent handling was either not started or possibly
   * that it has completed and the request was dispatched for further
   * processing of the concurrent result.
   *
   * @since 4.0
   */
  boolean isConcurrentHandlingStarted();

  /**
   * Returns the current {@code AsyncWebRequest} instance associated with this context.
   * If no asynchronous web request has been initialized yet, this method will create
   * a new instance using {@code createAsyncWebRequest()} and store it for future use.
   *
   * <p>Example usage:
   * <pre>{@code
   *   // Retrieve the async web request
   *   AsyncWebRequest asyncRequest = context.asyncWebRequest();
   *
   *   // Perform operations on the async request
   *   asyncRequest.setAttribute("key", "value");
   *   String value = (String) asyncRequest.getAttribute("key");
   * }</pre>
   *
   * @return the current {@code AsyncWebRequest} instance, or a newly created instance
   * if none has been initialized yet
   */
  AsyncWebRequest asyncWebRequest();

  /**
   * Returns the {@code WebAsyncManager} associated with this instance, lazily
   * initializing it if necessary. If the internal {@code WebAsyncManager} has not
   * been created yet, this method will invoke {@code createWebAsyncManager()} to
   * initialize it and store it for future use.
   *
   * <p>Example usage:
   * <pre>{@code
   *   // Retrieve the WebAsyncManager instance
   *   WebAsyncManager asyncManager = asyncManager();
   *
   *   // Use the WebAsyncManager to manage asynchronous operations
   *   asyncManager.startCallableProcessing(() -> {
   *     // Perform some asynchronous task
   *     return "Task Completed";
   *   });
   * }</pre>
   *
   * @return the {@code WebAsyncManager} instance associated with this object,
   * ensuring it is initialized before returning
   */
  WebAsyncManager asyncManager();

  // ---------------------------------------------------------------------
  // lifecycle callback
  // ---------------------------------------------------------------------

  /**
   * Add a lifecycle callback for the given phase.
   *
   * @param phase lifecycle phase
   * @param name the name of the callback
   * @param callback the callback to execute
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
   * <p>The callback is registered under a name derived from the callback object
   * using {@link Conventions#getVariableName(Object)}.
   *
   * @param callback the callback to be executed for destruction; must not be {@code null}
   * @return the name under which the callback was registered
   * @throws IllegalArgumentException if the callback is {@code null}
   * @since 5.0
   */
  default String registerCompletedCallback(Runnable callback) {
    return registerCallback(Lifecycle.COMPLETED, callback);
  }

  /**
   * Register a callback to be invoked just before the response headers
   * are committed. At this point, the status code and headers can still
   * be modified.
   *
   * @param callback the callback to execute; must not be {@code null}
   * @since 5.0
   */
  default String registerCommittingCallback(Runnable callback) {
    return registerCallback(Lifecycle.COMMITTING, callback);
  }

  /**
   * Register a callback to be invoked after the response has been
   * committed. At this point, headers can no longer be modified.
   *
   * @param callback the callback to execute; must not be {@code null}
   * @since 5.0
   */
  default String registerCommittedCallback(Runnable callback) {
    return registerCallback(Lifecycle.COMMITTED, callback);
  }

  /**
   * Add a lifecycle callback for the given phase.
   *
   * @param phase lifecycle phase
   * @param callback the callback to execute
   * @since 5.0
   */
  default String registerCallback(Lifecycle phase, Runnable callback) {
    Assert.notNull(callback, "Callback is required");
    String variableName = Conventions.getVariableName(callback);
    registerCallback(phase, variableName, callback);
    return variableName;
  }

  //

  /**
   * Returns {@code true} if the request is a valid CORS pre-flight
   * one by checking {code OPTIONS} method with {@code Origin} and
   * {@code Access-Control-Request-Method} headers presence.
   *
   * @since 4.0
   */
  boolean isPreFlightRequest();

  /**
   * Returns {@code true} if the request is a valid CORS one by checking
   * {@code Origin}header presence and ensuring that origins are different.
   *
   * @since 4.0
   */
  boolean isCorsRequest();

  /**
   * Return the media type of the request body, or {@code null} if the
   * media type is not known or cannot be parsed from the Content-Type header.
   * <p>This method retrieves the Content-Type header from the request headers
   * and attempts to parse it into a {@link MediaType} object. If the header
   * is not present or cannot be parsed, this method returns {@code null}.
   *
   * @return the {@link MediaType} of the request body, or {@code null} if
   * the media type is unknown or unparseable
   * @throws InvalidMediaTypeException if the media type value cannot be parsed
   * @see HttpHeaders#getContentType()
   * @since 5.0
   */
  @Override
  @Nullable
  MediaType getContentType();

  /**
   * Returns the MIME type of the body of the request, or <code>null</code> if the
   * type is not known.
   *
   * @return a <code>String</code> containing the name of the MIME type of the
   * request, or null if the type is not known
   */
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
   * Returns the preferred <code>Locale</code> that the client will
   * accept content in, based on the Accept-Language header. If the
   * client request doesn't provide an Accept-Language header, this
   * method returns the default locale for the server.
   *
   * @return the preferred <code>Locale</code> for the client
   * @since 4.0
   */
  Locale getLocale();

  // checkNotModified

  /**
   * Checks if the current object has not been modified.
   *
   * <p>This method returns the value of the internal flag {@code notModified},
   * which indicates whether the object remains in its original state since
   * it was last reset or initialized.
   *
   * @return {@code true} if the object has not been modified,
   * {@code false} otherwise.
   */
  boolean isNotModified();

  /**
   * Check whether the requested resource has been modified given the
   * supplied last-modified timestamp (as determined by the application).
   * <p>This will also transparently set the "Last-Modified" response header
   * and HTTP status when applicable.
   * <p>Typical usage:
   * <pre>{@code
   * public String myHandleMethod(HttpContext request, Model model) {
   *   long lastModified = // application-specific calculation
   *   if (request.checkNotModified(lastModified)) {
   *     // shortcut exit - no further processing necessary
   *     return null;
   *   }
   *   // further request processing, actually building content
   *   model.addAttribute(...);
   *   return "myViewName";
   * }
   * }</pre>
   * <p>This method works with conditional GET/HEAD requests, but
   * also with conditional POST/PUT/DELETE requests.
   * <p><strong>Note:</strong> you can use either
   * this {@code #checkNotModified(long)} method; or
   * {@link #checkNotModified(String)}. If you want enforce both
   * a strong entity tag and a Last-Modified value,
   * as recommended by the HTTP specification,
   * then you should use {@link #checkNotModified(String, long)}.
   * <p>If the "If-Modified-Since" header is set but cannot be parsed
   * to a date value, this method will ignore the header and proceed
   * with setting the last-modified timestamp on the response.
   *
   * @param lastModifiedTimestamp the last-modified timestamp in
   * milliseconds that the application determined for the underlying
   * resource
   * @return whether the request qualifies as not modified,
   * allowing to abort request processing and relying on the response
   * telling the client that the content has not been modified
   * @since 4.0
   */
  default boolean checkNotModified(long lastModifiedTimestamp) {
    return checkNotModified(null, lastModifiedTimestamp);
  }

  /**
   * Check whether the requested resource has been modified given the
   * supplied {@code ETag} (entity tag), as determined by the application.
   * <p>This will also transparently set the "ETag" response header
   * and HTTP status when applicable.
   * <p>Typical usage:
   * <pre>{@code
   * public String myHandleMethod(HttpContext request, Model model) {
   *   String eTag = // application-specific calculation
   *   if (request.checkNotModified(eTag)) {
   *     // shortcut exit - no further processing necessary
   *     return null;
   *   }
   *   // further request processing, actually building content
   *   model.addAttribute(...);
   *   return "myViewName";
   * }
   * }</pre>
   * <p><strong>Note:</strong> you can use either
   * this {@code #checkNotModified(String)} method; or
   * {@link #checkNotModified(long)}. If you want enforce both
   * a strong entity tag and a Last-Modified value,
   * as recommended by the HTTP specification,
   * then you should use {@link #checkNotModified(String, long)}.
   *
   * @param etag the entity tag that the application determined
   * for the underlying resource. This parameter will be padded
   * with quotes (") if necessary.
   * @return true if the request does not require further processing.
   * @since 4.0
   */
  default boolean checkNotModified(String etag) {
    return checkNotModified(etag, -1);
  }

  /**
   * Check whether the requested resource has been modified given the
   * supplied {@code ETag} (entity tag) and last-modified timestamp,
   * as determined by the application.
   * <p>This will also transparently set the "ETag" and "Last-Modified"
   * response headers, and HTTP status when applicable.
   * <p>Typical usage:
   * <pre>{@code
   * public String myHandleMethod(HttpContext request, Model model) {
   *   String eTag = // application-specific calculation
   *   long lastModified = // application-specific calculation
   *   if (request.checkNotModified(eTag, lastModified)) {
   *     // shortcut exit - no further processing necessary
   *     return null;
   *   }
   *   // further request processing, actually building content
   *   model.addAttribute(...);
   *   return "myViewName";
   * }
   * }</pre>
   * <p>This method works with conditional GET/HEAD requests, but
   * also with conditional POST/PUT/DELETE requests.
   * <p><strong>Note:</strong> The HTTP specification recommends
   * setting both ETag and Last-Modified values, but you can also
   * use {@code #checkNotModified(String)} or
   * {@link #checkNotModified(long)}.
   *
   * @param eTag the entity tag that the application determined
   * for the underlying resource. This parameter will be padded
   * with quotes (") if necessary.
   * @param lastModifiedTimestamp the last-modified timestamp in
   * milliseconds that the application determined for the underlying
   * resource
   * @return true if the request does not require further processing.
   * @since 4.0
   */
  boolean checkNotModified(@Nullable String eTag, long lastModifiedTimestamp);

  /**
   * Sets the matching metadata for this handler. The matching metadata provides
   * information about how a handler is matched to a specific request or context.
   * This method allows setting or updating the metadata, which can be null if no
   * matching information is available.
   *
   * @param handlerMatchingMetadata the metadata to set, or null if no metadata is available
   */
  void setMatchingMetadata(@Nullable HandlerMatchingMetadata handlerMatchingMetadata);

  /**
   * Returns the {@code HandlerMatchingMetadata} associated with this instance,
   * if available. This metadata typically contains information about how a
   * handler matches certain criteria, such as request mappings in a web
   * application context.
   *
   * <p>If no matching metadata has been set, this method will return
   * {@code null}. Ensure proper null checks when using the returned value.</p>
   *
   * @return the {@code HandlerMatchingMetadata} associated with this instance,
   * or {@code null} if no metadata is available
   */
  @Nullable
  HandlerMatchingMetadata getMatchingMetadata();

  /**
   * Returns the {@code HandlerMatchingMetadata} associated with this instance.
   *
   * <p>This method retrieves the metadata that describes how a handler matches
   * a specific request. If the metadata is not set, an exception is thrown to
   * indicate that it is required.
   *
   * @return the {@code HandlerMatchingMetadata} instance associated with this handler
   * @throws IllegalStateException if the {@code HandlerMatchingMetadata} is not set
   */
  HandlerMatchingMetadata matchingMetadata();

  boolean hasMatchingMetadata();

  /**
   * Sets the binding context for this component. The binding context is used to
   * manage data bindings between the component and its associated data model.
   *
   * <p>If {@code null} is passed, any existing binding context will be cleared.</p>
   *
   * @param bindingContext the {@link BindingContext} to set, or {@code null} to clear
   * the current binding context
   */
  void setBinding(@Nullable BindingContext bindingContext);

  /**
   * Checks if the current instance has a binding context established.
   *
   * This method evaluates whether the {@code bindingContext} is initialized
   * (i.e., not null). It is typically used to determine if the object is ready
   * for binding-related operations.
   *
   * @return {@code true} if the {@code bindingContext} is not null, indicating
   * that a binding context exists; {@code false} otherwise.
   * @since 4.0
   */
  boolean hasBinding();

  /**
   * Returns the current {@link BindingContext} associated with this instance.
   *
   * <p>This method provides access to the binding context, which may be used to manage
   * data bindings or retrieve information about the current binding state. If no binding
   * context is set, the method will return {@code null}.
   *
   * <p><strong>Example Usage:</strong>
   * <pre>{@code
   *   // Retrieve the binding context
   *   BindingContext context = getBinding();
   *
   *   if (context != null) {
   *     // Perform operations with the binding context
   *     System.out.println("Binding context found: " + context);
   *   }
   *   else {
   *     System.out.println("No binding context is currently set.");
   *   }
   * }</pre>
   *
   * @return the current {@link BindingContext}, or {@code null} if no binding context
   * is associated with this instance
   * @since 4.0
   */
  @Nullable
  BindingContext getBinding();

  /**
   * Returns the {@link BindingContext} associated with this instance.
   *
   * <p>This method retrieves the binding context, ensuring that it has been
   * initialized. If the binding context is not set, an exception is thrown
   * to indicate that a valid context is required.</p>
   *
   * @return the non-null {@link BindingContext} instance associated with this object
   * @throws IllegalStateException if BindingContext is not set
   * @since 4.0
   */
  BindingContext binding();

  /**
   * Return read-only "input" flash attributes from request before redirect.
   *
   * @return a RedirectModel, or {@code null} if not found
   * @see RedirectModel
   */
  default @Nullable RedirectModel getInputRedirectModel() {
    return getInputRedirectModel(null);
  }

  /**
   * Return read-only "input" flash attributes from request before redirect.
   *
   * @param manager RedirectModelManager manage RedirectModel
   * @return a RedirectModel, or {@code null} if not found
   * @see RedirectModel
   */
  @Nullable
  RedirectModel getInputRedirectModel(@Nullable RedirectModelManager manager);

  /**
   * Sets the length of the content body in the response , this method sets the
   * HTTP Content-Length header.
   *
   * @param length an long specifying the length of the content being returned to the
   * client; sets the Content-Length header
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
   * Clears any data that exists in the buffer as well as the status code,
   * headers. The state of calling {@link #getWriter} or {@link #getOutputStream}
   * is also cleared. It is legal, for instance, to call {@link #getWriter},
   * reset() and then {@link #getOutputStream}. If {@link #getWriter} or
   * {@link #getOutputStream} have been called before this method, then the
   * corresponding returned Writer or OutputStream will be staled and the
   * behavior of using the stale object is undefined. If the response has been
   * committed, this method throws an <code>IllegalStateException</code>.
   *
   * @throws IllegalStateException if the response has already been committed
   */
  void reset();

  /**
   * Sends a temporary redirect response to the client using the specified
   * redirect location URL and clears the buffer. The buffer will be replaced with
   * the data set by this method. Calling this method sets the status code to 302
   * (Found). This method can accept relative URLs;the Web container must
   * convert the relative URL to an absolute URL before sending the response to
   * the client. If the location is relative without a leading '/' the container
   * interprets it as relative to the current request URI. If the location is
   * relative with a leading '/' the container interprets it as relative to the
   * web container root. If the location is relative with two leading '/' the
   * container interprets it as a network-path reference (see
   * <a href="http://www.ietf.org/rfc/rfc3986.txt"> RFC 3986: Uniform Resource
   * Identifier (URI): Generic Syntax</a>, section 4.2 &quot;Relative
   * Reference&quot;).
   *
   * <p>
   * If the response has already been committed, this method throws an
   * IllegalStateException. After using this method, the response should be
   * considered to be committed and should not be written to.
   *
   * @param location the redirect location URL
   * @throws IOException If an input or output exception occurs
   * @throws IllegalStateException If the response was committed or if a partial URL is given and
   * cannot be converted into a valid URL
   */
  void sendRedirect(String location) throws IOException;

  /**
   * Forward the request to a new path, re-dispatching through the handler
   * pipeline without involving filters.
   * <p>Similar to Servlet's {@code RequestDispatcher.forward()}.
   * The response buffer is cleared and the request path is updated before
   * re-dispatching.
   * <p>This is a convenience that delegates to
   * {@link DispatcherHandler#forward(AbstractHttpContext, String)}.
   *
   * @param path the new path to forward to
   * @throws Exception if forwarding fails
   * @throws IllegalStateException if the response has already been committed
   * @see DispatcherHandler#forward(AbstractHttpContext, String)
   * @since 5.0
   */
  void forward(String path) throws Exception;

  /**
   * Sets the status code for this response.
   *
   * <p>
   * This method is used to set the return status code when there is no error .
   * <p>
   * This method preserves any cookies and other response headers.
   * <p>
   * Valid status codes are those in the 2XX, 3XX, 4XX, and 5XX ranges. Other
   * status codes are treated as container specific.
   *
   * @param sc the status code
   */
  void setStatus(int sc);

  /**
   * Sets the status code and message for this response.
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
   * Sends an error response to the client using the specified status code and
   * clears the buffer.
   * <p>
   * The server will preserve cookies and may clear or update any headers needed
   * to serve the error page as a valid response.
   * <p>
   * If an error-page declaration has been made for the web application
   * corresponding to the status code passed in, it will be served back the error
   * page
   *
   * <p>
   * If the response has already been committed, this method throws an
   * IllegalStateException. After using this method, the response should be
   * considered to be committed and should not be written to.
   *
   * @param code the error status code
   * @throws IOException If an input or output exception occurs
   * @throws IllegalStateException If the response was committed before this method call
   * @since 4.0
   */
  default void sendError(HttpStatusCode code) throws IOException {
    sendError(code.value());
  }

  /**
   * <p>
   * Sends an error response to the client using the specified status and clears
   * the buffer. The server defaults to creating the response to look like an
   * HTML-formatted server error page containing the specified message, setting
   * the content type to "text/html". The caller is <strong>not</strong>
   * responsible for escaping or re-encoding the message to ensure it is safe with
   * respect to the current response encoding and content type. This aspect of
   * safety is the responsibility of the container, as it is generating the error
   * page containing the message. The server will preserve cookies and may clear
   * or update any headers needed to serve the error page as a valid response.
   * </p>
   *
   * <p>
   * If an error-page declaration has been made for the web application
   * corresponding to the status code passed in, it will be served back in
   * preference to the suggested msg parameter and the msg parameter will be
   * ignored.
   * </p>
   *
   * <p>
   * If the response has already been committed, this method throws an
   * IllegalStateException. After using this method, the response should be
   * considered to be committed and should not be written to.
   *
   * @param code the error status code
   * @param msg the descriptive message
   * @throws IOException If an input or output exception occurs
   * @throws IllegalStateException If the response was committed
   * @since 4.0
   */
  default void sendError(HttpStatusCode code, @Nullable String msg) throws IOException {
    sendError(code.value(), msg);
  }

  /**
   * Sends an error response to the client using the specified status code and
   * clears the buffer.
   * <p>
   * The server will preserve cookies and may clear or update any headers needed
   * to serve the error page as a valid response.
   * <p>
   * If an error-page declaration has been made for the web application
   * corresponding to the status code passed in, it will be served back the error
   * page
   *
   * <p>
   * If the response has already been committed, this method throws an
   * IllegalStateException. After using this method, the response should be
   * considered to be committed and should not be written to.
   *
   * @param sc the error status code
   * @throws IOException If an input or output exception occurs
   * @throws IllegalStateException If the response was committed before this method call
   */
  void sendError(int sc) throws IOException;

  /**
   * <p>
   * Sends an error response to the client using the specified status and clears
   * the buffer. The server defaults to creating the response to look like an
   * HTML-formatted server error page containing the specified message, setting
   * the content type to "text/html". The caller is <strong>not</strong>
   * responsible for escaping or re-encoding the message to ensure it is safe with
   * respect to the current response encoding and content type. This aspect of
   * safety is the responsibility of the container, as it is generating the error
   * page containing the message. The server will preserve cookies and may clear
   * or update any headers needed to serve the error page as a valid response.
   * </p>
   *
   * <p>
   * If an error-page declaration has been made for the web application
   * corresponding to the status code passed in, it will be served back in
   * preference to the suggested msg parameter and the msg parameter will be
   * ignored.
   * </p>
   *
   * <p>
   * If the response has already been committed, this method throws an
   * IllegalStateException. After using this method, the response should be
   * considered to be committed and should not be written to.
   *
   * @param sc the error status code
   * @param msg the descriptive message
   * @throws IOException If an input or output exception occurs
   * @throws IllegalStateException If the response was committed
   */
  void sendError(int sc, @Nullable String msg) throws IOException;

  /**
   * Returns a {@link OutputStream} suitable for writing binary data in the
   * response. The Server container does not encode the binary data.
   * <p>
   * Calling flush() on the {@link OutputStream} commits the response.
   * <p>
   * Either this method or {@link #getWriter} may be called to write the body, not
   * both, except when {@link #reset} has been called.
   *
   * @return a {@link OutputStream} for writing binary data
   * @throws IllegalStateException if the <code>getWriter</code> method
   * has been called on this response
   * @throws IOException if an input or output exception occurred
   * @see #getWriter
   * @see #reset
   */
  @Override
  OutputStream getOutputStream() throws IOException;

  /**
   * Returns a <code>PrintWriter</code> object that can send character text to the
   * client. The <code>PrintWriter</code> uses the UTF-8 character encoding.
   * <p>
   * Calling flush() on the <code>PrintWriter</code> commits the response.
   * <p>
   * Either this method or {@link #getOutputStream} may be called to write the
   * body, not both, except when {@link #reset} has been called.
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
   * Sets the content type of the response being sent to the client, if the
   * response has not been committed yet. The given content type may include a
   * character encoding specification, for example,
   * <code>text/html;charset=UTF-8</code>. The response's character encoding is
   * only set from the given content type if this method is called before
   * <code>getWriter</code> is called.
   * <p>
   * This method may be called repeatedly to change content type and character
   * encoding. This method has no effect if called after the response has been
   * committed. It does not set the response's character encoding if it is called
   * after <code>getWriter</code> has been called or after the response has been
   * committed.
   * <p>
   * Containers must communicate the content type and the character encoding used
   * for the http response's writer to the client if the protocol provides a
   * way for doing so. In the case of HTTP, the <code>Content-Type</code> header
   * is used.
   *
   * @param contentType a <code>String</code> specifying the MIME type of the content
   */
  void setContentType(@Nullable String contentType);

  /**
   * Sets the content type of the response being sent to the client, if the
   * response has not been committed yet. The given content type may include a
   * character encoding specification, for example,
   * <code>text/html;charset=UTF-8</code>. The response's character encoding is
   * only set from the given content type if this method is called before
   * <code>getWriter</code> is called.
   * <p>
   * This method may be called repeatedly to change content type and character
   * encoding. This method has no effect if called after the response has been
   * committed. It does not set the response's character encoding if it is called
   * after <code>getWriter</code> has been called or after the response has been
   * committed.
   * <p>
   * Containers must communicate the content type and the character encoding used
   * for the http response's writer to the client if the protocol provides a
   * way for doing so. In the case of HTTP, the <code>Content-Type</code> header
   * is used.
   *
   * @param contentType a <code>String</code> specifying the MIME type of the content
   */
  default void setContentType(@Nullable MediaType contentType) {
    setContentType(contentType == null ? null : contentType.toString());
  }

  /**
   * Returns the content type used for the MIME body sent in this response.
   * The content type proper must have been specified using {@link #setContentType}
   * before the response is committed. If no content type has been specified, this
   * method returns null.
   *
   * @return a <code>String</code> specifying the content type, for example,
   * <code>text/html; charset=UTF-8</code>, or
   * null
   * @see #getContentTypeAsString()
   */
  @Nullable String getResponseContentType();

  /**
   * Sets a response header with the given name and value. If the
   * header had already been set, the new value overwrites the
   * previous one.
   *
   * @param name the name of the header
   * @param value the header value If it contains octet string,
   * it should be encoded according to RFC 2047
   * (<a href="http://www.ietf.org/rfc/rfc2047.txt">RFC 2047</a>)
   * @see HttpHeaders#setOrRemove
   * @since 4.0
   */
  default void setHeader(String name, @Nullable String value) {
    responseHeaders().setOrRemove(name, value);
  }

  /**
   * Replace all headers
   *
   * @since 5.0
   */
  default void setHeaders(@Nullable HttpHeaders headers) {
    responseHeaders().setAll(headers);
  }

  /**
   * Add a response header with the given name and value.
   *
   * @param name the name of the header
   * @param value the header value If it contains octet string,
   * it should be encoded according to RFC 2047
   * (<a href="http://www.ietf.org/rfc/rfc2047.txt">RFC 2047</a>)
   * @see HttpHeaders#add(String, String)
   * @since 4.0
   */
  default void addHeader(String name, @Nullable String value) {
    responseHeaders().add(name, value);
  }

  /**
   * merge headers to response http-headers
   *
   * @since 3.0
   */
  default void addHeaders(@Nullable HttpHeaders headers) {
    if (HttpHeaders.isNotEmpty(headers)) {
      responseHeaders().addAll(headers);
    }
  }

  /**
   * Removes the header with the specified name from the response headers.
   *
   * <p>This method checks if the {@code responseHeaders} map is not null and then
   * removes the header entry associated with the provided name. If the header does
   * not exist or {@code responseHeaders} is null, no action is taken.</p>
   *
   * <p>Example usage:</p>
   *
   * <pre>{@code
   *   // Assume responseHeaders contains {"Content-Type": "text/plain", "Authorization": "Bearer token"}
   *   removeHeader("Authorization");
   *
   *   // After the call, responseHeaders will contain:
   *   // {"Content-Type": "text/plain"}
   * }</pre>
   *
   * @param name the name of the header to be removed
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
   * Returns the HTTP response headers associated with this instance.
   * If the headers have not been initialized yet, they will be created
   * and cached for subsequent calls.
   *
   * <p>This method ensures that the headers are only created once, providing
   * a thread-safe and efficient way to access the response headers.</p>
   *
   * <p>Example usage:</p>
   *
   * <pre>{@code
   *   // Retrieve the response headers
   *   HttpHeaders headers = response.responseHeaders();
   *
   *   // Add a custom header to the response
   *   headers.add("Custom-Header", "HeaderValue");
   *
   *   // Check if a specific header exists
   *   if (headers.containsKey("Custom-Header")) {
   *     System.out.println("Custom-Header is present");
   *   }
   * }</pre>
   *
   * @return the {@link HttpHeaders} object representing the response headers.
   * This will never be {@code null}.
   */
  HttpHeaders responseHeaders();

  ServerHttpResponse asHttpOutputMessage();

  /**
   * Forces any content in the buffer to be written to the client.  A call
   * to this method automatically commits the response, meaning the status
   * code and headers will be written. Ensure that the headers and the content
   * of the response are written out.
   * <p>After the first flush, headers can no longer be changed.
   * Only further content writing and content flushing is possible.
   *
   * @throws IOException if the act of flushing the buffer cannot be completed.
   * @see #isCommitted
   * @see #reset
   */
  void flush() throws IOException;

  // ---------------------------------------------------------------------
  // MessageSource API
  // ---------------------------------------------------------------------

  /**
   * Retrieve the message for the given code, using the "defaultHtmlEscape" setting.
   *
   * @param code the code of the message
   * @param defaultMessage the String to return if the lookup fails
   * @return the message
   * @since 5.0
   */
  default String getMessage(String code, String defaultMessage) {
    return getMessage(code, null, defaultMessage, isDefaultHtmlEscape());
  }

  /**
   * Retrieve the message for the given code, using the "defaultHtmlEscape" setting.
   *
   * @param code the code of the message
   * @param args arguments for the message, or {@code null} if none
   * @param defaultMessage the String to return if the lookup fails
   * @return the message
   * @since 5.0
   */
  default String getMessage(String code, Object @Nullable [] args, String defaultMessage) {
    return getMessage(code, args, defaultMessage, isDefaultHtmlEscape());
  }

  /**
   * Retrieve the message for the given code, using the "defaultHtmlEscape" setting.
   *
   * @param code the code of the message
   * @param args arguments for the message as a List, or {@code null} if none
   * @param defaultMessage the String to return if the lookup fails
   * @return the message
   * @since 5.0
   */
  default String getMessage(String code, @Nullable List<?> args, String defaultMessage) {
    return getMessage(code, (args != null ? args.toArray() : null), defaultMessage, isDefaultHtmlEscape());
  }

  /**
   * Retrieve the message for the given code.
   *
   * @param code the code of the message
   * @param args arguments for the message, or {@code null} if none
   * @param defaultMessage the String to return if the lookup fails
   * @param htmlEscape if the message should be HTML-escaped
   * @return the message
   * @since 5.0
   */
  default String getMessage(String code, Object @Nullable [] args, String defaultMessage, boolean htmlEscape) {
    String msg = getMessageSource().getMessage(code, args, defaultMessage, getLocale());
    if (msg == null) {
      return "";
    }
    return htmlEscape ? HtmlUtils.htmlEscape(msg) : msg;
  }

  /**
   * Retrieve the message for the given code, using the "defaultHtmlEscape" setting.
   *
   * @param code the code of the message
   * @return the message
   * @throws infra.context.NoSuchMessageException if not found
   * @since 5.0
   */
  default String getMessage(String code) throws NoSuchMessageException {
    return getMessage(code, null, isDefaultHtmlEscape());
  }

  /**
   * Retrieve the message for the given code, using the "defaultHtmlEscape" setting.
   *
   * @param code the code of the message
   * @param args arguments for the message, or {@code null} if none
   * @return the message
   * @throws infra.context.NoSuchMessageException if not found
   * @since 5.0
   */
  default String getMessage(String code, Object @Nullable [] args) throws NoSuchMessageException {
    return getMessage(code, args, isDefaultHtmlEscape());
  }

  /**
   * Retrieve the message for the given code, using the "defaultHtmlEscape" setting.
   *
   * @param code the code of the message
   * @param args arguments for the message as a List, or {@code null} if none
   * @return the message
   * @throws infra.context.NoSuchMessageException if not found
   * @since 5.0
   */
  default String getMessage(String code, @Nullable List<?> args) throws NoSuchMessageException {
    return getMessage(code, args != null ? args.toArray() : null, isDefaultHtmlEscape());
  }

  /**
   * Retrieve the message for the given code.
   *
   * @param code the code of the message
   * @param args arguments for the message, or {@code null} if none
   * @param htmlEscape if the message should be HTML-escaped
   * @return the message
   * @throws infra.context.NoSuchMessageException if not found
   * @since 5.0
   */
  default String getMessage(String code, Object @Nullable [] args, boolean htmlEscape) throws NoSuchMessageException {
    String msg = getMessageSource().getMessage(code, args, getLocale());
    return htmlEscape ? HtmlUtils.htmlEscape(msg) : msg;
  }

  /**
   * Retrieve the given MessageSourceResolvable (for example, an ObjectError instance), using the "defaultHtmlEscape" setting.
   *
   * @param resolvable the MessageSourceResolvable
   * @return the message
   * @throws infra.context.NoSuchMessageException if not found
   * @since 5.0
   */
  default String getMessage(MessageSourceResolvable resolvable) throws NoSuchMessageException {
    return getMessage(resolvable, isDefaultHtmlEscape());
  }

  /**
   * Retrieve the given MessageSourceResolvable (for example, an ObjectError instance).
   *
   * @param resolvable the MessageSourceResolvable
   * @param htmlEscape if the message should be HTML-escaped
   * @return the message
   * @throws infra.context.NoSuchMessageException if not found
   * @since 5.0
   */
  default String getMessage(MessageSourceResolvable resolvable, boolean htmlEscape) throws NoSuchMessageException {
    String msg = getMessageSource().getMessage(resolvable, getLocale());
    return htmlEscape ? HtmlUtils.htmlEscape(msg) : msg;
  }

  /**
   * Return the MessageSource to use (typically the current ApplicationContext).
   *
   * @since 5.0
   */
  default MessageSource getMessageSource() {
    return getApplicationContext();
  }

  /**
   * Determines whether HTML escaping is enabled by default for message resolution.
   * This method returns the value of the static field {@code defaultHtmlEscape},
   * which is initialized from the application's configuration.
   *
   * @return {@code true} if HTML escaping is enabled by default, {@code false} otherwise
   * @since 5.0
   */
  default boolean isDefaultHtmlEscape() {
    return defaultHtmlEscape;
  }

  /**
   * Create a BindStatus for the given bind object, using the "defaultHtmlEscape" setting.
   *
   * @param path the bean and property path for which values and errors will be resolved (for example, "person.age")
   * @return the new BindStatus instance
   * @throws IllegalStateException if no corresponding Errors object found
   * @since 5.0
   */
  default BindStatus getBindStatus(String path) throws IllegalStateException {
    return new BindStatus(this, path, isDefaultHtmlEscape());
  }

  /**
   * Create a BindStatus for the given bind object, using the "defaultHtmlEscape" setting.
   *
   * @param path the bean and property path for which values and errors will be resolved (for example, "person.age")
   * @param htmlEscape create a BindStatus with automatic HTML escaping?
   * @return the new BindStatus instance
   * @throws IllegalStateException if no corresponding Errors object found
   * @since 5.0
   */
  default BindStatus getBindStatus(String path, boolean htmlEscape) throws IllegalStateException {
    return new BindStatus(this, path, htmlEscape);
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
   * Determine the session id of the given request, if any.
   *
   * @return the session id, or {@code null} if none
   */
  default @Nullable String getSessionId() {
    Session session = getSession(false);
    return session != null ? session.getId() : null;
  }

  /**
   * Return the underlying native context object, if available.
   *
   * @param type the desired type of context object
   * @return the matching context object, or {@code null} if none
   * of that type is available
   * @since 5.0
   */
  default <T> @Nullable T unwrap(@Nullable Class<T> type) {
    return WebUtils.getNativeContext(this, type);
  }

  /**
   * Return the underlying native context object, if available.
   *
   * @param type the desired type of context object
   * @return the matching context object, or {@code null} if none
   * of that type is available
   * @since 5.0
   */
  default <T> T required(Class<T> type) {
    T nativeContext = unwrap(type);
    Assert.state(nativeContext != null, "There is no context associated with this instance");
    return nativeContext;
  }

}
