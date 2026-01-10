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
