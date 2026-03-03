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

import infra.web.socket.CloseStatus;
import infra.web.socket.WebSocketHandler;
import infra.web.socket.server.support.NettyWebSocketSession;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.AttributeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class WebSocketAttributeTests {

  @Test
  void bindShouldStoreAttributeInChannel() {
    EmbeddedChannel channel = new EmbeddedChannel();
    WebSocketHandler handler = mock(WebSocketHandler.class);
    NettyWebSocketSession session = mock(NettyWebSocketSession.class);

    WebSocketAttribute.bind(channel, handler, session);

    WebSocketAttribute found = WebSocketAttribute.find(channel);
    assertThat(found).isNotNull();
    assertThat(found.wsHandler).isEqualTo(handler);
    assertThat(found.session).isEqualTo(session);
  }

  @Test
  void findShouldReturnNullWhenNoAttribute() {
    EmbeddedChannel channel = new EmbeddedChannel();

    WebSocketAttribute found = WebSocketAttribute.find(channel);

    assertThat(found).isNull();
  }

  @Test
  void unbindShouldRemoveAttributeFromChannel() {
    EmbeddedChannel channel = new EmbeddedChannel();
    WebSocketHandler handler = mock(WebSocketHandler.class);
    NettyWebSocketSession session = mock(NettyWebSocketSession.class);

    WebSocketAttribute.bind(channel, handler, session);
    WebSocketAttribute found = WebSocketAttribute.find(channel);
    assertThat(found).isNotNull();

    found.unbind(channel);

    assertThat(WebSocketAttribute.find(channel)).isNull();
  }

  @Test
  void closeShouldCallSessionOnCloseAndUnbind() {
    EmbeddedChannel channel = new EmbeddedChannel();
    WebSocketHandler handler = mock(WebSocketHandler.class);
    NettyWebSocketSession session = mock(NettyWebSocketSession.class);
    infra.logging.Logger logger = mock(infra.logging.Logger.class);
    CloseStatus status = CloseStatus.NORMAL;

    WebSocketAttribute.bind(channel, handler, session);
    WebSocketAttribute found = WebSocketAttribute.find(channel);

    found.close(channel, status, logger);

    verify(session).onClose(handler, status, logger);
    assertThat(WebSocketAttribute.find(channel)).isNull();
  }

  @Test
  void findShouldReturnAttributeFromAttributeMap() {
    EmbeddedChannel channel = new EmbeddedChannel();
    WebSocketHandler handler = mock(WebSocketHandler.class);
    NettyWebSocketSession session = mock(NettyWebSocketSession.class);

    WebSocketAttribute.bind(channel, handler, session);

    WebSocketAttribute found = WebSocketAttribute.find(channel);
    assertThat(found).isNotNull();
  }

}
