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

package infra.web.server.netty;

import java.util.concurrent.atomic.AtomicInteger;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http2.Http2StreamChannel;

/**
 * Applies request-body read control to an HTTP/1 connection channel or an
 * HTTP/2 child stream channel. HTTP/2 read control does not change the parent
 * connection's auto-read setting.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
final class ChannelRequestBodyReadController implements RequestBodyReadController {

  private final Channel channel;

  private final boolean manualRead;

  private final boolean initialAutoRead;

  private final AtomicInteger state = new AtomicInteger();

  private static final int RELEASED = 1;

  private static final int BODY_COMPLETE = 1 << 1;

  private static final int DESIRED_AUTO_READ = 1 << 2;

  ChannelRequestBodyReadController(Channel channel, boolean manualRead) {
    this.channel = channel;
    this.manualRead = manualRead;
    this.initialAutoRead = channel.config().isAutoRead();
    if (initialAutoRead && !manualRead) {
      state.set(DESIRED_AUTO_READ);
    }
    if (manualRead) {
      setAutoRead(false);
    }
  }

  @Override
  public void pause() {
    if (!manualRead) {
      updateDesiredAutoRead(false);
      setAutoRead(false);
    }
  }

  @Override
  public void resume() {
    if (!manualRead && !isReleased() && !(isHttp1() && isBodyComplete())) {
      updateDesiredAutoRead(true);
      setAutoRead(true);
    }
  }

  @Override
  public void requestRead() {
    if (isReleased() || !manualRead || (isHttp1() && isBodyComplete())) {
      return;
    }
    execute(() -> {
      if (!isReleased()) {
        channel.read();
      }
    });
  }

  @Override
  public void bodyComplete() {
    state.getAndUpdate(value -> value | BODY_COMPLETE);
    if (isHttp1() && !manualRead) {
      updateDesiredAutoRead(false);
      setAutoRead(false);
    }
  }

  @Override
  public void abort() {
    execute(channel::close);
  }

  @Override
  public void release(ChannelFuture responseFuture) {
    if ((state.getAndUpdate(value -> value | RELEASED) & RELEASED) != 0) {
      return;
    }
    responseFuture.addListener(future -> {
      if (isHttp1() && !isBodyComplete()) {
        channel.close();
      }
      else {
        updateDesiredAutoRead(initialAutoRead);
        setAutoRead(initialAutoRead);
      }
    });
  }

  private void setAutoRead(boolean autoRead) {
    execute(() -> {
      // A queued read-state change may have been superseded while waiting for the event loop.
      boolean released = isReleased();
      boolean desired = (state.get() & DESIRED_AUTO_READ) != 0;
      if (released ? autoRead != initialAutoRead : desired != autoRead) {
        return;
      }
      channel.config().setAutoRead(autoRead);
    });
  }

  private void execute(Runnable task) {
    if (channel.eventLoop().inEventLoop()) {
      task.run();
    }
    else {
      channel.eventLoop().execute(task);
    }
  }

  private boolean isHttp1() {
    return !(channel instanceof Http2StreamChannel);
  }

  private boolean isReleased() {
    return (state.get() & RELEASED) != 0;
  }

  private boolean isBodyComplete() {
    return (state.get() & BODY_COMPLETE) != 0;
  }

  private void updateDesiredAutoRead(boolean desired) {
    state.getAndUpdate(value -> desired ? value | DESIRED_AUTO_READ : value & ~DESIRED_AUTO_READ);
  }

}
