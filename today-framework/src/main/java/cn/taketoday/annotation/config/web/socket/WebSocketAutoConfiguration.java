/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.annotation.config.web.socket;

import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.websocket.server.WsSci;
import org.eclipse.jetty.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer;

import cn.taketoday.beans.factory.annotation.DisableAllDependencyInjection;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.config.AutoConfiguration;
import cn.taketoday.context.annotation.config.EnableAutoConfiguration;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.framework.annotation.ConditionalOnWebApplication;
import cn.taketoday.framework.annotation.ConditionalOnWebApplication.Type;
import cn.taketoday.framework.web.netty.NettyRequestUpgradeStrategy;
import cn.taketoday.stereotype.Component;
import cn.taketoday.web.socket.config.EnableWebSocket;
import cn.taketoday.web.socket.server.RequestUpgradeStrategy;
import cn.taketoday.web.socket.server.support.WebSocketHandlerMapping;

/**
 * {@link EnableAutoConfiguration Auto-configuration} WebSocket
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/2/5 21:52
 */
@EnableWebSocket
@AutoConfiguration
@DisableAllDependencyInjection
@ConditionalOnClass(WebSocketHandlerMapping.class)
public class WebSocketAutoConfiguration {

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass({ Tomcat.class, WsSci.class })
  static class TomcatWebSocketConfiguration {

    @Component
    @ConditionalOnMissingBean(name = "websocketServletWebServerCustomizer")
    TomcatWebSocketServletWebServerCustomizer websocketServletWebServerCustomizer() {
      return new TomcatWebSocketServletWebServerCustomizer();
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(JakartaWebSocketServletContainerInitializer.class)
  static class JettyWebSocketConfiguration {

    @Component
    @ConditionalOnMissingBean(name = "websocketServletWebServerCustomizer")
    JettyWebSocketServletWebServerCustomizer websocketServletWebServerCustomizer() {
      return new JettyWebSocketServletWebServerCustomizer();
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(io.undertow.websockets.jsr.Bootstrap.class)
  static class UndertowWebSocketConfiguration {

    @Component
    @ConditionalOnMissingBean(name = "websocketServletWebServerCustomizer")
    UndertowWebSocketServletWebServerCustomizer websocketServletWebServerCustomizer() {
      return new UndertowWebSocketServletWebServerCustomizer();
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(io.netty.handler.codec.http.HttpMethod.class)
  @ConditionalOnWebApplication(type = Type.NETTY)
  static class NettyWebSocketConfiguration {

    @Component
    @ConditionalOnMissingBean
    RequestUpgradeStrategy nettyRequestUpgradeStrategy() {
      return new NettyRequestUpgradeStrategy();
    }

  }

}
