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

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import infra.core.MethodIntrospector;
import infra.core.annotation.MergedAnnotations;
import infra.test.context.ContextConfigurationAttributes;
import infra.test.context.ContextCustomizerFactory;
import infra.test.context.DynamicPropertySource;
import infra.test.context.TestContextAnnotationUtils;

/**
 * {@link ContextCustomizerFactory} to support
 * {@link DynamicPropertySource @DynamicPropertySource} methods.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @see DynamicPropertiesContextCustomizer
 * @since 4.0
 */
class DynamicPropertiesContextCustomizerFactory implements ContextCustomizerFactory {

  @Override
  @Nullable
  public DynamicPropertiesContextCustomizer createContextCustomizer(Class<?> testClass,
          List<ContextConfigurationAttributes> configAttributes) {

    Set<Method> methods = new LinkedHashSet<>();
    findMethods(testClass, methods);
    if (methods.isEmpty()) {
      return null;
    }
    return new DynamicPropertiesContextCustomizer(methods);
  }

  private void findMethods(Class<?> testClass, Set<Method> methods) {
    // Beginning with Java 16, inner classes may contain static members.
    // We therefore need to search for @DynamicPropertySource methods in the
    // current class after searching enclosing classes so that a local
    // @DynamicPropertySource method can override properties registered in
    // an enclosing class.
    if (TestContextAnnotationUtils.searchEnclosingClass(testClass)) {
      findMethods(testClass.getEnclosingClass(), methods);
    }
    methods.addAll(MethodIntrospector.filterMethods(testClass, this::isAnnotated));
  }

  private boolean isAnnotated(Method method) {
    return MergedAnnotations.from(method).isPresent(DynamicPropertySource.class);
  }

}
