package cn.taketoday.framework.reactive;

import cn.taketoday.web.handler.DispatcherHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

/**
 * Synchronous
 *
 * @author TODAY 2021/3/20 12:06
 */
public class SyncNettyDispatcherHandler extends DispatcherHandler implements NettyDispatcher {

  @Override
  public void dispatch(ChannelHandlerContext ctx, FullHttpRequest request) {
    // Lookup handler mapping
    final NettyRequestContext context = new NettyRequestContext(getContextPath(), ctx, request);
    try {
      handle(context);
      context.send();
    }
    catch (Throwable e) {
      ctx.fireExceptionCaught(e);
    }
  }
}
