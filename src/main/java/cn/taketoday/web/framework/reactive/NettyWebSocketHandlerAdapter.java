package cn.taketoday.web.framework.reactive;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Constant;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.socket.AbstractWebSocketHandlerAdapter;
import cn.taketoday.web.socket.WebSocketHandler;
import cn.taketoday.web.socket.WebSocketSession;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.AttributeKey;

/**
 * The Netty websocket handler adapter
 *
 * @author TODAY 2021/5/24 21:02
 * @since 1.0.1
 */
public class NettyWebSocketHandlerAdapter extends AbstractWebSocketHandlerAdapter {
  public static final AttributeKey<WebSocketHandler> SOCKET_HANDLER_KEY = AttributeKey.valueOf("WebSocketHandler");
  public static final AttributeKey<WebSocketSession> SOCKET_SESSION_KEY = AttributeKey.valueOf("WebSocketSession");

  @Override
  protected WebSocketSession createSession(RequestContext context, WebSocketHandler handler) {
    if (!(context instanceof final NettyRequestContext nettyContext)) {
      throw new IllegalStateException("not running in netty");
    }
    final ChannelHandlerContext channelContext = nettyContext.getChannelContext();
    final String scheme = nettyContext.getScheme();
    return new NettyWebSocketSession(
            Constant.HTTPS.equals(scheme) || "wss".equals(scheme), channelContext);
  }

  @Override
  protected void doHandshake(RequestContext context, WebSocketSession session, WebSocketHandler handler) {
    final NettyRequestContext nettyContext = (NettyRequestContext) context; // just cast
    final FullHttpRequest request = nettyContext.nativeRequest();
    final ChannelHandlerContext channelContext = nettyContext.getChannelContext();
    final WebSocketServerHandshakerFactory wsFactory
            = new WebSocketServerHandshakerFactory(request.uri(), null, true); // TODO subprotocols
    final WebSocketServerHandshaker handShaker = wsFactory.newHandshaker(request);
    final Channel channel = channelContext.channel();
    if (handShaker == null) {
      WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(channel);
    }
    else {
      channel.attr(SOCKET_HANDLER_KEY).set(handler);
      channel.attr(SOCKET_SESSION_KEY).set(session);
      final HttpHeaders responseHeaders = nettyContext.responseHeaders();
      handShaker.handshake(channel, request, ((NettyHttpHeaders) responseHeaders).headers, channel.newPromise());
    }
  }
}
