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

package cn.taketoday.framework.web.servlet.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Scope;
import cn.taketoday.context.annotation.ScopedProxyMode;
import cn.taketoday.framework.web.servlet.context.config.ExampleServletWebServerApplicationConfiguration;
import cn.taketoday.framework.web.servlet.mock.MockServlet;
import cn.taketoday.framework.web.servlet.server.MockServletWebServerFactory;
import cn.taketoday.framework.web.servlet.server.ServletWebServerFactory;
import cn.taketoday.lang.Component;
import cn.taketoday.web.config.EnableWebMvc;
import cn.taketoday.web.servlet.ServletContextAware;
import jakarta.servlet.GenericServlet;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

/**
 * Tests for {@link AnnotationConfigServletWebServerApplicationContext}.
 *
 * @author Phillip Webb
 */
class AnnotationConfigServletWebServerApplicationContextTests {

  private AnnotationConfigServletWebServerApplicationContext context;

  @AfterEach
  void close() {
    if (this.context != null) {
      this.context.close();
    }
  }

  @Test
  void createFromScan() {
    this.context = new AnnotationConfigServletWebServerApplicationContext(
            ExampleServletWebServerApplicationConfiguration.class.getPackage().getName());
    verifyContext();
  }

  @Test
  void sessionScopeAvailable() {
    this.context = new AnnotationConfigServletWebServerApplicationContext(
            ExampleServletWebServerApplicationConfiguration.class, SessionScopedComponent.class);
    verifyContext();
  }

  @Test
  void sessionScopeAvailableToServlet() {
    this.context = new AnnotationConfigServletWebServerApplicationContext(
            ExampleServletWebServerApplicationConfiguration.class, ExampleServletWithAutowired.class,
            SessionScopedComponent.class);
    Servlet servlet = this.context.getBean(ExampleServletWithAutowired.class);
    assertThat(servlet).isNotNull();
  }

  @Test
  void createFromConfigClass() {
    this.context = new AnnotationConfigServletWebServerApplicationContext(
            ExampleServletWebServerApplicationConfiguration.class);
    verifyContext();
  }

  @Test
  void registerAndRefresh() {
    this.context = new AnnotationConfigServletWebServerApplicationContext();
    this.context.register(ExampleServletWebServerApplicationConfiguration.class);
    this.context.refresh();
    verifyContext();
  }

  @Test
  void multipleRegistersAndRefresh() {
    this.context = new AnnotationConfigServletWebServerApplicationContext();
    this.context.register(WebServerConfiguration.class);
    this.context.register(ServletContextAwareConfiguration.class);
    this.context.refresh();
    assertThat(this.context.getBeansOfType(Servlet.class)).hasSize(1);
    assertThat(this.context.getBeansOfType(ServletWebServerFactory.class)).hasSize(1);
  }

  @Test
  void scanAndRefresh() {
    this.context = new AnnotationConfigServletWebServerApplicationContext();
    this.context.scan(ExampleServletWebServerApplicationConfiguration.class.getPackage().getName());
    this.context.refresh();
    verifyContext();
  }

  @Test
  void createAndInitializeCyclic() {
    this.context = new AnnotationConfigServletWebServerApplicationContext(
            ServletContextAwareEmbeddedConfiguration.class);
    verifyContext();
    // You can't initialize the application context and inject the servlet context
    // because of a cycle - we'd like this to be not null but it never will be
    assertThat(this.context.getBean(ServletContextAwareEmbeddedConfiguration.class).getServletContext()).isNull();
  }

  @Test
  void createAndInitializeWithParent() {
    AnnotationConfigServletWebServerApplicationContext parent = new AnnotationConfigServletWebServerApplicationContext(
            WebServerConfiguration.class);
    this.context = new AnnotationConfigServletWebServerApplicationContext();
    this.context.register(WebServerConfiguration.class, ServletContextAwareConfiguration.class);
    this.context.setParent(parent);
    this.context.refresh();
    verifyContext();
    assertThat(this.context.getBean(ServletContextAwareConfiguration.class).getServletContext()).isNotNull();
  }

  private void verifyContext() {
    MockServletWebServerFactory factory = this.context.getBean(MockServletWebServerFactory.class);
    Servlet servlet = this.context.getBean(Servlet.class);
    then(factory.getServletContext()).should().addServlet("servlet", servlet);
  }

  @Component
  static class ExampleServletWithAutowired extends GenericServlet {

    @Autowired
    private SessionScopedComponent component;

    @Override
    public void service(ServletRequest req, ServletResponse res) {
      assertThat(this.component).isNotNull();
    }

  }

  @Component
  @Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
  static class SessionScopedComponent {

  }

  @Configuration(proxyBeanMethods = false)
  @EnableWebMvc
  static class ServletContextAwareEmbeddedConfiguration implements ServletContextAware {

    private ServletContext servletContext;

    @Bean
    ServletWebServerFactory webServerFactory() {
      return new MockServletWebServerFactory();
    }

    @Bean
    Servlet servlet() {
      return new MockServlet();
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
      this.servletContext = servletContext;
    }

    ServletContext getServletContext() {
      return this.servletContext;
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class WebServerConfiguration {

    @Bean
    ServletWebServerFactory webServerFactory() {
      return new MockServletWebServerFactory();
    }

  }

  @Configuration(proxyBeanMethods = false)
  @EnableWebMvc
  static class ServletContextAwareConfiguration implements ServletContextAware {

    private ServletContext servletContext;

    @Bean
    Servlet servlet() {
      return new MockServlet();
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
      this.servletContext = servletContext;
    }

    ServletContext getServletContext() {
      return this.servletContext;
    }

  }

}
