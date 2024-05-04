/*
 * Copyright 2017 - 2024 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.web.socket.client.standard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.core.task.SimpleAsyncTaskExecutor;
import cn.taketoday.web.socket.WebSocketHandler;
import cn.taketoday.web.socket.WebSocketHttpHeaders;
import cn.taketoday.web.socket.WebSocketSession;
import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.Endpoint;
import jakarta.websocket.WebSocketContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test fixture for {@link StandardWebSocketClient}.
 *
 * @author Rossen Stoyanchev
 */
public class StandardWebSocketClientTests {

  private StandardWebSocketClient wsClient;

  private WebSocketContainer wsContainer;

  private WebSocketHandler wsHandler;

  private WebSocketHttpHeaders headers;

  @BeforeEach
  public void setup() {
    this.headers = new WebSocketHttpHeaders();
    this.wsHandler = new WebSocketHandler() {
    };
    this.wsContainer = mock(WebSocketContainer.class);
    this.wsClient = new StandardWebSocketClient(this.wsContainer);
    wsClient.setSessionDecorator(delegate -> delegate);
    wsClient.addSessionDecorator(delegate -> delegate);
  }

  @Test
  public void testGetLocalAddress() throws Exception {
    URI uri = new URI("ws://localhost/abc");
    WebSocketSession session = this.wsClient.connect(this.wsHandler, this.headers, uri).get();

    assertThat(session.getLocalAddress()).isNotNull();
    assertThat(session.getLocalAddress().getPort()).isEqualTo(80);
  }

  @Test
  public void testGetLocalAddressWss() throws Exception {
    URI uri = new URI("wss://localhost/abc");
    WebSocketSession session = this.wsClient.connect(this.wsHandler, this.headers, uri).get();

    assertThat(session.getLocalAddress()).isNotNull();
    assertThat(session.getLocalAddress().getPort()).isEqualTo(443);
  }

  @Test
  public void testGetLocalAddressNoScheme() throws Exception {
    URI uri = new URI("localhost/abc");
    assertThatIllegalArgumentException().isThrownBy(() ->
            this.wsClient.connect(this.wsHandler, this.headers, uri));
  }

  @Test
  public void testGetRemoteAddress() throws Exception {
    URI uri = new URI("wss://localhost/abc");
    WebSocketSession session = this.wsClient.connect(this.wsHandler, this.headers, uri).get();

    assertThat(session.getRemoteAddress()).isNotNull();
    assertThat(session.getRemoteAddress().getHostName()).isEqualTo("localhost");
    assertThat(session.getLocalAddress().getPort()).isEqualTo(443);
  }

  @Test
  public void handshakeHeaders() throws Exception {

    URI uri = new URI("ws://localhost/abc");
    List<String> protocols = Collections.singletonList("abc");
    this.headers.setSecWebSocketProtocol(protocols);
    this.headers.add("foo", "bar");

    WebSocketSession session = this.wsClient.connect(this.wsHandler, this.headers, uri).get();

    assertThat(session.getHandshakeHeaders().size()).isEqualTo(1);
    assertThat(session.getHandshakeHeaders().getFirst("foo")).isEqualTo("bar");
  }

  @Test
  public void clientEndpointConfig() throws Exception {

    URI uri = new URI("ws://localhost/abc");
    List<String> protocols = Collections.singletonList("abc");
    this.headers.setSecWebSocketProtocol(protocols);

    this.wsClient.connect(this.wsHandler, this.headers, uri).get();

    ArgumentCaptor<ClientEndpointConfig> captor = ArgumentCaptor.forClass(ClientEndpointConfig.class);
    verify(this.wsContainer).connectToServer(ArgumentMatchers.any(Endpoint.class), captor.capture(), ArgumentMatchers.any(URI.class));
    ClientEndpointConfig endpointConfig = captor.getValue();

    assertThat(endpointConfig.getPreferredSubprotocols()).isEqualTo(protocols);
  }

  @Test
  public void clientEndpointConfigWithUserProperties() throws Exception {

    Map<String, Object> userProperties = Collections.singletonMap("foo", "bar");

    URI uri = new URI("ws://localhost/abc");
    this.wsClient.setUserProperties(userProperties);
    this.wsClient.connect(this.wsHandler, this.headers, uri).get();

    ArgumentCaptor<ClientEndpointConfig> captor = ArgumentCaptor.forClass(ClientEndpointConfig.class);
    verify(this.wsContainer).connectToServer(ArgumentMatchers.any(Endpoint.class), captor.capture(), ArgumentMatchers.any(URI.class));
    ClientEndpointConfig endpointConfig = captor.getValue();

    assertThat(endpointConfig.getUserProperties()).isEqualTo(userProperties);
  }

  @Test
  public void standardWebSocketClientConfiguratorInsertsHandshakeHeaders() throws Exception {

    URI uri = new URI("ws://localhost/abc");
    this.headers.add("foo", "bar");

    this.wsClient.connect(this.wsHandler, this.headers, uri).get();

    ArgumentCaptor<ClientEndpointConfig> captor = ArgumentCaptor.forClass(ClientEndpointConfig.class);
    verify(this.wsContainer).connectToServer(ArgumentMatchers.any(Endpoint.class), captor.capture(), ArgumentMatchers.any(URI.class));
    ClientEndpointConfig endpointConfig = captor.getValue();

    Map<String, List<String>> headers = new HashMap<>();
    endpointConfig.getConfigurator().beforeRequest(headers);
    assertThat(headers.size()).isEqualTo(1);
  }

  @Test
  public void taskExecutor() throws Exception {
    URI uri = new URI("ws://localhost/abc");
    this.wsClient.setTaskExecutor(new SimpleAsyncTaskExecutor());
    WebSocketSession session = this.wsClient.connect(this.wsHandler, this.headers, uri).get();

    assertThat(session).isNotNull();
  }

}
