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

package cn.taketoday.framework.reactive;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.function.Function;

import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextHolder;
import cn.taketoday.web.handler.DispatcherHandler;
import cn.taketoday.web.handler.HandlerAdapter;
import cn.taketoday.web.handler.NoHandlerFoundException;
import io.netty.channel.ChannelHandlerContext;
import reactor.core.publisher.Mono;

/**
 * Reactor Netty Dispatcher
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/29 22:45
 */
public class ReactorNettyDispatcher extends NettyDispatcher {

  public ReactorNettyDispatcher(DispatcherHandler dispatcherHandler) {
    super(dispatcherHandler);
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
    RequestContextHolder.set(nettyContext);
    try {
      doDispatch(nettyContext);
    }
    finally {
      RequestContextHolder.remove();
    }
  }

  private void doDispatch(NettyRequestContext nettyContext) {
    Mono.fromCallable(() -> dispatcherHandler.lookupHandler(nettyContext))
            .switchIfEmpty(noHandlerFound(nettyContext))
            .flatMap(handler -> Mono.zip(invokeHandler(nettyContext, handler), Mono.just(handler)))
            .flatMap(tuple -> {
              Object handler = tuple.getT2();
              Object returnValue = tuple.getT1();
              return handleReturnValue(nettyContext, handler, returnValue);
            })
            .doOnNext(v -> nettyContext.sendIfNotCommitted())
            .onErrorResume(new Function<Throwable, Mono<? extends Void>>() {
              @Override
              public Mono<? extends Void> apply(Throwable throwable) {
//                dispatcherHandler.handleException();
                return null;
              }
            })
            .subscribe(new Subscriber<>() {
              @Override
              public void onSubscribe(Subscription s) {

              }

              @Override
              public void onNext(Void unused) {

              }

              @Override
              public void onError(Throwable t) {
              }

              @Override
              public void onComplete() {

              }
            });

  }

  private Mono<Void> handleReturnValue(NettyRequestContext nettyContext, Object handler, Object returnValue) {
    if (returnValue != HandlerAdapter.NONE_RETURN_VALUE) {
      try {
        dispatcherHandler.lookupReturnValueHandler(handler, returnValue)
                .handleReturnValue(nettyContext, handler, returnValue);
      }
      catch (Exception e) {
        return Mono.error(e);
      }
    }
    return Mono.empty();
  }

  private Mono<Object> invokeHandler(NettyRequestContext nettyContext, Object handler) {
    try {
      HandlerAdapter adapter = dispatcherHandler.lookupHandlerAdapter(handler);
      return Mono.just(adapter.handle(nettyContext, handler));
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
            new NoHandlerFoundException(
                    request.getMethodValue(), request.getRequestPath(), request.requestHeaders()))
    );
  }
}
