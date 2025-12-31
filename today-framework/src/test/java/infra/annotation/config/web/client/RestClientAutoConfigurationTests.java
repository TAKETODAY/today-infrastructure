/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.annotation.config.web.client;

import org.junit.jupiter.api.Test;

import java.util.List;

import infra.annotation.config.http.HttpMessageConvertersAutoConfiguration;
import infra.app.test.context.runner.ApplicationContextRunner;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.AutoConfigurations;
import infra.core.ssl.SslBundles;
import infra.http.codec.CodecCustomizer;
import infra.http.converter.HttpMessageConverter;
import infra.web.config.HttpMessageConverters;
import infra.http.converter.StringHttpMessageConverter;
import infra.test.util.ReflectionTestUtils;
import infra.web.client.RestClient;
import infra.web.client.config.RestClientCustomizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/16 14:07
 */
class RestClientAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class));

  @Test
  void shouldSupplyBeans() {
    this.contextRunner.run((context) -> {
      assertThat(context).hasSingleBean(HttpMessageConvertersRestClientCustomizer.class);
      assertThat(context).hasSingleBean(RestClient.Builder.class);
    });
  }

  @Test
  void shouldSupplyRestClientSslIfSslBundlesIsThere() {
    this.contextRunner.withBean(SslBundles.class, () -> mock(SslBundles.class))
            .run((context) -> assertThat(context).hasSingleBean(RestClientSsl.class));
  }

  @Test
  void shouldCreateBuilder() {
    this.contextRunner.run((context) -> {
      RestClient.Builder builder = context.getBean(RestClient.Builder.class);
      RestClient restClient = builder.build();
      assertThat(restClient).isNotNull();
    });
  }

  @Test
  void restClientShouldApplyCustomizers() {
    this.contextRunner.withUserConfiguration(RestClientCustomizerConfig.class).run((context) -> {
      RestClient.Builder builder = context.getBean(RestClient.Builder.class);
      RestClientCustomizer customizer = context.getBean("restClientCustomizer", RestClientCustomizer.class);
      builder.build();
      then(customizer).should().customize(any(RestClient.Builder.class));
    });
  }

  @Test
  void shouldGetPrototypeScopedBean() {
    this.contextRunner.withUserConfiguration(RestClientCustomizerConfig.class).run((context) -> {
      RestClient.Builder firstBuilder = context.getBean(RestClient.Builder.class);
      RestClient.Builder secondBuilder = context.getBean(RestClient.Builder.class);
      assertThat(firstBuilder).isNotEqualTo(secondBuilder);
    });
  }

  @Test
  void shouldNotCreateClientBuilderIfAlreadyPresent() {
    this.contextRunner.withUserConfiguration(CustomRestClientBuilderConfig.class).run((context) -> {
      RestClient.Builder builder = context.getBean(RestClient.Builder.class);
      assertThat(builder).isInstanceOf(MyRestClientBuilder.class);
    });
  }

  @Test
  @SuppressWarnings("unchecked")
  void restClientWhenMessageConvertersDefinedShouldHaveMessageConverters() {
    this.contextRunner.withConfiguration(AutoConfigurations.of(HttpMessageConvertersAutoConfiguration.class))
            .withUserConfiguration(RestClientConfig.class)
            .run((context) -> {
              RestClient restClient = context.getBean(RestClient.class);
              List<HttpMessageConverter<?>> expectedConverters = context.getBean(HttpMessageConverters.class)
                      .getConverters();
              List<HttpMessageConverter<?>> actualConverters = (List<HttpMessageConverter<?>>) ReflectionTestUtils
                      .getField(restClient, "messageConverters");
              assertThat(actualConverters).containsExactlyElementsOf(expectedConverters);
            });
  }

  @Test
  @SuppressWarnings("unchecked")
  void restClientWhenNoMessageConvertersDefinedShouldHaveDefaultMessageConverters() {
    this.contextRunner.withUserConfiguration(RestClientConfig.class).run((context) -> {
      RestClient restClient = context.getBean(RestClient.class);
      RestClient defaultRestClient = RestClient.builder().build();
      List<HttpMessageConverter<?>> actualConverters = (List<HttpMessageConverter<?>>) ReflectionTestUtils
              .getField(restClient, "messageConverters");
      List<HttpMessageConverter<?>> expectedConverters = (List<HttpMessageConverter<?>>) ReflectionTestUtils
              .getField(defaultRestClient, "messageConverters");
      assertThat(actualConverters).hasSameSizeAs(expectedConverters);
    });
  }

  @Test
  @SuppressWarnings({ "unchecked", "rawtypes" })
  void restClientWhenHasCustomMessageConvertersShouldHaveMessageConverters() {
    this.contextRunner.withConfiguration(AutoConfigurations.of(HttpMessageConvertersAutoConfiguration.class))
            .withUserConfiguration(CustomHttpMessageConverter.class, RestClientConfig.class)
            .run((context) -> {
              RestClient restClient = context.getBean(RestClient.class);
              List<HttpMessageConverter<?>> actualConverters = (List<HttpMessageConverter<?>>) ReflectionTestUtils
                      .getField(restClient, "messageConverters");
              assertThat(actualConverters).extracting(HttpMessageConverter::getClass)
                      .contains((Class) CustomHttpMessageConverter.class);
            });
  }

  @Configuration(proxyBeanMethods = false)
  static class CodecConfiguration {

    @Bean
    CodecCustomizer myCodecCustomizer() {
      return mock(CodecCustomizer.class);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class RestClientCustomizerConfig {

    @Bean
    RestClientCustomizer restClientCustomizer() {
      return mock(RestClientCustomizer.class);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class CustomRestClientBuilderConfig {

    @Bean
    MyRestClientBuilder myRestClientBuilder() {
      return mock(MyRestClientBuilder.class);
    }

  }

  interface MyRestClientBuilder extends RestClient.Builder {

  }

  @Configuration(proxyBeanMethods = false)
  static class RestClientConfig {

    @Bean
    RestClient restClient(RestClient.Builder restClientBuilder) {
      return restClientBuilder.build();
    }

  }

  static class CustomHttpMessageConverter extends StringHttpMessageConverter {

  }

}