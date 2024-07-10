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

package cn.taketoday.test.context.junit4.rules;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.runner.Runner;
import org.junit.runners.JUnit4;

import cn.taketoday.test.context.TestExecutionListeners;
import cn.taketoday.test.context.junit4.TimedInfraRunnerTests;

import static org.assertj.core.api.Assertions.fail;

/**
 * This class is an extension of {@link TimedInfraRunnerTests}
 * that has been modified to use {@link InfraClassRule} and
 * {@link InfraMethodRule}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
public class TimedInfraRuleTests extends TimedInfraRunnerTests {

  // All tests are in superclass.

  @Override
  protected Class<?> getTestCase() {
    return TimedSpringRuleTestCase.class;
  }

  @Override
  protected Class<? extends Runner> getRunnerClass() {
    return JUnit4.class;
  }

  @Ignore("TestCase classes are run manually by the enclosing test class")
  @TestExecutionListeners({})
  public static final class TimedSpringRuleTestCase extends TimedSpringRunnerTestCase {

    @ClassRule
    public static final InfraClassRule applicationClassRule = new InfraClassRule();

    @Rule
    public final InfraMethodRule infraMethodRule = new InfraMethodRule();

    /**
     * Overridden to always throw an exception, since Infra Rule-based
     * JUnit integration does not fail a test for duplicate configuration
     * of timeouts.
     */
    @Override
    public void springAndJUnitTimeouts() {
      fail("intentional failure to make tests in superclass pass");
    }

    // All other tests are in superclass.
  }

}
