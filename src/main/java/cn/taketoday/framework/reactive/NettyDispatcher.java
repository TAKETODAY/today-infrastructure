package cn.taketoday.framework.reactive;

import io.netty.channel.ChannelHandlerContext;

/**
 * @author TODAY 2021/3/20 12:05
 */
public interface NettyDispatcher {

  void dispatch(ChannelHandlerContext ctx, NettyRequestContext nettyContext) throws Throwable;
}
