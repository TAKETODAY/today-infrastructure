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

package cn.taketoday.test.context.junit4;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;

import cn.taketoday.test.context.TestExecutionListeners;

/**
 * Verifies proper handling of JUnit's {@link Test#expected() &#064;Test(expected = ...)}
 * support in conjunction with the {@link InfraRunner}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@RunWith(JUnit4.class)
public class ExpectedExceptionInfraRunnerTests {

  @Test
  public void expectedExceptions() throws Exception {
    JUnitTestingUtils.runTestsAndAssertCounters(InfraRunner.class, ExpectedExceptionSpringRunnerTestCase.class, 1, 0, 1, 0, 0);
  }

  @Ignore("TestCase classes are run manually by the enclosing test class")
  @TestExecutionListeners({})
  public static final class ExpectedExceptionSpringRunnerTestCase {

    // Should Pass.
    @Test(expected = IndexOutOfBoundsException.class)
    public void verifyJUnitExpectedException() {
      new ArrayList<>().get(1);
    }
  }

}
