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

import org.junit.runner.RunWith;

import infra.test.context.TestExecutionListeners;
import infra.test.context.aot.DisabledInAotMode;

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
