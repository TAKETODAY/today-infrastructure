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

import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class HandshakeChannelTests {

  @Test
  void handshakeChannelShouldWriteAndFlushResponse() {
    EmbeddedChannel channel = new EmbeddedChannel();
    var promise = channel.newPromise();

    HandshakeChannel handshakeChannel = new HandshakeChannel(channel, promise);

    DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.EMPTY_BUFFER);
    handshakeChannel.writeAndFlush(response);

    assertThat(handshakeChannel.response).isEqualTo(response);
    assertThat(promise.isDone()).isFalse();
  }

  @Test
  void handshakeChannelShouldReleaseResponseOnRelease() {
    EmbeddedChannel channel = new EmbeddedChannel();
    var promise = channel.newPromise();

    HandshakeChannel handshakeChannel = new HandshakeChannel(channel, promise);

    DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.EMPTY_BUFFER);
    handshakeChannel.writeAndFlush(response);

    handshakeChannel.release();

    assertThat(handshakeChannel.response).isNull();
  }

  @Test
  void handshakeChannelRunShouldSetPromiseSuccess() {
    EmbeddedChannel channel = new EmbeddedChannel();
    var promise = channel.newPromise();

    HandshakeChannel handshakeChannel = new HandshakeChannel(channel, promise);

    handshakeChannel.run();

    assertThat(promise.isSuccess()).isTrue();
  }

  @Test
  void handshakeChannelShouldDelegateToOriginalChannel() {
    EmbeddedChannel channel = new EmbeddedChannel();
    var promise = channel.newPromise();

    HandshakeChannel handshakeChannel = new HandshakeChannel(channel, promise);

    assertThat(handshakeChannel.isActive()).isEqualTo(channel.isActive());
    assertThat(handshakeChannel.isOpen()).isEqualTo(channel.isOpen());
    assertThat(handshakeChannel.isWritable()).isEqualTo(channel.isWritable());
    assertThat(handshakeChannel.id()).isEqualTo(channel.id());
  }

}
