/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.framework.web.embedded.config;

import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.UpgradeProtocol;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.Loader;
import org.eclipse.jetty.webapp.WebAppContext;
import org.xnio.SslClientAuthMode;

import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.config.AutoConfiguration;
import cn.taketoday.context.annotation.config.EnableAutoConfiguration;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.core.env.Environment;
import cn.taketoday.framework.annotation.ConditionalOnWebApplication;
import cn.taketoday.framework.web.server.ServerProperties;
import io.undertow.Undertow;
import reactor.netty.http.server.HttpServer;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for embedded servlet and reactive
 * web servers customizations.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@AutoConfiguration
@ConditionalOnWebApplication
@EnableConfigurationProperties(ServerProperties.class)
public class EmbeddedWebServerFactoryCustomizerAutoConfiguration {

  /**
   * Nested configuration if Tomcat is being used.
   */
  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass({ Tomcat.class, UpgradeProtocol.class })
  public static class TomcatWebServerFactoryCustomizerConfiguration {

    @Bean
    public TomcatWebServerFactoryCustomizer tomcatWebServerFactoryCustomizer(Environment environment,
            ServerProperties serverProperties) {
      return new TomcatWebServerFactoryCustomizer(environment, serverProperties);
    }

  }

  /**
   * Nested configuration if Jetty is being used.
   */
  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass({ Server.class, Loader.class, WebAppContext.class })
  public static class JettyWebServerFactoryCustomizerConfiguration {

    @Bean
    public JettyWebServerFactoryCustomizer jettyWebServerFactoryCustomizer(Environment environment,
            ServerProperties serverProperties) {
      return new JettyWebServerFactoryCustomizer(environment, serverProperties);
    }

  }

  /**
   * Nested configuration if Undertow is being used.
   */
  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass({ Undertow.class, SslClientAuthMode.class })
  public static class UndertowWebServerFactoryCustomizerConfiguration {

    @Bean
    public UndertowWebServerFactoryCustomizer undertowWebServerFactoryCustomizer(Environment environment,
            ServerProperties serverProperties) {
      return new UndertowWebServerFactoryCustomizer(environment, serverProperties);
    }

  }

  /**
   * Nested configuration if Netty is being used.
   */
  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(HttpServer.class)
  public static class NettyWebServerFactoryCustomizerConfiguration {

    @Bean
    public NettyWebServerFactoryCustomizer nettyWebServerFactoryCustomizer(Environment environment,
            ServerProperties serverProperties) {
      return new NettyWebServerFactoryCustomizer(environment, serverProperties);
    }

  }

}
