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
import cn.taketoday.framework.web.embedded.jetty.JettyServletWebServerFactory;
import cn.taketoday.framework.web.embedded.tomcat.TomcatServletWebServerFactory;
import cn.taketoday.framework.web.embedded.undertow.UndertowServletWebServerFactory;
import cn.taketoday.framework.web.server.WebServer;
import cn.taketoday.framework.web.servlet.ServletListenerRegistrationBean;
import cn.taketoday.framework.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import cn.taketoday.framework.web.servlet.server.ServletWebServerFactory;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link WebServer}s driving {@link ServletContextListener}s correctly
 *
 * @author Andy Wilkinson
 */
class ServletWebServerServletContextListenerTests {

  @Test
  void registeredServletContextListenerBeanIsCalledByJetty() {
    registeredServletContextListenerBeanIsCalled(JettyConfiguration.class);
  }

  @Test
  void registeredServletContextListenerBeanIsCalledByTomcat() {
    registeredServletContextListenerBeanIsCalled(TomcatConfiguration.class);
  }

  @Test
  void registeredServletContextListenerBeanIsCalledByUndertow() {
    registeredServletContextListenerBeanIsCalled(UndertowConfiguration.class);
  }

  @Test
  void servletContextListenerBeanIsCalledByJetty() {
    servletContextListenerBeanIsCalled(JettyConfiguration.class);
  }

  @Test
  void servletContextListenerBeanIsCalledByTomcat() {
    servletContextListenerBeanIsCalled(TomcatConfiguration.class);
  }

  @Test
  void servletContextListenerBeanIsCalledByUndertow() {
    servletContextListenerBeanIsCalled(UndertowConfiguration.class);
  }

  private void servletContextListenerBeanIsCalled(Class<?> configuration) {
    AnnotationConfigServletWebServerApplicationContext context = new AnnotationConfigServletWebServerApplicationContext(
            ServletContextListenerBeanConfiguration.class, configuration);
    ServletContextListener servletContextListener = context.getBean("servletContextListener",
            ServletContextListener.class);
    then(servletContextListener).should().contextInitialized(any(ServletContextEvent.class));
    context.close();
  }

  private void registeredServletContextListenerBeanIsCalled(Class<?> configuration) {
    AnnotationConfigServletWebServerApplicationContext context = new AnnotationConfigServletWebServerApplicationContext(
            ServletListenerRegistrationBeanConfiguration.class, configuration);
    ServletContextListener servletContextListener = (ServletContextListener) context
            .getBean("registration", ServletListenerRegistrationBean.class).getListener();
    then(servletContextListener).should().contextInitialized(any(ServletContextEvent.class));
    context.close();
  }

  @Configuration(proxyBeanMethods = false)
  static class TomcatConfiguration {

    @Bean
    ServletWebServerFactory webServerFactory() {
      return new TomcatServletWebServerFactory(0);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class JettyConfiguration {

    @Bean
    ServletWebServerFactory webServerFactory() {
      return new JettyServletWebServerFactory(0);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class UndertowConfiguration {

    @Bean
    ServletWebServerFactory webServerFactory() {
      return new UndertowServletWebServerFactory(0);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class ServletContextListenerBeanConfiguration {

    @Bean
    ServletContextListener servletContextListener() {
      return mock(ServletContextListener.class);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class ServletListenerRegistrationBeanConfiguration {

    @Bean
    ServletListenerRegistrationBean<ServletContextListener> registration() {
      return new ServletListenerRegistrationBean<>(mock(ServletContextListener.class));
    }

  }

}
