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

import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.web.socket.WebSocketHandlerRegistry;

import javax.websocket.server.ServerEndpoint;
import java.lang.reflect.Method;

/**
 * javax.websocket.*
 *
 * @author TODAY 2021/5/8 22:08
 * @since 3.0.1
 */
public class StandardWebSocketHandlerRegistry extends WebSocketHandlerRegistry {

  public StandardWebSocketHandlerRegistry() { }

  public StandardWebSocketHandlerRegistry(AnnotationWebSocketHandlerBuilder annotationHandlerBuilder) {
    super(annotationHandlerBuilder);
  }

  @Override
  protected boolean isOnMessageHandler(Method declaredMethod, BeanDefinition definition) {
    return super.isOnMessageHandler(declaredMethod, definition)
            || declaredMethod.isAnnotationPresent(javax.websocket.OnMessage.class);
  }

  @Override
  protected boolean isOnCloseHandler(Method declaredMethod, BeanDefinition definition) {
    return super.isOnCloseHandler(declaredMethod, definition)
            || declaredMethod.isAnnotationPresent(javax.websocket.OnClose.class);
  }

  @Override
  protected boolean isOnOpenHandler(Method declaredMethod, BeanDefinition definition) {
    return super.isOnOpenHandler(declaredMethod, definition)
            || declaredMethod.isAnnotationPresent(javax.websocket.OnOpen.class);
  }

  @Override
  protected boolean isOnErrorHandler(Method declaredMethod, BeanDefinition definition) {
    return super.isOnErrorHandler(declaredMethod, definition)
            || declaredMethod.isAnnotationPresent(javax.websocket.OnError.class);
  }

  @Override
  protected boolean isEndpoint(BeanDefinition definition) {
    return super.isEndpoint(definition) || definition.isAnnotationPresent(ServerEndpoint.class);
  }

}
