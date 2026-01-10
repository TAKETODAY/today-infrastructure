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

package infra.test.context.junit4;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;

import infra.test.context.TestExecutionListeners;

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
    JUnitTestingUtils.runTestsAndAssertCounters(InfraRunner.class, ExpectedExceptionInfraRunnerTestCase.class, 1, 0, 1, 0, 0);
  }

  @Ignore("TestCase classes are run manually by the enclosing test class")
  @TestExecutionListeners({})
  public static final class ExpectedExceptionInfraRunnerTestCase {

    // Should Pass.
    @Test(expected = IndexOutOfBoundsException.class)
    public void verifyJUnitExpectedException() {
      new ArrayList<>().get(1);
    }
  }

}
