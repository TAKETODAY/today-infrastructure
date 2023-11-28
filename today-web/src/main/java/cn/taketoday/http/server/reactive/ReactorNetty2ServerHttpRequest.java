/*
 * Copyright 2017 - 2023 the original author or authors.
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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicLong;

import javax.net.ssl.SSLSession;

import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.Netty5DataBufferFactory;
import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpLogging;
import cn.taketoday.http.support.Netty5HeadersAdapter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.util.LinkedMultiValueMap;
import cn.taketoday.util.MultiValueMap;
import io.netty5.channel.Channel;
import io.netty5.handler.codec.http.HttpHeaderNames;
import io.netty5.handler.codec.http.headers.HttpCookiePair;
import io.netty5.handler.ssl.SslHandler;
import reactor.core.publisher.Flux;
import reactor.netty5.ChannelOperationsId;
import reactor.netty5.Connection;
import reactor.netty5.http.server.HttpServerRequest;

/**
 * Adapt {@link ServerHttpRequest} to the Reactor {@link HttpServerRequest}.
 *
 * <p>This class is based on {@link ReactorServerHttpRequest}.
 *
 * @author Violeta Georgieva
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class ReactorNetty2ServerHttpRequest extends AbstractServerHttpRequest {

  private static final Logger logger = HttpLogging.forLogName(ReactorNetty2ServerHttpRequest.class);

  private static final AtomicLong logPrefixIndex = new AtomicLong();

  private final HttpServerRequest request;

  private final Netty5DataBufferFactory bufferFactory;

  public ReactorNetty2ServerHttpRequest(HttpServerRequest request, Netty5DataBufferFactory bufferFactory)
          throws URISyntaxException {

    super(initUri(request), "", new Netty5HeadersAdapter(request.requestHeaders()));
    Assert.notNull(bufferFactory, "DataBufferFactory is required");
    this.request = request;
    this.bufferFactory = bufferFactory;
  }

  private static URI initUri(HttpServerRequest request) throws URISyntaxException {
    Assert.notNull(request, "HttpServerRequest is required");
    return new URI(resolveBaseUrl(request) + resolveRequestUri(request));
  }

  private static URI resolveBaseUrl(HttpServerRequest request) throws URISyntaxException {
    String scheme = getScheme(request);

    InetSocketAddress hostAddress = request.hostAddress();
    if (hostAddress != null) {
      return new URI(scheme, null, hostAddress.getHostString(), hostAddress.getPort(), null, null, null);
    }

    CharSequence charSequence = request.requestHeaders().get(HttpHeaderNames.HOST);
    if (charSequence != null) {
      String header = charSequence.toString();
      final int portIndex;
      if (header.startsWith("[")) {
        portIndex = header.indexOf(':', header.indexOf(']'));
      }
      else {
        portIndex = header.indexOf(':');
      }
      if (portIndex != -1) {
        try {
          return new URI(scheme, null, header.substring(0, portIndex),
                  Integer.parseInt(header, portIndex + 1, header.length(), 10), null, null, null);
        }
        catch (NumberFormatException ex) {
          throw new URISyntaxException(header, "Unable to parse port", portIndex);
        }
      }
      else {
        return new URI(scheme, header, null, null);
      }
    }

    throw new IllegalStateException("Neither local hostAddress nor HOST header available");
  }

  private static String getScheme(HttpServerRequest request) {
    return request.scheme();
  }

  private static String resolveRequestUri(HttpServerRequest request) {
    String uri = request.uri();
    for (int i = 0; i < uri.length(); i++) {
      char c = uri.charAt(i);
      if (c == '/' || c == '?' || c == '#') {
        break;
      }
      if (c == ':' && (i + 2 < uri.length())) {
        if (uri.charAt(i + 1) == '/' && uri.charAt(i + 2) == '/') {
          for (int j = i + 3; j < uri.length(); j++) {
            c = uri.charAt(j);
            if (c == '/' || c == '?' || c == '#') {
              return uri.substring(j);
            }
          }
          return "";
        }
      }
    }
    return uri;
  }

  @Override
  public String getMethodValue() {
    return this.request.method().name();
  }

  @Override
  protected MultiValueMap<String, HttpCookie> initCookies() {
    MultiValueMap<String, HttpCookie> cookies = new LinkedMultiValueMap<>();
    for (CharSequence name : this.request.cookies().keySet()) {
      for (HttpCookiePair cookie : this.request.cookies().get(name)) {
        CharSequence cookieValue = cookie.value();
        HttpCookie httpCookie = new HttpCookie(name.toString(), cookieValue != null ? cookieValue.toString() : null);
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
    return this.request.receive().transferOwnership().map(this.bufferFactory::wrap);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T getNativeRequest() {
    return (T) this.request;
  }

  @Override
  @Nullable
  protected String initId() {
    if (this.request instanceof Connection connection) {
      return connection.channel().id().asShortText() +
              "-" + logPrefixIndex.incrementAndGet();
    }
    return null;
  }

  @Override
  protected String initLogPrefix() {
    String id = null;
    if (this.request instanceof ChannelOperationsId operationsId) {
      id = (logger.isDebugEnabled() ? operationsId.asLongText() : operationsId.asShortText());
    }
    if (id != null) {
      return id;
    }
    if (this.request instanceof Connection connection) {
      return connection.channel().id().asShortText() +
              "-" + logPrefixIndex.incrementAndGet();
    }
    return getId();
  }

}
