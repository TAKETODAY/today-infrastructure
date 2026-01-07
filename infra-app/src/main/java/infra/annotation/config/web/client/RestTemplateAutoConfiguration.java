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

import org.jspecify.annotations.Nullable;

import java.util.List;

import infra.annotation.config.http.ClientHttpMessageConvertersCustomizer;
import infra.annotation.config.http.client.HttpClientAutoConfiguration;
import infra.context.annotation.Conditional;
import infra.context.annotation.Lazy;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingBean;
import infra.http.client.config.ClientHttpRequestFactoryBuilder;
import infra.http.client.config.HttpClientSettings;
import infra.http.converter.HttpMessageConverters;
import infra.stereotype.Component;
import infra.web.client.RestTemplate;
import infra.web.client.config.RestTemplateBuilder;
import infra.web.client.config.RestTemplateCustomizer;
import infra.web.client.config.RestTemplateRequestCustomizer;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link RestTemplate} (via
 * {@link RestTemplateBuilder}).
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/16 14:40
 */
@Lazy
@ConditionalOnClass({ RestTemplate.class, HttpMessageConverters.class })
@DisableDIAutoConfiguration(after = HttpClientAutoConfiguration.class)
@Conditional(NotReactiveWebApplicationCondition.class)
public final class RestTemplateAutoConfiguration {

  @Lazy
  @Component
  public static RestTemplateBuilderConfigurer restTemplateBuilderConfigurer(
          @Nullable ClientHttpRequestFactoryBuilder<?> clientHttpRequestFactoryBuilder,
          @Nullable HttpClientSettings httpClientSettings,
          List<ClientHttpMessageConvertersCustomizer> convertersCustomizers,
          List<RestTemplateCustomizer> restTemplateCustomizers,
          List<RestTemplateRequestCustomizer<?>> restTemplateRequestCustomizers) {
    RestTemplateBuilderConfigurer configurer = new RestTemplateBuilderConfigurer();
    configurer.setClientSettings(httpClientSettings);
    configurer.setRequestFactoryBuilder(clientHttpRequestFactoryBuilder);
    configurer.setHttpMessageConvertersCustomizers(convertersCustomizers);
    configurer.setRestTemplateCustomizers(restTemplateCustomizers);
    configurer.setRestTemplateRequestCustomizers(restTemplateRequestCustomizers);
    return configurer;
  }

  @Lazy
  @Component
  @ConditionalOnMissingBean
  public static RestTemplateBuilder restTemplateBuilder(RestTemplateBuilderConfigurer configurer) {
    return configurer.configure(new RestTemplateBuilder());
  }

}
