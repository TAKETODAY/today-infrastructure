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

package cn.taketoday.annotation.config.web.reactive;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.eclipse.jetty.server.Server;
import org.junit.jupiter.api.Test;

import cn.taketoday.context.ApplicationContextException;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.config.AutoConfigurations;
import cn.taketoday.framework.test.context.FilteredClassLoader;
import cn.taketoday.framework.test.context.runner.ReactiveWebApplicationContextRunner;
import cn.taketoday.framework.web.embedded.jetty.JettyReactiveWebServerFactory;
import cn.taketoday.framework.web.embedded.jetty.JettyServerCustomizer;
import cn.taketoday.framework.web.embedded.netty.ReactorNettyReactiveWebServerFactory;
import cn.taketoday.framework.web.embedded.netty.ReactorNettyServerCustomizer;
import cn.taketoday.framework.web.embedded.tomcat.TomcatConnectorCustomizer;
import cn.taketoday.framework.web.embedded.tomcat.TomcatContextCustomizer;
import cn.taketoday.framework.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import cn.taketoday.framework.web.embedded.tomcat.TomcatReactiveWebServerFactory;
import cn.taketoday.framework.web.embedded.undertow.UndertowBuilderCustomizer;
import cn.taketoday.framework.web.embedded.undertow.UndertowDeploymentInfoCustomizer;
import cn.taketoday.framework.web.embedded.undertow.UndertowReactiveWebServerFactory;
import cn.taketoday.framework.web.reactive.context.AnnotationConfigReactiveWebApplicationContext;
import cn.taketoday.framework.web.reactive.context.AnnotationConfigReactiveWebServerApplicationContext;
import cn.taketoday.framework.web.reactive.server.ConfigurableReactiveWebServerFactory;
import cn.taketoday.framework.web.reactive.server.ReactiveWebServerFactory;
import cn.taketoday.framework.web.server.WebServerFactoryCustomizer;
import cn.taketoday.http.server.reactive.ForwardedHeaderTransformer;
import cn.taketoday.http.server.reactive.HttpHandler;
import io.undertow.Undertow;
import io.undertow.Undertow.Builder;
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
          .withConfiguration(AutoConfigurations.of(ReactiveWebServerFactoryAutoConfiguration.class));

  @Test
  void createFromConfigClass() {
    this.contextRunner.withUserConfiguration(MockWebServerConfiguration.class, HttpHandlerConfiguration.class)
            .run((context) -> {
              assertThat(context.getBeansOfType(ReactiveWebServerFactory.class)).hasSize(1);
              assertThat(context.getBeansOfType(WebServerFactoryCustomizer.class)).hasSize(2);
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
  void defaultWebServerIsTomcat() {
    // Tomcat should be chosen over Netty if the Tomcat library is present.
    this.contextRunner.withUserConfiguration(HttpHandlerConfiguration.class).withPropertyValues("server.port=0")
            .run((context) -> assertThat(context.getBean(ReactiveWebServerFactory.class))
                    .isInstanceOf(TomcatReactiveWebServerFactory.class));
  }

  @Test
  void tomcatConnectorCustomizerBeanIsAddedToFactory() {
    ReactiveWebApplicationContextRunner runner = new ReactiveWebApplicationContextRunner(
            AnnotationConfigReactiveWebServerApplicationContext::new)
            .withConfiguration(AutoConfigurations.of(ReactiveWebServerFactoryAutoConfiguration.class))
            .withUserConfiguration(HttpHandlerConfiguration.class,
                    TomcatConnectorCustomizerConfiguration.class)
            .withPropertyValues("server.port: 0");
    runner.run((context) -> {
      TomcatReactiveWebServerFactory factory = context.getBean(TomcatReactiveWebServerFactory.class);
      TomcatConnectorCustomizer customizer = context.getBean("connectorCustomizer",
              TomcatConnectorCustomizer.class);
      assertThat(factory.getTomcatConnectorCustomizers()).contains(customizer);
      then(customizer).should().customize(any(Connector.class));
    });
  }

  @Test
  void tomcatConnectorCustomizerRegisteredAsBeanAndViaFactoryIsOnlyCalledOnce() {
    ReactiveWebApplicationContextRunner runner = new ReactiveWebApplicationContextRunner(
            AnnotationConfigReactiveWebServerApplicationContext::new)
            .withConfiguration(AutoConfigurations.of(ReactiveWebServerFactoryAutoConfiguration.class))
            .withUserConfiguration(HttpHandlerConfiguration.class,
                    DoubleRegistrationTomcatConnectorCustomizerConfiguration.class)
            .withPropertyValues("server.port: 0");
    runner.run((context) -> {
      TomcatReactiveWebServerFactory factory = context.getBean(TomcatReactiveWebServerFactory.class);
      TomcatConnectorCustomizer customizer = context.getBean("connectorCustomizer",
              TomcatConnectorCustomizer.class);
      assertThat(factory.getTomcatConnectorCustomizers()).contains(customizer);
      then(customizer).should().customize(any(Connector.class));
    });
  }

  @Test
  void tomcatContextCustomizerBeanIsAddedToFactory() {
    ReactiveWebApplicationContextRunner runner = new ReactiveWebApplicationContextRunner(
            AnnotationConfigReactiveWebServerApplicationContext::new)
            .withConfiguration(AutoConfigurations.of(ReactiveWebServerFactoryAutoConfiguration.class))
            .withUserConfiguration(HttpHandlerConfiguration.class,
                    TomcatContextCustomizerConfiguration.class)
            .withPropertyValues("server.port: 0");
    runner.run((context) -> {
      TomcatReactiveWebServerFactory factory = context.getBean(TomcatReactiveWebServerFactory.class);
      TomcatContextCustomizer customizer = context.getBean("contextCustomizer", TomcatContextCustomizer.class);
      assertThat(factory.getTomcatContextCustomizers()).contains(customizer);
      then(customizer).should().customize(any(Context.class));
    });
  }

  @Test
  void tomcatContextCustomizerRegisteredAsBeanAndViaFactoryIsOnlyCalledOnce() {
    ReactiveWebApplicationContextRunner runner = new ReactiveWebApplicationContextRunner(
            AnnotationConfigReactiveWebServerApplicationContext::new)
            .withConfiguration(AutoConfigurations.of(ReactiveWebServerFactoryAutoConfiguration.class))
            .withUserConfiguration(HttpHandlerConfiguration.class,
                    DoubleRegistrationTomcatContextCustomizerConfiguration.class)
            .withPropertyValues("server.port: 0");
    runner.run((context) -> {
      TomcatReactiveWebServerFactory factory = context.getBean(TomcatReactiveWebServerFactory.class);
      TomcatContextCustomizer customizer = context.getBean("contextCustomizer", TomcatContextCustomizer.class);
      assertThat(factory.getTomcatContextCustomizers()).contains(customizer);
      then(customizer).should().customize(any(Context.class));
    });
  }

  @Test
  void tomcatProtocolHandlerCustomizerBeanIsAddedToFactory() {
    ReactiveWebApplicationContextRunner runner = new ReactiveWebApplicationContextRunner(
            AnnotationConfigReactiveWebServerApplicationContext::new)
            .withConfiguration(AutoConfigurations.of(ReactiveWebServerFactoryAutoConfiguration.class))
            .withUserConfiguration(HttpHandlerConfiguration.class,
                    TomcatProtocolHandlerCustomizerConfiguration.class)
            .withPropertyValues("server.port: 0");
    runner.run((context) -> {
      TomcatReactiveWebServerFactory factory = context.getBean(TomcatReactiveWebServerFactory.class);
      TomcatProtocolHandlerCustomizer<?> customizer = context.getBean("protocolHandlerCustomizer",
              TomcatProtocolHandlerCustomizer.class);
      assertThat(factory.getTomcatProtocolHandlerCustomizers()).contains(customizer);
      then(customizer).should().customize(any());
    });
  }

  @Test
  void tomcatProtocolHandlerCustomizerRegisteredAsBeanAndViaFactoryIsOnlyCalledOnce() {
    ReactiveWebApplicationContextRunner runner = new ReactiveWebApplicationContextRunner(
            AnnotationConfigReactiveWebServerApplicationContext::new)
            .withConfiguration(AutoConfigurations.of(ReactiveWebServerFactoryAutoConfiguration.class))
            .withUserConfiguration(HttpHandlerConfiguration.class,
                    DoubleRegistrationTomcatProtocolHandlerCustomizerConfiguration.class)
            .withPropertyValues("server.port: 0");
    runner.run((context) -> {
      TomcatReactiveWebServerFactory factory = context.getBean(TomcatReactiveWebServerFactory.class);
      TomcatProtocolHandlerCustomizer<?> customizer = context.getBean("protocolHandlerCustomizer",
              TomcatProtocolHandlerCustomizer.class);
      assertThat(factory.getTomcatProtocolHandlerCustomizers()).contains(customizer);
      then(customizer).should().customize(any());
    });
  }

  @Test
  void jettyServerCustomizerBeanIsAddedToFactory() {
    new ReactiveWebApplicationContextRunner(AnnotationConfigReactiveWebApplicationContext::new)
            .withConfiguration(AutoConfigurations.of(ReactiveWebServerFactoryAutoConfiguration.class))
            .withClassLoader(new FilteredClassLoader(Tomcat.class, HttpServer.class))
            .withUserConfiguration(JettyServerCustomizerConfiguration.class, HttpHandlerConfiguration.class)
            .run((context) -> {
              JettyReactiveWebServerFactory factory = context.getBean(JettyReactiveWebServerFactory.class);
              assertThat(factory.getServerCustomizers()).hasSize(1);
            });
  }

  @Test
  void jettyServerCustomizerRegisteredAsBeanAndViaFactoryIsOnlyCalledOnce() {
    new ReactiveWebApplicationContextRunner(AnnotationConfigReactiveWebServerApplicationContext::new)
            .withConfiguration(AutoConfigurations.of(ReactiveWebServerFactoryAutoConfiguration.class))
            .withClassLoader(new FilteredClassLoader(Tomcat.class, HttpServer.class))
            .withUserConfiguration(DoubleRegistrationJettyServerCustomizerConfiguration.class,
                    HttpHandlerConfiguration.class)
            .withPropertyValues("server.port=0").run((context) -> {
              JettyReactiveWebServerFactory factory = context.getBean(JettyReactiveWebServerFactory.class);
              JettyServerCustomizer customizer = context.getBean("serverCustomizer", JettyServerCustomizer.class);
              assertThat(factory.getServerCustomizers()).contains(customizer);
              then(customizer).should().customize(any(Server.class));
            });
  }

  @Test
  void undertowBuilderCustomizerBeanIsAddedToFactory() {
    new ReactiveWebApplicationContextRunner(AnnotationConfigReactiveWebApplicationContext::new)
            .withConfiguration(AutoConfigurations.of(ReactiveWebServerFactoryAutoConfiguration.class))
            .withClassLoader(new FilteredClassLoader(Tomcat.class, HttpServer.class, Server.class))
            .withUserConfiguration(UndertowBuilderCustomizerConfiguration.class, HttpHandlerConfiguration.class)
            .run((context) -> {
              UndertowReactiveWebServerFactory factory = context.getBean(UndertowReactiveWebServerFactory.class);
              assertThat(factory.getBuilderCustomizers()).hasSize(1);
            });
  }

  @Test
  void undertowBuilderCustomizerRegisteredAsBeanAndViaFactoryIsOnlyCalledOnce() {
    new ReactiveWebApplicationContextRunner(AnnotationConfigReactiveWebServerApplicationContext::new)
            .withConfiguration(AutoConfigurations.of(ReactiveWebServerFactoryAutoConfiguration.class))
            .withClassLoader(new FilteredClassLoader(Tomcat.class, HttpServer.class, Server.class))
            .withUserConfiguration(DoubleRegistrationUndertowBuilderCustomizerConfiguration.class,
                    HttpHandlerConfiguration.class)
            .withPropertyValues("server.port: 0").run((context) -> {
              UndertowReactiveWebServerFactory factory = context.getBean(UndertowReactiveWebServerFactory.class);
              UndertowBuilderCustomizer customizer = context.getBean("builderCustomizer",
                      UndertowBuilderCustomizer.class);
              assertThat(factory.getBuilderCustomizers()).contains(customizer);
              then(customizer).should().customize(any(Builder.class));
            });
  }

  @Test
  void ReactorNettyServerCustomizerBeanIsAddedToFactory() {
    new ReactiveWebApplicationContextRunner(AnnotationConfigReactiveWebApplicationContext::new)
            .withConfiguration(AutoConfigurations.of(ReactiveWebServerFactoryAutoConfiguration.class))
            .withClassLoader(new FilteredClassLoader(Tomcat.class, Server.class, Undertow.class))
            .withUserConfiguration(ReactorNettyServerCustomizerConfiguration.class, HttpHandlerConfiguration.class)
            .run((context) -> {
              ReactorNettyReactiveWebServerFactory factory = context.getBean(ReactorNettyReactiveWebServerFactory.class);
              assertThat(factory.getServerCustomizers()).hasSize(1);
            });
  }

  @Test
  void ReactorNettyServerCustomizerRegisteredAsBeanAndViaFactoryIsOnlyCalledOnce() {
    new ReactiveWebApplicationContextRunner(AnnotationConfigReactiveWebServerApplicationContext::new)
            .withConfiguration(AutoConfigurations.of(ReactiveWebServerFactoryAutoConfiguration.class))
            .withClassLoader(new FilteredClassLoader(Tomcat.class, Server.class, Undertow.class))
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
  static class TomcatConnectorCustomizerConfiguration {

    @Bean
    TomcatConnectorCustomizer connectorCustomizer() {
      return mock(TomcatConnectorCustomizer.class);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class DoubleRegistrationTomcatConnectorCustomizerConfiguration {

    private final TomcatConnectorCustomizer customizer = mock(TomcatConnectorCustomizer.class);

    @Bean
    TomcatConnectorCustomizer connectorCustomizer() {
      return this.customizer;
    }

    @Bean
    WebServerFactoryCustomizer<TomcatReactiveWebServerFactory> tomcatCustomizer() {
      return (tomcat) -> tomcat.addConnectorCustomizers(this.customizer);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class TomcatContextCustomizerConfiguration {

    @Bean
    TomcatContextCustomizer contextCustomizer() {
      return mock(TomcatContextCustomizer.class);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class DoubleRegistrationTomcatContextCustomizerConfiguration {

    private final TomcatContextCustomizer customizer = mock(TomcatContextCustomizer.class);

    @Bean
    TomcatContextCustomizer contextCustomizer() {
      return this.customizer;
    }

    @Bean
    WebServerFactoryCustomizer<TomcatReactiveWebServerFactory> tomcatCustomizer() {
      return (tomcat) -> tomcat.addContextCustomizers(this.customizer);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class TomcatProtocolHandlerCustomizerConfiguration {

    @Bean
    TomcatProtocolHandlerCustomizer<?> protocolHandlerCustomizer() {
      return mock(TomcatProtocolHandlerCustomizer.class);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class DoubleRegistrationTomcatProtocolHandlerCustomizerConfiguration {

    private final TomcatProtocolHandlerCustomizer<?> customizer = mock(TomcatProtocolHandlerCustomizer.class);

    @Bean
    TomcatProtocolHandlerCustomizer<?> protocolHandlerCustomizer() {
      return this.customizer;
    }

    @Bean
    WebServerFactoryCustomizer<TomcatReactiveWebServerFactory> tomcatCustomizer() {
      return (tomcat) -> tomcat.addProtocolHandlerCustomizers(this.customizer);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class JettyServerCustomizerConfiguration {

    @Bean
    JettyServerCustomizer serverCustomizer() {
      return (server) -> {
      };
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class DoubleRegistrationJettyServerCustomizerConfiguration {

    private final JettyServerCustomizer customizer = mock(JettyServerCustomizer.class);

    @Bean
    JettyServerCustomizer serverCustomizer() {
      return this.customizer;
    }

    @Bean
    WebServerFactoryCustomizer<JettyReactiveWebServerFactory> jettyCustomizer() {
      return (jetty) -> jetty.addServerCustomizers(this.customizer);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class UndertowBuilderCustomizerConfiguration {

    @Bean
    UndertowBuilderCustomizer builderCustomizer() {
      return (builder) -> {
      };
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class DoubleRegistrationUndertowBuilderCustomizerConfiguration {

    private final UndertowBuilderCustomizer customizer = mock(UndertowBuilderCustomizer.class);

    @Bean
    UndertowBuilderCustomizer builderCustomizer() {
      return this.customizer;
    }

    @Bean
    WebServerFactoryCustomizer<UndertowReactiveWebServerFactory> undertowCustomizer() {
      return (undertow) -> undertow.addBuilderCustomizers(this.customizer);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class UndertowDeploymentInfoCustomizerConfiguration {

    @Bean
    UndertowDeploymentInfoCustomizer deploymentInfoCustomizer() {
      return (deploymentInfo) -> {
      };
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
