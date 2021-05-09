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

package cn.taketoday.web.socket;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.web.socket.annotation.StandardWebSocketHandlerRegistry;
import cn.taketoday.web.socket.tomcat.TomcatWebSocketHandlerAdapter;

/**
 * @author TODAY 2021/4/5 12:14
 * @since 3.0
 */
@Import(WebSocketConfig.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface EnableWebSocket {

}

class WebSocketConfig {

  @MissingBean(type = AbstractWebSocketHandlerAdapter.class)
  TomcatWebSocketHandlerAdapter webSocketHandlerAdapter() {
    return new TomcatWebSocketHandlerAdapter();
  }

  @MissingBean
  WebSocketHandlerRegistry webSocketHandlerRegistry(final List<WebSocketConfiguration> configurers) {
    WebSocketHandlerRegistry handlerRegistry;
    if (ClassUtils.isPresent("javax.websocket.Session")) {
      handlerRegistry = new StandardWebSocketHandlerRegistry();
    }
    else {
      handlerRegistry = new WebSocketHandlerRegistry();
    }

    for (final WebSocketConfiguration configurer : configurers) {
      configurer.configureWebSocketHandlers(handlerRegistry);
    }
    return handlerRegistry;
  }

}
