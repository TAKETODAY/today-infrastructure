/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.test.context.hint;

import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.test.context.ActiveProfiles;
import cn.taketoday.test.context.ActiveProfilesResolver;
import cn.taketoday.test.context.TestContextAnnotationUtils;
import cn.taketoday.test.context.aot.TestRuntimeHintsRegistrar;

import static cn.taketoday.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS;
import static cn.taketoday.core.annotation.MergedAnnotations.SearchStrategy.TYPE_HIERARCHY;

/**
 * {@link TestRuntimeHintsRegistrar} implementation that registers run-time hints
 * for standard functionality in the <em>Spring TestContext Framework</em>.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see TestContextRuntimeHints
 * @since 4.0
 */
class StandardTestRuntimeHints implements TestRuntimeHintsRegistrar {

  @Override
  public void registerHints(RuntimeHints runtimeHints, Class<?> testClass, ClassLoader classLoader) {
    // @ActiveProfiles(resolver = ...)
    MergedAnnotations.search(TYPE_HIERARCHY)
            .withEnclosingClasses(TestContextAnnotationUtils::searchEnclosingClass)
            .from(testClass)
            .stream(ActiveProfiles.class)
            .map(annotation -> annotation.<ActiveProfilesResolver>getClass("resolver"))
            .filter(type -> type != ActiveProfilesResolver.class)
            .forEach(resolverClass -> registerDeclaredConstructors(resolverClass, runtimeHints));
  }

  private void registerDeclaredConstructors(Class<?> type, RuntimeHints runtimeHints) {
    runtimeHints.reflection().registerType(type, INVOKE_DECLARED_CONSTRUCTORS);
  }

}
