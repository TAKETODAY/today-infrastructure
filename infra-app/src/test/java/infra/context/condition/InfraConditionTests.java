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

import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.ConditionContext;
import infra.context.annotation.Conditional;
import infra.context.annotation.Configuration;
import infra.core.type.AnnotatedTypeMetadata;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link InfraCondition}.
 *
 * @author Phillip Webb
 */
@SuppressWarnings("resource")
class InfraConditionTests {

  @Test
  void sensibleClassException() {
    assertThatIllegalStateException().isThrownBy(() -> new AnnotationConfigApplicationContext(ErrorOnClass.class))
            .withMessageContaining("Error processing condition on " + ErrorOnClass.class.getName());
  }

  @Test
  void sensibleMethodException() {
    assertThatIllegalStateException().isThrownBy(() -> new AnnotationConfigApplicationContext(ErrorOnMethod.class))
            .withMessageContaining("Error processing condition on " + ErrorOnMethod.class.getName() + ".myBean");
  }

  @Configuration(proxyBeanMethods = false)
  @Conditional(AlwaysThrowsCondition.class)
  static class ErrorOnClass {

  }

  @Configuration(proxyBeanMethods = false)
  static class ErrorOnMethod {

    @Bean
    @Conditional(AlwaysThrowsCondition.class)
    String myBean() {
      return "bean";
    }

  }

  static class AlwaysThrowsCondition extends InfraCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
      throw new RuntimeException("Oh no!");
    }

  }

}
