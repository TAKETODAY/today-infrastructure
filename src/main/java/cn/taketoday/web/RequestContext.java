/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import cn.taketoday.context.EmptyObject;
import cn.taketoday.context.io.Readable;
import cn.taketoday.context.io.Writable;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.CollectionUtils;
import cn.taketoday.context.utils.MultiValueMap;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.RequestContextHolder.ApplicationNotStartedContext;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.http.DefaultHttpHeaders;
import cn.taketoday.web.http.HttpHeaders;
import cn.taketoday.web.http.HttpStatus;
import cn.taketoday.web.multipart.MultipartFile;
import cn.taketoday.web.ui.Model;
import cn.taketoday.web.ui.ModelAndView;
import cn.taketoday.web.ui.ModelAttributes;
import cn.taketoday.web.utils.WebUtils;

import static cn.taketoday.context.Constant.DEFAULT_CHARSET;

/**
 * Context holder for request-specific state.
 *
 * @author TODAY <br>
 * 2019-06-22 15:48
 * @since 2.3.7
 */
public abstract class RequestContext implements Readable, Writable, Model, Flushable {

  public static final HttpCookie[] EMPTY_COOKIES = {};

  protected String contextPath;
  protected Object requestBody;
  protected HttpCookie[] cookies;
  protected String[] pathVariables;
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
   *
   * @since 3.0.1
   */
  public abstract String getScheme();

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
    String contextPath = this.contextPath;
    if (contextPath == null) {
      contextPath = doGetContextPath();
      this.contextPath = contextPath;
    }
    return contextPath;
  }

  protected String doGetContextPath() {
    return Constant.BLANK;
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
    String queryString = this.queryString;
    if (queryString == null) {
      queryString = doGetQueryString();
      this.queryString = queryString;
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
    HttpCookie[] cookies = this.cookies;
    if (cookies == null) {
      cookies = doGetCookies();
      this.cookies = cookies;
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
   * @param name
   *         Cookie name
   *
   * @return a {@link HttpCookie} object the client sent with this request. This
   * method returns <code>null</code> if no target cookie were sent.
   *
   * @since 2.3.7
   */
  public HttpCookie getCookie(final String name) {
    for (final HttpCookie cookie : getCookies()) {
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
   * @param cookie
   *         the Cookie to return to the client
   */
  public void addCookie(HttpCookie cookie) {
    responseCookies().add(cookie);
  }

  public ArrayList<HttpCookie> responseCookies() {
    ArrayList<HttpCookie> responseCookies = this.responseCookies;
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
  public Map<String, String[]> getParameters() {
    Map<String, String[]> parameters = this.parameters;
    if (parameters == null) {
      parameters = doGetParameters();
      this.parameters = parameters;
    }
    return parameters;
  }

  protected Map<String, String[]> doGetParameters() {
    final String queryString = StringUtils.decodeUrl(getQueryString());
    final MultiValueMap<String, String> parameters = WebUtils.parseParameters(queryString);
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
    final Map<String, String[]> parameters = getParameters();
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
   * @param name
   *         a <code>String</code> containing the name of the parameter whose
   *         value is requested
   *
   * @return an array of <code>String</code> objects containing the parameter's
   * values
   *
   * @see #getParameters()
   */
  public String[] getParameters(String name) {
    final Map<String, String[]> parameters = getParameters();
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
   * @param name
   *         a <code>String</code> specifying the name of the parameter
   *
   * @return a <code>String</code> representing the single value of the parameter
   *
   * @see #getParameters(String)
   */
  public String getParameter(String name) {
    final String[] parameters = getParameters(name);
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
  public final String getMethod() {
    if (method == null) {
      method = doGetMethod();
    }
    return method;
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

  /**
   * Retrieves the body of the request as binary data using a {@link InputStream}.
   * Either this method or {@link #getReader} may be called to read the body, not
   * both.
   *
   * @return a {@link InputStream} object containing the body of the request
   *
   * @throws IllegalStateException
   *         For Servlet Environment if the {@link #getReader} method has
   *         already been called for this request
   * @throws IOException
   *         if an input or output exception occurred
   */
  @Override
  public InputStream getInputStream() throws IOException {
    InputStream inputStream = this.inputStream;
    if (inputStream == null) {
      inputStream = doGetInputStream();
      this.inputStream = inputStream;
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
   *
   * @throws IllegalStateException
   *         For Servlet Environment if {@link #getInputStream} method has
   *         been called on this request
   * @throws IOException
   *         if an input or output exception occurred
   * @see #getInputStream
   */
  @Override
  public BufferedReader getReader() throws IOException {
    BufferedReader reader = this.reader;
    if (reader == null) {
      reader = doGetReader();
      this.reader = reader;
    }
    return reader;
  }

  protected BufferedReader doGetReader() throws IOException {
    return new BufferedReader(new InputStreamReader(getInputStream(), DEFAULT_CHARSET));
  }

  // -----------------------------------------------------

  /**
   * Get all {@link MultipartFile}s from current request
   */
  public MultiValueMap<String, MultipartFile> multipartFiles() {
    MultiValueMap<String, MultipartFile> multipartFiles = this.multipartFiles;
    if (multipartFiles == null) {
      multipartFiles = parseMultipartFiles();
      this.multipartFiles = multipartFiles;
    }
    return multipartFiles;
  }

  /**
   * map list {@link MultipartFile}
   *
   * @throws cn.taketoday.web.resolver.NotMultipartRequestException
   *         if this request is not of type multipart/form-data
   * @throws cn.taketoday.web.resolver.MultipartParsingException
   *         multipart parse failed
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
   *
   * @since 3.0
   */
  public HttpHeaders requestHeaders() {
    HttpHeaders ret = this.requestHeaders;
    if (ret == null) {
      this.requestHeaders = ret = createRequestHeaders();
    }
    return ret;
  }

  /**
   * @since 3.0
   */
  protected abstract HttpHeaders createRequestHeaders();

  // ---------------- response

  /**
   * Get a {@link ModelAndView}
   * <p>
   * If there isn't a {@link ModelAndView} in this {@link RequestContext},
   * <b>Create One</b>
   *
   * @return Returns {@link ModelAndView}, never be null but except
   * {@link ApplicationNotStartedContext}
   */
  public ModelAndView modelAndView() {
    ModelAndView ret = this.modelAndView;
    if (ret == null) {
      ret = new ModelAndView(this);
      this.modelAndView = ret;
    }
    return ret;
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
   * @param length
   *         an long specifying the length of the content being returned to the
   *         client; sets the Content-Length header
   */
  public void setContentLength(long length) {
    responseHeaders().setContentLength(length);
  }

  /**
   * Returns a boolean indicating if the response has been committed. A committed
   * response has already had its status code and headers written.
   *
   * @return a boolean indicating if the response has been committed
   *
   * @see #reset
   */
  public abstract boolean committed();

  /**
   * Clears any data that exists in the buffer as well as the status code,
   * headers. The state of calling {@link #getWriter} or {@link #getOutputStream}
   * is also cleared. It is legal, for instance, to call {@link #getWriter},
   * {@link #reset} and then {@link #getOutputStream}. If {@link #getWriter} or
   * {@link #getOutputStream} have been called before this method, then the
   * corresponding returned Writer or OutputStream will be staled and the
   * behavior of using the stale object is undefined. If the response has been
   * committed, this method throws an <code>IllegalStateException</code>.
   *
   * @throws IllegalStateException
   *         if the response has already been committed
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
   * @param location
   *         the redirect location URL
   *
   * @throws IOException
   *         If an input or output exception occurs
   * @throws IllegalStateException
   *         If the response was committed or if a partial URL is given and
   *         cannot be converted into a valid URL
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
   * @param sc
   *         the status code
   */
  public abstract void setStatus(int sc);

  /**
   * Sets the status code and message for this response.
   *
   * @param status
   *         the status code
   * @param message
   *         the status message
   */
  public abstract void setStatus(int status, String message);

  /**
   * Sets the status code and message for this response.
   *
   * @param status
   *         the status
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
   * @param sc
   *         the error status code
   *
   * @throws IOException
   *         If an input or output exception occurs
   * @throws IllegalStateException
   *         If the response was committed before this method call
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
   * @param sc
   *         the error status code
   * @param msg
   *         the descriptive message
   *
   * @throws IOException
   *         If an input or output exception occurs
   * @throws IllegalStateException
   *         If the response was committed
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
   *
   * @throws IllegalStateException
   *         For Servlet Environment if the <code>getWriter</code> method
   *         has been called on this response
   * @throws IOException
   *         if an input or output exception occurred
   * @see #getWriter
   * @see #reset
   */
  @Override
  public OutputStream getOutputStream() throws IOException {
    OutputStream outputStream = this.outputStream;
    if (outputStream == null) {
      outputStream = doGetOutputStream();
      this.outputStream = outputStream;
    }
    return outputStream;
  }

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
   *
   * @throws IOException
   *         if an input or output exception occurred
   * @throws IllegalStateException
   *         For Servlet Environment if the <code>getOutputStream</code>
   *         method has already been called for this response object
   * @see #getOutputStream
   * @see #reset
   */
  @Override
  public PrintWriter getWriter() throws IOException {
    PrintWriter writer = this.writer;
    if (writer == null) {
      writer = doGetWriter();
      this.writer = writer;
    }
    return writer;
  }

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
   * @param contentType
   *         a <code>String</code> specifying the MIME type of the content
   */
  public void setContentType(String contentType) {
    responseHeaders().set(Constant.CONTENT_TYPE, contentType);
  }

  /**
   * Get request HTTP headers
   *
   * @since 3.0
   */
  public HttpHeaders responseHeaders() {
    HttpHeaders ret = this.responseHeaders;
    if (ret == null) {
      this.responseHeaders = ret = createResponseHeaders();
    }
    return ret;
  }

  /**
   * @since 3.0
   */
  protected HttpHeaders createResponseHeaders() {
    return new DefaultHttpHeaders();
  }

  // ----------------------

  /**
   * Native request : HttpServletRequest
   */
  public abstract <T> T nativeRequest();

  public abstract <T> T nativeRequest(Class<T> requestClass);

  public abstract <T> T nativeResponse();

  public abstract <T> T nativeResponse(Class<T> responseClass);

  // ------------------

  /**
   * Request body object
   */
  public Object requestBody() {
    return requestBody;
  }

  /**
   * Cache request body object
   * <p>
   * If input body is {@code null} will cache {@link cn.taketoday.context.EmptyObject#INSTANCE}
   * </p>
   *
   * @param body
   *         Target request body object
   */
  public void setRequestBody(Object body) {
    this.requestBody = body != null ? body : EmptyObject.INSTANCE;
  }

  public String[] pathVariables() {
    return pathVariables;
  }

  /**
   * set current {@link PathVariable}s
   *
   * @param variables
   *         {@link PathVariable}s
   *
   * @return input variables
   */
  public String[] pathVariables(String[] variables) {
    return this.pathVariables = variables;
  }

  // Model

  /**
   * @since 3.0
   */
  private Model obtainModel() {
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
    return obtainModel().containsAttribute(name);
  }

  @Override
  public void setAttributes(Map<String, Object> attributes) {
    obtainModel().setAttributes(attributes);
  }

  @Override
  public Object getAttribute(String name) {
    return obtainModel().getAttribute(name);
  }

  @Override
  public <T> T getAttribute(String name, Class<T> targetClass) {
    return obtainModel().getAttribute(name, targetClass);
  }

  @Override
  public void setAttribute(String name, Object value) {
    obtainModel().setAttribute(name, value);
  }

  @Override
  public Object removeAttribute(String name) {
    return obtainModel().removeAttribute(name);
  }

  @Override
  public Map<String, Object> asMap() {
    return obtainModel().asMap();
  }

  @Override
  public void clear() {
    obtainModel().clear();
  }

  protected void resetResponseHeader() {
    if (responseHeaders != null) {
      responseHeaders.clear();
    }
  }

  @Override
  public void flush() throws IOException {
    final PrintWriter writer = this.writer;
    if (writer != null) {
      writer.flush();
    }
    else {
      final OutputStream outputStream = this.outputStream;
      if (outputStream != null) {
        outputStream.flush();
      }
    }
  }

  /**
   * cleanup multipart in this request context
   */
  public void cleanupMultipartFiles() {
    final MultiValueMap<String, MultipartFile> multipartFiles = this.multipartFiles;
    if (!CollectionUtils.isEmpty(multipartFiles)) {
      for (final Map.Entry<String, List<MultipartFile>> entry : multipartFiles.entrySet()) {
        final List<MultipartFile> value = entry.getValue();
        for (final MultipartFile multipartFile : value) {
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
    return getMethod() + " " + getRequestURL();
  }

}
