package cn.taketoday.framework.reactive;

import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import cn.taketoday.web.handler.DispatcherHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

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
  public void dispatch(
          final ChannelHandlerContext ctx, final FullHttpRequest request, final NettyRequestContextConfig config) {

    final NettyRequestContext nettyContext = new NettyRequestContext(getContextPath(), ctx, request, config);
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
