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

package infra.web.socket;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DefaultDataBufferFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2024/12/14 14:23
 */
class WebSocketMessageTests {

  DataBuffer helloText = DefaultDataBufferFactory.sharedInstance.copiedBuffer("hello");

  @Test
  void text() {
    var nativeMessage = WebSocketMessage.text(helloText).getNativeMessage();
    assertThat(nativeMessage).isNull();
    assertThat(WebSocketMessage.text(helloText).getPayloadAsText()).isEqualTo("hello");
    assertThat(WebSocketMessage.text(helloText).getPayloadAsText(StandardCharsets.UTF_8)).isEqualTo("hello");
    assertThat(WebSocketMessage.text(helloText).getPayloadAsText(StandardCharsets.US_ASCII)).isEqualTo("hello");
    assertThat(WebSocketMessage.text(helloText).getType()).isEqualTo(WebSocketMessage.Type.TEXT);
    assertThat(WebSocketMessage.text(helloText).isLast()).isTrue();
    assertThat(WebSocketMessage.text(helloText, false).isLast()).isFalse();
    assertThat(WebSocketMessage.text(helloText, true).isLast()).isTrue();
  }

  @Test
  void binary() {
    assertThat(WebSocketMessage.binary(helloText).getPayload()).isSameAs(helloText);
    assertThat(WebSocketMessage.binary(helloText).getPayloadAsText()).isEqualTo("hello");
    assertThat(WebSocketMessage.binary(helloText).getType()).isEqualTo(WebSocketMessage.Type.BINARY);
    assertThat(WebSocketMessage.binary(helloText)).isEqualTo(WebSocketMessage.binary(helloText));
    assertThat(WebSocketMessage.binary(helloText).isLast()).isTrue();
    assertThat(WebSocketMessage.binary(helloText, false).isLast()).isFalse();
    assertThat(WebSocketMessage.binary(helloText, true).isLast()).isTrue();
    WebSocketMessage binary = WebSocketMessage.binary(helloText);
    assertThat(binary).isEqualTo(binary);
  }

  @Test
  void ping() {
    assertThat(WebSocketMessage.ping(helloText).getPayload()).isSameAs(helloText);
    assertThat(WebSocketMessage.ping().isLast()).isTrue();
    assertThat(WebSocketMessage.ping().getPayload()).isEqualTo(DataBuffer.empty());
  }

  @Test
  void pong() {
    assertThat(WebSocketMessage.pong(helloText).getPayload()).isSameAs(helloText);
    assertThat(WebSocketMessage.pong().isLast()).isTrue();
    assertThat(WebSocketMessage.pong().getPayload()).isEqualTo(DataBuffer.empty());
  }

  @Test
  void toString_() {
    assertThat(WebSocketMessage.binary(helloText).toString()).isEqualTo("WebSocket BINARY message (5 bytes)");

  }
}