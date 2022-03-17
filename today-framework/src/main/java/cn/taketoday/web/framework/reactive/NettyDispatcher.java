package cn.taketoday.web.framework.reactive;

import cn.taketoday.lang.Assert;
import cn.taketoday.web.RequestContextHolder;
import cn.taketoday.web.handler.DispatcherHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * default implementation is Synchronous Netty
 * {@link cn.taketoday.web.handler.DispatcherHandler}
 * like {@link cn.taketoday.web.servlet.DispatcherServlet}
 *
 * @author TODAY 2021/3/20 12:05
 * @see AsyncNettyDispatcherHandler
 * @see cn.taketoday.web.handler.DispatcherHandler
 * @see cn.taketoday.web.servlet.DispatcherServlet
 */
public class NettyDispatcher {
  protected final DispatcherHandler dispatcherHandler;

  public NettyDispatcher(DispatcherHandler dispatcherHandler) {
    Assert.notNull(dispatcherHandler, "DispatcherHandler must not be null");
    this.dispatcherHandler = dispatcherHandler;
  }

  /**
   * dispatch request in netty
   * <p>
   * default is using Synchronous
   * </p>
   *
   * @param ctx netty channel handler context
   * @param nettyContext netty request context
   */
  public void dispatch(final ChannelHandlerContext ctx, final NettyRequestContext nettyContext) throws Throwable {
    RequestContextHolder.set(nettyContext);
    try {
      dispatcherHandler.dispatch(nettyContext); // handling HTTP request
      nettyContext.sendIfNotCommitted();
    }
    finally {
      RequestContextHolder.remove();
    }
  }
}
