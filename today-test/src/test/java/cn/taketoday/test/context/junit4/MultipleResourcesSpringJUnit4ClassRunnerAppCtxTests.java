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
 * we can specify multiple resource locations for our application context, each
 * configured differently.
 * <p>
 * As of Spring 3.0,
 * {@code MultipleResourcesSpringJUnit4ClassRunnerAppCtxTests} is also used
 * to verify support for the new {@code value} attribute alias for
 * {@code @ContextConfiguration}'s {@code locations} attribute.
 * </p>
 *
 * @author Sam Brannen
 * @since 2.5
 * @see SpringJUnit4ClassRunnerAppCtxTests
 */
@ContextConfiguration( { MultipleResourcesSpringJUnit4ClassRunnerAppCtxTests.CLASSPATH_RESOURCE_PATH,
	MultipleResourcesSpringJUnit4ClassRunnerAppCtxTests.LOCAL_RESOURCE_PATH,
	MultipleResourcesSpringJUnit4ClassRunnerAppCtxTests.ABSOLUTE_RESOURCE_PATH })
public class MultipleResourcesSpringJUnit4ClassRunnerAppCtxTests extends SpringJUnit4ClassRunnerAppCtxTests {

	public static final String CLASSPATH_RESOURCE_PATH = ResourceUtils.CLASSPATH_URL_PREFIX
			+ "/org/springframework/test/context/junit4/MultipleResourcesSpringJUnit4ClassRunnerAppCtxTests-context1.xml";
	public static final String LOCAL_RESOURCE_PATH = "MultipleResourcesSpringJUnit4ClassRunnerAppCtxTests-context2.xml";
	public static final String ABSOLUTE_RESOURCE_PATH = "/org/springframework/test/context/junit4/MultipleResourcesSpringJUnit4ClassRunnerAppCtxTests-context3.xml";

	/* all tests are in the parent class. */
}
