/*
 * Copyright 2012-2022 the original author or authors.
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

package cn.taketoday.annotation.config.web.client;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import cn.taketoday.annotation.config.http.HttpMessageConvertersAutoConfiguration;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.config.AutoConfigurations;
import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;
import cn.taketoday.framework.test.context.runner.ReactiveWebApplicationContextRunner;
import cn.taketoday.framework.test.context.runner.WebApplicationContextRunner;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.client.ClientHttpRequestFactory;
import cn.taketoday.http.client.HttpComponentsClientHttpRequestFactory;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.converter.HttpMessageConverters;
import cn.taketoday.http.converter.StringHttpMessageConverter;
import cn.taketoday.mock.http.client.MockClientHttpRequest;
import cn.taketoday.mock.http.client.MockClientHttpResponse;
import cn.taketoday.web.client.RestTemplate;
import cn.taketoday.web.client.config.RestTemplateAutoConfiguration;
import cn.taketoday.web.client.config.RestTemplateBuilder;
import cn.taketoday.web.client.config.RestTemplateBuilderConfigurer;
import cn.taketoday.web.client.config.RestTemplateCustomizer;
import cn.taketoday.web.client.config.RestTemplateRequestCustomizer;

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
  void whenServletWebApplicationRestTemplateBuilderIsConfigured() {
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
