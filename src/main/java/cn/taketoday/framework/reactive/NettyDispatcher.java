package cn.taketoday.framework.reactive;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

/**
 * @author TODAY 2021/3/20 12:05
 */
public interface NettyDispatcher {
  
  void dispatch(ChannelHandlerContext ctx, FullHttpRequest request, NettyRequestContextConfig config)
          throws Exception;
}
