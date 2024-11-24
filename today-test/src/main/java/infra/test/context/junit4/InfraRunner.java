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
