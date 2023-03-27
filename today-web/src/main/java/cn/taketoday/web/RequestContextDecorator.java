/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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
import java.io.Reader;
import java.net.URI;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.AttributeAccessor;
import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.server.PathContainer;
import cn.taketoday.http.server.RequestPath;
import cn.taketoday.http.server.ServerHttpResponse;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.web.context.async.AsyncWebRequest;
import cn.taketoday.web.multipart.MultipartRequest;

/**
 * Provides a convenient implementation of the RequestContext
 * that can be subclassed by developers wishing to adapt the request to web.
 * This class implements the Wrapper or Decorator pattern.
 * Methods default to calling through to the wrapped request object.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/5 13:53
 */
public class RequestContextDecorator extends RequestContext {
  private final RequestContext delegate;

  public RequestContextDecorator(RequestContext delegate) {
    super(delegate.getApplicationContext());
    Assert.notNull(delegate, "RequestContext delegate is required");
    this.delegate = delegate;
  }

  public RequestContext getDelegate() {
    return delegate;
  }

  // delegate

  @Override
  public long getRequestTimeMillis() {
    return delegate.getRequestTimeMillis();
  }

  @Override
  public ApplicationContext getApplicationContext() {
    return delegate.getApplicationContext();
  }

  @Override
  public Reader getReader(String encoding) throws IOException {
    return delegate.getReader(encoding);
  }

  @Override
  public ReadableByteChannel readableChannel() throws IOException {
    return delegate.readableChannel();
  }

  @Override
  public WritableByteChannel writableChannel() throws IOException {
    return delegate.writableChannel();
  }

  @Override
  public String getScheme() {
    return delegate.getScheme();
  }

  @Override
  public int getServerPort() {
    return delegate.getServerPort();
  }

  @Override
  public String getServerName() {
    return delegate.getServerName();
  }

  @Override
  public String getContextPath() {
    return delegate.getContextPath();
  }

  @Override
  public String doGetContextPath() {
    return delegate.doGetContextPath();
  }

  @Override
  public URI getURI() {
    return delegate.getURI();
  }

  @Override
  public String getRequestURI() {
    return delegate.getRequestURI();
  }

  @Override
  public PathContainer getLookupPath() {
    return delegate.getLookupPath();
  }

  @Override
  public boolean isPreFlightRequest() {
    return delegate.isPreFlightRequest();
  }

  @Override
  public boolean isCorsRequest() {
    return delegate.isCorsRequest();
  }

  @Override
  public RequestPath getRequestPath() {
    return delegate.getRequestPath();
  }

  @Override
  public String doGetRequestURI() {
    return delegate.doGetRequestURI();
  }

  @Override
  public String getRequestURL() {
    return delegate.getRequestURL();
  }

  @Override
  public String getQueryString() {
    return delegate.getQueryString();
  }

  @Override
  public String doGetQueryString() {
    return delegate.doGetQueryString();
  }

  @Override
  public HttpCookie[] getCookies() {
    return delegate.getCookies();
  }

  @Override
  public HttpCookie[] doGetCookies() {
    return delegate.doGetCookies();
  }

  @Override
  @Nullable
  public HttpCookie getCookie(String name) {
    return delegate.getCookie(name);
  }

  @Override
  public void addCookie(HttpCookie cookie) {
    delegate.addCookie(cookie);
  }

  @Override
  public void addCookie(String name, @Nullable String value) {
    delegate.addCookie(name, value);
  }

  @Override
  public List<HttpCookie> removeCookie(String name) {
    return delegate.removeCookie(name);
  }

  @Override
  public ArrayList<HttpCookie> responseCookies() {
    return delegate.responseCookies();
  }

  @Override
  public Map<String, String[]> getParameters() {
    return delegate.getParameters();
  }

  @Override
  public Map<String, String[]> doGetParameters() {
    return delegate.doGetParameters();
  }

  @Override
  public void postGetParameters(MultiValueMap<String, String> parameters) {
    delegate.postGetParameters(parameters);
  }

