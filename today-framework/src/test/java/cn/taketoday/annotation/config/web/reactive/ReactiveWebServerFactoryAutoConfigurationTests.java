/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.annotation.config.web.reactive;

import org.junit.jupiter.api.Test;

import cn.taketoday.annotation.config.ssl.SslAutoConfiguration;
import cn.taketoday.context.ApplicationContextException;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.config.AutoConfigurations;
import cn.taketoday.core.ssl.NoSuchSslBundleException;
import cn.taketoday.framework.test.context.runner.ReactiveWebApplicationContextRunner;
import cn.taketoday.framework.web.reactive.context.AnnotationConfigReactiveWebServerApplicationContext;
import cn.taketoday.framework.web.reactive.server.ConfigurableReactiveWebServerFactory;
import cn.taketoday.framework.web.reactive.server.ReactiveWebServerFactory;
import cn.taketoday.framework.web.reactive.server.netty.ReactorNettyReactiveWebServerFactory;
import cn.taketoday.framework.web.reactive.server.netty.ReactorNettyServerCustomizer;
import cn.taketoday.framework.web.server.WebServerFactoryCustomizer;
import cn.taketoday.http.server.reactive.ForwardedHeaderTransformer;
import cn.taketoday.http.server.reactive.HttpHandler;
import reactor.netty.http.server.HttpServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ReactiveWebServerFactoryAutoConfiguration}.
 *
 * @author Brian Clozel
 * @author Raheela Aslam
 * @author Madhura Bhave
 */
class ReactiveWebServerFactoryAutoConfigurationTests {

  private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner(
          AnnotationConfigReactiveWebServerApplicationContext::new)
          .withConfiguration(
                  AutoConfigurations.of(ReactiveWebServerFactoryAutoConfiguration.class, SslAutoConfiguration.class));

  @Test
  void createFromConfigClass() {
    this.contextRunner.withUserConfiguration(MockWebServerConfiguration.class, HttpHandlerConfiguration.class)
            .run((context) -> {
              assertThat(context.getBeansOfType(ReactiveWebServerFactory.class)).hasSize(1);
              assertThat(context.getBeansOfType(WebServerFactoryCustomizer.class)).hasSize(1);
              assertThat(context.getBeansOfType(ReactiveWebServerFactoryCustomizer.class)).hasSize(1);
            });
  }

  @Test
  void missingHttpHandler() {
    this.contextRunner.withUserConfiguration(MockWebServerConfiguration.class)
            .run((context) -> assertThat(context.getStartupFailure())
                    .isInstanceOf(ApplicationContextException.class).rootCause()
                    .hasMessageContaining("missing HttpHandler bean"));
  }

  @Test
  void multipleHttpHandler() {
    this.contextRunner
            .withUserConfiguration(MockWebServerConfiguration.class, HttpHandlerConfiguration.class,
                    TooManyHttpHandlers.class)
            .run((context) -> assertThat(context.getStartupFailure())
                    .isInstanceOf(ApplicationContextException.class).rootCause()
                    .hasMessageContaining("multiple HttpHandler beans : httpHandler,additionalHttpHandler"));
  }

  @Test
  void customizeReactiveWebServer() {
    this.contextRunner
            .withUserConfiguration(MockWebServerConfiguration.class, HttpHandlerConfiguration.class,
                    ReactiveWebServerCustomization.class)
            .run((context) -> assertThat(context.getBean(MockReactiveWebServerFactory.class).getPort())
                    .isEqualTo(9000));
  }

  @Test
  void webServerFailsWithInvalidSslBundle() {
    this.contextRunner.withUserConfiguration(HttpHandlerConfiguration.class)
            .withPropertyValues("server.port=0", "server.ssl.bundle=test-bundle")
            .run((context) -> {
              assertThat(context).hasFailed();
              assertThat(context.getStartupFailure().getCause()).isInstanceOf(NoSuchSslBundleException.class)
                      .withFailMessage("test");
            });
  }

