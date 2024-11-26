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

package infra.web.socket.client.support;

import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

import infra.core.Decorator;
import infra.http.HttpHeaders;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.DataSize;
import infra.util.StringUtils;
import infra.util.concurrent.Future;
import infra.util.concurrent.Promise;
import infra.web.socket.Message;
import infra.web.socket.WebSocketExtension;
import infra.web.socket.WebSocketHandler;
import infra.web.socket.WebSocketSession;
import infra.web.socket.client.AbstractWebSocketClient;
import infra.web.socket.server.support.WsNettyChannelHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFactory;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpDecoderConfig;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpObjectDecoder;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;

import static infra.util.concurrent.Future.ok;
import static infra.web.socket.client.support.PromiseAdapter.adapt;
import static infra.web.socket.handler.ExceptionWebSocketHandlerDecorator.tryCloseWithError;

/**
 * Netty websocket client
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0 2024/5/2 20:39
 */
public class NettyWebSocketClient extends AbstractWebSocketClient {

  /**
   * the maximum length of the aggregated content.
   * If the length of the aggregated content exceeds this value,
   *
   * @see HttpObjectAggregator#maxContentLength
   */
  private int maxContentLength = DataSize.ofKilobytes(64).toBytesInt();

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
   * A configuration object for specifying the behaviour
   * of {@link HttpObjectDecoder} and its subclasses.
   */
  private HttpDecoderConfig httpDecoderConfig = new HttpDecoderConfig()
          .setMaxInitialLineLength(4096)
          .setMaxHeaderSize(8192)
          .setMaxChunkSize(8192)
          .setValidateHeaders(true);

  /**
   * @see HttpClientCodec
   */
  private boolean parseHttpAfterConnectRequest = HttpClientCodec.DEFAULT_PARSE_HTTP_AFTER_CONNECT_REQUEST;

  /**
   * @see HttpClientCodec
   */
  private boolean failOnMissingResponse = HttpClientCodec.DEFAULT_FAIL_ON_MISSING_RESPONSE;

  @Nullable
  private ChannelFactory<?> channelFactory;

  @Nullable
  private NioEventLoopGroup eventLoopGroup;

  public void setFailOnMissingResponse(boolean failOnMissingResponse) {
    this.failOnMissingResponse = failOnMissingResponse;
  }

  public void setParseHttpAfterConnectRequest(boolean parseHttpAfterConnectRequest) {
    this.parseHttpAfterConnectRequest = parseHttpAfterConnectRequest;
  }

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

  /**
   * set A configuration object for specifying the behaviour
   * of {@link HttpObjectDecoder} and its subclasses.
   */
  public void setHttpDecoderConfig(HttpDecoderConfig httpDecoderConfig) {
    Assert.notNull(httpDecoderConfig, "httpDecoderConfig is required");
    this.httpDecoderConfig = httpDecoderConfig;
  }

  /**
   * {@link io.netty.channel.ChannelFactory} which is used to create {@link Channel} instances from
   * when calling {@link Bootstrap#bind()}.
   */
  public void setChannelFactory(@Nullable ChannelFactory<?> channelFactory) {
    this.channelFactory = channelFactory;
  }

  /**
   * The {@link EventLoopGroup} which is used to handle all the events for the to-be-created
   * {@link Channel}
   */
  public void setEventLoopGroup(@Nullable NioEventLoopGroup eventLoopGroup) {
    this.eventLoopGroup = eventLoopGroup;
  }

  @Override
  protected Future<WebSocketSession> doHandshakeInternal(WebSocketHandler webSocketHandler,
          HttpHeaders headers, URI uri, List<String> subProtocols, List<WebSocketExtension> extensions) {

    // Connect with V13 (RFC 6455 aka HyBi-17). You can change it to V08 or V00.
    // If you change it to V00, ping is not supported and remember to change
    // HttpResponseDecoder to WebSocketHttpResponseDecoder in the pipeline.
    WebSocketClientHandshaker handshaker = createHandshaker(uri, subProtocols, extensions, createHeaders(headers));
    MessageHandler handler = new MessageHandler(uri, webSocketHandler, handshaker);

    Bootstrap bootstrap = getBootstrap(handler);
    bootstrap.connect(uri.getHost(), uri.getPort());
    return handler.future;
  }

  /**
   * cleanup resource
   */
  @SuppressWarnings("unchecked")
  public Future<Void> shutdown() {
    if (eventLoopGroup != null) {
      return (Future<Void>) adapt(eventLoopGroup.shutdownGracefully(1, 10, TimeUnit.SECONDS));
    }
    return ok();
  }

