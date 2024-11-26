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

package infra.test.context.junit4.annotation;

import org.junit.Test;

import infra.beans.testfixture.beans.Employee;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify support for configuration classes in
 * the TestContext Framework.
 *
 * <p>Configuration will be loaded from {@link DefaultConfigClassesBaseTests.ContextConfiguration}
 * and {@link ContextConfiguration}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@ContextConfiguration
public class BeanOverridingDefaultConfigClassesInheritedTests extends DefaultConfigClassesBaseTests {

  @Configuration
  static class ContextConfiguration {

    @Bean
    public Employee employee() {
      Employee employee = new Employee();
      employee.setName("Yoda");
      employee.setAge(900);
      employee.setCompany("The Force");
      return employee;
    }
  }

  @Test
  @Override
  public void verifyEmployeeSetFromBaseContextConfig() {
    assertThat(this.employee).as("The employee should have been autowired.").isNotNull();
    assertThat(this.employee.getName()).as("The employee bean should have been overridden.").isEqualTo("Yoda");
  }

}
