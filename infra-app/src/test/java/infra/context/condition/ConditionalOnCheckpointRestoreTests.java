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

import infra.app.test.context.runner.ApplicationContextRunner;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.test.classpath.ClassPathExclusions;
import infra.test.classpath.ClassPathOverrides;

import static org.assertj.core.api.Assertions.assertThat;

class ConditionalOnCheckpointRestoreTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withUserConfiguration(BasicConfiguration.class);

  @Test
  @ClassPathExclusions("crac-*.jar")
  void whenCracIsUnavailableThenConditionDoesNotMatch() {
    this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean("someBean"));
  }

  @Test
  @ClassPathOverrides("org.crac:crac:1.3.0")
  void whenCracIsAvailableThenConditionMatches() {
    this.contextRunner.run((context) -> assertThat(context).hasBean("someBean"));
  }

  @Configuration(proxyBeanMethods = false)
  static class BasicConfiguration {

    @Bean
    @ConditionalOnCheckpointRestore
    String someBean() {
      return "someBean";
    }

  }

}