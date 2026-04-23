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
import org.junit.jupiter.api.Test;

import java.util.Collections;

import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.config.TypeExcludeFilter;
import infra.core.type.classreading.MetadataReader;
import infra.core.type.classreading.MetadataReaderFactory;
import infra.core.type.classreading.SimpleMetadataReaderFactory;
import infra.test.context.ContextCustomizer;
import infra.test.context.MergedContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TypeExcludeFiltersContextCustomizerFactory}.
 *
 * @author Phillip Webb
 */
class TypeExcludeFiltersContextCustomizerFactoryTests {

  private final TypeExcludeFiltersContextCustomizerFactory factory = new TypeExcludeFiltersContextCustomizerFactory();

  private final MergedContextConfiguration mergedContextConfiguration = mock(MergedContextConfiguration.class);

  private final ConfigurableApplicationContext context = new AnnotationConfigApplicationContext();

  @Test
  void getContextCustomizerWhenHasNoAnnotationShouldReturnNull() {
    ContextCustomizer customizer = this.factory.createContextCustomizer(NoAnnotation.class,
            Collections.emptyList());
    assertThat(customizer).isNull();
  }

  @Test
  void getContextCustomizerWhenHasAnnotationShouldReturnCustomizer() {
    ContextCustomizer customizer = this.factory.createContextCustomizer(WithExcludeFilters.class,
            Collections.emptyList());
    assertThat(customizer).isNotNull();
  }

  @Test
  void getContextCustomizerWhenEnclosingClassHasAnnotationShouldReturnCustomizer() {
    ContextCustomizer customizer = this.factory.createContextCustomizer(EnclosingClass.WithEnclosingClassExcludeFilters.class,
            Collections.emptyList());
    assertThat(customizer).isNotNull();
  }

  @Test
  void hashCodeAndEquals() {
    ContextCustomizer customizer1 = this.factory.createContextCustomizer(WithExcludeFilters.class,
            Collections.emptyList());
    ContextCustomizer customizer2 = this.factory.createContextCustomizer(WithSameExcludeFilters.class,
            Collections.emptyList());
    ContextCustomizer customizer3 = this.factory.createContextCustomizer(WithDifferentExcludeFilters.class,
            Collections.emptyList());
    assertThat(customizer1).hasSameHashCodeAs(customizer2);
    assertThat(customizer1).isEqualTo(customizer1).isEqualTo(customizer2).isNotEqualTo(customizer3);
  }

  @Test
  void getContextCustomizerShouldAddExcludeFilters() throws Exception {
    ContextCustomizer customizer = this.factory.createContextCustomizer(WithExcludeFilters.class,
            Collections.emptyList());
    assertThat(customizer).isNotNull();
    customizer.customizeContext(this.context, this.mergedContextConfiguration);
    this.context.refresh();
    TypeExcludeFilter filter = this.context.getBean(TypeExcludeFilter.class);
    MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
    MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(NoAnnotation.class.getName());
    assertThat(filter.match(metadataReader, metadataReaderFactory)).isFalse();
    metadataReader = metadataReaderFactory.getMetadataReader(SimpleExclude.class.getName());
    assertThat(filter.match(metadataReader, metadataReaderFactory)).isTrue();
    metadataReader = metadataReaderFactory.getMetadataReader(TestClassAwareExclude.class.getName());
    assertThat(filter.match(metadataReader, metadataReaderFactory)).isTrue();
  }

  static class NoAnnotation {

  }

  @TypeExcludeFilters({ SimpleExclude.class, TestClassAwareExclude.class })
  static class WithExcludeFilters {

  }

  @TypeExcludeFilters({ SimpleExclude.class, TestClassAwareExclude.class })
  static class EnclosingClass {

    class WithEnclosingClassExcludeFilters {

    }

  }

  @TypeExcludeFilters({ TestClassAwareExclude.class, SimpleExclude.class })
  static class WithSameExcludeFilters {

  }

  @TypeExcludeFilters(SimpleExclude.class)
  static class WithDifferentExcludeFilters {

  }

  static class SimpleExclude extends TypeExcludeFilter {

    @Override
    public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) {
      return metadataReader.getClassMetadata().getClassName().equals(getClass().getName());
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      return obj != null && obj.getClass() == getClass();
    }

    @Override
    public int hashCode() {
      return SimpleExclude.class.hashCode();
    }

  }

  static class TestClassAwareExclude extends SimpleExclude {

    TestClassAwareExclude(Class<?> testClass) {
      assertThat(testClass).isNotNull();
    }

  }

}
