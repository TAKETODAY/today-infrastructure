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

package infra.test.context.junit4.annotation;

import infra.test.context.ContextConfiguration;
import infra.test.context.junit4.JUnit4ClassRunnerAppCtxTests;

/**
 * Integration tests that verify support for configuration classes in
 * the TestContext Framework.
 *
 * <p>Furthermore, by extending {@link JUnit4ClassRunnerAppCtxTests},
 * this class also verifies support for several basic features of the
 * TestContext Framework. See JavaDoc in
 * {@code SpringJUnit4ClassRunnerAppCtxTests} for details.
 *
 * <p>Configuration will be loaded from {@link PojoAndStringConfig}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@ContextConfiguration(classes = PojoAndStringConfig.class, inheritLocations = false)
public class AnnotationConfigJUnit4ClassRunnerAppCtxTests extends JUnit4ClassRunnerAppCtxTests {
  /* all tests are in the parent class. */
}
