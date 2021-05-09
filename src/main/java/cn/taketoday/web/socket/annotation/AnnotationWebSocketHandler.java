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

import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.HandlerMethod;
import cn.taketoday.web.registry.HandlerMethodRegistry;

/**
 * @author TODAY 2021/5/8 22:43
 * @since 3.0.1
 */
public class AnnotationWebSocketHandler {

  protected final String pathPattern;
  protected final HandlerMethod afterHandshake;
  protected final WebSocketHandlerMethod onOpen;
  protected final WebSocketHandlerMethod onClose;
  protected final WebSocketHandlerMethod onError;
  protected final WebSocketHandlerMethod onMessage;

  protected final boolean containsPathVariable;

  public AnnotationWebSocketHandler(String pathPattern,
                                    WebSocketHandlerMethod onOpen,
                                    WebSocketHandlerMethod onClose,
                                    WebSocketHandlerMethod onError,
                                    WebSocketHandlerMethod onMessage,
                                    HandlerMethod afterHandshake) {
    this.pathPattern = pathPattern;
    this.onOpen = onOpen;
    this.onClose = onClose;
    this.onError = onError;
    this.onMessage = onMessage;
    this.afterHandshake = afterHandshake;
    this.containsPathVariable = HandlerMethodRegistry.containsPathVariable(pathPattern);
  }

  public void afterHandshake(RequestContext context) throws Throwable {
    final HandlerMethod afterHandshake = this.afterHandshake;
    if (afterHandshake != null) {
      afterHandshake.handleRequest(context);
    }
  }

}
