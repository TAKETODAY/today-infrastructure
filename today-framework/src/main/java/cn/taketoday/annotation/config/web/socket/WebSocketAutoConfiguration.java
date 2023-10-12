/*
 * Copyright 2017 - 2023 the original author or authors.
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
import org.eclipse.jetty.ee10.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer;
import org.eclipse.jetty.ee10.websocket.servlet.WebSocketUpgradeFilter;

import java.util.List;

import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Lazy;
import cn.taketoday.context.annotation.config.DisableDIAutoConfiguration;
import cn.taketoday.context.annotation.config.EnableAutoConfiguration;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.core.Decorator;
import cn.taketoday.core.Ordered;
import cn.taketoday.framework.annotation.ConditionalOnWebApplication;
import cn.taketoday.framework.annotation.ConditionalOnWebApplication.Type;
import cn.taketoday.framework.web.netty.NettyRequestUpgradeStrategy;
import cn.taketoday.framework.web.servlet.FilterRegistrationBean;
import cn.taketoday.lang.Nullable;
import cn.taketoday.stereotype.Component;
import cn.taketoday.web.socket.WebSocketSession;
import cn.taketoday.web.socket.config.EnableWebSocket;
import cn.taketoday.web.socket.server.RequestUpgradeStrategy;
import cn.taketoday.web.socket.server.support.WebSocketHandlerMapping;
import jakarta.servlet.DispatcherType;

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

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass({ Tomcat.class, WsSci.class })
  static class TomcatWebSocketConfiguration {

    @Component
    @ConditionalOnMissingBean(name = "websocketServletWebServerCustomizer")
    static TomcatWebSocketServletWebServerCustomizer websocketServletWebServerCustomizer() {
      return new TomcatWebSocketServletWebServerCustomizer();
    }

  }

  @Lazy
  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(JakartaWebSocketServletContainerInitializer.class)
  static class JettyWebSocketConfiguration {

    @Component
    @ConditionalOnMissingBean(name = "websocketServletWebServerCustomizer")
    static JettyWebSocketServletWebServerCustomizer websocketServletWebServerCustomizer() {
      return new JettyWebSocketServletWebServerCustomizer();
    }

    @Component
    @ConditionalOnMissingBean(
            value = WebSocketUpgradeFilter.class,
            parameterizedContainer = FilterRegistrationBean.class)
    static FilterRegistrationBean<WebSocketUpgradeFilter> webSocketUpgradeFilter() {
      WebSocketUpgradeFilter websocketFilter = new WebSocketUpgradeFilter();
      FilterRegistrationBean<WebSocketUpgradeFilter> registration = new FilterRegistrationBean<>(websocketFilter);
      registration.setAsyncSupported(true);
      registration.setDispatcherTypes(DispatcherType.REQUEST);
      registration.setName(WebSocketUpgradeFilter.class.getName());
      registration.setOrder(Ordered.LOWEST_PRECEDENCE);
      registration.setUrlPatterns(List.of("/*"));
      return registration;
    }

  }

  @Lazy
  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(io.undertow.websockets.jsr.Bootstrap.class)
  static class UndertowWebSocketConfiguration {

    @Component
    @ConditionalOnMissingBean(name = "websocketServletWebServerCustomizer")
    static UndertowWebSocketServletWebServerCustomizer websocketServletWebServerCustomizer() {
      return new UndertowWebSocketServletWebServerCustomizer();
    }

  }

  @Lazy
  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(io.netty.handler.codec.http.HttpMethod.class)
  @ConditionalOnWebApplication(type = Type.NETTY)
  static class NettyWebSocketConfiguration {

    @Component
    @ConditionalOnMissingBean
    static RequestUpgradeStrategy nettyRequestUpgradeStrategy(@Nullable Decorator<WebSocketSession> sessionDecorator) {
      return new NettyRequestUpgradeStrategy(sessionDecorator);
    }

  }

}
