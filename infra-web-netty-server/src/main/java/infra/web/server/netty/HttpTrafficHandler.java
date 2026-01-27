/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.web.server.netty;

import org.jspecify.annotations.Nullable;

import java.io.IOException;

import infra.context.ApplicationContext;
import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.ClassUtils;
import infra.web.DispatcherHandler;
import infra.web.HttpStatusProvider;
import infra.web.server.ServiceExecutor;
import infra.web.socket.CloseStatus;
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
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;

import static infra.web.socket.handler.ExceptionWebSocketHandler.tryCloseWithError;
import static io.netty.handler.codec.http.DefaultHttpHeadersFactory.trailersFactory;

/**
 * Handles HTTP traffic in Netty server, processing incoming requests and managing HTTP context.
 * This handler is responsible for creating and managing {@link HttpContext} instances,
 * delegating request handling to the service executor, and managing WebSocket frames.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-07-04 21:50
 */
public class HttpTrafficHandler extends ChannelInboundHandlerAdapter {

  private static final Logger log = LoggerFactory.getLogger(HttpTrafficHandler.class);

  private static final AttributeKey<@Nullable HttpContext> KEY = AttributeKey.valueOf(HttpContext.class, "KEY");

  private static final boolean webSocketPresent = ClassUtils.isPresent("infra.web.socket.WebSocketMessage", HttpTrafficHandler.class);

  protected final NettyRequestConfig requestConfig;

  protected final ApplicationContext context;

  protected final DispatcherHandler dispatcherHandler;

  protected final ServiceExecutor executor;

  public HttpTrafficHandler(NettyRequestConfig requestConfig, ApplicationContext context,
          DispatcherHandler dispatcherHandler, ServiceExecutor executor) {
    Assert.notNull(executor, "ServiceExecutor is required");
    Assert.notNull(context, "ApplicationContext is required");
    Assert.notNull(requestConfig, "NettyRequestConfig is required");
    Assert.notNull(dispatcherHandler, "DispatcherHandler is required");
    this.context = context;
    this.executor = executor;
    this.requestConfig = requestConfig;
    this.dispatcherHandler = dispatcherHandler;
  }

  @Override
  public final boolean isSharable() {
    return true;
  }

  @Override
  public final void channelRead(final ChannelHandlerContext ctx, final Object msg) throws IOException {
    if (msg instanceof HttpRequest request) {
      Channel channel = ctx.channel();
      HttpContext httpContext = new HttpContext(channel, request, requestConfig, context, dispatcherHandler, this);
      channel.attr(KEY).set(httpContext);
      executor.execute(httpContext, httpContext);
    }
    else if (msg instanceof HttpContent content) {
      HttpContext httpContext = ctx.channel().attr(KEY).get();
      if (httpContext != null) {
        httpContext.onDataReceived(content);
      }
      else {
        content.release();
      }
    }
    else if (msg instanceof WebSocketFrame) {
      handleWebSocketFrame(ctx, (WebSocketFrame) msg);
    }
    else {
      ctx.fireChannelRead(msg);
    }
  }

  protected void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
    if (webSocketPresent) {
      Ws.handleFrame(ctx, frame);
    }
    else {
      ctx.fireChannelRead(frame);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    if (webSocketPresent) {
      Ws.exceptionCaught(this, ctx, cause);
    }
    else {
      handleException(ctx.channel(), cause);
    }
  }

  void handleException(Channel channel, Throwable cause) {
    HttpResponse response = createErrorResponse(cause);
    if (response != null) {
      channel.writeAndFlush(response)
              .addListener(ChannelFutureListener.CLOSE);
    }
  }

  protected @Nullable HttpResponse createErrorResponse(Throwable cause) {
    var statusCode = HttpStatusProvider.getStatusCode(cause);
    return new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(statusCode.first.value()),
            Unpooled.EMPTY_BUFFER, requestConfig.httpHeadersFactory, trailersFactory());
  }

  //

  @Override
  public void channelInactive(ChannelHandlerContext ctx) {
    if (webSocketPresent) {
      Ws.channelInactive(ctx);
    }

    HttpContext context = ctx.channel().attr(KEY).getAndSet(null);
    if (context != null) {
      context.channelInactive();
    }
    ctx.fireChannelInactive();
  }

  private static final class Ws {

    public static void handleFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
      WebSocketAttribute attr = WebSocketAttribute.find(ctx.channel());
      if (attr == null) {
        ReferenceCountUtil.safeRelease(frame);
        return;
      }
      if (frame instanceof CloseWebSocketFrame cf) {
        attr.close(ctx, new CloseStatus(cf.statusCode(), cf.reasonText()), log);
        ReferenceCountUtil.safeRelease(frame);
      }
      else {
        attr.session.handleMessage(attr.wsHandler, frame, log);
      }
    }

    public static void exceptionCaught(HttpTrafficHandler handler, ChannelHandlerContext ctx, Throwable cause) {
      var attr = WebSocketAttribute.find(ctx.channel());
      if (attr != null) {
        try {
          attr.wsHandler.onError(attr.session, cause);
        }
        catch (Throwable e) {
          tryCloseWithError(attr.session, e, log);
        }
      }
      else {
        handler.handleException(ctx.channel(), cause);
      }
    }

    public static void channelInactive(ChannelHandlerContext ctx) {
      WebSocketAttribute attr = WebSocketAttribute.find(ctx.channel());
      if (attr != null) {
        attr.close(ctx, CloseStatus.NO_CLOSE_FRAME, log);
      }
    }
  }

}
