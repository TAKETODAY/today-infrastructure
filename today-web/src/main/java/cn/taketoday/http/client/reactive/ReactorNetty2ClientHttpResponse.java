/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.Netty5DataBufferFactory;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.ResponseCookie;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.LinkedMultiValueMap;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.util.ObjectUtils;
import io.netty5.handler.codec.http.headers.DefaultHttpSetCookie;
import io.netty5.handler.codec.http.headers.HttpSetCookie;
import reactor.core.publisher.Flux;
import reactor.netty5.ChannelOperationsId;
import reactor.netty5.Connection;
import reactor.netty5.NettyInbound;
import reactor.netty5.http.client.HttpClientResponse;

/**
 * {@link ClientHttpResponse} implementation for the Reactor Netty 2 (Netty 5) HTTP client.
 *
 * <p>This class is based on {@link ReactorClientHttpResponse}.
 *
 * @author Violeta Georgieva
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see reactor.netty5.http.client.HttpClient
 * @since 4.0
 */
class ReactorNetty2ClientHttpResponse implements ClientHttpResponse {

  private static final Logger logger = LoggerFactory.getLogger(ReactorNetty2ClientHttpResponse.class);

  private final HttpClientResponse response;

  private final HttpHeaders headers;

  private final NettyInbound inbound;

  private final Netty5DataBufferFactory bufferFactory;

  // 0 - not subscribed, 1 - subscribed, 2 - cancelled via connector (before subscribe)
  private final AtomicInteger state = new AtomicInteger();

  /**
   * Constructor that matches the inputs from
   * {@link reactor.netty5.http.client.HttpClient.ResponseReceiver#responseConnection(BiFunction)}.
   */
  public ReactorNetty2ClientHttpResponse(HttpClientResponse response, Connection connection) {
    this.response = response;
    MultiValueMap<String, String> adapter = new Netty5HeadersAdapter(response.responseHeaders());
    this.headers = HttpHeaders.readOnlyHttpHeaders(adapter);
    this.inbound = connection.inbound();
    this.bufferFactory = new Netty5DataBufferFactory(connection.outbound().alloc());
  }

  @Override
  public String getId() {
    String id = null;
    if (this.response instanceof ChannelOperationsId operationsId) {
      id = (logger.isDebugEnabled() ? operationsId.asLongText() : operationsId.asShortText());
    }
    if (id == null && this.response instanceof Connection connection) {
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
            .map(buffer -> this.bufferFactory.wrap(buffer.split()));
  }

  @Override
  public HttpHeaders getHeaders() {
    return this.headers;
  }

  @Override
  public HttpStatusCode getStatusCode() {
    return HttpStatusCode.valueOf(this.response.status().code());
  }

  @Override
  public int getRawStatusCode() {
    return this.response.status().code();
  }

  @Override
  public MultiValueMap<String, ResponseCookie> getCookies() {
    MultiValueMap<String, ResponseCookie> result = new LinkedMultiValueMap<>();
    this.response.cookies().values().stream()
            .flatMap(Collection::stream)
            .forEach(cookie -> result.add(cookie.name().toString(),
                    ResponseCookie.fromClientResponse(cookie.name().toString(), cookie.value().toString())
                            .domain(cookie.domain() != null ? cookie.domain().toString() : null)
                            .path(cookie.path() != null ? cookie.path().toString() : null)
                            .maxAge(cookie.maxAge() != null ? cookie.maxAge() : -1L)
                            .secure(cookie.isSecure())
                            .httpOnly(cookie.isHttpOnly())
                            .sameSite(getSameSite(cookie))
                            .build()));
    return MultiValueMap.forUnmodifiable(result);
  }

  @Nullable
  private static String getSameSite(HttpSetCookie cookie) {
    if (cookie instanceof DefaultHttpSetCookie defaultCookie) {
      if (defaultCookie.sameSite() != null) {
        return defaultCookie.sameSite().name();
      }
    }
    return null;
  }

  /**
   * Called by {@link ReactorNetty2ClientHttpConnector} when a cancellation is detected
   * but the content has not been subscribed to. If the subscription never
   * materializes then the content will remain not drained. Or it could still
   * materialize if the cancellation happened very early, or the response
   * reading was delayed for some reason.
   */
  void releaseAfterCancel(HttpMethod method) {
    if (mayHaveBody(method) && this.state.compareAndSet(0, 2)) {
      if (logger.isDebugEnabled()) {
        logger.debug("[" + getId() + "]" + "Releasing body, not yet subscribed.");
      }
      this.inbound.receive().doOnNext(buffer -> { }).subscribe(buffer -> { }, ex -> { });
    }
  }

  private boolean mayHaveBody(HttpMethod method) {
    int code = this.getStatusCode().value();
    return !((code >= 100 && code < 200) || code == 204 || code == 205 ||
            method.equals(HttpMethod.HEAD) || getHeaders().getContentLength() == 0);
  }

  @Override
  public String toString() {
    return "ReactorNetty2ClientHttpResponse{" +
            "request=[" + this.response.method().name() + " " + this.response.uri() + "]," +
            "status=" + getStatusCode() + '}';
  }

}
