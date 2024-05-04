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

package cn.taketoday.http.server.reactive;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.core.io.buffer.DefaultDataBufferFactory;
import cn.taketoday.http.DefaultHttpHeaders;
import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.util.LinkedCaseInsensitiveMap;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.mock.AsyncContext;
import cn.taketoday.web.mock.AsyncEvent;
import cn.taketoday.web.mock.AsyncListener;
import cn.taketoday.web.mock.ReadListener;
import cn.taketoday.web.mock.ServletInputStream;
import cn.taketoday.web.mock.http.Cookie;
import cn.taketoday.web.mock.http.HttpServletRequest;
import reactor.core.publisher.Flux;

/**
 * Adapt {@link ServerHttpRequest} to the Servlet {@link HttpServletRequest}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class ServletServerHttpRequest extends AbstractServerHttpRequest {

  static final DataBuffer EOF_BUFFER = DefaultDataBufferFactory.sharedInstance.allocateBuffer(0);

  private final Object cookieLock = new Object();

  private final byte[] buffer;

  private final HttpServletRequest request;

  private final AsyncListener asyncListener;

  private final ServletInputStream inputStream;

  private final DataBufferFactory bufferFactory;

  private final RequestBodyPublisher bodyPublisher;

  public ServletServerHttpRequest(HttpServletRequest request, AsyncContext asyncContext,
          String servletPath, DataBufferFactory bufferFactory, int bufferSize)
          throws IOException, URISyntaxException {
    this(createDefaultHttpHeaders(request), request, asyncContext, servletPath, bufferFactory, bufferSize);
  }

  public ServletServerHttpRequest(MultiValueMap<String, String> headers, HttpServletRequest request,
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

  private static MultiValueMap<String, String> createDefaultHttpHeaders(HttpServletRequest request) {
    var headers = MultiValueMap.<String, String>forAdaption(new LinkedCaseInsensitiveMap<>(8, Locale.ENGLISH));
    for (Enumeration<String> names = request.getHeaderNames(); names.hasMoreElements(); ) {
      String name = names.nextElement();
      headers.addAll(name, request.getHeaders(name));
    }
    return headers;
  }

  private static URI initUri(HttpServletRequest request) throws URISyntaxException {
    Assert.notNull(request, "'request' is required");
    StringBuffer url = request.getRequestURL();
    String query = request.getQueryString();
    if (StringUtils.hasText(query)) {
      url.append('?').append(query);
    }
    return new URI(url.toString());
  }

  private static MultiValueMap<String, String> initHeaders(
          MultiValueMap<String, String> headerValues, HttpServletRequest request) {

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
  public String getMethodValue() {
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
  @NonNull
  public InetSocketAddress getLocalAddress() {
    return new InetSocketAddress(this.request.getLocalAddr(), this.request.getLocalPort());
  }

  @Override
  @NonNull
  public InetSocketAddress getRemoteAddress() {
    return new InetSocketAddress(this.request.getRemoteHost(), this.request.getRemotePort());
  }

  @Override
  @Nullable
  protected SslInfo initSslInfo() {
    X509Certificate[] certificates = getX509Certificates();
    return certificates != null ? new DefaultSslInfo(getSslSessionId(), certificates) : null;
  }

  @Nullable
  private String getSslSessionId() {
    return (String) this.request.getAttribute("cn.taketoday.web.mock.request.ssl_session_id");
  }

  @Nullable
  private X509Certificate[] getX509Certificates() {
    String name = "cn.taketoday.web.mock.request.X509Certificate";
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
   * {@link ServletHttpHandlerAdapter} to ensure events are delegated.
   */
  AsyncListener getAsyncListener() {
    return this.asyncListener;
  }

  /**
   * Return the {@link ServletInputStream} for the current response.
   */
  protected final ServletInputStream getInputStream() {
    return this.inputStream;
  }

  /**
   * Read from the request body InputStream and return a DataBuffer.
   * Invoked only when {@link ServletInputStream#isReady()} returns "true".
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
    private final ServletInputStream inputStream;

    public RequestBodyPublisher(ServletInputStream inputStream) {
      super(ServletServerHttpRequest.this.getLogPrefix());
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
