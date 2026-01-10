/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.context.condition;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import infra.app.test.context.runner.ApplicationContextRunner;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnVirtualThreads @ConditionalOnVirtualThreads}.
 *
 * @author Moritz Halbritter
 */
class ConditionalOnVirtualThreadsTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withUserConfiguration(BasicConfiguration.class);

  @Test
  @EnabledForJreRange(max = JRE.JAVA_20)
  void isDisabledOnJdkBelow21EvenIfPropertyIsSet() {
    this.contextRunner.withPropertyValues("infra.threads.virtual.enabled=true")
            .run((context) -> assertThat(context).doesNotHaveBean("someBean"));
  }

  @Test
  @EnabledForJreRange(min = JRE.JAVA_21)
  void isDisabledOnJdk21IfPropertyIsNotSet() {
    this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean("someBean"));
  }

  @Test
  @EnabledForJreRange(min = JRE.JAVA_21)
  void isEnabledOnJdk21IfPropertyIsSet() {
    this.contextRunner.withPropertyValues("infra.threads.virtual.enabled=true")
            .run((context) -> assertThat(context).hasBean("someBean"));
  }

  @Configuration(proxyBeanMethods = false)
  static class BasicConfiguration {

    @Bean
    @ConditionalOnVirtualThreads
    String someBean() {
      return "someBean";
    }

  }

}
