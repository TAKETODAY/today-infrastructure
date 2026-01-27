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

package infra.http.reactive.client.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import infra.app.config.ssl.SslAutoConfiguration;
import infra.app.test.context.runner.ApplicationContextRunner;
import infra.beans.factory.config.BeanDefinition;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.AutoConfigurations;
import infra.core.ssl.SslBundle;
import infra.core.task.VirtualThreadTaskExecutor;
import infra.http.client.HttpClientSettings;
import infra.http.client.HttpRedirects;
import infra.http.client.config.HttpClientAutoConfiguration;
import infra.http.reactive.client.ClientHttpConnector;
import infra.http.reactive.client.ClientHttpConnectorBuilder;
import infra.http.reactive.client.JdkClientHttpConnectorBuilder;
import infra.http.reactive.client.ReactorClientHttpConnectorBuilder;
import infra.http.support.ReactorResourceFactory;
import infra.test.util.ReflectionTestUtils;
import reactor.netty.resources.LoopResources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ReactiveHttpClientAutoConfiguration}
 *
 * @author Brian Clozel
 * @author Phillip Webb
 */
class ReactiveHttpClientAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(ReactiveHttpClientAutoConfiguration.class,
                  HttpClientAutoConfiguration.class, SslAutoConfiguration.class));

  @Test
  void whenReactorIsAvailableThenReactorBeansAreDefined() {
    this.contextRunner.run((context) -> {
      BeanDefinition connectorDefinition = context.getBeanFactory().getBeanDefinition("clientHttpConnector");
      assertThat(connectorDefinition.isLazyInit()).isTrue();
      assertThat(context).hasSingleBean(ReactorResourceFactory.class);
      assertThat(context.getBean(ClientHttpConnectorBuilder.class))
              .isExactlyInstanceOf(ReactorClientHttpConnectorBuilder.class);
    });
  }

  @Test
  void shouldNotOverrideCustomClientConnector() {
    this.contextRunner.withUserConfiguration(CustomClientHttpConnectorConfig.class).run((context) -> {
      assertThat(context).hasSingleBean(ClientHttpConnector.class);
      assertThat(context).hasBean("customConnector");
    });
  }

  @Test
  void shouldUseCustomReactorResourceFactory() {
    this.contextRunner.withUserConfiguration(CustomReactorResourceConfig.class).run((context) -> {
      assertThat(context).hasSingleBean(ClientHttpConnector.class);
      assertThat(context).hasSingleBean(ReactorResourceFactory.class);
      assertThat(context).hasBean("customReactorResourceFactory");
      ClientHttpConnector connector = context.getBean(ClientHttpConnector.class);
      assertThat(connector).extracting("httpClient.config.loopResources")
              .isEqualTo(context.getBean("customReactorResourceFactory", ReactorResourceFactory.class)
                      .getLoopResources());
    });
  }

  @Test
  void shouldUseReactorResourceFactory() {
    this.contextRunner.run((context) -> {
      assertThat(context).hasSingleBean(ClientHttpConnector.class);
      assertThat(context).hasSingleBean(ReactorResourceFactory.class);
      ClientHttpConnector connector = context.getBean(ClientHttpConnector.class);
      assertThat(connector).extracting("httpClient.config.loopResources")
              .isEqualTo(context.getBean(ReactorResourceFactory.class).getLoopResources());
    });
  }

  @Test
  void configuresDetectedClientHttpConnectorBuilderBuilder() {
    this.contextRunner.run((context) -> assertThat(context).hasSingleBean(ClientHttpConnectorBuilder.class));
  }

  @Test
  void configuresClientHttpConnectorSettings() {
    this.contextRunner.withPropertyValues(sslPropertyValues().toArray(String[]::new))
            .withPropertyValues("http.clients.redirects=dont-follow", "http.clients.connect-timeout=10s",
                    "http.clients.read-timeout=20s", "http.clients.ssl.bundle=test")
            .run((context) -> {
              HttpClientSettings settings = context.getBean(HttpClientSettings.class);
              assertThat(settings.redirects()).isEqualTo(HttpRedirects.DONT_FOLLOW);
              assertThat(settings.connectTimeout()).isEqualTo(Duration.ofSeconds(10));
              assertThat(settings.readTimeout()).isEqualTo(Duration.ofSeconds(20));
              SslBundle sslBundle = settings.sslBundle();
              assertThat(sslBundle).isNotNull();
              assertThat(sslBundle.getKey().getAlias()).isEqualTo("alias1");
            });
  }

  @Test
  @EnabledForJreRange(min = JRE.JAVA_21)
  void whenVirtualThreadsEnabledAndUsingJdkHttpClientUsesVirtualThreadExecutor() {
    this.contextRunner
            .withPropertyValues("http.clients.reactive.connector=jdk", "infra.threads.virtual.enabled=true")
            .run((context) -> {
              ClientHttpConnector connector = context.getBean(ClientHttpConnectorBuilder.class).build();
              java.net.http.HttpClient httpClient = (java.net.http.HttpClient) ReflectionTestUtils.getField(connector,
                      "httpClient");
              assertThat(httpClient).isNotNull();
              assertThat(httpClient.executor()).containsInstanceOf(VirtualThreadTaskExecutor.class);
            });
  }

  private List<String> sslPropertyValues() {
    List<String> propertyValues = new ArrayList<>();
    String location = "classpath:infra/ssl/";
    propertyValues.add("ssl.bundle.pem.test.key.alias=alias1");
    propertyValues.add("ssl.bundle.pem.test.truststore.type=PKCS12");
    propertyValues.add("ssl.bundle.pem.test.truststore.certificate=" + location + "rsa-cert.pem");
    propertyValues.add("ssl.bundle.pem.test.truststore.private-key=" + location + "rsa-key.pem");
    return propertyValues;
  }

  @Test
  void clientHttpConnectorBuilderCustomizersAreApplied() {
    this.contextRunner.withPropertyValues("http.clients.reactive.connector=jdk")
            .withUserConfiguration(ClientHttpConnectorBuilderCustomizersConfiguration.class)
            .run((context) -> {
              ClientHttpConnector connector = context.getBean(ClientHttpConnectorBuilder.class).build();
              assertThat(connector).extracting("readTimeout").isEqualTo(Duration.ofSeconds(5));
            });
  }

  @Configuration(proxyBeanMethods = false)
  static class CustomClientHttpConnectorConfig {

    @Bean
    ClientHttpConnector customConnector() {
      return mock(ClientHttpConnector.class);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class CustomReactorResourceConfig {

    @Bean
    ReactorResourceFactory customReactorResourceFactory() {
      ReactorResourceFactory reactorResourceFactory = new ReactorResourceFactory();
      reactorResourceFactory.setUseGlobalResources(false);
      reactorResourceFactory.setLoopResources(LoopResources.create("custom-loop", 1, true));
      return reactorResourceFactory;
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class ClientHttpConnectorBuilderCustomizersConfiguration {

    @Bean
    ClientHttpConnectorBuilderCustomizer<JdkClientHttpConnectorBuilder> jdkCustomizer() {
      return (builder) -> builder.withCustomizer((connector) -> connector.setReadTimeout(Duration.ofSeconds(5)));
    }

  }

}
