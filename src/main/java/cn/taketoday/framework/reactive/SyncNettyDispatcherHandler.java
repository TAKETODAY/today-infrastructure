package cn.taketoday.framework.reactive;

import cn.taketoday.web.handler.DispatcherHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

/**
 * Synchronous Netty {@link cn.taketoday.web.handler.DispatcherHandler}
 * implementation like {@link cn.taketoday.web.servlet.DispatcherServlet}
 *
 * @author TODAY 2021/3/20 12:06
 * @see AsyncNettyDispatcherHandler
 * @see cn.taketoday.web.handler.DispatcherHandler
 * @see cn.taketoday.web.servlet.DispatcherServlet
 */
public class SyncNettyDispatcherHandler extends DispatcherHandler implements NettyDispatcher {

  @Override
  public void dispatch(ChannelHandlerContext ctx, FullHttpRequest request, NettyRequestContextConfig config) {
    // Lookup handler mapping
    final NettyRequestContext context = new NettyRequestContext(getContextPath(), ctx, request, config);
    try {
      handle(context);
      context.send();
    }
    catch (Throwable e) {
      ctx.fireExceptionCaught(e);
    }
  }
}
