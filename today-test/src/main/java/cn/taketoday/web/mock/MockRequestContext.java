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

package cn.taketoday.web.mock;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.http.DefaultHttpHeaders;
import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.InvalidMediaTypeException;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ResponseCookie;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.api.http.Cookie;
import cn.taketoday.mock.api.http.HttpMockRequest;
import cn.taketoday.mock.api.http.HttpMockResponse;
import cn.taketoday.mock.web.HttpMockRequestImpl;
import cn.taketoday.mock.web.MockHttpResponseImpl;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.CompositeIterator;
import cn.taketoday.util.ExceptionUtils;
import cn.taketoday.util.LinkedCaseInsensitiveMap;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.DispatcherHandler;
import cn.taketoday.web.MockIndicator;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.async.AsyncWebRequest;
import cn.taketoday.web.multipart.MultipartRequest;

/**
 * Servlet environment implementation
 *
 * @author TODAY 2019-07-07 22:27
 * @since 2.3.7
 */
public class MockRequestContext extends RequestContext implements MockIndicator {

  public final HttpMockRequest request;

  public final HttpMockResponse response;

  private final long requestTimeMillis = System.currentTimeMillis();

  private boolean bodyUsed = false;
  private boolean headersWritten = false;

  public MockRequestContext() {
    this(null);
  }

  public MockRequestContext(ApplicationContext context) {
    this(context, new HttpMockRequestImpl(), new MockHttpResponseImpl());
  }

  public MockRequestContext(HttpMockRequest request, HttpMockResponse response) {
    this(null, request, response);
  }

  public MockRequestContext(ApplicationContext context, HttpMockRequest request) {
    this(context, request, new MockHttpResponseImpl(), null);
  }

  public MockRequestContext(ApplicationContext context,
          HttpMockRequest request, HttpMockResponse response) {
    this(context, request, response, null);
  }

  public MockRequestContext(ApplicationContext context, HttpMockRequest request,
          HttpMockResponse response, DispatcherHandler dispatcherHandler) {
    super(context, dispatcherHandler);
    this.request = request;
    this.response = response;
  }

  @Override
  public HttpMockRequest getRequest() {
    return request;
  }

  @Override
  public HttpMockResponse getResponse() {
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
    LinkedHashSet<HttpCookie> requestCookies = new LinkedHashSet<>(this.requestCookies);
    Cookie[] servletCookies = request.getCookies();
    if (servletCookies != null) {
      for (Cookie servletCookie : servletCookies) {
        HttpCookie httpCookie = new HttpCookie(servletCookie.getName(), servletCookie.getValue());
        requestCookies.add(httpCookie);
      }
    }
    return requestCookies.toArray(EMPTY_COOKIES);
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
  protected MultiValueMap<String, String> doGetParameters() {
    var ret = MultiValueMap.<String, String>forSmartListAdaption(new LinkedHashMap<>());
    for (var entry : request.getParameterMap().entrySet()) {
      ret.addAll(entry.getKey(), entry.getValue());
    }
    return ret;
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
      servletCookie.setAttribute("SameSite", responseCookie.getSameSite());
    }

    response.addCookie(servletCookie);
  }

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

  public void setMultipartRequest(MultipartRequest multipartRequest) {
    this.multipartRequest = multipartRequest;
  }

  @Override
  protected MultipartRequest createMultipartRequest() {
    if (request instanceof MultipartRequest) {
      return (MultipartRequest) request;
    }
    return new MockMultipartRequest(request);
  }

  @Override
  protected AsyncWebRequest createAsyncWebRequest() {
    return new StandardMockAsyncWebRequest(this);
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

  public void setRequestHeaders(HttpHeaders requestHeaders) {
    this.requestHeaders = requestHeaders;
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

  public void setRequestURI(String requestURI) {
    this.requestURI = requestURI;
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

  public void setParameter(String key, String value) {
    getParameters().set(key, value);
  }

  public void addParameter(String key, String value) {
    getParameters().add(key, value);
  }

  public void addParameter(String key, String... values) {
    getParameters().addAll(key, values);
  }

  public void setParameter(String key, String... values) {
    removeParameter(key);
    getParameters().addAll(key, values);
  }

  public void removeParameter(String key) {
    getParameters().remove(key);
  }

  public void setParameters(MultiValueMap<String, String> parameters) {
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
