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

package cn.taketoday.annotation.config.web.servlet;

import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.UpgradeProtocol;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.Loader;
import org.eclipse.jetty.webapp.WebAppContext;
import org.xnio.SslClientAuthMode;

import java.util.List;

import cn.taketoday.beans.factory.ObjectProvider;
import cn.taketoday.beans.factory.annotation.DisableDependencyInjection;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.ImportBeanDefinitionRegistrar;
import cn.taketoday.context.annotation.config.AutoConfigureOrder;
import cn.taketoday.context.annotation.config.DisableDIAutoConfiguration;
import cn.taketoday.context.annotation.config.EnableAutoConfiguration;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.context.condition.ConditionalOnProperty;
import cn.taketoday.context.condition.SearchStrategy;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.core.ApplicationTemp;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.ssl.SslBundles;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.framework.annotation.ConditionalOnWebApplication;
import cn.taketoday.framework.annotation.ConditionalOnWebApplication.Type;
import cn.taketoday.framework.web.embedded.jetty.JettyServerCustomizer;
import cn.taketoday.framework.web.embedded.jetty.JettyServletWebServerFactory;
import cn.taketoday.framework.web.embedded.tomcat.TomcatConnectorCustomizer;
import cn.taketoday.framework.web.embedded.tomcat.TomcatContextCustomizer;
import cn.taketoday.framework.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import cn.taketoday.framework.web.embedded.tomcat.TomcatServletWebServerFactory;
import cn.taketoday.framework.web.embedded.undertow.UndertowBuilderCustomizer;
import cn.taketoday.framework.web.embedded.undertow.UndertowDeploymentInfoCustomizer;
import cn.taketoday.framework.web.embedded.undertow.UndertowServletWebServerFactory;
import cn.taketoday.framework.web.server.ErrorPageRegistrarBeanPostProcessor;
import cn.taketoday.framework.web.server.ServerProperties;
import cn.taketoday.framework.web.server.WebServerFactoryCustomizerBeanPostProcessor;
import cn.taketoday.framework.web.servlet.FilterRegistrationBean;
import cn.taketoday.framework.web.servlet.WebListenerRegistrar;
import cn.taketoday.framework.web.servlet.server.CookieSameSiteSupplier;
import cn.taketoday.framework.web.servlet.server.ServletWebServerFactory;
import cn.taketoday.lang.Nullable;
import cn.taketoday.stereotype.Component;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.web.servlet.filter.ForwardedHeaderFilter;
import io.undertow.Undertow;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletRequest;

import static cn.taketoday.annotation.config.web.servlet.ServletWebServerFactoryAutoConfiguration.BeanPostProcessorsRegistrar;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for servlet web servers.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Ivan Sopov
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/27 21:51
 */
@DisableDIAutoConfiguration
@ConditionalOnClass(ServletRequest.class)
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnWebApplication(type = Type.SERVLET)
@EnableConfigurationProperties(ServerProperties.class)
@Import(BeanPostProcessorsRegistrar.class)
public class ServletWebServerFactoryAutoConfiguration {

  @Component
  static ServletWebServerFactoryCustomizer servletWebServerFactoryCustomizer(
          List<WebListenerRegistrar> webListenerRegistrars,
          ServerProperties serverProperties, @Nullable ApplicationTemp applicationTemp,
          List<CookieSameSiteSupplier> cookieSameSiteSuppliers, @Nullable SslBundles sslBundles) {
    return new ServletWebServerFactoryCustomizer(serverProperties,
            webListenerRegistrars, cookieSameSiteSuppliers, sslBundles, applicationTemp);
  }