  @Override
  public Iterator<String> getParameterNames() {
    return delegate.getParameterNames();
  }

  @Override
  @Nullable
  public String[] getParameters(String name) {
    return delegate.getParameters(name);
  }

  @Override
  @Nullable
  public String getParameter(String name) {
    return delegate.getParameter(name);
  }

  @Override
  public String doGetMethod() {
    return delegate.doGetMethod();
  }

  @Override
  public String getRemoteAddress() {
    return delegate.getRemoteAddress();
  }

  @Override
  public long getContentLength() {
    return delegate.getContentLength();
  }

  @Override
  public InputStream getBody() throws IOException {
    return delegate.getBody();
  }

  @Override
  public HttpHeaders getHeaders() {
    return delegate.getHeaders();
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return delegate.getInputStream();
  }

  @Override
  public InputStream doGetInputStream() throws IOException {
    return delegate.doGetInputStream();
  }

  @Override
  public BufferedReader getReader() throws IOException {
    return delegate.getReader();
  }

  @Override
  public BufferedReader doGetReader() throws IOException {
    return delegate.doGetReader();
  }

  @Override
  public boolean isMultipart() {
    return delegate.isMultipart();
  }

  @Override
  public String getContentType() {
    return delegate.getContentType();
  }

  @Override
  public HttpHeaders requestHeaders() {
    return delegate.requestHeaders();
  }

  @Override
  public HttpHeaders createRequestHeaders() {
    return delegate.createRequestHeaders();
  }

  @Override
  public Locale getLocale() {
    return delegate.getLocale();
  }

  @Override
  public Locale doGetLocale() {
    return delegate.doGetLocale();
  }

  @Override
  public boolean checkNotModified(long lastModifiedTimestamp) {
    return delegate.checkNotModified(lastModifiedTimestamp);
  }

  @Override
  public boolean checkNotModified(String etag) {
    return delegate.checkNotModified(etag);
  }

  @Override
  public boolean checkNotModified(@Nullable String etag, long lastModifiedTimestamp) {
    return delegate.checkNotModified(etag, lastModifiedTimestamp);
  }

  @Override
  public boolean isNotModified() {
    return delegate.isNotModified();
  }

  @Override
  public void setContentLength(long length) {
    delegate.setContentLength(length);
  }

  @Override
  public boolean isCommitted() {
    return delegate.isCommitted();
  }

  @Override
  public void reset() {
    delegate.reset();
  }

  @Override
  public void sendRedirect(String location) throws IOException {
    delegate.sendRedirect(location);
  }

  @Override
  public void setStatus(int sc) {
    delegate.setStatus(sc);
  }

  @Override
  public void setStatus(HttpStatusCode status) {
    delegate.setStatus(status);
  }

  @Override
  public int getStatus() {
    return delegate.getStatus();
  }

  @Override
  public void sendError(HttpStatusCode code) throws IOException {
    delegate.sendError(code);
  }

  @Override
  public void sendError(HttpStatusCode code, String msg) throws IOException {
    delegate.sendError(code, msg);
  }

  @Override
  public void sendError(int sc) throws IOException {
    delegate.sendError(sc);
  }

  @Override
  public void sendError(int sc, String msg) throws IOException {
    delegate.sendError(sc, msg);
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    return delegate.getOutputStream();
  }

  @Override
  public OutputStream doGetOutputStream() throws IOException {
    return delegate.doGetOutputStream();
  }

  @Override
  public PrintWriter getWriter() throws IOException {
    return delegate.getWriter();
  }

  @Override
  public PrintWriter doGetWriter() throws IOException {
    return delegate.doGetWriter();
  }

  @Override
  public void setContentType(String contentType) {
    delegate.setContentType(contentType);
  }

  @Nullable
  @Override
  public String getResponseContentType() {
    return delegate.getResponseContentType();
  }

