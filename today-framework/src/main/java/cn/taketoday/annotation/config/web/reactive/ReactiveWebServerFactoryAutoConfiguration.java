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

package cn.taketoday.annotation.config.web.reactive;

import org.eclipse.jetty.servlet.ServletHolder;

import cn.taketoday.beans.factory.ObjectProvider;
import cn.taketoday.beans.factory.annotation.DisableDependencyInjection;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.context.BootstrapContext;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.ImportBeanDefinitionRegistrar;
import cn.taketoday.context.annotation.config.AutoConfigureOrder;
import cn.taketoday.context.annotation.config.DisableDIAutoConfiguration;
import cn.taketoday.context.annotation.config.EnableAutoConfiguration;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.context.condition.ConditionalOnProperty;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.core.ApplicationTemp;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.ssl.SslBundles;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.framework.annotation.ConditionalOnWebApplication;
import cn.taketoday.framework.web.embedded.jetty.JettyReactiveWebServerFactory;
import cn.taketoday.framework.web.embedded.jetty.JettyServerCustomizer;
import cn.taketoday.framework.web.embedded.netty.NettyRouteProvider;
import cn.taketoday.framework.web.embedded.netty.ReactorNettyReactiveWebServerFactory;
import cn.taketoday.framework.web.embedded.netty.ReactorNettyServerCustomizer;
import cn.taketoday.framework.web.embedded.tomcat.TomcatConnectorCustomizer;
import cn.taketoday.framework.web.embedded.tomcat.TomcatContextCustomizer;
import cn.taketoday.framework.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import cn.taketoday.framework.web.embedded.tomcat.TomcatReactiveWebServerFactory;
import cn.taketoday.framework.web.embedded.undertow.UndertowBuilderCustomizer;
import cn.taketoday.framework.web.embedded.undertow.UndertowReactiveWebServerFactory;
import cn.taketoday.framework.web.reactive.server.ReactiveWebServerFactory;
import cn.taketoday.framework.web.server.ServerProperties;
import cn.taketoday.framework.web.server.WebServerFactoryCustomizerBeanPostProcessor;
import cn.taketoday.http.ReactiveHttpInputMessage;
import cn.taketoday.http.client.reactive.JettyResourceFactory;
import cn.taketoday.http.client.reactive.ReactorResourceFactory;
import cn.taketoday.http.server.reactive.ForwardedHeaderTransformer;
import cn.taketoday.lang.Nullable;
import cn.taketoday.stereotype.Component;
import cn.taketoday.util.CollectionUtils;
import io.undertow.Undertow;
import reactor.netty.http.server.HttpServer;

import static cn.taketoday.annotation.config.web.reactive.ReactiveWebServerFactoryAutoConfiguration.BeanPostProcessorsRegistrar;
import static cn.taketoday.framework.annotation.ConditionalOnWebApplication.Type;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for a reactive web server.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Brian Clozel
 * @since 4.0 2022/10/21 12:12
 */
@DisableDIAutoConfiguration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnClass(ReactiveHttpInputMessage.class)
@ConditionalOnWebApplication(type = Type.REACTIVE)
@EnableConfigurationProperties(ServerProperties.class)
@Import(BeanPostProcessorsRegistrar.class)
public class ReactiveWebServerFactoryAutoConfiguration {

  @Component
  static ReactiveWebServerFactoryCustomizer reactiveWebServerFactoryCustomizer(
          ServerProperties serverProperties, @Nullable SslBundles sslBundles, @Nullable ApplicationTemp applicationTemp) {
    return new ReactiveWebServerFactoryCustomizer(serverProperties, sslBundles, applicationTemp);
  }

  @Component
  @ConditionalOnClass(name = "org.apache.catalina.startup.Tomcat")
  static TomcatReactiveWebServerFactoryCustomizer tomcatReactiveWebServerFactoryCustomizer(
          ServerProperties serverProperties) {
    return new TomcatReactiveWebServerFactoryCustomizer(serverProperties);
  }

  @Component
  @ConditionalOnMissingBean
  @ConditionalOnProperty(value = "server.forward-headers-strategy", havingValue = "framework")
  static ForwardedHeaderTransformer forwardedHeaderTransformer() {
    return new ForwardedHeaderTransformer();
  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass({ HttpServer.class })
  @ConditionalOnMissingBean(ReactiveWebServerFactory.class)
  static class EmbeddedNetty {

    @Component
    @ConditionalOnMissingBean
    static ReactorResourceFactory reactorServerResourceFactory() {
      return new ReactorResourceFactory();
    }

    @Component
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

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass({ org.apache.catalina.startup.Tomcat.class })
  @ConditionalOnMissingBean(ReactiveWebServerFactory.class)
  static class EmbeddedTomcat {

    @Component
    static TomcatReactiveWebServerFactory tomcatReactiveWebServerFactory(
            ObjectProvider<TomcatConnectorCustomizer> connectorCustomizers,
            ObjectProvider<TomcatContextCustomizer> contextCustomizers,
            ObjectProvider<TomcatProtocolHandlerCustomizer<?>> protocolHandlerCustomizers) {
      TomcatReactiveWebServerFactory factory = new TomcatReactiveWebServerFactory();
      contextCustomizers.addOrderedTo(factory.getTomcatContextCustomizers());
      connectorCustomizers.addOrderedTo(factory.getTomcatConnectorCustomizers());
      protocolHandlerCustomizers.addOrderedTo(factory.getTomcatProtocolHandlerCustomizers());
      return factory;
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass({ org.eclipse.jetty.server.Server.class, ServletHolder.class })
  @ConditionalOnMissingBean(ReactiveWebServerFactory.class)
  static class EmbeddedJetty {

    @Component
    @ConditionalOnMissingBean
    static JettyResourceFactory jettyServerResourceFactory() {
      return new JettyResourceFactory();
    }

    @Component
    static JettyReactiveWebServerFactory jettyReactiveWebServerFactory(
            JettyResourceFactory resourceFactory, ObjectProvider<JettyServerCustomizer> serverCustomizers) {
      JettyReactiveWebServerFactory serverFactory = new JettyReactiveWebServerFactory();
      serverCustomizers.addOrderedTo(serverFactory.getServerCustomizers());
      serverFactory.setResourceFactory(resourceFactory);
      return serverFactory;
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass({ Undertow.class })
  @ConditionalOnMissingBean(ReactiveWebServerFactory.class)
  static class EmbeddedUndertow {

    @Component
    static UndertowReactiveWebServerFactory undertowReactiveWebServerFactory(
            ObjectProvider<UndertowBuilderCustomizer> builderCustomizers) {
      UndertowReactiveWebServerFactory factory = new UndertowReactiveWebServerFactory();
      builderCustomizers.addOrderedTo(factory.getBuilderCustomizers());
      return factory;
    }

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
