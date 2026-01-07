/*
 * Copyright 2017 - 2026 the original author or authors.
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

package infra.annotation.config.http.client.reactive;

import java.util.List;

import infra.annotation.config.http.client.HttpClientAutoConfiguration;
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
import infra.http.client.ReactorResourceFactory;
import infra.http.client.config.HttpClientSettings;
import infra.http.client.config.reactive.ClientHttpConnectorBuilder;
import infra.http.client.config.reactive.JdkClientHttpConnectorBuilder;
import infra.http.client.config.reactive.ReactorClientHttpConnectorBuilder;
import infra.http.client.reactive.ClientHttpConnector;
import infra.http.config.annotation.ReactorNettyConfigurations;
import infra.stereotype.Component;
import infra.util.LambdaSafe;
import reactor.core.publisher.Mono;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for reactive HTTP clients.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see HttpClientAutoConfiguration
 * @since 5.0
 */
@DisableDIAutoConfiguration(after = HttpClientAutoConfiguration.class)
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
