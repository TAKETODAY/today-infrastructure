/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

import org.junit.jupiter.api.Test;

import java.util.List;

import cn.taketoday.core.annotation.AnnotationConfigurationException;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.ContextConfigurationAttributes;
import cn.taketoday.test.context.ContextLoader;

import static cn.taketoday.test.context.support.ContextLoaderUtils.resolveContextConfigurationAttributes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit tests for {@link ContextLoaderUtils} involving {@link ContextConfigurationAttributes}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
class ContextLoaderUtilsConfigurationAttributesTests extends AbstractContextConfigurationUtilsTests {

  private void assertLocationsFooAttributes(ContextConfigurationAttributes attributes) {
    assertAttributes(attributes, LocationsFoo.class, new String[] { "/foo.xml" }, EMPTY_CLASS_ARRAY,
            ContextLoader.class, false);
  }

  private void assertClassesFooAttributes(ContextConfigurationAttributes attributes) {
    assertAttributes(attributes, ClassesFoo.class, EMPTY_STRING_ARRAY, new Class<?>[] { FooConfig.class },
            ContextLoader.class, false);
  }

  private void assertLocationsBarAttributes(ContextConfigurationAttributes attributes) {
    assertAttributes(attributes, LocationsBar.class, new String[] { "/bar.xml" }, EMPTY_CLASS_ARRAY,
            AnnotationConfigContextLoader.class, true);
  }

  private void assertClassesBarAttributes(ContextConfigurationAttributes attributes) {
    assertAttributes(attributes, ClassesBar.class, EMPTY_STRING_ARRAY, new Class<?>[] { BarConfig.class },
            AnnotationConfigContextLoader.class, true);
  }

  @Test
  void resolveConfigAttributesWithConflictingLocations() {
    assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(() ->
                    resolveContextConfigurationAttributes(ConflictingLocations.class))
            .withMessageStartingWith("Different @AliasFor mirror values")
            .withMessageContaining(ConflictingLocations.class.getName())
            .withMessageContaining("attribute 'locations' and its alias 'value'")
            .withMessageContaining("values of [{y}] and [{x}]");
  }

  @Test
  void resolveConfigAttributesWithBareAnnotations() {
    Class<BareAnnotations> testClass = BareAnnotations.class;
    List<ContextConfigurationAttributes> attributesList = resolveContextConfigurationAttributes(testClass);
    assertThat(attributesList).isNotNull();
    assertThat(attributesList.size()).isEqualTo(1);
    assertAttributes(attributesList.get(0),
            testClass, EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, ContextLoader.class, true);
  }

  @Test
  void resolveConfigAttributesWithLocalAnnotationAndLocations() {
    List<ContextConfigurationAttributes> attributesList = resolveContextConfigurationAttributes(LocationsFoo.class);
    assertThat(attributesList).isNotNull();
    assertThat(attributesList.size()).isEqualTo(1);
    assertLocationsFooAttributes(attributesList.get(0));
  }

  @Test
  void resolveConfigAttributesWithMetaAnnotationAndLocations() {
    Class<MetaLocationsFoo> testClass = MetaLocationsFoo.class;
    List<ContextConfigurationAttributes> attributesList = resolveContextConfigurationAttributes(testClass);
    assertThat(attributesList).isNotNull();
    assertThat(attributesList.size()).isEqualTo(1);
    assertAttributes(attributesList.get(0),
            testClass, new String[] { "/foo.xml" }, EMPTY_CLASS_ARRAY, ContextLoader.class, true);
  }

  @Test
  void resolveConfigAttributesWithMetaAnnotationAndLocationsAndOverrides() {
    Class<MetaLocationsFooWithOverrides> testClass = MetaLocationsFooWithOverrides.class;
    List<ContextConfigurationAttributes> attributesList = resolveContextConfigurationAttributes(testClass);
    assertThat(attributesList).isNotNull();
    assertThat(attributesList.size()).isEqualTo(1);
    assertAttributes(attributesList.get(0),
            testClass, new String[] { "/foo.xml" }, EMPTY_CLASS_ARRAY, ContextLoader.class, true);
  }

  @Test
  void resolveConfigAttributesWithMetaAnnotationAndLocationsAndOverriddenAttributes() {
    Class<MetaLocationsFooWithOverriddenAttributes> testClass = MetaLocationsFooWithOverriddenAttributes.class;
    List<ContextConfigurationAttributes> attributesList = resolveContextConfigurationAttributes(testClass);
    assertThat(attributesList).isNotNull();
    assertThat(attributesList.size()).isEqualTo(1);
    assertAttributes(attributesList.get(0),
            testClass, new String[] { "foo1.xml", "foo2.xml" }, EMPTY_CLASS_ARRAY, ContextLoader.class, true);
  }

  @Test
  void resolveConfigAttributesWithMetaAnnotationAndLocationsInClassHierarchy() {
    Class<MetaLocationsBar> testClass = MetaLocationsBar.class;
    List<ContextConfigurationAttributes> attributesList = resolveContextConfigurationAttributes(testClass);
    assertThat(attributesList).isNotNull();
    assertThat(attributesList.size()).isEqualTo(2);
    assertAttributes(attributesList.get(0),
            testClass, new String[] { "/bar.xml" }, EMPTY_CLASS_ARRAY, ContextLoader.class, true);
    assertAttributes(attributesList.get(1),
            MetaLocationsFoo.class, new String[] { "/foo.xml" }, EMPTY_CLASS_ARRAY, ContextLoader.class, true);
  }

  @Test
  void resolveConfigAttributesWithLocalAnnotationAndClasses() {
    List<ContextConfigurationAttributes> attributesList = resolveContextConfigurationAttributes(ClassesFoo.class);
    assertThat(attributesList).isNotNull();
    assertThat(attributesList.size()).isEqualTo(1);
    assertClassesFooAttributes(attributesList.get(0));
  }

  @Test
  void resolveConfigAttributesWithLocalAndInheritedAnnotationsAndLocations() {
    List<ContextConfigurationAttributes> attributesList = resolveContextConfigurationAttributes(LocationsBar.class);
    assertThat(attributesList).isNotNull();
    assertThat(attributesList.size()).isEqualTo(2);
    assertLocationsBarAttributes(attributesList.get(0));
    assertLocationsFooAttributes(attributesList.get(1));
  }

  @Test
  void resolveConfigAttributesWithLocalAndInheritedAnnotationsAndClasses() {
    List<ContextConfigurationAttributes> attributesList = resolveContextConfigurationAttributes(ClassesBar.class);
    assertThat(attributesList).isNotNull();
    assertThat(attributesList.size()).isEqualTo(2);
    assertClassesBarAttributes(attributesList.get(0));
    assertClassesFooAttributes(attributesList.get(1));
  }

  /**
   * Verifies change requested in <a href="https://jira.spring.io/browse/SPR-11634">SPR-11634</a>.
   *
   * @since 4.0
   */
  @Test
  void resolveConfigAttributesWithLocationsAndClasses() {
    List<ContextConfigurationAttributes> attributesList = resolveContextConfigurationAttributes(LocationsAndClasses.class);
    assertThat(attributesList).isNotNull();
    assertThat(attributesList.size()).isEqualTo(1);
  }

  // -------------------------------------------------------------------------

  @ContextConfiguration(value = "x", locations = "y")
  private static class ConflictingLocations {
  }

  @ContextConfiguration(locations = "x", classes = Object.class)
  private static class LocationsAndClasses {
  }

}
