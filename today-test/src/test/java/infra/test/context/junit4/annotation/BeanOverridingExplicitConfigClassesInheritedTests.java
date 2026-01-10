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

import infra.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify support for configuration classes in
 * the TestContext Framework.
 *
 * <p>Configuration will be loaded from {@link DefaultConfigClassesBaseTests.ContextConfiguration}
 * and {@link BeanOverridingDefaultConfigClassesInheritedTests.ContextConfiguration}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@ContextConfiguration(classes = BeanOverridingDefaultConfigClassesInheritedTests.ContextConfiguration.class)
public class BeanOverridingExplicitConfigClassesInheritedTests extends ExplicitConfigClassesBaseTests {

  @Test
  @Override
  public void verifyEmployeeSetFromBaseContextConfig() {
    assertThat(this.employee).as("The employee should have been autowired.").isNotNull();
    assertThat(this.employee.getName()).as("The employee bean should have been overridden.").isEqualTo("Yoda");
  }

}
