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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.HttpCookie;
import java.util.Map;

import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.MultiValueMap;
import cn.taketoday.web.http.HttpHeaders;
import cn.taketoday.web.multipart.MultipartFile;

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
  static class ApplicationNotStartedContext extends RequestContext implements Serializable {

    @Override
    protected String doGetRequestPath() {
      return null;
    }

    @Override protected String doGetQueryString() {
      return null;
    }

    @Override public String getRequestURL() {
      return null;
    }

    @Override public String getQueryString() {
      return null;
    }

    @Override protected HttpCookie[] doGetCookies() {
      return new HttpCookie[0];
    }

    @Override public void addCookie(HttpCookie cookie) {

    }

    @Override public Map<String, String[]> doGetParameters() {
      return null;
    }

    @Override public String doGetMethod() {
      return null;
    }

    @Override public String remoteAddress() {
      return null;
    }

    @Override public long getContentLength() {
      return 0;
    }

    @Override protected InputStream doGetInputStream() throws IOException {
      return null;
    }

    @Override protected MultiValueMap<String, MultipartFile> parseMultipartFiles() {
      return null;
    }

    @Override public String getContentType() {
      return null;
    }

    @Override protected HttpHeaders createRequestHeaders() {
      return null;
    }

    @Override public void setContentLength(long length) {

    }

    @Override public boolean committed() {
      return false;
    }

    @Override public void sendRedirect(String location) throws IOException {

    }

    @Override public void setStatus(int sc) {

    }

    @Override public void setStatus(int status, String message) {

    }

    @Override public int getStatus() {
      return 0;
    }

    @Override public void sendError(int sc) throws IOException {

    }

    @Override public void sendError(int sc, String msg) throws IOException {

    }

    @Override protected OutputStream doGetOutputStream() throws IOException {
      return null;
    }

    @Override public <T> T nativeRequest() {
      return null;
    }

    @Override public <T> T nativeRequest(Class<T> requestClass) {
      return null;
    }

    @Override public <T> T nativeResponse() {
      return null;
    }

    @Override public <T> T nativeResponse(Class<T> responseClass) {
      return null;
    }

    @Override
    public String toString() {
      return "Application has not been started";
    }


  }
}
