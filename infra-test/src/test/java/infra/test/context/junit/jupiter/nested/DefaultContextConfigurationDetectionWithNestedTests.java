/*
 * Copyright 2002-present the original author or authors.
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

package infra.test.context.junit.jupiter.nested;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.core.annotation.AnnotatedElementUtils;
import infra.test.context.ContextConfiguration;
import infra.test.context.junit.jupiter.InfraExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for detection of default context configuration within
 * nested test class hierarchies without the use of {@link ContextConfiguration}.
 *
 * @author Sam Brannen
 * @since 5.0
 */
@ExtendWith(InfraExtension.class)
class DefaultContextConfigurationDetectionWithNestedTests {

  @Autowired
  String greeting;

  @Test
  void test(@Autowired String localGreeting) {
    // This class must NOT be annotated with @JUnitConfig or @ContextConfiguration.
    assertThat(AnnotatedElementUtils.hasAnnotation(getClass(), ContextConfiguration.class)).isFalse();

    assertThat(greeting).isEqualTo("TEST");
    assertThat(localGreeting).isEqualTo("TEST");
  }

  @Nested
  class NestedTests {

    @Test
    void test(@Autowired String localGreeting) {
      assertThat(greeting).isEqualTo("TEST");
      assertThat(localGreeting).isEqualTo("TEST");
    }
  }

  @Configuration
  static class DefaultConfig {

    @Bean
    String greeting() {
      return "TEST";
    }
  }

}
