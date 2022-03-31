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

package cn.taketoday.test.context.junit4;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import cn.taketoday.test.context.TestExecutionListeners;

import java.util.ArrayList;

import static cn.taketoday.test.context.junit4.JUnitTestingUtils.runTestsAndAssertCounters;

/**
 * Verifies proper handling of JUnit's {@link Test#expected() &#064;Test(expected = ...)}
 * support in conjunction with the {@link Runner}.
 *
 * @author Sam Brannen
 * @since 3.0
 */
@RunWith(JUnit4.class)
public class ExpectedExceptionSpringRunnerTests {

	@Test
	public void expectedExceptions() throws Exception {
		JUnitTestingUtils.runTestsAndAssertCounters(Runner.class, ExpectedExceptionSpringRunnerTestCase.class, 1, 0, 1, 0, 0);
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
