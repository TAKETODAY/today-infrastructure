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
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.context.condition.ConditionalOnBean;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingBean;
import infra.core.Ordered;
import infra.core.annotation.Order;
import infra.core.ssl.SslBundles;
import infra.web.config.HttpMessageConverters;
import infra.stereotype.Component;
import infra.stereotype.Prototype;
import infra.web.client.RestClient;
import infra.web.client.config.ClientHttpRequestFactories;
import infra.web.client.config.ClientHttpRequestFactorySettings;
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

  private RestClientAutoConfiguration() {
  }

  @Component
  @ConditionalOnMissingBean
  @Order(Ordered.LOWEST_PRECEDENCE)
  public static HttpMessageConvertersRestClientCustomizer httpMessageConvertersRestClientCustomizer(
          @Nullable HttpMessageConverters messageConverters) {
    return new HttpMessageConvertersRestClientCustomizer(messageConverters);
  }

  @Component
  @ConditionalOnBean(SslBundles.class)
  @ConditionalOnMissingBean(RestClientSsl.class)
  public static AutoConfiguredRestClientSsl restClientSsl(SslBundles sslBundles) {
    return new AutoConfiguredRestClientSsl(sslBundles);
  }

  @Prototype
  @ConditionalOnMissingBean
  public static RestClient.Builder restClientBuilder(List<RestClientCustomizer> customizerProvider) {
    RestClient.Builder builder = RestClient.builder()
            .requestFactory(ClientHttpRequestFactories.get(ClientHttpRequestFactorySettings.DEFAULTS));

    for (RestClientCustomizer customizer : customizerProvider) {
      customizer.customize(builder);
    }
    return builder;
  }

}
