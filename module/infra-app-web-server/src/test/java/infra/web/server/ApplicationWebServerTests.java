/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.server;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Iterator;

import infra.app.Application;
import infra.app.ApplicationType;
import infra.app.context.event.ApplicationEnvironmentPreparedEvent;
import infra.app.web.context.StandardWebEnvironment;
import infra.app.web.context.reactive.ReactiveWebApplicationContext;
import infra.app.web.context.reactive.StandardReactiveWebEnvironment;
import infra.context.ApplicationListener;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.core.env.PropertySource;
import infra.http.server.reactive.HttpHandler;
import infra.test.classpath.resources.WithResource;
import infra.test.context.support.TestPropertySourceUtils;
import infra.web.server.context.AnnotationConfigWebServerApplicationContext;
import infra.web.server.context.ConfigurableWebServerApplicationContext;
import infra.web.server.context.WebServerApplicationContext;
import infra.web.server.reactive.MockReactiveWebServerFactory;
import infra.web.server.reactive.context.AnnotationConfigReactiveWebServerApplicationContext;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Application} with a {@link WebServer}.
 *
 * @author Andy wilkinson
 */
class ApplicationWebServerTests {

  private @Nullable String headlessProperty;

  private @Nullable ConfigurableApplicationContext context;

  @BeforeEach
  void storeAndClearHeadlessProperty() {
    this.headlessProperty = System.getProperty("java.awt.headless");
    System.clearProperty("java.awt.headless");
  }

  @AfterEach
  void reinstateHeadlessProperty() {
    if (this.headlessProperty == null) {
      System.clearProperty("java.awt.headless");
    }
    else {
      System.setProperty("java.awt.headless", this.headlessProperty);
    }
  }

  @AfterEach
  void cleanUp() {
    if (this.context != null) {
      this.context.close();
    }
    System.clearProperty("app.main.banner-mode");
  }

  @Test
  void defaultApplicationContextForWeb() {
    Application application = new Application(ExampleWebConfig.class);
    application.setApplicationType(ApplicationType.WEB);
    this.context = application.run();
    assertThat(this.context).isInstanceOf(AnnotationConfigWebServerApplicationContext.class);
  }

  @Test
  void defaultApplicationContextForReactiveWeb() {
    Application application = new Application(ExampleReactiveWebConfig.class);
    application.setApplicationType(ApplicationType.REACTIVE_WEB);
    this.context = application.run();
    assertThat(this.context).isInstanceOf(AnnotationConfigReactiveWebServerApplicationContext.class);
  }

  @Test
  void environmentForWeb() {
    Application application = new Application(ExampleWebConfig.class);
    application.setApplicationType(ApplicationType.WEB);
    this.context = application.run();
    assertThat(this.context.getEnvironment()).isInstanceOf(StandardWebEnvironment.class);
    assertThat(this.context.getEnvironment().getClass().getName()).endsWith("ApplicationWebEnvironment");
  }

  @Test
  void environmentForReactiveWeb() {
    Application application = new Application(ExampleReactiveWebConfig.class);
    application.setApplicationType(ApplicationType.REACTIVE_WEB);
    this.context = application.run();
    assertThat(this.context.getEnvironment()).isInstanceOf(StandardReactiveWebEnvironment.class);
    assertThat(this.context.getEnvironment().getClass().getName()).endsWith("ApplicationReactiveWebEnvironment");
  }

  @Test
  void webApplicationConfiguredViaAPropertyHasTheCorrectTypeOfContextAndEnvironment() {
    ConfigurableApplicationContext context = new Application(ExampleWebConfig.class)
            .run("--app.main.application-type=web");
    assertThat(context).isInstanceOf(ConfigurableWebServerApplicationContext.class);
    assertThat(context.getEnvironment()).isInstanceOf(StandardWebEnvironment.class);
    assertThat(context.getEnvironment().getClass().getName()).endsWith("ApplicationWebEnvironment");
  }

  @Test
  void reactiveApplicationConfiguredViaAPropertyHasTheCorrectTypeOfContextAndEnvironment() {
    ConfigurableApplicationContext context = new Application(ExampleReactiveWebConfig.class)
            .run("--app.main.application-type=reactive_web");
    assertThat(context).isInstanceOf(ReactiveWebApplicationContext.class);
    assertThat(context.getEnvironment()).isInstanceOf(StandardReactiveWebEnvironment.class);
    assertThat(context.getEnvironment().getClass().getName()).endsWith("ApplicationReactiveWebEnvironment");
  }

  @Test
  @WithResource(name = "application-withApplicationType.properties",
          content = "app.main.application-type=reactive_web")
  void environmentIsConvertedIfTypeDoesNotMatch() {
    ConfigurableApplicationContext context = new Application(ExampleReactiveWebConfig.class)
            .run("--infra.profiles.active=withApplicationType");
    assertThat(context).isInstanceOf(ReactiveWebApplicationContext.class);
    assertThat(context.getEnvironment()).isInstanceOf(StandardReactiveWebEnvironment.class);
    assertThat(context.getEnvironment().getClass().getName()).endsWith("ApplicationReactiveWebEnvironment");
  }

  @Test
  void webApplicationSwitchedOffInListener() {
    Application application = new Application(ExampleWebConfig.class);
    application.addListeners((ApplicationListener<ApplicationEnvironmentPreparedEvent>) (event) -> {
      assertThat(event.getEnvironment().getClass().getName()).endsWith("ApplicationWebEnvironment");
      TestPropertySourceUtils.addInlinedPropertiesToEnvironment(event.getEnvironment(), "foo=bar");
      event.getApplication().setApplicationType(ApplicationType.NORMAL);
    });
    this.context = application.run();
    assertThat(this.context.getEnvironment()).isNotInstanceOf(StandardWebEnvironment.class);
    assertThat(this.context.getEnvironment().getProperty("foo")).isEqualTo("bar");
    Iterator<PropertySource<?>> iterator = this.context.getEnvironment().getPropertySources().iterator();
    assertThat(iterator.next().getName()).isEqualTo("configurationProperties");
    assertThat(iterator.next().getName())
            .isEqualTo(TestPropertySourceUtils.INLINED_PROPERTIES_PROPERTY_SOURCE_NAME);
  }

  @Test
  void nonWebApplicationConfiguredViaAPropertyHasTheCorrectTypeOfContextAndEnvironment() {
    ConfigurableApplicationContext context = new Application(ExampleConfig.class)
            .run("--app.main.application-type=normal");
    assertThat(context).isNotInstanceOfAny(WebServerApplicationContext.class, ReactiveWebApplicationContext.class);
  }

  @Configuration(proxyBeanMethods = false)
  static class ExampleConfig {

    @Bean
    String someBean() {
      return "test";
    }
  }

  @Configuration(proxyBeanMethods = false)
  static class ExampleWebConfig {

    @Bean
    MockWebServerFactory webServer() {
      return new MockWebServerFactory();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class ExampleReactiveWebConfig {

    @Bean
    MockReactiveWebServerFactory webServerFactory() {
      return new MockReactiveWebServerFactory();
    }

    @Bean
    HttpHandler httpHandler() {
      return (serverHttpRequest, serverHttpResponse) -> Mono.empty();
    }

  }

}
