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

package infra.http.client.reactive;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.NettyDataBufferFactory;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.ResponseCookie;
import infra.http.support.Netty4HttpHeaders;
import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.MultiValueMap;
import infra.util.ObjectUtils;
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
    this.inbound = connection.inbound();
    this.headers = new Netty4HttpHeaders(response.responseHeaders()).asReadOnly();
    this.bufferFactory = new NettyDataBufferFactory(connection.outbound().alloc());
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
    MultiValueMap<String, ResponseCookie> result = MultiValueMap.forLinkedHashMap();
    for (Map.Entry<CharSequence, Set<Cookie>> entry : response.cookies().entrySet()) {
      Set<Cookie> cookies = entry.getValue();
      for (Cookie cookie : cookies) {
        result.add(cookie.name(), ResponseCookie.fromClientResponse(cookie.name(), cookie.value())
                .domain(cookie.domain())
                .path(cookie.path())
                .maxAge(cookie.maxAge())
                .secure(cookie.isSecure())
                .httpOnly(cookie.isHttpOnly())
                .sameSite(getSameSite(cookie))
                .partitioned(getPartitioned(cookie))
                .build());
      }
    }
    return result.asReadOnly();
  }

  @Nullable
  private static String getSameSite(Cookie cookie) {
    if (cookie instanceof DefaultCookie defaultCookie && defaultCookie.sameSite() != null) {
      return defaultCookie.sameSite().name();
    }
    return null;
  }

  private static boolean getPartitioned(Cookie cookie) {
    if (cookie instanceof DefaultCookie defaultCookie) {
      return defaultCookie.isPartitioned();
    }
    return false;
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
    int code = getRawStatusCode();
    return !((code >= 100 && code < 200) || code == 204 || code == 205 ||
            method.equals(HttpMethod.HEAD) || getHeaders().getContentLength() == 0);
  }

  @Override
  public String toString() {
    return "ReactorClientHttpResponse{request=[%s %s],status=%d}"
            .formatted(this.response.method().name(), this.response.uri(), getRawStatusCode());
  }

}
