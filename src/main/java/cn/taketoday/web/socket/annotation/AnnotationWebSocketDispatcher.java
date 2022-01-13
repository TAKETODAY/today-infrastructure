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

package cn.taketoday.web.socket.annotation;

import java.util.List;
import java.util.Map;

import cn.taketoday.core.PathMatcher;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.socket.CloseStatus;
import cn.taketoday.web.socket.Message;
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
  protected final AnnotationHandlerDelegate socketHandler;
  protected final List<EndpointParameterResolver> resolvers;
  private final boolean supportPartialMessage;

  public AnnotationWebSocketDispatcher(
          AnnotationHandlerDelegate socketHandler, List<EndpointParameterResolver> resolvers, boolean supportPartialMessage) {
    this.resolvers = resolvers;
    this.socketHandler = socketHandler;
    this.supportPartialMessage = supportPartialMessage;
  }

  @Override
  public void afterHandshake(RequestContext context, WebSocketSession session) throws Throwable {
    context.setAttribute(WebSocketSession.WEBSOCKET_SESSION_KEY, session);
    // invoke after handshake callback
    socketHandler.afterHandshake(context);

    if (socketHandler.containsPathVariable) {
      // for path variables handling
      final PathMatcher pathMatcher = (PathMatcher) context.getAttribute(WebSocketSession.PATH_MATCHER);
      final String requestPath = context.getRequestPath();
      final Map<String, String> variables = pathMatcher.extractUriTemplateVariables(socketHandler.pathPattern, requestPath);
      session.setAttribute(WebSocketSession.URI_TEMPLATE_VARIABLES, variables);
    }
  }

  @Override
  public void onOpen(WebSocketSession session) {
    handle(socketHandler.onOpen, session, null);
  }

  protected Object[] resolveParameters(
          WebSocketSession session, WebSocketHandlerMethod handler, Message<?> message, Object... providedArgs) {
    final ResolvableMethodParameter[] parameters = handler.getParameters();
    if (parameters == null) {
      return null;
    }
    final Object[] ret = new Object[parameters.length];
    int i = 0;
    for (final ResolvableMethodParameter parameter : parameters) {
      Object argument = findProvidedArgument(parameter, providedArgs);
      if (argument == null) {
        for (final EndpointParameterResolver resolver : resolvers) {
          if (resolver.supports(parameter)) {
            argument = resolver.resolve(session, message, parameter);
            break;
          }
        }
      }
      ret[i++] = argument;
    }
    return ret;
  }

  protected static Object findProvidedArgument(ResolvableMethodParameter parameter, Object[] providedArgs) {
    if (providedArgs != null) {
      final Class<?> parameterType = parameter.getParameterClass();
      for (final Object providedArg : providedArgs) {
        if (parameterType.isInstance(providedArg)) {
          return providedArg;
        }
      }
    }
    return null;
  }

  protected void handle(
          WebSocketHandlerMethod handler, WebSocketSession session, Message<?> message, Object... providedArgs) {
    if (handler != null) {
      final Object[] parameters = resolveParameters(session, handler, message, providedArgs);
      handler.invoke(parameters);
    }
  }

  @Override
  public void onClose(WebSocketSession session, CloseStatus status) {
    handle(socketHandler.onClose, session, null, status);
  }

  @Override
  public void onError(WebSocketSession session, Throwable thr) {
    handle(socketHandler.onError, session, null, thr);
  }

  @Override
  public void handleMessage(WebSocketSession session, Message<?> message) {
    handle(socketHandler.onMessage, session, message, message);
  }

  @Override
  public boolean supportPartialMessage() {
    return supportPartialMessage;
  }

}
