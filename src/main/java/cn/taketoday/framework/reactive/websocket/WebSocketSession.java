package cn.taketoday.framework.reactive.websocket;

import cn.taketoday.web.session.DefaultSession;
import cn.taketoday.web.session.WebSession;
import cn.taketoday.web.session.WebSessionStorage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author WangYi
 * @since 2020/8/13
 */
public class WebSocketSession extends DefaultSession implements WebSession {
  private final Channel channel;
  private final ChannelHandlerContext ctx;

  WebSocketSession(String id, WebSessionStorage storage, ChannelHandlerContext ctx) {
    super(id, storage);
    this.ctx = ctx;
    this.channel = ctx.channel();
  }

}
