/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

  private @Nullable ScheduledFuture<?> timeoutFuture;

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

  void checkTimeout() {
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
