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

package cn.taketoday.test.context.junit4;

import org.junit.runner.RunWith;

import cn.taketoday.test.context.TestExecutionListeners;
import cn.taketoday.test.context.aot.DisabledInAotMode;

/**
 * <p>
 * Simple unit test to verify that {@link InfraRunner} does not
 * hinder correct functionality of standard JUnit 4.4+ testing features.
 * </p>
 * <p>
 * Note that {@link TestExecutionListeners @TestExecutionListeners} is
 * explicitly configured with an empty list, thus disabling all default
 * listeners.
 * </p>
 *
 * @author Sam Brannen
 * @see StandardJUnit4FeaturesTests
 * @since 4.0
 */
@RunWith(InfraRunner.class)
@TestExecutionListeners({})
// Since this test class does not load an ApplicationContext,
// this test class simply is not supported for AOT processing.
@DisabledInAotMode
public class StandardJUnit4FeaturesInfraRunnerTests extends StandardJUnit4FeaturesTests {

  /* All tests are in the parent class... */

}
