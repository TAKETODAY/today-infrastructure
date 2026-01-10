/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
