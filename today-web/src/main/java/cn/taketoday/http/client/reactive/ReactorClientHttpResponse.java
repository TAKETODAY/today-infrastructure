/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.http.client.reactive;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import cn.taketoday.core.MultiValueMap;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.NettyDataBufferFactory;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.ResponseCookie;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ObjectUtils;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import reactor.core.publisher.Flux;
import reactor.netty.ChannelOperationsId;
import reactor.netty.Connection;
import reactor.netty.NettyInbound;
import reactor.netty.http.client.HttpClientResponse;

/**
 * {@link ClientHttpResponse} implementation for the Reactor-Netty HTTP client.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see reactor.netty.http.client.HttpClient
 * @since 4.0
 */
class ReactorClientHttpResponse implements ClientHttpResponse {

  private static final Logger logger = LoggerFactory.getLogger(ReactorClientHttpResponse.class);

  private final HttpHeaders headers;
  private final NettyInbound inbound;
  private final HttpClientResponse response;
  private final NettyDataBufferFactory bufferFactory;

  // 0 - not subscribed, 1 - subscribed, 2 - cancelled via connector (before subscribe)
  private final AtomicInteger state = new AtomicInteger();

  /**
   * Constructor that matches the inputs from
   * {@link reactor.netty.http.client.HttpClient.ResponseReceiver#responseConnection(BiFunction)}.
   */
  public ReactorClientHttpResponse(HttpClientResponse response, Connection connection) {
    this.response = response;
    MultiValueMap<String, String> adapter = new NettyHeadersAdapter(response.responseHeaders());
    this.headers = HttpHeaders.readOnlyHttpHeaders(adapter);
    this.inbound = connection.inbound();
    this.bufferFactory = new NettyDataBufferFactory(connection.outbound().alloc());
  }

  /**
   * Constructor with inputs extracted from a {@link Connection}.
   */
  public ReactorClientHttpResponse(HttpClientResponse response, NettyInbound inbound, ByteBufAllocator alloc) {
    this.response = response;
    MultiValueMap<String, String> adapter = new NettyHeadersAdapter(response.responseHeaders());
    this.headers = HttpHeaders.readOnlyHttpHeaders(adapter);
    this.inbound = inbound;
    this.bufferFactory = new NettyDataBufferFactory(alloc);
  }

  @Override
  public String getId() {
    String id = null;
    if (response instanceof ChannelOperationsId operationsId) {
      id = logger.isDebugEnabled() ? operationsId.asLongText() : operationsId.asShortText();
    }
    if (id == null && response instanceof Connection connection) {
      id = connection.channel().id().asShortText();
    }
    return (id != null ? id : ObjectUtils.getIdentityHexString(this));
  }

  @Override
  public Flux<DataBuffer> getBody() {
    return this.inbound.receive()
            .doOnSubscribe(s -> {
              if (this.state.compareAndSet(0, 1)) {
                return;
              }
              if (this.state.get() == 2) {
                throw new IllegalStateException(
                        "The client response body has been released already due to cancellation.");
              }
            })
            .map(byteBuf -> {
              byteBuf.retain();
              return this.bufferFactory.wrap(byteBuf);
            });
  }

  @Override
  public HttpHeaders getHeaders() {
    return this.headers;
  }

  @Override
  public int getRawStatusCode() {
    return this.response.status().code();
  }

  @Override
  public MultiValueMap<String, ResponseCookie> getCookies() {
    MultiValueMap<String, ResponseCookie> result = MultiValueMap.fromLinkedHashMap();
    for (Map.Entry<CharSequence, Set<Cookie>> entry : response.cookies().entrySet()) {
      Set<Cookie> cookies = entry.getValue();
      for (Cookie cookie : cookies) {
        result.add(cookie.name(),
                ResponseCookie.fromClientResponse(cookie.name(), cookie.value())
                        .domain(cookie.domain())
                        .path(cookie.path())
                        .maxAge(cookie.maxAge())
                        .secure(cookie.isSecure())
                        .httpOnly(cookie.isHttpOnly())
                        .sameSite(getSameSite(cookie))
                        .build());
      }
    }
    return MultiValueMap.unmodifiable(result);
  }

  @Nullable
  private static String getSameSite(Cookie cookie) {
    if (cookie instanceof DefaultCookie defaultCookie && defaultCookie.sameSite() != null) {
      return defaultCookie.sameSite().name();
    }
    return null;
  }

  /**
   * Called by {@link ReactorClientHttpConnector} when a cancellation is detected
   * but the content has not been subscribed to. If the subscription never
   * materializes then the content will remain not drained. Or it could still
   * materialize if the cancellation happened very early, or the response
   * reading was delayed for some reason.
   */
  void releaseAfterCancel(HttpMethod method) {
    if (mayHaveBody(method) && this.state.compareAndSet(0, 2)) {
      if (logger.isDebugEnabled()) {
        logger.debug("[{}] Releasing body, not yet subscribed.", getId());
      }
      this.inbound.receive()
              .doOnNext(byteBuf -> {

              })
              .subscribe(byteBuf -> {

              }, ex -> {

              });
    }
  }

  private boolean mayHaveBody(HttpMethod method) {
    int code = this.getRawStatusCode();
    return !((code >= 100 && code < 200) || code == 204 || code == 205 ||
            method.equals(HttpMethod.HEAD) || getHeaders().getContentLength() == 0);
  }

  @Override
  public String toString() {
    return "ReactorClientHttpResponse{" +
            "request=[" + this.response.method().name() + " " + this.response.uri() + "]," +
            "status=" + getRawStatusCode() + '}';
  }

}
