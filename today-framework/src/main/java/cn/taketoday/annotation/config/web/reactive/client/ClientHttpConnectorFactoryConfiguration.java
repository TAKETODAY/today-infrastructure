/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.annotation.config.web.reactive.client;

import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.core5.http.nio.AsyncRequestProducer;
import org.apache.hc.core5.reactive.ReactiveResponseConsumer;

import cn.taketoday.beans.factory.ObjectProvider;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.http.client.reactive.JettyResourceFactory;
import cn.taketoday.http.client.reactive.ReactorResourceFactory;
import cn.taketoday.stereotype.Component;

/**
 * Configuration classes for WebClient client connectors.
 * <p>
 * Those should be {@code @Import} in a regular auto-configuration class to guarantee
 * their order of execution.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class ClientHttpConnectorFactoryConfiguration {

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(reactor.netty.http.client.HttpClient.class)
  @ConditionalOnMissingBean(ClientHttpConnectorFactory.class)
  static class ReactorNetty {

    @Component
    @ConditionalOnMissingBean
    ReactorResourceFactory reactorServerResourceFactory() {
      return new ReactorResourceFactory();
    }

    @Component
    ReactorClientHttpConnectorFactory reactorClientHttpConnectorFactory(
            ReactorResourceFactory reactorResourceFactory,
            ObjectProvider<ReactorNettyHttpClientMapper> mapperProvider) {
      return new ReactorClientHttpConnectorFactory(reactorResourceFactory, mapperProvider::orderedStream);
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(org.eclipse.jetty.reactive.client.ReactiveRequest.class)
  @ConditionalOnMissingBean(ClientHttpConnectorFactory.class)
  static class JettyClient {

    @Component
    @ConditionalOnMissingBean
    JettyResourceFactory jettyClientResourceFactory() {
      return new JettyResourceFactory();
    }

    @Component
    JettyClientHttpConnectorFactory jettyClientHttpConnectorFactory(JettyResourceFactory jettyResourceFactory) {
      return new JettyClientHttpConnectorFactory(jettyResourceFactory);
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass({ HttpAsyncClients.class, AsyncRequestProducer.class, ReactiveResponseConsumer.class })
  @ConditionalOnMissingBean(ClientHttpConnectorFactory.class)
  static class HttpClient5 {

    @Component
    HttpComponentsClientHttpConnectorFactory httpComponentsClientHttpConnectorFactory() {
      return new HttpComponentsClientHttpConnectorFactory();
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(java.net.http.HttpClient.class)
  @ConditionalOnMissingBean(ClientHttpConnectorFactory.class)
  static class JdkClient {

    @Component
    JdkClientHttpConnectorFactory jdkClientHttpConnectorFactory() {
      return new JdkClientHttpConnectorFactory();
    }

  }

}
