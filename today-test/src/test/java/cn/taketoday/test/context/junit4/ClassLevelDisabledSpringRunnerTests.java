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

import org.junit.Test;
import org.junit.runner.RunWith;

import cn.taketoday.test.annotation.IfProfileValue;
import cn.taketoday.test.context.TestContext;
import cn.taketoday.test.context.TestExecutionListener;
import cn.taketoday.test.context.TestExecutionListeners;

import static org.assertj.core.api.Assertions.fail;

/**
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
@RunWith(InfraRunner.class)
@TestExecutionListeners(ClassLevelDisabledSpringRunnerTests.CustomTestExecutionListener.class)
@IfProfileValue(name = "ClassLevelDisabledSpringRunnerTests.profile_value.name", value = "enigmaX")
public class ClassLevelDisabledSpringRunnerTests {

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
