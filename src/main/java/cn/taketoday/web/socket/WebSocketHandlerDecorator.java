/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.web.socket;

import cn.taketoday.lang.Assert;
import cn.taketoday.web.RequestContext;

/**
 * Wraps another {@link WebSocketHandler} instance and delegates to it.
 *
 * <p>Also provides a {@link #getDelegate()} method to return the decorated
 * handler as well as a {@link #getLastHandler()} method to go through all nested
 * delegates and return the "last" handler.
 *
 * @author Rossen Stoyanchev
 * @author TODAY 2021/11/12 16:44
 * @since 4.0
 * @since 4.0
 */
public class WebSocketHandlerDecorator extends WebSocketHandler {
  private final WebSocketHandler delegate;

  public WebSocketHandlerDecorator(WebSocketHandler delegate) {
    Assert.notNull(delegate, "Delegate must not be null");
    this.delegate = delegate;
  }

  public WebSocketHandler getDelegate() {
    return this.delegate;
  }

  public WebSocketHandler getLastHandler() {
    WebSocketHandler result = this.delegate;
    while (result instanceof WebSocketHandlerDecorator) {
      result = ((WebSocketHandlerDecorator) result).getDelegate();
    }
    return result;
  }

  public static WebSocketHandler unwrap(WebSocketHandler handler) {
    if (handler instanceof WebSocketHandlerDecorator) {
      return ((WebSocketHandlerDecorator) handler).getLastHandler();
    }
    else {
      return handler;
    }
  }

  //---------------------------------------------------------------------
  // Implementation of WebSocketHandler
  //---------------------------------------------------------------------

  @Override
  public void afterHandshake(RequestContext context, WebSocketSession session) throws Throwable {
    delegate.afterHandshake(context, session);
  }

  @Override
  public void onOpen(WebSocketSession session) {
    delegate.onOpen(session);
  }

  @Override
  public void handleMessage(WebSocketSession session, Message<?> message) {
    delegate.handleMessage(session, message);
  }

  @Override
  public void onClose(WebSocketSession session) {
    delegate.onClose(session);
  }

  @Override
  public void onClose(WebSocketSession session, CloseStatus status) {
    delegate.onClose(session, status);
  }

  @Override
  public void onError(WebSocketSession session, Throwable throwable) {
    delegate.onError(session, throwable);
  }

  @Override
  protected void throwNotSupportMessage(Message<?> message) {
    delegate.throwNotSupportMessage(message);
  }

  @Override
  protected void handlePingMessage(WebSocketSession session, PingMessage message) {
    delegate.handlePingMessage(session, message);
  }

  @Override
  protected void handlePongMessage(WebSocketSession session, PongMessage message) {
    delegate.handlePongMessage(session, message);
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) {
    delegate.handleTextMessage(session, message);
  }

  @Override
  protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
    delegate.handleBinaryMessage(session, message);
  }

  @Override
  public boolean supportPartialMessage() {
    return delegate.supportPartialMessage();
  }

  public String toString() {
    return getClass().getSimpleName() + " [delegate=" + this.delegate + "]";
  }

}
