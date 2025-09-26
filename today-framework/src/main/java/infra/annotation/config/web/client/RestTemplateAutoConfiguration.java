/*
 * Copyright 2017 - 2025 the original author or authors.
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

import infra.annotation.config.http.HttpMessageConvertersAutoConfiguration;
import infra.context.annotation.Conditional;
import infra.context.annotation.Lazy;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingBean;
import infra.http.converter.HttpMessageConverters;
import infra.stereotype.Component;
import infra.web.client.RestTemplate;
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

  private RestTemplateAutoConfiguration() {
  }

  @Lazy
  @Component
  @ConditionalOnMissingBean
  public static RestTemplateBuilderConfigurer restTemplateBuilderConfigurer(
          @Nullable HttpMessageConverters messageConverters,
          @Nullable List<RestTemplateCustomizer> restTemplateCustomizers,
          @Nullable List<RestTemplateRequestCustomizer<?>> restTemplateRequestCustomizers) {

    RestTemplateBuilderConfigurer configurer = new RestTemplateBuilderConfigurer();
    configurer.setHttpMessageConverters(messageConverters);
    configurer.setRestTemplateCustomizers(restTemplateCustomizers);
    configurer.setRestTemplateRequestCustomizers(restTemplateRequestCustomizers);
    return configurer;
  }

  @Lazy
  @Component
  @ConditionalOnMissingBean
  public static RestTemplateBuilder restTemplateBuilder(RestTemplateBuilderConfigurer restTemplateBuilderConfigurer) {
    RestTemplateBuilder builder = new RestTemplateBuilder();
    return restTemplateBuilderConfigurer.configure(builder);
  }

}
