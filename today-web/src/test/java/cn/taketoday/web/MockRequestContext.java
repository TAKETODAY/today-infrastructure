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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.server.ServerHttpResponse;
import cn.taketoday.web.multipart.MultipartRequest;

/**
 * @author TODAY 2021/3/10 16:35
 */
public class MockRequestContext extends RequestContext {

  public MockRequestContext() {
    super(null);
  }

  public MockRequestContext(WebApplicationContext context) { super(context); }

  @Override
  public String getScheme() {
    return null;
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
  protected String doGetRequestURI() {
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
    return requestCookies.toArray(new HttpCookie[0]);
  }

  final List<HttpCookie> requestCookies = new ArrayList<>();

  public List<HttpCookie> getRequestCookies() {
    return requestCookies;
  }

  @Override
  public Map<String, String[]> doGetParameters() {
    return null;
  }

  @Override
  protected String doGetMethod() {
    return null;
  }

  @Override
  public String getRemoteAddress() {
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

  public void setMultipartRequest(MultipartRequest multipartRequest) {
    this.multipartRequest = multipartRequest;
  }

  @Override
  protected MultipartRequest createMultipartRequest() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getContentType() {
    MediaType contentType = responseHeaders().getContentType();
    if (contentType == null) {
      return null;
    }
    return contentType.toString();
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
  public void sendRedirect(String location) throws IOException {

  }

  @Override
  public void setStatus(int sc) {
    this.status = HttpStatus.valueOf(sc);
  }

  @Override
  public void setStatus(int status, String message) {
    this.status = HttpStatus.valueOf(status);
  }

  protected HttpStatus status = HttpStatus.OK;

  @Override
  public int getStatus() {
    return status.value();
  }

  @Override
  public void sendError(int sc) throws IOException {
  }

  @Override
  public void sendError(int sc, String msg) throws IOException {
  }

  @Override
  protected OutputStream doGetOutputStream() throws IOException {
    return null;
  }

  @Override
  public ServerHttpResponse getServerHttpResponse() {
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
    return "Mock Request context";
  }

  //

  public void setMethod(String method) {
    this.method = method;
  }

}
