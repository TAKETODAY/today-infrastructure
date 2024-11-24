/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.annotation.config.freemarker;

import infra.annotation.ConditionalOnNotWebApplication;
import infra.annotation.ConditionalOnWebApplication;
import infra.annotation.config.web.WebMvcAutoConfiguration;
import infra.annotation.config.web.WebMvcProperties;
import infra.context.annotation.Configuration;
import infra.context.annotation.Lazy;
import infra.context.annotation.config.AutoConfigureAfter;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.condition.ConditionalOnProperty;
import infra.context.properties.EnableConfigurationProperties;
import infra.stereotype.Component;
import infra.ui.freemarker.FreeMarkerConfigurationFactory;
import infra.ui.freemarker.FreeMarkerConfigurationFactoryBean;
import infra.web.view.freemarker.FreeMarkerConfig;
import infra.web.view.freemarker.FreeMarkerConfigurer;
import infra.web.view.freemarker.FreeMarkerViewResolver;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for FreeMarker.
 *
 * @author Andy Wilkinson
 * @author Dave Syer
 * @author Kazuki Shimizu
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@Lazy
@DisableDIAutoConfiguration
@EnableConfigurationProperties(FreeMarkerProperties.class)
@ConditionalOnClass({ freemarker.template.Configuration.class, FreeMarkerConfigurationFactory.class })
public class FreeMarkerAutoConfiguration {

  @ConditionalOnNotWebApplication
  @Configuration(proxyBeanMethods = false)
  static class FreeMarkerNonWebConfiguration {

    @Component
    @ConditionalOnMissingBean
    static FreeMarkerConfigurationFactoryBean freeMarkerConfiguration(FreeMarkerProperties properties) {
      var freeMarkerFactoryBean = new FreeMarkerConfigurationFactoryBean();
      properties.applyTo(freeMarkerFactoryBean);
      return freeMarkerFactoryBean;
    }

  }

  @ConditionalOnWebApplication
  @Configuration(proxyBeanMethods = false)
  @AutoConfigureAfter(WebMvcAutoConfiguration.class)
  @ConditionalOnClass({ FreeMarkerConfigurer.class })
  static class FreeMarkerWebConfiguration {

    @Component
    @ConditionalOnMissingBean(FreeMarkerConfig.class)
    static FreeMarkerConfigurer freeMarkerConfigurer(FreeMarkerProperties properties) {
      FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
      properties.applyTo(configurer);
      return configurer;
    }

    @Component
    static freemarker.template.Configuration freeMarkerConfiguration(FreeMarkerConfig configurer) {
      return configurer.getConfiguration();
    }

    @Component
    @ConditionalOnMissingBean(name = "freeMarkerViewResolver")
    @ConditionalOnProperty(name = "freemarker.enabled", matchIfMissing = true)
    static FreeMarkerViewResolver freeMarkerViewResolver(FreeMarkerProperties properties, WebMvcProperties mvcProperties) {
      FreeMarkerViewResolver resolver = new FreeMarkerViewResolver();
      properties.applyToMvcViewResolver(resolver);
      mvcProperties.view.applyTo(resolver);
      return resolver;
    }

  }

}
