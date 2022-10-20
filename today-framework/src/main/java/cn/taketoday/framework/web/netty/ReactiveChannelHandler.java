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
package cn.taketoday.framework.web.netty;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.lang.Assert;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

/**
 * ChannelInboundHandler
 *
 * @author TODAY 2019-07-04 21:50
 */
public class ReactiveChannelHandler implements ChannelInboundHandler {

  private NettyDispatcher nettyDispatcher;
  private NettyRequestConfig contextConfig;

  private final ApplicationContext context;

  public ReactiveChannelHandler(
          NettyDispatcher nettyDispatcher, NettyRequestConfig contextConfig, ApplicationContext context) {
    Assert.notNull(context, "ApplicationContext is required");
    this.context = context;
    setContextConfig(contextConfig);
    setNettyDispatcher(nettyDispatcher);
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) {
    ctx.flush();
  }

  @Override
  public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
    if (msg instanceof FullHttpRequest httpRequest) {
      var nettyContext = new NettyRequestContext(context, ctx, httpRequest, contextConfig);
      try {
        nettyDispatcher.dispatch(ctx, nettyContext);
      }
      catch (Throwable e) {
        ctx.fireExceptionCaught(e);
      }
    }
    else {
      readExceptHttp(ctx, msg);
    }
  }

  /**
   * read other msg
   */
  protected void readExceptHttp(ChannelHandlerContext ctx, Object msg) {
    ctx.fireChannelRead(msg);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    var response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR);
    ctx.writeAndFlush(response)
            .addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
  }

  public void setContextConfig(NettyRequestConfig contextConfig) {
    Assert.notNull(contextConfig, "NettyRequestContextConfig cannot be null");
    this.contextConfig = contextConfig;
  }

  public void setNettyDispatcher(NettyDispatcher nettyDispatcher) {
    Assert.notNull(nettyDispatcher, "NettyDispatcher cannot be null");
    this.nettyDispatcher = nettyDispatcher;
  }

  public NettyDispatcher getNettyDispatcher() {
    return nettyDispatcher;
  }

  /**
   * Get Netty RequestContext config
   */
  public NettyRequestConfig getContextConfig() {
    return contextConfig;
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

}
