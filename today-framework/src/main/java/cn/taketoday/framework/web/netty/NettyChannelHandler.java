/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.framework.web.netty;

import java.nio.charset.StandardCharsets;

import cn.taketoday.beans.factory.SmartInitializingSingleton;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.web.DispatcherHandler;
import cn.taketoday.web.RequestContextHolder;
import cn.taketoday.web.socket.BinaryMessage;
import cn.taketoday.web.socket.CloseStatus;
import cn.taketoday.web.socket.Message;
import cn.taketoday.web.socket.PingMessage;
import cn.taketoday.web.socket.PongMessage;
import cn.taketoday.web.socket.TextMessage;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.ReferenceCountUtil;

import static cn.taketoday.web.socket.handler.ExceptionWebSocketHandlerDecorator.tryCloseWithError;

/**
 * ChannelInboundHandler
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-07-04 21:50
 */
public class NettyChannelHandler extends DispatcherHandler implements ChannelInboundHandler, SmartInitializingSingleton {

  private static final boolean websocketPresent = ClassUtils.isPresent(
          "cn.taketoday.web.socket.Message", NettyChannelHandler.class);

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
    if (msg instanceof FullHttpRequest httpRequest) {
      var nettyContext = createContext(ctx, httpRequest);
      RequestContextHolder.set(nettyContext);
      try {
        dispatch(nettyContext); // handling HTTP request
      }
      catch (Throwable e) {
        exceptionCaught(ctx, e);
      }
      finally {
        RequestContextHolder.cleanup();
        ReferenceCountUtil.safeRelease(httpRequest);
      }
    }
    else if (websocketPresent && msg instanceof WebSocketFrame) {
      WebSocketDelegate.handleWebSocketFrame(ctx, (WebSocketFrame) msg, logger);
      ReferenceCountUtil.safeRelease(msg);
    }
    else {
      ctx.fireChannelRead(msg);
    }
  }

  protected NettyRequestContext createContext(ChannelHandlerContext ctx, FullHttpRequest httpRequest) {
    return new NettyRequestContext(getApplicationContext(), ctx, httpRequest, requestConfig, this);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    if (!websocketPresent || !WebSocketDelegate.isErrorHandled(ctx, cause, logger)) {
      HttpResponse response = getErrorResponse(ctx, cause);
      ctx.writeAndFlush(response)
              .addListener(ChannelFutureListener.CLOSE);
    }
  }

  protected HttpResponse getErrorResponse(ChannelHandlerContext ctx, Throwable cause) {
    return new DefaultHttpResponse(HttpVersion.HTTP_1_1,
            HttpResponseStatus.INTERNAL_SERVER_ERROR, false, false);
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
    try {
      if (websocketPresent) {
        WebSocketDelegate.close(ctx, CloseStatus.NORMAL, logger);
      }
    }
    finally {
      ctx.fireChannelInactive();
    }
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

  static class WebSocketDelegate {

    static boolean isErrorHandled(ChannelHandlerContext ctx, Throwable cause, Logger logger) {
      var socketHolder = WebSocketHolder.find(ctx.channel());
      if (socketHolder != null) {
        try {
          socketHolder.wsHandler.onError(socketHolder.session, cause);
        }
        catch (Exception e) {
          tryCloseWithError(socketHolder.session, e, logger);
        }
        return true;
      }
      return false;
    }

    static void close(ChannelHandlerContext ctx, CloseStatus closeStatus, Logger logger) {
      WebSocketHolder socketHolder = WebSocketHolder.find(ctx.channel());
      if (socketHolder != null) {
        try {
          socketHolder.wsHandler.onClose(socketHolder.session, closeStatus);
        }
        catch (Exception ex) {
          logger.warn("Unhandled on-close exception for {}", socketHolder.session, ex);
        }
      }
    }

    /**
     * Handle websocket request
     *
     * @param ctx ChannelHandlerContext
     * @param frame WebSocket Request
     */
    static void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame, Logger logger) {
      WebSocketHolder socketHolder = WebSocketHolder.find(ctx.channel());
      if (socketHolder == null) {
        return;
      }
      if (frame instanceof CloseWebSocketFrame closeFrame) {
        int statusCode = closeFrame.statusCode();
        String reasonText = closeFrame.reasonText();
        CloseStatus closeStatus = new CloseStatus(statusCode, reasonText);
        close(ctx, closeStatus, logger);
        socketHolder.unbind(ctx.channel());
        ctx.close();
      }
      else {
        Message<?> message = adaptMessage(frame);
        if (message != null) {
          try {
            socketHolder.wsHandler.handleMessage(socketHolder.session, message);
          }
          catch (Exception e) {
            tryCloseWithError(socketHolder.session, e, logger);
          }
        }
      }
    }

    /**
     * Adapt WebSocketFrame to {@link Message}
     *
     * @param frame WebSocketFrame
     * @return websocket message
     */
    @Nullable
    private static Message<?> adaptMessage(WebSocketFrame frame) {
      if (frame instanceof PingWebSocketFrame) {
        return new PingMessage(frame.content().nioBuffer());
      }
      if (frame instanceof PongWebSocketFrame) {
        return new PongMessage(frame.content().nioBuffer());
      }
      if (frame instanceof TextWebSocketFrame) {
        String text = frame.content().toString(StandardCharsets.UTF_8);
        return new TextMessage(text, frame.isFinalFragment());
      }
      if (frame instanceof BinaryWebSocketFrame) {
        return new BinaryMessage(frame.content().nioBuffer(), frame.isFinalFragment());
      }
      return null;
    }

  }

}
