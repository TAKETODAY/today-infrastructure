/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.framework.web.netty;

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
