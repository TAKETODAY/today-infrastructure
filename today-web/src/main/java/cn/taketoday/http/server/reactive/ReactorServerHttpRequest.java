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

package cn.taketoday.http.server.reactive;

import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import javax.net.ssl.SSLSession;

import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.NettyDataBufferFactory;
import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpLogging;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.util.DefaultMultiValueMap;
import cn.taketoday.util.MultiValueMap;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.ssl.SslHandler;
import reactor.core.publisher.Flux;
import reactor.netty.ChannelOperationsId;
import reactor.netty.Connection;
import reactor.netty.http.server.HttpServerRequest;

/**
 * Adapt {@link ServerHttpRequest} to the Reactor {@link HttpServerRequest}.
 *
 * @author Stephane Maldini
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class ReactorServerHttpRequest extends AbstractServerHttpRequest {

  private static final Logger logger = HttpLogging.forLogName(ReactorServerHttpRequest.class);

  private static final AtomicLong logPrefixIndex = new AtomicLong();

  private final HttpServerRequest request;
  private final NettyDataBufferFactory bufferFactory;

  public ReactorServerHttpRequest(HttpServerRequest request,
          NettyDataBufferFactory bufferFactory) throws URISyntaxException {
    super(HttpMethod.valueOf(request.method().name()), ReactorUriHelper.createUri(request), "",
            new NettyHeadersAdapter(request.requestHeaders()));
    Assert.notNull(bufferFactory, "DataBufferFactory must not be null");
    this.request = request;
    this.bufferFactory = bufferFactory;
  }

  @Override
  public String getMethodValue() {
    return this.request.method().name();
  }

  @Override
  protected MultiValueMap<String, HttpCookie> initCookies() {
    DefaultMultiValueMap<String, HttpCookie> cookies = MultiValueMap.fromLinkedHashMap();
    for (Map.Entry<CharSequence, Set<Cookie>> entry : request.cookies().entrySet()) {
      CharSequence name = entry.getKey();
      for (Cookie cookie : entry.getValue()) {
        HttpCookie httpCookie = new HttpCookie(name.toString(), cookie.value());
        cookies.add(name.toString(), httpCookie);
      }
    }
    return cookies;
  }

  @Override
  @Nullable
  public InetSocketAddress getLocalAddress() {
    return this.request.hostAddress();
  }

  @Override
  @Nullable
  public InetSocketAddress getRemoteAddress() {
    return this.request.remoteAddress();
  }

  @Override
  @Nullable
  protected SslInfo initSslInfo() {
    Channel channel = ((Connection) this.request).channel();
    SslHandler sslHandler = channel.pipeline().get(SslHandler.class);
    if (sslHandler == null && channel.parent() != null) { // HTTP/2
      sslHandler = channel.parent().pipeline().get(SslHandler.class);
    }
    if (sslHandler != null) {
      SSLSession session = sslHandler.engine().getSession();
      return new DefaultSslInfo(session);
    }
    return null;
  }

  @Override
  public Flux<DataBuffer> getBody() {
    return this.request.receive().retain().map(this.bufferFactory::wrap);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T getNativeRequest() {
    return (T) this.request;
  }

  @Override
  @Nullable
  protected String initId() {
    if (this.request instanceof Connection) {
      return ((Connection) this.request).channel().id().asShortText() + "-" + logPrefixIndex.incrementAndGet();
    }
    return null;
  }

  @Override
  protected String initLogPrefix() {
    String id = null;
    if (request instanceof ChannelOperationsId operationsId) {
      id = logger.isDebugEnabled() ? operationsId.asLongText() : operationsId.asShortText();
    }
    if (id != null) {
      return id;
    }
    if (request instanceof Connection) {
      return ((Connection) request).channel().id().asShortText() +
              "-" + logPrefixIndex.incrementAndGet();
    }
    return getId();
  }

}
