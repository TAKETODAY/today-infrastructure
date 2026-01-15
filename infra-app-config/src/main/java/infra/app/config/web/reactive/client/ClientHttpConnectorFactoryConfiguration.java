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

package infra.app.config.web.reactive.client;

import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.core5.http.nio.AsyncRequestProducer;
import org.apache.hc.core5.reactive.ReactiveResponseConsumer;

import infra.beans.factory.ObjectProvider;
import infra.beans.factory.annotation.DisableAllDependencyInjection;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingBean;
import infra.http.client.ReactorResourceFactory;
import infra.http.config.annotation.ReactorNettyConfigurations;
import infra.stereotype.Component;

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
@DisableAllDependencyInjection
class ClientHttpConnectorFactoryConfiguration {

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(reactor.netty.http.client.HttpClient.class)
  @Import(ReactorNettyConfigurations.ReactorResourceFactoryConfiguration.class)
  static class ReactorNetty {

    @Component
    static ReactorClientHttpConnectorFactory reactorClientHttpConnectorFactory(
            ReactorResourceFactory reactorResourceFactory,
            ObjectProvider<ReactorNettyHttpClientMapper> mapperProvider) {
      return new ReactorClientHttpConnectorFactory(reactorResourceFactory, mapperProvider);
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass({ HttpAsyncClients.class, AsyncRequestProducer.class, ReactiveResponseConsumer.class })
  @ConditionalOnMissingBean(ClientHttpConnectorFactory.class)
  static class HttpClient5 {

    @Component
    static HttpComponentsClientHttpConnectorFactory httpComponentsClientHttpConnectorFactory() {
      return new HttpComponentsClientHttpConnectorFactory();
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(java.net.http.HttpClient.class)
  @ConditionalOnMissingBean(ClientHttpConnectorFactory.class)
  static class JdkClient {

    @Component
    static JdkClientHttpConnectorFactory jdkClientHttpConnectorFactory() {
      return new JdkClientHttpConnectorFactory();
    }

  }
}
