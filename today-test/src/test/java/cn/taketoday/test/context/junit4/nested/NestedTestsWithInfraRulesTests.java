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

package cn.taketoday.test.context.junit4.nested;

import org.junit.Test;
import org.junit.runner.RunWith;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.junit4.rules.InfraClassRule;
import cn.taketoday.test.context.junit4.rules.InfraMethodRule;
import de.bechte.junit.runners.context.HierarchicalContextRunner;

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
