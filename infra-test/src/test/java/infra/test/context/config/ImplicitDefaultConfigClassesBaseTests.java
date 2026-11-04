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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.test.context.config;

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
 * Base tests for detection of implicit configuration classes.
 *
 * @author Sam Brannen
 * @since 5.0
 */
@ExtendWith(InfraExtension.class)
class ImplicitDefaultConfigClassesBaseTests {

  @Autowired
  String greeting1;

  @Autowired
  Integer puzzle1;

  @Test
  final void greeting1AndPuzzle1() {
    // This class must NOT be annotated with @JUnitConfig or @ContextConfiguration.
    assertThat(AnnotatedElementUtils.hasAnnotation(getClass(), ContextConfiguration.class)).isFalse();

    assertThat(greeting1).isEqualTo("TEST 1");
    assertThat(puzzle1).isEqualTo(111);
  }

  @Configuration
  static class DefaultConfig1A {

    @Bean
    String greeting1() {
      return "TEST 1";
    }
  }

  @Configuration
  static class DefaultConfig1B {

    @Bean
    Integer puzzle1() {
      return 111;
    }
  }

}
