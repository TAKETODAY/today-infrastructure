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

package infra.test.context.filter.annotation;

import org.junit.jupiter.api.Test;

import java.util.Set;

import infra.context.annotation.ComponentScan.Filter;
import infra.context.annotation.FilterType;
import infra.context.support.GenericApplicationContext;
import infra.core.type.classreading.SimpleMetadataReaderFactory;
import infra.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class AnnotationCustomizableTypeExcludeFilterTests {

  @Test
  void emptyTypeExcludeCustomizersDoNotRegisterBeansAndHaveValueEquality() {
    var customizer = new TypeExcludeFiltersContextCustomizer(getClass(), Set.of());
    assertThat(customizer).isEqualTo(new TypeExcludeFiltersContextCustomizer(getClass(), Set.of()))
            .isNotEqualTo(null).isNotEqualTo(new Object());
    try (var context = new GenericApplicationContext()) {
      customizer.customizeContext(context, null);
      assertThat(context.getBeanFactory().getSingletonCount()).isZero();
    }
  }

  @Test
  void invalidFilterKindsAreRejectedForClassesAndPatterns() {
    for (Class<?> type : new Class<?>[] { UnsupportedClassFilter.class, UnsupportedPatternFilter.class }) {
      var filters = type.getAnnotation(Include.class).value();
      assertThatIllegalArgumentException().isThrownBy(() -> new FilterAnnotations(getClass().getClassLoader(), filters));
    }
  }

  @Test
  void defaultAndComponentIncludesMatchAnnotationsAndAssignableTypes() throws Exception {
    ConfigurableFilter filter = new ConfigurableFilter();
    SimpleMetadataReaderFactory factory = new SimpleMetadataReaderFactory();
    var metadata = factory.getMetadataReader(Candidate.class.getName());
    filter.defaults = Set.of(Component.class);
    assertThat(filter.match(metadata, factory)).isFalse();
    filter.defaults = Set.of(Runnable.class);
    assertThat(filter.match(metadata, factory)).isFalse();
    filter.defaults = Set.of(CharSequence.class);
    assertThat(filter.match(metadata, factory)).isTrue();
    filter.components = Set.of(Runnable.class);
    assertThat(filter.match(metadata, factory)).isFalse();
    filter.components = Set.of(CharSequence.class);
    assertThat(filter.match(metadata, factory)).isTrue();
    filter.useDefaults = false;
    assertThat(filter.match(metadata, factory)).isTrue();
    filter.present = false;
    assertThat(filter.match(metadata, factory)).isFalse();
  }

  @Test
  void explicitIncludeAndExcludeFiltersTakePrecedence() throws Exception {
    ConfigurableFilter filter = new ConfigurableFilter();
    SimpleMetadataReaderFactory factory = new SimpleMetadataReaderFactory();
    var metadata = factory.getMetadataReader(Candidate.class.getName());
    filter.useDefaults = false;
    filter.includes = Filters.class.getAnnotation(Include.class).value();
    assertThat(filter.match(metadata, factory)).isFalse();
    filter.excludes = filter.includes;
    assertThat(filter.match(metadata, factory)).isTrue();
  }

  @Test
  void equalityAndHashCodeIncludeAllConfiguration() {
    ConfigurableFilter first = new ConfigurableFilter();
    ConfigurableFilter second = new ConfigurableFilter();
    assertThat(first).isEqualTo(first).isEqualTo(second).isNotEqualTo(null).isNotEqualTo("other");
    assertThat(first.hashCode()).isEqualTo(second.hashCode());
    second.present = false;
    assertThat(first).isNotEqualTo(second);
    second.present = true;
    second.useDefaults = false;
    assertThat(first).isNotEqualTo(second);
    second.useDefaults = true;
    second.defaults = Set.of(Component.class);
    assertThat(first).isNotEqualTo(second);
    second.defaults = Set.of();
    second.components = Set.of(Runnable.class);
    assertThat(first).isNotEqualTo(second);
    second.components = Set.of();
    second.includes = Filters.class.getAnnotation(Include.class).value();
    assertThat(first).isNotEqualTo(second);
  }

  @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
  @interface Include {
    Filter[] value();
  }

  @Include(@Filter(classes = Component.class))
  static class Filters { }

  @Include(@Filter(type = FilterType.REGEX, classes = String.class))
  static class UnsupportedClassFilter { }

  @Include(@Filter(type = FilterType.ASSIGNABLE_TYPE, pattern = "value"))
  static class UnsupportedPatternFilter { }

  @Component
  static class Candidate implements Runnable {
    @Override
    public void run() { }
  }

  static class ConfigurableFilter extends AnnotationCustomizableTypeExcludeFilter {
    boolean present = true;
    boolean useDefaults = true;
    Set<Class<?>> defaults = Set.of();
    Set<Class<?>> components = Set.of();
    Filter[] includes = new Filter[0];
    Filter[] excludes = new Filter[0];

    ConfigurableFilter() {
      setBeanClassLoader(getClass().getClassLoader());
    }

    @Override
    protected boolean hasAnnotation() {
      return present;
    }

    @Override
    protected Filter[] getFilters(FilterType type) {
      return type == FilterType.INCLUDE ? includes : excludes;
    }

    @Override
    protected boolean isUseDefaultFilters() {
      return useDefaults;
    }

    @Override
    protected Set<Class<?>> getDefaultIncludes() {
      return defaults;
    }

    @Override
    protected Set<Class<?>> getComponentIncludes() {
      return components;
    }
  }

}
