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

package cn.taketoday.web.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serial;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.http.DefaultHttpHeaders;
import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.InvalidMediaTypeException;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ResponseCookie;
import cn.taketoday.http.server.PathContainer;
import cn.taketoday.http.server.RequestPath;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.CompositeIterator;
import cn.taketoday.util.ExceptionUtils;
import cn.taketoday.util.LinkedCaseInsensitiveMap;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.DispatcherHandler;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.ServletIndicator;
import cn.taketoday.web.context.async.AsyncWebRequest;
import cn.taketoday.web.multipart.MultipartRequest;
import cn.taketoday.web.util.UriUtils;
import cn.taketoday.web.mock.http.Cookie;
import cn.taketoday.web.mock.http.HttpServletMapping;
import cn.taketoday.web.mock.http.HttpServletRequest;
import cn.taketoday.web.mock.http.HttpServletResponse;
import cn.taketoday.web.mock.http.MappingMatch;

/**
 * Servlet environment implementation
 *
 * @author TODAY 2019-07-07 22:27
 * @since 2.3.7
 */
public final class ServletRequestContext extends RequestContext implements ServletIndicator {

  @Serial
  private static final long serialVersionUID = 1L;

  private final HttpServletRequest request;
  private final HttpServletResponse response;
  private final long requestTimeMillis = System.currentTimeMillis();

  private boolean bodyUsed = false;
  private boolean headersWritten = false;

  public ServletRequestContext(ApplicationContext context,
          HttpServletRequest request, HttpServletResponse response) {
    this(context, request, response, null);
  }

  public ServletRequestContext(ApplicationContext context, HttpServletRequest request,
          HttpServletResponse response, DispatcherHandler dispatcherHandler) {
    super(context, dispatcherHandler);
    this.request = request;
    this.response = response;
  }

  @Override
  public HttpServletRequest getRequest() {
    return request;
  }

  @Override
  public HttpServletResponse getResponse() {
    return response;
  }

  @Override
  public long getRequestTimeMillis() {
    return requestTimeMillis;
  }

  @Nullable
  @Override
  protected String initId() {
    return request.getRequestId();
  }

  @Override
  public String getScheme() {
    return request.getScheme();
  }

  @Override
  public String getServerName() {
    return request.getServerName();
  }

  @Override
  public int getServerPort() {
    return request.getServerPort();
  }

  public String getContextPath() {
    return request.getContextPath();
  }

  @Override
  protected RequestPath doGetRequestPath() {
    return ServletRequestPath.parse(this);
  }

  @SuppressWarnings("unchecked")
  public <T> T nativeRequest() {
    return (T) request;
  }

  @Override
  protected OutputStream doGetOutputStream() throws IOException {
    this.bodyUsed = true;
    writeHeaders();
    return response.getOutputStream();
  }

  @Override
  protected InputStream doGetInputStream() throws IOException {
    return request.getInputStream();
  }

  @Override
  protected PrintWriter doGetWriter() throws IOException {
    return response.getWriter();
  }

  @Override
  public BufferedReader doGetReader() throws IOException {
    return request.getReader();
  }

  @Override
  public String doGetRequestURI() {
    return request.getRequestURI();
  }

  @Override
  public String getRequestURL() {
    return request.getRequestURL().toString();
  }

  @Override
  public String doGetQueryString() {
    return request.getQueryString();
  }

  @Override
  protected HttpCookie[] doGetCookies() {

    Cookie[] servletCookies = request.getCookies();
    if (ObjectUtils.isEmpty(servletCookies)) { // there is no cookies
      return EMPTY_COOKIES;
    }
    HttpCookie[] cookies = new HttpCookie[servletCookies.length];

    int i = 0;
    for (Cookie servletCookie : servletCookies) {

      HttpCookie httpCookie = new HttpCookie(servletCookie.getName(), servletCookie.getValue());
      cookies[i++] = httpCookie;
    }
    return cookies;
  }

  @Override
  protected MultiValueMap<String, String> doGetParameters() {
    var ret = MultiValueMap.<String, String>forSmartListAdaption(new LinkedHashMap<>());
    for (var entry : request.getParameterMap().entrySet()) {
      ret.addAll(entry.getKey(), entry.getValue());
    }
    return ret;
  }

  @Override
  public String[] getParameters(String name) {
    return request.getParameterValues(name);
  }

  @Override
  public String getParameter(String name) {
    return request.getParameter(name);
  }

  @Override
  protected String doGetMethod() {
    return request.getMethod();
  }

  @Override
  public String getRemoteAddress() {
    return request.getRemoteAddr();
  }

  @Override
  public long getContentLength() {
    return request.getContentLengthLong();
  }

  @Override
  public String getContentType() {
    return request.getContentType();
  }

