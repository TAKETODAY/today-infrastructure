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

package infra.test.context.junit4;

import org.junit.runners.model.InitializationError;

import infra.test.context.TestContextManager;
import infra.test.context.junit4.rules.InfraClassRule;
import infra.test.context.junit4.rules.InfraMethodRule;

/**
 * {@code Runner} is an <em>alias</em> for the {@link JUnit4ClassRunner}.
 *
 * <p>To use this class, simply annotate a JUnit 4 based test class with
 * {@code @RunWith(Runner.class)}.
 *
 * <p>If you would like to use the TestContext Framework with a runner other than
 * this one, use {@link InfraClassRule}
 * and {@link InfraMethodRule}.
 *
 * <p><strong>NOTE:</strong> This class requires JUnit 4.12 or higher.
 *
 * @author Sam Brannen
 * @see JUnit4ClassRunner
 * @see InfraClassRule
 * @see InfraMethodRule
 * @since 4.0
 */
public final class InfraRunner extends JUnit4ClassRunner {

  /**
   * Construct a new {@code Runner} and initialize a
   * {@link TestContextManager TestContextManager}
   * to provide Infra testing functionality to standard JUnit 4 tests.
   *
   * @param clazz the test class to be run
   * @see #createTestContextManager(Class)
   */
  public InfraRunner(Class<?> clazz) throws InitializationError {
    super(clazz);
  }

}
