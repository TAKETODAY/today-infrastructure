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

package infra.annotation.config.http.client;

import java.util.List;

import infra.context.annotation.Conditional;
import infra.context.annotation.Lazy;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.condition.Threading;
import infra.context.properties.EnableConfigurationProperties;
import infra.core.env.Environment;
import infra.core.io.ResourceLoader;
import infra.core.task.VirtualThreadTaskExecutor;
import infra.http.client.ClientHttpRequestFactory;
import infra.http.client.config.ClientHttpRequestFactoryBuilder;
import infra.http.client.config.HttpClientSettings;
import infra.http.client.config.JdkClientHttpRequestFactoryBuilder;
import infra.stereotype.Component;
import infra.util.LambdaSafe;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for imperative HTTP clients.
 *
 * @author Phillip Webb
 * @author Sangmin Park
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see HttpClientAutoConfiguration
 * @since 5.0
 */
@Conditional(NotReactiveWebApplicationCondition.class)
@DisableDIAutoConfiguration(after = HttpClientAutoConfiguration.class)
@ConditionalOnClass(ClientHttpRequestFactory.class)
@EnableConfigurationProperties(ImperativeHttpClientsProperties.class)
public final class ImperativeHttpClientAutoConfiguration {

  @Component
  @ConditionalOnMissingBean
  static ClientHttpRequestFactoryBuilder<?> clientHttpRequestFactoryBuilder(ResourceLoader resourceLoader,
          ImperativeHttpClientsProperties properties, Environment environment,
          List<ClientHttpRequestFactoryBuilderCustomizer<?>> clientHttpRequestFactoryBuilderCustomizers) {
    var builder = properties.factory != null ? properties.factory.builder()
            : ClientHttpRequestFactoryBuilder.detect(resourceLoader.getClassLoader());

    if (builder instanceof JdkClientHttpRequestFactoryBuilder jdk && Threading.VIRTUAL.isActive(environment)) {
      builder = jdk.withExecutor(new VirtualThreadTaskExecutor("httpclient-"));
    }
    return customize(builder, clientHttpRequestFactoryBuilderCustomizers);
  }

  @SuppressWarnings("unchecked")
  private static ClientHttpRequestFactoryBuilder<?> customize(ClientHttpRequestFactoryBuilder<?> builder,
          List<ClientHttpRequestFactoryBuilderCustomizer<?>> customizers) {
    ClientHttpRequestFactoryBuilder<?>[] builderReference = { builder };
    LambdaSafe.callbacks(ClientHttpRequestFactoryBuilderCustomizer.class, customizers, builderReference[0])
            .invoke((customizer) -> builderReference[0] = customizer.customize(builderReference[0]));
    return builderReference[0];
  }

  @Lazy
  @Component
  @ConditionalOnMissingBean
  static ClientHttpRequestFactory clientHttpRequestFactory(ClientHttpRequestFactoryBuilder<?> clientHttpRequestFactoryBuilder, HttpClientSettings httpClientSettings) {
    return clientHttpRequestFactoryBuilder.build(httpClientSettings);
  }

}
