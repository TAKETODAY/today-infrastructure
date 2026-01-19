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

package infra.http.client.config;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import infra.app.config.ssl.SslAutoConfiguration;
import infra.app.test.context.runner.ApplicationContextRunner;
import infra.app.test.context.runner.ReactiveWebApplicationContextRunner;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.AutoConfigurations;
import infra.core.ssl.SslBundle;
import infra.core.task.VirtualThreadTaskExecutor;
import infra.http.client.ClientHttpRequestFactory;
import infra.http.client.ClientHttpRequestFactoryBuilder;
import infra.http.client.ClientHttpRequestFactoryBuilderCustomizer;
import infra.http.client.HttpClientSettings;
import infra.http.client.HttpComponentsClientHttpRequestFactoryBuilder;
import infra.http.client.HttpRedirects;
import infra.http.client.JdkClientHttpRequestFactoryBuilder;
import infra.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/1/2 20:57
 */
class ImperativeHttpClientAutoConfigurationTests {

  private static final AutoConfigurations autoConfigurations = AutoConfigurations
          .of(ImperativeHttpClientAutoConfiguration.class, HttpClientAutoConfiguration.class, SslAutoConfiguration.class);

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withConfiguration(autoConfigurations);

  @Test
  void configuresDetectedClientHttpRequestFactoryBuilder() {
    this.contextRunner.run((context) -> assertThat(context).hasSingleBean(ClientHttpRequestFactoryBuilder.class));
  }

  @Test
  void configuresDefinedClientHttpRequestFactoryBuilder() {
    this.contextRunner.withPropertyValues("http.clients.imperative.factory=jdk")
            .run((context) -> assertThat(context.getBean(ClientHttpRequestFactoryBuilder.class))
                    .isInstanceOf(JdkClientHttpRequestFactoryBuilder.class));
  }

  @Test
  void configuresClientHttpRequestFactorySettings() {
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

  private List<String> sslPropertyValues() {
    List<String> propertyValues = new ArrayList<>();
    String location = "classpath:infra/annotation/config/ssl/";
    propertyValues.add("ssl.bundle.pem.test.key.alias=alias1");
    propertyValues.add("ssl.bundle.pem.test.truststore.type=PKCS12");
    propertyValues.add("ssl.bundle.pem.test.truststore.certificate=" + location + "rsa-cert.pem");
    propertyValues.add("ssl.bundle.pem.test.truststore.private-key=" + location + "rsa-key.pem");
    return propertyValues;
  }

  @Test
  @Disabled("Reactive")
  void whenReactiveWebApplicationBeansAreNotConfigured() {
    new ReactiveWebApplicationContextRunner().withConfiguration(autoConfigurations)
            .run((context) -> assertThat(context).doesNotHaveBean(ClientHttpRequestFactoryBuilder.class)
                    .hasSingleBean(HttpClientSettings.class));
  }

  @Test
  void clientHttpRequestFactoryBuilderCustomizersAreApplied() {
    this.contextRunner.withUserConfiguration(ClientHttpRequestFactoryBuilderCustomizersConfiguration.class)
            .run((context) -> {
              ClientHttpRequestFactory factory = context.getBean(ClientHttpRequestFactoryBuilder.class).build();
              assertThat(factory).extracting("readTimeout").isEqualTo(5L);
            });
  }

  @Test
  @EnabledForJreRange(min = JRE.JAVA_21)
  void whenVirtualThreadsEnabledAndUsingJdkHttpClientUsesVirtualThreadExecutor() {
    this.contextRunner
            .withPropertyValues("http.clients.imperative.factory=jdk", "infra.threads.virtual.enabled=true")
            .run((context) -> {
              ClientHttpRequestFactory factory = context.getBean(ClientHttpRequestFactoryBuilder.class).build();
              HttpClient httpClient = (HttpClient) ReflectionTestUtils.getField(factory, "httpClient");
              assertThat(httpClient).isNotNull();
              assertThat(httpClient.executor()).containsInstanceOf(VirtualThreadTaskExecutor.class);
            });
  }

  @Configuration(proxyBeanMethods = false)
  static class ClientHttpRequestFactoryBuilderCustomizersConfiguration {

    @Bean
    ClientHttpRequestFactoryBuilderCustomizer<HttpComponentsClientHttpRequestFactoryBuilder> httpComponentsCustomizer() {
      return (builder) -> builder.withCustomizer((factory) -> factory.setReadTimeout(5));
    }

  }

}