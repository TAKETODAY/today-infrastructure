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

package infra.test.context.hint;

import infra.aot.hint.RuntimeHints;
import infra.core.annotation.MergedAnnotations;
import infra.test.context.ActiveProfiles;
import infra.test.context.ActiveProfilesResolver;
import infra.test.context.TestContextAnnotationUtils;
import infra.test.context.aot.TestRuntimeHintsRegistrar;

import static infra.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS;
import static infra.core.annotation.MergedAnnotations.SearchStrategy.TYPE_HIERARCHY;

/**
 * {@link TestRuntimeHintsRegistrar} implementation that registers run-time hints
 * for standard functionality in the <em>Infra TestContext Framework</em>.
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
