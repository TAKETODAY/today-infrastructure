package cn.taketoday.framework.reactive;

import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import cn.taketoday.web.handler.DispatcherHandler;
import io.netty.channel.ChannelHandlerContext;

import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * Async Netty {@link cn.taketoday.web.handler.DispatcherHandler}
 * implementation like {@link cn.taketoday.web.servlet.DispatcherServlet}
 *
 * @author TODAY 2021/3/20 12:05
 * @see SyncNettyDispatcherHandler
 * @see cn.taketoday.web.handler.DispatcherHandler
 * @see cn.taketoday.web.servlet.DispatcherServlet
 */
public class AsyncNettyDispatcherHandler extends DispatcherHandler implements NettyDispatcher {

  @Override
  public void dispatch(final ChannelHandlerContext ctx, final NettyRequestContext nettyContext) {
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

    final Executor executor = ctx.executor();
    completedFuture(nettyContext)
            .thenApplyAsync(this::lookupHandler, executor)
            .thenApplyAsync(new Handler(), executor)
            .thenAcceptAsync(new Sender(), executor);
  }

}
