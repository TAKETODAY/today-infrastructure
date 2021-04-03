package cn.taketoday.framework.reactive;

import cn.taketoday.web.RequestContextHolder;
import cn.taketoday.web.handler.DispatcherHandler;
import io.netty.channel.ChannelHandlerContext;

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
  public void dispatch(ChannelHandlerContext ctx, final NettyRequestContext nettyContext) throws Throwable {
    RequestContextHolder.prepareContext(nettyContext);
    try {
      handle(nettyContext); // handling HTTP request
      nettyContext.sendIfNotCommitted();
    }
    finally {
      RequestContextHolder.resetContext();
    }
  }
}
