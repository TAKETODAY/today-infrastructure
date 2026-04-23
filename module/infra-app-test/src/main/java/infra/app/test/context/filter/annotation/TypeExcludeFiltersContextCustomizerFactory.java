/*
 * Copyright 2012-present the original author or authors.
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

package infra.app.test.context.filter.annotation;

import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import infra.aot.AotDetector;
import infra.context.annotation.config.TypeExcludeFilter;
import infra.test.context.ContextConfigurationAttributes;
import infra.test.context.ContextCustomizer;
import infra.test.context.ContextCustomizerFactory;
import infra.test.context.TestContextAnnotationUtils;

/**
 * {@link ContextCustomizerFactory} to support
 * {@link TypeExcludeFilters @TypeExcludeFilters}.
 *
 * @author Phillip Webb
 * @see TypeExcludeFiltersContextCustomizer
 */
class TypeExcludeFiltersContextCustomizerFactory implements ContextCustomizerFactory {

  @Override
  public @Nullable ContextCustomizer createContextCustomizer(Class<?> testClass, List<ContextConfigurationAttributes> configurationAttributes) {
    if (AotDetector.useGeneratedArtifacts()) {
      return null;
    }
    var descriptor = TestContextAnnotationUtils.findAnnotationDescriptor(testClass, TypeExcludeFilters.class);
    if (descriptor == null) {
      return null;
    }
    Class<?>[] filterClasses = descriptor.getAnnotation().value();
    return createContextCustomizer(descriptor.getRootDeclaringClass(), filterClasses);
  }

  @SuppressWarnings("unchecked")
  private ContextCustomizer createContextCustomizer(Class<?> testClass, Class<?>[] filterClasses) {
    return new TypeExcludeFiltersContextCustomizer(testClass,
            new LinkedHashSet<>(Arrays.asList((Class<? extends TypeExcludeFilter>[]) filterClasses)));
  }

}
