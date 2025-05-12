/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URI;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import infra.context.ApplicationContext;
import infra.core.AttributeAccessor;
import infra.http.HttpCookie;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpStatusCode;
import infra.http.MediaType;
import infra.http.server.RequestPath;
import infra.http.server.ServerHttpResponse;
import infra.lang.Nullable;
import infra.util.MultiValueMap;
import infra.web.async.AsyncWebRequest;
import infra.web.async.WebAsyncManager;
import infra.web.multipart.MultipartRequest;

/**
 * Decorating RequestContext
 * <p>
 * Provides a convenient implementation of the RequestContext
 * that can be subclassed by developers wishing to adapt the request to web.
 * This class implements the Wrapper or Decorator pattern.
 * Methods default to calling through to the wrapped request object.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/6/13 23:38
 */
public abstract class DecoratingRequestContext extends RequestContext {

  protected DecoratingRequestContext() {
    super(null, null);
  }

  @Override
  public long getRequestTimeMillis() {
    return getDelegate().getRequestTimeMillis();
  }

  @Override
  public ApplicationContext getApplicationContext() {
    return getDelegate().getApplicationContext();
  }

  @Override
  public Reader getReader(Charset encoding) throws IOException {
    return getDelegate().getReader(encoding);
  }

  @Override
  public ReadableByteChannel readableChannel() throws IOException {
    return getDelegate().readableChannel();
  }

  @Override
  public WritableByteChannel writableChannel() throws IOException {
    return getDelegate().writableChannel();
  }

  @Override
  public String getScheme() {
    return getDelegate().getScheme();
  }

  @Override
  public int getServerPort() {
    return getDelegate().getServerPort();
  }

  @Override
  public String getServerName() {
    return getDelegate().getServerName();
  }

  @Override
  public URI getURI() {
    return getDelegate().getURI();
  }

  @Override
  public String getRequestURI() {
    return getDelegate().getRequestURI();
  }

  @Override
  public boolean isPreFlightRequest() {
    return getDelegate().isPreFlightRequest();
  }

  @Override
  public boolean isCorsRequest() {
    return getDelegate().isCorsRequest();
  }

  @Override
  public RequestPath getRequestPath() {
    return getDelegate().getRequestPath();
  }

  @Override
  protected String readRequestURI() {
    return getDelegate().readRequestURI();
  }

  @Override
  public String getRequestURL() {
    return getDelegate().getRequestURL();
  }

  @Override
  public String getQueryString() {
    return getDelegate().getQueryString();
  }

  @Override
  protected String readQueryString() {
    return getDelegate().readQueryString();
  }

  @Override
  public HttpCookie[] getCookies() {
    return getDelegate().getCookies();
  }

  @Override
  protected HttpCookie[] readCookies() {
    return getDelegate().readCookies();
  }

  @Override
  @Nullable
  public HttpCookie getCookie(String name) {
    return getDelegate().getCookie(name);
  }

  @Override
  public void addCookie(HttpCookie cookie) {
    getDelegate().addCookie(cookie);
  }

  @Override
  public void addCookie(String name, @Nullable String value) {
    getDelegate().addCookie(name, value);
  }

  @Nullable
  @Override
  public List<HttpCookie> removeCookie(String name) {
    return getDelegate().removeCookie(name);
  }

  @Override
  public boolean hasResponseCookie() {
    return getDelegate().hasResponseCookie();
  }

  @Override
  public ArrayList<HttpCookie> responseCookies() {
    return getDelegate().responseCookies();
  }

  @Override
  public MultiValueMap<String, String> getParameters() {
    return getDelegate().getParameters();
  }

  @Override
  protected MultiValueMap<String, String> readParameters() {
    return getDelegate().readParameters();
  }

  @Override
  public Set<String> getParameterNames() {
    return getDelegate().getParameterNames();
  }

  @Override
  @Nullable
  public String[] getParameters(String name) {
    return getDelegate().getParameters(name);
  }

  @Override
  @Nullable
  public String getParameter(String name) {
    return getDelegate().getParameter(name);
  }

  @Override
  protected String readMethod() {
    return getDelegate().readMethod();
  }

  @Override
  public String getRemoteAddress() {
    return getDelegate().getRemoteAddress();
  }

  @Override
  public long getContentLength() {
    return getDelegate().getContentLength();
  }

  @Override
  public InputStream getBody() throws IOException {
    return getDelegate().getBody();
  }

