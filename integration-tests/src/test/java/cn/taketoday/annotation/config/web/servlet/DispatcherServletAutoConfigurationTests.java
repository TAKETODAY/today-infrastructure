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

package cn.taketoday.annotation.config.web.servlet;

import org.junit.jupiter.api.Test;

import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.config.AutoConfigurations;
import cn.taketoday.framework.test.context.runner.WebApplicationContextRunner;
import cn.taketoday.framework.web.servlet.MultipartConfigFactory;
import cn.taketoday.framework.web.servlet.ServletRegistrationBean;
import cn.taketoday.util.DataSize;
import cn.taketoday.web.servlet.DispatcherServlet;
import jakarta.servlet.MultipartConfigElement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DispatcherServletAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Brian Clozel
 */
class DispatcherServletAutoConfigurationTests {

  private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(DispatcherServletAutoConfiguration.class));

  @Test
  void registrationProperties() {
    this.contextRunner.run((context) -> {
      assertThat(context.getBean(DispatcherServlet.class)).isNotNull();
      ServletRegistrationBean<?> registration = context.getBean(ServletRegistrationBean.class);
      assertThat(registration.getUrlMappings()).containsExactly("/");
    });
  }

  @Test
  void registrationNonServletBean() {
    this.contextRunner.withUserConfiguration(NonServletConfiguration.class).run((context) -> {
      assertThat(context).doesNotHaveBean(ServletRegistrationBean.class);
      assertThat(context).doesNotHaveBean(DispatcherServlet.class);
    });
  }

  // If a DispatcherServlet instance is registered with a name different
  // from the default one, we're registering one anyway
  @Test
  void registrationOverrideWithDispatcherServletWrongName() {
    this.contextRunner
            .withUserConfiguration(CustomDispatcherServletDifferentName.class)
            .run((context) -> {
              ServletRegistrationBean<?> registration = context.getBean(ServletRegistrationBean.class);
              assertThat(registration.getUrlMappings()).containsExactly("/");
              assertThat(registration.getServletName()).isEqualTo("dispatcherServlet");
              assertThat(context).getBeanNames(DispatcherServlet.class).hasSize(2);
            });
  }

  @Test
  void registrationOverrideWithAutowiredServlet() {
    this.contextRunner.withUserConfiguration(CustomAutowiredRegistration.class).run((context) -> {
      ServletRegistrationBean<?> registration = context.getBean(ServletRegistrationBean.class);
      assertThat(registration.getUrlMappings()).containsExactly("/foo");
      assertThat(registration.getServletName()).isEqualTo("customDispatcher");
      assertThat(context).hasSingleBean(DispatcherServlet.class);
    });
  }

  @Test
  void servletPath() {
    this.contextRunner.withPropertyValues("mvc.servlet.path:/spring").run((context) -> {
      assertThat(context.getBean(DispatcherServlet.class)).isNotNull();
      ServletRegistrationBean<?> registration = context.getBean(ServletRegistrationBean.class);
      assertThat(registration.getUrlMappings()).containsExactly("/spring/*");
      assertThat(registration.getMultipartConfig()).isNull();
    });
  }

  @Test
  void multipartConfig() {
    this.contextRunner.withUserConfiguration(MultipartConfiguration.class).run((context) -> {
      ServletRegistrationBean<?> registration = context.getBean(ServletRegistrationBean.class);
      assertThat(registration.getMultipartConfig()).isNotNull();
    });
  }

  @Test
  void dispatcherServletDefaultConfig() {
    this.contextRunner.run((context) -> {
      DispatcherServlet dispatcherServlet = context.getBean(DispatcherServlet.class);
      assertThat(dispatcherServlet).extracting("throwExceptionIfNoHandlerFound").isEqualTo(false);
      assertThat(dispatcherServlet).extracting("dispatchOptionsRequest").isEqualTo(true);
      assertThat(dispatcherServlet).extracting("dispatchTraceRequest").isEqualTo(false);
      assertThat(dispatcherServlet).extracting("enableLoggingRequestDetails").isEqualTo(false);
      assertThat(dispatcherServlet).extracting("publishEvents").isEqualTo(true);
      assertThat(context.getBean("dispatcherServletRegistration")).hasFieldOrPropertyWithValue("loadOnStartup",
              -1);
    });
  }

  @Test
  void dispatcherServletCustomConfig() {
    this.contextRunner
            .withPropertyValues("mvc.throw-exception-if-no-handler-found:true",
                    "mvc.dispatch-options-request:false", "mvc.dispatch-trace-request:true",
                    "mvc.publish-request-handled-events:false", "mvc.servlet.load-on-startup=5")
            .run((context) -> {
              DispatcherServlet dispatcherServlet = context.getBean(DispatcherServlet.class);
              assertThat(dispatcherServlet).extracting("throwExceptionIfNoHandlerFound").isEqualTo(true);
              assertThat(dispatcherServlet).extracting("dispatchOptionsRequest").isEqualTo(false);
              assertThat(dispatcherServlet).extracting("dispatchTraceRequest").isEqualTo(true);
              assertThat(dispatcherServlet).extracting("publishEvents").isEqualTo(false);
              assertThat(context.getBean("dispatcherServletRegistration"))
                      .hasFieldOrPropertyWithValue("loadOnStartup", 5);
            });
  }

  @Configuration(proxyBeanMethods = false)
  static class MultipartConfiguration {

    @Bean
    MultipartConfigElement multipartConfig() {
      MultipartConfigFactory factory = new MultipartConfigFactory();
      factory.setMaxFileSize(DataSize.ofKilobytes(128));
      factory.setMaxRequestSize(DataSize.ofKilobytes(128));
      return factory.createMultipartConfig();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class CustomDispatcherServletDifferentName {

    @Bean
    DispatcherServlet customDispatcherServlet() {
      return new DispatcherServlet();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class CustomAutowiredRegistration {

    @Bean
    ServletRegistrationBean<?> dispatcherServletRegistration(DispatcherServlet dispatcherServlet) {
      ServletRegistrationBean<DispatcherServlet> registration = new ServletRegistrationBean<>(dispatcherServlet,
              "/foo");
      registration.setName("customDispatcher");
      return registration;
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class NonServletConfiguration {

    @Bean
    String dispatcherServlet() {
      return "spring";
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class CustomDispatcherServletSameName {

    @Bean(name = DispatcherServletAutoConfiguration.DEFAULT_DISPATCHER_SERVLET_BEAN_NAME)
    DispatcherServlet dispatcherServlet() {
      return new DispatcherServlet();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class CustomDispatcherServletRegistration {

    @Bean(name = DispatcherServletAutoConfiguration.DEFAULT_DISPATCHER_SERVLET_REGISTRATION_BEAN_NAME)
    ServletRegistrationBean<DispatcherServlet> dispatcherServletRegistration(DispatcherServlet dispatcherServlet) {
      ServletRegistrationBean<DispatcherServlet> registration = new ServletRegistrationBean<>(dispatcherServlet,
              "/foo");
      registration.setName("customDispatcher");
      return registration;
    }

  }

}
