/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.test.context.support;

import infra.context.ApplicationContext;
import infra.test.annotation.DirtiesContext;
import infra.test.context.TestContext;
import infra.test.context.TestExecutionListeners;

/**
 * {@code TestExecutionListener} which provides support for marking the
 * {@code ApplicationContext} associated with a test as <em>dirty</em> for
 * both test classes and test methods annotated with the
 * {@link DirtiesContext @DirtiesContext} annotation.
 *
 * <p>This listener supports test methods with the
 * {@linkplain DirtiesContext#methodMode method mode} set to
 * {@link DirtiesContext.MethodMode#AFTER_METHOD AFTER_METHOD} and test classes with the
 * {@linkplain DirtiesContext#classMode() class mode} set to
 * {@link DirtiesContext.ClassMode#AFTER_EACH_TEST_METHOD AFTER_EACH_TEST_METHOD} or
 * {@link DirtiesContext.ClassMode#AFTER_CLASS AFTER_CLASS}. For support for <em>BEFORE</em>
 * modes, see {@link DirtiesContextBeforeModesTestExecutionListener}.
 *
 * <p>When {@linkplain TestExecutionListeners#mergeMode merging}
 * {@code TestExecutionListeners} with the defaults, this listener will
 * automatically be ordered after the {@link DependencyInjectionTestExecutionListener};
 * otherwise, this listener must be manually configured to execute after the
 * {@code DependencyInjectionTestExecutionListener}.
 *
 * @author Sam Brannen
 * @see DirtiesContext
 * @see DirtiesContextBeforeModesTestExecutionListener
 * @since 4.0
 */
public class DirtiesContextTestExecutionListener extends AbstractDirtiesContextTestExecutionListener {

  /**
   * Returns {@code 3000}.
   */
  @Override
  public final int getOrder() {
    return 3000;
  }

  /**
   * If the current test method of the supplied {@linkplain TestContext test
   * context} is annotated with {@code @DirtiesContext} and the {@linkplain
   * DirtiesContext#methodMode() method mode} is set to {@link
   * DirtiesContext.MethodMode#AFTER_METHOD AFTER_METHOD}, or if the test class is
   * annotated with {@code @DirtiesContext} and the {@linkplain
   * DirtiesContext#classMode() class mode} is set to {@link
   * DirtiesContext.ClassMode#AFTER_EACH_TEST_METHOD AFTER_EACH_TEST_METHOD}, the
   * {@linkplain ApplicationContext application context} of the test context
   * will be {@linkplain TestContext#markApplicationContextDirty marked as dirty} and the
   * {@link DependencyInjectionTestExecutionListener#REINJECT_DEPENDENCIES_ATTRIBUTE
   * REINJECT_DEPENDENCIES_ATTRIBUTE} in the test context will be set to {@code true}.
   */
  @Override
  public void afterTestMethod(TestContext testContext) throws Exception {
    beforeOrAfterTestMethod(testContext, DirtiesContext.MethodMode.AFTER_METHOD, DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD);
  }

  /**
   * If the test class of the supplied {@linkplain TestContext test context}
   * is annotated with {@code @DirtiesContext} and the {@linkplain
   * DirtiesContext#classMode() class mode} is set to {@link
   * DirtiesContext.ClassMode#AFTER_CLASS AFTER_CLASS}, the {@linkplain ApplicationContext
   * application context} of the test context will be
   * {@linkplain TestContext#markApplicationContextDirty marked as dirty}, and the
   * {@link DependencyInjectionTestExecutionListener#REINJECT_DEPENDENCIES_ATTRIBUTE
   * REINJECT_DEPENDENCIES_ATTRIBUTE} in the test context will be set to
   * {@code true}.
   */
  @Override
  public void afterTestClass(TestContext testContext) throws Exception {
    beforeOrAfterTestClass(testContext, DirtiesContext.ClassMode.AFTER_CLASS);
  }

}
