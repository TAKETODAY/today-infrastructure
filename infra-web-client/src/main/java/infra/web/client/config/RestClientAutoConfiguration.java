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

package infra.web.client.config;

import java.util.List;

import infra.beans.factory.ObjectProvider;
import infra.context.annotation.Lazy;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.context.condition.ConditionalOnBean;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingBean;
import infra.core.Ordered;
import infra.core.annotation.Order;
import infra.core.io.ResourceLoader;
import infra.core.ssl.SslBundles;
import infra.http.client.ClientHttpRequestFactoryBuilder;
import infra.http.client.HttpClientSettings;
import infra.http.converter.config.ClientHttpMessageConvertersCustomizer;
import infra.stereotype.Component;
import infra.stereotype.Prototype;
import infra.web.client.RestClient;
import infra.web.client.RestClientCustomizer;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link RestClient}.
 * <p>
 * This will produce a {@link infra.web.client.RestClient.Builder
 * RestClient.Builder} bean with the {@code prototype} scope, meaning each injection point
 * will receive a newly cloned instance of the builder.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@Lazy
@DisableDIAutoConfiguration(afterName = {
        "infra.app.config.ssl.SslAutoConfiguration",
        "infra.app.config.task.TaskExecutionAutoConfiguration",
        "infra.http.client.config.ImperativeHttpClientAutoConfiguration"
})
@ConditionalOnClass(RestClient.class)
public final class RestClientAutoConfiguration {

  @Component
  @ConditionalOnBean(SslBundles.class)
  @ConditionalOnMissingBean(RestClientSsl.class)
  static AutoConfiguredRestClientSsl restClientSsl(ResourceLoader resourceLoader,
          ObjectProvider<ClientHttpRequestFactoryBuilder<?>> clientHttpRequestFactoryBuilder,
          ObjectProvider<HttpClientSettings> httpClientSettings, SslBundles sslBundles) {
    ClassLoader classLoader = resourceLoader.getClassLoader();
    return new AutoConfiguredRestClientSsl(
            clientHttpRequestFactoryBuilder.getIfAvailable(() -> ClientHttpRequestFactoryBuilder.detect(classLoader)),
            httpClientSettings.getIfAvailable(HttpClientSettings::defaults), sslBundles);
  }

  @Component
  @ConditionalOnMissingBean
  static RestClientBuilderConfigurer restClientBuilderConfigurer(ResourceLoader resourceLoader,
          ObjectProvider<ClientHttpRequestFactoryBuilder<?>> clientHttpRequestFactoryBuilder,
          ObjectProvider<HttpClientSettings> httpClientSettings, List<RestClientCustomizer> customizers) {
    return new RestClientBuilderConfigurer(clientHttpRequestFactoryBuilder
            .getIfAvailable(() -> ClientHttpRequestFactoryBuilder.detect(resourceLoader.getClassLoader())),
            httpClientSettings.getIfAvailable(HttpClientSettings::defaults), customizers);
  }

  @Prototype
  @ConditionalOnMissingBean
  static RestClient.Builder restClientBuilder(RestClientBuilderConfigurer restClientBuilderConfigurer) {
    return restClientBuilderConfigurer.configure(RestClient.builder());
  }

  @Component
  @ConditionalOnBean(ClientHttpMessageConvertersCustomizer.class)
  @Order(Ordered.LOWEST_PRECEDENCE)
  static HttpMessageConvertersRestClientCustomizer httpMessageConvertersRestClientCustomizer(ObjectProvider<ClientHttpMessageConvertersCustomizer> customizers) {
    return new HttpMessageConvertersRestClientCustomizer(customizers);
  }

}
