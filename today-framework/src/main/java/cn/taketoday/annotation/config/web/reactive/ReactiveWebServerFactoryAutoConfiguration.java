/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.annotation.config.web.reactive;

import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.context.BootstrapContext;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.ImportBeanDefinitionRegistrar;
import cn.taketoday.context.annotation.config.AutoConfiguration;
import cn.taketoday.context.annotation.config.AutoConfigureOrder;
import cn.taketoday.context.annotation.config.EnableAutoConfiguration;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.context.condition.ConditionalOnProperty;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.ssl.SslBundles;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.framework.annotation.ConditionalOnWebApplication;
import cn.taketoday.framework.web.server.ServerProperties;
import cn.taketoday.framework.web.server.WebServerFactoryCustomizerBeanPostProcessor;
import cn.taketoday.http.ReactiveHttpInputMessage;
import cn.taketoday.http.server.reactive.ForwardedHeaderTransformer;
import cn.taketoday.lang.Nullable;
import cn.taketoday.stereotype.Component;
import cn.taketoday.util.CollectionUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for a reactive web server.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Brian Clozel
 * @since 4.0 2022/10/21 12:12
 */
@AutoConfiguration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnClass(ReactiveHttpInputMessage.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@EnableConfigurationProperties(ServerProperties.class)
@Import({
        ReactiveWebServerFactoryAutoConfiguration.BeanPostProcessorsRegistrar.class,
        ReactiveWebServerFactoryConfiguration.EmbeddedTomcat.class,
        ReactiveWebServerFactoryConfiguration.EmbeddedJetty.class,
        ReactiveWebServerFactoryConfiguration.EmbeddedUndertow.class,
        ReactiveWebServerFactoryConfiguration.EmbeddedNetty.class
})
public class ReactiveWebServerFactoryAutoConfiguration {

  @Component
  public ReactiveWebServerFactoryCustomizer reactiveWebServerFactoryCustomizer(
          ServerProperties serverProperties, @Nullable SslBundles sslBundles) {
    return new ReactiveWebServerFactoryCustomizer(serverProperties, sslBundles);
  }

  @Component
  @ConditionalOnClass(name = "org.apache.catalina.startup.Tomcat")
  public TomcatReactiveWebServerFactoryCustomizer tomcatReactiveWebServerFactoryCustomizer(
          ServerProperties serverProperties) {
    return new TomcatReactiveWebServerFactoryCustomizer(serverProperties);
  }

  @Component
  @ConditionalOnMissingBean
  @ConditionalOnProperty(value = "server.forward-headers-strategy", havingValue = "framework")
  public ForwardedHeaderTransformer forwardedHeaderTransformer() {
    return new ForwardedHeaderTransformer();
  }

  /**
   * Registers a {@link WebServerFactoryCustomizerBeanPostProcessor}. Registered via
   * {@link ImportBeanDefinitionRegistrar} for early registration.
   */
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
