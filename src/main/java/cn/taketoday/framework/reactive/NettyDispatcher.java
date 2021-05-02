package cn.taketoday.framework.reactive;

import cn.taketoday.context.utils.Assert;
import cn.taketoday.web.handler.DispatcherHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author TODAY 2021/3/20 12:05
 */
public abstract class NettyDispatcher {
  protected final DispatcherHandler dispatcherHandler;

  protected NettyDispatcher(DispatcherHandler dispatcherHandler) {
    Assert.notNull(dispatcherHandler, "DispatcherHandler must not be null");
    this.dispatcherHandler = dispatcherHandler;
  }

  /**
   * dispatch request in netty
   *
   * @param ctx
   *         netty channel handler context
   * @param nettyContext
   *         netty request context
   */
  public abstract void dispatch(
          ChannelHandlerContext ctx, NettyRequestContext nettyContext) throws Throwable;
}
