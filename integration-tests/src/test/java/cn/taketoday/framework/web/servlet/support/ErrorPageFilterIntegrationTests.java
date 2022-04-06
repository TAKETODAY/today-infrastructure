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

package cn.taketoday.framework.web.servlet.support;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.framework.web.embedded.tomcat.TomcatServletWebServerFactory;
import cn.taketoday.framework.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import cn.taketoday.framework.web.servlet.server.ServletWebServerFactory;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.test.annotation.DirtiesContext;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.MergedContextConfiguration;
import cn.taketoday.test.context.junit.jupiter.SpringExtension;
import cn.taketoday.test.context.support.AbstractContextLoader;
import cn.taketoday.web.bind.annotation.RequestMapping;
import cn.taketoday.web.bind.annotation.ResponseBody;
import cn.taketoday.web.bind.annotation.ResponseStatus;
import cn.taketoday.web.client.RestTemplate;
import cn.taketoday.web.servlet.DispatcherServlet;
import cn.taketoday.web.servlet.HandlerInterceptor;
import cn.taketoday.web.servlet.ModelAndView;
import cn.taketoday.web.servlet.config.annotation.EnableWebMvc;
import cn.taketoday.web.servlet.config.annotation.InterceptorRegistry;
import cn.taketoday.web.servlet.config.annotation.WebMvcConfigurer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ErrorPageFilter}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 */
@ExtendWith(SpringExtension.class)
@DirtiesContext
@ContextConfiguration(classes = ErrorPageFilterIntegrationTests.TomcatConfig.class,
                      loader = ErrorPageFilterIntegrationTests.EmbeddedWebContextLoader.class)
class ErrorPageFilterIntegrationTests {

  @Autowired
  private HelloWorldController controller;

  @Autowired
  private AnnotationConfigServletWebServerApplicationContext context;

  @AfterEach
  void init() {
    this.controller.reset();
  }

  @Test
  void created() throws Exception {
    doTest(this.context, "/create", HttpStatus.CREATED);
    assertThat(this.controller.getStatus()).isEqualTo(201);
  }

  @Test
  void ok() throws Exception {
    doTest(this.context, "/hello", HttpStatus.OK);
    assertThat(this.controller.getStatus()).isEqualTo(200);
  }

  private void doTest(AnnotationConfigServletWebServerApplicationContext context, String resourcePath,
          HttpStatus status) throws Exception {
    int port = context.getWebServer().getPort();
    RestTemplate template = new RestTemplate();
    ResponseEntity<String> entity = template.getForEntity(new URI("http://localhost:" + port + resourcePath),
            String.class);
    assertThat(entity.getBody()).isEqualTo("Hello World");
    assertThat(entity.getStatusCode()).isEqualTo(status);
  }

  @Configuration(proxyBeanMethods = false)
  @EnableWebMvc
  static class TomcatConfig {

    @Bean
    ServletWebServerFactory webServerFactory() {
      return new TomcatServletWebServerFactory(0);
    }

    @Bean
    ErrorPageFilter errorPageFilter() {
      return new ErrorPageFilter();
    }

    @Bean
    DispatcherServlet dispatcherServlet() {
      return new DispatcherServlet();
    }

    @Bean
    HelloWorldController helloWorldController() {
      return new HelloWorldController();
    }

  }

  @Controller
  static class HelloWorldController implements WebMvcConfigurer {

    private int status;

    private CountDownLatch latch = new CountDownLatch(1);

    int getStatus() throws InterruptedException {
      assertThat(this.latch.await(1, TimeUnit.SECONDS)).as("Timed out waiting for latch").isTrue();
      return this.status;
    }

    void setStatus(int status) {
      this.status = status;
    }

    void reset() {
      this.status = 0;
      this.latch = new CountDownLatch(1);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
      registry.addInterceptor(new HandlerInterceptor() {
        @Override
        public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                ModelAndView modelAndView) {
          HelloWorldController.this.setStatus(response.getStatus());
          HelloWorldController.this.latch.countDown();
        }
      });
    }

    @RequestMapping("/hello")
    @ResponseBody
    String sayHello() {
      return "Hello World";
    }

    @RequestMapping("/create")
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
    String created() {
      return "Hello World";
    }

  }

  static class EmbeddedWebContextLoader extends AbstractContextLoader {

    private static final String[] EMPTY_RESOURCE_SUFFIXES = {};

    @Override
    public ApplicationContext loadContext(MergedContextConfiguration config) {
      AnnotationConfigServletWebServerApplicationContext context = new AnnotationConfigServletWebServerApplicationContext(
              config.getClasses());
      context.registerShutdownHook();
      return context;
    }

    @Override
    public ApplicationContext loadContext(String... locations) {
      throw new UnsupportedOperationException();
    }

    @Override
    protected String[] getResourceSuffixes() {
      return EMPTY_RESOURCE_SUFFIXES;
    }

    @Override
    protected String getResourceSuffix() {
      throw new UnsupportedOperationException();
    }

  }

}
