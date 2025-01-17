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

import java.lang.annotation.Inherited;

import infra.test.context.ContextConfiguration;

/**
 * Extension of {@link JUnit4ClassRunnerAppCtxTests} which verifies that
 * the configuration of an application context and dependency injection of a
 * test instance function as expected within a class hierarchy, since
 * {@link ContextConfiguration configuration} is {@link Inherited inherited}.
 *
 * @author Sam Brannen
 * @see JUnit4ClassRunnerAppCtxTests
 * @since 4.0
 */
public class InheritedConfigJUnit4ClassRunnerAppCtxTests extends JUnit4ClassRunnerAppCtxTests {
  /* all tests are in the parent class. */
}
