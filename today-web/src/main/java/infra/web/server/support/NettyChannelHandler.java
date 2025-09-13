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

import java.util.ArrayList;
import java.util.concurrent.Executor;

import infra.context.ApplicationContext;
import infra.lang.Nullable;
import infra.web.DispatcherHandler;
import infra.web.HttpStatusProvider;
import infra.web.RequestContextHolder;
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
import io.netty.handler.codec.http.LastHttpContent;
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

  protected final Executor executor;

  private HttpHandler httpHandler;

  protected NettyChannelHandler(NettyRequestConfig requestConfig, ApplicationContext context,
          DispatcherHandler dispatcherHandler, Executor executor) {
    this.context = context;
    this.executor = executor;
    this.requestConfig = requestConfig;
    this.dispatcherHandler = dispatcherHandler;
  }

  @Override
  public final void channelRead(final ChannelHandlerContext ctx, final Object msg) {
    if (msg instanceof HttpRequest request) {
      Channel channel = ctx.channel();
      HttpHandler httpHandler = new HttpHandler(channel, request);
      this.httpHandler = httpHandler;
      executor.execute(httpHandler);
    }
    else if (msg instanceof HttpContent content) {
      httpHandler.addContent(content);
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

  protected NettyRequestContext createContext(Channel channel, HttpRequest httpRequest) {
    return new NettyRequestContext(context, channel, httpRequest, requestConfig, dispatcherHandler);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    handleException(ctx.channel(), cause);
  }

  private void handleException(Channel channel, Throwable cause) {
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
    ctx.fireChannelInactive();
  }

  class HttpHandler implements Runnable {

    private final Channel channel;

    private final HttpRequest request;

    private final NettyRequestContext context;

    private final boolean multipart;

    @Nullable
    private ArrayList<HttpContent> contents;

    HttpHandler(Channel channel, HttpRequest request) {
      this.channel = channel;
      this.request = request;
      this.context = createContext(channel, request);
      this.multipart = context.isMultipart();
    }

    public void addContent(HttpContent httpContent) {
      if (httpContent instanceof LastHttpContent) {

      }
      else {
        if (contents == null) {
          contents = new ArrayList<>();
        }
        contents.add(httpContent);
      }
      context.addContent(httpContent);
    }

    @Override
    public void run() {
      RequestContextHolder.set(context);
      try {
        if (request.decoderResult().cause() != null) {
          dispatcherHandler.processDispatchResult(context, null, null, request.decoderResult().cause());
        }
        else {
          dispatcherHandler.handleRequest(context); // handling HTTP request
        }
      }
      catch (Throwable e) {
        handleException(channel, e);
      }
      finally {
        RequestContextHolder.cleanup();
      }
    }
  }

}