  @Test
  void ReactorNettyServerCustomizerRegisteredAsBeanAndViaFactoryIsOnlyCalledOnce() {
    new ReactiveWebApplicationContextRunner(AnnotationConfigReactiveWebServerApplicationContext::new)
            .withConfiguration(AutoConfigurations.of(ReactiveWebServerFactoryAutoConfiguration.class))
//            .withClassLoader(new FilteredClassLoader(Tomcat.class))
            .withUserConfiguration(DoubleRegistrationReactorNettyServerCustomizerConfiguration.class,
                    HttpHandlerConfiguration.class)
            .withPropertyValues("server.port: 0").run((context) -> {
              ReactorNettyReactiveWebServerFactory factory = context.getBean(ReactorNettyReactiveWebServerFactory.class);
              ReactorNettyServerCustomizer customizer = context.getBean("serverCustomizer", ReactorNettyServerCustomizer.class);
              assertThat(factory.getServerCustomizers()).contains(customizer);
              then(customizer).should().apply(any(HttpServer.class));
            });
  }

  @Test
  void forwardedHeaderTransformerShouldBeConfigured() {
    this.contextRunner.withUserConfiguration(HttpHandlerConfiguration.class)
            .withPropertyValues("server.forward-headers-strategy=framework", "server.port=0")
            .run((context) -> assertThat(context).hasSingleBean(ForwardedHeaderTransformer.class));
  }

  @Test
  void forwardedHeaderTransformerWhenStrategyNotFilterShouldNotBeConfigured() {
    this.contextRunner.withUserConfiguration(HttpHandlerConfiguration.class)
            .withPropertyValues("server.forward-headers-strategy=native", "server.port=0")
            .run((context) -> assertThat(context).doesNotHaveBean(ForwardedHeaderTransformer.class));
  }

  @Test
  void forwardedHeaderTransformerWhenAlreadyRegisteredShouldBackOff() {
    this.contextRunner
            .withUserConfiguration(ForwardedHeaderTransformerConfiguration.class, HttpHandlerConfiguration.class)
            .withPropertyValues("server.forward-headers-strategy=framework", "server.port=0")
            .run((context) -> assertThat(context).hasSingleBean(ForwardedHeaderTransformer.class));
  }

  @Configuration(proxyBeanMethods = false)
  static class HttpHandlerConfiguration {

    @Bean
    HttpHandler httpHandler() {
      return mock(HttpHandler.class);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class TooManyHttpHandlers {

    @Bean
    HttpHandler additionalHttpHandler() {
      return mock(HttpHandler.class);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class ReactiveWebServerCustomization {

    @Bean
    WebServerFactoryCustomizer<ConfigurableReactiveWebServerFactory> reactiveWebServerCustomizer() {
      return (factory) -> factory.setPort(9000);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class MockWebServerConfiguration {

    @Bean
    MockReactiveWebServerFactory mockReactiveWebServerFactory() {
      return new MockReactiveWebServerFactory();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class ReactorNettyServerCustomizerConfiguration {

    @Bean
    ReactorNettyServerCustomizer serverCustomizer() {
      return (server) -> server;
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class DoubleRegistrationReactorNettyServerCustomizerConfiguration {

    private final ReactorNettyServerCustomizer customizer = mock(ReactorNettyServerCustomizer.class);

    DoubleRegistrationReactorNettyServerCustomizerConfiguration() {
      given(this.customizer.apply(any(HttpServer.class))).willAnswer((invocation) -> invocation.getArgument(0));
    }

    @Bean
    ReactorNettyServerCustomizer serverCustomizer() {
      return this.customizer;
    }

    @Bean
    WebServerFactoryCustomizer<ReactorNettyReactiveWebServerFactory> nettyCustomizer() {
      return (netty) -> netty.addServerCustomizers(this.customizer);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class ForwardedHeaderTransformerConfiguration {

    @Bean
    ForwardedHeaderTransformer testForwardedHeaderTransformer() {
      return new ForwardedHeaderTransformer();
    }

  }

}