  /**
   * @since 5.0
   */
  protected Bootstrap getBootstrap(ChannelHandler handler) {
    NioEventLoopGroup eventLoopGroup = eventLoopGroup();
    ChannelFactory<?> channelFactory = channelFactory();
    return new Bootstrap()
            .group(eventLoopGroup)
            .channelFactory(channelFactory)
            .handler(new ChannelInitializer<SocketChannel>() {

              @Override
              protected void initChannel(SocketChannel ch) {
                ch.pipeline()
                        .addLast("httpClientCodec", new HttpClientCodec(httpDecoderConfig, parseHttpAfterConnectRequest, failOnMissingResponse))
                        .addLast("httpObjectAggregator", new HttpObjectAggregator(maxContentLength, closeOnExpectationFailed))
                        .addLast("message-handler", handler);
                NettyWebSocketClient.this.initChannel(ch);
              }
            });
  }

  protected WebSocketClientHandshaker createHandshaker(URI uri, List<String> subProtocols,
          List<WebSocketExtension> extensions, io.netty.handler.codec.http.HttpHeaders customHeaders) {
    return WebSocketClientHandshakerFactory.newHandshaker(uri, WebSocketVersion.V13,
            StringUtils.collectionToCommaDelimitedString(subProtocols), true, customHeaders);
  }

  protected WebSocketSession createSession(Channel channel, boolean secure,
          @Nullable Decorator<WebSocketSession> sessionDecorator, WebSocketClientHandshaker handshaker) {
    WebSocketSession session = new NettyClientWebSocketSession(secure, channel, handshaker);

    if (sessionDecorator != null) {
      session = sessionDecorator.decorate(session);
    }
    return session;
  }

  /**
   * init websocket channel
   */
  protected void initChannel(SocketChannel ch) {

  }

  /**
   * process close frame
   */
  protected void processCloseFrame(ChannelHandlerContext ctx) {
    ctx.channel().close();
  }

  /**
   * default Channel is NioSocketChannel
   */
  private ChannelFactory<?> channelFactory() {
    ChannelFactory<?> channelFactory = this.channelFactory;
    if (channelFactory == null) {
      channelFactory = NioSocketChannel::new;
      this.channelFactory = channelFactory;
    }
    return channelFactory;
  }

  private NioEventLoopGroup eventLoopGroup() {
    NioEventLoopGroup eventLoopGroup = this.eventLoopGroup;
    if (eventLoopGroup == null) {
      eventLoopGroup = new NioEventLoopGroup(4);
      this.eventLoopGroup = eventLoopGroup;
    }
    return eventLoopGroup;
  }

  private DefaultHttpHeaders createHeaders(HttpHeaders headers) {
    DefaultHttpHeaders entries = new DefaultHttpHeaders();
    headers.forEach(entries::add);
    return entries;
  }

  final class MessageHandler extends ChannelInboundHandlerAdapter {

    private final URI uri;

    private final WebSocketHandler handler;

    private final WebSocketClientHandshaker handshaker;

    public final Promise<WebSocketSession> future = Future.forPromise();

    private WebSocketSession session;

    MessageHandler(URI uri, WebSocketHandler handler, WebSocketClientHandshaker handshaker) {
      this.uri = uri;
      this.handler = handler;
      this.handshaker = handshaker;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
      handshaker.handshake(ctx.channel());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      if (msg instanceof WebSocketFrame frame) {
        Message<?> message = WsNettyChannelHandler.adaptMessage(frame);
        if (message != null) {
          try {
            handler.handleMessage(session, message);
          }
          catch (Exception e) {
            tryCloseWithError(session, e, logger);
          }
        }

        if (msg instanceof CloseWebSocketFrame) {
          processCloseFrame(ctx);
        }
      }
      else if (msg instanceof FullHttpResponse response) {
        if (!handshaker.isHandshakeComplete()) {
          Channel channel = ctx.channel();
          try {
            handshaker.finishHandshake(channel, response);
            session = createSession(channel, "wss".equals(uri.getScheme()), sessionDecorator, handshaker);
            handler.onOpen(session);
            future.setSuccess(session);
          }
          catch (Exception e) {
            future.setFailure(e);
          }
        }
      }
      else {
        super.channelRead(ctx, msg);
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
          tryCloseWithError(session, e, logger);
        }
      }
    }
  }

}
