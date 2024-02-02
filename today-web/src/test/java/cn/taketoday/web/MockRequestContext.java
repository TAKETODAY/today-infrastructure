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

package cn.taketoday.web;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.context.async.AsyncWebRequest;
import cn.taketoday.web.multipart.MultipartRequest;

/**
 * @author TODAY 2021/3/10 16:35
 */
@SuppressWarnings("serial")
public class MockRequestContext extends RequestContext {
  private final long requestTimeMillis = System.currentTimeMillis();

  private String method = "GET";
  private String scheme = "http";
  private String serverName = "localhost";
  private String requestURI = "/";
  private String queryString = "";

  @Nullable
  private String requestURL;

  private int serverPort = 8080;
  private String remoteAddress;

  private long contentLength = 0;

  @Nullable
  private MediaType contentType;

  @Nullable
  private HttpHeaders requestHeaders;

  private boolean committed;

  private String redirectLocation;

  protected HttpStatus status = HttpStatus.OK;

  private String errorMessage;

  public MockRequestContext() {
    super(null, null);
  }

  public MockRequestContext(ApplicationContext context) {
    super(context, null);
  }

  @Override
  public long getRequestTimeMillis() {
    return requestTimeMillis;
  }

  @Override
  public String getScheme() {
    return scheme;
  }

  public void setScheme(String scheme) {
    this.scheme = scheme;
  }

  @Override
  public String getServerName() {
    return serverName;
  }

  public void setServerName(String serverName) {
    this.serverName = serverName;
  }

  public void setServerPort(int serverPort) {
    this.serverPort = serverPort;
  }

  @Override
  public int getServerPort() {
    return serverPort;
  }

  @Override
  protected String doGetRequestURI() {
    return requestURI;
  }

  public void setRequestURI(String requestURI) {
    this.requestURI = requestURI;
  }

  public void setRequestURL(@Nullable String requestURL) {
    this.requestURL = requestURL;
  }

  @Override
  public String getRequestURL() {
    if (requestURL == null) {
      return super.getRequestURL();
    }
    return requestURL;
  }

  @Override
  protected String doGetQueryString() {
    return queryString;
  }

  public void setQueryString(String queryString) {
    this.queryString = queryString;
  }

  @Override
  protected HttpCookie[] doGetCookies() {
    return requestCookies.toArray(new HttpCookie[0]);
  }

  private final ArrayList<HttpCookie> requestCookies = new ArrayList<>();

  public List<HttpCookie> getRequestCookies() {
    return requestCookies;
  }

  public void addRequestCookies(List<HttpCookie> requestCookies) {
    this.requestCookies.addAll(requestCookies);
  }

  public void addRequestCookies(HttpCookie... requestCookies) {
    CollectionUtils.addAll(this.requestCookies, requestCookies);
  }

  public void setRequestCookies(List<HttpCookie> requestCookies) {
    this.requestCookies.clear();
    this.requestCookies.addAll(requestCookies);
  }

  @Override
  public Map<String, String[]> doGetParameters() {
    if (parameters == null) {
      return Collections.emptyMap();
    }
    return parameters;
  }

  public void setParameters(Map<String, String[]> parameters) {
    this.parameters = parameters;
  }

  public void setParameter(String name, String parameter) {
    if (parameters == null) {
      setParameters(new LinkedHashMap<>());
    }
    parameters.put(name, new String[] { parameter });
  }

  public void setParameter(String name, String[] parameter) {
    if (parameters == null) {
      setParameters(new LinkedHashMap<>());
    }
    parameters.put(name, parameter);
  }

  @Override
  protected String doGetMethod() {
    return method;
  }

  public void setMethod(@Nullable String method) {
    this.method = method;
  }

  @Override
  public String getRemoteAddress() {
    return remoteAddress;
  }

  public void setRemoteAddress(String remoteAddress) {
    this.remoteAddress = remoteAddress;
  }

  @Override
  public long getContentLength() {
    return contentLength;
  }

  private ByteArrayInputStream requestBody;

  @Override
  protected InputStream doGetInputStream() {
    if (requestBody == null) {
      requestBody = new ByteArrayInputStream(new byte[0]);
      this.contentLength = 0;
    }
    return requestBody;
  }

  public void setRequestBody(ByteArrayInputStream requestBody) {
    this.requestBody = requestBody;
    this.contentLength = requestBody.available();
  }

  public void setRequestBody(String requestBody) {
    this.requestBody = new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8));
    this.contentLength = this.requestBody.available();
  }

  public void setMultipartRequest(MultipartRequest multipartRequest) {
    this.multipartRequest = multipartRequest;
  }

  public void setAsyncWebRequest(AsyncWebRequest asyncWebRequest) {
    this.asyncWebRequest = asyncWebRequest;
  }

  @Override
  protected MultipartRequest createMultipartRequest() {
    throw new UnsupportedOperationException();
  }

  @Override
  protected AsyncWebRequest createAsyncWebRequest() {
    throw new UnsupportedOperationException();
  }

  public void setRequestContentType(@Nullable String contentType) {
    Optional.ofNullable(contentType)
            .map(MediaType::valueOf)
            .ifPresent(this::setRequestContentType);
  }

  public void setRequestContentType(@Nullable MediaType contentType) {
    this.contentType = contentType;
  }

  @Override
  public String getContentType() {
    if (contentType == null) {
      MediaType contentType = responseHeaders().getContentType();
      if (contentType == null) {
        return null;
      }
      return contentType.toString();
    }
    return contentType.toString();
  }

  @Override
  protected HttpHeaders createRequestHeaders() {
    if (requestHeaders == null) {
      requestHeaders = HttpHeaders.forWritable();
    }
    return requestHeaders;
  }

  public void setRequestHeaders(@Nullable HttpHeaders requestHeaders) {
    this.requestHeaders = requestHeaders;
  }

  @Nullable
  public HttpHeaders getRequestHeaders() {
    return requestHeaders;
  }

  public void setCommitted(boolean committed) {
    this.committed = committed;
  }

  @Override
  public boolean isCommitted() {
    return committed;
  }

  public void setRedirectLocation(String redirectLocation) {
    this.redirectLocation = redirectLocation;
  }

  @Override
  public void sendRedirect(String redirectLocation) throws IOException {
    this.redirectLocation = redirectLocation;
  }

  public String getRedirectLocation() {
    return redirectLocation;
  }

  @Override
  public void setStatus(int sc) {
    this.status = HttpStatus.valueOf(sc);
  }

  @Override
  public int getStatus() {
    return status.value();
  }

  @Override
  public void sendError(int sc) throws IOException {
    setStatus(sc);
  }

  @Override
  public void sendError(int sc, String msg) throws IOException {
    setStatus(sc);
    this.errorMessage = msg;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public boolean hasError() {
    return errorMessage != null;
  }

  @Nullable
  private ByteArrayOutputStream responseBody;

  @Override
  protected ByteArrayOutputStream doGetOutputStream() {
    if (responseBody == null) {
      responseBody = new ByteArrayOutputStream();
    }
    return responseBody;
  }

  public void setResponseBody(@Nullable ByteArrayOutputStream outputStream) {
    this.responseBody = outputStream;
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
  public String toString() {
    return "Mock Request context";
  }

}
