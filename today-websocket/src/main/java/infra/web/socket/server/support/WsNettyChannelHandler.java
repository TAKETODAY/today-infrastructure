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

package infra.web.socket.server.support;

import infra.context.ApplicationContext;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.web.DispatcherHandler;
import infra.web.server.ServiceExecutor;
import infra.web.server.support.NettyChannelHandler;
import infra.web.server.support.NettyRequestConfig;
import infra.web.socket.CloseStatus;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

import static infra.web.socket.handler.ExceptionWebSocketHandlerDecorator.tryCloseWithError;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/4/28 15:02
 */
final class WsNettyChannelHandler extends NettyChannelHandler {

  private static final Logger log = LoggerFactory.getLogger(WsNettyChannelHandler.class);

  WsNettyChannelHandler(NettyRequestConfig requestConfig, ApplicationContext context,
          DispatcherHandler dispatcherHandler, ServiceExecutor executor) {
    super(requestConfig, context, dispatcherHandler, executor);
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
    WebSocketAttribute attr = WebSocketAttribute.find(channel);
    if (attr == null) {
      return;
    }
    if (frame instanceof CloseWebSocketFrame cf) {
      CloseStatus closeStatus = new CloseStatus(cf.statusCode(), cf.reasonText());
      onClose(channel, closeStatus);
    }
    else {
      attr.session.handleMessage(attr.wsHandler, frame, log);
    }
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) {
    try {
      onClose(ctx.channel(), CloseStatus.NO_CLOSE_FRAME);
    }
    finally {
      ctx.fireChannelInactive();
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
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
      super.exceptionCaught(ctx, cause);
    }
  }

  private static void onClose(Channel channel, CloseStatus closeStatus) {
    WebSocketAttribute attr = WebSocketAttribute.find(channel);
    if (attr != null) {
      attr.unbind(channel);
      attr.session.onClose(attr.wsHandler, closeStatus, log);
    }
  }

}
