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

import infra.test.context.ContextConfiguration;

/**
 * Extension of {@link JUnit4ClassRunnerAppCtxTests}, which verifies that
 * we can specify an explicit, <em>relative path</em> location for our
 * application context.
 *
 * @author Sam Brannen
 * @see JUnit4ClassRunnerAppCtxTests
 * @see AbsolutePathJUnit4ClassRunnerAppCtxTests
 * @since 4.0
 */
@ContextConfiguration(locations = { "JUnit4ClassRunnerAppCtxTests-context.xml" })
public class RelativePathJUnit4ClassRunnerAppCtxTests extends JUnit4ClassRunnerAppCtxTests {
  /* all tests are in the parent class. */
}
