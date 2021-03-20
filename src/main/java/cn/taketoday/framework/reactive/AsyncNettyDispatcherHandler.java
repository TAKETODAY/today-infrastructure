package cn.taketoday.framework.reactive;

import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import cn.taketoday.web.handler.DispatcherHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * Async
 *
 * @author TODAY 2021/3/20 12:05
 */
public class AsyncNettyDispatcherHandler extends DispatcherHandler implements NettyDispatcher {

  @Override
  public void dispatch(ChannelHandlerContext ctx, FullHttpRequest request) {

    final NettyRequestContext nettyContext = new NettyRequestContext(getContextPath(), ctx, request);
    final Executor executor = ctx.executor();

    final class Handler implements UnaryOperator<Object> {
      @Override
      public Object apply(Object handler) {
        try {
          handle(handler, nettyContext);
        }
        catch (Throwable e) {
          ctx.fireExceptionCaught(e);
        }
        return handler;
      }
    }

    final class Sender implements Consumer<Object> {
      @Override
      public void accept(Object handler) {
        nettyContext.send();
      }
    }

    completedFuture(nettyContext)
            .thenApplyAsync(this::lookupHandler, executor)
            .thenApplyAsync(new Handler(), executor)
            .thenAcceptAsync(new Sender(), executor);
  }
}
