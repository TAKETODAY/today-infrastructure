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

package cn.taketoday.annotation.config.web.socket;

import cn.taketoday.context.annotation.Lazy;
import cn.taketoday.context.annotation.config.DisableDIAutoConfiguration;
import cn.taketoday.context.annotation.config.EnableAutoConfiguration;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.core.Decorator;
import cn.taketoday.framework.annotation.ConditionalOnWebApplication;
import cn.taketoday.framework.annotation.ConditionalOnWebApplication.Type;
import cn.taketoday.web.socket.server.support.NettyRequestUpgradeStrategy;
import cn.taketoday.lang.Nullable;
import cn.taketoday.stereotype.Component;
import cn.taketoday.web.socket.WebSocketSession;
import cn.taketoday.web.socket.config.EnableWebSocket;
import cn.taketoday.web.socket.server.RequestUpgradeStrategy;
import cn.taketoday.web.socket.server.support.WebSocketHandlerMapping;

/**
 * {@link EnableAutoConfiguration Auto-configuration} WebSocket
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/2/5 21:52
 */
@Lazy
@EnableWebSocket
@DisableDIAutoConfiguration
@ConditionalOnClass(WebSocketHandlerMapping.class)
public class WebSocketAutoConfiguration {

  @Component
  @ConditionalOnClass(io.netty.handler.codec.http.HttpMethod.class)
  @ConditionalOnWebApplication(type = Type.NETTY)
  @ConditionalOnMissingBean
  static RequestUpgradeStrategy nettyRequestUpgradeStrategy(@Nullable Decorator<WebSocketSession> sessionDecorator) {
    return new NettyRequestUpgradeStrategy(sessionDecorator);
  }

}
