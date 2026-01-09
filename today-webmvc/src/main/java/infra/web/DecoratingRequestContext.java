/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.web;

import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
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
import infra.http.ResponseCookie;
import infra.http.server.RequestPath;
import infra.http.server.ServerHttpResponse;
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

  @SuppressWarnings("NullAway")
  protected DecoratingRequestContext() {
    super(null, null);
  }

  @Override
  public long getRequestTimeMillis() {
    return delegate().getRequestTimeMillis();
  }

  @Override
  public ApplicationContext getApplicationContext() {
    return delegate().getApplicationContext();
  }

  @Override
  public Reader getReader(Charset encoding) throws IOException {
    return delegate().getReader(encoding);
  }

  @Override
  public ReadableByteChannel readableChannel() throws IOException {
    return delegate().readableChannel();
  }

  @Override
  public WritableByteChannel writableChannel() throws IOException {
    return delegate().writableChannel();
  }

  @Override
  public String getScheme() {
    return delegate().getScheme();
  }

  @Override
  public int getServerPort() {
    return delegate().getServerPort();
  }

  @Override
  public String getServerName() {
    return delegate().getServerName();
  }

  @Override
  public URI getURI() {
    return delegate().getURI();
  }

  @Override
  public String getRequestURI() {
    return delegate().getRequestURI();
  }

  @Override
  public boolean isPreFlightRequest() {
    return delegate().isPreFlightRequest();
  }

  @Override
  public boolean isCorsRequest() {
    return delegate().isCorsRequest();
  }

  @Override
  public RequestPath getRequestPath() {
    return delegate().getRequestPath();
  }

  @Override
  protected String readRequestURI() {
    return delegate().readRequestURI();
  }

  @Override
  public String getRequestURL() {
    return delegate().getRequestURL();
  }

  @Override
  public String getQueryString() {
    return delegate().getQueryString();
  }

  @Override
  protected String readQueryString() {
    return delegate().readQueryString();
  }

  @Override
  public HttpCookie[] getCookies() {
    return delegate().getCookies();
  }

  @Override
  protected HttpCookie[] readCookies() {
    return delegate().readCookies();
  }

  @Override
  @Nullable
  public HttpCookie getCookie(String name) {
    return delegate().getCookie(name);
  }

  @Override
  public void addCookie(ResponseCookie cookie) {
    delegate().addCookie(cookie);
  }

  @Override
  public void addCookie(String name, @Nullable String value) {
    delegate().addCookie(name, value);
  }

  @Nullable
  @Override
  public List<ResponseCookie> removeCookie(String name) {
    return delegate().removeCookie(name);
  }

  @Override
  public boolean hasResponseCookie() {
    return delegate().hasResponseCookie();
  }

  @Override
  public ArrayList<ResponseCookie> responseCookies() {
    return delegate().responseCookies();
  }

  @Override
  public MultiValueMap<String, String> getParameters() {
    return delegate().getParameters();
  }

  @Override
  protected MultiValueMap<String, String> readParameters() {
    return delegate().readParameters();
  }

  @Override
  public Set<String> getParameterNames() {
    return delegate().getParameterNames();
  }

  @Override
  public String @Nullable [] getParameters(String name) {
    return delegate().getParameters(name);
  }

  @Override
  @Nullable
  public String getParameter(String name) {
    return delegate().getParameter(name);
  }

  @Override
  protected String readMethod() {
    return delegate().readMethod();
  }

  @Override
  public String getRemoteAddress() {
    return delegate().getRemoteAddress();
  }

  @Override
  public int getRemotePort() {
    return delegate().getRemotePort();
  }

  @Override
  public SocketAddress localAddress() {
    return delegate().localAddress();
  }

  @Override
  public InetSocketAddress remoteAddress() {
    return delegate().remoteAddress();
  }

  @Override
  public long getContentLength() {
    return delegate().getContentLength();
  }

  @Override
  public InputStream getBody() throws IOException {
    return delegate().getBody();
  }

  @Override
  public HttpHeaders getHeaders() {
    return delegate().getHeaders();
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return delegate().getInputStream();
  }

  @Override
  protected InputStream createInputStream() throws IOException {
    return delegate().createInputStream();
  }

  @Override
  public BufferedReader getReader() throws IOException {
    return delegate().getReader();
  }

  @Override
  protected BufferedReader createReader() throws IOException {
    return delegate().createReader();
  }

  @Override
  public boolean isMultipart() {
    return delegate().isMultipart();
  }

  @Override
  public @Nullable String getContentTypeAsString() {
    return delegate().getContentTypeAsString();
  }

  @Override
  public @Nullable MediaType getContentType() {
    return delegate().getContentType();
  }

  @Override
  public HttpHeaders requestHeaders() {
    return delegate().requestHeaders();
  }

  @Override
  public HttpHeaders createRequestHeaders() {
    return delegate().createRequestHeaders();
  }

  @Override
  public @Nullable String getHeader(String name) {
    return delegate().getHeader(name);
  }

  @Override
  public List<String> getHeaders(String name) {
    return delegate().getHeaders(name);
  }

  @Override
  public Collection<String> getHeaderNames() {
    return delegate().getHeaderNames();
  }

  @Override
  public Locale getLocale() {
    return delegate().getLocale();
  }

  @Override
  protected Locale readLocale() {
    return delegate().readLocale();
  }

  @Override
  public boolean checkNotModified(long lastModifiedTimestamp) {
    return delegate().checkNotModified(lastModifiedTimestamp);
  }

  @Override
  public boolean checkNotModified(String etag) {
    return delegate().checkNotModified(etag);
  }

  @Override
  public boolean checkNotModified(@Nullable String etag, long lastModifiedTimestamp) {
    return delegate().checkNotModified(etag, lastModifiedTimestamp);
  }

  @Override
  public boolean isNotModified() {
    return delegate().isNotModified();
  }

  @Override
  public void setContentLength(long length) {
    delegate().setContentLength(length);
  }

  @Override
  public boolean isCommitted() {
    return delegate().isCommitted();
  }

  @Override
  public void reset() {
    delegate().reset();
  }

  @Override
  public void sendRedirect(String location) throws IOException {
    delegate().sendRedirect(location);
  }

  @Override
  public void setStatus(int sc) {
    delegate().setStatus(sc);
  }

  @Override
  public void setStatus(HttpStatusCode status) {
    delegate().setStatus(status);
  }

  @Override
  public int getStatus() {
    return delegate().getStatus();
  }

  @Override
  public void sendError(HttpStatusCode code) throws IOException {
    delegate().sendError(code);
  }

  @Override
  public void sendError(HttpStatusCode code, @Nullable String msg) throws IOException {
    delegate().sendError(code, msg);
  }

  @Override
  public void sendError(int sc) throws IOException {
    delegate().sendError(sc);
  }

  @Override
  public void sendError(int sc, @Nullable String msg) throws IOException {
    delegate().sendError(sc, msg);
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    return delegate().getOutputStream();
  }

  @Override
  protected OutputStream createOutputStream() throws IOException {
    return delegate().createOutputStream();
  }

  @Override
  public PrintWriter getWriter() throws IOException {
    return delegate().getWriter();
  }

  @Override
  protected PrintWriter createWriter() throws IOException {
    return delegate().createWriter();
  }

  @Override
  public void setContentType(@Nullable String contentType) {
    delegate().setContentType(contentType);
  }

  @Override
  public void setContentType(@Nullable MediaType contentType) {
    delegate().setContentType(contentType);
  }

  @Nullable
  @Override
  public String getResponseContentType() {
    return delegate().getResponseContentType();
  }

  @Override
  public void setHeader(String name, @Nullable String value) {
    delegate().setHeader(name, value);
  }

  @Override
  public void addHeader(String name, @Nullable String value) {
    delegate().addHeader(name, value);
  }

  @Override
  public void removeHeader(String name) {
    delegate().removeHeader(name);
  }

  @Override
  public boolean containsResponseHeader(String name) {
    return delegate().containsResponseHeader(name);
  }

  @Override
  public HttpHeaders responseHeaders() {
    return delegate().responseHeaders();
  }

  @Override
  public void addHeaders(@Nullable HttpHeaders headers) {
    delegate().addHeaders(headers);
  }

  @Override
  public HttpHeaders createResponseHeaders() {
    return delegate().createResponseHeaders();
  }

  @Override
  public ServerHttpResponse asHttpOutputMessage() {
    return delegate().asHttpOutputMessage();
  }

  @Override
  public <T> T nativeRequest() {
    return delegate().nativeRequest();
  }

  @Nullable
  @Override
  public HandlerMatchingMetadata getMatchingMetadata() {
    return delegate().getMatchingMetadata();
  }

  @Override
  public void setMatchingMetadata(@Nullable HandlerMatchingMetadata handlerMatchingMetadata) {
    delegate().setMatchingMetadata(handlerMatchingMetadata);
  }

  @Override
  public boolean hasMatchingMetadata() {
    return delegate().hasMatchingMetadata();
  }

  @Nullable
  @Override
  public Object getAttribute(String name) {
    return delegate().getAttribute(name);
  }

  @Override
  public void setAttribute(String name, @Nullable Object value) {
    delegate().setAttribute(name, value);
  }

  @Nullable
  @Override
  public Object removeAttribute(String name) {
    return delegate().removeAttribute(name);
  }

  @Override
  public void clearAttributes() {
    delegate().clearAttributes();
  }

  @Override
  public String[] getAttributeNames() {
    return delegate().getAttributeNames();
  }

  @Override
  protected void writeHeaders() {
    delegate().writeHeaders();
  }

  @Override
  public void flush() throws IOException {
    delegate().flush();
  }

  @Override
  public void requestCompleted() {
    delegate().requestCompleted();
  }

  @Override
  public void requestCompleted(@Nullable Throwable notHandled) {
    delegate().requestCompleted(notHandled);
  }

  @Override
  protected MultipartRequest createMultipartRequest() {
    return delegate().createMultipartRequest();
  }

  @Override
  protected AsyncWebRequest createAsyncWebRequest() {
    return delegate().createAsyncWebRequest();
  }

  @Override
  public AsyncWebRequest asyncWebRequest() {
    return delegate().asyncWebRequest();
  }

  @Override
  public boolean isConcurrentHandlingStarted() {
    return delegate().isConcurrentHandlingStarted();
  }

  @Override
  public MultipartRequest asMultipartRequest() {
    return delegate().asMultipartRequest();
  }

  @Override
  public void setBinding(@Nullable BindingContext bindingContext) {
    delegate().setBinding(bindingContext);
  }

  @Nullable
  @Override
  public BindingContext getBinding() {
    return delegate().getBinding();
  }

  @Override
  public BindingContext binding() {
    return delegate().binding();
  }

  @Override
  public boolean hasBinding() {
    return delegate().hasBinding();
  }

  @Nullable
  @Override
  public RedirectModel getInputRedirectModel() {
    return delegate().getInputRedirectModel();
  }

  @Nullable
  @Override
  public RedirectModel getInputRedirectModel(@Nullable RedirectModelManager manager) {
    return delegate().getInputRedirectModel(manager);
  }

  // AttributeAccessorSupport

  @Override
  public <T> T computeAttribute(String name, Function<String, @Nullable T> computeFunction) {
    return delegate().computeAttribute(name, computeFunction);
  }

  @Override
  public boolean hasAttribute(String name) {
    return delegate().hasAttribute(name);
  }

  @Override
  public Iterable<String> attributeNames() {
    return delegate().attributeNames();
  }

  @Override
  public void copyFrom(AttributeAccessor source) {
    delegate().copyFrom(source);
  }

  @Override
  public boolean hasAttributes() {
    return delegate().hasAttributes();
  }

  @Override
  public Map<String, Object> getAttributes() {
    return delegate().getAttributes();
  }

  @Override
  public HttpMethod getMethod() {
    return delegate().getMethod();
  }

  @Override
  public String getMethodAsString() {
    return delegate().getMethodAsString();
  }

  @Override
  public HandlerMatchingMetadata matchingMetadata() {
    return delegate().matchingMetadata();
  }

  @Override
  public WebAsyncManager asyncManager() {
    return delegate().asyncManager();
  }

  @Override
  public void requestCompletedInternal(@Nullable Throwable notHandled) {
    delegate().requestCompletedInternal(notHandled);
  }

  @Override
  protected RequestPath readRequestPath() {
    return delegate().readRequestPath();
  }

  public abstract RequestContext delegate();

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof DecoratingRequestContext that))
      return false;
    return Objects.equals(delegate(), that.delegate());
  }

  @Override
  public int hashCode() {
    return System.identityHashCode(this);
  }

  @Override
  public String toString() {
    return "Wrapper for " + delegate();
  }

}
