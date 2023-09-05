/*
 * Copyright 2017 - 2023 the original author or authors.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.annotation.config.web.client;

import java.util.List;

import cn.taketoday.annotation.config.http.HttpMessageConvertersAutoConfiguration;
import cn.taketoday.context.annotation.Conditional;
import cn.taketoday.context.annotation.Lazy;
import cn.taketoday.context.annotation.config.DisableDIAutoConfiguration;
import cn.taketoday.context.annotation.config.EnableAutoConfiguration;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.Order;
import cn.taketoday.http.converter.HttpMessageConverters;
import cn.taketoday.lang.Nullable;
import cn.taketoday.stereotype.Component;
import cn.taketoday.stereotype.Prototype;
import cn.taketoday.web.client.RestClient;
import cn.taketoday.web.client.config.ClientHttpRequestFactories;
import cn.taketoday.web.client.config.ClientHttpRequestFactorySettings;
import cn.taketoday.web.client.config.RestClientCustomizer;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link RestClient}.
 * <p>
 * This will produce a {@link cn.taketoday.web.client.RestClient.Builder
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
  @ConditionalOnMissingBean
  @Order(Ordered.LOWEST_PRECEDENCE)
  static HttpMessageConvertersRestClientCustomizer httpMessageConvertersRestClientCustomizer(
          @Nullable HttpMessageConverters messageConverters) {
    return new HttpMessageConvertersRestClientCustomizer(messageConverters);
  }

  @Prototype
  @ConditionalOnMissingBean
  static RestClient.Builder restClientBuilder(List<RestClientCustomizer> customizerProvider) {
    RestClient.Builder builder = RestClient.builder()
            .requestFactory(ClientHttpRequestFactories.get(ClientHttpRequestFactorySettings.DEFAULTS));

    for (RestClientCustomizer customizer : customizerProvider) {
      customizer.customize(builder);
    }
    return builder;
  }

}
