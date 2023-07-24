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

import java.util.List;

import cn.taketoday.beans.factory.annotation.DisableAllDependencyInjection;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.ImportBeanDefinitionRegistrar;
import cn.taketoday.context.annotation.config.AutoConfiguration;
import cn.taketoday.context.annotation.config.AutoConfigureOrder;
import cn.taketoday.context.annotation.config.EnableAutoConfiguration;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.context.condition.ConditionalOnProperty;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.core.ApplicationTemp;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.ssl.SslBundles;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.framework.annotation.ConditionalOnWebApplication;
import cn.taketoday.framework.annotation.ConditionalOnWebApplication.Type;
import cn.taketoday.framework.web.server.ErrorPageRegistrarBeanPostProcessor;
import cn.taketoday.framework.web.server.ServerProperties;
import cn.taketoday.framework.web.server.WebServerFactoryCustomizerBeanPostProcessor;
import cn.taketoday.framework.web.servlet.FilterRegistrationBean;
import cn.taketoday.framework.web.servlet.WebListenerRegistrar;
import cn.taketoday.framework.web.servlet.server.CookieSameSiteSupplier;
import cn.taketoday.lang.Nullable;
import cn.taketoday.stereotype.Component;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.web.servlet.filter.ForwardedHeaderFilter;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletRequest;

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
@AutoConfiguration
@DisableAllDependencyInjection
@ConditionalOnClass(ServletRequest.class)
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnWebApplication(type = Type.SERVLET)
@EnableConfigurationProperties(ServerProperties.class)
@Import({
        ServletWebServerFactoryAutoConfiguration.BeanPostProcessorsRegistrar.class,
        ServletWebServerFactoryConfiguration.EmbeddedTomcat.class,
        ServletWebServerFactoryConfiguration.EmbeddedJetty.class,
        ServletWebServerFactoryConfiguration.EmbeddedUndertow.class
})
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
    FilterRegistrationBean<ForwardedHeaderFilter> forwardedHeaderFilter(ServerProperties properties) {
      ForwardedHeaderFilter filter = new ForwardedHeaderFilter();

      if (ClassUtils.isPresent("org.apache.catalina.startup.Tomcat", getClass().getClassLoader())) {
        filter.setRelativeRedirects(properties.getTomcat().isUseRelativeRedirects());
      }

      var registration = new FilterRegistrationBean<>(filter);
      registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR);
      registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
      return registration;
    }

  }

  /**
   * Registers a {@link WebServerFactoryCustomizerBeanPostProcessor}. Registered via
   * {@link ImportBeanDefinitionRegistrar} for early registration.
   */
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
