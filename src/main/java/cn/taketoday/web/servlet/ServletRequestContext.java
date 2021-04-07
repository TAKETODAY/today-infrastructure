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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.web.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpCookie;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.web.AbstractRequestContext;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.http.DefaultHttpHeaders;
import cn.taketoday.web.http.HttpHeaders;
import cn.taketoday.web.multipart.DefaultMultipartFile;
import cn.taketoday.web.multipart.MultipartFile;
import cn.taketoday.web.resolver.MultipartFileParsingException;
import cn.taketoday.web.resolver.NotMultipartRequestException;
import cn.taketoday.web.ui.Model;
import cn.taketoday.web.ui.ModelAttributes;
import cn.taketoday.web.utils.ServletUtils;

/**
 * Servlet environment implementation
 *
 * @author TODAY 2019-07-07 22:27
 * @since 2.3.7
 */
public class ServletRequestContext
        extends AbstractRequestContext implements RequestContext {

  private final HttpServletRequest request;
  private final HttpServletResponse response;

  public ServletRequestContext(HttpServletRequest request, HttpServletResponse response) {
    this.request = request;
    this.response = response;
  }

  public HttpServletRequest getRequest() {
    return request;
  }

  @Override
  protected String getContextPathInternal() {
    return request.getContextPath();
  }

  @SuppressWarnings("unchecked")
  public <T> T nativeRequest() {
    return (T) request;
  }

  @Override
  public <T> T nativeRequest(Class<T> requestClass) {
    return ServletUtils.getNativeRequest(request, requestClass);
  }

  @Override
  public <T> T nativeResponse(Class<T> responseClass) {
    return ServletUtils.getNativeResponse(response, responseClass);
  }

  @SuppressWarnings("unchecked")
  public <T> T nativeResponse() {
    return (T) response;
  }

  @Override
  protected OutputStream getOutputStreamInternal() throws IOException {
    return response.getOutputStream();
  }

  @Override
  protected InputStream getInputStreamInternal() throws IOException {
    return request.getInputStream();
  }

  @Override
  protected PrintWriter getWriterInternal() throws IOException {
    return response.getWriter();
  }

  @Override
  public BufferedReader getReaderInternal() throws IOException {
    return request.getReader();
  }

  @Override
  public String getRequestURI() {
    return request.getRequestURI();
  }

  @Override
  public String getRequestURL() {
    return request.getRequestURL().toString();
  }

  @Override
  public String getQueryString() {
    return request.getQueryString();
  }

  @Override
  protected HttpCookie[] getCookiesInternal() {

    final Cookie[] servletCookies = request.getCookies();
    if (ObjectUtils.isEmpty(servletCookies)) { // there is not cookies
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
  public Map<String, String[]> getParameters() {
    return request.getParameterMap();
  }

  @Override
  public Enumeration<String> getParameterNames() {
    return request.getParameterNames();
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
  public String getMethod() {
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
  public boolean committed() {
    return response.isCommitted();
  }

  @Override
  public void reset() {
    resetResponseHeader();

    response.reset();
  }

  @Override
  public void addCookie(final HttpCookie cookie) {

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
    final HttpServletRequest request = this.request;
    final DefaultHttpHeaders httpHeaders = new DefaultHttpHeaders();
    final Enumeration<String> headerNames = request.getHeaderNames();
    while (headerNames.hasMoreElements()) {
      final String name = headerNames.nextElement();
      final Enumeration<String> headers = request.getHeaders(name);
      httpHeaders.addAll(name, Collections.list(headers));
    }
    return httpHeaders;
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
  protected Map<String, List<MultipartFile>> parseMultipartFiles() {
    final HashMap<String, List<MultipartFile>> multipartFiles = new HashMap<>();
    final class MappingFunction implements Function<String, List<MultipartFile>> {
      @Override
      public List<MultipartFile> apply(String k) {
        return new LinkedList<>();
      }
    }
    final MappingFunction mappingFunction = new MappingFunction();
    try {
      for (final Part part : request.getParts()) {
        final String name = part.getName();
        List<MultipartFile> parts = multipartFiles.computeIfAbsent(name, mappingFunction);
        parts.add(new DefaultMultipartFile(part));
      }
      return multipartFiles;
    }
    catch (IOException e) {
      throw new MultipartFileParsingException("MultipartFile parsing failed.", e);
    }
    catch (ServletException e) {
      throw new NotMultipartRequestException("This is not a multipart request", e);
    }
  }

  @Override
  protected void doApplyHeaders(final HttpHeaders responseHeaders) {
    final HttpServletResponse response = this.response;
    for (final Map.Entry<String, List<String>> entry : responseHeaders.entrySet()) {
      final String headerName = entry.getKey();
      for (final String value : entry.getValue()) {
        response.addHeader(headerName, value);
      }
    }
  }

  @Override
  public void flush() throws IOException {
    response.flushBuffer();
  }

  // Model

  @Override
  protected Model createModel() {
    final class RequestModel extends ModelAttributes {
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
    }
    return new RequestModel();
  }
}
