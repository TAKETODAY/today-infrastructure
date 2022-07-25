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

package cn.taketoday.framework.web.servlet.config;

import java.util.function.Supplier;
import java.util.stream.Collectors;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.ObjectProvider;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.ImportBeanDefinitionRegistrar;
import cn.taketoday.context.annotation.config.AutoConfiguration;
import cn.taketoday.context.annotation.config.AutoConfigureOrder;
import cn.taketoday.context.annotation.config.EnableAutoConfiguration;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.context.condition.ConditionalOnProperty;
import cn.taketoday.context.loader.BootstrapContext;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.framework.annotation.ConditionalOnWebApplication;
import cn.taketoday.framework.annotation.ConditionalOnWebApplication.Type;
import cn.taketoday.framework.web.server.ErrorPageRegistrarBeanPostProcessor;
import cn.taketoday.framework.web.server.ServerProperties;
import cn.taketoday.framework.web.server.WebServerFactoryCustomizerBeanPostProcessor;
import cn.taketoday.framework.web.servlet.ConditionalOnMissingFilterBean;
import cn.taketoday.framework.web.servlet.FilterRegistrationBean;
import cn.taketoday.framework.web.servlet.WebListenerRegistrar;
import cn.taketoday.framework.web.servlet.server.CookieSameSiteSupplier;
import cn.taketoday.stereotype.Component;
import cn.taketoday.util.CollectionUtils;
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
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnClass(ServletRequest.class)
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
  public ServletWebServerFactoryCustomizer servletWebServerFactoryCustomizer(ServerProperties serverProperties,
          ObjectProvider<WebListenerRegistrar> webListenerRegistrars,
          ObjectProvider<CookieSameSiteSupplier> cookieSameSiteSuppliers) {
    return new ServletWebServerFactoryCustomizer(serverProperties,
            webListenerRegistrars.orderedStream().collect(Collectors.toList()),
            cookieSameSiteSuppliers.orderedStream().collect(Collectors.toList()));
  }

  @Component
  @ConditionalOnClass(name = "org.apache.catalina.startup.Tomcat")
  public TomcatServletWebServerFactoryCustomizer tomcatServletWebServerFactoryCustomizer(
          ServerProperties serverProperties) {
    return new TomcatServletWebServerFactoryCustomizer(serverProperties);
  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnProperty(value = "server.forward-headers-strategy", havingValue = "today")
  @ConditionalOnMissingFilterBean(ForwardedHeaderFilter.class)
  static class ForwardedHeaderFilterConfiguration {

    @Component
    @ConditionalOnClass(name = "org.apache.catalina.startup.Tomcat")
    ForwardedHeaderFilterCustomizer tomcatForwardedHeaderFilterCustomizer(ServerProperties serverProperties) {
      return filter -> filter.setRelativeRedirects(serverProperties.getTomcat().isUseRelativeRedirects());
    }

    @Component
    FilterRegistrationBean<ForwardedHeaderFilter> forwardedHeaderFilter(
            ObjectProvider<ForwardedHeaderFilterCustomizer> customizerProvider) {
      ForwardedHeaderFilter filter = new ForwardedHeaderFilter();
      customizerProvider.ifAvailable(customizer -> customizer.customize(filter));
      FilterRegistrationBean<ForwardedHeaderFilter> registration = new FilterRegistrationBean<>(filter);
      registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR);
      registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
      return registration;
    }

  }

  interface ForwardedHeaderFilterCustomizer {

    void customize(ForwardedHeaderFilter filter);

  }

  /**
   * Registers a {@link WebServerFactoryCustomizerBeanPostProcessor}. Registered via
   * {@link ImportBeanDefinitionRegistrar} for early registration.
   */
  public static class BeanPostProcessorsRegistrar implements ImportBeanDefinitionRegistrar, BeanFactoryAware {

    private ConfigurableBeanFactory beanFactory;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
      if (beanFactory instanceof ConfigurableBeanFactory) {
        this.beanFactory = (ConfigurableBeanFactory) beanFactory;
      }
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importMetadata, BootstrapContext context) {
      if (this.beanFactory == null) {
        return;
      }
      registerSyntheticBeanIfMissing(context.getRegistry(), "webServerFactoryCustomizerBeanPostProcessor",
              WebServerFactoryCustomizerBeanPostProcessor.class, WebServerFactoryCustomizerBeanPostProcessor::new);
      registerSyntheticBeanIfMissing(context.getRegistry(), "errorPageRegistrarBeanPostProcessor",
              ErrorPageRegistrarBeanPostProcessor.class, ErrorPageRegistrarBeanPostProcessor::new);
    }

    private <T> void registerSyntheticBeanIfMissing(
            BeanDefinitionRegistry registry, String name, Class<T> beanClass, Supplier<T> instanceSupplier) {
      if (CollectionUtils.isEmpty(beanFactory.getBeanNamesForType(beanClass, true, false))) {
        RootBeanDefinition beanDefinition = new RootBeanDefinition(beanClass, instanceSupplier);
        beanDefinition.setSynthetic(true);
        registry.registerBeanDefinition(name, beanDefinition);
      }
    }

  }

}
