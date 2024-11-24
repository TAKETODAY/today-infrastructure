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

package infra.web.socket.handler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import infra.core.AttributeAccessor;
import infra.lang.Nullable;
import infra.web.socket.CloseStatus;
import infra.web.socket.Message;
import infra.web.socket.WebSocketSession;

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
  public void sendText(CharSequence text) throws IOException {
    delegate.sendText(text);
  }

  @Override
  public void sendBinary(ByteBuffer data) throws IOException {
    delegate.sendBinary(data);
  }

  @Override
  public void sendMessage(Message<?> message) throws IOException {
    delegate.sendMessage(message);
  }

  @Override
  public void sendPing() throws IOException {
    delegate.sendPing();
  }

  @Override
  public void sendPong() throws IOException {
    delegate.sendPong();
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
  public boolean isActive() {
    return delegate.isActive();
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
  public Iterable<String> attributeNames() {
    return delegate.attributeNames();
  }

  @Override
  public void copyFrom(AttributeAccessor source) {
    delegate.copyFrom(source);
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
    return "%s [delegate=%s]".formatted(getClass().getSimpleName(), this.delegate);
  }

}
