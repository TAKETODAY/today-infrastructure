/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.test.context.junit4;

import cn.taketoday.test.context.ContextConfiguration;

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
@ContextConfiguration(locations = { "SpringJUnit4ClassRunnerAppCtxTests-context.xml" })
public class RelativePathJUnit4ClassRunnerAppCtxTests extends JUnit4ClassRunnerAppCtxTests {
  /* all tests are in the parent class. */
}
