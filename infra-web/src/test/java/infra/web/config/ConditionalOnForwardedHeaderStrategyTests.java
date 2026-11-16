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

package infra.web.config;

import org.junit.jupiter.api.Test;

import infra.test.context.runner.ApplicationContextRunner;
import infra.stereotype.Component;
import infra.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class ConditionalOnForwardedHeaderStrategyTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withUserConfiguration(FrameworkRequiredConfiguration.class);

  @Test
  void matchesIfStrategyPropertyIsValue() {
    this.contextRunner.withPropertyValues("web.forwarded-headers.strategy=framework")
            .run(context -> assertThat(context).hasBean("bean"));
  }

  @Test
  void doesNotMatchesIfStrategyIsNotValue() {
    this.contextRunner.withPropertyValues("web.forwarded-headers.strategy=none")
            .run(context -> assertThat(context).doesNotHaveBean("bean"));
  }

  @Test
  void doesNotMatchesIfStrategyIsMissing() {
    this.contextRunner.run(context -> assertThat(context).doesNotHaveBean("bean"));
  }

  @Test
  void matchesHyphenatedWebServerStrategy() {
    new ApplicationContextRunner().withUserConfiguration(WebServerRequiredConfiguration.class)
            .withPropertyValues("web.forwarded-headers.strategy=web-server")
            .run(context -> assertThat(context).hasBean("bean"));
  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnForwardedHeaderStrategy(ForwardedHeaderStrategy.WEB_SERVER)
  static class WebServerRequiredConfiguration {

    @Component
    String bean() {
      return "bean";
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnForwardedHeaderStrategy(ForwardedHeaderStrategy.FRAMEWORK)
  static class FrameworkRequiredConfiguration {

    @Component
    String bean() {
      return "bean";
    }

  }

}
