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
package cn.taketoday.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.HttpCookie;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import cn.taketoday.context.utils.Assert;
import cn.taketoday.web.http.HttpHeaders;
import cn.taketoday.web.multipart.MultipartFile;
import cn.taketoday.web.ui.ModelAndView;

/**
 * Holder class to expose the web request in the form of a thread-bound
 * {@link RequestContext} object.
 *
 * @author TODAY 2019-03-23 10:29
 * @see #replaceContextHolder(RequestThreadLocal)
 * @since 2.3.7
 */
public abstract class RequestContextHolder {
  public static final RequestContext ApplicationNotStartedContext = new ApplicationNotStartedContext();
  private static RequestThreadLocal contextHolder = new DefaultRequestThreadLocal();

  public static void resetContext() {
    contextHolder.remove();
  }

  public static void prepareContext(RequestContext requestContext) {
    contextHolder.set(requestContext);
  }

  public static RequestContext currentContext() {
    final RequestContext ret = getContext();
    return ret == null ? ApplicationNotStartedContext : ret;
  }

  public static RequestContext getContext() {
    return contextHolder.get();
  }

  public static <T> T currentRequest() {
    return currentContext().nativeRequest();
  }

  public static <T> T currentResponse() {
    return currentContext().nativeResponse();
  }

  /**
   * replace {@link RequestThreadLocal}
   *
   * @param contextHolder
   *         new {@link RequestThreadLocal} object
   */
  public static void replaceContextHolder(RequestThreadLocal contextHolder) {
    Assert.notNull(contextHolder, "contextHolder must not be null");
    RequestContextHolder.contextHolder = contextHolder;
  }

  @SuppressWarnings("serial")
  static class ApplicationNotStartedContext implements RequestContext, Serializable {

    @Override
    public Object getAttribute(String name) {
      return null;
    }

    @Override
    public <T> T getAttribute(String name, Class<T> targetClass) {
      return null;
    }

    @Override
    public void setAttribute(String name, Object value) {
    }

    @Override
    public Object removeAttribute(String name) {
      return null;
    }

    @Override
    public Map<String, Object> asMap() {
      return null;
    }

    @Override
    public void clear() {}

    @Override
    public String contentType() {
      return null;
    }

    @Override
    public HttpHeaders requestHeaders() {
      return null;
    }

    @Override
    public void flush() throws IOException {}

    @Override
    public String contextPath() {
      return null;
    }

    @Override
    public String requestURI() {
      return null;
    }

    @Override
    public String requestURL() {
      return null;
    }

    @Override
    public String queryString() {
      return null;
    }

    @Override
    public HttpCookie[] cookies() {
      return null;
    }

    @Override
    public HttpCookie cookie(String name) {
      return null;
    }

    @Override
    public void addCookie(HttpCookie cookie) {
    }

    @Override
    public Map<String, String[]> parameters() {
      return null;
    }

    @Override
    public Enumeration<String> parameterNames() {
      return null;
    }

    @Override
    public String[] parameters(String name) {
      return null;
    }

    @Override
    public String parameter(String name) {
      return null;
    }

    @Override
    public String method() {
      return null;
    }

    @Override
    public String remoteAddress() {
      return null;
    }

    @Override
    public long contentLength() {
      return 0;
    }

    @Override
    public InputStream getInputStream() throws IOException {
      return null;
    }

    @Override
    public BufferedReader getReader() throws IOException {
      return null;
    }

    @Override
    public Object requestBody() {
      return null;
    }

    @Override
    public void requestBody(Object body) {}

    @Override
    public String[] pathVariables() {
      return null;
    }

    @Override
    public String[] pathVariables(String[] variables) {
      return null;
    }

    @Override
    public Map<String, List<MultipartFile>> multipartFiles() {
      return null;
    }

    @Override
    public ModelAndView modelAndView() {
      return null;
    }

    @Override
    public boolean hasModelAndView() {
      return false;
    }

    @Override
    public void contentLength(long length) {
    }

    @Override
    public boolean committed() {
      return false;
    }

    @Override
    public void reset() {
    }

    @Override
    public void redirect(String location) throws IOException {
    }

    @Override
    public void status(int sc) {
    }

    @Override
    public void status(final int status, final String message) {
    }

    @Override
    public int status() {
      return 0;
    }

    @Override
    public void sendError(int sc) throws IOException {
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
      return null;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
      return null;
    }

    @Override public void contentType(String contentType) {

    }

    @Override public HttpHeaders responseHeaders() {
      return null;
    }

    @Override
    public <T> T nativeRequest() {
      return null;
    }

    @Override
    public <T> T nativeRequest(Class<T> requestClass) {
      return null;
    }

    @Override
    public <T> T nativeResponse() {
      return null;
    }

    @Override
    public <T> T nativeResponse(Class<T> responseClass) {
      return null;
    }

    @Override
    public String toString() {
      return "Application has not been started";
    }
  }
}
