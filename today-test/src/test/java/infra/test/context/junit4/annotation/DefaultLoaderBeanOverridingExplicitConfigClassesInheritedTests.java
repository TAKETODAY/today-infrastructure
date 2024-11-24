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

import infra.test.context.ContextConfiguration;
import infra.test.context.support.DelegatingSmartContextLoader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify support for configuration classes in
 * the TestContext Framework in conjunction with the
 * {@link DelegatingSmartContextLoader}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@ContextConfiguration(classes = DefaultLoaderBeanOverridingDefaultConfigClassesInheritedTests.Config.class)
public class DefaultLoaderBeanOverridingExplicitConfigClassesInheritedTests extends
        DefaultLoaderExplicitConfigClassesBaseTests {

  @Test
  @Override
  public void verifyEmployeeSetFromBaseContextConfig() {
    assertThat(this.employee).as("The employee should have been autowired.").isNotNull();
    assertThat(this.employee.getName()).as("The employee bean should have been overridden.").isEqualTo("Yoda");
  }

}
