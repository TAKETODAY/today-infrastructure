/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.framework.reactive;

import cn.taketoday.context.utils.Assert;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * ChannelInboundHandler
 *
 * @author TODAY 2019-07-04 21:50
 */
public class ReactiveChannelHandler implements ChannelInboundHandler {

  private NettyDispatcher nettyDispatcher;
  private NettyRequestContextConfig contextConfig;

  public ReactiveChannelHandler() {
    this(new AsyncNettyDispatcherHandler());
  }

  public ReactiveChannelHandler(NettyDispatcher nettyDispatcher) {
    this(nettyDispatcher, new NettyRequestContextConfig());
  }

  public ReactiveChannelHandler(NettyDispatcher nettyDispatcher, NettyRequestContextConfig contextConfig) {
    this.contextConfig = contextConfig;
    this.nettyDispatcher = nettyDispatcher;
  }

  public NettyDispatcher getNettyDispatcher() {
    return nettyDispatcher;
  }

  public void setNettyDispatcher(NettyDispatcher nettyDispatcher) {
    this.nettyDispatcher = nettyDispatcher;
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) {
    ctx.flush();
  }

  @Override
  public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
    if (msg instanceof FullHttpRequest) {
      nettyDispatcher.dispatch(ctx, (FullHttpRequest) msg, obtainContextConfig());
    }
    else {
      ctx.fireChannelRead(msg);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR);
    ctx.writeAndFlush(response)
            .addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
  }

  public void setContextConfig(NettyRequestContextConfig contextConfig) {
    this.contextConfig = contextConfig;
  }

  public NettyRequestContextConfig getContextConfig() {
    return contextConfig;
  }

  private NettyRequestContextConfig obtainContextConfig() {
    final NettyRequestContextConfig contextConfig = getContextConfig();
    Assert.state(contextConfig != null, "No NettyRequestContextConfig");
    return contextConfig;
  }

  //

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) {}

  @Override
  public void handlerRemoved(ChannelHandlerContext ctx) {}

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
