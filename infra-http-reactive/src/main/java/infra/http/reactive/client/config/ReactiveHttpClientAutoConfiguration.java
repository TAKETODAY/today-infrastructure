/*
 * Copyright 2012-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.http.reactive.client.config;

import java.util.List;

import infra.beans.factory.ObjectProvider;
import infra.context.annotation.Conditional;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.context.annotation.Lazy;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.condition.Threading;
import infra.context.properties.EnableConfigurationProperties;
import infra.core.annotation.Order;
import infra.core.env.Environment;
import infra.core.io.ResourceLoader;
import infra.core.task.VirtualThreadTaskExecutor;
import infra.http.client.HttpClientSettings;
import infra.http.reactive.client.ClientHttpConnector;
import infra.http.reactive.client.ClientHttpConnectorBuilder;
import infra.http.reactive.client.JdkClientHttpConnectorBuilder;
import infra.http.reactive.client.ReactorClientHttpConnectorBuilder;
import infra.http.support.ReactorNettyConfigurations;
import infra.http.support.ReactorResourceFactory;
import infra.stereotype.Component;
import infra.util.LambdaSafe;
import reactor.core.publisher.Mono;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for reactive HTTP clients.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see infra.http.client.config.HttpClientAutoConfiguration
 * @since 5.0
 */
@DisableDIAutoConfiguration(afterName = "infra.http.client.config.HttpClientAutoConfiguration")
@ConditionalOnClass({ ClientHttpConnector.class, Mono.class })
@Conditional(ConditionalOnClientHttpConnectorBuilderDetection.class)
@EnableConfigurationProperties(ReactiveHttpClientsProperties.class)
public final class ReactiveHttpClientAutoConfiguration {

  @Component
  @ConditionalOnMissingBean
  static ClientHttpConnectorBuilder<?> clientHttpConnectorBuilder(ResourceLoader resourceLoader,
          ReactiveHttpClientsProperties properties, Environment environment,
          ObjectProvider<ClientHttpConnectorBuilderCustomizer<?>> clientHttpConnectorBuilderCustomizers) {
    ClientHttpConnectorBuilder<?> builder = properties.connector != null
            ? properties.connector.builder()
            : ClientHttpConnectorBuilder.detect(resourceLoader.getClassLoader());
    if (builder instanceof JdkClientHttpConnectorBuilder jdk && Threading.VIRTUAL.isActive(environment)) {
      builder = jdk.withExecutor(new VirtualThreadTaskExecutor("httpclient-"));
    }
    return customize(builder, clientHttpConnectorBuilderCustomizers.orderedStream().toList());
  }

  @SuppressWarnings("unchecked")
  private static ClientHttpConnectorBuilder<?> customize(ClientHttpConnectorBuilder<?> builder,
          List<ClientHttpConnectorBuilderCustomizer<?>> customizers) {
    ClientHttpConnectorBuilder<?>[] builderReference = { builder };
    LambdaSafe.callbacks(ClientHttpConnectorBuilderCustomizer.class, customizers, builderReference[0])
            .invoke((customizer) -> builderReference[0] = customizer.customize(builderReference[0]));
    return builderReference[0];
  }

  @Component
  @Lazy
  @ConditionalOnMissingBean
  static ClientHttpConnector clientHttpConnector(ResourceLoader resourceLoader,
          ObjectProvider<ClientHttpConnectorBuilder<?>> clientHttpConnectorBuilder,
          ObjectProvider<HttpClientSettings> httpClientSettings) {
    return clientHttpConnectorBuilder
            .getIfAvailable(() -> ClientHttpConnectorBuilder.detect(resourceLoader.getClassLoader()))
            .build(httpClientSettings.getIfAvailable(HttpClientSettings::defaults));
  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass({ reactor.netty.http.client.HttpClient.class, ReactorNettyConfigurations.class })
  @Import(ReactorNettyConfigurations.ReactorResourceFactoryConfiguration.class)
  static class ReactorNetty {

    @Component
    @Order(0)
    static ClientHttpConnectorBuilderCustomizer<ReactorClientHttpConnectorBuilder> reactorResourceFactoryClientHttpConnectorBuilderCustomizer(
            ReactorResourceFactory reactorResourceFactory) {
      return (builder) -> builder.withReactorResourceFactory(reactorResourceFactory);
    }

  }

}
