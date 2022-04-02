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

package cn.taketoday.test.context;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JUnit 4 based unit test for {@link TestContextManager}, which verifies proper
 * <em>execution order</em> of registered {@link TestExecutionListener
 * TestExecutionListeners}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
class TestContextManagerTests {

  private static final List<String> executionOrder = new ArrayList<>();

  private final TestContextManager testContextManager = new TestContextManager(ExampleTestCase.class);

  private final Method testMethod;

  {
    try {
      this.testMethod = ExampleTestCase.class.getDeclaredMethod("exampleTestMethod");
    }
    catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  @Test
  void listenerExecutionOrder() throws Exception {
    // @formatter:off
		assertThat(this.testContextManager.getTestExecutionListeners().size()).as("Registered TestExecutionListeners").isEqualTo(3);

		this.testContextManager.beforeTestMethod(this, this.testMethod);
		assertExecutionOrder("beforeTestMethod",
			"beforeTestMethod-1",
				"beforeTestMethod-2",
					"beforeTestMethod-3"
		);

		this.testContextManager.beforeTestExecution(this, this.testMethod);
		assertExecutionOrder("beforeTestExecution",
			"beforeTestMethod-1",
				"beforeTestMethod-2",
					"beforeTestMethod-3",
						"beforeTestExecution-1",
							"beforeTestExecution-2",
								"beforeTestExecution-3"
		);

		this.testContextManager.afterTestExecution(this, this.testMethod, null);
		assertExecutionOrder("afterTestExecution",
			"beforeTestMethod-1",
				"beforeTestMethod-2",
					"beforeTestMethod-3",
						"beforeTestExecution-1",
							"beforeTestExecution-2",
								"beforeTestExecution-3",
								"afterTestExecution-3",
							"afterTestExecution-2",
						"afterTestExecution-1"
		);

		this.testContextManager.afterTestMethod(this, this.testMethod, null);
		assertExecutionOrder("afterTestMethod",
			"beforeTestMethod-1",
				"beforeTestMethod-2",
					"beforeTestMethod-3",
						"beforeTestExecution-1",
							"beforeTestExecution-2",
								"beforeTestExecution-3",
								"afterTestExecution-3",
							"afterTestExecution-2",
						"afterTestExecution-1",
					"afterTestMethod-3",
				"afterTestMethod-2",
			"afterTestMethod-1"
		);
		// @formatter:on
  }

  private static void assertExecutionOrder(String usageContext, String... expectedBeforeTestMethodCalls) {
    assertThat(executionOrder).as("execution order (" + usageContext + ") ==>").isEqualTo(Arrays.asList(expectedBeforeTestMethodCalls));
  }

  @TestExecutionListeners({ FirstTel.class, SecondTel.class, ThirdTel.class })
  private static class ExampleTestCase {

    @SuppressWarnings("unused")
    public void exampleTestMethod() {
    }
  }

  private static class NamedTestExecutionListener implements TestExecutionListener {

    private final String name;

    NamedTestExecutionListener(String name) {
      this.name = name;
    }

    @Override
    public void beforeTestMethod(TestContext testContext) {
      executionOrder.add("beforeTestMethod-" + this.name);
    }

    @Override
    public void beforeTestExecution(TestContext testContext) {
      executionOrder.add("beforeTestExecution-" + this.name);
    }

    @Override
    public void afterTestExecution(TestContext testContext) {
      executionOrder.add("afterTestExecution-" + this.name);
    }

    @Override
    public void afterTestMethod(TestContext testContext) {
      executionOrder.add("afterTestMethod-" + this.name);
    }
  }

  private static class FirstTel extends NamedTestExecutionListener {

    public FirstTel() {
      super("1");
    }
  }

  private static class SecondTel extends NamedTestExecutionListener {

    public SecondTel() {
      super("2");
    }
  }

  private static class ThirdTel extends NamedTestExecutionListener {

    public ThirdTel() {
      super("3");
    }
  }

}
