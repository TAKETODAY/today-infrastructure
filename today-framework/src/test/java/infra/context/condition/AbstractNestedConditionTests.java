/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.context.condition;

import org.junit.jupiter.api.Test;

import infra.app.test.context.runner.ApplicationContextRunner;
import infra.context.annotation.Bean;
import infra.context.annotation.Conditional;
import infra.context.annotation.Configuration;
import infra.context.condition.AbstractNestedCondition;
import infra.context.condition.ConditionOutcome;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.condition.OnBeanCondition;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AbstractNestedCondition}.
 *
 * @author Razib Shahriar
 */
class AbstractNestedConditionTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

  @Test
  void validPhase() {
    this.contextRunner.withUserConfiguration(ValidConfig.class)
            .run((context) -> assertThat(context).hasBean("myBean"));
  }

  @Test
  void invalidMemberPhase() {
    this.contextRunner.withUserConfiguration(InvalidConfig.class).run((context) -> {
      assertThat(context).hasFailed();
      assertThat(context.getStartupFailure().getCause()).isInstanceOf(IllegalStateException.class)
              .hasMessageContaining("Nested condition " + InvalidNestedCondition.class.getName()
                      + " uses a configuration phase that is inappropriate for class "
                      + OnBeanCondition.class.getName());
    });
  }

  @Test
  void invalidNestedMemberPhase() {
    this.contextRunner.withUserConfiguration(DoubleNestedConfig.class).run((context) -> {
      assertThat(context).hasFailed();
      assertThat(context.getStartupFailure().getCause()).isInstanceOf(IllegalStateException.class)
              .hasMessageContaining("Nested condition " + DoubleNestedCondition.class.getName()
                      + " uses a configuration phase that is inappropriate for class "
                      + ValidNestedCondition.class.getName());
    });
  }

  @Configuration(proxyBeanMethods = false)
  @Conditional(ValidNestedCondition.class)
  static class ValidConfig {

    @Bean
    String myBean() {
      return "myBean";
    }

  }

  static class ValidNestedCondition extends AbstractNestedCondition {

    ValidNestedCondition() {
      super(ConfigurationPhase.REGISTER_BEAN);
    }

    @Override
    protected ConditionOutcome getFinalMatchOutcome(MemberMatchOutcomes memberOutcomes) {
      return ConditionOutcome.match();
    }

    @ConditionalOnMissingBean(name = "myBean")
    static class MissingMyBean {

    }

  }

  @Configuration(proxyBeanMethods = false)
  @Conditional(InvalidNestedCondition.class)
  static class InvalidConfig {

    @Bean
    String myBean() {
      return "myBean";
    }

  }

  static class InvalidNestedCondition extends AbstractNestedCondition {

    InvalidNestedCondition() {
      super(ConfigurationPhase.PARSE_CONFIGURATION);
    }

    @Override
    protected ConditionOutcome getFinalMatchOutcome(MemberMatchOutcomes memberOutcomes) {
      return ConditionOutcome.match();
    }

    @ConditionalOnMissingBean(name = "myBean")
    static class MissingMyBean {

    }

  }

  @Configuration(proxyBeanMethods = false)
  @Conditional(DoubleNestedCondition.class)
  static class DoubleNestedConfig {

  }

  static class DoubleNestedCondition extends AbstractNestedCondition {

    DoubleNestedCondition() {
      super(ConfigurationPhase.PARSE_CONFIGURATION);
    }

    @Override
    protected ConditionOutcome getFinalMatchOutcome(MemberMatchOutcomes memberOutcomes) {
      return ConditionOutcome.match();
    }

    @Conditional(ValidNestedCondition.class)
    static class NestedConditionThatIsValid {

    }

  }

}