  @Override
  public void setContentType(String contentType) {
    super.setContentType(contentType);
    response.setContentType(contentType);
  }

  @Override
  public String getResponseContentType() {
    String contentType = super.getResponseContentType();
    if (contentType == null) {
      return response.getContentType();
    }
    return contentType;
  }

  @Override
  public void setContentLength(long length) {
    response.setContentLengthLong(length);
  }

  @Override
  public boolean isCommitted() {
    return response.isCommitted();
  }

  @Override
  protected Locale doGetLocale() {
    return request.getLocale();
  }

  @Override
  public void reset() {
    super.reset();
    response.reset();
    this.headersWritten = false;
  }

  @Override
  public void sendRedirect(String location) throws IOException {
    response.sendRedirect(location);
  }

  @Override
  public void setStatus(int sc) {
    response.setStatus(sc);
  }

  @Override
  public int getStatus() {
    return response.getStatus();
  }

  // HTTP headers

  /**
   * @since 3.0
   */
  @Override
  protected HttpHeaders createRequestHeaders() {
    DefaultHttpHeaders httpHeaders = new DefaultHttpHeaders();

    Enumeration<String> headerNames = request.getHeaderNames();
    while (headerNames.hasMoreElements()) {
      String name = headerNames.nextElement();
      Enumeration<String> headers = request.getHeaders(name);
      httpHeaders.addAll(name, headers);
    }

    // HttpServletRequest exposes some headers as properties:
    // we should include those if not already present
    try {
      MediaType contentType = httpHeaders.getContentType();
      if (contentType == null) {
        String requestContentType = request.getContentType();
        if (StringUtils.isNotEmpty(requestContentType)) {
          contentType = MediaType.parseMediaType(requestContentType);
          if (contentType.isConcrete()) {
            httpHeaders.setContentType(contentType);
          }
        }
      }
      if (contentType != null && contentType.getCharset() == null) {
        String requestEncoding = request.getCharacterEncoding();
        if (StringUtils.isNotEmpty(requestEncoding)) {
          Charset charSet = Charset.forName(requestEncoding);
          Map<String, String> params = new LinkedCaseInsensitiveMap<>();
          params.putAll(contentType.getParameters());
          params.put("charset", charSet.toString());
          MediaType mediaType = new MediaType(contentType.getType(), contentType.getSubtype(), params);
          httpHeaders.setContentType(mediaType);
        }
      }
    }
    catch (InvalidMediaTypeException ex) {
      // Ignore: simply not exposing an invalid content type in HttpHeaders...
    }

    if (httpHeaders.getContentLength() < 0) {
      int requestContentLength = request.getContentLength();
      if (requestContentLength != -1) {
        httpHeaders.setContentLength(requestContentLength);
      }
    }

    return httpHeaders;
  }

  @Override
  public void sendError(int sc) throws IOException {
    response.sendError(sc);
  }

  @Override
  public void sendError(int sc, @Nullable String msg) throws IOException {
    response.sendError(sc, msg);
  }

  @Override
  protected MultipartRequest createMultipartRequest() {
    if (request instanceof MultipartRequest) {
      return (MultipartRequest) request;
    }
    return new ServletMultipartRequest(request);
  }

  @Override
  protected AsyncWebRequest createAsyncWebRequest() {
    return new StandardServletAsyncWebRequest(this);
  }

