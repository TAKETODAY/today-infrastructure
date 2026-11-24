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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.AttributeKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class HandshakeChannelTests {

  private final EmbeddedChannel channel = new EmbeddedChannel();

  @AfterEach
  void releaseChannel() {
    channel.finishAndReleaseAll();
  }

  @Test
  void handshakeChannelShouldWriteAndFlushResponse() {
    var promise = channel.newPromise();

    HandshakeChannel handshakeChannel = new HandshakeChannel(channel, promise);

    DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.EMPTY_BUFFER);
    try {
      assertThat(handshakeChannel.writeAndFlush(response)).isSameAs(promise);
      assertThat(handshakeChannel.response).isSameAs(response);
      assertThat(channel.outboundMessages()).isEmpty();
      assertThat(promise.isDone()).isFalse();
    }
    finally {
      handshakeChannel.release();
    }
  }

  @Test
  void handshakeChannelShouldReleaseResponseOnRelease() {
    var promise = channel.newPromise();

    HandshakeChannel handshakeChannel = new HandshakeChannel(channel, promise);

    DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
            Unpooled.buffer().writeByte(1));
    handshakeChannel.writeAndFlush(response);

    handshakeChannel.release();
    handshakeChannel.release();

    assertThat(handshakeChannel.response).isNull();
    assertThat(response.refCnt()).isZero();
  }

  @Test
  void handshakeChannelRunShouldSetPromiseSuccess() {
    var promise = channel.newPromise();

    HandshakeChannel handshakeChannel = new HandshakeChannel(channel, promise);

    handshakeChannel.run();

    assertThat(promise.isSuccess()).isTrue();
  }

  @Test
  void handshakeChannelShouldDelegateToOriginalChannel() {
    var promise = channel.newPromise();

    HandshakeChannel handshakeChannel = new HandshakeChannel(channel, promise);

    assertThat(handshakeChannel.isActive()).isEqualTo(channel.isActive());
    assertThat(handshakeChannel.isOpen()).isEqualTo(channel.isOpen());
    assertThat(handshakeChannel.isWritable()).isEqualTo(channel.isWritable());
    assertThat(handshakeChannel.id()).isEqualTo(channel.id());
    assertThat(handshakeChannel.isRegistered()).isEqualTo(channel.isRegistered());
    assertThat(handshakeChannel.alloc()).isSameAs(channel.alloc());
    assertThat(handshakeChannel.config()).isSameAs(channel.config());
    assertThat(handshakeChannel.eventLoop()).isSameAs(channel.eventLoop());
    assertThat(handshakeChannel.pipeline()).isSameAs(channel.pipeline());
    assertThat(handshakeChannel.metadata()).isSameAs(channel.metadata());
    assertThat(handshakeChannel.unsafe()).isSameAs(channel.unsafe());
    assertThat(handshakeChannel.parent()).isSameAs(channel.parent());
    assertThat(handshakeChannel.localAddress()).isSameAs(channel.localAddress());
    assertThat(handshakeChannel.remoteAddress()).isSameAs(channel.remoteAddress());
    assertThat(handshakeChannel.bytesBeforeWritable()).isEqualTo(channel.bytesBeforeWritable());
    assertThat(handshakeChannel.bytesBeforeUnwritable()).isEqualTo(channel.bytesBeforeUnwritable());
    assertThat(handshakeChannel.compareTo(channel)).isZero();
  }

  @Test
  void attributesAndPromisesBelongToUnderlyingChannel() {
    var handshake = new HandshakeChannel(channel, channel.newPromise());
    var key = AttributeKey.<String>valueOf(HandshakeChannelTests.class, "protocol");
    var failure = new IllegalStateException("test failure");

    assertThat(handshake.hasAttr(key)).isFalse();
    handshake.attr(key).set("websocket");
    assertThat(handshake.hasAttr(key)).isTrue();
    assertThat(channel.attr(key).get()).isEqualTo("websocket");
    assertThat(handshake.newPromise().channel()).isSameAs(channel);
    assertThat(handshake.newProgressivePromise().channel()).isSameAs(channel);
    assertThat(handshake.newSucceededFuture().isSuccess()).isTrue();
    assertThat(handshake.newFailedFuture(failure).cause()).isSameAs(failure);
    assertThat(handshake.voidPromise()).isSameAs(channel.voidPromise());
    assertThat(handshake.closeFuture()).isSameAs(channel.closeFuture());
  }

  @Test
  void ordinaryWritesAreForwardedInOrderWithoutCompletingTheHandshake() {
    var handshakePromise = channel.newPromise();
    var handshake = new HandshakeChannel(channel, handshakePromise);
    var writePromise = channel.newPromise();
    var flushPromise = channel.newPromise();

    ChannelFuture firstWrite = handshake.write("first");
    assertThat(handshake.write("second", writePromise)).isSameAs(writePromise);
    assertThat(channel.outboundMessages()).isEmpty();
    assertThat(handshake.flush()).isSameAs(channel);
    assertThat(handshake.writeAndFlush("third", flushPromise)).isSameAs(flushPromise);
    assertThat(firstWrite.isSuccess()).isTrue();
    assertThat(writePromise.isSuccess()).isTrue();
    assertThat(flushPromise.isSuccess()).isTrue();
    assertThat(channel.outboundMessages()).containsExactly("first", "second", "third");
    assertThat(handshake.read()).isSameAs(channel);
    assertThat(handshakePromise.isDone()).isFalse();
  }

  @Test
  void transportOperationsForwardAddressesAndPromises() {
    Channel transport = mock(Channel.class);
    var promise = channel.newPromise();
    var handshake = new HandshakeChannel(transport, channel.newPromise());
    var local = new InetSocketAddress("localhost", 0);
    var remote = new InetSocketAddress("localhost", 8080);
    var future = channel.newSucceededFuture();
    given(transport.bind(local)).willReturn(future);
    given(transport.bind(local, promise)).willReturn(promise);
    given(transport.connect(remote)).willReturn(future);
    given(transport.connect(remote, local)).willReturn(future);
    given(transport.connect(remote, promise)).willReturn(promise);
    given(transport.connect(remote, local, promise)).willReturn(promise);
    given(transport.disconnect()).willReturn(future);
    given(transport.disconnect(promise)).willReturn(promise);
    given(transport.deregister()).willReturn(future);
    given(transport.deregister(promise)).willReturn(promise);
    given(transport.close()).willReturn(future);
    given(transport.close(promise)).willReturn(promise);

    assertThat(handshake.bind(local)).isSameAs(future);
    assertThat(handshake.bind(local, promise)).isSameAs(promise);
    assertThat(handshake.connect(remote)).isSameAs(future);
    assertThat(handshake.connect(remote, local)).isSameAs(future);
    assertThat(handshake.connect(remote, promise)).isSameAs(promise);
    assertThat(handshake.connect(remote, local, promise)).isSameAs(promise);
    assertThat(handshake.disconnect()).isSameAs(future);
    assertThat(handshake.disconnect(promise)).isSameAs(promise);
    assertThat(handshake.deregister()).isSameAs(future);
    assertThat(handshake.deregister(promise)).isSameAs(promise);
    assertThat(handshake.close()).isSameAs(future);
    assertThat(handshake.close(promise)).isSameAs(promise);
  }

}
