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

package infra.web.server.reactive.context;

import org.junit.jupiter.api.Test;

import infra.beans.factory.support.BeanNameGenerator;
import infra.beans.factory.support.StandardBeanFactory;
import infra.context.ApplicationListener;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.ScopeMetadataResolver;
import infra.context.event.ApplicationEventMulticaster;
import infra.context.event.ContextRefreshedEvent;
import infra.context.event.SimpleApplicationEventMulticaster;
import infra.http.server.reactive.HttpHandler;
import infra.web.server.reactive.ReactiveWebServerFactory;
import infra.web.server.reactive.context.WebServerManager.DelayedInitializationHttpHandler;
import infra.web.server.reactive.context.config.ExampleReactiveWebServerApplicationConfiguration;
import infra.web.server.reactive.server.MockReactiveWebServerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    this.context = new AnnotationConfigReactiveWebServerApplicationContext(InitializationTestConfig.class);
    verifyContext();
  }

  @Test
  void shouldCreateEmptyContext() {
    // when
    AnnotationConfigReactiveWebServerApplicationContext context = new AnnotationConfigReactiveWebServerApplicationContext();

    // then
    assertThat(context).isNotNull();
    assertThat(context.getBeanFactory()).isNotNull();
  }

  @Test
  void shouldCreateContextWithBeanFactory() {
    // given
    StandardBeanFactory beanFactory = new StandardBeanFactory();

    // when
    AnnotationConfigReactiveWebServerApplicationContext context = new AnnotationConfigReactiveWebServerApplicationContext(beanFactory);

    // then
    assertThat(context).isNotNull();
    assertThat(context.getBeanFactory()).isEqualTo(beanFactory);
  }

  @Test
  void shouldSetBeanNameGenerator() {
    // given
    AnnotationConfigReactiveWebServerApplicationContext context = new AnnotationConfigReactiveWebServerApplicationContext();
    BeanNameGenerator beanNameGenerator = mock(BeanNameGenerator.class);

    // when
    context.setBeanNameGenerator(beanNameGenerator);

    assertThatIllegalStateException()
            .isThrownBy(() -> context.setBeanNameGenerator(beanNameGenerator));
  }

  @Test
  void shouldSetScopeMetadataResolver() {
    // given
    AnnotationConfigReactiveWebServerApplicationContext context = new AnnotationConfigReactiveWebServerApplicationContext();
    ScopeMetadataResolver scopeMetadataResolver = mock(ScopeMetadataResolver.class);

    // when
    context.setScopeMetadataResolver(scopeMetadataResolver);

    // then
    // Should not throw exception
    assertThatNoException().isThrownBy(() -> context.setScopeMetadataResolver(scopeMetadataResolver));
  }

  @Test
  void shouldRegisterAnnotatedClasses() {
    // given
    AnnotationConfigReactiveWebServerApplicationContext context = new AnnotationConfigReactiveWebServerApplicationContext();

    // when
    context.register(Configuration.class);

    // then
    assertThatNoException().isThrownBy(() -> context.register(Configuration.class));
  }

  @Test
  void shouldScanBasePackages() {
    // given
    AnnotationConfigReactiveWebServerApplicationContext context = new AnnotationConfigReactiveWebServerApplicationContext();

    // when
    context.scan("infra.context");

    // then
    assertThatNoException().isThrownBy(() -> context.scan("infra.context"));
  }

  @Test
  void shouldThrowExceptionWhenRegisterWithEmptyClasses() {
    // given
    AnnotationConfigReactiveWebServerApplicationContext context = new AnnotationConfigReactiveWebServerApplicationContext();

    // when & then
    assertThatThrownBy(() -> context.register(new Class[0]))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("At least one annotated class must be specified");
  }

  @Test
  void shouldThrowExceptionWhenScanWithEmptyPackages() {
    // given
    AnnotationConfigReactiveWebServerApplicationContext context = new AnnotationConfigReactiveWebServerApplicationContext();

    // when & then
    assertThatThrownBy(context::scan)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("At least one base package must be specified");
  }

  private void verifyContext() {
    MockReactiveWebServerFactory factory = this.context.getBean(MockReactiveWebServerFactory.class);
    HttpHandler expectedHandler = this.context.getBean(HttpHandler.class);
    HttpHandler actualHandler = factory.getWebServer().getHttpHandler();
    if (actualHandler instanceof DelayedInitializationHttpHandler) {
      actualHandler = ((DelayedInitializationHttpHandler) actualHandler).delegate;
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
