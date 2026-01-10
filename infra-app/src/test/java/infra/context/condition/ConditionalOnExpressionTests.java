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

import java.util.Collections;

import infra.app.test.context.runner.ApplicationContextRunner;
import infra.context.annotation.Bean;
import infra.context.annotation.ConditionContext;
import infra.context.annotation.Configuration;
import infra.core.annotation.MergedAnnotation;
import infra.core.type.AnnotatedTypeMetadata;
import infra.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConditionalOnExpression @ConditionalOnExpression}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 */
class ConditionalOnExpressionTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

  @Test
  void expressionIsTrue() {
    this.contextRunner.withUserConfiguration(BasicConfiguration.class)
            .run((context) -> assertThat(context.getBean("foo")).isEqualTo("foo"));
  }

  @Test
  void expressionEvaluatesToTrueRegistersBean() {
    this.contextRunner.withUserConfiguration(MissingConfiguration.class)
            .run((context) -> assertThat(context).doesNotHaveBean("foo"));
  }

  @Test
  void expressionEvaluatesToFalseDoesNotRegisterBean() {
    this.contextRunner.withUserConfiguration(NullConfiguration.class)
            .run((context) -> assertThat(context).doesNotHaveBean("foo"));
  }

  @Test
  void expressionEvaluationWithNoBeanFactoryDoesNotMatch() {
    OnExpressionCondition condition = new OnExpressionCondition();
    MockEnvironment environment = new MockEnvironment();
    ConditionContext evaluationContext = mock(ConditionContext.class);
    given(evaluationContext.getEnvironment()).willReturn(environment);
    ConditionOutcome outcome = condition.getMatchOutcome(evaluationContext, mockMetaData("invalid-spel"));
    assertThat(outcome.isMatch()).isFalse();
    assertThat(outcome.getMessage()).contains("invalid-spel").contains("no BeanFactory available");
  }

  private AnnotatedTypeMetadata mockMetaData(String value) {
    AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
    MergedAnnotation<ConditionalOnExpression> annotation = MergedAnnotation.valueOf(
            ConditionalOnExpression.class, Collections.singletonMap("value", value));
    given(metadata.getAnnotation(ConditionalOnExpression.class)).willReturn(annotation);
    return metadata;
  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnExpression("false")
  static class MissingConfiguration {

    @Bean
    String bar() {
      return "bar";
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnExpression("true")
  static class BasicConfiguration {

    @Bean
    String foo() {
      return "foo";
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnExpression("true ? null : false")
  static class NullConfiguration {

    @Bean
    String foo() {
      return "foo";
    }

  }

}
