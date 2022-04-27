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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.web.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.DefaultMultiValueMap;
import cn.taketoday.core.MultiValueMap;
import cn.taketoday.http.DefaultHttpHeaders;
import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.ResponseCookie;
import cn.taketoday.lang.Constant;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.CompositeIterator;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.bind.MultipartException;
import cn.taketoday.web.bind.NotMultipartRequestException;
import cn.taketoday.web.multipart.MultipartFile;
import cn.taketoday.web.multipart.ServletPartMultipartFile;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

/**
 * Servlet environment implementation
 *
 * @author TODAY 2019-07-07 22:27
 * @since 2.3.7
 */
public final class ServletRequestContext extends RequestContext {

  private final HttpServletRequest request;
  private final HttpServletResponse response;

  public ServletRequestContext(
          ApplicationContext context, HttpServletRequest request, HttpServletResponse response) {
    super(context);
    this.request = request;
    this.response = response;
  }

  public HttpServletRequest getRequest() {
    return request;
  }

  public HttpServletResponse getResponse() {
    return response;
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

  @Override
  public <T> T unwrapResponse(Class<T> responseClass) {
    return ServletUtils.getNativeResponse(response, responseClass);
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
  public String doGetRequestPath() {
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
  public Iterator<String> getParameterNames() {
    return request.getParameterNames().asIterator();
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
  public String remoteAddress() {
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
  public String getResponseContentType() {
    return response.getContentType();
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
  @SuppressWarnings("deprecation")
  public void setStatus(int status, String message) {
    response.setStatus(status, message);
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
  protected HttpHeaders createResponseHeaders() {
    return new ServletResponseHttpHeaders(response);
  }

  static final class ServletResponseHttpHeaders extends DefaultHttpHeaders {
    @Serial
    private static final long serialVersionUID = 1L;
    private final HttpServletResponse response;

    ServletResponseHttpHeaders(HttpServletResponse response) {
      this.response = response;
    }

    @Override
    public void set(String headerName, String headerValue) {
      super.set(headerName, headerValue);
      response.setHeader(headerName, headerValue);
    }

    @Override
    public void add(String headerName, String headerValue) {
      super.add(headerName, headerValue);
      response.addHeader(headerName, headerValue);
    }

    @Override
    public void addAll(String key, List<? extends String> values) {
      if (values != null) {
        for (final String value : values) {
          add(key, value);
        }
      }
    }

    @Override
    public void addAll(MultiValueMap<String, String> values) {
      values.forEach(this::addAll);
    }

    @Override
    public void setAll(Map<String, String> values) {
      values.forEach(this::set);
    }

    @Override
    public List<String> put(String key, List<String> values) {
      doPut(key, values, response);
      return super.put(key, values);
    }

    private static void doPut(String key, List<String> values, HttpServletResponse response) {
      for (final String value : values) {
        response.addHeader(key, value);
      }
    }

    @Override
    public void putAll(Map<? extends String, ? extends List<String>> map) {
      super.putAll(map);
      for (final Entry<? extends String, ? extends List<String>> entry : map.entrySet()) {
        doPut(entry.getKey(), entry.getValue(), response);
      }
    }
  }

  @Override
  public void sendError(int sc) throws IOException {
    response.sendError(sc);
  }

  @Override
  public void sendError(int sc, String msg) throws IOException {
    response.sendError(sc, msg);
  }

  // parseMultipartFiles

  @Override
  protected MultiValueMap<String, MultipartFile> parseMultipartFiles() {
    DefaultMultiValueMap<String, MultipartFile> multipartFiles = MultiValueMap.fromLinkedHashMap();
    try {
      for (final Part part : request.getParts()) {
        final String name = part.getName();
        multipartFiles.add(name, new ServletPartMultipartFile(part));
      }
      return multipartFiles;
    }
    catch (IOException e) {
      throw new MultipartException("MultipartFile parsing failed.", e);
    }
    catch (ServletException e) {
      throw new NotMultipartRequestException("This is not a multipart request", e);
    }
  }

  @Override
  public void flush() throws IOException {
    super.flush();
    response.flushBuffer();
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

}
