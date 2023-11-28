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

import org.junit.jupiter.api.Test;

import cn.taketoday.annotation.config.ssl.SslAutoConfiguration;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.config.AutoConfigurations;
import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;
import cn.taketoday.http.codec.CodecConfigurer;
import cn.taketoday.http.codec.CodecCustomizer;
import cn.taketoday.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link WebClientAutoConfiguration}
 *
 * @author Brian Clozel
 */
class WebClientAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(ClientHttpConnectorAutoConfiguration.class,
                  WebClientAutoConfiguration.class, SslAutoConfiguration.class));

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
