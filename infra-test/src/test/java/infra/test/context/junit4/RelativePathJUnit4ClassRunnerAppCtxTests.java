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
