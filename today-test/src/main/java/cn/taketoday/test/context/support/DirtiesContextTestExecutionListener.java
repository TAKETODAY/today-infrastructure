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

package cn.taketoday.test.context.support;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.test.annotation.DirtiesContext;
import cn.taketoday.test.annotation.DirtiesContext.ClassMode;
import cn.taketoday.test.annotation.DirtiesContext.MethodMode;
import cn.taketoday.test.context.TestContext;
import cn.taketoday.test.context.TestExecutionListeners;

import static cn.taketoday.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS;
import static cn.taketoday.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;
import static cn.taketoday.test.annotation.DirtiesContext.MethodMode.AFTER_METHOD;

/**
 * {@code TestExecutionListener} which provides support for marking the
 * {@code ApplicationContext} associated with a test as <em>dirty</em> for
 * both test classes and test methods annotated with the
 * {@link DirtiesContext @DirtiesContext} annotation.
 *
 * <p>This listener supports test methods with the
 * {@linkplain DirtiesContext#methodMode method mode} set to
 * {@link MethodMode#AFTER_METHOD AFTER_METHOD} and test classes with the
 * {@linkplain DirtiesContext#classMode() class mode} set to
 * {@link ClassMode#AFTER_EACH_TEST_METHOD AFTER_EACH_TEST_METHOD} or
 * {@link ClassMode#AFTER_CLASS AFTER_CLASS}. For support for <em>BEFORE</em>
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
   * MethodMode#AFTER_METHOD AFTER_METHOD}, or if the test class is
   * annotated with {@code @DirtiesContext} and the {@linkplain
   * DirtiesContext#classMode() class mode} is set to {@link
   * ClassMode#AFTER_EACH_TEST_METHOD AFTER_EACH_TEST_METHOD}, the
   * {@linkplain ApplicationContext application context} of the test context
   * will be {@linkplain TestContext#markApplicationContextDirty marked as dirty} and the
   * {@link DependencyInjectionTestExecutionListener#REINJECT_DEPENDENCIES_ATTRIBUTE
   * REINJECT_DEPENDENCIES_ATTRIBUTE} in the test context will be set to {@code true}.
   */
  @Override
  public void afterTestMethod(TestContext testContext) throws Exception {
    beforeOrAfterTestMethod(testContext, AFTER_METHOD, AFTER_EACH_TEST_METHOD);
  }

  /**
   * If the test class of the supplied {@linkplain TestContext test context}
   * is annotated with {@code @DirtiesContext} and the {@linkplain
   * DirtiesContext#classMode() class mode} is set to {@link
   * ClassMode#AFTER_CLASS AFTER_CLASS}, the {@linkplain ApplicationContext
   * application context} of the test context will be
   * {@linkplain TestContext#markApplicationContextDirty marked as dirty}, and the
   * {@link DependencyInjectionTestExecutionListener#REINJECT_DEPENDENCIES_ATTRIBUTE
   * REINJECT_DEPENDENCIES_ATTRIBUTE} in the test context will be set to
   * {@code true}.
   */
  @Override
  public void afterTestClass(TestContext testContext) throws Exception {
    beforeOrAfterTestClass(testContext, AFTER_CLASS);
  }

}
