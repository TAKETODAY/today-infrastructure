/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web;

import java.io.BufferedReader;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.regex.Matcher;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.MultiValueMap;
import cn.taketoday.core.io.InputStreamSource;
import cn.taketoday.core.io.OutputStreamSource;
import cn.taketoday.http.DefaultHttpHeaders;
import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpInputMessage;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpRequest;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.server.PathContainer;
import cn.taketoday.http.server.RequestPath;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.bind.MultipartException;
import cn.taketoday.web.bind.NotMultipartRequestException;
import cn.taketoday.web.multipart.MultipartFile;
import cn.taketoday.web.view.Model;
import cn.taketoday.web.view.ModelAndView;
import cn.taketoday.web.view.ModelAttributes;

import static cn.taketoday.lang.Constant.DEFAULT_CHARSET;

/**
 * Context holder for request-specific state.
 *
 * @author TODAY 2019-06-22 15:48
 * @since 2.3.7
 */
public abstract class RequestContext
        implements InputStreamSource, OutputStreamSource, Model, Flushable, HttpInputMessage, HttpRequest {

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

  protected String contextPath;
  protected Object requestBody;
  protected HttpCookie[] cookies;
  protected ModelAndView modelAndView;

  protected PrintWriter writer;
  protected BufferedReader reader;
  protected InputStream inputStream;
  protected OutputStream outputStream;

  protected MultiValueMap<String, MultipartFile> multipartFiles;

  /** @since 3.0 */
  protected HttpHeaders requestHeaders;
  /** @since 3.0 */
  protected HttpHeaders responseHeaders;
  /** @since 3.0 */
  protected Model model;
  /** @since 3.0 */
  protected String method;
  /** @since 3.0 */
  protected String requestPath;
  /** @since 3.0 */
  protected Map<String, String[]> parameters;
  /** @since 3.0 */
  protected String queryString;

  /** @since 3.0 */
  protected ArrayList<HttpCookie> responseCookies;

  /** @since 4.0 */
  protected URI uri;

  /** @since 4.0 */
  private boolean requestHandled = false;

  /** @since 4.0 */
  protected HttpMethod httpMethod;

  /** @since 4.0 */
  protected RequestPath lookupPath;

  /** @since 4.0 */
  protected PathContainer pathWithinApplication;

  /** @since 4.0 */
  protected Locale locale;

  /** @since 4.0 */
  protected String responseContentType;

  /** @since 4.0 */
  protected final ApplicationContext applicationContext;

  protected boolean notModified = false;

  @Nullable
  private HandlerMatchingMetadata matchingMetadata;

  protected RequestContext(ApplicationContext context) {
    this.applicationContext = context;
  }

  /**
   * Return the WebApplicationContext that this request runs in.
   */
  public ApplicationContext getApplicationContext() {
    return this.applicationContext;
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

  /**
   * Returns the portion of the request URI that indicates the context of the
   * request. The context path always comes first in a request URI. The path
   * starts with a "" character but does not end with a "" character. The
   * container does not decode this string.
   *
   * @return a <code>String</code> specifying the portion of the request URI that
   * indicates the context of the request
   */
  public String getContextPath() {
    if (contextPath == null) {
      this.contextPath = doGetContextPath();
    }
    return contextPath;
  }

  protected String doGetContextPath() {
    return Constant.BLANK;
  }

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
                  "Could not resolve HttpServletRequest as URI: " + urlString, ex);
        }
        // Maybe a malformed query string... try plain request URL
        try {
          urlString = getRequestURL();
          this.uri = new URI(urlString);
        }
        catch (URISyntaxException ex2) {
          throw new IllegalStateException(
                  "Could not resolve HttpServletRequest as URI: " + urlString, ex2);
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
  public String getRequestPath() {
    String requestPath = this.requestPath;
    if (requestPath == null) {
      requestPath = doGetRequestPath();
      this.requestPath = requestPath;
    }
    return requestPath;
  }

  /**
   * @since 4.0
   */
  public final RequestPath getLookupPath() {
    RequestPath lookupPath = this.lookupPath;
    if (lookupPath == null) {
      lookupPath = RequestPath.parse(getRequestPath(), getContextPath());
      this.lookupPath = lookupPath;
    }
    return lookupPath;
  }

  public void setMatchingMetadata(@Nullable HandlerMatchingMetadata handlerMatchingMetadata) {
    this.matchingMetadata = handlerMatchingMetadata;
  }

  @Nullable
  public HandlerMatchingMetadata getMatchingMetadata() {
    return this.matchingMetadata;
  }

  protected abstract String doGetRequestPath();

  /**
   * The returned URL contains a protocol, server name, port number, and server
   * path, but it does not include query string parameters.
   *
   * @return A URL
   */
  public abstract String getRequestURL();

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
    if (queryString == null) {
      this.queryString = doGetQueryString();
    }
    return queryString;
  }

  protected abstract String doGetQueryString();

  /**
   * Returns an array containing all of the <code>Cookie</code> objects the client
   * sent with this request. This method returns <code>null</code> if no cookies
   * were sent.
   *
   * @return an array of all the <code>Cookies</code> included with this request,
   * or {@link #EMPTY_COOKIES} if the request has no cookies
   */
  public HttpCookie[] getCookies() {
    if (cookies == null) {
      this.cookies = doGetCookies();
    }
    return cookies;
  }

  /**
   * @return an array of all the Cookies included with this request,or
   * {@link #EMPTY_COOKIES} if the request has no cookies
   */
  protected abstract HttpCookie[] doGetCookies();

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

  public ArrayList<HttpCookie> responseCookies() {
    if (responseCookies == null) {
      this.responseCookies = new ArrayList<>();
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
  public Map<String, String[]> getParameters() {
    if (parameters == null) {
      this.parameters = doGetParameters();
    }
    return parameters;
  }

  protected Map<String, String[]> doGetParameters() {
    String queryString = URLDecoder.decode(getQueryString(), StandardCharsets.UTF_8);
    MultiValueMap<String, String> parameters = RequestContextUtils.parseParameters(queryString);
    postGetParameters(parameters);
    if (!parameters.isEmpty()) {
      return parameters.toArrayMap(String[]::new);
    }
    return Collections.emptyMap();
  }

  protected void postGetParameters(MultiValueMap<String, String> parameters) {
    // no-op
  }

  /**
   * Returns an <code>Iterator</code> of <code>String</code> objects containing
   * the names of the parameters contained in this request. If the request has no
   * parameters, the method returns an empty <code>Iterator</code>.
   *
   * @return an <code>Iterator</code> of <code>String</code> objects, each
   * <code>String</code> containing the name of a request parameter; or an
   * empty <code>Iterator</code> if the request has no parameters
   */
  public Iterator<String> getParameterNames() {
    Map<String, String[]> parameters = getParameters();
    if (CollectionUtils.isEmpty(parameters)) {
      return Collections.emptyIterator();
    }
    return parameters.keySet().iterator();
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
    Map<String, String[]> parameters = getParameters();
    if (CollectionUtils.isEmpty(parameters)) {
      return null;
    }
    return parameters.get(name);
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
  public final String getMethodValue() {
    if (method == null) {
      this.method = doGetMethod();
    }
    return method;
  }

  @Override
  public final HttpMethod getMethod() {
    if (httpMethod == null) {
      httpMethod = HttpMethod.from(getMethodValue());
    }
    return httpMethod;
  }

  protected abstract String doGetMethod();

  /**
   * Returns the Internet Protocol (IP) address of the client or last proxy that
   * sent the request.
   *
   * @return a <code>String</code> containing the IP address of the client that
   * sent the request
   */
  public abstract String remoteAddress();

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
   * @throws IllegalStateException For Servlet Environment if the {@link #getReader} method has
   * already been called for this request
   * @throws IOException if an input or output exception occurred
   */
  @Override
  public InputStream getInputStream() throws IOException {
    if (inputStream == null) {
      this.inputStream = doGetInputStream();
    }
    return inputStream;
  }

  protected abstract InputStream doGetInputStream() throws IOException;

  /**
   * Retrieves the body of the request as character data using a
   * <code>BufferedReader</code>. The reader translates the character data
   * according to the character encoding used on the body. Either this method or
   * {@link #getInputStream} may be called to read the body, not both.
   *
   * @return a <code>BufferedReader</code> containing the body of the request
   * @throws IllegalStateException For Servlet Environment if {@link #getInputStream} method has
   * been called on this request
   * @throws IOException if an input or output exception occurred
   * @see #getInputStream
   */
  @Override
  public BufferedReader getReader() throws IOException {
    if (reader == null) {
      this.reader = doGetReader();
    }
    return reader;
  }

  /** template method for get reader */
  protected BufferedReader doGetReader() throws IOException {
    return new BufferedReader(new InputStreamReader(getInputStream(), DEFAULT_CHARSET));
  }

  // -----------------------------------------------------

  /**
   * @return return whether this request is multipart
   * @since 4.0
   */
  public boolean isMultipart() {
    if (!"POST".equals(getMethodValue())) {
      return false;
    }
    String contentType = getContentType();
    return contentType != null && contentType.toLowerCase().startsWith("multipart/");
  }

  /**
   * Get all {@link MultipartFile}s from current request
   */
  public MultiValueMap<String, MultipartFile> multipartFiles() {
    if (multipartFiles == null) {
      this.multipartFiles = parseMultipartFiles();
    }
    return multipartFiles;
  }

  /**
   * template method for different MultipartFile parsing strategy
   *
   * @return map list {@link MultipartFile}
   * @throws NotMultipartRequestException if this request is not of type multipart/form-data
   * @throws MultipartException multipart parse failed
   */
  protected abstract MultiValueMap<String, MultipartFile> parseMultipartFiles();

  /**
   * Returns the MIME type of the body of the request, or <code>null</code> if the
   * type is not known.
   *
   * @return a <code>String</code> containing the name of the MIME type of the
   * request, or null if the type is not known
   */
  public abstract String getContentType();

  /**
   * Get request HTTP headers
   *
   * @return request read only HTTP header ,never be {@code null}
   * @since 3.0
   */
  public HttpHeaders requestHeaders() {
    if (requestHeaders == null) {
      this.requestHeaders = createRequestHeaders();
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
      locale = doGetLocale();
    }
    return locale;
  }

  // @since 4.0
  protected Locale doGetLocale() {
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
   * <pre class="code">
   * public String myHandleMethod(RequestContext request, Model model) {
   *   long lastModified = // application-specific calculation
   *   if (request.checkNotModified(lastModified)) {
   *     // shortcut exit - no further processing necessary
   *     return null;
   *   }
   *   // further request processing, actually building content
   *   model.addAttribute(...);
   *   return "myViewName";
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
   * <pre class="code">
   * public String myHandleMethod(RequestContext request, Model model) {
   *   String eTag = // application-specific calculation
   *   if (request.checkNotModified(eTag)) {
   *     // shortcut exit - no further processing necessary
   *     return null;
   *   }
   *   // further request processing, actually building content
   *   model.addAttribute(...);
   *   return "myViewName";
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
   * <pre class="code">
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
   * }</pre>
   * <p>This method works with conditional GET/HEAD requests, but
   * also with conditional POST/PUT/DELETE requests.
   * <p><strong>Note:</strong> The HTTP specification recommends
   * setting both ETag and Last-Modified values, but you can also
   * use {@code #checkNotModified(String)} or
   * {@link #checkNotModified(long)}.
   *
   * @param etag the entity tag that the application determined
   * for the underlying resource. This parameter will be padded
   * with quotes (") if necessary.
   * @param lastModifiedTimestamp the last-modified timestamp in
   * milliseconds that the application determined for the underlying
   * resource
   * @return true if the request does not require further processing.
   * @since 4.0
   */
  public boolean checkNotModified(@Nullable String etag, long lastModifiedTimestamp) {
    if (notModified || (HttpStatus.OK.value() != getStatus())) {
      return notModified;
    }

    // Evaluate conditions in order of precedence.
    // See https://tools.ietf.org/html/rfc7232#section-6

    if (validateIfUnmodifiedSince(lastModifiedTimestamp)) {
      if (notModified) {
        setStatus(HttpStatus.PRECONDITION_FAILED);
      }
      return notModified;
    }

    boolean validated = validateIfNoneMatch(etag);
    if (!validated) {
      validateIfModifiedSince(lastModifiedTimestamp);
    }

    // Update response
    String methodValue = getMethodValue();
    boolean isHttpGetOrHead = "GET".equals(methodValue) || "HEAD".equals(methodValue);
    if (notModified) {
      setStatus(isHttpGetOrHead
                ? HttpStatus.NOT_MODIFIED : HttpStatus.PRECONDITION_FAILED);
    }
    if (isHttpGetOrHead) {
      HttpHeaders responseHeaders = responseHeaders();
      if (lastModifiedTimestamp > 0 && parseDateValue(responseHeaders.getFirst(HttpHeaders.LAST_MODIFIED)) == -1) {
        responseHeaders.setDate(HttpHeaders.LAST_MODIFIED, lastModifiedTimestamp);
      }
      if (StringUtils.isNotEmpty(etag) && responseHeaders.getFirst(HttpHeaders.ETAG) == null) {
        responseHeaders.set(HttpHeaders.ETAG, padEtagIfNecessary(etag));
      }
    }

    return notModified;
  }

  private boolean validateIfUnmodifiedSince(long lastModifiedTimestamp) {
    if (lastModifiedTimestamp < 0) {
      return false;
    }
    long ifUnmodifiedSince = parseDateHeader(HttpHeaders.IF_UNMODIFIED_SINCE);
    if (ifUnmodifiedSince == -1) {
      return false;
    }
    // We will perform this validation...
    this.notModified = (ifUnmodifiedSince < (lastModifiedTimestamp / 1000 * 1000));
    return true;
  }

  private boolean validateIfNoneMatch(@Nullable String etag) {
    if (StringUtils.isEmpty(etag)) {
      return false;
    }

    HttpHeaders httpHeaders = getHeaders();
    Iterator<String> ifNoneMatch;
    try {
      List<String> strings = httpHeaders.get(HttpHeaders.IF_NONE_MATCH);
      if (strings == null) {
        strings = Collections.emptyList();
      }
      ifNoneMatch = strings.iterator();
    }
    catch (IllegalArgumentException ex) {
      return false;
    }
    if (!ifNoneMatch.hasNext()) {
      return false;
    }

    // We will perform this validation...
    etag = padEtagIfNecessary(etag);
    if (etag.startsWith("W/")) {
      etag = etag.substring(2);
    }
    while (ifNoneMatch.hasNext()) {
      String clientETags = ifNoneMatch.next();
      Matcher etagMatcher = HttpHeaders.ETAG_HEADER_VALUE_PATTERN.matcher(clientETags);
      // Compare weak/strong ETags as per https://tools.ietf.org/html/rfc7232#section-2.3
      while (etagMatcher.find()) {
        if (StringUtils.isNotEmpty(etagMatcher.group()) && etag.equals(etagMatcher.group(3))) {
          this.notModified = true;
          break;
        }
      }
    }

    return true;
  }

  private String padEtagIfNecessary(String etag) {
    if (StringUtils.isEmpty(etag)) {
      return etag;
    }
    if ((etag.startsWith("\"") || etag.startsWith("W/\"")) && etag.endsWith("\"")) {
      return etag;
    }
    return "\"" + etag + "\"";
  }

  private boolean validateIfModifiedSince(long lastModifiedTimestamp) {
    if (lastModifiedTimestamp < 0) {
      return false;
    }
    long ifModifiedSince = parseDateHeader(HttpHeaders.IF_MODIFIED_SINCE);
    if (ifModifiedSince == -1) {
      return false;
    }
    // We will perform this validation...
    this.notModified = ifModifiedSince >= (lastModifiedTimestamp / 1000 * 1000);
    return true;
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
          catch (NumberFormatException ignored) { }
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

  // ---------------- response

  /**
   * Get a {@link ModelAndView}
   * <p>
   * If there isn't a {@link ModelAndView} in this {@link RequestContext},
   * <b>Create One</b>
   *
   * @return Returns {@link ModelAndView}
   */
  public ModelAndView modelAndView() {
    if (modelAndView == null) {
      this.modelAndView = new ModelAndView(this);
    }
    return modelAndView;
  }

  /**
   * @since 3.0
   */
  public boolean hasModelAndView() {
    return modelAndView != null;
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
    resetResponseHeader();
  }

  /**
   * Sends a temporary redirect response to the client using the specified
   * redirect location URL and clears the buffer. The buffer will be replaced with
   * the data set by this method. Calling this method sets the status code to 302
   * (Found). This method can accept relative URLs;the servlet container must
   * convert the relative URL to an absolute URL before sending the response to
   * the client. If the location is relative without a leading '/' the container
   * interprets it as relative to the current request URI. If the location is
   * relative with a leading '/' the container interprets it as relative to the
   * servlet container root. If the location is relative with two leading '/' the
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
   * @param status the status code
   * @param message the status message
   */
  public abstract void setStatus(int status, String message);

  /**
   * Sets the status code and message for this response.
   *
   * @param status the status
   */
  public void setStatus(HttpStatus status) {
    setStatus(status.value(), status.getReasonPhrase());
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
   *
   * The server will preserve cookies and may clear or update any headers needed
   * to serve the error page as a valid response.
   *
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
  public abstract void sendError(int sc, String msg) throws IOException;

  /**
   * Returns a {@link OutputStream} suitable for writing binary data in the
   * response. The Server container does not encode the binary data.
   * <p>
   * Calling flush() on the {@link OutputStream} commits the response.
   *
   * Either this method or {@link #getWriter} may be called to write the body, not
   * both, except when {@link #reset} has been called.
   *
   * @return a {@link OutputStream} for writing binary data
   * @throws IllegalStateException For Servlet Environment if the <code>getWriter</code> method
   * has been called on this response
   * @throws IOException if an input or output exception occurred
   * @see #getWriter
   * @see #reset
   */
  @Override
  public OutputStream getOutputStream() throws IOException {
    if (outputStream == null) {
      this.outputStream = doGetOutputStream();
    }
    return outputStream;
  }

  /** template method for get OutputStream */
  protected abstract OutputStream doGetOutputStream() throws IOException;

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
   * @throws IllegalStateException For Servlet Environment if the <code>getOutputStream</code>
   * method has already been called for this response object
   * @see #getOutputStream
   * @see #reset
   */
  @Override
  public PrintWriter getWriter() throws IOException {
    if (writer == null) {
      this.writer = doGetWriter();
    }
    return writer;
  }

  /**
   * template method for get writer
   */
  protected PrintWriter doGetWriter() throws IOException {
    return new PrintWriter(getOutputStream());
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
   * for the servlet response's writer to the client if the protocol provides a
   * way for doing so. In the case of HTTP, the <code>Content-Type</code> header
   * is used.
   *
   * @param contentType a <code>String</code> specifying the MIME type of the content
   */
  public void setContentType(String contentType) {
    this.responseContentType = contentType;
    responseHeaders().set(HttpHeaders.CONTENT_TYPE, contentType);
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
   * @see jakarta.servlet.http.HttpServletResponse#getContentType()
   */
  @Nullable
  public String getResponseContentType() {
    return responseContentType;
  }

  /**
   * Get request HTTP headers
   *
   * @since 3.0
   */
  public HttpHeaders responseHeaders() {
    if (responseHeaders == null) {
      this.responseHeaders = createResponseHeaders();
    }
    return responseHeaders;
  }

  /**
   * merge headers to response http-headers
   *
   * @since 3.0
   */
  public void mergeToResponse(HttpHeaders headers) {
    responseHeaders().putAll(headers);
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

  /**
   * Native request eg: HttpServletRequest
   */
  public abstract <T> T nativeRequest();

  /**
   * @param requestClass wrapped request class
   * @return returns {@code null} indicated that not a requestClass
   */
  @Nullable
  public abstract <T> T unwrapRequest(Class<T> requestClass);

  /**
   * @return this request-context underlying implementation
   */
  public abstract <T> T nativeResponse();

  /**
   * @param responseClass wrapped response class
   * @return returns {@code null} indicated that not a responseClass
   */
  @Nullable
  public abstract <T> T unwrapResponse(Class<T> responseClass);

  // ------------------

  // Model

  /**
   * @since 4.0
   */
  public Model getModel() {
    Model model = this.model;
    if (model == null) {
      model = createModel();
      this.model = model;
    }
    return model;
  }

  protected Model createModel() {
    return new ModelAttributes();
  }

  @Override
  public boolean containsAttribute(String name) {
    return getModel().containsAttribute(name);
  }

  @Override
  public void setAttributes(Map<String, Object> attributes) {
    getModel().setAttributes(attributes);
  }

  @Override
  public Object getAttribute(String name) {
    return getModel().getAttribute(name);
  }

  @Override
  public void setAttribute(String name, Object value) {
    getModel().setAttribute(name, value);
  }

  @Override
  public Object removeAttribute(String name) {
    return getModel().removeAttribute(name);
  }

  @Override
  public Map<String, Object> asMap() {
    return getModel().asMap();
  }

  @Override
  public String[] getAttributeNames() {
    return getModel().getAttributeNames();
  }

  @Override
  public Iterator<String> attributeNames() {
    return getModel().attributeNames();
  }

  @Override
  public Model addAttribute(@Nullable Object attributeValue) {
    return getModel().addAttribute(attributeValue);
  }

  @Override
  public Model addAllAttributes(@Nullable Map<String, ?> attributes) {
    return getModel().addAllAttributes(attributes);
  }

  @Override
  public Model addAllAttributes(@Nullable Collection<?> attributeValues) {
    return getModel().addAllAttributes(attributeValues);
  }

  @Override
  public Model mergeAttributes(@Nullable Map<String, ?> attributes) {
    return getModel().mergeAttributes(attributes);
  }

  @Override
  public boolean isEmpty() {
    return getModel().isEmpty();
  }

  @Override
  public void clear() {
    getModel().clear();
  }

  protected void resetResponseHeader() {
    if (responseHeaders != null) {
      responseHeaders.clear();
    }
  }

  /**
   * Forces any content in the buffer to be written to the client.  A call
   * to this method automatically commits the response, meaning the status
   * code and headers will be written.
   *
   * @throws IOException if the act of flushing the buffer cannot be completed.
   * @see #isCommitted
   * @see #reset
   */
  @Override
  public void flush() throws IOException {
    if (writer != null) {
      writer.flush();
    }
    else {
      if (outputStream != null) {
        outputStream.flush();
      }
    }
  }

  /**
   * cleanup multipart in this request context
   */
  public void cleanupMultipartFiles() {
    if (CollectionUtils.isNotEmpty(multipartFiles)) {
      for (Map.Entry<String, List<MultipartFile>> entry : multipartFiles.entrySet()) {
        List<MultipartFile> value = entry.getValue();
        for (MultipartFile multipartFile : value) {
          try {
            multipartFile.delete();
          }
          catch (IOException e) {
            LoggerFactory.getLogger(RequestContext.class)
                    .error("error occurred when cleanup multipart", e);
          }
        }
      }
    }
  }

  @Override
  public String toString() {
    return getMethodValue() + " " + getRequestURL();
  }

  /**
   * Whether the request has been handled fully within the handler, e.g.
   * {@code @ResponseBody} method, and therefore view resolution is not
   * necessary. This flag can also be set when controller methods declare an
   * argument of type {@code ServletResponse} or {@code OutputStream}).
   * <p>The default value is {@code false}.
   */
  public void setRequestHandled(boolean requestHandled) {
    this.requestHandled = requestHandled;
  }

  /**
   * Whether the request has been handled fully within the handler.
   */
  public boolean isRequestHandled() {
    return this.requestHandled;
  }

}
