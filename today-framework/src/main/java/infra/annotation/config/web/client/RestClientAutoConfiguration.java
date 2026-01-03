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

package infra.annotation.config.web.client;

import java.util.List;

import infra.annotation.config.http.ClientHttpMessageConvertersCustomizer;
import infra.annotation.config.http.HttpMessageConvertersAutoConfiguration;
import infra.beans.factory.ObjectProvider;
import infra.context.annotation.Conditional;
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
import infra.stereotype.Component;
import infra.stereotype.Prototype;
import infra.web.client.RestClient;
import infra.http.client.config.ClientHttpRequestFactoryBuilder;
import infra.http.client.config.HttpClientSettings;
import infra.web.client.config.RestClientCustomizer;

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
@DisableDIAutoConfiguration(after = HttpMessageConvertersAutoConfiguration.class)
@ConditionalOnClass(RestClient.class)
@Conditional(NotReactiveWebApplicationCondition.class)
public class RestClientAutoConfiguration {

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
  static HttpMessageConvertersRestClientCustomizer httpMessageConvertersRestClientCustomizer(
          List<ClientHttpMessageConvertersCustomizer> customizerProvider) {
    return new HttpMessageConvertersRestClientCustomizer(customizerProvider);
  }

}
