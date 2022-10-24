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

import cn.taketoday.util.ExceptionUtils;
import cn.taketoday.web.HandlerMatchingMetadata;
import cn.taketoday.web.context.async.AsyncWebRequest;
import cn.taketoday.web.handler.result.AsyncReturnValueHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.EventExecutor;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/21 13:47
 */
public class NettyAsyncWebRequest extends AsyncWebRequest {
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
      executor.execute(() -> sendToClient(concurrentResult));
    }
  }

  private void sendToClient(Object concurrentResult) {
    System.out.println(concurrentResult);
    if (concurrentResult instanceof Throwable asyncError) {
      // handle Exception
      throw ExceptionUtils.sneakyThrow(asyncError);
    }
    else {
      HandlerMatchingMetadata matchingMetadata = request.getMatchingMetadata();
      if (matchingMetadata != null) {
        Object handler = matchingMetadata.getHandler();
        if (handler instanceof AsyncReturnValueHandler returnValueHandler) {
          ExceptionUtils.sneakyThrow(() ->
                  returnValueHandler.handleAsyncReturnValue(request, concurrentResult));
        }
        else {
//          ReturnValueHandler returnValueHandler = manager.findHandler(handler, concurrentResult);
//          if (returnValueHandler != null) {
//            ExceptionUtils.sneakyThrow(() ->
//                    returnValueHandler.handleReturnValue(request, handler, concurrentResult));
//          }
//          else {
//            throw new ReturnValueHandlerNotFoundException(concurrentResult, handler);
//          }
        }
      }
      else {

      }
    }

    dispatchEvent(completionHandlers);
    // cleanup
    request.requestCompleted();
  }

}
