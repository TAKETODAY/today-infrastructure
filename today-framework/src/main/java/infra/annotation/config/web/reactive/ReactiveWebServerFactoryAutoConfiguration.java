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

package infra.annotation.config.web.reactive;

import infra.annotation.ConditionalOnWebApplication;
import infra.beans.factory.annotation.DisableDependencyInjection;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.support.RootBeanDefinition;
import infra.context.BootstrapContext;
import infra.context.annotation.Import;
import infra.context.annotation.ImportBeanDefinitionRegistrar;
import infra.context.annotation.Lazy;
import infra.context.annotation.config.AutoConfigureOrder;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.condition.ConditionalOnProperty;
import infra.context.properties.EnableConfigurationProperties;
import infra.core.ApplicationTemp;
import infra.core.Ordered;
import infra.core.ssl.SslBundles;
import infra.core.type.AnnotationMetadata;
import infra.http.ReactiveHttpInputMessage;
import infra.http.server.reactive.ForwardedHeaderTransformer;
import infra.lang.Nullable;
import infra.stereotype.Component;
import infra.util.CollectionUtils;
import infra.web.server.ServerProperties;
import infra.web.server.WebServerFactoryCustomizerBeanPostProcessor;

import static infra.annotation.ConditionalOnWebApplication.Type;
import static infra.annotation.config.web.reactive.ReactiveWebServerFactoryAutoConfiguration.BeanPostProcessorsRegistrar;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for a reactive web server.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Brian Clozel
 * @since 4.0 2022/10/21 12:12
 */
@Lazy
@DisableDIAutoConfiguration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnClass(ReactiveHttpInputMessage.class)
@ConditionalOnWebApplication(type = Type.REACTIVE)
@EnableConfigurationProperties(ServerProperties.class)
@Import({ BeanPostProcessorsRegistrar.class,
        ReactiveWebServerFactoryConfiguration.EmbeddedNetty.class })
public class ReactiveWebServerFactoryAutoConfiguration {

  @Component
  static ReactiveWebServerFactoryCustomizer reactiveWebServerFactoryCustomizer(
          ServerProperties serverProperties, @Nullable SslBundles sslBundles, @Nullable ApplicationTemp applicationTemp) {
    return new ReactiveWebServerFactoryCustomizer(serverProperties, sslBundles, applicationTemp);
  }

  @Component
  @ConditionalOnMissingBean
  @ConditionalOnProperty(value = "server.forward-headers-strategy", havingValue = "framework")
  static ForwardedHeaderTransformer forwardedHeaderTransformer() {
    return new ForwardedHeaderTransformer();
  }

  /**
   * Registers a {@link WebServerFactoryCustomizerBeanPostProcessor}. Registered via
   * {@link ImportBeanDefinitionRegistrar} for early registration.
   */
  @DisableDependencyInjection
  public static class BeanPostProcessorsRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importMetadata, BootstrapContext context) {
      ConfigurableBeanFactory beanFactory = context.getBeanFactory();
      if (CollectionUtils.isEmpty(beanFactory.getBeanNamesForType(
              WebServerFactoryCustomizerBeanPostProcessor.class, true, false))) {
        RootBeanDefinition beanDefinition = new RootBeanDefinition(WebServerFactoryCustomizerBeanPostProcessor.class);
        beanDefinition.setSynthetic(true);
        beanDefinition.setEnableDependencyInjection(false);
        context.registerBeanDefinition("webServerFactoryCustomizerBeanPostProcessor", beanDefinition);
      }
    }

  }

}
