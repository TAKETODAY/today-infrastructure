package cn.taketoday.web.framework.reactive;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import cn.taketoday.web.RequestContextHolder;
import cn.taketoday.web.handler.DispatcherHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * Async Netty {@link cn.taketoday.web.handler.DispatcherHandler}
 * implementation like {@link cn.taketoday.web.servlet.DispatcherServlet}
 *
 * @author TODAY 2021/3/20 12:05
 * @see cn.taketoday.web.handler.DispatcherHandler
 * @see cn.taketoday.web.servlet.DispatcherServlet
 */
public final class AsyncNettyDispatcherHandler extends NettyDispatcher {

  public AsyncNettyDispatcherHandler(DispatcherHandler dispatcherHandler) {
    super(dispatcherHandler);
  }

  @Override
  public void dispatch(final ChannelHandlerContext ctx, final NettyRequestContext nettyContext) {
    final class AsyncHandler implements UnaryOperator<Object> {
      @Override
      public Object apply(final Object handler) {
        RequestContextHolder.set(nettyContext);
        try {
          dispatcherHandler.handle(handler, nettyContext);
        }
        catch (Throwable e) {
          ctx.fireExceptionCaught(e);
        }
        finally {
          RequestContextHolder.remove();
        }
        return handler;
      }
    }

    final class AsyncSender implements Consumer<Object> {
      @Override
      public void accept(final Object handler) {
        nettyContext.sendIfNotCommitted();
      }
    }

    final class HandlerDetector implements Function<NettyRequestContext, Object> {
      @Override
      public Object apply(NettyRequestContext path) {
        return dispatcherHandler.lookupHandler(path);
      }
    }

    Executor executor = ctx.executor();
    CompletableFuture.completedFuture(nettyContext)
            .thenApplyAsync(new HandlerDetector(), executor)
            .thenApplyAsync(new AsyncHandler(), executor)
            .thenAcceptAsync(new AsyncSender(), executor);
  }

}
