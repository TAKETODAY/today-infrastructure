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

package infra.web.socket.server.support;

import infra.context.ApplicationContext;
import infra.core.io.buffer.NettyDataBufferFactory;
import infra.lang.Nullable;
import infra.web.server.support.NettyChannelHandler;
import infra.web.server.support.NettyRequestConfig;
import infra.web.socket.BinaryMessage;
import infra.web.socket.CloseStatus;
import infra.web.socket.Message;
import infra.web.socket.PingMessage;
import infra.web.socket.PongMessage;
import infra.web.socket.TextMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

import static infra.web.socket.handler.ExceptionWebSocketHandlerDecorator.tryCloseWithError;

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
    Channel channel = ctx.channel();
    WebSocketHolder holder = WebSocketHolder.find(channel);
    if (holder == null) {
      return;
    }
    if (frame instanceof CloseWebSocketFrame closeFrame) {
      int statusCode = closeFrame.statusCode();
      String reasonText = closeFrame.reasonText();
      CloseStatus closeStatus = new CloseStatus(statusCode, reasonText);
      close(channel, closeStatus);
      holder.unbind(channel);
      channel.close();
    }
    else {
      Message<?> message = adaptMessage(holder.allocator, frame);
      if (message != null) {
        try {
          holder.wsHandler.handleMessage(holder.session, message);
        }
        catch (Exception e) {
          tryCloseWithError(holder.session, e, log);
        }
      }
    }
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) {
    try {
      close(ctx.channel(), CloseStatus.NORMAL);
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

  private static void close(Channel channel, CloseStatus closeStatus) {
    WebSocketHolder socketHolder = WebSocketHolder.find(channel);
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
  public static Message<?> adaptMessage(NettyDataBufferFactory allocator, WebSocketFrame frame) {
    if (frame instanceof PingWebSocketFrame) {
      return new PingMessage(allocator.wrap(frame.content()));
    }
    if (frame instanceof PongWebSocketFrame) {
      return new PongMessage(allocator.wrap(frame.content()));
    }
    if (frame instanceof TextWebSocketFrame twsf) {
      String text = twsf.text();
      return new TextMessage(text, frame.isFinalFragment());
    }
    if (frame instanceof BinaryWebSocketFrame) {
      return new BinaryMessage(allocator.wrap(frame.content()), frame.isFinalFragment());
    }
    return null;
  }

}
