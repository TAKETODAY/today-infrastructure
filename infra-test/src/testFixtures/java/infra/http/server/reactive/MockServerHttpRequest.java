/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.http.server.reactive;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DataBufferFactory;
import infra.core.io.buffer.DefaultDataBufferFactory;
import infra.http.DefaultHttpHeaders;
import infra.http.HttpCookie;
import infra.http.HttpHeaders;
import infra.http.MediaType;
import infra.lang.Assert;
import org.jspecify.annotations.Nullable;
import infra.logging.Logger;
import infra.mock.api.AsyncContext;
import infra.mock.api.AsyncEvent;
import infra.mock.api.AsyncListener;
import infra.mock.api.MockInputStream;
import infra.mock.api.ReadListener;
import infra.mock.api.http.Cookie;
import infra.mock.api.http.HttpMockRequest;
import infra.util.LinkedCaseInsensitiveMap;
import infra.util.MultiValueMap;
import infra.util.StringUtils;
import reactor.core.publisher.Flux;

/**
 * Adapt {@link ServerHttpRequest} to the Servlet {@link HttpMockRequest}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class MockServerHttpRequest extends AbstractServerHttpRequest {

  static final DataBuffer EOF_BUFFER = DefaultDataBufferFactory.sharedInstance.allocateBuffer(0);

  private final Object cookieLock = new Object();

  private final byte[] buffer;

  private final HttpMockRequest request;

  private final AsyncListener asyncListener;

  private final MockInputStream inputStream;

  private final DataBufferFactory bufferFactory;

  private final RequestBodyPublisher bodyPublisher;

  public MockServerHttpRequest(HttpMockRequest request, AsyncContext asyncContext,
          String servletPath, DataBufferFactory bufferFactory, int bufferSize)
          throws IOException, URISyntaxException {
    this(createDefaultHttpHeaders(request), request, asyncContext, servletPath, bufferFactory, bufferSize);
  }

  public MockServerHttpRequest(MultiValueMap<String, String> headers, HttpMockRequest request,
          AsyncContext asyncContext, String servletPath, DataBufferFactory bufferFactory, int bufferSize)
          throws IOException, URISyntaxException {

    super(initUri(request), servletPath, initHeaders(headers, request));

    Assert.notNull(bufferFactory, "'bufferFactory' is required");
    Assert.isTrue(bufferSize > 0, "'bufferSize' must be higher than 0");

    this.request = request;
    this.bufferFactory = bufferFactory;
    this.buffer = new byte[bufferSize];

    this.asyncListener = new RequestAsyncListener();

    // Tomcat expects ReadListener registration on initial thread
    this.inputStream = request.getInputStream();
    this.bodyPublisher = new RequestBodyPublisher(inputStream);
    this.bodyPublisher.registerReadListener();
  }

  private static MultiValueMap<String, String> createDefaultHttpHeaders(HttpMockRequest request) {
    var headers = MultiValueMap.<String, String>forAdaption(new LinkedCaseInsensitiveMap<>(8, Locale.ENGLISH));
    for (Enumeration<String> names = request.getHeaderNames(); names.hasMoreElements(); ) {
      String name = names.nextElement();
      headers.addAll(name, request.getHeaders(name));
    }
    return headers;
  }

  private static URI initUri(HttpMockRequest request) throws URISyntaxException {
    Assert.notNull(request, "'request' is required");
    StringBuffer url = request.getRequestURL();
    String query = request.getQueryString();
    if (StringUtils.hasText(query)) {
      url.append('?').append(query);
    }
    return new URI(url.toString());
  }

  private static MultiValueMap<String, String> initHeaders(
          MultiValueMap<String, String> headerValues, HttpMockRequest request) {

    HttpHeaders headers = null;
    MediaType contentType = null;
    if (StringUtils.isEmpty(headerValues.getFirst(HttpHeaders.CONTENT_TYPE))) {
      String requestContentType = request.getContentType();
      if (StringUtils.isNotEmpty(requestContentType)) {
        contentType = MediaType.parseMediaType(requestContentType);
        headers = new DefaultHttpHeaders(headerValues);
        headers.setContentType(contentType);
      }
    }
    if (contentType != null && contentType.getCharset() == null) {
      String encoding = request.getCharacterEncoding();
      if (StringUtils.isNotEmpty(encoding)) {
        Map<String, String> params = new LinkedCaseInsensitiveMap<>();
        params.putAll(contentType.getParameters());
        params.put("charset", Charset.forName(encoding).toString());
        headers.setContentType(new MediaType(contentType, params));
      }
    }
    if (headerValues.getFirst(HttpHeaders.CONTENT_TYPE) == null) {
      int contentLength = request.getContentLength();
      if (contentLength != -1) {
        headers = (headers != null ? headers : new DefaultHttpHeaders(headerValues));
        headers.setContentLength(contentLength);
      }
    }
    return (headers != null ? headers : headerValues);
  }

  @Override
  public String getMethodAsString() {
    return this.request.getMethod();
  }

  @Override
  protected MultiValueMap<String, HttpCookie> initCookies() {
    MultiValueMap<String, HttpCookie> httpCookies = MultiValueMap.forLinkedHashMap();
    Cookie[] cookies;
    synchronized(this.cookieLock) {
      cookies = this.request.getCookies();
    }
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        String name = cookie.getName();
        HttpCookie httpCookie = new HttpCookie(name, cookie.getValue());
        httpCookies.add(name, httpCookie);
      }
    }
    return httpCookies;
  }

  @Override
  public InetSocketAddress getLocalAddress() {
    return new InetSocketAddress(this.request.getLocalAddr(), this.request.getLocalPort());
  }

  @Override
  public InetSocketAddress getRemoteAddress() {
    return new InetSocketAddress(this.request.getRemoteHost(), this.request.getRemotePort());
  }

  @Override
  @Nullable
  protected SslInfo initSslInfo() {
    X509Certificate[] certificates = getX509Certificates();
    return certificates != null ? new infra.http.server.reactive.DefaultSslInfo(getSslSessionId(), certificates) : null;
  }

  @Nullable
  private String getSslSessionId() {
    return (String) this.request.getAttribute("infra.mock.api.request.ssl_session_id");
  }

  @Nullable
  private X509Certificate[] getX509Certificates() {
    String name = "infra.mock.api.request.X509Certificate";
    return (X509Certificate[]) this.request.getAttribute(name);
  }

  @Override
  public Flux<DataBuffer> getBody() {
    return Flux.from(this.bodyPublisher);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T getNativeRequest() {
    return (T) this.request;
  }

  /**
   * Return an {@link RequestAsyncListener} that completes the request body
   * Publisher when the Servlet container notifies that request input has ended.
   * The listener is not actually registered but is rather exposed for
   * {@link MockHttpHandlerAdapter} to ensure events are delegated.
   */
  AsyncListener getAsyncListener() {
    return this.asyncListener;
  }

  /**
   * Return the {@link MockInputStream} for the current response.
   */
  protected final MockInputStream getInputStream() {
    return this.inputStream;
  }

  /**
   * Read from the request body InputStream and return a DataBuffer.
   * Invoked only when {@link MockInputStream#isReady()} returns "true".
   *
   * @return a DataBuffer with data read, or
   * {@link AbstractListenerReadPublisher#EMPTY_BUFFER} if 0 bytes were read,
   * or {@link #EOF_BUFFER} if the input stream returned -1.
   */
  @Nullable
  DataBuffer readFromInputStream() throws IOException {
    int read = inputStream.read(buffer);
    logBytesRead(read);

    if (read > 0) {
      DataBuffer dataBuffer = bufferFactory.allocateBuffer(read);
      dataBuffer.write(buffer, 0, read);
      return dataBuffer;
    }

    if (read == -1) {
      return EOF_BUFFER;
    }

    return AbstractListenerReadPublisher.EMPTY_BUFFER;
  }

  protected final void logBytesRead(int read) {
    Logger rsReadLogger = AbstractListenerReadPublisher.rsReadLogger;
    if (rsReadLogger.isTraceEnabled()) {
      rsReadLogger.trace("{}Read {}{}", getLogPrefix(), read, (read != -1 ? " bytes" : ""));
    }
  }

  private final class RequestAsyncListener implements AsyncListener {

    @Override
    public void onStartAsync(AsyncEvent event) { }

    @Override
    public void onTimeout(AsyncEvent event) {
      Throwable ex = event.getThrowable();
      ex = ex != null ? ex : new IllegalStateException("Async operation timeout.");
      bodyPublisher.onError(ex);
    }

    @Override
    public void onError(AsyncEvent event) {
      bodyPublisher.onError(event.getThrowable());
    }

    @Override
    public void onComplete(AsyncEvent event) {
      bodyPublisher.onAllDataRead();
    }
  }

  private class RequestBodyPublisher extends AbstractListenerReadPublisher<DataBuffer> {
    private final MockInputStream inputStream;

    public RequestBodyPublisher(MockInputStream inputStream) {
      super(MockServerHttpRequest.this.getLogPrefix());
      this.inputStream = inputStream;
    }

    public void registerReadListener() throws IOException {
      this.inputStream.setReadListener(new RequestBodyPublisherReadListener());
    }

    @Override
    protected void checkOnDataAvailable() {
      if (this.inputStream.isReady() && !this.inputStream.isFinished()) {
        onDataAvailable();
      }
    }

    @Override
    @Nullable
    protected DataBuffer read() throws IOException {
      if (this.inputStream.isReady()) {
        DataBuffer dataBuffer = readFromInputStream();
        if (dataBuffer == EOF_BUFFER) {
          // No need to wait for container callback...
          onAllDataRead();
          dataBuffer = null;
        }
        return dataBuffer;
      }
      return null;
    }

    @Override
    protected void readingPaused() {
      // no-op
    }

    @Override
    protected void discardData() {
      // Nothing to discard since we pass data buffers on immediately..
    }

    private class RequestBodyPublisherReadListener implements ReadListener {

      @Override
      public void onDataAvailable() throws IOException {
        RequestBodyPublisher.this.onDataAvailable();
      }

      @Override
      public void onAllDataRead() throws IOException {
        RequestBodyPublisher.this.onAllDataRead();
      }

      @Override
      public void onError(Throwable throwable) {
        RequestBodyPublisher.this.onError(throwable);
      }
    }
  }

}
