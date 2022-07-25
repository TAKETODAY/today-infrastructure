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

import java.net.URI;

import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.PropertySource;
import cn.taketoday.core.env.Environment;
import cn.taketoday.framework.web.embedded.jetty.JettyServletWebServerFactory;
import cn.taketoday.framework.web.embedded.tomcat.TomcatServletWebServerFactory;
import cn.taketoday.framework.web.embedded.undertow.UndertowServletWebServerFactory;
import cn.taketoday.framework.web.server.WebServer;
import cn.taketoday.framework.web.servlet.ServletRegistrationBean;
import cn.taketoday.framework.web.servlet.server.ServletWebServerFactory;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.client.ClientHttpRequest;
import cn.taketoday.http.client.ClientHttpResponse;
import cn.taketoday.http.client.SimpleClientHttpRequestFactory;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.ResponseBody;
import cn.taketoday.web.config.EnableWebMvc;
import cn.taketoday.web.servlet.DispatcherServlet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ServletWebServerApplicationContext} and {@link WebServer}s
 * running Spring MVC.
 *
 * @author Phillip Webb
 * @author Ivan Sopov
 */
class ServletWebServerMvcIntegrationTests {

  private AnnotationConfigServletWebServerApplicationContext context;

  @AfterEach
  void closeContext() {
    try {
      this.context.close();
    }
    catch (Exception ex) {
      // Ignore
    }
  }

  @Test
  void tomcat() throws Exception {
    this.context = new AnnotationConfigServletWebServerApplicationContext(TomcatConfig.class);
    doTest(this.context, "/hello");
  }

  @Test
  void jetty() throws Exception {
    this.context = new AnnotationConfigServletWebServerApplicationContext(JettyConfig.class);
    doTest(this.context, "/hello");
  }

  @Test
  void undertow() throws Exception {
    this.context = new AnnotationConfigServletWebServerApplicationContext(UndertowConfig.class);
    doTest(this.context, "/hello");
  }

  @Test
  void advancedConfig() throws Exception {
    this.context = new AnnotationConfigServletWebServerApplicationContext(AdvancedConfig.class);
    doTest(this.context, "/example/spring/hello");
  }

  private void doTest(AnnotationConfigServletWebServerApplicationContext context, String resourcePath)
          throws Exception {
    SimpleClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
    ClientHttpRequest request = clientHttpRequestFactory.createRequest(
            new URI("http://localhost:" + context.getWebServer().getPort() + resourcePath), HttpMethod.GET);
    try (ClientHttpResponse response = request.execute()) {
      assertThat(response.getBody()).hasContent("Hello World");
    }
  }

  // Simple main method for testing in a browser
  @SuppressWarnings("resource")
  static void main(String[] args) {
    new AnnotationConfigServletWebServerApplicationContext(JettyServletWebServerFactory.class, Config.class);
  }

  @Configuration(proxyBeanMethods = false)
  @Import(Config.class)
  static class TomcatConfig {

    @Bean
    ServletWebServerFactory webServerFactory() {
      return new TomcatServletWebServerFactory(0);
    }

  }

  @Configuration(proxyBeanMethods = false)
  @Import(Config.class)
  static class JettyConfig {

    @Bean
    ServletWebServerFactory webServerFactory() {
      return new JettyServletWebServerFactory(0);
    }

  }

  @Configuration(proxyBeanMethods = false)
  @Import(Config.class)
  static class UndertowConfig {

    @Bean
    ServletWebServerFactory webServerFactory() {
      return new UndertowServletWebServerFactory(0);
    }

  }

  @Configuration(proxyBeanMethods = false)
  @EnableWebMvc
  static class Config {

    @Bean
    DispatcherServlet dispatcherServlet() {
      return new DispatcherServlet();
      // Alternatively you can use ServletContextInitializer beans including
      // ServletRegistration and FilterRegistration. Read the
      // EmbeddedWebApplicationContext Javadoc for details.
    }

    @Bean
    HelloWorldController helloWorldController() {
      return new HelloWorldController();
    }

  }

  @Configuration(proxyBeanMethods = false)
  @EnableWebMvc
  @PropertySource("classpath:/cn/taketoday/framework/web/servlet/context/conf.properties")
  static class AdvancedConfig {

    private final Environment env;

    AdvancedConfig(Environment env) {
      this.env = env;
    }

    @Bean
    ServletWebServerFactory webServerFactory() {
      JettyServletWebServerFactory factory = new JettyServletWebServerFactory(0);
      factory.setContextPath(this.env.getProperty("context"));
      return factory;
    }

    @Bean
    ServletRegistrationBean<DispatcherServlet> dispatcherRegistration(DispatcherServlet dispatcherServlet) {
      ServletRegistrationBean<DispatcherServlet> registration = new ServletRegistrationBean<>(dispatcherServlet);
      registration.addUrlMappings("/spring/*");
      return registration;
    }

    @Bean
    DispatcherServlet dispatcherServlet() {
      // Can configure dispatcher servlet here as would usually do via init-params
      return new DispatcherServlet();
    }

    @Bean
    HelloWorldController helloWorldController() {
      return new HelloWorldController();
    }

  }

  @Controller
  static class HelloWorldController {

    @RequestMapping("/hello")
    @ResponseBody
    String sayHello() {
      return "Hello World";
    }

  }

}
