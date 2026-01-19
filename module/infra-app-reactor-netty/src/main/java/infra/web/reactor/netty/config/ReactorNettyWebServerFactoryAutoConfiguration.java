/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.web.reactor.netty.config;

import infra.app.config.ConditionalOnWebApplication;
import infra.beans.factory.ObjectProvider;
import infra.context.annotation.Import;
import infra.context.annotation.Lazy;
import infra.context.annotation.config.AutoConfigureOrder;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.condition.ConditionalOnProperty;
import infra.context.properties.EnableConfigurationProperties;
import infra.core.Ordered;
import infra.core.env.Environment;
import infra.http.reactive.ReactiveHttpInputMessage;
import infra.http.support.ReactorResourceFactory;
import infra.stereotype.Component;
import infra.web.reactive.server.ForwardedHeaderTransformer;
import infra.web.reactor.netty.NettyRouteProvider;
import infra.web.reactor.netty.ReactorNettyReactiveWebServerFactory;
import infra.web.reactor.netty.ReactorNettyServerCustomizer;
import infra.web.reactor.netty.ReactorServerProperties;
import infra.http.support.ReactorNettyConfigurations.ReactorResourceFactoryConfiguration;
import infra.web.server.config.ServerProperties;
import infra.web.server.config.WebServerConfiguration;
import infra.web.server.reactive.ReactiveWebServerFactory;
import reactor.netty.http.server.HttpServer;

import static infra.app.config.ConditionalOnWebApplication.Type;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for a reactive web server.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Brian Clozel
 * @since 4.0 2022/10/21 12:12
 */
@Lazy
@DisableDIAutoConfiguration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnClass(ReactiveHttpInputMessage.class)
@ConditionalOnWebApplication(type = Type.REACTIVE)
@EnableConfigurationProperties({ ServerProperties.class, ReactorServerProperties.class })
@Import({ WebServerConfiguration.class, ReactorResourceFactoryConfiguration.class })
public final class ReactorNettyWebServerFactoryAutoConfiguration {

  @Component
  @ConditionalOnClass({ HttpServer.class })
  @ConditionalOnMissingBean(ReactiveWebServerFactory.class)
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

  @Component
  static ReactorNettyWebServerFactoryCustomizer nettyWebServerFactoryCustomizer(
          Environment environment, ServerProperties serverProperties, ReactorServerProperties reactorProperties) {
    return new ReactorNettyWebServerFactoryCustomizer(environment, serverProperties, reactorProperties);
  }

  @Component
  @ConditionalOnMissingBean
  @ConditionalOnProperty(value = "server.forward-headers-strategy", havingValue = "framework")
  public static ForwardedHeaderTransformer forwardedHeaderTransformer() {
    return new ForwardedHeaderTransformer();
  }

}
