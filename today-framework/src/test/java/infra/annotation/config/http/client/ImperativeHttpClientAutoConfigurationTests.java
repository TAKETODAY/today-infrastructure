/*
 * Copyright 2017 - 2026 the original author or authors.
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

package infra.annotation.config.http.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import infra.annotation.config.ssl.SslAutoConfiguration;
import infra.app.test.context.runner.ApplicationContextRunner;
import infra.app.test.context.runner.ReactiveWebApplicationContextRunner;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.AutoConfigurations;
import infra.core.ssl.SslBundle;
import infra.core.task.VirtualThreadTaskExecutor;
import infra.http.client.ClientHttpRequestFactory;
import infra.http.client.config.ClientHttpRequestFactoryBuilder;
import infra.http.client.config.HttpClientSettings;
import infra.http.client.config.HttpComponentsClientHttpRequestFactoryBuilder;
import infra.http.client.config.HttpRedirects;
import infra.http.client.config.JdkClientHttpRequestFactoryBuilder;
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