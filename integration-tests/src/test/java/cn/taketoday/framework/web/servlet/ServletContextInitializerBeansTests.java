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

package cn.taketoday.framework.web.servlet;

import org.junit.jupiter.api.Test;

import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpSessionIdListener;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ServletContextInitializerBeans}.
 *
 * @author Andy Wilkinson
 */
class ServletContextInitializerBeansTests {

  private ConfigurableApplicationContext context;

  @Test
  void servletThatImplementsServletContextInitializerIsOnlyRegisteredOnce() {
    load(ServletConfiguration.class);
    ServletContextInitializerBeans initializerBeans = new ServletContextInitializerBeans(
            this.context.getBeanFactory());
    assertThat(initializerBeans.size()).isEqualTo(1);
    assertThat(initializerBeans.iterator()).toIterable().hasOnlyElementsOfType(TestServlet.class);
  }

  @Test
  void filterThatImplementsServletContextInitializerIsOnlyRegisteredOnce() {
    load(FilterConfiguration.class);
    ServletContextInitializerBeans initializerBeans = new ServletContextInitializerBeans(
            this.context.getBeanFactory());
    assertThat(initializerBeans.size()).isEqualTo(1);
    assertThat(initializerBeans.iterator()).toIterable().hasOnlyElementsOfType(TestFilter.class);
  }

  @Test
  void looksForInitializerBeansOfSpecifiedType() {
    load(TestConfiguration.class);
    ServletContextInitializerBeans initializerBeans = new ServletContextInitializerBeans(
            this.context.getBeanFactory(), TestServletContextInitializer.class);
    assertThat(initializerBeans.size()).isEqualTo(1);
    assertThat(initializerBeans.iterator()).toIterable().hasOnlyElementsOfType(TestServletContextInitializer.class);
  }

  @Test
  void whenAnHttpSessionIdListenerBeanIsDefinedThenARegistrationBeanIsCreatedForIt() {
    load(HttpSessionIdListenerConfiguration.class);
    ServletContextInitializerBeans initializerBeans = new ServletContextInitializerBeans(
            this.context.getBeanFactory());
    assertThat(initializerBeans).hasSize(1);
    assertThat(initializerBeans).first().isInstanceOf(ServletListenerRegistrationBean.class)
            .extracting((initializer) -> ((ServletListenerRegistrationBean<?>) initializer).getListener())
            .isInstanceOf(HttpSessionIdListener.class);
  }

  private void load(Class<?>... configuration) {
    this.context = new AnnotationConfigApplicationContext(configuration);
  }

  @Configuration(proxyBeanMethods = false)
  static class ServletConfiguration {

    @Bean
    TestServlet testServlet() {
      return new TestServlet();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class FilterConfiguration {

    @Bean
    TestFilter testFilter() {
      return new TestFilter();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class TestConfiguration {

    @Bean
    TestServletContextInitializer testServletContextInitializer() {
      return new TestServletContextInitializer();
    }

    @Bean
    OtherTestServletContextInitializer otherTestServletContextInitializer() {
      return new OtherTestServletContextInitializer();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class HttpSessionIdListenerConfiguration {

    @Bean
    HttpSessionIdListener httpSessionIdListener() {
      return (event, oldId) -> {
      };
    }

  }

  @SuppressWarnings("serial")
  static class TestServlet extends HttpServlet implements ServletContextInitializer {

    @Override
    public void onStartup(ServletContext servletContext) {

    }

  }

  static class TestFilter implements Filter, ServletContextInitializer {

    @Override
    public void onStartup(ServletContext servletContext) {

    }

    @Override
    public void init(FilterConfig filterConfig) {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {

    }

    @Override
    public void destroy() {

    }

  }

  static class TestServletContextInitializer implements ServletContextInitializer {

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {

    }

  }

  static class OtherTestServletContextInitializer implements ServletContextInitializer {

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {

    }

  }

}