  @Override
  public HttpHeaders responseHeaders() {
    return delegate.responseHeaders();
  }

  @Override
  public void mergeToResponse(HttpHeaders headers) {
    delegate.mergeToResponse(headers);
  }

  @Override
  public HttpHeaders createResponseHeaders() {
    return delegate.createResponseHeaders();
  }

  @Override
  public ServerHttpResponse asHttpOutputMessage() {
    return delegate.asHttpOutputMessage();
  }

  @Override
  public <T> T nativeRequest() {
    return delegate.nativeRequest();
  }

  @Override
  @Nullable
  public <T> T unwrapRequest(Class<T> requestClass) {
    return delegate.unwrapRequest(requestClass);
  }

  @Override
  public HandlerMatchingMetadata getMatchingMetadata() {
    return delegate.getMatchingMetadata();
  }

  @Override
  public void setMatchingMetadata(HandlerMatchingMetadata handlerMatchingMetadata) {
    delegate.setMatchingMetadata(handlerMatchingMetadata);
  }

  @Override
  public boolean hasMatchingMetadata() {
    return delegate.hasMatchingMetadata();
  }

  @Override
  public Object getAttribute(String name) {
    return delegate.getAttribute(name);
  }

  @Override
  public void setAttribute(String name, Object value) {
    delegate.setAttribute(name, value);
  }

  @Override
  public Object removeAttribute(String name) {
    return delegate.removeAttribute(name);
  }

  @Override
  public void clearAttributes() {
    delegate.clearAttributes();
  }

  @Override
  public String[] getAttributeNames() {
    return delegate.getAttributeNames();
  }

  @Override
  public void writeHeaders() {
    delegate.writeHeaders();
  }

  @Override
  public void flush() throws IOException {
    delegate.flush();
  }

  @Override
  public void requestCompleted() {
    delegate.requestCompleted();
  }

  @Override
  public void requestCompleted(@Nullable Throwable notHandled) {
    delegate.requestCompleted(notHandled);
  }

  @Override
  protected MultipartRequest createMultipartRequest() {
    return delegate.createMultipartRequest();
  }

  @Override
  protected AsyncWebRequest createAsyncWebRequest() {
    return delegate.createAsyncWebRequest();
  }

  @Override
  public AsyncWebRequest getAsyncWebRequest() {
    return delegate.getAsyncWebRequest();
  }

  @Override
  public boolean isConcurrentHandlingStarted() {
    return delegate.isConcurrentHandlingStarted();
  }

  @Override
  public MultipartRequest getMultipartRequest() {
    return delegate.getMultipartRequest();
  }

  @Override
  public void setBindingContext(BindingContext bindingContext) {
    delegate.setBindingContext(bindingContext);
  }

  @Override
  public BindingContext getBindingContext() {
    return delegate.getBindingContext();
  }

  @Override
  public boolean hasBindingContext() {
    return delegate.hasBindingContext();
  }

  // AttributeAccessorSupport

  @Override
  public <T> T computeAttribute(String name, Function<String, T> computeFunction) {
    return delegate.computeAttribute(name, computeFunction);
  }

  @Override
  public boolean hasAttribute(String name) {
    return delegate.hasAttribute(name);
  }

  @Override
  public Iterator<String> attributeNames() {
    return delegate.attributeNames();
  }

  @Override
  public void copyAttributesFrom(AttributeAccessor source) {
    delegate.copyAttributesFrom(source);
  }

  @Override
  public boolean hasAttributes() {
    return delegate.hasAttributes();
  }

  @Override
  public Map<String, Object> getAttributes() {
    return delegate.getAttributes();
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }

  @NonNull
  @Override
  public HttpMethod getMethod() {
    return delegate.getMethod();
  }

  @Override
  public String getMethodValue() {
    return delegate.getMethodValue();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof RequestContextDecorator that))
      return false;
    return Objects.equals(delegate, that.delegate);
  }

  @Override
  public String toString() {
    return "Wrapper for " + delegate;
  }

}
