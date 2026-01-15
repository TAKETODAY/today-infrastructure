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

package infra.app.health.config.contributor;

import org.junit.jupiter.api.Test;

import infra.app.health.contributor.HealthIndicator;
import infra.app.health.contributor.PingHealthIndicator;
import infra.app.test.context.runner.ApplicationContextRunner;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnEnabledHealthIndicator}.
 *
 * @author Phillip Webb
 */
class ConditionalOnEnabledHealthIndicatorTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withUserConfiguration(CustomHealthIndicatorConfiguration.class);

  @Test
  void whenNoPropertyCreatesBean() {
    this.contextRunner.run((context) -> assertThat(context).hasSingleBean(HealthIndicator.class));
  }

  @Test
  void whenIndicatorPropertyTrueCreatesBean() {
    this.contextRunner.withPropertyValues("app.health.custom.enabled=true")
            .run((context) -> assertThat(context).hasSingleBean(HealthIndicator.class));
  }

  @Test
  void whenIndicatorPropertyFalseDoesNotCreateBean() {
    this.contextRunner.withPropertyValues("app.health.custom.enabled=false")
            .run((context) -> assertThat(context).doesNotHaveBean(HealthIndicator.class));
  }

  @Test
  void whenDefaultsPropertyTrueCreatesBean() {
    this.contextRunner.withPropertyValues("app.health.defaults.enabled=true")
            .run((context) -> assertThat(context).hasSingleBean(HealthIndicator.class));
  }

  @Test
  void whenDefaultsPropertyFalseDoesNotCreateBean() {
    this.contextRunner.withPropertyValues("app.health.defaults.enabled=false")
            .run((context) -> assertThat(context).doesNotHaveBean(HealthIndicator.class));
  }

  @Test
  void whenIndicatorPropertyTrueAndDefaultsPropertyFalseCreatesBean() {
    this.contextRunner
            .withPropertyValues("app.health.custom.enabled=true", "app.health.defaults.enabled=false")
            .run((context) -> assertThat(context).hasSingleBean(HealthIndicator.class));
  }

  @Test
  void whenIndicatorPropertyFalseAndDefaultsPropertyTrueDoesNotCreateBean() {
    this.contextRunner
            .withPropertyValues("app.health.custom.enabled=false", "app.health.defaults.enabled=true")
            .run((context) -> assertThat(context).doesNotHaveBean(HealthIndicator.class));
  }

  @Configuration(proxyBeanMethods = false)
  static class CustomHealthIndicatorConfiguration {

    @Bean
    @ConditionalOnEnabledHealthIndicator("custom")
    PingHealthIndicator customHealthIndicator() {
      return new PingHealthIndicator();
    }

  }

}
