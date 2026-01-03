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

package infra.annotation.config.web.client;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import infra.annotation.config.http.HttpMessageConvertersAutoConfiguration;
import infra.app.test.context.runner.ApplicationContextRunner;
import infra.app.test.context.runner.ReactiveWebApplicationContextRunner;
import infra.app.test.context.runner.WebApplicationContextRunner;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.AutoConfigurations;
import infra.http.HttpStatus;
import infra.http.client.ClientHttpRequestFactory;
import infra.http.client.HttpComponentsClientHttpRequestFactory;
import infra.http.converter.HttpMessageConverter;
import infra.web.config.HttpMessageConverters;
import infra.http.converter.StringHttpMessageConverter;
import infra.mock.http.client.MockClientHttpRequest;
import infra.mock.http.client.MockClientHttpResponse;
import infra.web.client.RestTemplate;
import infra.web.client.config.RestTemplateBuilder;
import infra.web.client.config.RestTemplateCustomizer;
import infra.web.client.config.RestTemplateRequestCustomizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RestTemplateAutoConfiguration}
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class RestTemplateAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(RestTemplateAutoConfiguration.class));

  @Test
  void restTemplateBuilderConfigurerShouldBeLazilyDefined() {
    this.contextRunner.run((context) -> assertThat(
            context.getBeanFactory().getBeanDefinition("restTemplateBuilderConfigurer").isLazyInit()).isTrue());
  }

  @Test
  void restTemplateBuilderShouldBeLazilyDefined() {
    this.contextRunner.run(
            (context) -> assertThat(context.getBeanFactory().getBeanDefinition("restTemplateBuilder").isLazyInit())
                    .isTrue());
  }

  @Test
  void restTemplateWhenMessageConvertersDefinedShouldHaveMessageConverters() {
    this.contextRunner.withConfiguration(AutoConfigurations.of(HttpMessageConvertersAutoConfiguration.class))
            .withUserConfiguration(RestTemplateConfig.class).run((context) -> {
              assertThat(context).hasSingleBean(RestTemplate.class);
              RestTemplate restTemplate = context.getBean(RestTemplate.class);
              List<HttpMessageConverter<?>> converters = context.getBean(HttpMessageConverters.class)
                      .getConverters();
              assertThat(restTemplate.getMessageConverters()).containsExactlyElementsOf(converters);
              assertThat(restTemplate.getRequestFactory())
                      .isInstanceOf(HttpComponentsClientHttpRequestFactory.class);
            });
  }

  @Test
  void restTemplateWhenNoMessageConvertersDefinedShouldHaveDefaultMessageConverters() {
    this.contextRunner.withUserConfiguration(RestTemplateConfig.class).run((context) -> {
      assertThat(context).hasSingleBean(RestTemplate.class);
      RestTemplate restTemplate = context.getBean(RestTemplate.class);
      assertThat(restTemplate.getMessageConverters().size())
              .isEqualTo(new RestTemplate().getMessageConverters().size());
    });
  }

  @Test
  @SuppressWarnings({ "rawtypes" })
  void restTemplateWhenHasCustomMessageConvertersShouldHaveMessageConverters() {
    this.contextRunner.withConfiguration(AutoConfigurations.of(HttpMessageConvertersAutoConfiguration.class))
            .withUserConfiguration(CustomHttpMessageConverter.class, RestTemplateConfig.class).run((context) -> {
              assertThat(context).hasSingleBean(RestTemplate.class);
              RestTemplate restTemplate = context.getBean(RestTemplate.class);
              assertThat(restTemplate.getMessageConverters()).extracting(HttpMessageConverter::getClass)
                      .contains((Class) CustomHttpMessageConverter.class);
            });
  }

  @Test
  void restTemplateShouldApplyCustomizer() {
    this.contextRunner.withUserConfiguration(RestTemplateConfig.class, RestTemplateCustomizerConfig.class)
            .run((context) -> {
              assertThat(context).hasSingleBean(RestTemplate.class);
              RestTemplate restTemplate = context.getBean(RestTemplate.class);
              RestTemplateCustomizer customizer = context.getBean(RestTemplateCustomizer.class);
              then(customizer).should().customize(restTemplate);
            });
  }

  @Test
  void restTemplateWhenHasCustomBuilderShouldUseCustomBuilder() {
    this.contextRunner.withUserConfiguration(RestTemplateConfig.class, CustomRestTemplateBuilderConfig.class,
            RestTemplateCustomizerConfig.class).run((context) -> {
      assertThat(context).hasSingleBean(RestTemplate.class);
      RestTemplate restTemplate = context.getBean(RestTemplate.class);
      assertThat(restTemplate.getMessageConverters()).hasSize(1);
      assertThat(restTemplate.getMessageConverters().get(0))
              .isInstanceOf(CustomHttpMessageConverter.class);
      then(context.getBean(RestTemplateCustomizer.class)).shouldHaveNoInteractions();
    });
  }

  @Test
  void restTemplateWhenHasCustomBuilderCouldReuseBuilderConfigurer() {
    this.contextRunner.withUserConfiguration(RestTemplateConfig.class,
                    CustomRestTemplateBuilderWithConfigurerConfig.class, RestTemplateCustomizerConfig.class)
            .run((context) -> {
              assertThat(context).hasSingleBean(RestTemplate.class);
              RestTemplate restTemplate = context.getBean(RestTemplate.class);
              assertThat(restTemplate.getMessageConverters()).hasSize(1);
              assertThat(restTemplate.getMessageConverters().get(0))
                      .isInstanceOf(CustomHttpMessageConverter.class);
              RestTemplateCustomizer customizer = context.getBean(RestTemplateCustomizer.class);
              then(customizer).should().customize(restTemplate);
            });
  }

  @Test
  void restTemplateShouldApplyRequestCustomizer() {
    this.contextRunner.withUserConfiguration(RestTemplateRequestCustomizerConfig.class).run((context) -> {
      RestTemplateBuilder builder = context.getBean(RestTemplateBuilder.class);
      ClientHttpRequestFactory requestFactory = mock(ClientHttpRequestFactory.class);
      MockClientHttpRequest request = new MockClientHttpRequest();
      request.setResponse(new MockClientHttpResponse(new byte[0], HttpStatus.OK));
      given(requestFactory.createRequest(any(), any())).willReturn(request);
      RestTemplate restTemplate = builder.requestFactory(() -> requestFactory).build();
      restTemplate.getForEntity("http://localhost:8080/test", String.class);
      assertThat(request.getHeaders()).containsEntry("spring", Collections.singletonList("boot"));
    });
  }

  @Test
  void builderShouldBeFreshForEachUse() {
    this.contextRunner.withUserConfiguration(DirtyRestTemplateConfig.class)
            .run((context) -> assertThat(context).hasNotFailed());
  }

  @Test
  void whenMockWebApplicationRestTemplateBuilderIsConfigured() {
    new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(RestTemplateAutoConfiguration.class))
            .run((context) -> assertThat(context).hasSingleBean(RestTemplateBuilder.class)
                    .hasSingleBean(RestTemplateBuilderConfigurer.class));
  }

  @Test
  void whenReactiveWebApplicationRestTemplateBuilderIsNotConfigured() {
    new ReactiveWebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RestTemplateAutoConfiguration.class))
            .run((context) -> assertThat(context).doesNotHaveBean(RestTemplateBuilder.class)
                    .doesNotHaveBean(RestTemplateBuilderConfigurer.class));
  }

  @Configuration(proxyBeanMethods = false)
  static class RestTemplateConfig {

    @Bean
    RestTemplate restTemplate(RestTemplateBuilder builder) {
      return builder.build();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class DirtyRestTemplateConfig {

    @Bean
    RestTemplate restTemplateOne(RestTemplateBuilder builder) {
      try {
        return builder.build();
      }
      finally {
        breakBuilderOnNextCall(builder);
      }
    }

    @Bean
    RestTemplate restTemplateTwo(RestTemplateBuilder builder) {
      try {
        return builder.build();
      }
      finally {
        breakBuilderOnNextCall(builder);
      }
    }

    private void breakBuilderOnNextCall(RestTemplateBuilder builder) {
      builder.additionalCustomizers((restTemplate) -> {
        throw new IllegalStateException();
      });
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class CustomRestTemplateBuilderConfig {

    @Bean
    RestTemplateBuilder restTemplateBuilder() {
      return new RestTemplateBuilder().messageConverters(new CustomHttpMessageConverter());
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class CustomRestTemplateBuilderWithConfigurerConfig {

    @Bean
    RestTemplateBuilder restTemplateBuilder(RestTemplateBuilderConfigurer configurer) {
      return configurer.configure(new RestTemplateBuilder()).messageConverters(new CustomHttpMessageConverter());
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class RestTemplateCustomizerConfig {

    @Bean
    RestTemplateCustomizer restTemplateCustomizer() {
      return mock(RestTemplateCustomizer.class);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class RestTemplateRequestCustomizerConfig {

    @Bean
    RestTemplateRequestCustomizer<?> restTemplateRequestCustomizer() {
      return (request) -> request.getHeaders().add("spring", "boot");
    }

  }

  static class CustomHttpMessageConverter extends StringHttpMessageConverter {

  }

}
