/*
 * Copyright 2017 - 2026 the original author or authors.
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

package infra.test.context.junit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import infra.test.annotation.IfProfileValue;
import infra.test.context.TestContext;
import infra.test.context.TestExecutionListener;
import infra.test.context.TestExecutionListeners;
import infra.test.context.aot.DisabledInAotMode;

import static org.assertj.core.api.Assertions.fail;

/**
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
@RunWith(InfraRunner.class)
@TestExecutionListeners(ClassLevelDisabledInfraRunnerTests.CustomTestExecutionListener.class)
@IfProfileValue(name = "ClassLevelDisabledSpringRunnerTests.profile_value.name", value = "enigmaX")
// Since Infra test's AOT processing support does not evaluate @IfProfileValue,
// this test class simply is not supported for AOT processing.
@DisabledInAotMode
public class ClassLevelDisabledInfraRunnerTests {

  @Test
  public void testIfProfileValueDisabled() {
    fail("The body of a disabled test should never be executed!");
  }

  public static class CustomTestExecutionListener implements TestExecutionListener {

    @Override
    public void beforeTestClass(TestContext testContext) throws Exception {
      fail("A listener method for a disabled test should never be executed!");
    }

    @Override
    public void prepareTestInstance(TestContext testContext) throws Exception {
      fail("A listener method for a disabled test should never be executed!");
    }

    @Override
    public void beforeTestMethod(TestContext testContext) throws Exception {
      fail("A listener method for a disabled test should never be executed!");
    }

    @Override
    public void afterTestMethod(TestContext testContext) throws Exception {
      fail("A listener method for a disabled test should never be executed!");
    }

    @Override
    public void afterTestClass(TestContext testContext) throws Exception {
      fail("A listener method for a disabled test should never be executed!");
    }
  }
}
