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

package cn.taketoday.http.server.reactive;

import org.xnio.channels.StreamSourceChannel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.net.ssl.SSLSession;

import cn.taketoday.core.MultiValueMap;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.http.HttpCookie;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;
import io.undertow.connector.ByteBufferPool;
import io.undertow.connector.PooledByteBuffer;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import reactor.core.publisher.Flux;

/**
 * Adapt {@link ServerHttpRequest} to the Undertow {@link HttpServerExchange}.
 *
 * @author Marek Hawrylczak
 * @author Rossen Stoyanchev
 * @since 4.0
 */
class UndertowServerHttpRequest extends AbstractServerHttpRequest {
  private static final AtomicLong logPrefixIndex = new AtomicLong();

  private final RequestBodyPublisher body;
  private final HttpServerExchange exchange;

  public UndertowServerHttpRequest(HttpServerExchange exchange, DataBufferFactory bufferFactory)
          throws URISyntaxException {

    super(initUri(exchange), "", new UndertowHeadersAdapter(exchange.getRequestHeaders()));
    this.exchange = exchange;
    this.body = new RequestBodyPublisher(exchange, bufferFactory);
    this.body.registerListeners(exchange);
  }

  private static URI initUri(HttpServerExchange exchange) throws URISyntaxException {
    Assert.notNull(exchange, "HttpServerExchange is required");
    String requestURL = exchange.getRequestURL();
    String query = exchange.getQueryString();
    String requestUriAndQuery = (StringUtils.isNotEmpty(query) ? requestURL + "?" + query : requestURL);
    return new URI(requestUriAndQuery);
  }

  @Override
  public String getMethodValue() {
    return this.exchange.getRequestMethod().toString();
  }

  @SuppressWarnings("deprecation")
  @Override
  protected MultiValueMap<String, HttpCookie> initCookies() {
    MultiValueMap<String, HttpCookie> cookies = MultiValueMap.fromLinkedHashMap();
    // getRequestCookies() is deprecated in Undertow 2.2
    for (Map.Entry<String, Cookie> entry : exchange.getRequestCookies().entrySet()) {
      String name = entry.getKey();
      Cookie cookie = entry.getValue();
      HttpCookie httpCookie = new HttpCookie(name, cookie.getValue());
      cookies.add(name, httpCookie);
    }
    return cookies;
  }

  @Override
  @Nullable
  public InetSocketAddress getLocalAddress() {
    return this.exchange.getDestinationAddress();
  }

  @Override
  @Nullable
  public InetSocketAddress getRemoteAddress() {
    return this.exchange.getSourceAddress();
  }

  @Nullable
  @Override
  protected SslInfo initSslInfo() {
    SSLSession session = this.exchange.getConnection().getSslSession();
    if (session != null) {
      return new DefaultSslInfo(session);
    }
    return null;
  }

  @Override
  public Flux<DataBuffer> getBody() {
    return Flux.from(this.body);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T getNativeRequest() {
    return (T) this.exchange;
  }

  @Override
  protected String initId() {
    return ObjectUtils.getIdentityHexString(this.exchange.getConnection())
            + "-" + logPrefixIndex.incrementAndGet();
  }

  private class RequestBodyPublisher extends AbstractListenerReadPublisher<DataBuffer> {

    private final StreamSourceChannel channel;
    private final ByteBufferPool byteBufferPool;
    private final DataBufferFactory bufferFactory;

    public RequestBodyPublisher(HttpServerExchange exchange, DataBufferFactory bufferFactory) {
      super(UndertowServerHttpRequest.this.getLogPrefix());
      this.channel = exchange.getRequestChannel();
      this.bufferFactory = bufferFactory;
      this.byteBufferPool = exchange.getConnection().getByteBufferPool();
    }

    private void registerListeners(HttpServerExchange exchange) {
      exchange.addExchangeCompleteListener((ex, next) -> {
        onAllDataRead();
        next.proceed();
      });
      this.channel.getReadSetter().set(c -> onDataAvailable());
      this.channel.getCloseSetter().set(c -> onAllDataRead());
      this.channel.resumeReads();
    }

    @Override
    protected void checkOnDataAvailable() {
      this.channel.resumeReads();
      // We are allowed to try, it will return null if data is not available
      onDataAvailable();
    }

    @Override
    protected void readingPaused() {
      this.channel.suspendReads();
    }

    @Override
    @Nullable
    protected DataBuffer read() throws IOException {
      PooledByteBuffer pooledByteBuffer = this.byteBufferPool.allocate();
      try (pooledByteBuffer) {
        ByteBuffer byteBuffer = pooledByteBuffer.getBuffer();
        int read = this.channel.read(byteBuffer);

        if (rsReadLogger.isTraceEnabled()) {
          rsReadLogger.trace("{}Read {}{}", getLogPrefix(), read, (read != -1 ? " bytes" : ""));
        }

        if (read > 0) {
          byteBuffer.flip();
          DataBuffer dataBuffer = this.bufferFactory.allocateBuffer(read);
          dataBuffer.write(byteBuffer);
          return dataBuffer;
        }
        else if (read == -1) {
          onAllDataRead();
        }
        return null;
      }
    }

    @Override
    protected void discardData() {
      // Nothing to discard since we pass data buffers on immediately..
    }
  }

}
