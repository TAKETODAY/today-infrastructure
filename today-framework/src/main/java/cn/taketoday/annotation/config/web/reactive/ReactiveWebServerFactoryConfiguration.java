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

package cn.taketoday.annotation.config.web.reactive;

import org.eclipse.jetty.ee10.servlet.ServletHolder;

import cn.taketoday.beans.factory.ObjectProvider;
import cn.taketoday.beans.factory.annotation.DisableAllDependencyInjection;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.framework.web.embedded.jetty.JettyReactiveWebServerFactory;
import cn.taketoday.framework.web.embedded.jetty.JettyServerCustomizer;
import cn.taketoday.framework.web.embedded.netty.NettyRouteProvider;
import cn.taketoday.framework.web.embedded.netty.ReactorNettyReactiveWebServerFactory;
import cn.taketoday.framework.web.embedded.netty.ReactorNettyServerCustomizer;
import cn.taketoday.framework.web.embedded.tomcat.TomcatConnectorCustomizer;
import cn.taketoday.framework.web.embedded.tomcat.TomcatContextCustomizer;
import cn.taketoday.framework.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import cn.taketoday.framework.web.embedded.tomcat.TomcatReactiveWebServerFactory;
import cn.taketoday.framework.web.embedded.undertow.UndertowBuilderCustomizer;
import cn.taketoday.framework.web.embedded.undertow.UndertowReactiveWebServerFactory;
import cn.taketoday.framework.web.reactive.server.ReactiveWebServerFactory;
import cn.taketoday.http.client.reactive.JettyResourceFactory;
import cn.taketoday.http.client.ReactorResourceFactory;
import cn.taketoday.stereotype.Component;
import io.undertow.Undertow;
import reactor.netty.http.server.HttpServer;

/**
 * Configuration classes for reactive web servers
 * <p>
 * Those should be {@code @Import} in a regular auto-configuration class to guarantee
 * their order of execution.
 *
 * @author Brian Clozel
 * @author Raheela Aslam
 * @author Sergey Serdyuk
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/21 12:12
 */
@DisableAllDependencyInjection
abstract class ReactiveWebServerFactoryConfiguration {

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass({ org.apache.catalina.startup.Tomcat.class })
  @ConditionalOnMissingBean(ReactiveWebServerFactory.class)
  static class EmbeddedTomcat {

    @Component
    static TomcatReactiveWebServerFactory tomcatReactiveWebServerFactory(
            ObjectProvider<TomcatContextCustomizer> contextCustomizers,
            ObjectProvider<TomcatConnectorCustomizer> connectorCustomizers,
            ObjectProvider<TomcatProtocolHandlerCustomizer<?>> protocolHandlerCustomizers) {
      TomcatReactiveWebServerFactory factory = new TomcatReactiveWebServerFactory();
      contextCustomizers.addOrderedTo(factory.getTomcatContextCustomizers());
      connectorCustomizers.addOrderedTo(factory.getTomcatConnectorCustomizers());
      protocolHandlerCustomizers.addOrderedTo(factory.getTomcatProtocolHandlerCustomizers());
      return factory;
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass({ org.eclipse.jetty.server.Server.class, ServletHolder.class })
  @ConditionalOnMissingBean(ReactiveWebServerFactory.class)
  static class EmbeddedJetty {

    @Component
    @ConditionalOnMissingBean
    static JettyResourceFactory jettyServerResourceFactory() {
      return new JettyResourceFactory();
    }

    @Component
    static JettyReactiveWebServerFactory jettyReactiveWebServerFactory(
            JettyResourceFactory resourceFactory, ObjectProvider<JettyServerCustomizer> serverCustomizers) {
      JettyReactiveWebServerFactory serverFactory = new JettyReactiveWebServerFactory();
      serverCustomizers.addOrderedTo(serverFactory.getServerCustomizers());
      serverFactory.setResourceFactory(resourceFactory);
      return serverFactory;
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass({ Undertow.class })
  @ConditionalOnMissingBean(ReactiveWebServerFactory.class)
  static class EmbeddedUndertow {

    @Component
    static UndertowReactiveWebServerFactory undertowReactiveWebServerFactory(
            ObjectProvider<UndertowBuilderCustomizer> builderCustomizers) {
      UndertowReactiveWebServerFactory factory = new UndertowReactiveWebServerFactory();
      builderCustomizers.addOrderedTo(factory.getBuilderCustomizers());
      return factory;
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass({ HttpServer.class })
  @ConditionalOnMissingBean(ReactiveWebServerFactory.class)
  static class EmbeddedNetty {

    @Component
    @ConditionalOnMissingBean
    static ReactorResourceFactory reactorServerResourceFactory() {
      return new ReactorResourceFactory();
    }

    @Component
    static ReactorNettyReactiveWebServerFactory reactorNettyReactiveWebServerFactory(ReactorResourceFactory resourceFactory,
            ObjectProvider<NettyRouteProvider> routes, ObjectProvider<ReactorNettyServerCustomizer> serverCustomizers) {

      ReactorNettyReactiveWebServerFactory serverFactory = new ReactorNettyReactiveWebServerFactory();
      serverFactory.setResourceFactory(resourceFactory);
      for (NettyRouteProvider route : routes) {
        serverFactory.addRouteProviders(route);
      }
      serverCustomizers.addOrderedTo(serverFactory.getServerCustomizers());
      return serverFactory;
    }

  }

}

