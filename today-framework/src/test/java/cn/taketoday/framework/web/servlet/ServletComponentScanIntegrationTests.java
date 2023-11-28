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

package cn.taketoday.framework.web.servlet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import cn.taketoday.beans.factory.ObjectProvider;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.framework.web.context.ServerPortInfoApplicationContextInitializer;
import cn.taketoday.framework.web.embedded.jetty.JettyServletWebServerFactory;
import cn.taketoday.framework.web.embedded.tomcat.TomcatServletWebServerFactory;
import cn.taketoday.framework.web.embedded.undertow.UndertowServletWebServerFactory;
import cn.taketoday.framework.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import cn.taketoday.framework.web.servlet.server.ConfigurableServletWebServerFactory;
import cn.taketoday.framework.web.servlet.server.ServletWebServerFactory;
import cn.taketoday.framework.web.servlet.testcomponents.TestMultipartServlet;
import cn.taketoday.test.web.servlet.DirtiesUrlFactories;
import cn.taketoday.web.client.RestTemplate;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.annotation.WebServlet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ServletComponentScan @ServletComponentScan}
 *
 * @author Andy Wilkinson
 */
@DirtiesUrlFactories
class ServletComponentScanIntegrationTests {

  private AnnotationConfigServletWebServerApplicationContext context;

  @TempDir
  File temp;

  @AfterEach
  void cleanUp() {
    if (this.context != null) {
      this.context.close();
    }
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("testConfiguration")
  void componentsAreRegistered(String serverName, Class<?> configuration) {
    this.context = new AnnotationConfigServletWebServerApplicationContext();
    this.context.register(configuration);
    new ServerPortInfoApplicationContextInitializer().initialize(this.context);
    this.context.refresh();
    String port = this.context.getEnvironment().getProperty("local.server.port");
    String response = new RestTemplate().getForObject("http://localhost:" + port + "/test", String.class);
    assertThat(response).isEqualTo("alpha bravo charlie");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("testConfiguration")
  void indexedComponentsAreRegistered(String serverName, Class<?> configuration) throws IOException {
    writeIndex(this.temp);
    this.context = new AnnotationConfigServletWebServerApplicationContext();
    try (URLClassLoader classLoader = new URLClassLoader(new URL[] { this.temp.toURI().toURL() },
            getClass().getClassLoader())) {
      this.context.setClassLoader(classLoader);
      this.context.register(configuration);
      new ServerPortInfoApplicationContextInitializer().initialize(this.context);
      this.context.refresh();
      String port = this.context.getEnvironment().getProperty("local.server.port");
      String response = new RestTemplate().getForObject("http://localhost:" + port + "/test", String.class);
      assertThat(response).isEqualTo("alpha bravo charlie");
    }
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("testConfiguration")
  void multipartConfigIsHonoured(String serverName, Class<?> configuration) {
    this.context = new AnnotationConfigServletWebServerApplicationContext();
    this.context.register(configuration);
    new ServerPortInfoApplicationContextInitializer().initialize(this.context);
    this.context.refresh();
    @SuppressWarnings("rawtypes")
    Map<String, ServletRegistrationBean> beans = this.context.getBeansOfType(ServletRegistrationBean.class);
    ServletRegistrationBean<?> servletRegistrationBean = beans.get(TestMultipartServlet.class.getName());
    assertThat(servletRegistrationBean).isNotNull();
    MultipartConfigElement multipartConfig = servletRegistrationBean.getMultipartConfig();
    assertThat(multipartConfig).isNotNull();
    assertThat(multipartConfig.getLocation()).isEqualTo("test");
    assertThat(multipartConfig.getMaxRequestSize()).isEqualTo(2048);
    assertThat(multipartConfig.getMaxFileSize()).isEqualTo(1024);
    assertThat(multipartConfig.getFileSizeThreshold()).isEqualTo(512);
  }

  private void writeIndex(File temp) throws IOException {
    File metaInf = new File(temp, "META-INF");
    metaInf.mkdirs();
    Properties index = new Properties();
    index.setProperty("cn.taketoday.framework.web.servlet.testcomponents.TestFilter", WebFilter.class.getName());
    index.setProperty("cn.taketoday.framework.web.servlet.testcomponents.TestListener",
            WebListener.class.getName());
    index.setProperty("cn.taketoday.framework.web.servlet.testcomponents.TestServlet",
            WebServlet.class.getName());
    try (FileWriter writer = new FileWriter(new File(metaInf, "spring.components"))) {
      index.store(writer, null);
    }
  }

  static Stream<Arguments> testConfiguration() {
    return Stream.of(Arguments.of("Jetty", JettyTestConfiguration.class),
            Arguments.of("Tomcat", TomcatTestConfiguration.class),
            Arguments.of("Undertow", UndertowTestConfiguration.class));
  }

  @ServletComponentScan(basePackages = "cn.taketoday.framework.web.servlet.testcomponents")
  abstract static class AbstractTestConfiguration {

    @Bean
    protected ServletWebServerFactory webServerFactory(ObjectProvider<WebListenerRegistrar> webListenerRegistrars) {
      ConfigurableServletWebServerFactory factory = createWebServerFactory();
      webListenerRegistrars.orderedStream().forEach((registrar) -> registrar.register(factory));
      return factory;
    }

    abstract ConfigurableServletWebServerFactory createWebServerFactory();

  }

  @Configuration(proxyBeanMethods = false)
  static class JettyTestConfiguration extends AbstractTestConfiguration {

    @Override
    ConfigurableServletWebServerFactory createWebServerFactory() {
      return new JettyServletWebServerFactory(0);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class TomcatTestConfiguration extends AbstractTestConfiguration {

    @Override
    ConfigurableServletWebServerFactory createWebServerFactory() {
      return new TomcatServletWebServerFactory(0);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class UndertowTestConfiguration extends AbstractTestConfiguration {

    @Override
    ConfigurableServletWebServerFactory createWebServerFactory() {
      return new UndertowServletWebServerFactory(0);
    }

  }

}
