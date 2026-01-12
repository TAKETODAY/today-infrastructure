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

import infra.context.ApplicationContext;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.web.DispatcherHandler;
import infra.web.server.ServiceExecutor;
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
public final class WsNettyChannelHandler extends NettyChannelHandler {

  private static final Logger log = LoggerFactory.getLogger(WsNettyChannelHandler.class);

  public WsNettyChannelHandler(NettyRequestConfig requestConfig, ApplicationContext context,
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
      super.channelInactive(ctx);
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
