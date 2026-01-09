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
