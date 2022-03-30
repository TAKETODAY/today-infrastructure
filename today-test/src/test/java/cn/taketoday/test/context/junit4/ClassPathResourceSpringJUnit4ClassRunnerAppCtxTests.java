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
import cn.taketoday.util.ResourceUtils;

/**
 * Extension of {@link SpringJUnit4ClassRunnerAppCtxTests}, which verifies that
 * we can specify an explicit, <em>classpath</em> location for our application
 * context.
 *
 * @author Sam Brannen
 * @since 2.5
 * @see SpringJUnit4ClassRunnerAppCtxTests
 * @see #CLASSPATH_CONTEXT_RESOURCE_PATH
 * @see AbsolutePathSpringJUnit4ClassRunnerAppCtxTests
 * @see RelativePathSpringJUnit4ClassRunnerAppCtxTests
 */
@ContextConfiguration(locations = { ClassPathResourceSpringJUnit4ClassRunnerAppCtxTests.CLASSPATH_CONTEXT_RESOURCE_PATH })
public class ClassPathResourceSpringJUnit4ClassRunnerAppCtxTests extends SpringJUnit4ClassRunnerAppCtxTests {

	/**
	 * Classpath-based resource path for the application context configuration
	 * for {@link SpringJUnit4ClassRunnerAppCtxTests}:
	 * {@code &quot;classpath:/org/springframework/test/context/junit4/SpringJUnit4ClassRunnerAppCtxTests-context.xml&quot;}
	 *
	 * @see SpringJUnit4ClassRunnerAppCtxTests#DEFAULT_CONTEXT_RESOURCE_PATH
	 * @see ResourceUtils#CLASSPATH_URL_PREFIX
	 */
	public static final String CLASSPATH_CONTEXT_RESOURCE_PATH = ResourceUtils.CLASSPATH_URL_PREFIX
			+ SpringJUnit4ClassRunnerAppCtxTests.DEFAULT_CONTEXT_RESOURCE_PATH;

	/* all tests are in the parent class. */
}
