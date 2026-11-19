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

import org.junit.jupiter.api.Test;

import infra.web.server.netty.ChannelRequestBodyReadController;
import io.netty.channel.ChannelFuture;
import io.netty.channel.embedded.EmbeddedChannel;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests event-loop serialization and lifecycle handling for request-body reads.
 */
class ChannelRequestBodyReadControllerTests {

  @Test
  void manualReadRequestsOneChannelRead() {
    EmbeddedChannel channel = new EmbeddedChannel();
    ChannelRequestBodyReadController controller =
            new ChannelRequestBodyReadController(channel, true);

    controller.requestRead();

    assertThat(channel.config().isAutoRead()).isFalse();
    channel.finishAndReleaseAll();
  }

  @Test
  void releaseAfterBodyCompletionRestoresInitialAutoRead() {
    EmbeddedChannel channel = new EmbeddedChannel();
    ChannelRequestBodyReadController controller =
            new ChannelRequestBodyReadController(channel, false);

    controller.bodyComplete();
    ChannelFuture response = channel.newSucceededFuture();
    controller.release(response);

    assertThat(channel.config().isAutoRead()).isTrue();
    channel.finishAndReleaseAll();
  }

  @Test
  void releaseWithIncompleteHttp1BodyClosesConnection() {
    EmbeddedChannel channel = new EmbeddedChannel();
    ChannelRequestBodyReadController controller =
            new ChannelRequestBodyReadController(channel, true);

    controller.release(channel.newSucceededFuture());

    assertThat(channel.isOpen()).isFalse();
  }

}
