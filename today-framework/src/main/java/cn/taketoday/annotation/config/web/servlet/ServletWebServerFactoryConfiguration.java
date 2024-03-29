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

package cn.taketoday.annotation.config.web.servlet;

import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.UpgradeProtocol;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.Loader;
import org.xnio.SslClientAuthMode;

import cn.taketoday.beans.factory.ObjectProvider;
import cn.taketoday.beans.factory.annotation.DisableAllDependencyInjection;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.context.condition.SearchStrategy;
import cn.taketoday.framework.web.embedded.jetty.JettyServerCustomizer;
import cn.taketoday.framework.web.embedded.jetty.JettyServletWebServerFactory;
import cn.taketoday.framework.web.embedded.tomcat.TomcatConnectorCustomizer;
import cn.taketoday.framework.web.embedded.tomcat.TomcatContextCustomizer;
import cn.taketoday.framework.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import cn.taketoday.framework.web.embedded.tomcat.TomcatServletWebServerFactory;
import cn.taketoday.framework.web.embedded.undertow.UndertowBuilderCustomizer;
import cn.taketoday.framework.web.embedded.undertow.UndertowDeploymentInfoCustomizer;
import cn.taketoday.framework.web.embedded.undertow.UndertowServletWebServerFactory;
import cn.taketoday.framework.web.server.ServerProperties;
import cn.taketoday.framework.web.servlet.server.ServletWebServerFactory;
import cn.taketoday.stereotype.Component;
import io.undertow.Undertow;
import jakarta.servlet.Servlet;

/**
 * Configuration classes for servlet web servers
 * <p>
 * Those should be {@code @Import} in a regular auto-configuration class to guarantee
 * their order of execution.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Ivan Sopov
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @author Raheela Asalm
 * @author Sergey Serdyuk
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/27 21:51
 */
@DisableAllDependencyInjection
class ServletWebServerFactoryConfiguration {

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass({ Servlet.class, Tomcat.class, UpgradeProtocol.class })
  @ConditionalOnMissingBean(value = ServletWebServerFactory.class, search = SearchStrategy.CURRENT)
  static class EmbeddedTomcat {

    @Component
    static TomcatServletWebServerFactory tomcatServletWebServerFactory(
            ObjectProvider<TomcatContextCustomizer> contextCustomizers,
            ObjectProvider<TomcatConnectorCustomizer> connectorCustomizers,
            ObjectProvider<TomcatProtocolHandlerCustomizer<?>> protocolHandlerCustomizers) {
      TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
      contextCustomizers.addOrderedTo(factory.getTomcatContextCustomizers());
      connectorCustomizers.addOrderedTo(factory.getTomcatConnectorCustomizers());
      protocolHandlerCustomizers.addOrderedTo(factory.getTomcatProtocolHandlerCustomizers());
      return factory;
    }

  }

  /**
   * Nested configuration if Jetty is being used.
   */
  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass({ Servlet.class, Server.class, Loader.class, WebAppContext.class })
  @ConditionalOnMissingBean(value = ServletWebServerFactory.class, search = SearchStrategy.CURRENT)
  static class EmbeddedJetty {

    @Component
    static JettyServletWebServerFactory JettyServletWebServerFactory(
            ObjectProvider<JettyServerCustomizer> serverCustomizers) {
      JettyServletWebServerFactory factory = new JettyServletWebServerFactory();
      serverCustomizers.addOrderedTo(factory.getServerCustomizers());
      return factory;
    }

  }

  /**
   * Nested configuration if Undertow is being used.
   */
  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass({ Servlet.class, Undertow.class, SslClientAuthMode.class })
  @ConditionalOnMissingBean(value = ServletWebServerFactory.class, search = SearchStrategy.CURRENT)
  static class EmbeddedUndertow {

    @Component
    static UndertowServletWebServerFactory undertowServletWebServerFactory(
            ObjectProvider<UndertowDeploymentInfoCustomizer> deploymentInfoCustomizers,
            ObjectProvider<UndertowBuilderCustomizer> builderCustomizers) {
      UndertowServletWebServerFactory factory = new UndertowServletWebServerFactory();
      builderCustomizers.addOrderedTo(factory.getBuilderCustomizers());
      deploymentInfoCustomizers.addOrderedTo(factory.getDeploymentInfoCustomizers());
      return factory;
    }

    @Component
    static UndertowServletWebServerFactoryCustomizer undertowServletWebServerFactoryCustomizer(
            ServerProperties serverProperties) {
      return new UndertowServletWebServerFactoryCustomizer(serverProperties);
    }

  }

}
