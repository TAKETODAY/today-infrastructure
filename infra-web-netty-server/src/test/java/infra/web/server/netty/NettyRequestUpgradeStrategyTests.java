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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import infra.context.ApplicationContext;
import infra.web.DispatcherHandler;
import infra.web.multipart.MultipartParser;
import infra.web.server.error.SendErrorHandler;
import infra.web.socket.CloseStatus;
import infra.web.socket.WebSocketHandler;
import infra.web.socket.WebSocketMessage;
import infra.web.socket.WebSocketSession;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketDecoderConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0 2024/5/3 11:05
 */
class NettyRequestUpgradeStrategyTests {

  private final EmbeddedChannel channel = new EmbeddedChannel();

  private final NettyRequestUpgradeStrategy strategy = new NettyRequestUpgradeStrategy();

  private final WebSocketHandler socketHandler = mock(WebSocketHandler.class);

  @AfterEach
  void releaseChannel() {
    channel.finishAndReleaseAll();
  }

  @Test
  void setDecoderConfig() {
    assertThat(new NettyRequestUpgradeStrategy()).extracting("decoderConfig").isNotNull();

    WebSocketDecoderConfig config = WebSocketDecoderConfig.newBuilder().build();
    assertThat(new NettyRequestUpgradeStrategy(config)).extracting("decoderConfig").isSameAs(config);
  }

  @Test
  void upgradeRequiresAnHttp1Context() {
    var context = mock(infra.web.HttpContext.class);
    assertThat(strategy.getSupportedVersions()).containsExactly("13");
    assertThat(strategy.getSupportedExtensions(context)).isEmpty();
    assertThatIllegalStateException().isThrownBy(() -> strategy.upgrade(context, null, List.of(), socketHandler, Map.of()))
            .withMessageContaining("HTTP/1.x");
  }

  @Test
  void unsupportedVersionReturnsUpgradeRequiredWithoutOpeningSession() {
    var session = installPipeline(null);
    sendHandshake("12");

    assertThat(readOutbound()).startsWith("HTTP/1.1 426").contains("sec-websocket-version: 13");
    assertThat(session.get()).isNull();
    verifyNoInteractions(socketHandler);
  }

  @ParameterizedTest
  @ValueSource(strings = "chat")
  @NullSource
  void successfulUpgradeDispatchesMessagesAndClose(String selectedProtocol) throws Exception {
    var sessionRef = installPipeline(selectedProtocol);
    sendHandshake("13");

    WebSocketSession session = sessionRef.get();
    assertThat(session).isNotNull();
    assertThat(session.getAttributes()).containsEntry("source", "test");
    String response = readOutbound();
    assertThat(response).startsWith("HTTP/1.1 101").contains("sec-websocket-accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo=");
    if (selectedProtocol == null) {
      assertThat(response).doesNotContain("sec-websocket-protocol:");
    }
    else {
      assertThat(response).contains("sec-websocket-protocol: chat");
    }
    verify(socketHandler).onOpen(session);

    var message = new TextWebSocketFrame("hello");
    channel.writeInbound(message);
    var received = ArgumentCaptor.forClass(WebSocketMessage.class);
    verify(socketHandler).handleMessage(eq(session), received.capture());
    assertThat(received.getValue().getType()).isEqualTo(WebSocketMessage.Type.TEXT);
    assertThat(message.refCnt()).isZero();

    var close = new CloseWebSocketFrame(1000, "done");
    channel.writeInbound(close);
    verify(socketHandler).onClose(session, CloseStatus.create(1000, "done"));
    assertThat(WebSocketAttribute.find(channel)).isNull();
    assertThat(close.refCnt()).isZero();
  }

  @Test
  void channelClosureNotifiesUpgradedSession() throws Exception {
    var sessionRef = installPipeline(null);
    sendHandshake("13");
    assertThat(readOutbound()).startsWith("HTTP/1.1 101");

    var failure = new IllegalStateException("test failure");
    channel.pipeline().fireExceptionCaught(failure);
    verify(socketHandler).onError(sessionRef.get(), failure);
    channel.close().syncUninterruptibly();
    verify(socketHandler).onClose(sessionRef.get(), CloseStatus.NO_CLOSE_FRAME);
    assertThat(WebSocketAttribute.find(channel)).isNull();
  }

  private AtomicReference<WebSocketSession> installPipeline(String protocol) {
    var session = new AtomicReference<WebSocketSession>();
    var context = mock(ApplicationContext.class);
    var dispatcher = new DispatcherHandler(context) {
      @Override
      public void handleRequest(infra.web.HttpContext request) throws Exception {
        var upgraded = strategy.upgrade(request, protocol, List.of(), socketHandler, Map.of("source", "test"));
        session.set(upgraded);
        if (upgraded != null) {
          completeRequest(request, null);
        }
      }
    };
    var config = NettyRequestConfig.forBuilder(false)
            .sendErrorHandler(mock(SendErrorHandler.class)).multipartParser(mock(MultipartParser.class)).build();
    channel.pipeline().addLast(new HttpServerCodec(),
            new HttpTrafficHandler(config, context, dispatcher, (request, task) -> task.run()));
    return session;
  }

  private void sendHandshake(String version) {
    channel.writeInbound(Unpooled.copiedBuffer("GET /chat HTTP/1.1\r\n"
            + "Host: localhost\r\nConnection: Upgrade\r\nUpgrade: websocket\r\n"
            + "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\nSec-WebSocket-Protocol: chat\r\n"
            + "Sec-WebSocket-Version: " + version + "\r\n\r\n", StandardCharsets.US_ASCII));
  }

  private String readOutbound() {
    var response = new StringBuilder();
    ByteBuf buffer;
    while ((buffer = channel.readOutbound()) != null) {
      try {
        response.append(buffer.toString(StandardCharsets.US_ASCII));
      }
      finally {
        buffer.release();
      }
    }
    return response.toString();
  }

}
