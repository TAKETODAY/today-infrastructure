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

package cn.taketoday.annotation.config.freemarker;

import cn.taketoday.annotation.config.web.WebMvcAutoConfiguration;
import cn.taketoday.annotation.config.web.WebMvcProperties;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Lazy;
import cn.taketoday.context.annotation.config.AutoConfigureAfter;
import cn.taketoday.context.annotation.config.DisableDIAutoConfiguration;
import cn.taketoday.context.annotation.config.EnableAutoConfiguration;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.context.condition.ConditionalOnProperty;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.framework.annotation.ConditionalOnNotWebApplication;
import cn.taketoday.framework.annotation.ConditionalOnWebApplication;
import cn.taketoday.stereotype.Component;
import cn.taketoday.ui.freemarker.FreeMarkerConfigurationFactory;
import cn.taketoday.ui.freemarker.FreeMarkerConfigurationFactoryBean;
import cn.taketoday.web.view.freemarker.FreeMarkerConfig;
import cn.taketoday.web.view.freemarker.FreeMarkerConfigurer;
import cn.taketoday.web.view.freemarker.FreeMarkerViewResolver;

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

  @Lazy
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

  @Lazy
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
      mvcProperties.getView().applyTo(resolver);
      return resolver;
    }

  }

}
