/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.annotation.config.web.reactive;

import org.jspecify.annotations.Nullable;

import infra.annotation.ConditionalOnWebApplication;
import infra.beans.factory.ObjectProvider;
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
import infra.http.client.ReactorResourceFactory;
import infra.http.config.annotation.ReactorNettyConfigurations.ReactorResourceFactoryConfiguration;
import infra.http.server.reactive.ForwardedHeaderTransformer;
import infra.stereotype.Component;
import infra.util.ObjectUtils;
import infra.web.server.ServerProperties;
import infra.web.server.WebServerFactoryCustomizerBeanPostProcessor;
import infra.web.server.reactive.ReactiveWebServerFactory;
import infra.web.server.reactive.support.NettyRouteProvider;
import infra.web.server.reactive.support.ReactorNettyReactiveWebServerFactory;
import infra.web.server.reactive.support.ReactorNettyServerCustomizer;
import reactor.netty.http.server.HttpServer;

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
@Import({ BeanPostProcessorsRegistrar.class, ReactorResourceFactoryConfiguration.class })
public class ReactiveWebServerFactoryAutoConfiguration {

  @Component
  @ConditionalOnClass({ HttpServer.class })
  @ConditionalOnMissingBean(ReactiveWebServerFactory.class)
  static ReactorNettyReactiveWebServerFactory reactorNettyReactiveWebServerFactory(ReactorResourceFactory resourceFactory,
          ObjectProvider<NettyRouteProvider> routes, ObjectProvider<ReactorNettyServerCustomizer> serverCustomizers) {

    ReactorNettyReactiveWebServerFactory serverFactory = new ReactorNettyReactiveWebServerFactory();
    serverFactory.setResourceFactory(resourceFactory);
    for (NettyRouteProvider route : routes) {
      serverFactory.addRouteProviders(route);
    }
    serverCustomizers.addOrderedTo(serverFactory.getServerCustomizers());
    return serverFactory;
  }

  @Component
  public static ReactiveWebServerFactoryCustomizer reactiveWebServerFactoryCustomizer(
          ServerProperties serverProperties, @Nullable SslBundles sslBundles, @Nullable ApplicationTemp applicationTemp) {
    return new ReactiveWebServerFactoryCustomizer(serverProperties, sslBundles, applicationTemp);
  }

  @Component
  @ConditionalOnMissingBean
  @ConditionalOnProperty(value = "server.forward-headers-strategy", havingValue = "framework")
  public static ForwardedHeaderTransformer forwardedHeaderTransformer() {
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
      if (ObjectUtils.isEmpty(beanFactory.getBeanNamesForType(
              WebServerFactoryCustomizerBeanPostProcessor.class, true, false))) {
        RootBeanDefinition beanDefinition = new RootBeanDefinition(WebServerFactoryCustomizerBeanPostProcessor.class);
        beanDefinition.setSynthetic(true);
        beanDefinition.setEnableDependencyInjection(false);
        context.registerBeanDefinition("webServerFactoryCustomizerBeanPostProcessor", beanDefinition);
      }
    }

  }

}
