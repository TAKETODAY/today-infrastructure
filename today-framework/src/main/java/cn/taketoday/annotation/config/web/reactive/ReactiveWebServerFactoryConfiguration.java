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

package cn.taketoday.annotation.config.web.reactive;

import cn.taketoday.beans.factory.ObjectProvider;
import cn.taketoday.beans.factory.annotation.DisableAllDependencyInjection;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.web.server.reactive.ReactiveWebServerFactory;
import cn.taketoday.web.server.reactive.support.NettyRouteProvider;
import cn.taketoday.web.server.reactive.support.ReactorNettyReactiveWebServerFactory;
import cn.taketoday.web.server.reactive.support.ReactorNettyServerCustomizer;
import cn.taketoday.http.client.ReactorResourceFactory;
import cn.taketoday.stereotype.Component;
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

