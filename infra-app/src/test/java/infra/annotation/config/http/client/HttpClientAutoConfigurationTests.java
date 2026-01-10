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

package infra.annotation.config.http.client;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import infra.annotation.config.ssl.SslAutoConfiguration;
import infra.app.test.context.runner.ApplicationContextRunner;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.AutoConfigurations;
import infra.http.client.config.HttpClientSettings;
import infra.http.client.config.HttpRedirects;

import static org.assertj.core.api.Assertions.*;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/1/2 20:57
 */
class HttpClientAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(HttpClientAutoConfiguration.class, SslAutoConfiguration.class));

  @Test
  void createsDefaultHttpClientSettings() {
    this.contextRunner.run((context) -> assertThat(context.getBean(HttpClientSettings.class))
            .isEqualTo(HttpClientSettings.defaults()));
  }

  @Test
  void createsHttpClientSettingsFromProperties() {
    this.contextRunner
            .withPropertyValues("http.clients.redirects=dont-follow", "http.clients.connect-timeout=1s", "http.clients.read-timeout=2s")
            .run((context) -> assertThat(context.getBean(HttpClientSettings.class)).isEqualTo(new HttpClientSettings(
                    HttpRedirects.DONT_FOLLOW, Duration.ofSeconds(1), Duration.ofSeconds(2), null)));
  }

  @Test
  void doesNotReplaceUserProvidedHttpClientSettings() {
    this.contextRunner.withUserConfiguration(TestHttpClientConfiguration.class)
            .run((context) -> assertThat(context.getBean(HttpClientSettings.class))
                    .isEqualTo(new HttpClientSettings(null, Duration.ofSeconds(1), Duration.ofSeconds(2), null)));
  }

  @Configuration(proxyBeanMethods = false)
  static class TestHttpClientConfiguration {

    @Bean
    HttpClientSettings httpClientSettings() {
      return HttpClientSettings.defaults().withTimeouts(Duration.ofSeconds(1), Duration.ofSeconds(2));
    }

  }

}