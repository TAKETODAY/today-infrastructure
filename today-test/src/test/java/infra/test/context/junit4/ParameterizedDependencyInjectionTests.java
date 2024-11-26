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

package infra.test.context.junit4;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.concurrent.atomic.AtomicInteger;

import infra.beans.factory.annotation.Autowired;
import infra.beans.testfixture.beans.Employee;
import infra.beans.testfixture.beans.Pet;
import infra.context.ApplicationContext;
import infra.test.context.ContextConfiguration;
import infra.test.context.TestContextManager;
import infra.test.context.TestExecutionListeners;
import infra.test.context.junit4.rules.ParameterizedSpringRuleTests;
import infra.test.context.support.DependencyInjectionTestExecutionListener;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simple JUnit 4 based integration test which demonstrates how to use JUnit's
 * {@link Parameterized} Runner in conjunction with
 * {@link ContextConfiguration @ContextConfiguration}, the
 * {@link DependencyInjectionTestExecutionListener}, and a
 * {@link TestContextManager} to provide dependency injection to a
 * <em>parameterized test instance</em>.
 *
 * @author Sam Brannen
 * @see ParameterizedSpringRuleTests
 * @since 4.0
 */
@RunWith(Parameterized.class)
@ContextConfiguration
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class })
public class ParameterizedDependencyInjectionTests {

  private static final AtomicInteger invocationCount = new AtomicInteger();

  private static final TestContextManager testContextManager = new TestContextManager(ParameterizedDependencyInjectionTests.class);

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private Pet pet;

  @Parameter(0)
  public String employeeBeanName;

  @Parameter(1)
  public String employeeName;

  @Parameters(name = "bean [{0}], employee [{1}]")
  public static String[][] employeeData() {
    return new String[][] { { "employee1", "John Smith" }, { "employee2", "Jane Smith" } };
  }

  @BeforeClass
  public static void BeforeClass() {
    invocationCount.set(0);
  }

  @Before
  public void injectDependencies() throws Exception {
    testContextManager.prepareTestInstance(this);
  }

  @Test
  public final void verifyPetAndEmployee() {
    invocationCount.incrementAndGet();

    // Verifying dependency injection:
    assertThat(this.pet).as("The pet field should have been autowired.").isNotNull();

    // Verifying 'parameterized' support:
    Employee employee = this.applicationContext.getBean(this.employeeBeanName, Employee.class);
    assertThat(employee.getName()).as("Name of the employee configured as bean [" + this.employeeBeanName + "].").isEqualTo(this.employeeName);
  }

  @AfterClass
  public static void verifyNumParameterizedRuns() {
    assertThat(invocationCount.get()).as("Number of times the parameterized test method was executed.").isEqualTo(employeeData().length);
  }

}
