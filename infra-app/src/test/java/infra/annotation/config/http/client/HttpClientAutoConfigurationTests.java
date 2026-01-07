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