  @Component
  @ConditionalOnClass(name = "org.apache.catalina.startup.Tomcat")
  static TomcatServletWebServerFactoryCustomizer tomcatServletWebServerFactoryCustomizer(
          ServerProperties serverProperties) {
    return new TomcatServletWebServerFactoryCustomizer(serverProperties);
  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnProperty(value = "server.forward-headers-strategy", havingValue = "framework")
  @ConditionalOnMissingFilterBean(ForwardedHeaderFilter.class)
  static class ForwardedHeaderFilterConfiguration {

    @Component
    static FilterRegistrationBean<ForwardedHeaderFilter> forwardedHeaderFilter(ServerProperties properties) {
      ForwardedHeaderFilter filter = new ForwardedHeaderFilter();

      if (ClassUtils.isPresent("org.apache.catalina.startup.Tomcat", ForwardedHeaderFilterConfiguration.class.getClassLoader())) {
        filter.setRelativeRedirects(properties.getTomcat().isUseRelativeRedirects());
      }

      var registration = new FilterRegistrationBean<>(filter);
      registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR);
      registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
      return registration;
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass({ Servlet.class, Tomcat.class, UpgradeProtocol.class })
  @ConditionalOnMissingBean(value = ServletWebServerFactory.class, search = SearchStrategy.CURRENT)
  static class EmbeddedTomcat {

    @Component
    static TomcatServletWebServerFactory tomcatServletWebServerFactory(
            ObjectProvider<TomcatContextCustomizer> contextCustomizers,
            ObjectProvider<TomcatConnectorCustomizer> connectorCustomizers,
            ObjectProvider<TomcatProtocolHandlerCustomizer<?>> protocolHandlerCustomizers) {
      TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
      contextCustomizers.addOrderedTo(factory.getTomcatContextCustomizers());
      connectorCustomizers.addOrderedTo(factory.getTomcatConnectorCustomizers());
      protocolHandlerCustomizers.addOrderedTo(factory.getTomcatProtocolHandlerCustomizers());
      return factory;
    }

  }

  /**
   * Nested configuration if Jetty is being used.
   */
  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass({ Servlet.class, Server.class, Loader.class, WebAppContext.class })
  @ConditionalOnMissingBean(value = ServletWebServerFactory.class, search = SearchStrategy.CURRENT)
  static class EmbeddedJetty {

    @Component
    static JettyServletWebServerFactory JettyServletWebServerFactory(
            ObjectProvider<JettyServerCustomizer> serverCustomizers) {
      JettyServletWebServerFactory factory = new JettyServletWebServerFactory();
      serverCustomizers.addOrderedTo(factory.getServerCustomizers());
      return factory;
    }

  }

  /**
   * Nested configuration if Undertow is being used.
   */
  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass({ Servlet.class, Undertow.class, SslClientAuthMode.class })
  @ConditionalOnMissingBean(value = ServletWebServerFactory.class, search = SearchStrategy.CURRENT)
  static class EmbeddedUndertow {

    @Component
    static UndertowServletWebServerFactory undertowServletWebServerFactory(
            ObjectProvider<UndertowDeploymentInfoCustomizer> deploymentInfoCustomizers,
            ObjectProvider<UndertowBuilderCustomizer> builderCustomizers) {
      UndertowServletWebServerFactory factory = new UndertowServletWebServerFactory();
      builderCustomizers.addOrderedTo(factory.getBuilderCustomizers());
      deploymentInfoCustomizers.addOrderedTo(factory.getDeploymentInfoCustomizers());
      return factory;
    }

    @Component
    static UndertowServletWebServerFactoryCustomizer undertowServletWebServerFactoryCustomizer(
            ServerProperties serverProperties) {
      return new UndertowServletWebServerFactoryCustomizer(serverProperties);
    }

  }

  /**
   * Registers a {@link WebServerFactoryCustomizerBeanPostProcessor}. Registered via
   * {@link ImportBeanDefinitionRegistrar} for early registration.
   */
  @DisableDependencyInjection
  public static class BeanPostProcessorsRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importMetadata,
            cn.taketoday.context.BootstrapContext context) {

      registerSyntheticBeanIfMissing(context, "webServerFactoryCustomizerBeanPostProcessor",
              WebServerFactoryCustomizerBeanPostProcessor.class);
      registerSyntheticBeanIfMissing(context, "errorPageRegistrarBeanPostProcessor",
              ErrorPageRegistrarBeanPostProcessor.class);
    }

    private <T> void registerSyntheticBeanIfMissing(
            cn.taketoday.context.BootstrapContext context, String name, Class<T> beanClass) {
      if (ObjectUtils.isEmpty(context.getBeanFactory().getBeanNamesForType(beanClass, true, false))) {
        RootBeanDefinition beanDefinition = new RootBeanDefinition(beanClass);
        beanDefinition.setSynthetic(true);
        beanDefinition.setEnableDependencyInjection(false);
        context.registerBeanDefinition(name, beanDefinition);
      }
    }

  }

}
