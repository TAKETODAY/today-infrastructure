/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

import java.util.stream.Collectors;

import cn.taketoday.annotation.config.http.HttpMessageConvertersAutoConfiguration;
import cn.taketoday.beans.factory.ObjectProvider;
import cn.taketoday.beans.factory.annotation.DisableAllDependencyInjection;
import cn.taketoday.context.annotation.Conditional;
import cn.taketoday.context.annotation.Lazy;
import cn.taketoday.context.annotation.config.AutoConfiguration;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.context.condition.NoneNestedConditions;
import cn.taketoday.framework.annotation.ConditionalOnWebApplication;
import cn.taketoday.framework.annotation.ConditionalOnWebApplication.Type;
import cn.taketoday.http.converter.HttpMessageConverters;
import cn.taketoday.stereotype.Component;
import cn.taketoday.web.client.RestTemplate;
import cn.taketoday.web.client.config.RestTemplateBuilder;
import cn.taketoday.web.client.config.RestTemplateCustomizer;
import cn.taketoday.web.client.config.RestTemplateRequestCustomizer;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/16 14:40
 */
@DisableAllDependencyInjection
@ConditionalOnClass(RestTemplate.class)
@AutoConfiguration(after = HttpMessageConvertersAutoConfiguration.class)
@Conditional(RestTemplateAutoConfiguration.NotReactiveWebApplicationCondition.class)
public class RestTemplateAutoConfiguration {

  @Lazy
  @Component
  @ConditionalOnMissingBean
  public RestTemplateBuilderConfigurer restTemplateBuilderConfigurer(
          ObjectProvider<HttpMessageConverters> messageConverters,
          ObjectProvider<RestTemplateCustomizer> restTemplateCustomizers,
          ObjectProvider<RestTemplateRequestCustomizer<?>> restTemplateRequestCustomizers) {

    RestTemplateBuilderConfigurer configurer = new RestTemplateBuilderConfigurer();
    configurer.setHttpMessageConverters(messageConverters.getIfUnique());
    configurer.setRestTemplateCustomizers(restTemplateCustomizers.orderedStream().collect(Collectors.toList()));
    configurer.setRestTemplateRequestCustomizers(
            restTemplateRequestCustomizers.orderedStream().collect(Collectors.toList()));
    return configurer;
  }

  @Lazy
  @Component
  @ConditionalOnMissingBean
  public RestTemplateBuilder restTemplateBuilder(RestTemplateBuilderConfigurer restTemplateBuilderConfigurer) {
    RestTemplateBuilder builder = new RestTemplateBuilder();
    return restTemplateBuilderConfigurer.configure(builder);
  }

  static class NotReactiveWebApplicationCondition extends NoneNestedConditions {

    NotReactiveWebApplicationCondition() {
      super(ConfigurationPhase.PARSE_CONFIGURATION);
    }

    @ConditionalOnWebApplication(type = Type.REACTIVE)
    private static class ReactiveWebApplication {

    }

  }
}