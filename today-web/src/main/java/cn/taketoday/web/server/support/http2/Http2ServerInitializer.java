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

package cn.taketoday.web.server.support.http2;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http.HttpServerUpgradeHandler.UpgradeCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler.UpgradeCodecFactory;
import io.netty.handler.codec.http2.CleartextHttp2ServerUpgradeHandler;
import io.netty.handler.codec.http2.Http2CodecUtil;
import io.netty.handler.codec.http2.Http2ServerUpgradeCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.util.AsciiString;
import io.netty.util.ReferenceCountUtil;

import static io.netty.util.internal.ObjectUtil.checkPositiveOrZero;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2024/9/30 21:58
 */
public class Http2ServerInitializer extends ChannelInitializer<SocketChannel> implements ChannelHandler {

  private static final UpgradeCodecFactory upgradeCodecFactory = new UpgradeCodecFactory() {

    @Override
    public UpgradeCodec newUpgradeCodec(CharSequence protocol) {
      if (AsciiString.contentEquals(Http2CodecUtil.HTTP_UPGRADE_PROTOCOL_NAME, protocol)) {
        return new Http2ServerUpgradeCodec(new HelloWorldHttp2HandlerBuilder().build());
      }
      return null;
    }
  };

  private final SslContext sslCtx;

  private final int maxHttpContentLength;

  public Http2ServerInitializer(SslContext sslCtx) {
    this(sslCtx, 16 * 1024);
  }

  public Http2ServerInitializer(SslContext sslCtx, int maxHttpContentLength) {
    this.sslCtx = sslCtx;
    this.maxHttpContentLength = checkPositiveOrZero(maxHttpContentLength, "maxHttpContentLength");
  }

  @Override
  public void initChannel(SocketChannel ch) {
    if (sslCtx != null) {
      configureSsl(ch);
    }
    else {
      configureClearText(ch);
    }
  }

  /**
   * Configure the pipeline for TLS NPN negotiation to HTTP/2.
   */
  private void configureSsl(SocketChannel ch) {
    ch.pipeline().addLast(sslCtx.newHandler(ch.alloc()), new Http2OrHttpHandler());
  }

  /**
   * Configure the pipeline for a cleartext upgrade from HTTP to HTTP/2.0
   */
  private void configureClearText(SocketChannel ch) {
    final ChannelPipeline p = ch.pipeline();
    final HttpServerCodec sourceCodec = new HttpServerCodec();
    final HttpServerUpgradeHandler upgradeHandler = new HttpServerUpgradeHandler(sourceCodec, upgradeCodecFactory);
    final CleartextHttp2ServerUpgradeHandler cleartextHttp2ServerUpgradeHandler =
            new CleartextHttp2ServerUpgradeHandler(sourceCodec, upgradeHandler,
                    new HelloWorldHttp2HandlerBuilder().build());

    p.addLast(cleartextHttp2ServerUpgradeHandler);
    p.addLast(new SimpleChannelInboundHandler<HttpMessage>() {
      @Override
      protected void channelRead0(ChannelHandlerContext ctx, HttpMessage msg) throws Exception {
        // If this handler is hit then no upgrade has been attempted and the client is just talking HTTP.
        System.err.println("Directly talking: " + msg.protocolVersion() + " (no upgrade was attempted)");
        ChannelPipeline pipeline = ctx.pipeline();
        pipeline.addAfter(ctx.name(), null, new HelloWorldHttp1Handler("Direct. No Upgrade Attempted."));
        pipeline.replace(this, null, new HttpObjectAggregator(maxHttpContentLength));
        ctx.fireChannelRead(ReferenceCountUtil.retain(msg));
      }
    });

    p.addLast(new UserEventLogger());
  }

  /**
   * Class that logs any User Events triggered on this channel.
   */
  private static class UserEventLogger extends ChannelInboundHandlerAdapter {
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
      System.out.println("User Event Triggered: " + evt);
      ctx.fireUserEventTriggered(evt);
    }
  }

}
