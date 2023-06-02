/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.web.RequestContextHolder;
import cn.taketoday.web.handler.DispatcherHandler;
import cn.taketoday.web.socket.BinaryMessage;
import cn.taketoday.web.socket.CloseStatus;
import cn.taketoday.web.socket.Message;
import cn.taketoday.web.socket.PingMessage;
import cn.taketoday.web.socket.PongMessage;
import cn.taketoday.web.socket.TextMessage;
import cn.taketoday.web.socket.WebSocketHandler;
import cn.taketoday.web.socket.WebSocketSession;
import cn.taketoday.web.socket.handler.ExceptionWebSocketHandlerDecorator;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
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

/**
 * ChannelInboundHandler
 *
 * @author TODAY 2019-07-04 21:50
 */
public class NettyChannelHandler extends DispatcherHandler implements ChannelInboundHandler {
  private static final boolean websocketPresent = ClassUtils.isPresent(
          "cn.taketoday.web.socket.Message", NettyChannelHandler.class.getClassLoader());

  protected final NettyRequestConfig requestConfig;

  public NettyChannelHandler(NettyRequestConfig requestConfig, ApplicationContext context) {
    super(context);
    Assert.notNull(context, "ApplicationContext is required");
    Assert.notNull(requestConfig, "NettyRequestConfig is required");
    this.requestConfig = requestConfig;
    init();
  }

  @Override
  protected ConfigurableEnvironment createEnvironment() {
    return new StandardNettyWebEnvironment();
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) {
    ctx.flush();
  }

  @Override
  public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
    if (msg instanceof FullHttpRequest httpRequest) {
      var nettyContext = createContext(ctx, httpRequest);
      RequestContextHolder.set(nettyContext);
      try {
        nettyContext.setAttribute(DispatcherHandler.BEAN_NAME, this);
        dispatch(nettyContext); // handling HTTP request
      }
      catch (Throwable e) {
        exceptionCaught(ctx, e);
      }
      finally {
        RequestContextHolder.remove();
        ReferenceCountUtil.safeRelease(httpRequest);
      }
    }
    else {
      if (websocketPresent && msg instanceof WebSocketFrame) {
        WebSocketDelegate.handleWebSocketFrame(ctx, (WebSocketFrame) msg, log);
      }
      else {
        ctx.fireChannelRead(msg);
      }
    }
  }

  protected NettyRequestContext createContext(ChannelHandlerContext ctx, FullHttpRequest httpRequest) {
    return new NettyRequestContext(getApplicationContext(), ctx, httpRequest, requestConfig);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    if (!websocketPresent || !WebSocketDelegate.isErrorHandled(ctx, cause, log)) {
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

  static class WebSocketDelegate {

    static boolean isErrorHandled(ChannelHandlerContext ctx, Throwable cause, Logger logger) {
      Channel channel = ctx.channel();
      var socketHandler = channel.attr(NettyRequestUpgradeStrategy.SOCKET_HANDLER_KEY).get();
      var webSocketSession = channel.attr(NettyRequestUpgradeStrategy.SOCKET_SESSION_KEY).get();
      if (socketHandler != null && webSocketSession != null) {
        try {
          socketHandler.onError(webSocketSession, cause);
        }
        catch (Exception e) {
          ExceptionWebSocketHandlerDecorator.tryCloseWithError(webSocketSession, e, logger);
        }
        return true;
      }
      return false;
    }

    /**
     * Handle websocket request
     *
     * @param ctx ChannelHandlerContext
     * @param frame WebSocket Request
     */
    static void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame, Logger logger) {
      Channel channel = ctx.channel();

      WebSocketHandler socketHandler = channel.attr(NettyRequestUpgradeStrategy.SOCKET_HANDLER_KEY).get();
      WebSocketSession webSocketSession = channel.attr(NettyRequestUpgradeStrategy.SOCKET_SESSION_KEY).get();
      if (frame instanceof CloseWebSocketFrame closeFrame) {
        int statusCode = closeFrame.statusCode();
        String reasonText = closeFrame.reasonText();
        CloseStatus closeStatus = new CloseStatus(statusCode, reasonText);
        try {
          socketHandler.onClose(webSocketSession, closeStatus);
        }
        catch (Exception ex) {
          logger.warn("Unhandled on-close exception for {}", webSocketSession, ex);
        }
        channel.writeAndFlush(frame)
                .addListener(ChannelFutureListener.CLOSE);
      }
      else {
        Message<?> message = getMessage(frame);
        if (message != null) {
          try {
            socketHandler.handleMessage(webSocketSession, message);
          }
          catch (Exception e) {
            ExceptionWebSocketHandlerDecorator.tryCloseWithError(webSocketSession, e, logger);
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
    private static Message<?> getMessage(WebSocketFrame frame) {
      ByteBuf content = frame.content();
      if (frame instanceof PingWebSocketFrame) {
        ByteBuffer byteBuffer = content.nioBuffer();
        return new PingMessage(byteBuffer);
      }
      if (frame instanceof PongWebSocketFrame) {
        ByteBuffer byteBuffer = content.nioBuffer();
        return new PongMessage(byteBuffer);
      }
      if (frame instanceof TextWebSocketFrame) {
        String text = content.toString(StandardCharsets.UTF_8);
        boolean finalFragment = frame.isFinalFragment();
        return new TextMessage(text, finalFragment);
      }
      if (frame instanceof BinaryWebSocketFrame) {
        ByteBuffer byteBuffer = content.nioBuffer();
        boolean finalFragment = frame.isFinalFragment();
        return new BinaryMessage(byteBuffer, finalFragment);
      }
      return null;
    }

  }

}
