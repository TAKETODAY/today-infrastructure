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

import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.util.ResourceUtils;

/**
 * Extension of {@link JUnit4ClassRunnerAppCtxTests}, which verifies that
 * we can specify an explicit, <em>classpath</em> location for our application
 * context.
 *
 * @author Sam Brannen
 * @see JUnit4ClassRunnerAppCtxTests
 * @see #CLASSPATH_CONTEXT_RESOURCE_PATH
 * @see AbsolutePathJUnit4ClassRunnerAppCtxTests
 * @see RelativePathJUnit4ClassRunnerAppCtxTests
 * @since 4.0
 */
@ContextConfiguration(locations = { ClassPathResourceJUnit4ClassRunnerAppCtxTests.CLASSPATH_CONTEXT_RESOURCE_PATH }, inheritLocations = false)
public class ClassPathResourceJUnit4ClassRunnerAppCtxTests extends JUnit4ClassRunnerAppCtxTests {

  /**
   * Classpath-based resource path for the application context configuration
   * for {@link JUnit4ClassRunnerAppCtxTests}:
   * {@code &quot;classpath:/cn/taketoday/test/context/junit4/JUnit4ClassRunnerAppCtxTests-context.xml&quot;}
   *
   * @see JUnit4ClassRunnerAppCtxTests#DEFAULT_CONTEXT_RESOURCE_PATH
   * @see ResourceUtils#CLASSPATH_URL_PREFIX
   */
  public static final String CLASSPATH_CONTEXT_RESOURCE_PATH = ResourceLoader.CLASSPATH_URL_PREFIX
          + JUnit4ClassRunnerAppCtxTests.DEFAULT_CONTEXT_RESOURCE_PATH;

  /* all tests are in the parent class. */
}
