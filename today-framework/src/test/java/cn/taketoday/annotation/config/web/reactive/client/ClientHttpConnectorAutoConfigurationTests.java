/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.annotation.config.web.reactive.client;

import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.eclipse.jetty.reactive.client.ReactiveRequest;
import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.config.AutoConfigurations;
import cn.taketoday.framework.test.context.FilteredClassLoader;
import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;
import cn.taketoday.http.client.ReactorResourceFactory;
import cn.taketoday.http.client.reactive.ClientHttpConnector;
import cn.taketoday.http.client.reactive.ReactorClientHttpConnector;
import cn.taketoday.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ClientHttpConnectorAutoConfiguration}
 *
 * @author Brian Clozel
 */
class ClientHttpConnectorAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(ClientHttpConnectorAutoConfiguration.class));

  @Test
  void whenReactorIsAvailableThenReactorBeansAreDefined() {
    this.contextRunner.run((context) -> {
      BeanDefinition customizerDefinition = context.getBeanFactory()
              .getBeanDefinition("webClientHttpConnectorCustomizer");
      assertThat(customizerDefinition.isLazyInit()).isTrue();
      BeanDefinition connectorDefinition = context.getBeanFactory().getBeanDefinition("webClientHttpConnector");
      assertThat(connectorDefinition.isLazyInit()).isTrue();
      assertThat(context).hasBean("reactorClientHttpConnectorFactory");
      assertThat(context).hasSingleBean(ReactorResourceFactory.class);
    });
  }

  @Test
  void whenReactorIsUnavailableThenJettyBeansAreDefined() {
    this.contextRunner.withClassLoader(new FilteredClassLoader(HttpClient.class)).run((context) -> {
      BeanDefinition customizerDefinition = context.getBeanFactory()
              .getBeanDefinition("webClientHttpConnectorCustomizer");
      assertThat(customizerDefinition.isLazyInit()).isTrue();
      BeanDefinition connectorDefinition = context.getBeanFactory().getBeanDefinition("webClientHttpConnector");
      assertThat(connectorDefinition.isLazyInit()).isTrue();
      assertThat(context).hasBean("jettyClientResourceFactory");
      assertThat(context).hasBean("jettyClientHttpConnectorFactory");
    });
  }

  @Test
  void whenReactorAndJettyAreUnavailableThenHttpClientBeansAreDefined() {
    this.contextRunner.withClassLoader(new FilteredClassLoader(HttpClient.class, ReactiveRequest.class))
            .run((context) -> {
              BeanDefinition customizerDefinition = context.getBeanFactory()
                      .getBeanDefinition("webClientHttpConnectorCustomizer");
              assertThat(customizerDefinition.isLazyInit()).isTrue();
              BeanDefinition connectorDefinition = context.getBeanFactory()
                      .getBeanDefinition("webClientHttpConnector");
              assertThat(connectorDefinition.isLazyInit()).isTrue();
              assertThat(context).hasBean("httpComponentsClientHttpConnectorFactory");
            });
  }

  @Test
  void whenReactorJettyAndHttpClientBeansAreUnavailableThenJdkClientBeansAreDefined() {
    this.contextRunner
            .withClassLoader(new FilteredClassLoader(HttpClient.class, ReactiveRequest.class, HttpAsyncClients.class))
            .run((context) -> {
              BeanDefinition customizerDefinition = context.getBeanFactory()
                      .getBeanDefinition("webClientHttpConnectorCustomizer");
              assertThat(customizerDefinition.isLazyInit()).isTrue();
              BeanDefinition connectorDefinition = context.getBeanFactory()
                      .getBeanDefinition("webClientHttpConnector");
              assertThat(connectorDefinition.isLazyInit()).isTrue();
              assertThat(context).hasBean("jdkClientHttpConnectorFactory");
            });
  }

  @Test
  void shouldCreateHttpClientBeans() {
    this.contextRunner.run((context) -> {
      assertThat(context).hasSingleBean(ReactorResourceFactory.class);
      assertThat(context).hasSingleBean(ClientHttpConnector.class);
      WebClientCustomizer clientCustomizer = context.getBean(WebClientCustomizer.class);
      WebClient.Builder builder = mock(WebClient.Builder.class);
      clientCustomizer.customize(builder);
      then(builder).should().clientConnector(any(ReactorClientHttpConnector.class));
    });
  }

  @Test
  void shouldNotOverrideCustomClientConnector() {
    this.contextRunner.withUserConfiguration(CustomClientHttpConnectorConfig.class).run((context) -> {
      assertThat(context).hasSingleBean(ClientHttpConnector.class).hasBean("customConnector");
      WebClientCustomizer clientCustomizer = context.getBean(WebClientCustomizer.class);
      WebClient.Builder builder = mock(WebClient.Builder.class);
      clientCustomizer.customize(builder);
      then(builder).should().clientConnector(any(ClientHttpConnector.class));
    });
  }

  @Test
  void shouldNotOverrideCustomClientConnectorFactory() {
    this.contextRunner.withUserConfiguration(CustomClientHttpConnectorFactoryConfig.class).run((context) -> {
      assertThat(context).hasSingleBean(ClientHttpConnectorFactory.class)
              .hasBean("customConnector")
              .doesNotHaveBean(ReactorResourceFactory.class);
      WebClientCustomizer clientCustomizer = context.getBean(WebClientCustomizer.class);
      WebClient.Builder builder = mock(WebClient.Builder.class);
      clientCustomizer.customize(builder);
      then(builder).should().clientConnector(any(ClientHttpConnector.class));
    });
  }

  @Test
  void shouldUseCustomReactorResourceFactory() {
    this.contextRunner.withUserConfiguration(CustomReactorResourceConfig.class)
            .run((context) -> assertThat(context).hasSingleBean(ClientHttpConnector.class)
                    .hasSingleBean(ReactorResourceFactory.class)
                    .hasBean("customReactorResourceFactory"));
  }

  @Configuration(proxyBeanMethods = false)
  static class CustomClientHttpConnectorConfig {

    @Bean
    ClientHttpConnector customConnector() {
      return mock(ClientHttpConnector.class);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class CustomClientHttpConnectorFactoryConfig {

    @Bean
    ClientHttpConnectorFactory<?> customConnector() {
      return (sslBundle) -> mock(ClientHttpConnector.class);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class CustomReactorResourceConfig {

    @Bean
    ReactorResourceFactory customReactorResourceFactory() {
      return new ReactorResourceFactory();
    }

  }

}
