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

import infra.test.context.ContextConfiguration;
import infra.util.ResourceUtils;

/**
 * Extension of {@link JUnit4ClassRunnerAppCtxTests}, which verifies that
 * we can specify multiple resource locations for our application context, each
 * configured differently.
 * <p>
 * {@code MultipleResourcesInfraJUnit4ClassRunnerAppCtxTests} is also used
 * to verify support for the new {@code value} attribute alias for
 * {@code @ContextConfiguration}'s {@code locations} attribute.
 * </p>
 *
 * @author Sam Brannen
 * @see JUnit4ClassRunnerAppCtxTests
 * @since 4.0
 */
@ContextConfiguration({ MultipleResourcesJUnit4ClassRunnerAppCtxTests.CLASSPATH_RESOURCE_PATH,
        MultipleResourcesJUnit4ClassRunnerAppCtxTests.LOCAL_RESOURCE_PATH,
        MultipleResourcesJUnit4ClassRunnerAppCtxTests.ABSOLUTE_RESOURCE_PATH })
public class MultipleResourcesJUnit4ClassRunnerAppCtxTests extends JUnit4ClassRunnerAppCtxTests {

  public static final String CLASSPATH_RESOURCE_PATH = ResourceUtils.CLASSPATH_URL_PREFIX
          + "/infra/test/context/junit4/MultipleResourcesSpringJUnit4ClassRunnerAppCtxTests-context1.xml";
  public static final String LOCAL_RESOURCE_PATH = "MultipleResourcesSpringJUnit4ClassRunnerAppCtxTests-context2.xml";
  public static final String ABSOLUTE_RESOURCE_PATH = "/infra/test/context/junit4/MultipleResourcesSpringJUnit4ClassRunnerAppCtxTests-context3.xml";

  /* all tests are in the parent class. */
}
