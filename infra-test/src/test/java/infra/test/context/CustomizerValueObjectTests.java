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

package infra.test.context;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import infra.aot.AotDetector;
import infra.core.io.PropertySourceDescriptor;
import infra.core.io.ClassPathResource;
import infra.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mockStatic;

class CustomizerValueObjectTests {

  @Test
  void mergedConfigurationsDistinguishPropertiesAndParentsAndDetectResources() {
    var empty = configuration(List.of(), null, null);
    var descriptor = configuration(List.of(new PropertySourceDescriptor("classpath:other.properties")), null, null);
    var properties = configuration(List.of(), new String[] { "key=value" }, null);
    var parent = configuration(List.of(), null, empty);
    assertThat(empty).isNotEqualTo(descriptor).isNotEqualTo(properties).isNotEqualTo(parent);
    assertThat(parent).isNotEqualTo(empty);
    assertThatIllegalStateException().isThrownBy(parent::getParentApplicationContext)
            .withMessageContaining("CacheAwareContextLoaderDelegate");
    assertThat(empty.hasResources()).isFalse();
    assertThat(new MergedContextConfiguration(getClass(), new String[] { "context.xml" }, null, null, null).hasResources()).isTrue();
    assertThat(new MergedContextConfiguration(getClass(), null, new Class<?>[] { getClass() }, null, null).hasResources()).isTrue();
  }

  @Test
  void resourceFiltersHandleEmptyListsNullNamesAndNonmatchingPaths() {
    assertThat(FilteredClassLoader.ClassPathResourceFilter.of().test("other")).isFalse();
    assertThat(FilteredClassLoader.ClassPathResourceFilter.of(new ClassPathResource("hidden")).test("other")).isFalse();
    var unnamed = org.mockito.Mockito.mock(ClassPathResource.class);
    assertThat(FilteredClassLoader.ClassPathResourceFilter.of(unnamed).test("other")).isFalse();
  }

  @Test
  void propertyMappingsSupportTrailingDotsAndReportMultipleComposedAnnotations() {
    assertThat(new AnnotationsPropertySource(Mapped.class).getProperty("test.value")).isEqualTo("mapped");
    var processor = new PropertyMappingContextCustomizer.PropertyMappingCheckBeanPostProcessor();
    assertThatIllegalStateException().isThrownBy(() -> processor.postProcessBeforeInitialization(new MappedComponent(), "mapped"))
            .withMessageContaining("annotations ").withMessageContaining(", ");
  }

  private MergedContextConfiguration configuration(List<PropertySourceDescriptor> descriptors, String[] properties,
          MergedContextConfiguration parent) {
    return new MergedContextConfiguration(getClass(), null, null, null, null, descriptors,
            properties, Set.of(), null, null, parent);
  }

  @Test
  void propertyMappingEqualityDistinguishesNullTypesAndSourceContents() {
    var source = new AnnotationsPropertySource(getClass());
    var customizer = new PropertyMappingContextCustomizer(source);
    assertThat(customizer).isEqualTo(new PropertyMappingContextCustomizer(new AnnotationsPropertySource(getClass())))
            .hasSameHashCodeAs(new PropertyMappingContextCustomizer(source))
            .isNotEqualTo(null).isNotEqualTo(new Object());
    assertThat(source).isEqualTo(source).isNotEqualTo(null).isNotEqualTo(new Object());
  }

  @Test
  void autoConfigurationCustomizerIsAbsentWhenEnabledOrInAotRuntime() {
    var factory = new OverrideAutoConfigurationContextCustomizerFactory();
    assertThat(factory.createContextCustomizer(Enabled.class, List.of())).isNull();
    var disabled = factory.createContextCustomizer(Disabled.class, List.of());
    assertThat(disabled).isNotNull().isNotEqualTo(null).isNotEqualTo(new Object())
            .isEqualTo(factory.createContextCustomizer(Disabled.class, List.of()))
            .hasSameHashCodeAs(factory.createContextCustomizer(Disabled.class, List.of()));
    try (var detector = mockStatic(AotDetector.class)) {
      detector.when(AotDetector::useGeneratedArtifacts).thenReturn(true);
      assertThat(factory.createContextCustomizer(Disabled.class, List.of())).isNull();
    }
  }

  @OverrideAutoConfiguration(enabled = true)
  static class Enabled { }

  @OverrideAutoConfiguration(enabled = false)
  static class Disabled { }

  @Retention(RetentionPolicy.RUNTIME)
  @PropertyMapping("test.")
  @interface Mapping {
    String value() default "mapped";
  }

  @Retention(RetentionPolicy.RUNTIME)
  @PropertyMapping("other")
  @interface OtherMapping { }

  @Mapping
  static class Mapped { }

  @Component
  @Mapping
  @OtherMapping
  static class MappedComponent { }

}
