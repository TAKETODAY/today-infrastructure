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

package infra.http.client.config;

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
import infra.http.client.ClientHttpRequestFactoryBuilder;
import infra.http.client.ClientHttpRequestFactoryBuilderCustomizer;
import infra.http.client.HttpClientSettings;
import infra.http.client.JdkClientHttpRequestFactoryBuilder;
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
