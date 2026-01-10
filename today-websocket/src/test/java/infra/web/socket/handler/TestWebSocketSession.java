/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.socket.handler;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import infra.core.io.buffer.DataBufferFactory;
import infra.core.io.buffer.DefaultDataBufferFactory;
import infra.http.HttpHeaders;
import infra.util.concurrent.Future;
import infra.web.socket.CloseStatus;
import infra.web.socket.WebSocketExtension;
import infra.web.socket.WebSocketMessage;
import infra.web.socket.WebSocketSession;

/**
 * A {@link WebSocketSession} for use in tests.
 *
 * @author Rossen Stoyanchev
 */
public class TestWebSocketSession extends WebSocketSession {

  private String id;

  private URI uri;

  private Map<String, Object> attributes = new HashMap<>();

  private InetSocketAddress localAddress;

  private InetSocketAddress remoteAddress;

  private String protocol;

  private List<WebSocketExtension> extensions = new ArrayList<>();

  private boolean open;

  private final List<WebSocketMessage> messages = new ArrayList<>();

  private CloseStatus status;

  private HttpHeaders headers;

  public TestWebSocketSession() {
  }

  public TestWebSocketSession(String id) {
    this.id = id;
  }

  public TestWebSocketSession(boolean open) {
    this.open = open;
  }

  @Override
  public String getId() {
    return this.id;
  }

  @Override
  public DataBufferFactory bufferFactory() {
    return DefaultDataBufferFactory.sharedInstance;
  }

  public void setId(String id) {
    this.id = id;
  }

  public URI getUri() {
    return this.uri;
  }

  public void setUri(URI uri) {
    this.uri = uri;
  }

  public HttpHeaders getHeaders() {
    return this.headers;
  }

  public void setHeaders(HttpHeaders headers) {
    this.headers = headers;
  }

  public void setAttributes(Map<String, Object> attributes) {
    this.attributes = attributes;
  }

  @Override
  public Map<String, Object> getAttributes() {
    return this.attributes;
  }

  @Override
  public InetSocketAddress getLocalAddress() {
    return this.localAddress;
  }

  public void setLocalAddress(InetSocketAddress localAddress) {
    this.localAddress = localAddress;
  }

  @Override
  public InetSocketAddress getRemoteAddress() {
    return this.remoteAddress;
  }

  public void setRemoteAddress(InetSocketAddress remoteAddress) {
    this.remoteAddress = remoteAddress;
  }

  @Override
  public String getAcceptedProtocol() {
    return this.protocol;
  }

  public void setAcceptedProtocol(String protocol) {
    this.protocol = protocol;
  }

  public List<WebSocketExtension> getExtensions() {
    return this.extensions;
  }

  public void setExtensions(List<WebSocketExtension> extensions) {
    this.extensions = extensions;
  }

  @Override
  public boolean isOpen() {
    return this.open;
  }

  public void setOpen(boolean open) {
    this.open = open;
  }

  public List<WebSocketMessage> getSentMessages() {
    return this.messages;
  }

  public CloseStatus getCloseStatus() {
    return this.status;
  }

  @Override
  public Future<Void> send(WebSocketMessage message) {
    messages.add(message);
    return Future.ok();
  }

  @Override
  public boolean isSecure() {
    return false;
  }

  @Override
  public void abort() {
    this.open = false;
  }

  @Override
  public Future<Void> close() {
    this.open = false;
    return Future.ok();
  }

  @Override
  public Future<Void> close(CloseStatus status) {
    this.open = false;
    this.status = status;
    return Future.ok();
  }

}
