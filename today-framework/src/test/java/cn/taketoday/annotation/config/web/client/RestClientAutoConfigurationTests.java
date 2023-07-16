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

package cn.taketoday.annotation.config.web.client;

import org.junit.jupiter.api.Test;

import java.util.List;

import cn.taketoday.annotation.config.http.HttpMessageConvertersAutoConfiguration;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.config.AutoConfigurations;
import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;
import cn.taketoday.http.config.CodecCustomizer;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.converter.HttpMessageConverters;
import cn.taketoday.http.converter.StringHttpMessageConverter;
import cn.taketoday.test.util.ReflectionTestUtils;
import cn.taketoday.web.client.RestClient;
import cn.taketoday.web.client.config.RestClientCustomizer;

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