  @Override
  protected void writeHeaders() {
    if (!headersWritten) {
      HttpHeaders headers = responseHeaders();
      for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
        String headerName = entry.getKey();
        for (String headerValue : entry.getValue()) {
          response.addHeader(headerName, headerValue);
        }
      }

      // HttpServletResponse exposes some headers as properties: we should include those if not already present
      MediaType contentType = headers.getContentType();
      if (response.getContentType() == null && contentType != null) {
        response.setContentType(contentType.toString());
      }

      long contentLength = headers.getContentLength();
      if (contentLength != -1) {
        response.setContentLengthLong(contentLength);
      }

      // apply cookies
      ArrayList<HttpCookie> responseCookies = this.responseCookies;
      if (responseCookies != null) {
        for (HttpCookie cookie : responseCookies) {
          Cookie servletCookie = new Cookie(cookie.getName(), cookie.getValue());
          if (cookie instanceof ResponseCookie responseCookie) {
            servletCookie.setPath(responseCookie.getPath());
            if (responseCookie.getDomain() != null) {
              servletCookie.setDomain(responseCookie.getDomain());
            }
            servletCookie.setSecure(responseCookie.isSecure());
            servletCookie.setHttpOnly(responseCookie.isHttpOnly());
            servletCookie.setMaxAge((int) responseCookie.getMaxAge().toSeconds());
            servletCookie.setAttribute("SameSite", responseCookie.getSameSite());
          }

          response.addCookie(servletCookie);
        }
      }

      this.headersWritten = true;
    }
  }

  @Override
  public void flush() throws IOException {
    writeHeaders();

    if (bodyUsed) {
      response.flushBuffer();
    }
  }

  @Override
  protected void postRequestCompleted(@Nullable Throwable notHandled) {
    if (notHandled == null) {
      try {
        flush();
      }
      catch (IOException e) {
        throw ExceptionUtils.sneakyThrow(e);
      }
    }
  }

  // attributes

  @Override
  public void setAttribute(String name, Object value) {
    super.setAttribute(name, value);
    request.setAttribute(name, value);
  }

  @Override
  public Object removeAttribute(String name) {
    request.removeAttribute(name);
    return super.removeAttribute(name);
  }

  @Override
  public void clearAttributes() {
    super.clearAttributes();
    CollectionUtils.iterate(request.getAttributeNames(), request::removeAttribute);
  }

  @Override
  public Object getAttribute(String name) {
    Object attribute = super.getAttribute(name);
    if (attribute == null) {
      attribute = request.getAttribute(name);
      if (attribute != null) {
        super.setAttribute(name, attribute);
      }
    }
    return attribute;
  }

  @Override
  public boolean hasAttributes() {
    return super.hasAttributes() || request.getAttributeNames().hasMoreElements();
  }

  @Override
  public boolean hasAttribute(String name) {
    return super.hasAttribute(name) || request.getAttribute(name) != null;
  }

  @Override
  public String[] getAttributeNames() {
    if (super.hasAttributes()) {
      ArrayList<String> names = new ArrayList<>(8);
      CollectionUtils.addAll(names, super.getAttributeNames());
      CollectionUtils.addAll(names, request.getAttributeNames());
      return StringUtils.toStringArray(names);
    }
    else {
      return StringUtils.toStringArray(request.getAttributeNames());
    }
  }

  @Override
  public Iterator<String> attributeNames() {
    if (super.hasAttributes()) {
      CompositeIterator<String> iterator = new CompositeIterator<>();
      iterator.add(super.attributeNames());
      iterator.add(request.getAttributeNames().asIterator());
      return iterator;
    }
    else {
      return request.getAttributeNames().asIterator();
    }
  }

  /**
   * Simple wrapper around the default {@link RequestPath} implementation that
   * supports a servletPath as an additional prefix to be omitted from
   * {@link #pathWithinApplication()}.
   */
  private static final class ServletRequestPath extends RequestPath {

    private final RequestPath requestPath;

    private final PathContainer contextPath;

    private ServletRequestPath(String rawPath, @Nullable String contextPath, String servletPath) {
      this.requestPath = RequestPath.parse(rawPath, contextPath + servletPath);
      this.contextPath = StringUtils.hasText(contextPath) ? PathContainer.parsePath(contextPath) : PathContainer.empty();
    }

    @Override
    public String value() {
      return this.requestPath.value();
    }

    @Override
    public List<Element> elements() {
      return this.requestPath.elements();
    }

    @Override
    public PathContainer contextPath() {
      return this.contextPath;
    }

    @Override
    public PathContainer pathWithinApplication() {
      return this.requestPath.pathWithinApplication();
    }

    @Override
    public RequestPath modifyContextPath(String contextPath) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(@Nullable Object other) {
      if (this == other) {
        return true;
      }
      if (other == null || getClass() != other.getClass()) {
        return false;
      }
      return (this.requestPath.equals(((ServletRequestPath) other).requestPath));
    }

    @Override
    public int hashCode() {
      return this.requestPath.hashCode();
    }

    @Override
    public String toString() {
      return this.requestPath.toString();
    }

    public static RequestPath parse(ServletRequestContext request) {
      HttpServletRequest servletRequest = request.getRequest();
      String servletPath = getServletPath(servletRequest);
      if (StringUtils.hasText(servletPath)) {
        if (servletPath.endsWith("/")) {
          servletPath = servletPath.substring(0, servletPath.length() - 1);
        }
        return new ServletRequestPath(request.getRequestURI(), servletRequest.getContextPath(), servletPath);
      }
      return RequestPath.parse(request.getRequestURI(), servletRequest.getContextPath());
    }

    @Nullable
    public static String getServletPath(HttpServletRequest request) {
      HttpServletMapping mapping = request.getHttpServletMapping();
      if (ObjectUtils.nullSafeEquals(mapping.getMappingMatch(), MappingMatch.PATH)) {
        String servletPath = request.getServletPath();
        return UriUtils.encodePath(servletPath, StandardCharsets.UTF_8);
      }
      return null;
    }
  }

}
