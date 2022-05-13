/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.socket.BinaryMessage;
import cn.taketoday.web.socket.CloseStatus;
import cn.taketoday.web.socket.Message;
import cn.taketoday.web.socket.PingMessage;
import cn.taketoday.web.socket.PongMessage;
import cn.taketoday.web.socket.TextMessage;
import cn.taketoday.web.socket.WebSocketHandler;
import cn.taketoday.web.socket.WebSocketSession;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

/**
 * Handle websocket request and http request
 *
 * @author TODAY 2021/5/24 21:22
 * @since 1.0.1
 */
public class WebSocketReactiveChannelHandler extends ReactiveChannelHandler {

  public WebSocketReactiveChannelHandler(NettyDispatcher nettyDispatcher, WebApplicationContext context) {
    super(nettyDispatcher, context);
  }

  public WebSocketReactiveChannelHandler(
          NettyDispatcher nettyDispatcher, NettyRequestConfig contextConfig, WebApplicationContext context) {
    super(nettyDispatcher, contextConfig, context);
  }

  @Override
  protected void readExceptHttp(ChannelHandlerContext ctx, Object msg) {
    if (msg instanceof WebSocketFrame) {
      handleWebSocketFrame(ctx, (WebSocketFrame) msg);
    }
    else {
      super.readExceptHttp(ctx, msg);
    }
  }

  /**
   * Handle websocket request
   *
   * @param ctx ChannelHandlerContext
   * @param frame WebSocket Request
   */
  private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
    final Channel channel = ctx.channel();

    final WebSocketHandler socketHandler = channel.attr(NettyWebSocketHandlerAdapter.SOCKET_HANDLER_KEY).get();
    final WebSocketSession webSocketSession = channel.attr(NettyWebSocketHandlerAdapter.SOCKET_SESSION_KEY).get();
    if (frame instanceof CloseWebSocketFrame) {
      final int statusCode = ((CloseWebSocketFrame) frame).statusCode();
      final String reasonText = ((CloseWebSocketFrame) frame).reasonText();
      final CloseStatus closeStatus = new CloseStatus(statusCode, reasonText);
      socketHandler.onClose(webSocketSession, closeStatus);
      channel.writeAndFlush(frame)
              .addListener(ChannelFutureListener.CLOSE);
    }
    else {
      final Message<?> message = getMessage(frame);
      if (message != null) {
        socketHandler.handleMessage(webSocketSession, message);
      }
    }
  }

  /**
   * Adapt WebSocketFrame to {@link Message}
   *
   * @param frame WebSocketFrame
   * @return websocket message
   */
  private Message<?> getMessage(WebSocketFrame frame) {
    final ByteBuf content = frame.content();
    if (frame instanceof PingWebSocketFrame) {
      final ByteBuffer byteBuffer = content.nioBuffer();
      return new PingMessage(byteBuffer);
    }
    if (frame instanceof PongWebSocketFrame) {
      final ByteBuffer byteBuffer = content.nioBuffer();
      return new PongMessage(byteBuffer);
    }
    if (frame instanceof TextWebSocketFrame) {
      final String text = content.toString(StandardCharsets.UTF_8);
      final boolean finalFragment = frame.isFinalFragment();
      return new TextMessage(text, finalFragment);
    }
    if (frame instanceof BinaryWebSocketFrame) {
      final ByteBuffer byteBuffer = content.nioBuffer();
      final boolean finalFragment = frame.isFinalFragment();
      return new BinaryMessage(byteBuffer, finalFragment);
    }
    return null;
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    final Channel channel = ctx.channel();
    final WebSocketHandler socketHandler = channel.attr(NettyWebSocketHandlerAdapter.SOCKET_HANDLER_KEY).get();
    final WebSocketSession webSocketSession = channel.attr(NettyWebSocketHandlerAdapter.SOCKET_SESSION_KEY).get();
    if (socketHandler != null && webSocketSession != null) {
      socketHandler.onError(webSocketSession, cause);
    }
    else {
      super.exceptionCaught(ctx, cause);
    }
  }
}
