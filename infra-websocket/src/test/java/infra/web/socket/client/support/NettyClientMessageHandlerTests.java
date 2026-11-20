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

package infra.web.socket.client.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;

import infra.http.HttpHeaders;
import infra.logging.Logger;
import infra.web.client.HttpClientErrorException;
import infra.web.client.HttpServerErrorException;
import infra.web.client.ResponseErrorHandler;
import infra.web.socket.CloseStatus;
import infra.web.socket.WebSocketHandler;
import infra.web.socket.server.support.NettyWebSocketSession;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class NettyClientMessageHandlerTests {

  private final NettyWebSocketClient client = spy(new NettyWebSocketClient());

  private final WebSocketHandler handler = mock(WebSocketHandler.class);

  private final WebSocketClientHandshaker handshaker = mock(WebSocketClientHandshaker.class);

  private final NettyWebSocketClient.MessageHandler inbound = client.new MessageHandler(false, handler,
          handshaker, URI.create("ws://example.test/socket"), HttpHeaders.forWritable());

  @ParameterizedTest
  @ValueSource(ints = { 400, 503 })
  void fallsBackToStatusExceptionWhenCustomHandlerIgnoresError(int code) throws Exception {
    client.setErrorHandler(mock(ResponseErrorHandler.class));
    var response = response(HttpResponseStatus.valueOf(code));

    inbound.channelRead(mock(ChannelHandlerContext.class), response);

    assertThat(inbound.promise.getCause()).isInstanceOf(code == 400 ? HttpClientErrorException.class : HttpServerErrorException.class);
    assertThat(response.refCnt()).isZero();
  }

  @Test
  void releasesResponseWhenHandshakerFails() throws Exception {
    var context = mock(ChannelHandlerContext.class);
    var channel = mock(Channel.class);
    given(context.channel()).willReturn(channel);
    var response = response(HttpResponseStatus.SWITCHING_PROTOCOLS);
    var failure = new IllegalStateException("handshake failed");
    doThrow(failure).when(handshaker).finishHandshake(channel, response);

    inbound.channelRead(context, response);

    assertThat(inbound.promise.getCause()).isSameAs(failure);
    assertThat(response.refCnt()).isZero();
  }

  @Test
  void ignoresRedundantHandshakeResponseAndForwardsOtherInboundObjects() throws Exception {
    ChannelHandlerContext context = mock(ChannelHandlerContext.class);
    given(handshaker.isHandshakeComplete()).willReturn(true);
    var response = response(HttpResponseStatus.SWITCHING_PROTOCOLS);
    Object inboundEvent = new Object();

    inbound.channelRead(context, response);
    inbound.channelRead(context, inboundEvent);

    assertThat(response.refCnt()).isZero();
    assertThat(inbound.promise.isDone()).isFalse();
    verify(context).fireChannelRead(inboundEvent);
  }

  @Test
  void cancellationOfConnectFutureCancelsHandshake() {
    ChannelFuture connect = mock(ChannelFuture.class);
    given(connect.isCancelled()).willReturn(true);

    inbound.operationComplete(connect);

    assertThat(inbound.promise.isCancelled()).isTrue();
  }

  @Test
  void transportFailureBeforeHandshakeFailsThePromise() {
    var failure = new IllegalStateException("transport failure");

    inbound.exceptionCaught(mock(ChannelHandlerContext.class), failure);

    assertThat(inbound.promise.getCause()).isSameAs(failure);
  }

  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  void notifiesRemoteCloseOrTransportDisconnection(boolean receivesCloseFrame) throws Exception {
    ChannelHandlerContext context = mock(ChannelHandlerContext.class);
    EmbeddedChannel channel = new EmbeddedChannel();
    NettyWebSocketSession session = mock(NettyWebSocketSession.class);
    given(context.channel()).willReturn(channel);
    doReturn(session).when(client).createSession(eq(channel), eq(false), eq(handshaker), any());
    try {
      inbound.channelRead(context, response(HttpResponseStatus.SWITCHING_PROTOCOLS));
      assertThat(inbound.promise.getNow()).isSameAs(session);
      verify(handler).onOpen(session);

      TextWebSocketFrame text = new TextWebSocketFrame("test");
      try {
        inbound.channelRead(context, text);
        verify(session).handleMessage(eq(handler), eq(text), any(Logger.class));
      }
      finally {
        text.release();
      }

      Throwable failure = new IllegalStateException("transport failure");
      inbound.exceptionCaught(context, failure);
      verify(handler).onError(session, failure);
      given(session.isOpen()).willReturn(true);
      doThrow(new IllegalStateException("handler failure")).when(handler).onError(session, failure);
      inbound.exceptionCaught(context, failure);
      verify(session).close(CloseStatus.SERVER_ERROR);

      if (receivesCloseFrame) {
        inbound.channelRead(context, new Object());
        var close = new CloseWebSocketFrame(1000, "done");
        inbound.channelRead(context, close);
        assertThat(close.refCnt()).isZero();
        verify(session).onClose(eq(handler), eq(new CloseStatus(1000, "done")), any(Logger.class));
        inbound.channelInactive(context);
        verify(handler, never()).onClose(session, CloseStatus.NO_CLOSE_FRAME);
      }
      else {
        inbound.channelInactive(context);
        verify(handler).onClose(session, CloseStatus.NO_CLOSE_FRAME);
      }
    }
    finally {
      channel.finishAndReleaseAll();
    }
  }

  private static DefaultFullHttpResponse response(HttpResponseStatus status) {
    return new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.buffer(1).writeByte('x'));
  }

}
