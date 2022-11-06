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

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.context.async.AsyncWebRequest;
import cn.taketoday.web.context.async.WebAsyncUtils;
import cn.taketoday.web.handler.DispatcherHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.EventExecutor;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/21 13:47
 */
public class NettyAsyncWebRequest extends AsyncWebRequest {
  private static final Logger log = LoggerFactory.getLogger(NettyAsyncWebRequest.class);

  private final NettyRequestContext request;
  private final ChannelHandlerContext channelContext;

  private volatile boolean asyncStarted;

  public NettyAsyncWebRequest(NettyRequestContext request) {
    this.request = request;
    this.channelContext = request.getChannelContext();
  }

  @Override
  public void startAsync() {
    EventExecutor executor = channelContext.executor();
    if (timeout != null) {
      executor.schedule(this::checkTimeout, timeout, TimeUnit.MILLISECONDS);
    }

    this.asyncStarted = true;
  }

  private void checkTimeout() {
    if (!isAsyncComplete()) {
      // timeout
      log.debug("Async timeout, dispatch timeout events");
      dispatchEvent(timeoutHandlers);
    }
  }

  @Override
  public boolean isAsyncStarted() {
    return asyncStarted;
  }

  @Override
  public void dispatch(Object concurrentResult) {
    this.asyncStarted = false;
    if (asyncCompleted.compareAndSet(false, true)) {
      EventExecutor executor = channelContext.executor();
      executor.execute(() -> {
        try {
          dispatchToClient(request, concurrentResult);
        }
        catch (Exception e) {
          // last exception handling
          for (Consumer<Throwable> exceptionHandler : exceptionHandlers) {
            exceptionHandler.accept(e);
          }
        }
        finally {
          dispatchEvent(completionHandlers);
        }
      });
    }
  }

  /**
   * write result to client
   *
   * @param request current request
   * @param concurrentResult async result
   */
  protected final void dispatchToClient(RequestContext request, Object concurrentResult) {
    DispatcherHandler dispatcherHandler = (DispatcherHandler) request.getAttribute(DispatcherHandler.BEAN_NAME);
    Assert.state(dispatcherHandler != null, "No DispatcherHandler");

    Object handler = WebAsyncUtils.findHttpRequestHandler(request);
    dispatcherHandler.handleConcurrentResult(request, handler, concurrentResult);
  }

}
