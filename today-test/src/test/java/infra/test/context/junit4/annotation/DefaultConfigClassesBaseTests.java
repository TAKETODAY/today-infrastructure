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
import org.junit.runner.RunWith;

import infra.beans.factory.annotation.Autowired;
import infra.beans.testfixture.beans.Employee;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.test.context.ContextConfiguration;
import infra.test.context.junit4.JUnit4ClassRunner;
import infra.test.context.support.AnnotationConfigContextLoader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify support for configuration classes in
 * the TestContext Framework.
 *
 * <p>Configuration will be loaded from {@link ContextConfiguration}.
 *
 * @author Sam Brannen
 * @see DefaultLoaderDefaultConfigClassesBaseTests
 * @since 4.0
 */
@RunWith(JUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class DefaultConfigClassesBaseTests {

  @Configuration
  static class ContextConfiguration {

    @Bean
    public Employee employee() {
      Employee employee = new Employee();
      employee.setName("John Smith");
      employee.setAge(42);
      employee.setCompany("Acme Widgets, Inc.");
      return employee;
    }
  }

  @Autowired
  protected Employee employee;

  @Test
  public void verifyEmployeeSetFromBaseContextConfig() {
    assertThat(this.employee).as("The employee field should have been autowired.").isNotNull();
    assertThat(this.employee.getName()).isEqualTo("John Smith");
  }

}
