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

package cn.taketoday.web.server.support;

import cn.taketoday.beans.factory.SmartInitializingSingleton;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.DispatcherHandler;
import cn.taketoday.web.HttpStatusProvider;
import cn.taketoday.web.RequestContextHolder;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.ReferenceCountUtil;

import static io.netty.handler.codec.http.DefaultHttpHeadersFactory.trailersFactory;

/**
 * process HTTP Requests
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-07-04 21:50
 */
public class NettyChannelHandler extends DispatcherHandler implements ChannelInboundHandler, SmartInitializingSingleton {

  protected final NettyRequestConfig requestConfig;

  public NettyChannelHandler(NettyRequestConfig requestConfig, ApplicationContext context) {
    super(context);
    Assert.notNull(context, "ApplicationContext is required");
    Assert.notNull(requestConfig, "NettyRequestConfig is required");
    this.requestConfig = requestConfig;
  }

  @Override
  public void afterSingletonsInstantiated() {
    init();
  }

  @Override
  protected ConfigurableEnvironment createEnvironment() {
    return new StandardNettyWebEnvironment();
  }

  @Override
  public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
    if (msg instanceof FullHttpRequest request) {
      var nettyContext = createContext(ctx, request);
      RequestContextHolder.set(nettyContext);
      try {
        handleRequest(nettyContext); // handling HTTP request
      }
      catch (Throwable e) {
        exceptionCaught(ctx, e);
      }
      finally {
        RequestContextHolder.cleanup();
      }
    }
    else if (msg instanceof WebSocketFrame) {
      handleWebSocketFrame(ctx, (WebSocketFrame) msg);
      ReferenceCountUtil.safeRelease(msg);
    }
    else {
      ctx.fireChannelRead(msg);
    }
  }

  protected void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
    ctx.fireChannelRead(frame);
  }

  protected NettyRequestContext createContext(ChannelHandlerContext ctx, FullHttpRequest httpRequest) {
    return new NettyRequestContext(getApplicationContext(), ctx, httpRequest, requestConfig, this);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    HttpResponse response = createErrorResponse(ctx, cause);
    if (response != null) {
      ctx.writeAndFlush(response)
              .addListener(ChannelFutureListener.CLOSE);
    }
  }

  @Nullable
  protected HttpResponse createErrorResponse(ChannelHandlerContext ctx, Throwable cause) {
    var statusCode = HttpStatusProvider.getStatusCode(cause);
    return new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(statusCode.first.value()),
            Unpooled.EMPTY_BUFFER, requestConfig.httpHeadersFactory, trailersFactory());
  }

  //

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) {
    // no-op
  }

  @Override
  public void handlerRemoved(ChannelHandlerContext ctx) {
    // no-op
  }

  @Override
  public void channelRegistered(ChannelHandlerContext ctx) {
    ctx.fireChannelRegistered();
  }

  @Override
  public void channelUnregistered(ChannelHandlerContext ctx) {
    ctx.fireChannelUnregistered();
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    ctx.fireChannelActive();
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) {
    ctx.fireChannelInactive();
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
    ctx.fireUserEventTriggered(evt);
  }

  @Override
  public void channelWritabilityChanged(ChannelHandlerContext ctx) {
    ctx.fireChannelWritabilityChanged();
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) {
    ctx.fireChannelReadComplete();
  }

}
