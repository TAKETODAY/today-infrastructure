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

import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextHolder;
import jakarta.websocket.server.ServerEndpointConfig;

/**
 * jakarta.websocket
 *
 * @author TODAY 2021/5/3 23:17
 */
public abstract class AbstractStandardWebSocketHandlerAdapter extends AbstractWebSocketHandlerAdapter {
  protected ServerEndpointConfig.Configurator configurator;

  protected ServerEndpointConfig getServerEndpointConfig(WebSocketHandler handler) {
    if (handler instanceof StandardWebSocketHandler) {
      return ((StandardWebSocketHandler) handler).getEndpointConfig();
    }
    final RequestContext context = RequestContextHolder.getRequired();
    return new StandardServerEndpointConfig(context.getRequestURI(), configurator);
  }

  @Override
  protected WebSocketSession createSession(RequestContext context, WebSocketHandler handler) {
    return new StandardWebSocketSession(context.requestHeaders(), null, null);
  }
}
