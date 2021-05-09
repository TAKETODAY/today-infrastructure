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

import cn.taketoday.web.handler.MethodParameter;
import cn.taketoday.web.socket.NativeWebSocketSession;
import cn.taketoday.web.socket.WebSocketSession;

/**
 * @author TODAY 2021/5/9 22:31
 * @since 3.0.1
 */
public class WebSocketSessionEndpointParameterResolver implements EndpointParameterResolver {

  @Override
  public boolean supports(MethodParameter parameter) {
    return parameter.isAssignableTo(WebSocketSession.class);
  }

  @Override
  public Object resolve(WebSocketSession session, MethodParameter parameter) {
    return session;
  }

  static Object getNativeSessionSession(WebSocketSession session, MethodParameter parameter) {
    if (session instanceof NativeWebSocketSession) {
      final Object nativeSession = ((NativeWebSocketSession) session).obtainNativeSession();
      if (parameter.isInstance(nativeSession)) {
        return nativeSession;
      }
    }
    return null;
  }

}
