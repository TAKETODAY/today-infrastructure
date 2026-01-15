/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.app.config.web.reactive.client;

import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import infra.app.test.context.FilteredClassLoader;
import infra.app.test.context.runner.ApplicationContextRunner;
import infra.beans.factory.config.BeanDefinition;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.AutoConfigurations;
import infra.http.client.ReactorResourceFactory;
import infra.http.client.reactive.ClientHttpConnector;
import infra.http.client.reactive.ReactorClientHttpConnector;
import infra.web.client.reactive.WebClient;
import reactor.netty.http.client.HttpClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link infra.app.config.web.reactive.client.ClientHttpConnectorAutoConfiguration}
 *
 * @author Brian Clozel
 */
class ClientHttpConnectorAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(infra.app.config.web.reactive.client.ClientHttpConnectorAutoConfiguration.class));

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
  void whenReactorUnavailableThenHttpClientBeansAreDefined() {
    this.contextRunner.withClassLoader(new FilteredClassLoader(HttpClient.class))
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
  void whenReactorHttpClientBeansAreUnavailableThenJdkClientBeansAreDefined() {
    this.contextRunner
            .withClassLoader(new FilteredClassLoader(HttpClient.class, HttpAsyncClients.class))
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
  @Disabled
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
