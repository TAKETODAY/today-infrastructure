/*
 * Copyright 2017 - 2023 the original author or authors.
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
import java.net.URI;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.http.DefaultHttpHeaders;
import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.ResponseCookie;
import cn.taketoday.http.server.PathContainer;
import cn.taketoday.http.server.RequestPath;
import cn.taketoday.http.server.ServerHttpResponse;
import cn.taketoday.http.server.ServletServerHttpResponse;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.CompositeIterator;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.DispatcherHandler;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.ServletIndicator;
import cn.taketoday.web.context.async.AsyncWebRequest;
import cn.taketoday.web.context.async.StandardServletAsyncWebRequest;
import cn.taketoday.web.multipart.MultipartRequest;
import cn.taketoday.web.multipart.support.ServletMultipartRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/27 16:36
 */
@SuppressWarnings("serial")
public class MockServletRequestContext extends RequestContext implements ServletIndicator {

  private final HttpServletRequest request;
  private final HttpServletResponse response;
  private final long requestTimeMillis = System.currentTimeMillis();

  public MockServletRequestContext(HttpServletRequest request, HttpServletResponse response) {
    this(null, request, response);
  }

  public MockServletRequestContext(ApplicationContext context,
          HttpServletRequest request, HttpServletResponse response) {
    this(context, request, response, null);
  }

  public MockServletRequestContext(ApplicationContext context, HttpServletRequest request,
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

  @Override
  public String getScheme() {
    return request.getScheme();
  }

  @Override
  public String getServerName() {
    return null;
  }

  @Override
  public int getServerPort() {
    return 0;
  }

  @Override
  protected String doGetContextPath() {
    return request.getContextPath();
  }

  @SuppressWarnings("unchecked")
  public <T> T nativeRequest() {
    return (T) request;
  }

  @Override
  public <T> T unwrapRequest(Class<T> requestClass) {
    return ServletUtils.getNativeRequest(request, requestClass);
  }

  @SuppressWarnings("unchecked")
  public <T> T nativeResponse() {
    return (T) response;
  }

  @Override
  protected OutputStream doGetOutputStream() throws IOException {
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
  public Map<String, String[]> doGetParameters() {
    return request.getParameterMap();
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
    response.setContentType(contentType);
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
  public void reset() {
    super.reset();
    response.reset();
  }

  @Override
  public void addCookie(HttpCookie cookie) {
    super.addCookie(cookie);

    Cookie servletCookie = new Cookie(cookie.getName(), cookie.getValue());
    if (cookie instanceof ResponseCookie responseCookie) {
      servletCookie.setPath(responseCookie.getPath());
      if (responseCookie.getDomain() != null) {
        servletCookie.setDomain(responseCookie.getDomain());
      }
      servletCookie.setSecure(responseCookie.isSecure());
      servletCookie.setHttpOnly(responseCookie.isHttpOnly());
      servletCookie.setMaxAge((int) responseCookie.getMaxAge().toSeconds());
    }

    response.addCookie(servletCookie);
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
    final DefaultHttpHeaders httpHeaders = new DefaultHttpHeaders();
    final Enumeration<String> headerNames = request.getHeaderNames();
    while (headerNames.hasMoreElements()) {
      final String name = headerNames.nextElement();
      final Enumeration<String> headers = request.getHeaders(name);
      httpHeaders.addAll(name, headers);
    }
    return httpHeaders;
  }

  @Override
  public ServerHttpResponse asHttpOutputMessage() {
    return new ServletServerHttpResponse(response);
  }

  @Override
  public void sendError(int sc) throws IOException {
    response.sendError(sc);
  }

  @Override
  public void sendError(int sc, String msg) throws IOException {
    response.sendError(sc, msg);
  }

  // MultipartFiles

  public void setMultipartRequest(MultipartRequest multipartRequest) {
    this.multipartRequest = multipartRequest;
  }

  @Override
  protected MultipartRequest createMultipartRequest() {
    return new ServletMultipartRequest(request);
  }

  @Override
  protected AsyncWebRequest createAsyncWebRequest() {
    return new StandardServletAsyncWebRequest(request, response);
  }

  @Override
  public void flush() throws IOException {
    super.flush();
    response.flushBuffer();
  }

  // Model

  //
  public void setRequestHeaders(HttpHeaders requestHeaders) {
    this.requestHeaders = requestHeaders;
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

  public void setContextPath(String contextPath) {
    this.contextPath = contextPath;
  }

  public void setCookies(HttpCookie[] cookies) {
    this.cookies = cookies;
  }

  public void setWriter(PrintWriter writer) {
    this.writer = writer;
  }

  public void setReader(BufferedReader reader) {
    this.reader = reader;
  }

  public void setInputStream(InputStream inputStream) {
    this.inputStream = inputStream;
  }

  public void setOutputStream(OutputStream outputStream) {
    this.outputStream = outputStream;
  }

  public void setResponseHeaders(HttpHeaders responseHeaders) {
    this.responseHeaders = responseHeaders;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public void setRequestPath(String requestPath) {
    this.requestURI = requestPath;
  }

  public void setParameters(Map<String, String[]> parameters) {
    this.parameters = parameters;
  }

  public void setQueryString(String queryString) {
    this.queryString = queryString;
  }

  public void setResponseCookies(ArrayList<HttpCookie> responseCookies) {
    this.responseCookies = responseCookies;
  }

  public void setUri(URI uri) {
    this.uri = uri;
  }

  public void setHttpMethod(HttpMethod httpMethod) {
    this.httpMethod = httpMethod;
  }

  public void setLookupPath(RequestPath lookupPath) {
    this.lookupPath = lookupPath;
  }

  public void setPathWithinApplication(PathContainer pathWithinApplication) {
    this.pathWithinApplication = pathWithinApplication;
  }

  public void setLocale(Locale locale) {
    this.locale = locale;
  }

  public void setResponseContentType(String responseContentType) {
    this.responseContentType = responseContentType;
  }

  public void setNotModified(boolean notModified) {
    this.notModified = notModified;
  }

}
