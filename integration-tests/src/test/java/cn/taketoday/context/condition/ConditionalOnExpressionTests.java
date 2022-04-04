/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.context.condition;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.ConditionEvaluationContext;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.core.type.AnnotatedTypeMetadata;
import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;
import cn.taketoday.mock.env.MockEnvironment;

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
    ConditionEvaluationContext evaluationContext = mock(ConditionEvaluationContext.class);
    given(evaluationContext.getEnvironment()).willReturn(environment);
    ConditionOutcome outcome = condition.getMatchOutcome(evaluationContext, mockMetaData("invalid-spel"));
    assertThat(outcome.isMatch()).isFalse();
    assertThat(outcome.getMessage()).contains("invalid-spel").contains("no BeanFactory available");
  }

  private AnnotatedTypeMetadata mockMetaData(String value) {
    AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
    given(metadata.getAnnotationAttributes(ConditionalOnExpression.class.getName()))
            .willReturn(Collections.singletonMap("value", value));
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
