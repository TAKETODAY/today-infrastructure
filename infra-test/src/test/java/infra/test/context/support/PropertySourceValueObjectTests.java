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

package infra.test.context.support;

import org.junit.jupiter.api.Test;

import java.util.List;

import infra.core.annotation.MergedAnnotations;
import infra.core.io.PropertySourceDescriptor;
import infra.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class PropertySourceValueObjectTests {

  @Test
  void mergedSourcesEqualityIncludesDescriptorsAndProperties() {
    PropertySourceDescriptor descriptor = new PropertySourceDescriptor("classpath:test.properties");
    MergedTestPropertySources first = new MergedTestPropertySources(List.of(descriptor), new String[] { "a=b" });
    MergedTestPropertySources same = new MergedTestPropertySources(List.of(descriptor), new String[] { "a=b" });
    assertThat(first).isEqualTo(first).isEqualTo(same).isNotEqualTo(null).isNotEqualTo("other");
    assertThat(first.hashCode()).isEqualTo(same.hashCode());
    assertThat(first).isNotEqualTo(new MergedTestPropertySources(List.of(), new String[] { "a=b" }));
    assertThat(first).isNotEqualTo(new MergedTestPropertySources(List.of(descriptor), new String[] { "a=c" }));
    assertThat(first.getProperties()).containsExactly("a=b");
    assertThat(first.getPropertySourceDescriptors()).containsExactly(descriptor);
    assertThat(first.toString()).contains("descriptors", "properties");
    assertThat(MergedTestPropertySources.empty()).isSameAs(MergedTestPropertySources.empty());
    assertThatIllegalArgumentException().isThrownBy(() -> new MergedTestPropertySources(null, new String[0]));
    assertThatIllegalArgumentException().isThrownBy(() -> new MergedTestPropertySources(List.of(), null));
  }

  @Test
  void attributeEqualityIncludesResourcesPropertiesAndInheritance() {
    TestPropertySourceAttributes attributes = attributes(Properties.class);
    TestPropertySourceAttributes same = attributes(Properties.class);
    assertThat(attributes).isEqualTo(attributes).isEqualTo(same).isNotEqualTo(null).isNotEqualTo("other");
    assertThat(attributes.hashCode()).isEqualTo(same.hashCode());
    assertThat(attributes).isNotEqualTo(attributes(OtherProperties.class));
    assertThat(attributes).isNotEqualTo(attributes(NoInheritLocations.class));
    assertThat(attributes).isNotEqualTo(attributes(NoInheritProperties.class));
    assertThat(attributes).isNotEqualTo(attributes(Locations.class));
    assertThat(attributes.isEmpty()).isFalse();
    assertThat(attributes.getDeclaringClass()).isEqualTo(Properties.class);
    assertThat(attributes.toString()).contains("declaringClass", "inheritLocations");
    attributes.mergeWith(same);
    assertThat(attributes.getProperties()).containsExactly("a=b", "a=b");
    assertThatIllegalStateException().isThrownBy(() -> attributes.mergeWith(attributes(OtherProperties.class)))
            .withMessageContaining("different sources");
  }

  @Test
  void resourceDescriptorsRetainEncodingAndNoDefaultFileIsAnError() {
    TestPropertySourceAttributes locations = attributes(Locations.class);
    assertThat(locations.getPropertySourceDescriptors()).hasSize(1);
    assertThat(locations.getPropertySourceDescriptors().get(0).encoding()).isEqualTo("UTF-8");
    assertThat(locations.isEmpty()).isFalse();
    assertThatIllegalStateException().isThrownBy(() -> attributes(MissingDefault.class))
            .withMessageContaining("Could not detect default properties file");
  }

  private TestPropertySourceAttributes attributes(Class<?> type) {
    return new TestPropertySourceAttributes(MergedAnnotations.from(type).get(TestPropertySource.class));
  }

  @TestPropertySource(properties = "a=b")
  static class Properties { }

  @TestPropertySource(properties = "a=c")
  static class OtherProperties { }

  @TestPropertySource(properties = "a=b", inheritLocations = false)
  static class NoInheritLocations { }

  @TestPropertySource(properties = "a=b", inheritProperties = false)
  static class NoInheritProperties { }

  @TestPropertySource(locations = "test.properties", encoding = "UTF-8")
  static class Locations { }

  @TestPropertySource
  static class MissingDefault { }

}
