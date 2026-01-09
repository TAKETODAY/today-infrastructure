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
