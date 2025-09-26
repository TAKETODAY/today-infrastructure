/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.web.server.support;

import org.jspecify.annotations.Nullable;

import java.util.concurrent.TimeUnit;

import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.web.async.AsyncWebRequest;
import io.netty.channel.Channel;
import io.netty.util.concurrent.ScheduledFuture;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/21 13:47
 */
public class NettyAsyncWebRequest extends AsyncWebRequest {

  private static final Logger log = LoggerFactory.getLogger(NettyAsyncWebRequest.class);

  private final NettyRequestContext request;

  private final Channel channel;

  private volatile boolean asyncStarted;

  @Nullable
  private ScheduledFuture<?> timeoutFuture;

  NettyAsyncWebRequest(NettyRequestContext request) {
    this.request = request;
    this.channel = request.channel;
  }

  @Override
  public void startAsync() {
    if (timeout != null && timeout > 0) {
      timeoutFuture = channel.eventLoop().schedule(this::checkTimeout, timeout, TimeUnit.MILLISECONDS);
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
  public void dispatch(@Nullable Object concurrentResult) {
    this.asyncStarted = false;
    if (asyncCompleted.compareAndSet(false, true)) {
      if (timeoutFuture != null) {
        timeoutFuture.cancel(true);
      }
      try {
        request.dispatchConcurrentResult(concurrentResult);
      }
      catch (Throwable e) {
        // last exception handling
        for (var exceptionHandler : exceptionHandlers) {
          exceptionHandler.accept(e);
        }
        channel.pipeline().fireExceptionCaught(e);
      }
      finally {
        dispatchEvent(completionHandlers);
      }
    }
  }

}
