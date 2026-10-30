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
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.Assert;
import infra.util.Feature;
import infra.web.DispatcherHandler;
import infra.web.HttpStatusProvider;
import infra.web.server.HttpRequestExecutor;
import infra.web.socket.CloseStatus;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.codec.http2.Http2ResetFrame;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.handler.codec.http2.Http2StreamFrame;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;

import static infra.util.FeatureDetector.isPresent;
import static infra.web.socket.handler.ExceptionWebSocketHandler.tryCloseWithError;

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

  private static final boolean webSocketPresent = isPresent(Feature.WEBSOCKET);

  protected final NettyRequestConfig requestConfig;

  protected final ApplicationContext context;

  protected final DispatcherHandler dispatcherHandler;

  protected final HttpRequestExecutor executor;

  public HttpTrafficHandler(NettyRequestConfig requestConfig, ApplicationContext context,
          DispatcherHandler dispatcherHandler, HttpRequestExecutor executor) {
    Assert.notNull(executor, "HttpRequestExecutor is required");
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
      HttpContext httpContext = new HttpContext(channel, request, protocol(channel), requestConfig,
              context, dispatcherHandler, this);
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
    else if (msg instanceof Http2StreamFrame frame) {
      handleHttp2StreamFrame(ctx, frame);
    }
    else if (msg instanceof WebSocketFrame frame) {
      handleWebSocketFrame(ctx, frame);
    }
    else {
      ctx.fireChannelRead(msg);
    }
  }

  private void handleHttp2StreamFrame(ChannelHandlerContext ctx, Http2StreamFrame frame) throws IOException {
    if (frame instanceof Http2HeadersFrame headersFrame) {
      handleHttp2Headers(ctx, headersFrame);
    }
    else if (frame instanceof Http2DataFrame dataFrame) {
      HttpContext httpContext = ctx.channel().attr(KEY).get();
      if (httpContext != null) {
        httpContext.onDataReceived(dataFrame.content(), dataFrame.isEndStream());
      }
      else {
        dataFrame.release();
      }
    }
    else if (frame instanceof Http2ResetFrame resetFrame) {
      HttpContext httpContext = ctx.channel().attr(KEY).getAndSet(null);
      if (httpContext != null) {
        httpContext.onError(new IOException("HTTP/2 stream reset: " + resetFrame.errorCode()));
      }
    }
    else {
      ctx.fireChannelRead(frame);
    }
  }

  private void handleHttp2Headers(ChannelHandlerContext ctx, Http2HeadersFrame frame) throws IOException {
    Http2Headers headers = frame.headers();
    if (headers.method() == null) {
      // trailing headers of an already started request
      HttpContext httpContext = ctx.channel().attr(KEY).get();
      if (httpContext != null) {
        httpContext.onDataReceived(Unpooled.EMPTY_BUFFER, true);
      }
      return;
    }

    Channel channel = ctx.channel();
    int streamId = channel instanceof Http2StreamChannel streamChannel ? streamChannel.stream().id() : -1;
    HttpContext httpContext = new HttpContext(channel, new Http2NettyRequest(headers, streamId), protocol(channel),
            requestConfig, context, dispatcherHandler, this);
    channel.attr(KEY).set(httpContext);
    executor.execute(httpContext, httpContext);
    if (frame.isEndStream()) {
      httpContext.onDataReceived(Unpooled.EMPTY_BUFFER, true);
    }
  }

  private static HttpProtocol protocol(Channel channel) {
    HttpProtocol protocol = channel.attr(HttpChannelInitializer.PROTOCOL).get();
    return protocol != null ? protocol : HttpProtocol.HTTP_1_1;
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
    var statusCode = HttpStatusProvider.getStatusCode(cause);
    NettyResponse.of(channel, protocol(channel))
            .writeError(HttpResponseStatus.valueOf(statusCode.first.value()), true);
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
        attr.close(ctx, CloseStatus.create(cf.statusCode(), cf.reasonText()), log);
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
