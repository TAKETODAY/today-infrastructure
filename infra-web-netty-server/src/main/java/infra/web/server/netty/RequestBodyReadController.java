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

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

/**
 * Controls transport reads for one request body.
 *
 * <p>Calls can originate on the application thread or the Netty event loop.
 * The controller coordinates read and close operations on the channel's event loop.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
interface RequestBodyReadController {

  /**
   * Pause automatic reads when the buffer reaches its high-water mark.
   */
  void pause();

  /**
   * Resume automatic reads when the buffer drains to its low-water mark,
   * provided the exchange still allows reads.
   */
  void resume();

  /**
   * Request a transport read in manual mode when the reader needs more data.
   */
  void requestRead();

  /**
   * Mark the body as fully received from the transport, even if the application
   * has not yet consumed all buffered bytes. For HTTP/1, keep further reads
   * paused until the response finishes.
   */
  void bodyComplete();

  /**
   * Close the HTTP/1 connection or the HTTP/2 stream when the body cannot be
   * buffered within the hard limits.
   */
  void abort();

  /**
   * Release read control and restore the channel's original auto-read setting
   * after the response future completes. Close an HTTP/1 connection instead
   * if the transport has not received the end of the request body.
   *
   * @param responseFuture the terminating response write, or a completed future
   * if there is no write to wait for
   */
  void release(ChannelFuture responseFuture);

  /**
   * Create a controller for one exchange.
   *
   * @param channel the HTTP/1 connection or HTTP/2 stream channel
   * @param manualRead whether to request reads on demand
   * @return the controller for that channel
   */
  static RequestBodyReadController forChannel(Channel channel, boolean manualRead) {
    return new ChannelRequestBodyReadController(channel, manualRead);
  }

}
