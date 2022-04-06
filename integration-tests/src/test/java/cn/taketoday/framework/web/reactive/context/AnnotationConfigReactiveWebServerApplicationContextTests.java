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

package cn.taketoday.framework.web.reactive.context;

import org.junit.jupiter.api.Test;

import cn.taketoday.context.ApplicationListener;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.event.ApplicationEventMulticaster;
import cn.taketoday.context.event.ContextRefreshedEvent;
import cn.taketoday.context.event.SimpleApplicationEventMulticaster;
import cn.taketoday.framework.web.reactive.context.WebServerManager.DelayedInitializationHttpHandler;
import cn.taketoday.framework.web.reactive.context.config.ExampleReactiveWebServerApplicationConfiguration;
import cn.taketoday.framework.web.reactive.server.MockReactiveWebServerFactory;
import cn.taketoday.framework.web.reactive.server.ReactiveWebServerFactory;
import cn.taketoday.http.server.reactive.HttpHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AnnotationConfigReactiveWebServerApplicationContext}.
 *
 * @author Phillip Webb
 */
class AnnotationConfigReactiveWebServerApplicationContextTests {

  private AnnotationConfigReactiveWebServerApplicationContext context;

  @Test
  void createFromScan() {
    this.context = new AnnotationConfigReactiveWebServerApplicationContext(
            ExampleReactiveWebServerApplicationConfiguration.class.getPackage().getName());
    verifyContext();
  }

  @Test
  void createFromConfigClass() {
    this.context = new AnnotationConfigReactiveWebServerApplicationContext(
            ExampleReactiveWebServerApplicationConfiguration.class);
    verifyContext();
  }

  @Test
  void registerAndRefresh() {
    this.context = new AnnotationConfigReactiveWebServerApplicationContext();
    this.context.register(ExampleReactiveWebServerApplicationConfiguration.class);
    this.context.refresh();
    verifyContext();
  }

  @Test
  void multipleRegistersAndRefresh() {
    this.context = new AnnotationConfigReactiveWebServerApplicationContext();
    this.context.register(WebServerConfiguration.class);
    this.context.register(HttpHandlerConfiguration.class);
    this.context.refresh();
    assertThat(this.context.getBeansOfType(WebServerConfiguration.class)).hasSize(1);
    assertThat(this.context.getBeansOfType(HttpHandlerConfiguration.class)).hasSize(1);
  }

  @Test
  void scanAndRefresh() {
    this.context = new AnnotationConfigReactiveWebServerApplicationContext();
    this.context.scan(ExampleReactiveWebServerApplicationConfiguration.class.getPackage().getName());
    this.context.refresh();
    verifyContext();
  }

  @Test
  void httpHandlerInitialization() {
    // gh-14666
    this.context = new AnnotationConfigReactiveWebServerApplicationContext(InitializationTestConfig.class);
    verifyContext();
  }

  private void verifyContext() {
    MockReactiveWebServerFactory factory = this.context.getBean(MockReactiveWebServerFactory.class);
    HttpHandler expectedHandler = this.context.getBean(HttpHandler.class);
    HttpHandler actualHandler = factory.getWebServer().getHttpHandler();
    if (actualHandler instanceof DelayedInitializationHttpHandler) {
      actualHandler = ((DelayedInitializationHttpHandler) actualHandler).getHandler();
    }
    assertThat(actualHandler).isEqualTo(expectedHandler);
  }

  @Configuration(proxyBeanMethods = false)
  static class WebServerConfiguration {

    @Bean
    ReactiveWebServerFactory webServerFactory() {
      return new MockReactiveWebServerFactory();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class HttpHandlerConfiguration {

    @Bean
    HttpHandler httpHandler() {
      return mock(HttpHandler.class);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class InitializationTestConfig {

    private static boolean addedListener;

    @Bean
    ReactiveWebServerFactory webServerFactory() {
      return new MockReactiveWebServerFactory();
    }

    @Bean
    HttpHandler httpHandler() {
      if (!addedListener) {
        throw new RuntimeException(
                "Handlers should be added after listeners, we're being initialized too early!");
      }
      return mock(HttpHandler.class);
    }

    @Bean
    Listener listener() {
      return new Listener();
    }

    @Bean
    ApplicationEventMulticaster applicationEventMulticaster() {
      return new SimpleApplicationEventMulticaster() {

        @Override
        public void addApplicationListenerBean(String listenerBeanName) {
          super.addApplicationListenerBean(listenerBeanName);
          if ("listener".equals(listenerBeanName)) {
            addedListener = true;
          }
        }

      };
    }

    static class Listener implements ApplicationListener<ContextRefreshedEvent> {

      @Override
      public void onApplicationEvent(ContextRefreshedEvent event) {
      }

    }

  }

}
