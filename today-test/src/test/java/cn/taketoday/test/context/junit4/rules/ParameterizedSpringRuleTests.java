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

package cn.taketoday.test.context.junit4.rules;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.concurrent.atomic.AtomicInteger;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.testfixture.beans.Employee;
import cn.taketoday.beans.testfixture.beans.Pet;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.junit4.ParameterizedDependencyInjectionTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test which demonstrates how to use JUnit's {@link Parameterized}
 * runner in conjunction with {@link InfraClassRule} and {@link InfraMethodRule}
 * to provide dependency injection to a <em>parameterized test instance</em>.
 *
 * @author Sam Brannen
 * @see ParameterizedDependencyInjectionTests
 * @since 4.0
 */
@RunWith(Parameterized.class)
@ContextConfiguration("/cn/taketoday/test/context/junit4/ParameterizedDependencyInjectionTests-context.xml")
public class ParameterizedSpringRuleTests {

  private static final AtomicInteger invocationCount = new AtomicInteger();

  @ClassRule
  public static final InfraClassRule applicationClassRule = new InfraClassRule();

  @Rule
  public final InfraMethodRule infraMethodRule = new InfraMethodRule();

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
