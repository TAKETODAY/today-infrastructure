/*
 * Copyright 2017 - 2023 the original author or authors.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.test.context.support;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.stream.Stream;

import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.core.annotation.AnnotationConfigurationException;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.MapPropertySource;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.core.env.PropertySources;
import cn.taketoday.core.io.ByteArrayResource;
import cn.taketoday.core.io.PropertySourceDescriptor;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.mock.env.MockEnvironment;
import cn.taketoday.mock.env.MockPropertySource;
import cn.taketoday.test.context.TestPropertySource;

import static cn.taketoday.test.context.support.TestPropertySourceUtils.INLINED_PROPERTIES_PROPERTY_SOURCE_NAME;
import static cn.taketoday.test.context.support.TestPropertySourceUtils.addInlinedPropertiesToEnvironment;
import static cn.taketoday.test.context.support.TestPropertySourceUtils.addPropertiesFilesToEnvironment;
import static cn.taketoday.test.context.support.TestPropertySourceUtils.buildMergedTestPropertySources;
import static cn.taketoday.test.context.support.TestPropertySourceUtils.convertInlinedPropertiesToMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link TestPropertySourceUtils}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
class TestPropertySourceUtilsTests {

  private static final String[] EMPTY_STRING_ARRAY = new String[0];

  private static final String[] KEY_VALUE_PAIR = { "key = value" };

  private static final String[] FOO_LOCATIONS = { "classpath:/foo.properties" };

  @Test
  void emptyAnnotation() {
    assertThatIllegalStateException()
            .isThrownBy(() -> buildMergedTestPropertySources(EmptyPropertySources.class))
            .withMessageContainingAll(
                    "Could not detect default properties file for test class",
                    "class path resource",
                    "does not exist",
                    "EmptyPropertySources.properties");
  }

  @Test
  void extendedEmptyAnnotation() {
    assertThatIllegalStateException()
            .isThrownBy(() -> buildMergedTestPropertySources(ExtendedEmptyPropertySources.class))
            .withMessageStartingWith("Could not detect default properties file for test")
            .withMessageContaining("class path resource")
            .withMessageContaining("does not exist")
            .withMessageContaining("ExtendedEmptyPropertySources.properties");
  }

  @Test
  void repeatedTestPropertySourcesWithConflictingInheritLocationsFlags() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> buildMergedTestPropertySources(RepeatedPropertySourcesWithConflictingInheritLocationsFlags.class))
            .withMessage("@TestPropertySource on RepeatedPropertySourcesWithConflictingInheritLocationsFlags and " +
                    "@InheritLocationsFalseTestProperty on RepeatedPropertySourcesWithConflictingInheritLocationsFlags " +
                    "must declare the same value for 'inheritLocations' as other directly present or meta-present @TestPropertySource annotations");
  }

  @Test
  void repeatedTestPropertySourcesWithConflictingInheritPropertiesFlags() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> buildMergedTestPropertySources(RepeatedPropertySourcesWithConflictingInheritPropertiesFlags.class))
            .withMessage("@TestPropertySource on RepeatedPropertySourcesWithConflictingInheritPropertiesFlags and " +
                    "@InheritPropertiesFalseTestProperty on RepeatedPropertySourcesWithConflictingInheritPropertiesFlags " +
                    "must declare the same value for 'inheritProperties' as other directly present or meta-present @TestPropertySource annotations");
  }

  @Test
  void value() {
    assertMergedTestPropertySources(ValuePropertySources.class, asArray("classpath:/value.xml"),
            EMPTY_STRING_ARRAY);
  }

  @Test
  void locationsAndValueAttributes() {
    assertThatExceptionOfType(AnnotationConfigurationException.class)
            .isThrownBy(() -> buildMergedTestPropertySources(LocationsAndValuePropertySources.class));
  }

  @Test
  void locationsAndProperties() {
    assertMergedTestPropertySources(LocationsAndPropertiesPropertySources.class,
            asArray("classpath:/foo1.xml", "classpath:/foo2.xml"), asArray("k1a=v1a", "k1b: v1b"));
  }

  @Test
  void inheritedLocationsAndProperties() {
    assertMergedTestPropertySources(InheritedPropertySources.class,
            asArray("classpath:/foo1.xml", "classpath:/foo2.xml"), asArray("k1a=v1a", "k1b: v1b"));
  }

  @Test
  void locationsAndPropertiesDuplicatedLocally() {
    assertMergedTestPropertySources(LocallyDuplicatedLocationsAndProperties.class,
            asArray("classpath:/foo1.xml", "classpath:/foo2.xml"), asArray("k1a=v1a", "k1b: v1b"));
  }

  @Test
  void locationsAndPropertiesDuplicatedOnSuperclass() {
    assertMergedTestPropertySources(DuplicatedLocationsAndPropertiesPropertySources.class,
            asArray("classpath:/foo1.xml", "classpath:/foo2.xml"), asArray("k1a=v1a", "k1b: v1b"));
  }

  @Test
  void locationsAndPropertiesDuplicatedOnEnclosingClass() {
    assertMergedTestPropertySources(LocationsAndPropertiesPropertySources.Nested.class,
            asArray("classpath:/foo1.xml", "classpath:/foo2.xml"), asArray("k1a=v1a", "k1b: v1b"));
  }

  @Test
  void extendedLocationsAndProperties() {
    assertMergedTestPropertySources(ExtendedPropertySources.class,
            asArray("classpath:/foo1.xml", "classpath:/foo2.xml", "classpath:/bar1.xml", "classpath:/bar2.xml"),
            asArray("k1a=v1a", "k1b: v1b", "k2a v2a", "k2b: v2b"));
  }

  @Test
  void overriddenLocations() {
    assertMergedTestPropertySources(OverriddenLocationsPropertySources.class,
            asArray("classpath:/baz.properties"), asArray("k1a=v1a", "k1b: v1b", "key = value"));
  }

  @Test
  void overriddenProperties() {
    assertMergedTestPropertySources(OverriddenPropertiesPropertySources.class,
            asArray("classpath:/foo1.xml", "classpath:/foo2.xml", "classpath:/baz.properties"), KEY_VALUE_PAIR);
  }

  @Test
  void overriddenLocationsAndProperties() {
    assertMergedTestPropertySources(OverriddenLocationsAndPropertiesPropertySources.class,
            asArray("classpath:/baz.properties"), KEY_VALUE_PAIR);
  }

  @Test
  void addPropertiesFilesToEnvironmentWithNullContext() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> addPropertiesFilesToEnvironment((ConfigurableApplicationContext) null, FOO_LOCATIONS))
            .withMessageContaining("'context' is required");
  }

  @Test
  void addPropertiesFilesToEnvironmentWithContextAndNullLocations() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> addPropertiesFilesToEnvironment(mock(ConfigurableApplicationContext.class), (String[]) null))
            .withMessageContaining("'locations' is required");
  }

  @Test
  void addPropertiesFilesToEnvironmentWithNullEnvironment() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> addPropertiesFilesToEnvironment((ConfigurableEnvironment) null, mock(), FOO_LOCATIONS))
            .withMessageContaining("'environment' is required");
  }

  @Test
  void addPropertiesFilesToEnvironmentWithEnvironmentLocationsAndNullResourceLoader() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> addPropertiesFilesToEnvironment(new MockEnvironment(), null, FOO_LOCATIONS))
            .withMessageContaining("'resourceLoader' is required");
  }

  @Test
  void addPropertiesFilesToEnvironmentWithEnvironmentAndNullLocations() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> addPropertiesFilesToEnvironment(new MockEnvironment(), mock(), (String[]) null))
            .withMessageContaining("'locations' is required");
  }

  @Test
  void addPropertiesFilesToEnvironmentWithSinglePropertyFromVirtualFile() {
    ConfigurableEnvironment environment = new MockEnvironment();

    PropertySources propertySources = environment.getPropertySources();
    propertySources.remove(MockPropertySource.MOCK_PROPERTIES_PROPERTY_SOURCE_NAME);
    assertThat(propertySources).isEmpty();

    String pair = "key = value";
    ByteArrayResource resource = new ByteArrayResource(pair.getBytes(), "from inlined property: " + pair);
    ResourceLoader resourceLoader = mock();
    given(resourceLoader.getResource(anyString())).willReturn(resource);

    addPropertiesFilesToEnvironment(environment, resourceLoader, FOO_LOCATIONS);
    assertThat(propertySources).hasSize(1);
    assertThat(environment.getProperty("key")).isEqualTo("value");
  }

  @Test
  void addInlinedPropertiesToEnvironmentWithNullContext() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> addInlinedPropertiesToEnvironment((ConfigurableApplicationContext) null, KEY_VALUE_PAIR))
            .withMessageContaining("'context' is required");
  }

  @Test
  void addInlinedPropertiesToEnvironmentWithContextAndNullInlinedProperties() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> addInlinedPropertiesToEnvironment(mock(ConfigurableApplicationContext.class), (String[]) null))
            .withMessageContaining("'inlinedProperties' is required");
  }

  @Test
  void addInlinedPropertiesToEnvironmentWithNullEnvironment() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> addInlinedPropertiesToEnvironment((ConfigurableEnvironment) null, KEY_VALUE_PAIR))
            .withMessageContaining("'environment' is required");
  }

  @Test
  void addInlinedPropertiesToEnvironmentWithEnvironmentAndNullInlinedProperties() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> addInlinedPropertiesToEnvironment(new MockEnvironment(), (String[]) null))
            .withMessageContaining("'inlinedProperties' is required");
  }

  @Test
  void addInlinedPropertiesToEnvironmentWithMalformedUnicodeInValue() {
    String properties = "key = \\uZZZZ";
    assertThatIllegalStateException()
            .isThrownBy(() -> addInlinedPropertiesToEnvironment(new MockEnvironment(), properties))
            .withMessageContaining("Failed to load test environment properties from [%s]", properties);
  }

  @Test
  void addInlinedPropertiesToEnvironmentWithMultipleKeyValuePairsInSingleInlinedProperty() {
    ConfigurableEnvironment environment = new MockEnvironment();
    PropertySources propertySources = environment.getPropertySources();
    propertySources.remove(MockPropertySource.MOCK_PROPERTIES_PROPERTY_SOURCE_NAME);
    assertThat(propertySources).isEmpty();
    addInlinedPropertiesToEnvironment(environment, """
            a=b
            x=y
            """);
    assertThat(propertySources).hasSize(1);
    PropertySource<?> propertySource = propertySources.get(INLINED_PROPERTIES_PROPERTY_SOURCE_NAME);
    assertThat(propertySource).isInstanceOf(MapPropertySource.class);
    assertThat(((MapPropertySource) propertySource).getSource()).containsExactly(entry("a", "b"), entry("x", "y"));
  }

  @Test
  void addInlinedPropertiesToEnvironmentWithEmptyProperty() {
    ConfigurableEnvironment environment = new MockEnvironment();
    PropertySources propertySources = environment.getPropertySources();
    propertySources.remove(MockPropertySource.MOCK_PROPERTIES_PROPERTY_SOURCE_NAME);
    assertThat(propertySources).isEmpty();
    addInlinedPropertiesToEnvironment(environment, "  ");
    assertThat(propertySources).hasSize(1);
    PropertySource<?> propertySource = propertySources.get(INLINED_PROPERTIES_PROPERTY_SOURCE_NAME);
    assertThat(propertySource).isInstanceOf(MapPropertySource.class);
    assertThat(((MapPropertySource) propertySource).getSource()).isEmpty();
  }

  @Test
  void convertInlinedPropertiesToMapWithNullInlinedProperties() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> convertInlinedPropertiesToMap((String[]) null))
            .withMessageContaining("'inlinedProperties' is required");
  }

  private static void assertMergedTestPropertySources(Class<?> testClass, String[] expectedLocations,
          String[] expectedProperties) {

    MergedTestPropertySources mergedPropertySources = buildMergedTestPropertySources(testClass);
    SoftAssertions.assertSoftly(softly -> {
      softly.assertThat(mergedPropertySources).isNotNull();
      Stream<String> locations = mergedPropertySources.getPropertySourceDescriptors().stream()
              .map(PropertySourceDescriptor::locations).flatMap(List::stream);
      softly.assertThat(locations).containsExactly(expectedLocations);
      softly.assertThat(mergedPropertySources.getProperties()).isEqualTo(expectedProperties);
    });
  }

  @SafeVarargs
  private static <T> T[] asArray(T... arr) {
    return arr;
  }

  @Retention(RetentionPolicy.RUNTIME)
  @TestPropertySource(locations = "foo.properties", inheritLocations = false)
  @interface InheritLocationsFalseTestProperty {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @TestPropertySource(properties = "a = b", inheritProperties = false)
  @interface InheritPropertiesFalseTestProperty {
  }

  @TestPropertySource
  static class EmptyPropertySources {
  }

  @TestPropertySource
  static class ExtendedEmptyPropertySources extends EmptyPropertySources {
  }

  @InheritLocationsFalseTestProperty
  @TestPropertySource(locations = "bar.properties", inheritLocations = true)
  static class RepeatedPropertySourcesWithConflictingInheritLocationsFlags {
  }

  @TestPropertySource(properties = "x = y", inheritProperties = true)
  @InheritPropertiesFalseTestProperty
  static class RepeatedPropertySourcesWithConflictingInheritPropertiesFlags {
  }

  @TestPropertySource(locations = "/foo", value = "/bar")
  static class LocationsAndValuePropertySources {
  }

  @TestPropertySource("/value.xml")
  static class ValuePropertySources {
  }

  @TestPropertySource(locations = { "/foo1.xml", "/foo2.xml" }, properties = { "k1a=v1a", "k1b: v1b" })
  static class LocationsAndPropertiesPropertySources {

    @TestPropertySource(locations = { "/foo1.xml", "/foo2.xml" }, properties = { "k1a=v1a", "k1b: v1b" })
    class Nested {
    }
  }

  static class InheritedPropertySources extends LocationsAndPropertiesPropertySources {
  }

  @TestPropertySource(locations = { "/bar1.xml", "/bar2.xml" }, properties = { "k2a v2a", "k2b: v2b" })
  static class ExtendedPropertySources extends LocationsAndPropertiesPropertySources {
  }

  @TestPropertySource(locations = "/baz.properties", properties = "key = value", inheritLocations = false)
  static class OverriddenLocationsPropertySources extends LocationsAndPropertiesPropertySources {
  }

  @TestPropertySource(locations = "/baz.properties", properties = "key = value", inheritProperties = false)
  static class OverriddenPropertiesPropertySources extends LocationsAndPropertiesPropertySources {
  }

  @TestPropertySource(locations = "/baz.properties", properties = "key = value", inheritLocations = false, inheritProperties = false)
  static class OverriddenLocationsAndPropertiesPropertySources extends LocationsAndPropertiesPropertySources {
  }

  @TestPropertySource(locations = { "/foo1.xml", "/foo2.xml" }, properties = { "k1a=v1a", "k1b: v1b" })
  @TestPropertySource(locations = { "/foo1.xml", "/foo2.xml" }, properties = { "k1a=v1a", "k1b: v1b" })
  static class LocallyDuplicatedLocationsAndProperties {
  }

  @TestPropertySource(locations = { "/foo1.xml", "/foo2.xml" }, properties = { "k1a=v1a", "k1b: v1b" })
  static class DuplicatedLocationsAndPropertiesPropertySources extends LocationsAndPropertiesPropertySources {
  }

}
