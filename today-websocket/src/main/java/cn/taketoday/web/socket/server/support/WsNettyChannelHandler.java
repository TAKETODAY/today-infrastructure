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

package cn.taketoday.web.socket.server.support;

import java.nio.charset.StandardCharsets;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.server.support.NettyChannelHandler;
import cn.taketoday.web.server.support.NettyRequestConfig;
import cn.taketoday.web.socket.BinaryMessage;
import cn.taketoday.web.socket.CloseStatus;
import cn.taketoday.web.socket.Message;
import cn.taketoday.web.socket.PingMessage;
import cn.taketoday.web.socket.PongMessage;
import cn.taketoday.web.socket.TextMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

import static cn.taketoday.web.socket.handler.ExceptionWebSocketHandlerDecorator.tryCloseWithError;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/4/28 15:02
 */
public class WsNettyChannelHandler extends NettyChannelHandler {

  public WsNettyChannelHandler(NettyRequestConfig requestConfig, ApplicationContext context) {
    super(requestConfig, context);
  }

  /**
   * Handle websocket request
   *
   * @param ctx ChannelHandlerContext
   * @param frame WebSocket Request
   */
  @Override
  protected void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
    WebSocketHolder socketHolder = WebSocketHolder.find(ctx.channel());
    if (socketHolder == null) {
      return;
    }
    if (frame instanceof CloseWebSocketFrame closeFrame) {
      int statusCode = closeFrame.statusCode();
      String reasonText = closeFrame.reasonText();
      CloseStatus closeStatus = new CloseStatus(statusCode, reasonText);
      close(ctx, closeStatus);
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
          tryCloseWithError(socketHolder.session, e, log);
        }
      }
    }
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) {
    try {
      close(ctx, CloseStatus.NORMAL);
    }
    finally {
      ctx.fireChannelInactive();
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    var socketHolder = WebSocketHolder.find(ctx.channel());
    if (socketHolder != null) {
      try {
        socketHolder.wsHandler.onError(socketHolder.session, cause);
      }
      catch (Exception e) {
        tryCloseWithError(socketHolder.session, e, log);
      }
    }
    else {
      super.exceptionCaught(ctx, cause);
    }
  }

  private static void close(ChannelHandlerContext ctx, CloseStatus closeStatus) {
    WebSocketHolder socketHolder = WebSocketHolder.find(ctx.channel());
    if (socketHolder != null) {
      try {
        socketHolder.wsHandler.onClose(socketHolder.session, closeStatus);
      }
      catch (Exception ex) {
        log.warn("Unhandled on-close exception for {}", socketHolder.session, ex);
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
  public static Message<?> adaptMessage(WebSocketFrame frame) {
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
