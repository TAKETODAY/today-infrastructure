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

package infra.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;

import infra.beans.factory.BeanFactoryUtils;
import infra.context.ApplicationContext;
import infra.core.AttributeAccessor;
import infra.core.AttributeAccessorSupport;
import infra.core.Conventions;
import infra.core.io.InputStreamSource;
import infra.core.io.OutputStreamSource;
import infra.http.DefaultHttpHeaders;
import infra.http.ETag;
import infra.http.HttpCookie;
import infra.http.HttpHeaders;
import infra.http.HttpInputMessage;
import infra.http.HttpMethod;
import infra.http.HttpRequest;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.http.MediaType;
import infra.http.server.RequestPath;
import infra.http.server.ServerHttpResponse;
import infra.lang.Assert;
import infra.lang.NullValue;
import infra.lang.Nullable;
import infra.util.CollectionUtils;
import infra.util.MultiValueMap;
import infra.util.ObjectUtils;
import infra.util.StringUtils;
import infra.web.async.AsyncWebRequest;
import infra.web.async.WebAsyncManager;
import infra.web.async.WebAsyncManagerFactory;
import infra.web.multipart.MultipartRequest;
import infra.web.util.UriComponents;
import infra.web.util.UriComponentsBuilder;

import static infra.lang.Constant.DEFAULT_CHARSET;

/**
 * Context holder for request-specific state.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2.3.7 2019-06-22 15:48
 */
