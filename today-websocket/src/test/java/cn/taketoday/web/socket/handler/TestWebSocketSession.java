/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.web.socket.handler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.web.socket.BinaryMessage;
import cn.taketoday.web.socket.CloseStatus;
import cn.taketoday.web.socket.Message;
import cn.taketoday.web.socket.PingMessage;
import cn.taketoday.web.socket.PongMessage;
import cn.taketoday.web.socket.WebSocketExtension;
import cn.taketoday.web.socket.WebSocketSession;

/**
 * A {@link WebSocketSession} for use in tests.
 *
 * @author Rossen Stoyanchev
 */
@SuppressWarnings("serial")
public class TestWebSocketSession extends WebSocketSession {

  private String id;

  private URI uri;

  private Map<String, Object> attributes = new HashMap<>();

  private InetSocketAddress localAddress;

  private InetSocketAddress remoteAddress;

  private String protocol;

  private List<WebSocketExtension> extensions = new ArrayList<>();

  private boolean open;

  private final List<Message<?>> messages = new ArrayList<>();

  private CloseStatus status;

  private HttpHeaders headers;

  public TestWebSocketSession(String id) {
    super(HttpHeaders.create());
    this.id = id;
  }

  public TestWebSocketSession(boolean open) {
    super(HttpHeaders.create());
    this.open = open;
  }

  @Override
  public String getId() {
    return this.id;
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

  @Override
  public HttpHeaders getHandshakeHeaders() {
    return this.headers;
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

  @Override
  public long getMaxIdleTimeout() {
    return 0;
  }

  @Override
  public void setMaxIdleTimeout(long timeout) {

  }

  @Override
  public void setMaxBinaryMessageBufferSize(int max) {

  }

  @Override
  public int getMaxBinaryMessageBufferSize() {
    return 0;
  }

  @Override
  public void setMaxTextMessageBufferSize(int max) {

  }

  @Override
  public int getMaxTextMessageBufferSize() {
    return 0;
  }

  public void setOpen(boolean open) {
    this.open = open;
  }

  public List<Message<?>> getSentMessages() {
    return this.messages;
  }

  public CloseStatus getCloseStatus() {
    return this.status;
  }

  @Override
  public void sendMessage(Message<?> message) throws IOException {
    this.messages.add(message);
  }

  @Override
  public void sendText(String text) throws IOException {

  }

  @Override
  public void sendPartialText(String partialMessage, boolean isLast) throws IOException {

  }

  @Override
  public void sendBinary(BinaryMessage data) throws IOException {

  }

  @Override
  public void sendPartialBinary(ByteBuffer partialByte, boolean isLast) throws IOException {

  }

  @Override
  public void sendPing(PingMessage message) throws IOException {

  }

  @Override
  public void sendPong(PongMessage message) throws IOException {

  }

  @Override
  public boolean isSecure() {
    return false;
  }

  @Override
  public void close() throws IOException {
    this.open = false;
  }

  @Override
  public void close(CloseStatus status) throws IOException {
    this.open = false;
    this.status = status;
  }

}