  @Override
  public HttpHeaders getHeaders() {
    return getDelegate().getHeaders();
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return getDelegate().getInputStream();
  }

  @Override
  protected InputStream createInputStream() throws IOException {
    return getDelegate().createInputStream();
  }

  @Override
  public BufferedReader getReader() throws IOException {
    return getDelegate().getReader();
  }

  @Override
  protected BufferedReader createReader() throws IOException {
    return getDelegate().createReader();
  }

  @Override
  public boolean isMultipart() {
    return getDelegate().isMultipart();
  }

  @Nullable
  @Override
  public String getContentType() {
    return getDelegate().getContentType();
  }

  @Override
  public HttpHeaders requestHeaders() {
    return getDelegate().requestHeaders();
  }

  @Override
  public HttpHeaders createRequestHeaders() {
    return getDelegate().createRequestHeaders();
  }

  @Override
  public Locale getLocale() {
    return getDelegate().getLocale();
  }

  @Override
  protected Locale readLocale() {
    return getDelegate().readLocale();
  }

  @Override
  public boolean checkNotModified(long lastModifiedTimestamp) {
    return getDelegate().checkNotModified(lastModifiedTimestamp);
  }

  @Override
  public boolean checkNotModified(String etag) {
    return getDelegate().checkNotModified(etag);
  }

  @Override
  public boolean checkNotModified(@Nullable String etag, long lastModifiedTimestamp) {
    return getDelegate().checkNotModified(etag, lastModifiedTimestamp);
  }

  @Override
  public boolean isNotModified() {
    return getDelegate().isNotModified();
  }

  @Override
  public void setContentLength(long length) {
    getDelegate().setContentLength(length);
  }

  @Override
  public boolean isCommitted() {
    return getDelegate().isCommitted();
  }

  @Override
  public void reset() {
    getDelegate().reset();
  }

  @Override
  public void sendRedirect(String location) throws IOException {
    getDelegate().sendRedirect(location);
  }

  @Override
  public void setStatus(int sc) {
    getDelegate().setStatus(sc);
  }

  @Override
  public void setStatus(HttpStatusCode status) {
    getDelegate().setStatus(status);
  }

  @Override
  public int getStatus() {
    return getDelegate().getStatus();
  }

  @Override
  public void sendError(HttpStatusCode code) throws IOException {
    getDelegate().sendError(code);
  }

  @Override
  public void sendError(HttpStatusCode code, @Nullable String msg) throws IOException {
    getDelegate().sendError(code, msg);
  }

  @Override
  public void sendError(int sc) throws IOException {
    getDelegate().sendError(sc);
  }

  @Override
  public void sendError(int sc, @Nullable String msg) throws IOException {
    getDelegate().sendError(sc, msg);
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    return getDelegate().getOutputStream();
  }

  @Override
  protected OutputStream createOutputStream() throws IOException {
    return getDelegate().createOutputStream();
  }

  @Override
  public PrintWriter getWriter() throws IOException {
    return getDelegate().getWriter();
  }

  @Override
  protected PrintWriter createWriter() throws IOException {
    return getDelegate().createWriter();
  }

  @Override
  public void setContentType(@Nullable String contentType) {
    getDelegate().setContentType(contentType);
  }

  @Override
  public void setContentType(@Nullable MediaType contentType) {
    getDelegate().setContentType(contentType);
  }

  @Nullable
  @Override
  public String getResponseContentType() {
    return getDelegate().getResponseContentType();
  }

  @Override
  public void setHeader(String name, @Nullable String value) {
    getDelegate().setHeader(name, value);
  }

  @Override
  public void addHeader(String name, @Nullable String value) {
    getDelegate().addHeader(name, value);
  }

  @Override
  public void removeHeader(String name) {
    getDelegate().removeHeader(name);
  }

  @Override
  public boolean containsResponseHeader(String name) {
    return getDelegate().containsResponseHeader(name);
  }

  @Override
  public HttpHeaders responseHeaders() {
    return getDelegate().responseHeaders();
  }

  @Override
  public void mergeToResponse(@Nullable HttpHeaders headers) {
    getDelegate().mergeToResponse(headers);
  }

  @Override
  public HttpHeaders createResponseHeaders() {
    return getDelegate().createResponseHeaders();
  }

  @Override
  public ServerHttpResponse asHttpOutputMessage() {
    return getDelegate().asHttpOutputMessage();
  }

  @Override
  public <T> T nativeRequest() {
    return getDelegate().nativeRequest();
  }

