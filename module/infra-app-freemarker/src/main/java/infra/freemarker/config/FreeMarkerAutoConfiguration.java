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

package infra.freemarker.config;

import java.util.List;

import infra.app.config.ConditionalOnNotWebApplication;
import infra.app.config.ConditionalOnWebApplication;
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
import infra.web.config.WebMvcAutoConfiguration;
import infra.web.config.WebMvcProperties;

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
public final class FreeMarkerAutoConfiguration {

  @ConditionalOnNotWebApplication
  @Configuration(proxyBeanMethods = false)
  static class FreeMarkerNonWebConfiguration {

    @Component
    @ConditionalOnMissingBean
    static FreeMarkerConfigurationFactoryBean freeMarkerConfiguration(FreeMarkerProperties properties, List<FreeMarkerVariablesCustomizer> variablesCustomizer) {
      var freeMarkerFactoryBean = new FreeMarkerConfigurationFactoryBean();
      properties.applyTo(freeMarkerFactoryBean, variablesCustomizer);
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
    static FreeMarkerConfigurer freeMarkerConfigurer(FreeMarkerProperties properties, List<FreeMarkerVariablesCustomizer> variablesCustomizer) {
      FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
      properties.applyTo(configurer, variablesCustomizer);
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
