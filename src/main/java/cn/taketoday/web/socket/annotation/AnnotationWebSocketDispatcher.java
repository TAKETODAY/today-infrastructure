/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.web.socket.annotation;

import cn.taketoday.context.factory.BeanFactory;
import cn.taketoday.context.utils.ContextUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.MethodParameter;
import cn.taketoday.web.socket.BinaryMessage;
import cn.taketoday.web.socket.CloseStatus;
import cn.taketoday.web.socket.TextMessage;
import cn.taketoday.web.socket.WebSocketHandler;
import cn.taketoday.web.socket.WebSocketSession;

/**
 * @author TODAY 2021/4/5 12:29
 * @see OnOpen
 * @see OnClose
 * @see OnError
 * @see AfterHandshake
 * @since 3.0
 */
public class AnnotationWebSocketDispatcher extends WebSocketHandler {
  private final BeanFactory beanFactory;
  private final AnnotationWebSocketHandler socketHandler;

  public AnnotationWebSocketDispatcher(AnnotationWebSocketHandler socketHandler, BeanFactory beanFactory) {
    this.socketHandler = socketHandler;
    this.beanFactory = beanFactory;
  }

  @Override
  public void afterHandshake(RequestContext context, WebSocketSession session) throws Throwable {
    context.setAttribute(WebSocketSession.WEBSOCKET_SESSION_KEY, session);
    socketHandler.afterHandshake(context);
    final WebSocketHandlerMethod onOpen = socketHandler.onOpen;
    if (onOpen != null) {
      final MethodParameter[] parameters = onOpen.getParameters();

    }
  }

  @Override
  public void onOpen(WebSocketSession session) {
    final WebSocketHandlerMethod onOpen = socketHandler.onOpen;
    if (onOpen != null) {
      final Object[] parameters = createOnOpenParameters(onOpen, session);
      handle(onOpen, parameters);
    }
  }

  protected Object[] createOnOpenParameters(WebSocketHandlerMethod onOpen, WebSocketSession session) {
    return ContextUtils.resolveParameter(onOpen.getMethod(), beanFactory, new Object[] { session });
  }

  private static void handle(WebSocketHandlerMethod handler, Object[] parameters) {
    if (handler != null) {
      handler.invoke(parameters);
    }
  }

  @Override
  public void onClose(WebSocketSession session, CloseStatus status) {

  }

  @Override
  public void onError(WebSocketSession session, Throwable thr) {

  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) {

  }

  @Override
  protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {

  }

  @Override
  public boolean supportPartialMessage() {
    return false;
  }

  public static class Builder {

  }

}
