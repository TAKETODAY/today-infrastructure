/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.test.context.junit4;

import infra.core.io.ResourceLoader;
import infra.test.context.ContextConfiguration;
import infra.util.ResourceUtils;

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
   * {@code &quot;classpath:/infra/test/context/junit4/JUnit4ClassRunnerAppCtxTests-context.xml&quot;}
   *
   * @see JUnit4ClassRunnerAppCtxTests#DEFAULT_CONTEXT_RESOURCE_PATH
   * @see ResourceUtils#CLASSPATH_URL_PREFIX
   */
  public static final String CLASSPATH_CONTEXT_RESOURCE_PATH = ResourceLoader.CLASSPATH_URL_PREFIX
          + DEFAULT_CONTEXT_RESOURCE_PATH;

  /* all tests are in the parent class. */
}
