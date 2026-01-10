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

package infra.test.context.junit4.nested;

import org.junit.Test;
import org.junit.runner.RunWith;

import de.bechte.junit.runners.context.HierarchicalContextRunner;
import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.test.context.ContextConfiguration;
import infra.test.context.junit4.rules.InfraClassRule;
import infra.test.context.junit4.rules.InfraMethodRule;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JUnit 4 based integration tests for <em>nested</em> test classes that are
 * executed via a custom JUnit 4 {@link HierarchicalContextRunner} and Infra
 * {@link InfraClassRule} and {@link InfraMethodRule} support.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@RunWith(HierarchicalContextRunner.class)
@ContextConfiguration(classes = NestedTestsWithInfraRulesTests.TopLevelConfig.class)
public class NestedTestsWithInfraRulesTests extends InfraRuleConfigurer {

  @Autowired
  String foo;

  @Test
  public void topLevelTest() {
    assertThat(foo).isEqualTo("foo");
  }

  @ContextConfiguration(classes = NestedConfig.class)
  public class NestedTestCase extends InfraRuleConfigurer {

    @Autowired
    String bar;

    @Test
    public void nestedTest() throws Exception {
      assertThat(foo).isEqualTo("foo");
      assertThat(bar).isEqualTo("bar");
    }
  }

  // -------------------------------------------------------------------------

  @Configuration
  public static class TopLevelConfig {

    @Bean
    String foo() {
      return "foo";
    }
  }

  @Configuration
  public static class NestedConfig {

    @Bean
    String bar() {
      return "bar";
    }
  }

}
