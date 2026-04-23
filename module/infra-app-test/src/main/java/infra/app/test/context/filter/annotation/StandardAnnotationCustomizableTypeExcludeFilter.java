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

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import infra.context.annotation.ComponentScan.Filter;
import infra.core.ResolvableType;
import infra.core.annotation.MergedAnnotation;
import infra.core.annotation.MergedAnnotations;
import infra.core.annotation.MergedAnnotations.SearchStrategy;
import infra.lang.Assert;

/**
 * {@link AnnotationCustomizableTypeExcludeFilter} that can be used to any test annotation
 * that uses the standard {@code includeFilters}, {@code excludeFilters} and
 * {@code useDefaultFilters} attributes.
 *
 * @param <A> the annotation type
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public abstract class StandardAnnotationCustomizableTypeExcludeFilter<A extends Annotation>
        extends AnnotationCustomizableTypeExcludeFilter {

  private static final Filter[] NO_FILTERS = {};

  private static final String[] FILTER_TYPE_ATTRIBUTES;

  static {
    FilterType[] filterValues = FilterType.values();
    FILTER_TYPE_ATTRIBUTES = new String[filterValues.length];
    for (int i = 0; i < filterValues.length; i++) {
      FILTER_TYPE_ATTRIBUTES[i] = filterValues[i].name().toLowerCase(Locale.ROOT) + "Filters";
    }
  }

  private final MergedAnnotation<A> annotation;

  protected StandardAnnotationCustomizableTypeExcludeFilter(Class<?> testClass) {
    this.annotation = MergedAnnotations.from(testClass, SearchStrategy.INHERITED_ANNOTATIONS)
            .get(getAnnotationType());
  }

  protected final MergedAnnotation<A> getAnnotation() {
    return this.annotation;
  }

  @Override
  protected boolean hasAnnotation() {
    return this.annotation.isPresent();
  }

  @Override
  protected Filter[] getFilters(FilterType type) {
    return this.annotation.getValue(FILTER_TYPE_ATTRIBUTES[type.ordinal()], Filter[].class, NO_FILTERS);
  }

  @Override
  protected boolean isUseDefaultFilters() {
    return this.annotation.getValue("useDefaultFilters", Boolean.class, false);
  }

  @Override
  protected final Set<Class<?>> getDefaultIncludes() {
    Set<Class<?>> defaultIncludes = new HashSet<>();
    defaultIncludes.addAll(getKnownIncludes());
    defaultIncludes.addAll(TypeIncludes.load(this.annotation.getType(), getClass().getClassLoader()).getIncludes());
    return defaultIncludes;
  }

  protected Set<Class<?>> getKnownIncludes() {
    return Collections.emptySet();
  }

  @Override
  protected Set<Class<?>> getComponentIncludes() {
    return Collections.emptySet();
  }

  @SuppressWarnings("unchecked")
  protected Class<A> getAnnotationType() {
    ResolvableType type = ResolvableType.forClass(StandardAnnotationCustomizableTypeExcludeFilter.class, getClass());
    Class<A> generic = (Class<A>) type.resolveGeneric();
    Assert.state(generic != null, "'generic' is required");
    return generic;
  }

}
