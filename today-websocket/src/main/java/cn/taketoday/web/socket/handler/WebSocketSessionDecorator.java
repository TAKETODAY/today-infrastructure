/*
 * Copyright 2017 - 2023 the original author or authors.
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
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import cn.taketoday.core.AttributeAccessor;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.socket.BinaryMessage;
import cn.taketoday.web.socket.CloseStatus;
import cn.taketoday.web.socket.Message;
import cn.taketoday.web.socket.PingMessage;
import cn.taketoday.web.socket.PongMessage;
import cn.taketoday.web.socket.TextMessage;
import cn.taketoday.web.socket.WebSocketSession;

/**
 * Wraps another {@link WebSocketSession} instance
 * and delegates to it.
 *
 * <p>Also provides a {@link #delegate} method to return the decorated session
 * as well as a {@link #getRawSession()} method to go through all nested delegates
 * and return the "last" session.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/10/11 22:06
 */
public class WebSocketSessionDecorator extends WebSocketSession {

  protected final WebSocketSession delegate;

  public WebSocketSessionDecorator(WebSocketSession delegate) {
    super(null);
    this.delegate = delegate;
  }

  public WebSocketSession getRawSession() {
    WebSocketSession result = this.delegate;
    while (result instanceof WebSocketSessionDecorator decorator) {
      result = decorator.delegate;
    }
    return result;
  }

  /**
   * Return an appropriate session object of the specified type, if available,
   * unwrapping the given session as far as necessary.
   *
   * @param requiredType the desired type of session object
   * @return the matching session object, or {@code null} if none
   * of that type is available
   */
  @Nullable
  @SuppressWarnings("unchecked")
  public static <T> T unwrap(WebSocketSession session, @Nullable Class<T> requiredType) {
    if (requiredType != null) {
      if (requiredType.isInstance(session)) {
        return (T) session;
      }
      else if (session instanceof WebSocketSessionDecorator wrapper) {
        return unwrap(wrapper.delegate, requiredType);
      }
    }
    return null;
  }

  public static <T> T unwrapRequired(WebSocketSession session, @Nullable Class<T> requiredType) {
    T unwrapped = unwrap(session, requiredType);
    if (unwrapped == null) {
      throw new IllegalArgumentException("WebSocketSession not a required type: " + requiredType);
    }
    return unwrapped;
  }

  @Override
  public String getId() {
    return delegate.getId();
  }

  @Override
  public void sendPartialMessage(Message<?> message) throws IOException {
    delegate.sendPartialMessage(message);
  }

  @Override
  public void sendText(String text) throws IOException {
    delegate.sendText(text);
  }

  @Override
  public void sendPartialText(TextMessage partialMessage) throws IOException {
    delegate.sendPartialText(partialMessage);
  }

  @Override
  public void sendPartialText(String partialMessage, boolean isLast) throws IOException {
    delegate.sendPartialText(partialMessage, isLast);
  }

  @Override
  public void sendBinary(BinaryMessage data) throws IOException {
    delegate.sendBinary(data);
  }

  @Override
  public void sendPartialBinary(BinaryMessage data) throws IOException {
    delegate.sendPartialBinary(data);
  }

  @Override
  public void sendPartialBinary(ByteBuffer partialByte, boolean isLast) throws IOException {
    delegate.sendPartialBinary(partialByte, isLast);
  }

  @Override
  public void sendPing(PingMessage message) throws IOException {
    delegate.sendPing(message);
  }

  @Override
  public void sendPong(PongMessage message) throws IOException {
    delegate.sendPong(message);
  }

  @Override
  public boolean isSecure() {
    return delegate.isSecure();
  }

  @Override
  public boolean isOpen() {
    return delegate.isOpen();
  }

  @Override
  public long getMaxIdleTimeout() {
    return delegate.getMaxIdleTimeout();
  }

  @Override
  public void setMaxIdleTimeout(long timeout) {
    delegate.setMaxIdleTimeout(timeout);
  }

  @Override
  public void setMaxBinaryMessageBufferSize(int max) {
    delegate.setMaxBinaryMessageBufferSize(max);
  }

  @Override
  public int getMaxBinaryMessageBufferSize() {
    return delegate.getMaxBinaryMessageBufferSize();
  }

  @Override
  public void setMaxTextMessageBufferSize(int max) {
    delegate.setMaxTextMessageBufferSize(max);
  }

  @Override
  public int getMaxTextMessageBufferSize() {
    return delegate.getMaxTextMessageBufferSize();
  }

  @Override
  public void close() throws IOException {
    delegate.close();
  }

  @Override
  public void close(CloseStatus status) throws IOException {
    delegate.close(status);
  }

  @Override
  @Nullable
  public InetSocketAddress getLocalAddress() {
    return delegate.getLocalAddress();
  }

  @Override
  @Nullable
  public InetSocketAddress getRemoteAddress() {
    return delegate.getRemoteAddress();
  }

  @Override
  public HttpHeaders getHandshakeHeaders() {
    return delegate.getHandshakeHeaders();
  }

  @Override
  @Nullable
  public String getAcceptedProtocol() {
    return delegate.getAcceptedProtocol();
  }

  // AttributeAccessorSupport

  @Override
  public void setAttribute(String name, @Nullable Object value) {
    delegate.setAttribute(name, value);
  }

  @Override
  @Nullable
  public Object getAttribute(String name) {
    return delegate.getAttribute(name);
  }

  @Override
  public <T> T computeAttribute(String name, Function<String, T> computeFunction) {
    return delegate.computeAttribute(name, computeFunction);
  }

  @Override
  public Object removeAttribute(String name) {
    return delegate.removeAttribute(name);
  }

  @Override
  public boolean hasAttribute(String name) {
    return delegate.hasAttribute(name);
  }

  @Override
  public String[] getAttributeNames() {
    return delegate.getAttributeNames();
  }

  @Override
  public Iterator<String> attributeNames() {
    return delegate.attributeNames();
  }

  @Override
  public void copyAttributesFrom(AttributeAccessor source) {
    delegate.copyAttributesFrom(source);
  }

  @Override
  public void clearAttributes() {
    delegate.clearAttributes();
  }

  @Override
  public boolean hasAttributes() {
    return delegate.hasAttributes();
  }

  @Override
  public Map<String, Object> getAttributes() {
    return delegate.getAttributes();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof WebSocketSessionDecorator that))
      return false;
    return Objects.equals(delegate, that.delegate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(delegate);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + " [delegate=" + this.delegate + "]";
  }

}