public abstract class RequestContext extends AttributeAccessorSupport
        implements InputStreamSource, OutputStreamSource, HttpInputMessage, HttpRequest, AttributeAccessor {

  /**
   * Scope identifier for request scope: "request".
   * Supported in addition to the standard scopes "singleton" and "prototype".
   */
  public static final String SCOPE_REQUEST = "request";

  /**
   * Scope identifier for session scope: "session".
   * Supported in addition to the standard scopes "singleton" and "prototype".
   */
  public static final String SCOPE_SESSION = "session";

  private static final List<String> SAFE_METHODS = List.of("GET", "HEAD");

  /**
   * Date formats as specified in the HTTP RFC.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.1.1">Section 7.1.1.1 of RFC 7231</a>
   */
  private static final String[] DATE_FORMATS = new String[] {
          "EEE, dd MMM yyyy HH:mm:ss zzz",
          "EEE, dd-MMM-yy HH:mm:ss zzz",
          "EEE MMM dd HH:mm:ss yyyy"
  };

  private static final TimeZone GMT = TimeZone.getTimeZone("GMT");

  public static final HttpCookie[] EMPTY_COOKIES = {};

  @Nullable
  protected HttpCookie[] cookies;

  @Nullable
  protected PrintWriter writer;

  @Nullable
  protected BufferedReader reader;

  @Nullable
  protected InputStream inputStream;

  @Nullable
  protected OutputStream outputStream;

  /** @since 3.0 */
  @Nullable
  protected HttpHeaders requestHeaders;
  /** @since 3.0 */
  @Nullable
  protected HttpHeaders responseHeaders;

  /** @since 3.0 */
  @Nullable
  protected String requestURI;

  /** @since 4.0 */
  @Nullable
  protected RequestPath requestPath;
  /** @since 3.0 */
  @Nullable
  protected MultiValueMap<String, String> parameters;
  /** @since 3.0 */
  @Nullable
  protected String queryString;

  /** @since 3.0 */
  @Nullable
  protected ArrayList<HttpCookie> responseCookies;

  /** @since 4.0 */
  @Nullable
  protected URI uri;

  /** @since 4.0 */
  @Nullable
  protected HttpMethod httpMethod;

  /** @since 4.0 */
  @Nullable
  protected Locale locale;

  /** @since 4.0 */
  @Nullable
  protected String responseContentType;

  /** @since 4.0 */
  @Nullable
  protected MultipartRequest multipartRequest;

  /** @since 4.0 */
  @Nullable
  protected AsyncWebRequest asyncRequest;

  @Nullable
  protected WebAsyncManager webAsyncManager;

  protected boolean notModified = false;

  @Nullable
  private HandlerMatchingMetadata matchingMetadata;

  @Nullable
  protected BindingContext bindingContext;

  @Nullable
  protected Object redirectModel;

  @Nullable
  protected Boolean multipartFlag;

  @Nullable
  protected Boolean preFlightRequestFlag;

  @Nullable
  protected Boolean corsRequestFlag;

  /** Map from attribute name String to destruction callback Runnable.  @since 5.0 */
  @Nullable
  protected LinkedHashMap<String, Runnable> destructionCallbacks;

  private long requestCompletedTimeMillis;

  protected final DispatcherHandler dispatcherHandler;

  /** @since 4.0 */
  protected final ApplicationContext applicationContext;

  protected RequestContext(ApplicationContext context, DispatcherHandler dispatcherHandler) {
    this.applicationContext = context;
    this.dispatcherHandler = dispatcherHandler;
  }

  /**
   * Return the WebApplicationContext that this request runs in.
   *
   * @since 4.0
   */
  public ApplicationContext getApplicationContext() {
    return this.applicationContext;
  }

  /**
   * Get start handling this request time millis
   *
   * @return start handling this request time millis
   * @since 4.0
   */
  public abstract long getRequestTimeMillis();

  /**
   * Get this request processing time millis
   *
   * @return this request processing time millis
   * @since 4.0
   */
  public final long getRequestProcessingTime() {
    long requestCompletedTimeMillis = this.requestCompletedTimeMillis;
    if (requestCompletedTimeMillis > 0) {
      return requestCompletedTimeMillis - getRequestTimeMillis();
    }
    return System.currentTimeMillis() - getRequestTimeMillis();
  }

  // --- request

  /**
   * Returns the name of the scheme used to make this request,
   * for example,
   * <code>http</code>, <code>https</code>, or <code>ftp</code>.
   * Different schemes have different rules for constructing URLs,
   * as noted in RFC 1738.
   *
   * @return a <code>String</code> containing the name
   * of the scheme used to make this request
   * @since 3.0.1
   */
  public abstract String getScheme();

  /**
   * Returns the host name of the server to which the request was sent.
   * It is the value of the part before ":" in the <code>Host</code>
   * header value, if any, or the resolved server name, or the server IP address.
   *
   * @return a <code>String</code> containing the name of the server
   * @since 4.0
   */
  public abstract String getServerName();

  /**
   * Returns the port number to which the request was sent. It is the
   * value of the part after ":" in the <code>Host</code> header value,
   * if any, or the server port where the client connection was accepted on.
   *
   * @return an integer specifying the port number
   * @since 4.0
   */
  public abstract int getServerPort();

  // @since 4.0
  @Override
  public URI getURI() {
    if (this.uri == null) {
      String urlString = null;
      boolean hasQuery = false;
      try {
        StringBuilder url = new StringBuilder(getRequestURL());
        String query = getQueryString();
        hasQuery = StringUtils.hasText(query);
        if (hasQuery) {
          url.append('?').append(query);
        }
        urlString = url.toString();
        this.uri = new URI(urlString);
      }
      catch (URISyntaxException ex) {
        if (!hasQuery) {
          throw new IllegalStateException(
                  "Could not resolve RequestContext as URI: " + urlString, ex);
        }
        // Maybe a malformed query string... try plain request URL
        try {
          urlString = getRequestURL();
          this.uri = new URI(urlString);
        }
        catch (URISyntaxException ex2) {
          throw new IllegalStateException(
                  "Could not resolve RequestContext as URI: " + urlString, ex2);
        }
      }
    }
    return this.uri;
  }

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
  public String getRequestURI() {
    String requestURI = this.requestURI;
    if (requestURI == null) {
      requestURI = readRequestURI();
      this.requestURI = requestURI;
    }
    return requestURI;
  }

  /**
   * @see #getRequestURI()
   */
  public RequestPath getRequestPath() {
    RequestPath requestPath = this.requestPath;
    if (requestPath == null) {
      requestPath = readRequestPath();
      this.requestPath = requestPath;
    }
    return requestPath;
  }

  protected RequestPath readRequestPath() {
    return RequestPath.parse(getRequestURI(), null);
  }

  public void setMatchingMetadata(@Nullable HandlerMatchingMetadata handlerMatchingMetadata) {
    this.matchingMetadata = handlerMatchingMetadata;
  }

  @Nullable
  public HandlerMatchingMetadata getMatchingMetadata() {
    return this.matchingMetadata;
  }

  /**
   * @throws IllegalStateException if HandlerMatchingMetadata is not set
   * @since 4.0
   */
  public HandlerMatchingMetadata matchingMetadata() {
    HandlerMatchingMetadata matchingMetadata = this.matchingMetadata;
    Assert.state(matchingMetadata != null, "HandlerMatchingMetadata is required");
    return matchingMetadata;
  }

  public boolean hasMatchingMetadata() {
    return matchingMetadata != null;
  }

  protected abstract String readRequestURI();

  /**
   * The returned URL contains a protocol, server name, port number, and server
   * path, but it does not include query string parameters.
   *
   * @return A URL
   */
  public String getRequestURL() {
    String host = requestHeaders().getFirst(HttpHeaders.HOST);
    if (host == null) {
      host = "localhost";
    }
    return getScheme() + "://" + host + StringUtils.prependLeadingSlash(getRequestURI());
  }

  /**
   * Returns the query string that is contained in the request URL after the path.
   * This method returns <code>null</code> if the URL does not have a query
   * string. Same as the value of the CGI variable QUERY_STRING.
   *
   * @return a <code>String</code> containing the query string or
   * <code>null</code> if the URL contains no query string. The value is
   * not decoded by the container.
   */
  public String getQueryString() {
    String queryString = this.queryString;
    if (queryString == null) {
      queryString = readQueryString();
      this.queryString = queryString;
    }
    return queryString;
  }

  protected abstract String readQueryString();

  /**
   * Returns an array containing all of the <code>Cookie</code> objects the client
   * sent with this request. This method returns <code>null</code> if no cookies
   * were sent.
   *
   * @return an array of all the <code>Cookies</code> included with this request,
   * or {@link #EMPTY_COOKIES} if the request has no cookies
   */
  public HttpCookie[] getCookies() {
    var cookies = this.cookies;
    if (cookies == null) {
      cookies = readCookies();
      this.cookies = cookies;
    }
    return cookies;
  }

  /**
   * @return an array of all the Cookies included with this request,or
   * {@link #EMPTY_COOKIES} if the request has no cookies
   */
  protected abstract HttpCookie[] readCookies();

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
  public HttpCookie getCookie(String name) {
    for (HttpCookie cookie : getCookies()) {
      if (Objects.equals(name, cookie.getName())) {
        return cookie;
      }
    }
    return null;
  }

  /**
   * Adds the specified cookie to the response. This method can be called multiple
   * times to set more than one cookie.
   *
   * @param cookie the Cookie to return to the client
   */
  public void addCookie(HttpCookie cookie) {
    responseCookies().add(cookie);
  }

  /**
   * Adds the specified cookie to the response. This method can be called multiple
   * times to set more than one cookie.
   *
   * @param name the Cookie name to return to the client
   * @param value the Cookie value to return to the client
   */
  public void addCookie(String name, @Nullable String value) {
    addCookie(new HttpCookie(name, value));
  }

  /**
   * remove the specified cookie from response
   *
   * @param name cookie name
   * @return removed cookie
   */
  @Nullable
  public List<HttpCookie> removeCookie(String name) {
    if (responseCookies != null) {
      ArrayList<HttpCookie> toRemove = new ArrayList<>(2);
      for (HttpCookie responseCookie : responseCookies) {
        if (Objects.equals(name, responseCookie.getName())) {
          toRemove.add(responseCookie);
        }
      }
      responseCookies.removeAll(toRemove);
      return toRemove;
    }
    return null;
  }

  public boolean hasResponseCookie() {
    return responseCookies != null;
  }

  public ArrayList<HttpCookie> responseCookies() {
    var responseCookies = this.responseCookies;
    if (responseCookies == null) {
      responseCookies = new ArrayList<>();
      this.responseCookies = responseCookies;
    }
    return responseCookies;
  }

  /**
   * Returns a java.util.Map of the parameters of this request.
   *
   * <p>
   * Request parameters are extra information sent with the request. Parameters
   * are contained in the query string or posted form data.
   *
   * @return java.util.Map containing parameter names as keys and parameter values
   * as map values. The keys in the parameter map are of type String. The
   * values in the parameter map are of type String array.
   */
  public MultiValueMap<String, String> getParameters() {
    var parameters = this.parameters;
    if (parameters == null) {
      parameters = readParameters();
      this.parameters = parameters;
    }
    return parameters;
  }

  protected abstract MultiValueMap<String, String> readParameters();

  /**
   * Returns an <code>Set</code> of <code>String</code> objects containing
   * the names of the parameters contained in this request. If the request has no
   * parameters, the method returns an empty {@link Collections#emptySet() Set}.
   *
   * @return an <code>Set</code> of <code>String</code> objects, each
   * <code>String</code> containing the name of a request parameter; or an
   * empty {@link Collections#emptySet() Set} if the request has no parameters
   */
  public Set<String> getParameterNames() {
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
  @Nullable
  public String[] getParameters(String name) {
    var parameters = getParameters();
    if (CollectionUtils.isEmpty(parameters)) {
      return null;
    }
    List<String> list = parameters.get(name);
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
  @Nullable
  public String getParameter(String name) {
    String[] parameters = getParameters(name);
    if (ObjectUtils.isNotEmpty(parameters)) {
      return parameters[0];
    }
    return null;
  }

  /**
   * Returns the name of the HTTP method with which this request was made, for
   * example, GET, POST, or PUT.
   *
   * @return a <code>String</code> specifying the name of the method with which
   * this request was made
   */
  @Override
  public String getMethodValue() {
    return getMethod().name();
  }

  @Override
  public HttpMethod getMethod() {
    HttpMethod httpMethod = this.httpMethod;
    if (httpMethod == null) {
      httpMethod = HttpMethod.valueOf(readMethod());
      this.httpMethod = httpMethod;
    }
    return httpMethod;
  }

  /**
   * @return upper-case http method
   */
  protected abstract String readMethod();

  /**
   * Returns the Internet Protocol (IP) address of the client or last proxy that
   * sent the request.
   *
   * @return a <code>String</code> containing the IP address of the client that
   * sent the request
   */
  public abstract String getRemoteAddress();

  /**
   * Returns the length, in bytes, of the request body and made available by the
   * input stream, or -1 if the length is not known.
   *
   * @return a long containing the length of the request body or -1L if the length
   * is not known
   */
  public abstract long getContentLength();

  @Override
  public InputStream getBody() throws IOException {
    return getInputStream();
  }

  @Override
  public HttpHeaders getHeaders() {
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
  public InputStream getInputStream() throws IOException {
    InputStream inputStream = this.inputStream;
    if (inputStream == null) {
      inputStream = createInputStream();
      this.inputStream = inputStream;
    }
    return inputStream;
  }

  protected abstract InputStream createInputStream() throws IOException;

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
  public BufferedReader getReader() throws IOException {
    BufferedReader reader = this.reader;
    if (reader == null) {
      reader = createReader();
      this.reader = reader;
    }
    return reader;
  }

  /** template method for get reader */
  protected BufferedReader createReader() throws IOException {
    return new BufferedReader(new InputStreamReader(getInputStream(), DEFAULT_CHARSET));
  }

  // -----------------------------------------------------

  /**
   * @return return whether this request is multipart
   * @since 4.0
   */
  public boolean isMultipart() {
    Boolean multipartFlag = this.multipartFlag;
    if (multipartFlag == null) {
      HttpMethod method = getMethod();
      if (method == HttpMethod.GET || method == HttpMethod.HEAD) {
        multipartFlag = false;
      }
      else {
        multipartFlag = StringUtils.startsWithIgnoreCase(getContentType(), "multipart/");
      }
      this.multipartFlag = multipartFlag;
    }
    return multipartFlag;
  }

  /**
   * @see #isMultipart()
   * @since 4.0
   */
  public MultipartRequest getMultipartRequest() {
    var multipartRequest = this.multipartRequest;
    if (multipartRequest == null) {
      multipartRequest = createMultipartRequest();
      this.multipartRequest = multipartRequest;
    }
    return multipartRequest;
  }

  /**
   * create MultipartRequest
   *
   * @since 4.0
   */
  protected abstract MultipartRequest createMultipartRequest();

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
  public boolean isConcurrentHandlingStarted() {
    return asyncRequest != null && asyncRequest.isAsyncStarted();
  }

  public AsyncWebRequest getAsyncWebRequest() {
    var asyncRequest = this.asyncRequest;
    if (asyncRequest == null) {
      asyncRequest = createAsyncWebRequest();
      this.asyncRequest = asyncRequest;
    }
    return asyncRequest;
  }

  protected abstract AsyncWebRequest createAsyncWebRequest();

  public WebAsyncManager getAsyncManager() {
    WebAsyncManager webAsyncManager = this.webAsyncManager;
    if (webAsyncManager == null) {
      webAsyncManager = createWebAsyncManager();
      this.webAsyncManager = webAsyncManager;
    }
    return webAsyncManager;
  }

  private WebAsyncManager createWebAsyncManager() {
    DispatcherHandler dispatcherHandler = this.dispatcherHandler;
    if (dispatcherHandler == null && getApplicationContext() != null) {
      dispatcherHandler = BeanFactoryUtils.find(getApplicationContext(), DispatcherHandler.class);
    }
    WebAsyncManagerFactory factory = null;
    if (dispatcherHandler != null) {
      factory = dispatcherHandler.webAsyncManagerFactory;
    }
    if (factory == null && getApplicationContext() != null) {
      factory = BeanFactoryUtils.find(getApplicationContext(), WebAsyncManagerFactory.class);
    }
    if (factory == null) {
      factory = new WebAsyncManagerFactory();
    }
    return factory.getWebAsyncManager(this);
  }

  // ---------------------------------------------------------------------
  // requestCompleted
  // ---------------------------------------------------------------------

  /**
   * Signal that the request has been completed.
   * <p>Executes all request destruction callbacks and other resources cleanup
   */
  public void requestCompleted() {
    requestCompleted(null);
  }

  /**
   * Signal that the request has been completed.
   * <p>Executes all request destruction callbacks and other resources cleanup
   *
   * @param notHandled exception not handled
   */
  public void requestCompleted(@Nullable Throwable notHandled) {
    requestCompletedTimeMillis = System.currentTimeMillis();

    if (multipartRequest != null) {
      // @since 3.0 cleanup MultipartFiles
      multipartRequest.cleanup();
    }

    requestCompletedInternal(notHandled);

    var callbacks = destructionCallbacks;
    if (callbacks != null) {
      destructionCallbacks = null;
      for (Runnable runnable : callbacks.values()) {
        runnable.run();
      }
      callbacks.clear();
    }
  }

  protected void requestCompletedInternal(@Nullable Throwable notHandled) {

  }

  /**
   * Register the given callback as to be executed after request completion.
   *
   * @param callback the callback to be executed for destruction
   * @since 5.0
   */
  public final void registerDestructionCallback(Runnable callback) {
    Assert.notNull(callback, "Destruction Callback is required");
    String variableName = Conventions.getVariableName(callback);
    registerDestructionCallback(variableName, callback);
  }

  /**
   * Register the given callback as to be executed after request completion.
   *
   * @param name the name of the attribute to register the callback for
   * @param callback the callback to be executed for destruction
   * @since 5.0
   */
  public final void registerDestructionCallback(String name, Runnable callback) {
    Assert.notNull(name, "Name is required");
    Assert.notNull(callback, "Destruction Callback is required");
    if (destructionCallbacks == null) {
      destructionCallbacks = new LinkedHashMap<>(8);
    }
    destructionCallbacks.put(name, callback);
  }

  /**
   * Remove the request destruction callback for the specified attribute, if any.
   *
   * @param name the name of the attribute to remove the callback for
   * @since 5.0
   */
  public final void removeDestructionCallback(String name) {
    Assert.notNull(name, "Name is required");
    if (destructionCallbacks != null) {
      destructionCallbacks.remove(name);
    }
  }

  //

  /**
   * Returns {@code true} if the request is a valid CORS pre-flight
   * one by checking {code OPTIONS} method with {@code Origin} and
   * {@code Access-Control-Request-Method} headers presence.
   *
   * @since 4.0
   */
  public boolean isPreFlightRequest() {
    Boolean preFlightRequestFlag = this.preFlightRequestFlag;
    if (preFlightRequestFlag == null) {
      if (HttpMethod.OPTIONS == getMethod()) {
        HttpHeaders httpHeaders = requestHeaders();
        preFlightRequestFlag = httpHeaders.getFirst(HttpHeaders.ORIGIN) != null
                && httpHeaders.getFirst(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD) != null;
      }
      else {
        preFlightRequestFlag = false;
      }
      this.preFlightRequestFlag = preFlightRequestFlag;
    }
    return preFlightRequestFlag;
  }

  /**
   * Returns {@code true} if the request is a valid CORS one by checking
   * {@code Origin}header presence and ensuring that origins are different.
   *
   * @since 4.0
   */
  public boolean isCorsRequest() {
    Boolean corsRequestFlag = this.corsRequestFlag;
    if (corsRequestFlag == null) {
      String origin = requestHeaders().getFirst(HttpHeaders.ORIGIN);
      if (origin == null) {
        corsRequestFlag = false;
      }
      else {
        UriComponents originUrl = UriComponentsBuilder.forOriginHeader(origin).build();
        String scheme = getScheme();
        String host = getServerName();
        int port = getServerPort();
        corsRequestFlag = !(ObjectUtils.nullSafeEquals(scheme, originUrl.getScheme())
                && ObjectUtils.nullSafeEquals(host, originUrl.getHost())
                && getPort(scheme, port) == getPort(originUrl.getScheme(), originUrl.getPort()));
      }
    }
    return corsRequestFlag;
  }

  protected static int getPort(@Nullable String scheme, int port) {
    if (port == -1) {
      if ("http".equals(scheme) || "ws".equals(scheme)) {
        port = 80;
      }
      else if ("https".equals(scheme) || "wss".equals(scheme)) {
        port = 443;
      }
    }
    return port;
  }

  /**
   * Returns the MIME type of the body of the request, or <code>null</code> if the
   * type is not known.
   *
   * @return a <code>String</code> containing the name of the MIME type of the
   * request, or null if the type is not known
   */
  @Nullable
  public abstract String getContentType();

  /**
   * Get request HTTP headers
   *
   * @return request read only HTTP header ,never be {@code null}
   * @since 3.0
   */
  public HttpHeaders requestHeaders() {
    HttpHeaders requestHeaders = this.requestHeaders;
    if (requestHeaders == null) {
      requestHeaders = createRequestHeaders();
      this.requestHeaders = requestHeaders;
    }
    return requestHeaders;
  }

  /**
   * template method for create request http-headers
   *
   * @since 3.0
   */
  protected abstract HttpHeaders createRequestHeaders();

  /**
   * Returns the preferred <code>Locale</code> that the client will
   * accept content in, based on the Accept-Language header. If the
   * client request doesn't provide an Accept-Language header, this
   * method returns the default locale for the server.
   *
   * @return the preferred <code>Locale</code> for the client
   * @since 4.0
   */
  public Locale getLocale() {
    if (locale == null) {
      locale = readLocale();
    }
    return locale;
  }

  // @since 4.0
  protected Locale readLocale() {
    List<Locale> locales = requestHeaders().getAcceptLanguageAsLocales();
    Locale locale = CollectionUtils.firstElement(locales);
    if (locale == null) {
      return Locale.getDefault();
    }
    return locale;
  }

  // checkNotModified

  /**
   * Check whether the requested resource has been modified given the
   * supplied last-modified timestamp (as determined by the application).
   * <p>This will also transparently set the "Last-Modified" response header
   * and HTTP status when applicable.
   * <p>Typical usage:
   * <pre>{@code
   * public String myHandleMethod(RequestContext request, Model model) {
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
  public boolean checkNotModified(long lastModifiedTimestamp) {
    return checkNotModified(null, lastModifiedTimestamp);
  }

  /**
   * Check whether the requested resource has been modified given the
   * supplied {@code ETag} (entity tag), as determined by the application.
   * <p>This will also transparently set the "ETag" response header
   * and HTTP status when applicable.
   * <p>Typical usage:
   * <pre>{@code
   * public String myHandleMethod(RequestContext request, Model model) {
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
  public boolean checkNotModified(String etag) {
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
   * public String myHandleMethod(RequestContext request, Model model) {
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
  public boolean checkNotModified(@Nullable String eTag, long lastModifiedTimestamp) {
    if (this.notModified || (HttpStatus.OK.value() != getStatus())) {
      return this.notModified;
    }
    // Evaluate conditions in order of precedence.
    // See https://datatracker.ietf.org/doc/html/rfc9110#section-13.2.2
    if (validateIfMatch(eTag)) {
      updateResponseStateChanging(eTag, lastModifiedTimestamp);
      return this.notModified;
    }
    // 2) If-Unmodified-Since
    else if (validateIfUnmodifiedSince(lastModifiedTimestamp)) {
      updateResponseStateChanging(eTag, lastModifiedTimestamp);
      return this.notModified;
    }
    // 3) If-None-Match
    if (!validateIfNoneMatch(eTag)) {
      // 4) If-Modified-Since
      validateIfModifiedSince(lastModifiedTimestamp);
    }
    updateResponseIdempotent(eTag, lastModifiedTimestamp);
    return this.notModified;
  }

  private boolean validateIfMatch(@Nullable String eTag) {
    if (SAFE_METHODS.contains(getMethodValue())) {
      return false;
    }

    List<String> ifMatchHeaders = requestHeaders().get(HttpHeaders.IF_MATCH);
    if (CollectionUtils.isEmpty(ifMatchHeaders)) {
      return false;
    }
    this.notModified = matchRequestedETags(ifMatchHeaders, eTag, false);
    return true;
  }

  private boolean validateIfNoneMatch(@Nullable String eTag) {
    List<String> ifNoneMatchHeaders = requestHeaders().get(HttpHeaders.IF_NONE_MATCH);
    if (CollectionUtils.isEmpty(ifNoneMatchHeaders)) {
      return false;
    }
    this.notModified = !matchRequestedETags(ifNoneMatchHeaders, eTag, true);
    return true;
  }

  private boolean matchRequestedETags(List<String> requestedETags, @Nullable String tag, boolean weakCompare) {
    if (StringUtils.isNotEmpty(tag)) {
      ETag eTag = ETag.create(tag);
      boolean isNotSafeMethod = !SAFE_METHODS.contains(getMethodValue());
      for (String requestedETagString : requestedETags) {
        // Compare weak/strong ETags as per https://datatracker.ietf.org/doc/html/rfc9110#section-8.8.3
        for (ETag requestedETag : ETag.parse(requestedETagString)) {
          // only consider "lost updates" checks for unsafe HTTP methods
          if (requestedETag.isWildcard() && isNotSafeMethod) {
            return false;
          }
          if (requestedETag.compare(eTag, !weakCompare)) {
            return false;
          }
        }
      }
    }
    return true;
  }

  private void updateResponseStateChanging(@Nullable String eTag, long lastModifiedTimestamp) {
    if (this.notModified) {
      setStatus(HttpStatus.PRECONDITION_FAILED.value());
    }
    else {
      addCachingResponseHeaders(eTag, lastModifiedTimestamp);
    }
  }

  private boolean validateIfUnmodifiedSince(long lastModifiedTimestamp) {
    if (lastModifiedTimestamp < 0) {
      return false;
    }
    long ifUnmodifiedSince = parseDateHeader(HttpHeaders.IF_UNMODIFIED_SINCE);
    if (ifUnmodifiedSince == -1) {
      return false;
    }
    this.notModified = (ifUnmodifiedSince < (lastModifiedTimestamp / 1000 * 1000));
    return true;
  }

  private void validateIfModifiedSince(long lastModifiedTimestamp) {
    if (lastModifiedTimestamp < 0) {
      return;
    }
    long ifModifiedSince = parseDateHeader(HttpHeaders.IF_MODIFIED_SINCE);
    if (ifModifiedSince != -1) {
      // We will perform this validation...
      this.notModified = ifModifiedSince >= (lastModifiedTimestamp / 1000 * 1000);
    }
  }

  private void updateResponseIdempotent(@Nullable String eTag, long lastModifiedTimestamp) {
    boolean isHttpGetOrHead = SAFE_METHODS.contains(getMethodValue());
    if (this.notModified) {
      setStatus(isHttpGetOrHead ?
              HttpStatus.NOT_MODIFIED.value() : HttpStatus.PRECONDITION_FAILED.value());
    }
    if (isHttpGetOrHead) {
      HttpHeaders httpHeaders = responseHeaders();
      if (lastModifiedTimestamp > 0 && parseDateValue(httpHeaders.getFirst(HttpHeaders.LAST_MODIFIED)) == -1) {
        httpHeaders.setDate(HttpHeaders.LAST_MODIFIED, lastModifiedTimestamp);
      }
      if (StringUtils.isNotEmpty(eTag) && httpHeaders.get(HttpHeaders.ETAG) == null) {
        httpHeaders.setOrRemove(HttpHeaders.ETAG, ETag.quoteETagIfNecessary(eTag));
      }
    }
  }

  private void addCachingResponseHeaders(@Nullable String eTag, long lastModifiedTimestamp) {
    if (SAFE_METHODS.contains(getMethodValue())) {
      HttpHeaders httpHeaders = responseHeaders();
      if (lastModifiedTimestamp > 0 && parseDateValue(httpHeaders.getFirst(HttpHeaders.LAST_MODIFIED)) == -1) {
        httpHeaders.setLastModified(lastModifiedTimestamp);
      }
      if (StringUtils.isNotEmpty(eTag) && httpHeaders.get(HttpHeaders.ETAG) == null) {
        httpHeaders.setOrRemove(HttpHeaders.ETAG, ETag.quoteETagIfNecessary(eTag));
      }
    }
  }

  public boolean isNotModified() {
    return this.notModified;
  }

  private long parseDateHeader(String headerName) {
    long dateValue = -1;
    HttpHeaders httpHeaders = requestHeaders();
    try {
      dateValue = httpHeaders.getFirstDate(headerName);
    }
    catch (IllegalArgumentException ex) {
      String headerValue = httpHeaders.getFirst(headerName);
      // Possibly an IE 10 style value: "Wed, 09 Apr 2014 09:57:42 GMT; length=13774"
      if (headerValue != null) {
        int separatorIndex = headerValue.indexOf(';');
        if (separatorIndex != -1) {
          String datePart = headerValue.substring(0, separatorIndex);
          dateValue = parseDateValue(datePart);
        }
        else {
          try {
            return Long.parseLong(headerValue);
          }
          catch (NumberFormatException ignored) {
          }
        }
      }
    }
    return dateValue;
  }

  private long parseDateValue(@Nullable String headerValue) {
    if (headerValue == null) {
      // No header value sent at all
      return -1;
    }
    if (headerValue.length() >= 3) {
      // Short "0" or "-1" like values are never valid HTTP date headers...
      // Let's only bother with SimpleDateFormat parsing for long enough values.
      for (String dateFormat : DATE_FORMATS) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat, Locale.US);
        simpleDateFormat.setTimeZone(GMT);
        try {
          return simpleDateFormat.parse(headerValue).getTime();
        }
        catch (ParseException ex) {
          // ignore
        }
      }
    }
    return -1;
  }

  /** @since 4.0 */
  public void setBinding(@Nullable BindingContext bindingContext) {
    this.bindingContext = bindingContext;
  }

  /** @since 4.0 */
  public boolean hasBinding() {
    return bindingContext != null;
  }

  /**
   * @since 4.0
   */
  @Nullable
  public BindingContext getBinding() {
    return bindingContext;
  }

  /**
   * @throws IllegalStateException if BindingContext is not set
   * @since 4.0
   */
  public BindingContext binding() {
    BindingContext bindingContext = this.bindingContext;
    Assert.state(bindingContext != null, "BindingContext is required");
    return bindingContext;
  }

  /**
   * Return read-only "input" flash attributes from request before redirect.
   *
   * @return a RedirectModel, or {@code null} if not found
   * @see RedirectModel
   */
  @Nullable
  public RedirectModel getInputRedirectModel() {
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
  public RedirectModel getInputRedirectModel(@Nullable RedirectModelManager manager) {
    if (redirectModel instanceof RedirectModel ret) {
      return ret;
    }
    else if (redirectModel == NullValue.INSTANCE) {
      return null;
    }

    if (manager == null) {
      manager = RequestContextUtils.getRedirectModelManager(this);
    }
    if (manager != null) {
      RedirectModel redirectModel = manager.retrieveAndUpdate(this);
      if (redirectModel != null) {
        this.redirectModel = redirectModel;
        return redirectModel;
      }
    }
    Object attribute = getAttribute(RedirectModel.INPUT_ATTRIBUTE);
    if (attribute instanceof RedirectModel ret) {
      this.redirectModel = ret;
      return ret;
    }
    this.redirectModel = NullValue.INSTANCE;
    return null;
  }

  /**
   * Sets the length of the content body in the response , this method sets the
   * HTTP Content-Length header.
   *
   * @param length an long specifying the length of the content being returned to the
   * client; sets the Content-Length header
   */
  public void setContentLength(long length) {
    responseHeaders().setContentLength(length);
  }

  /**
   * Returns a boolean indicating if the response has been committed. A committed
   * response has already had its status code and headers written.
   *
   * @return a boolean indicating if the response has been committed
   * @see #reset
   */
  public abstract boolean isCommitted();

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
  public void reset() {
    if (responseHeaders != null) {
      responseHeaders.clear();
    }
  }

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
  public abstract void sendRedirect(String location) throws IOException;

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
  public abstract void setStatus(int sc);

  /**
   * Sets the status code and message for this response.
   *
   * @param status the status
   */
  public void setStatus(HttpStatusCode status) {
    setStatus(status.value());
  }

  /**
   * Gets the current status code of this response.
   *
   * @return the current status code of this response
   */
  public abstract int getStatus();

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
  public void sendError(HttpStatusCode code) throws IOException {
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
  public void sendError(HttpStatusCode code, @Nullable String msg) throws IOException {
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
  public abstract void sendError(int sc) throws IOException;

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
  public abstract void sendError(int sc, @Nullable String msg) throws IOException;

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
  public OutputStream getOutputStream() throws IOException {
    OutputStream outputStream = this.outputStream;
    if (outputStream == null) {
      outputStream = createOutputStream();
      this.outputStream = outputStream;
    }
    return outputStream;
  }

  /**
   * template method for create OutputStream
   */
  protected abstract OutputStream createOutputStream() throws IOException;

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
   * @throws IllegalStateException if the <code>getOutputStream</code>
   * method has already been called for this response object
   * @see #getOutputStream
   * @see #reset
   * @see #getOutputStream()
   * @see PrintWriter#PrintWriter(OutputStream, boolean, Charset)
   */
  @Override
  public PrintWriter getWriter() throws IOException {
    PrintWriter writer = this.writer;
    if (writer == null) {
      writer = createWriter();
      this.writer = writer;
    }
    return writer;
  }

  /**
   * template method for get writer
   */
  protected PrintWriter createWriter() throws IOException {
    return new PrintWriter(getOutputStream(), true);
  }

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
  public void setContentType(@Nullable String contentType) {
    this.responseContentType = contentType;
    setHeader(HttpHeaders.CONTENT_TYPE, contentType);
  }

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
  public void setContentType(@Nullable MediaType contentType) {
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
   * @see #getContentType()
   */
  @Nullable
  public String getResponseContentType() {
    if (responseContentType == null) {
      if (responseHeaders != null) {
        return responseHeaders.getFirst(HttpHeaders.CONTENT_TYPE);
      }
    }
    return responseContentType;
  }

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
  public void setHeader(String name, @Nullable String value) {
    responseHeaders().setOrRemove(name, value);
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
  public void addHeader(String name, @Nullable String value) {
    responseHeaders().add(name, value);
  }

  /**
   * Remove a response header with the given name.
   *
   * @param name the name of the header
   * @see HttpHeaders#remove
   * @since 4.0
   */
  public void removeHeader(String name) {
    if (responseHeaders != null) {
      responseHeaders.remove(name);
    }
  }

  /**
   * Returns a boolean indicating whether the named
   * response header has already been set.
   *
   * @param name the header name
   * @return <code>true</code> if the named response header
   * has already been set; <code>false</code> otherwise
   * @since 4.0
   */
  public boolean containsResponseHeader(String name) {
    return responseHeaders != null && responseHeaders.containsKey(name);
  }

  /**
   * Get request HTTP headers
   *
   * @since 3.0
   */
  public HttpHeaders responseHeaders() {
    HttpHeaders responseHeaders = this.responseHeaders;
    if (responseHeaders == null) {
      responseHeaders = createResponseHeaders();
      this.responseHeaders = responseHeaders;
    }
    return responseHeaders;
  }

  /**
   * merge headers to response http-headers
   *
   * @since 3.0
   */
  public void mergeToResponse(@Nullable HttpHeaders headers) {
    if (CollectionUtils.isNotEmpty(headers)) {
      responseHeaders().addAll(headers);
    }
  }

  /**
   * create a new response http-header
   *
   * @since 3.0
   */
  protected HttpHeaders createResponseHeaders() {
    return new DefaultHttpHeaders();
  }

  // ----------------------

  public ServerHttpResponse asHttpOutputMessage() {
    return new RequestContextHttpOutputMessage();
  }

  /**
   * Native request
   */
  public abstract <T> T nativeRequest();

  // ------------------

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
  public void flush() throws IOException {
    writeHeaders();

    if (writer != null) {
      writer.flush();
    }
    else if (outputStream != null) {
      outputStream.flush();
    }
  }

  /**
   * write headers to response
   *
   * @since 4.0
   */
  protected void writeHeaders() { }

  @Override
  public String toString() {
    String url = URLDecoder.decode(getRequestURL(), StandardCharsets.UTF_8);
    return getMethodValue() + " " + url;
  }

  final class RequestContextHttpOutputMessage implements ServerHttpResponse {

    @Override
    public void setStatusCode(HttpStatusCode status) {
      setStatus(status);
    }

    @Override
    public void flush() throws IOException {
      RequestContext.this.flush();
    }

    @Override
    public void close() {
      writeHeaders();
    }

    @Override
    public OutputStream getBody() throws IOException {
      return getOutputStream();
    }

    @Override
    public HttpHeaders getHeaders() {
      return responseHeaders();
    }

    @Override
    public void setContentType(@Nullable MediaType mediaType) {
      RequestContext.this.setContentType(mediaType);
    }
  }

}
