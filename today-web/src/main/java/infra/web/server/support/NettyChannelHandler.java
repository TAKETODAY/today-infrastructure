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

package infra.web.server.support;

import java.io.IOException;

import org.jspecify.annotations.Nullable;

import infra.beans.factory.SmartInitializingSingleton;
import infra.context.ApplicationContext;
import infra.core.env.ConfigurableEnvironment;
import infra.lang.Assert;
import infra.web.DispatcherHandler;
import infra.web.HttpStatusProvider;
import infra.web.server.ServiceExecutor;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

import static io.netty.handler.codec.http.DefaultHttpHeadersFactory.trailersFactory;

/**
 * process HTTP Requests
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-07-04 21:50
 */
public class NettyChannelHandler extends ChannelInboundHandlerAdapter {

  protected final NettyRequestConfig requestConfig;

  protected final ApplicationContext context;

  protected final DispatcherHandler dispatcherHandler;

  protected final ServiceExecutor executor;

  private HttpContext httpContext;

  protected NettyChannelHandler(NettyRequestConfig requestConfig, ApplicationContext context,
          DispatcherHandler dispatcherHandler, ServiceExecutor executor) {
    this.context = context;
    this.executor = executor;
    this.requestConfig = requestConfig;
    this.dispatcherHandler = dispatcherHandler;
  }

  @Override
  public final void channelRead(final ChannelHandlerContext ctx, final Object msg) throws IOException {
    if (msg instanceof HttpRequest request) {
      Channel channel = ctx.channel();
      HttpContext httpContext = new HttpContext(channel, request, requestConfig, context, dispatcherHandler, this);
      this.httpContext = httpContext;
      executor.execute(httpContext, httpContext);
    }
    else if (msg instanceof HttpContent content) {
      httpContext.onDataReceived(content);
    }
    else if (msg instanceof WebSocketFrame) {
      handleWebSocketFrame(ctx, (WebSocketFrame) msg);
    }
    else {
      ctx.fireChannelRead(msg);
    }
  }

  protected void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
    ctx.fireChannelRead(frame);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    handleException(ctx.channel(), cause);
  }

  void handleException(Channel channel, Throwable cause) {
    HttpResponse response = createErrorResponse(cause);
    if (response != null) {
      channel.writeAndFlush(response)
              .addListener(ChannelFutureListener.CLOSE);
    }
  }

  @Nullable
  protected HttpResponse createErrorResponse(Throwable cause) {
    var statusCode = HttpStatusProvider.getStatusCode(cause);
    return new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(statusCode.first.value()),
            Unpooled.EMPTY_BUFFER, requestConfig.httpHeadersFactory, trailersFactory());
  }

  //

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    ctx.fireChannelActive();
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) {
    if (httpContext != null) {
      httpContext.channelInactive();
    }
    ctx.fireChannelInactive();
  }

}
