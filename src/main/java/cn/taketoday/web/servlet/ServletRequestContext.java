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
import java.net.HttpCookie;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import cn.taketoday.core.DefaultMultiValueMap;
import cn.taketoday.core.MultiValueMap;
import cn.taketoday.util.EnumerationIterator;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.http.DefaultHttpHeaders;
import cn.taketoday.web.http.HttpHeaders;
import cn.taketoday.web.multipart.MultipartFile;
import cn.taketoday.web.multipart.ServletPartMultipartFile;
import cn.taketoday.web.resolver.MultipartParsingException;
import cn.taketoday.web.resolver.NotMultipartRequestException;
import cn.taketoday.web.view.Model;
import cn.taketoday.web.view.ModelAttributes;

/**
 * Servlet environment implementation
 *
 * @author TODAY 2019-07-07 22:27
 * @since 2.3.7
 */
public final class ServletRequestContext extends RequestContext {

  private final HttpServletRequest request;
  private final HttpServletResponse response;

  public ServletRequestContext(HttpServletRequest request, HttpServletResponse response) {
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

    final Cookie[] servletCookies = request.getCookies();
    if (ObjectUtils.isEmpty(servletCookies)) { // there is no cookies
      return EMPTY_COOKIES;
    }
    final HttpCookie[] cookies = new HttpCookie[servletCookies.length];

    int i = 0;
    for (final Cookie servletCookie : servletCookies) {

      final HttpCookie httpCookie = new HttpCookie(servletCookie.getName(), servletCookie.getValue());

      httpCookie.setPath(servletCookie.getPath());
      httpCookie.setDomain(servletCookie.getDomain());
      httpCookie.setMaxAge(servletCookie.getMaxAge());
      httpCookie.setSecure(servletCookie.getSecure());
      httpCookie.setVersion(servletCookie.getVersion());
      httpCookie.setComment(servletCookie.getComment());
      httpCookie.setHttpOnly(servletCookie.isHttpOnly());

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
    return new EnumerationIterator<>(request.getParameterNames());
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
  public void addCookie(final HttpCookie cookie) {
    super.addCookie(cookie);

    final Cookie servletCookie = new Cookie(cookie.getName(), cookie.getValue());
    servletCookie.setPath(cookie.getPath());
    if (cookie.getDomain() != null) {
      servletCookie.setDomain(cookie.getDomain());
    }
    servletCookie.setSecure(cookie.getSecure());
    servletCookie.setComment(cookie.getComment());
    servletCookie.setVersion(cookie.getVersion());
    servletCookie.setHttpOnly(cookie.isHttpOnly());
    servletCookie.setMaxAge((int) cookie.getMaxAge());

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
  public void setStatus(final int status, final String message) {
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
      for (final String value : values) {
        add(key, value);
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
    DefaultMultiValueMap<String, MultipartFile> multipartFiles = new DefaultMultiValueMap<>();
    try {
      for (final Part part : request.getParts()) {
        final String name = part.getName();
        multipartFiles.add(name, new ServletPartMultipartFile(part));
      }
      return multipartFiles;
    }
    catch (IOException e) {
      throw new MultipartParsingException("MultipartFile parsing failed.", e);
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

  // Model

  @Override
  protected Model createModel() {
    return new ServletRequestModel();
  }

  private final class ServletRequestModel extends ModelAttributes {
    private static final long serialVersionUID = 1L;

    // auto flush to request attributes
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
    public void clear() {
      super.clear();
      Enumeration<String> attributeNames = request.getAttributeNames();
      while (attributeNames.hasMoreElements()) {
        final String name = attributeNames.nextElement();
        request.removeAttribute(name);
      }
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
    public boolean containsAttribute(String name) {
      return hasAttribute(name) || request.getAttribute(name) != null;
    }

  }

}
