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

import org.reactivestreams.Subscription;

import cn.taketoday.http.server.reactive.HttpHandler;
import cn.taketoday.http.server.reactive.ServerHttpRequest;
import cn.taketoday.http.server.reactive.ServerHttpResponse;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.HandlerAdapter;
import cn.taketoday.web.HttpRequestHandler;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.DispatcherHandler;
import cn.taketoday.web.handler.HandlerNotFoundException;
import io.netty.channel.ChannelHandlerContext;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Reactor Netty Dispatcher
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/29 22:45
 */
public class ReactorNettyDispatcher extends NettyDispatcher implements HttpHandler {

  public ReactorNettyDispatcher(DispatcherHandler dispatcherHandler) {
    super(dispatcherHandler);
  }

  @Override
  public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
    return null;
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
  @Override
  public void dispatch(final ChannelHandlerContext ctx, final NettyRequestContext nettyContext) throws Throwable {
    Mono.fromCallable(() -> dispatcherHandler.lookupHandler(nettyContext))
            .switchIfEmpty(noHandlerFound(nettyContext))
            .flatMap(handler -> invokeHandler(nettyContext, handler))
            .flatMap(result -> handleReturnValue(nettyContext, result.handler, result.returnValue))
            .contextWrite(context -> context.put(RequestContext.class, nettyContext))
            .publishOn(Schedulers.fromExecutor(ctx.executor()))
            .subscribe(new NettySubscriber(nettyContext));
  }

  private Mono<Void> handleReturnValue(NettyRequestContext nettyContext,
          Object handler, @Nullable Object returnValue) {
    return Mono.defer(() -> {
      if (returnValue != HttpRequestHandler.NONE_RETURN_VALUE) {
        try {
          dispatcherHandler.lookupReturnValueHandler(handler, returnValue)
                  .handleReturnValue(nettyContext, handler, returnValue);
        }
        catch (Exception e) {
          return Mono.error(e);
        }
      }
      return Mono.<Void>empty();
    }).onErrorResume(ex -> {
      try {
        dispatcherHandler.handleException(handler, ex, nettyContext);
      }
      catch (Throwable e) {
        return Mono.error(e);
      }
      return Mono.empty();
    });
  }

  private Mono<HandlerResult> invokeHandler(NettyRequestContext nettyContext, Object handler) {
    try {
      HandlerAdapter adapter = dispatcherHandler.lookupHandlerAdapter(handler);
      return Mono.just(new HandlerResult(handler, adapter.handle(nettyContext, handler)));
    }
    catch (Throwable e) {
      return Mono.error(e);
    }
  }

  /**
   * No handler found &rarr; set appropriate HTTP response status.
   *
   * @param request current HTTP request
   */
  protected Mono<Void> noHandlerFound(RequestContext request) {
    return Mono.defer(() -> Mono.error(
            new HandlerNotFoundException(
                    request.getMethodValue(), request.getRequestURI(), request.requestHeaders()))
    );
  }

  static class HandlerResult {
    public final Object handler;

    @Nullable
    public final Object returnValue;

    HandlerResult(Object handler, @Nullable Object returnValue) {
      this.handler = handler;
      this.returnValue = returnValue;
    }
  }

  static class NettySubscriber implements CoreSubscriber<Void> {

    private final NettyRequestContext nettyContext;
    private Subscription subscription;

    public NettySubscriber(NettyRequestContext nettyContext) {
      this.nettyContext = nettyContext;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
      this.subscription = subscription;
    }

    @Override
    public void onNext(Void unused) {
      subscription.request(1);
    }

    @Override
    public void onError(Throwable throwable) {
      nettyContext.getChannelContext().fireExceptionCaught(throwable);
    }

    @Override
    public void onComplete() {
      nettyContext.sendIfNotCommitted();
    }

  }
}
