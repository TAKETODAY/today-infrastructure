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

import infra.annotation.config.http.ClientHttpMessageConvertersCustomizer;
import infra.annotation.config.http.HttpMessageConvertersAutoConfiguration;
import infra.beans.factory.ObjectProvider;
import infra.context.annotation.Conditional;
import infra.context.annotation.Lazy;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingBean;
import infra.stereotype.Component;
import infra.web.client.RestTemplate;
import infra.http.client.config.ClientHttpRequestFactoryBuilder;
import infra.http.client.config.HttpClientSettings;
import infra.web.client.config.RestTemplateBuilder;
import infra.web.client.config.RestTemplateCustomizer;
import infra.web.client.config.RestTemplateRequestCustomizer;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/16 14:40
 */
@Lazy
@ConditionalOnClass(RestTemplate.class)
@DisableDIAutoConfiguration(after = HttpMessageConvertersAutoConfiguration.class)
@Conditional(NotReactiveWebApplicationCondition.class)
public class RestTemplateAutoConfiguration {

  @Lazy
  @Component
  @ConditionalOnMissingBean
  public static RestTemplateBuilderConfigurer restTemplateBuilderConfigurer(
          ObjectProvider<ClientHttpRequestFactoryBuilder<?>> clientHttpRequestFactoryBuilder,
          ObjectProvider<HttpClientSettings> httpClientSettings,
          ObjectProvider<ClientHttpMessageConvertersCustomizer> convertersCustomizers,
          ObjectProvider<RestTemplateCustomizer> restTemplateCustomizers,
          ObjectProvider<RestTemplateRequestCustomizer<?>> restTemplateRequestCustomizers) {
    RestTemplateBuilderConfigurer configurer = new RestTemplateBuilderConfigurer();
    configurer.setRequestFactoryBuilder(clientHttpRequestFactoryBuilder.getIfAvailable());
    configurer.setClientSettings(httpClientSettings.getIfAvailable());
    configurer.setHttpMessageConvertersCustomizers(convertersCustomizers.orderedStream().toList());
    configurer.setRestTemplateCustomizers(restTemplateCustomizers.orderedStream().toList());
    configurer.setRestTemplateRequestCustomizers(restTemplateRequestCustomizers.orderedStream().toList());
    return configurer;
  }

  @Lazy
  @Component
  @ConditionalOnMissingBean
  public static RestTemplateBuilder restTemplateBuilder(RestTemplateBuilderConfigurer configurer) {
    return configurer.configure(new RestTemplateBuilder());
  }

}
