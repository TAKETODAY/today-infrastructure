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

import java.util.Map;

import infra.context.ApplicationContext;
import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.NettyDataBufferFactory;
import infra.lang.Assert;
import infra.web.server.support.NettyChannelHandler;
import infra.web.server.support.NettyRequestConfig;
import infra.web.socket.CloseStatus;
import infra.web.socket.WebSocketMessage;
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

  private static final Map<Class<?>, WebSocketMessage.Type> messageTypes = Map.of(
          TextWebSocketFrame.class, infra.web.socket.WebSocketMessage.Type.TEXT,
          PingWebSocketFrame.class, WebSocketMessage.Type.PING,
          PongWebSocketFrame.class, WebSocketMessage.Type.PONG,
          BinaryWebSocketFrame.class, WebSocketMessage.Type.BINARY);

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
      channel.close();
    }
    else {
      try {
        var message = adaptMessage(holder.allocator, frame);
        holder.wsHandler.handleMessage(holder.session, message);
      }
      catch (Throwable e) {
        tryCloseWithError(holder.session, e, log);
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
      catch (Throwable e) {
        tryCloseWithError(socketHolder.session, e, log);
      }
    }
    else {
      super.exceptionCaught(ctx, cause);
    }
  }

  private static void close(Channel channel, CloseStatus closeStatus) {
    WebSocketHolder holder = WebSocketHolder.find(channel);
    if (holder != null) {
      try {
        holder.unbind(channel);
        holder.wsHandler.onClose(holder.session, closeStatus);
      }
      catch (Throwable ex) {
        log.warn("Unhandled on-close exception for {}", holder.session, ex);
      }
    }
  }

  /**
   * Adapt WebSocketFrame to {@link WebSocketMessage}
   *
   * @param frame WebSocketFrame
   * @return websocket message
   * @since 5.0
   */
  public static WebSocketMessage adaptMessage(NettyDataBufferFactory allocator, WebSocketFrame frame) {
    WebSocketMessage.Type messageType = messageTypes.get(frame.getClass());
    Assert.state(messageType != null, "Unexpected message type");
    DataBuffer payload = allocator.wrap(frame.content());
    return new WebSocketMessage(messageType, payload, frame, frame.isFinalFragment());
  }

}
