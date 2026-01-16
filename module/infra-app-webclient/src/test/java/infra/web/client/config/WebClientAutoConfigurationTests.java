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

package infra.web.client.config;

import org.junit.jupiter.api.Test;

import infra.app.config.ssl.SslAutoConfiguration;
import infra.app.test.context.runner.ApplicationContextRunner;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.AutoConfigurations;
import infra.http.client.config.HttpClientAutoConfiguration;
import infra.http.client.config.reactive.ReactiveHttpClientAutoConfiguration;
import infra.http.codec.CodecConfigurer;
import infra.http.codec.CodecCustomizer;
import infra.web.client.WebClientCustomizer;
import infra.web.client.reactive.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link WebClientAutoConfiguration}
 *
 * @author Brian Clozel
 * @author Phillip Webb
 */
class WebClientAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(ReactiveHttpClientAutoConfiguration.class,
                  HttpClientAutoConfiguration.class, WebClientAutoConfiguration.class, SslAutoConfiguration.class));

  @Test
  void shouldCreateBuilder() {
    this.contextRunner.run((context) -> {
      WebClient.Builder builder = context.getBean(WebClient.Builder.class);
      WebClient webClient = builder.build();
      assertThat(webClient).isNotNull();
    });
  }

  @Test
  void shouldCustomizeClientCodecs() {
    this.contextRunner.withUserConfiguration(CodecConfiguration.class).run((context) -> {
      WebClient.Builder builder = context.getBean(WebClient.Builder.class);
      CodecCustomizer codecCustomizer = context.getBean(CodecCustomizer.class);
      WebClientCodecCustomizer clientCustomizer = context.getBean(WebClientCodecCustomizer.class);
      builder.build();
      assertThat(clientCustomizer).isNotNull();
      then(codecCustomizer).should().customize(any(CodecConfigurer.class));
    });
  }

  @Test
  void webClientShouldApplyCustomizers() {
    this.contextRunner.withUserConfiguration(WebClientCustomizerConfig.class).run((context) -> {
      WebClient.Builder builder = context.getBean(WebClient.Builder.class);
      WebClientCustomizer customizer = context.getBean("webClientCustomizer", WebClientCustomizer.class);
      builder.build();
      then(customizer).should().customize(any(WebClient.Builder.class));
    });
  }

  @Test
  void shouldGetPrototypeScopedBean() {
    this.contextRunner.withUserConfiguration(WebClientCustomizerConfig.class).run((context) -> {
      WebClient.Builder firstBuilder = context.getBean(WebClient.Builder.class);
      WebClient.Builder secondBuilder = context.getBean(WebClient.Builder.class);
      assertThat(firstBuilder).isNotEqualTo(secondBuilder);
    });
  }

  @Test
  void shouldNotCreateClientBuilderIfAlreadyPresent() {
    this.contextRunner.withUserConfiguration(WebClientCustomizerConfig.class, CustomWebClientBuilderConfig.class)
            .run((context) -> {
              WebClient.Builder builder = context.getBean(WebClient.Builder.class);
              assertThat(builder).isInstanceOf(MyWebClientBuilder.class);
            });
  }

  @Test
  void shouldCreateWebClientSsl() {
    this.contextRunner.run((context) -> {
      WebClientSsl webClientSsl = context.getBean(WebClientSsl.class);
      assertThat(webClientSsl).isInstanceOf(AutoConfiguredWebClientSsl.class);
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
  static class WebClientCustomizerConfig {

    @Bean
    WebClientCustomizer webClientCustomizer() {
      return mock(WebClientCustomizer.class);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class CustomWebClientBuilderConfig {

    @Bean
    MyWebClientBuilder myWebClientBuilder() {
      return mock(MyWebClientBuilder.class);
    }

  }

  interface MyWebClientBuilder extends WebClient.Builder {

  }

}
