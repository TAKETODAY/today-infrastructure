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

package cn.taketoday.web.socket.client.support;

import java.net.URI;
import java.util.List;

import cn.taketoday.core.Decorator;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.concurrent.Future;
import cn.taketoday.util.concurrent.SettableFuture;
import cn.taketoday.web.socket.Message;
import cn.taketoday.web.socket.WebSocketExtension;
import cn.taketoday.web.socket.WebSocketHandler;
import cn.taketoday.web.socket.WebSocketSession;
import cn.taketoday.web.socket.client.AbstractWebSocketClient;
import cn.taketoday.web.socket.server.support.NettyWebSocketSession;
import cn.taketoday.web.socket.server.support.WsNettyChannelHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.util.CharsetUtil;

import static cn.taketoday.web.socket.handler.ExceptionWebSocketHandlerDecorator.tryCloseWithError;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0 2024/5/2 20:39
 */
public class NettyWebSocketClient extends AbstractWebSocketClient {

  private static final Logger log = LoggerFactory.getLogger(NettyWebSocketClient.class);

  /**
   * the maximum length of the aggregated content.
   * If the length of the aggregated content exceeds this value,
   *
   * @see HttpObjectAggregator#maxContentLength
   */
  private int maxContentLength = 1024 * 1024 * 64;

  /**
   * If a 100-continue response is detected but the content
   * length is too large then true means close the connection.
   * otherwise the connection will remain open and data will be
   * consumed and discarded until the next request is received.
   *
   * @see HttpObjectAggregator#closeOnExpectationFailed
   */
  private boolean closeOnExpectationFailed = false;

  @Nullable
  private Decorator<WebSocketSession> sessionDecorator;

  /**
   * Set the maximum length of the aggregated content.
   * If the length of the aggregated content exceeds this value,
   *
   * @param maxContentLength the maximum length of the aggregated content.
   * If the length of the aggregated content exceeds this value,
   * @see HttpObjectAggregator#maxContentLength
   */
  public void setMaxContentLength(int maxContentLength) {
    this.maxContentLength = maxContentLength;
  }

  /**
   * Set If a 100-continue response is detected but the content
   * length is too large then true means close the connection.
   * otherwise the connection will remain open and data will be
   * consumed and discarded until the next request is received.
   *
   * @see HttpObjectAggregator#closeOnExpectationFailed
   */
  public void setCloseOnExpectationFailed(boolean closeOnExpectationFailed) {
    this.closeOnExpectationFailed = closeOnExpectationFailed;
  }

  public void setSessionDecorator(@Nullable Decorator<WebSocketSession> sessionDecorator) {
    this.sessionDecorator = sessionDecorator;
  }

  @Override
  protected Future<WebSocketSession> doHandshakeInternal(WebSocketHandler webSocketHandler,
          HttpHeaders headers, URI uri, List<String> subProtocols, List<WebSocketExtension> extensions) {
    SettableFuture<WebSocketSession> future = Future.forSettable();

    // Connect with V13 (RFC 6455 aka HyBi-17). You can change it to V08 or V00.
    // If you change it to V00, ping is not supported and remember to change
    // HttpResponseDecoder to WebSocketHttpResponseDecoder in the pipeline.
    WebSocketClientHandshaker handshaker = createHandshaker(uri, subProtocols, extensions, createHeaders(headers));
    WebSocketClientHandler handler = new WebSocketClientHandler(uri, headers, webSocketHandler, handshaker, future);

    Bootstrap bootstrap = new Bootstrap();
    bootstrap.group(new NioEventLoopGroup())
            .channel(NioSocketChannel.class)
            .handler(new ChannelInitializer<SocketChannel>() {

              @Override
              protected void initChannel(SocketChannel ch) {
                ch.pipeline()
                        .addLast("httpClientCodec", new HttpClientCodec())
                        .addLast("httpObjectAggregator", new HttpObjectAggregator(maxContentLength, closeOnExpectationFailed))
                        .addLast("message-handler", handler);
                NettyWebSocketClient.this.initChannel(ch);
              }
            });

    bootstrap.connect(uri.getHost(), uri.getPort());
    return future;
  }

  private DefaultHttpHeaders createHeaders(HttpHeaders headers) {
    DefaultHttpHeaders entries = new DefaultHttpHeaders();
    headers.forEach(entries::add);
    return entries;
  }

  protected WebSocketClientHandshaker createHandshaker(URI uri, List<String> subProtocols,
          List<WebSocketExtension> extensions, io.netty.handler.codec.http.HttpHeaders customHeaders) {
    return WebSocketClientHandshakerFactory.newHandshaker(
            uri, WebSocketVersion.V13, null, true, customHeaders);
  }

  protected WebSocketSession createSession(HttpHeaders headers, Channel channel,
          boolean secure, @Nullable Decorator<WebSocketSession> sessionDecorator) {
    WebSocketSession session = new NettyWebSocketSession(headers, secure, channel);

    if (sessionDecorator != null) {
      session = sessionDecorator.decorate(session);
    }
    return session;
  }

  protected void initChannel(SocketChannel ch) {

  }

  final class WebSocketClientHandler extends ChannelInboundHandlerAdapter {

    private final URI uri;

    private final HttpHeaders headers;

    private final WebSocketHandler handler;

    private final WebSocketClientHandshaker handshaker;

    private final SettableFuture<WebSocketSession> future;

    private WebSocketSession session;

    public WebSocketClientHandler(URI uri, HttpHeaders headers, WebSocketHandler handler,
            WebSocketClientHandshaker handshaker, SettableFuture<WebSocketSession> future) {
      this.uri = uri;
      this.headers = headers;
      this.handler = handler;
      this.future = future;
      this.handshaker = handshaker;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
      handshaker.handshake(ctx.channel());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      if (!handshaker.isHandshakeComplete()) {
        Channel channel = ctx.channel();
        try {
          handshaker.finishHandshake(channel, (FullHttpResponse) msg);
          session = createSession(headers, channel, "wss".equals(uri.getScheme()), sessionDecorator);
          handler.onOpen(session);
          future.setSuccess(session);
        }
        catch (WebSocketHandshakeException e) {
          future.setFailure(e);
        }
        return;
      }

      if (msg instanceof FullHttpResponse response) {
        throw new IllegalStateException("Unexpected FullHttpResponse (getStatus=%s, content=%s)"
                .formatted(response.status(), response.content().toString(CharsetUtil.UTF_8)));
      }

      if (msg instanceof WebSocketFrame frame) {
        Message<?> message = WsNettyChannelHandler.adaptMessage(frame);
        if (message != null) {
          try {
            handler.handleMessage(session, message);
          }
          catch (Exception e) {
            tryCloseWithError(session, e, log);
          }
        }

        if (msg instanceof CloseWebSocketFrame) {
          ctx.channel().close();
        }
      }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
      if (!future.isDone()) {
        future.setFailure(cause);
      }
      else {
        try {
          handler.onError(session, cause);
        }
        catch (Exception e) {
          tryCloseWithError(session, e, log);
        }
      }
    }
  }

}
