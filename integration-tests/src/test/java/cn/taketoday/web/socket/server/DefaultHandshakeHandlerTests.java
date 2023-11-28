/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.web.socket.server;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.web.socket.AbstractHttpRequestTests;
import cn.taketoday.web.socket.SubProtocolCapable;
import cn.taketoday.web.socket.WebSocketExtension;
import cn.taketoday.web.socket.WebSocketHandler;
import cn.taketoday.web.socket.WebSocketHttpHeaders;
import cn.taketoday.web.socket.handler.TextWebSocketHandler;
import cn.taketoday.web.socket.server.support.DefaultHandshakeHandler;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test fixture for {@link cn.taketoday.web.socket.server.support.DefaultHandshakeHandler}.
 *
 * @author Rossen Stoyanchev
 */
public class DefaultHandshakeHandlerTests extends AbstractHttpRequestTests {

  private RequestUpgradeStrategy upgradeStrategy = mock(RequestUpgradeStrategy.class);

  private DefaultHandshakeHandler handshakeHandler = new DefaultHandshakeHandler(this.upgradeStrategy);

  @Test
  public void supportedSubProtocols() {
    this.handshakeHandler.setSupportedProtocols("stomp", "mqtt");
    given(this.upgradeStrategy.getSupportedVersions()).willReturn(new String[] { "13" });

    this.servletRequest.setMethod("GET");
    initHeaders(this.request.getHeaders()).setSecWebSocketProtocol("STOMP");

    WebSocketHandler handler = new TextWebSocketHandler();
    Map<String, Object> attributes = Collections.emptyMap();
    this.handshakeHandler.doHandshake(this.request, handler, attributes);

    verify(this.upgradeStrategy).upgrade(this.request, "STOMP",
            Collections.emptyList(), handler, attributes);
  }

  @Test
  public void supportedExtensions() {
    WebSocketExtension extension1 = new WebSocketExtension("ext1");
    WebSocketExtension extension2 = new WebSocketExtension("ext2");

    given(this.upgradeStrategy.getSupportedVersions()).willReturn(new String[] { "13" });
    given(this.upgradeStrategy.getSupportedExtensions(this.request)).willReturn(Collections.singletonList(extension1));
    this.servletRequest.setMethod("GET");
    initHeaders(this.request.getHeaders())
            .setSecWebSocketExtensions(Arrays.asList(extension1, extension2));

    WebSocketHandler handler = new TextWebSocketHandler();
    Map<String, Object> attributes = Collections.emptyMap();
    this.handshakeHandler.doHandshake(this.request, handler, attributes);

    verify(this.upgradeStrategy).upgrade(this.request, null,
            Collections.singletonList(extension1), handler, attributes);
  }

  @Test
  public void subProtocolCapableHandler() {
    given(this.upgradeStrategy.getSupportedVersions()).willReturn(new String[] { "13" });

    this.servletRequest.setMethod("GET");
    initHeaders(this.request.getHeaders()).setSecWebSocketProtocol("v11.stomp");

    WebSocketHandler handler = new SubProtocolCapableHandler("v12.stomp", "v11.stomp");
    Map<String, Object> attributes = Collections.emptyMap();
    this.handshakeHandler.doHandshake(this.request, handler, attributes);

    verify(this.upgradeStrategy).upgrade(this.request, "v11.stomp",
            Collections.emptyList(), handler, attributes);
  }

  @Test
  public void subProtocolCapableHandlerNoMatch() {
    given(this.upgradeStrategy.getSupportedVersions()).willReturn(new String[] { "13" });

    this.servletRequest.setMethod("GET");
    initHeaders(this.request.getHeaders()).setSecWebSocketProtocol("v10.stomp");

    WebSocketHandler handler = new SubProtocolCapableHandler("v12.stomp", "v11.stomp");
    Map<String, Object> attributes = Collections.emptyMap();
    this.handshakeHandler.doHandshake(this.request, handler, attributes);

    verify(this.upgradeStrategy).upgrade(this.request, null,
            Collections.emptyList(), handler, attributes);
  }

  private WebSocketHttpHeaders initHeaders(HttpHeaders httpHeaders) {
    WebSocketHttpHeaders headers = new WebSocketHttpHeaders(httpHeaders);
    headers.setUpgrade("WebSocket");
    headers.setConnection("Upgrade");
    headers.setSecWebSocketVersion("13");
    headers.setSecWebSocketKey("82/ZS2YHjEnUN97HLL8tbw==");
    return headers;
  }

  private static class SubProtocolCapableHandler extends TextWebSocketHandler implements SubProtocolCapable {

    private final List<String> subProtocols;

    public SubProtocolCapableHandler(String... subProtocols) {
      this.subProtocols = Arrays.asList(subProtocols);
    }

    @Override
    public List<String> getSubProtocols() {
      return this.subProtocols;
    }
  }

}
