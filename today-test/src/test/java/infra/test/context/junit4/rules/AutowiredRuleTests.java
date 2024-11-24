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

package infra.test.context.junit4.rules;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for an issue raised in https://jira.spring.io/browse/SPR-15927.
 *
 * @author Sam Brannen
 * @since 4.0
 */
public class AutowiredRuleTests {

  @ClassRule
  public static final InfraClassRule applicationClassRule = new InfraClassRule();

  @Rule
  public final InfraMethodRule infraMethodRule = new InfraMethodRule();

  @Autowired
  @Rule
  public AutowiredTestRule autowiredTestRule;

  @Test
  public void test() {
    assertThat(autowiredTestRule).as("TestRule should have been @Autowired").isNotNull();

    // Rationale for the following assertion:
    //
    // The field value for the custom rule is null when JUnit sees it. JUnit then
    // ignores the null value, and at a later point in time Infra injects the rule
    // from the ApplicationContext and overrides the null field value. But that's too
    // late: JUnit never sees the rule supplied by Infra via dependency injection.
    assertThat(autowiredTestRule.applied).as("@Autowired TestRule should NOT have been applied").isFalse();
  }

  @Configuration
  static class Config {

    @Bean
    AutowiredTestRule autowiredTestRule() {
      return new AutowiredTestRule();
    }
  }

  static class AutowiredTestRule implements TestRule {

    private boolean applied = false;

    @Override
    public Statement apply(Statement base, Description description) {
      this.applied = true;
      return base;
    }
  }

}
