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

package infra.app.test.config;

import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;

import infra.app.test.context.InfraTestContextBootstrapper;
import infra.core.ResolvableType;
import infra.core.annotation.MergedAnnotation;
import infra.core.annotation.MergedAnnotations;
import infra.core.annotation.MergedAnnotations.SearchStrategy;
import infra.lang.Assert;
import infra.test.context.TestContextAnnotationUtils;
import infra.test.context.TestContextBootstrapper;

/**
 * Base class for test slice {@link TestContextBootstrapper test context bootstrappers}.
 *
 * @param <T> the test slice annotation
 * @author Yanming Zhou
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public abstract class TestSliceTestContextBootstrapper<T extends Annotation> extends InfraTestContextBootstrapper {

  private final Class<T> annotationType;

  @SuppressWarnings("unchecked")
  protected TestSliceTestContextBootstrapper() {
    Class<T> annotationType = (Class<T>) ResolvableType.forClass(getClass())
            .as(TestSliceTestContextBootstrapper.class)
            .getGeneric(0)
            .resolve();
    Assert.notNull(annotationType, "'%s' doesn't contain type parameter of '%s'".formatted(getClass().getName(),
            TestSliceTestContextBootstrapper.class.getName()));
    this.annotationType = annotationType;
  }

  @Override
  protected String @Nullable [] getProperties(Class<?> testClass) {
    MergedAnnotation<T> annotation = MergedAnnotations.search(SearchStrategy.TYPE_HIERARCHY)
            .withEnclosingClasses(TestContextAnnotationUtils::searchEnclosingClass)
            .from(testClass)
            .get(this.annotationType);
    return annotation.isPresent() ? annotation.getStringArray("properties") : null;
  }

}
