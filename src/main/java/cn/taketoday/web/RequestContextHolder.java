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
package cn.taketoday.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

import cn.taketoday.core.MultiValueMap;
import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.multipart.MultipartFile;

/**
 * Holder class to expose the web request in the form of a thread-bound
 * {@link RequestContext} object.
 * <p>
 * user can replace RequestThreadLocal use {@link #replaceContextHolder(RequestThreadLocal)}
 * to hold RequestContext
 * </p>
 *
 * @author TODAY 2019-03-23 10:29
 * @see #replaceContextHolder(RequestThreadLocal)
 * @since 2.3.7
 */
public abstract class RequestContextHolder {
  public static final RequestContext ApplicationNotStartedContext = new ApplicationNotStartedContext();
  private static RequestThreadLocal contextHolder = new DefaultRequestThreadLocal();

  public static void remove() {
    contextHolder.remove();
  }

  public static void set(RequestContext requestContext) {
    contextHolder.set(requestContext);
  }

  public static RequestContext currentContext() {
    final RequestContext ret = get();
    return ret == null ? ApplicationNotStartedContext : ret;
  }

  @Nullable
  public static RequestContext get() {
    return contextHolder.get();
  }

  public static RequestContext getRequired() {
    RequestContext context = contextHolder.get();
    if (context == null) {
      throw new IllegalStateException("No RequestContext set");
    }
    return context;
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
   * @param contextHolder new {@link RequestThreadLocal} object
   * @since 3.0
   */
  public static void replaceContextHolder(RequestThreadLocal contextHolder) {
    Assert.notNull(contextHolder, "contextHolder must not be null");
    RequestContextHolder.contextHolder = contextHolder;
  }

  /**
   * @since 3.0
   */
  public static RequestThreadLocal getRequestThreadLocal() {
    return RequestContextHolder.contextHolder;
  }

  @SuppressWarnings("serial")
  static class ApplicationNotStartedContext extends RequestContext implements Serializable {

    ApplicationNotStartedContext() { super(null); }

    @Override
    public String getScheme() {
      return null;
    }

    @Override
    protected String doGetRequestPath() {
      return null;
    }

    @Override
    public String getRequestURL() {
      return null;
    }

    @Override
    protected String doGetQueryString() {
      return null;
    }

    @Override
    protected HttpCookie[] doGetCookies() {
      return new HttpCookie[0];
    }

    @Override
    protected String doGetMethod() {
      return null;
    }

    @Override
    public String remoteAddress() {
      return null;
    }

    @Override
    public long getContentLength() {
      return 0;
    }

    @Override
    protected InputStream doGetInputStream() throws IOException {
      return null;
    }

    @Override
    protected MultiValueMap<String, MultipartFile> parseMultipartFiles() {
      return null;
    }

    @Override
    public String getContentType() {
      return null;
    }

    @Override
    protected HttpHeaders createRequestHeaders() {
      return null;
    }

    @Override
    public boolean isCommitted() {
      return false;
    }

    @Override
    public void sendRedirect(String location) { }

    @Override
    public void setStatus(int sc) { }

    @Override
    public void setStatus(int status, String message) { }

    @Override
    public int getStatus() {
      return 0;
    }

    @Override
    public void sendError(int sc) throws IOException { }

    @Override
    public void sendError(int sc, String msg) throws IOException { }

    @Override
    protected OutputStream doGetOutputStream() throws IOException {
      return null;
    }

    @Override
    public <T> T nativeRequest() {
      return null;
    }

    @Override
    public <T> T unwrapRequest(Class<T> requestClass) {
      return null;
    }

    @Override
    public <T> T nativeResponse() {
      return null;
    }

    @Override
    public <T> T unwrapResponse(Class<T> responseClass) {
      return null;
    }

    @Override
    public String toString() {
      return "Application has not been started";
    }

    @Override
    public String[] getAttributeNames() {
      return new String[0];
    }
  }
}