  @Nullable
  @Override
  public HandlerMatchingMetadata getMatchingMetadata() {
    return getDelegate().getMatchingMetadata();
  }

  @Override
  public void setMatchingMetadata(@Nullable HandlerMatchingMetadata handlerMatchingMetadata) {
    getDelegate().setMatchingMetadata(handlerMatchingMetadata);
  }

  @Override
  public boolean hasMatchingMetadata() {
    return getDelegate().hasMatchingMetadata();
  }

  @Nullable
  @Override
  public Object getAttribute(String name) {
    return getDelegate().getAttribute(name);
  }

  @Override
  public void setAttribute(String name, @Nullable Object value) {
    getDelegate().setAttribute(name, value);
  }

  @Nullable
  @Override
  public Object removeAttribute(String name) {
    return getDelegate().removeAttribute(name);
  }

  @Override
  public void clearAttributes() {
    getDelegate().clearAttributes();
  }

  @Override
  public String[] getAttributeNames() {
    return getDelegate().getAttributeNames();
  }

  @Override
  protected void writeHeaders() {
    getDelegate().writeHeaders();
  }

  @Override
  public void flush() throws IOException {
    getDelegate().flush();
  }

  @Override
  public void requestCompleted() {
    getDelegate().requestCompleted();
  }

  @Override
  public void requestCompleted(@Nullable Throwable notHandled) {
    getDelegate().requestCompleted(notHandled);
  }

  @Override
  protected MultipartRequest createMultipartRequest() {
    return getDelegate().createMultipartRequest();
  }

  @Override
  protected AsyncWebRequest createAsyncWebRequest() {
    return getDelegate().createAsyncWebRequest();
  }

  @Override
  public AsyncWebRequest getAsyncWebRequest() {
    return getDelegate().getAsyncWebRequest();
  }

  @Override
  public boolean isConcurrentHandlingStarted() {
    return getDelegate().isConcurrentHandlingStarted();
  }

  @Override
  public MultipartRequest getMultipartRequest() {
    return getDelegate().getMultipartRequest();
  }

  @Override
  public void setBinding(@Nullable BindingContext bindingContext) {
    getDelegate().setBinding(bindingContext);
  }

  @Nullable
  @Override
  public BindingContext getBinding() {
    return getDelegate().getBinding();
  }

  @Override
  public BindingContext binding() {
    return getDelegate().binding();
  }

  @Override
  public boolean hasBinding() {
    return getDelegate().hasBinding();
  }

  @Nullable
  @Override
  public RedirectModel getInputRedirectModel() {
    return getDelegate().getInputRedirectModel();
  }

  @Nullable
  @Override
  public RedirectModel getInputRedirectModel(@Nullable RedirectModelManager manager) {
    return getDelegate().getInputRedirectModel(manager);
  }

  // AttributeAccessorSupport

  @Override
  public <T> T computeAttribute(String name, Function<String, T> computeFunction) {
    return getDelegate().computeAttribute(name, computeFunction);
  }

  @Override
  public boolean hasAttribute(String name) {
    return getDelegate().hasAttribute(name);
  }

  @Override
  public Iterable<String> attributeNames() {
    return getDelegate().attributeNames();
  }

  @Override
  public void copyFrom(AttributeAccessor source) {
    getDelegate().copyFrom(source);
  }

  @Override
  public boolean hasAttributes() {
    return getDelegate().hasAttributes();
  }

  @Override
  public Map<String, Object> getAttributes() {
    return getDelegate().getAttributes();
  }

  @Override
  public HttpMethod getMethod() {
    return getDelegate().getMethod();
  }

  @Override
  public String getMethodValue() {
    return getDelegate().getMethodValue();
  }

  @Override
  public HandlerMatchingMetadata matchingMetadata() {
    return getDelegate().matchingMetadata();
  }

  @Override
  public WebAsyncManager getAsyncManager() {
    return getDelegate().getAsyncManager();
  }

  @Override
  public void requestCompletedInternal(@Nullable Throwable notHandled) {
    getDelegate().requestCompletedInternal(notHandled);
  }

  @Override
  protected RequestPath readRequestPath() {
    return getDelegate().readRequestPath();
  }

  public abstract RequestContext getDelegate();

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof DecoratingRequestContext that))
      return false;
    return Objects.equals(getDelegate(), that.getDelegate());
  }

  @Override
  public int hashCode() {
    return System.identityHashCode(this);
  }

  @Override
  public String toString() {
    return "Wrapper for " + getDelegate();
  }

}
