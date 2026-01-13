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

package infra.context.condition;

import org.junit.jupiter.api.Test;

import infra.annotation.ConditionalOnNotWebApplication;
import infra.app.test.context.runner.ApplicationContextRunner;
import infra.app.test.context.runner.ReactiveWebApplicationContextRunner;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.http.server.reactive.HttpHandler;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link ConditionalOnNotWebApplication @ConditionalOnNotWebApplication}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 */
class ConditionalOnNotWebApplicationTests {

  @Test
//  @Disabled("Reactive Web Server not supported")
  void testNotWebApplicationWithReactiveContext() {
    new ReactiveWebApplicationContextRunner()
            .withUserConfiguration(ReactiveApplicationConfig.class, NotWebApplicationConfiguration.class)
            .run((context) -> assertThat(context).doesNotHaveBean(String.class));
  }

  @Test
  void testNotWebApplication() {
    new ApplicationContextRunner().withUserConfiguration(NotWebApplicationConfiguration.class)
            .run((context) -> assertThat(context).getBeans(String.class).containsExactly(entry("none", "none")));
  }

  @Configuration(proxyBeanMethods = false)
  static class ReactiveApplicationConfig {

    @Bean
    HttpHandler httpHandler() {
      return (request, response) -> Mono.empty();
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnNotWebApplication
  static class NotWebApplicationConfiguration {

    @Bean
    String none() {
      return "none";
    }

  }

}
