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

package cn.taketoday.test.context;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import cn.taketoday.context.ApplicationContextInitializer;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.test.context.support.AnnotationConfigContextLoader;
import cn.taketoday.test.context.support.GenericXmlContextLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link MergedContextConfiguration}.
 *
 * <p>These tests primarily exist to ensure that {@code MergedContextConfiguration}
 * can safely be used as the cache key for
 * {@link cn.taketoday.test.context.cache.ContextCache ContextCache}.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 4.0
 */
class MergedContextConfigurationTests {

  private static final String[] EMPTY_STRING_ARRAY = new String[0];

  private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];

  private final GenericXmlContextLoader loader = new GenericXmlContextLoader();

  @Test
  void hashCodeWithNulls() {
    MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(null, null, null, null, null);
    MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(null, null, null, null, null);
    assertThat(mergedConfig2).hasSameHashCodeAs(mergedConfig1);
  }

  @Test
  void hashCodeWithNullArrays() {
    MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(), null, null, null, loader);
    MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(), null, null, null, loader);
    assertThat(mergedConfig2).hasSameHashCodeAs(mergedConfig1);
  }

  @Test
  void hashCodeWithEmptyArrays() {
    MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(),
            EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, loader);
    MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(),
            EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, loader);
    assertThat(mergedConfig2).hasSameHashCodeAs(mergedConfig1);
  }

  @Test
  void hashCodeWithEmptyArraysAndDifferentLoaders() {
    MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(),
            EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, loader);
    MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(),
            EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, new AnnotationConfigContextLoader());
    assertThat(mergedConfig2.hashCode()).isNotEqualTo(mergedConfig1.hashCode());
  }

  @Test
  void hashCodeWithSameLocations() {
    String[] locations = new String[] { "foo", "bar}" };
    MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(), locations,
            EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, loader);
    MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(), locations,
            EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, loader);
    assertThat(mergedConfig2).hasSameHashCodeAs(mergedConfig1);
  }

  @Test
  void hashCodeWithDifferentLocations() {
    String[] locations1 = new String[] { "foo", "bar}" };
    String[] locations2 = new String[] { "baz", "quux}" };
    MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(), locations1,
            EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, loader);
    MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(), locations2,
            EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, loader);
    assertThat(mergedConfig2.hashCode()).isNotEqualTo(mergedConfig1.hashCode());
  }

  @Test
  void hashCodeWithSameConfigClasses() {
    Class<?>[] classes = new Class<?>[] { String.class, Integer.class };
    MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(),
            EMPTY_STRING_ARRAY, classes, EMPTY_STRING_ARRAY, loader);
    MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(),
            EMPTY_STRING_ARRAY, classes, EMPTY_STRING_ARRAY, loader);
    assertThat(mergedConfig2).hasSameHashCodeAs(mergedConfig1);
  }

  @Test
  void hashCodeWithDifferentConfigClasses() {
    Class<?>[] classes1 = new Class<?>[] { String.class, Integer.class };
    Class<?>[] classes2 = new Class<?>[] { Boolean.class, Number.class };
    MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(),
            EMPTY_STRING_ARRAY, classes1, EMPTY_STRING_ARRAY, loader);
    MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(),
            EMPTY_STRING_ARRAY, classes2, EMPTY_STRING_ARRAY, loader);
    assertThat(mergedConfig2.hashCode()).isNotEqualTo(mergedConfig1.hashCode());
  }

  @Test
  void hashCodeWithSameProfiles() {
    String[] activeProfiles = new String[] { "catbert", "dogbert" };
    MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(),
            EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, activeProfiles, loader);
    MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(),
            EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, activeProfiles, loader);
    assertThat(mergedConfig2).hasSameHashCodeAs(mergedConfig1);
  }

  @Test
  void hashCodeWithSameProfilesReversed() {
    String[] activeProfiles1 = new String[] { "catbert", "dogbert" };
    String[] activeProfiles2 = new String[] { "dogbert", "catbert" };
    MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(),
            EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, activeProfiles1, loader);
    MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(),
            EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, activeProfiles2, loader);
    assertThat(mergedConfig2.hashCode()).isNotEqualTo(mergedConfig1.hashCode());
  }

  @Test
  void hashCodeWithSameDuplicateProfiles() {
    String[] activeProfiles1 = new String[] { "catbert", "dogbert" };
    String[] activeProfiles2 = new String[] { "catbert", "dogbert", "catbert", "dogbert", "catbert" };
    MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(),
            EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, activeProfiles1, loader);
    MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(),
            EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, activeProfiles2, loader);
    assertThat(mergedConfig2).hasSameHashCodeAs(mergedConfig1);
  }

  @Test
  void hashCodeWithDifferentProfiles() {
    String[] activeProfiles1 = new String[] { "catbert", "dogbert" };
    String[] activeProfiles2 = new String[] { "X", "Y" };
    MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(),
            EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, activeProfiles1, loader);
    MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(),
            EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, activeProfiles2, loader);
    assertThat(mergedConfig2.hashCode()).isNotEqualTo(mergedConfig1.hashCode());
  }

  @Test
  void hashCodeWithSameInitializers() {
    Set<Class<? extends ApplicationContextInitializer>> initializerClasses1 =
            new HashSet<>();
    initializerClasses1.add(FooInitializer.class);
    initializerClasses1.add(BarInitializer.class);

    Set<Class<? extends ApplicationContextInitializer>> initializerClasses2 =
            new HashSet<>();
    initializerClasses2.add(BarInitializer.class);
    initializerClasses2.add(FooInitializer.class);

    MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(),
            EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, initializerClasses1, EMPTY_STRING_ARRAY, loader);
    MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(),
            EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, initializerClasses2, EMPTY_STRING_ARRAY, loader);
    assertThat(mergedConfig2).hasSameHashCodeAs(mergedConfig1);
  }

  @Test
  void hashCodeWithDifferentInitializers() {
    Set<Class<? extends ApplicationContextInitializer>> initializerClasses1 =
            new HashSet<>();
    initializerClasses1.add(FooInitializer.class);

    Set<Class<? extends ApplicationContextInitializer>> initializerClasses2 =
            new HashSet<>();
    initializerClasses2.add(BarInitializer.class);

    MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(),
            EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, initializerClasses1, EMPTY_STRING_ARRAY, loader);
    MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(),
            EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, initializerClasses2, EMPTY_STRING_ARRAY, loader);
    assertThat(mergedConfig2.hashCode()).isNotEqualTo(mergedConfig1.hashCode());
  }

  /**
   * @since 4.0
   */
  @Test
  void hashCodeWithSameParent() {
    MergedContextConfiguration parent = new MergedContextConfiguration(getClass(), new String[] { "foo", "bar}" },
            EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, loader);

    MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(),
            EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, null, EMPTY_STRING_ARRAY, loader, null, parent);
    MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(), EMPTY_STRING_ARRAY,
            EMPTY_CLASS_ARRAY, null, EMPTY_STRING_ARRAY, loader, null, parent);
    assertThat(mergedConfig2).hasSameHashCodeAs(mergedConfig1);
  }

  /**
   * @since 4.0
   */
  @Test
  void hashCodeWithDifferentParents() {
    MergedContextConfiguration parent1 = new MergedContextConfiguration(getClass(), new String[] { "foo", "bar}" },
            EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, loader);
    MergedContextConfiguration parent2 = new MergedContextConfiguration(getClass(), new String[] { "baz", "quux" },
            EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, loader);

    MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(), EMPTY_STRING_ARRAY,
            EMPTY_CLASS_ARRAY, null, EMPTY_STRING_ARRAY, loader, null, parent1);
    MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(), EMPTY_STRING_ARRAY,
            EMPTY_CLASS_ARRAY, null, EMPTY_STRING_ARRAY, loader, null, parent2);
    assertThat(mergedConfig2.hashCode()).isNotEqualTo(mergedConfig1.hashCode());
  }

  @Test
  void equalsBasics() {
    MergedContextConfiguration mergedConfig = new MergedContextConfiguration(null, null, null, null, null);
    assertThat(mergedConfig).isEqualTo(mergedConfig);
    assertThat(mergedConfig).isNotEqualTo(null);
    assertThat(mergedConfig).isNotEqualTo(1);
  }

  @Test
  void equalsWithNulls() {
    MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(null, null, null, null, null);
    MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(null, null, null, null, null);
    assertThat(mergedConfig2).isEqualTo(mergedConfig1);
  }

  @Test
  void equalsWithNullArrays() {
    MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(), null, null, null, loader);
    MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(), null, null, null, loader);
    assertThat(mergedConfig2).isEqualTo(mergedConfig1);
  }

  @Test
  void equalsWithEmptyArrays() {
    MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(),
            EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, loader);
    MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(),
            EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, loader);
    assertThat(mergedConfig2).isEqualTo(mergedConfig1);
  }

  @Test
  void equalsWithEmptyArraysAndDifferentLoaders() {
    MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(),
            EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, loader);
    MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(),
            EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, new AnnotationConfigContextLoader());
    assertThat(mergedConfig2).isNotEqualTo(mergedConfig1);
    assertThat(mergedConfig1).isNotEqualTo(mergedConfig2);
  }

  @Test
  void equalsWithSameLocations() {
    String[] locations = new String[] { "foo", "bar}" };
    MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(),
            locations, EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, loader);
    MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(),
            locations, EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, loader);
    assertThat(mergedConfig2).isEqualTo(mergedConfig1);
  }

  @Test
  void equalsWithDifferentLocations() {
    String[] locations1 = new String[] { "foo", "bar}" };
    String[] locations2 = new String[] { "baz", "quux}" };
    MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(),
            locations1, EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, loader);
    MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(),
            locations2, EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, loader);
    assertThat(mergedConfig2).isNotEqualTo(mergedConfig1);
    assertThat(mergedConfig1).isNotEqualTo(mergedConfig2);
  }

  @Test
  void equalsWithSameConfigClasses() {
    Class<?>[] classes = new Class<?>[] { String.class, Integer.class };
    MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(),
            EMPTY_STRING_ARRAY, classes, EMPTY_STRING_ARRAY, loader);
    MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(),
            EMPTY_STRING_ARRAY, classes, EMPTY_STRING_ARRAY, loader);
    assertThat(mergedConfig2).isEqualTo(mergedConfig1);
  }

  @Test
  void equalsWithDifferentConfigClasses() {
    Class<?>[] classes1 = new Class<?>[] { String.class, Integer.class };
    Class<?>[] classes2 = new Class<?>[] { Boolean.class, Number.class };
    MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(),
            EMPTY_STRING_ARRAY, classes1, EMPTY_STRING_ARRAY, loader);
    MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(),
            EMPTY_STRING_ARRAY, classes2, EMPTY_STRING_ARRAY, loader);
    assertThat(mergedConfig2).isNotEqualTo(mergedConfig1);
    assertThat(mergedConfig1).isNotEqualTo(mergedConfig2);
  }

  @Test
  void equalsWithSameProfiles() {
    String[] activeProfiles = new String[] { "catbert", "dogbert" };
    MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(),
            EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, activeProfiles, loader);
    MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(),
            EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, activeProfiles, loader);
    assertThat(mergedConfig2).isEqualTo(mergedConfig1);
  }

  @Test
  void equalsWithSameProfilesReversed() {
    String[] activeProfiles1 = new String[] { "catbert", "dogbert" };
    String[] activeProfiles2 = new String[] { "dogbert", "catbert" };
    MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(),
            EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, activeProfiles1, loader);
    MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(),
            EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, activeProfiles2, loader);
    assertThat(mergedConfig2).isNotEqualTo(mergedConfig1);
  }

  @Test
  void equalsWithSameDuplicateProfiles() {
    String[] activeProfiles1 = new String[] { "catbert", "dogbert" };
    String[] activeProfiles2 = new String[] { "catbert", "dogbert", "catbert", "dogbert", "catbert" };
    MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(),
            EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, activeProfiles1, loader);
    MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(),
            EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, activeProfiles2, loader);
    assertThat(mergedConfig2).isEqualTo(mergedConfig1);
  }

  @Test
  void equalsWithDifferentProfiles() {
    String[] activeProfiles1 = new String[] { "catbert", "dogbert" };
    String[] activeProfiles2 = new String[] { "X", "Y" };
    MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(),
            EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, activeProfiles1, loader);
    MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(),
            EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, activeProfiles2, loader);
    assertThat(mergedConfig2).isNotEqualTo(mergedConfig1);
    assertThat(mergedConfig1).isNotEqualTo(mergedConfig2);
  }

  @Test
  void equalsWithSameInitializers() {
    Set<Class<? extends ApplicationContextInitializer>> initializerClasses1 =
            new HashSet<>();
    initializerClasses1.add(FooInitializer.class);
    initializerClasses1.add(BarInitializer.class);

    Set<Class<? extends ApplicationContextInitializer>> initializerClasses2 =
            new HashSet<>();
    initializerClasses2.add(BarInitializer.class);
    initializerClasses2.add(FooInitializer.class);

    MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(),
            EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, initializerClasses1, EMPTY_STRING_ARRAY, loader);
    MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(),
            EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, initializerClasses2, EMPTY_STRING_ARRAY, loader);
    assertThat(mergedConfig2).isEqualTo(mergedConfig1);
  }

  @Test
  void equalsWithDifferentInitializers() {
    Set<Class<? extends ApplicationContextInitializer>> initializerClasses1 =
            new HashSet<>();
    initializerClasses1.add(FooInitializer.class);

    Set<Class<? extends ApplicationContextInitializer>> initializerClasses2 =
            new HashSet<>();
    initializerClasses2.add(BarInitializer.class);

    MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(),
            EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, initializerClasses1, EMPTY_STRING_ARRAY, loader);
    MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(),
            EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, initializerClasses2, EMPTY_STRING_ARRAY, loader);
    assertThat(mergedConfig2).isNotEqualTo(mergedConfig1);
    assertThat(mergedConfig1).isNotEqualTo(mergedConfig2);
  }

  /**
   * @since 4.0
   */
  @Test
  void equalsWithSameContextCustomizers() {
    Set<ContextCustomizer> customizers = Collections.singleton(mock(ContextCustomizer.class));
    MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(), EMPTY_STRING_ARRAY,
            EMPTY_CLASS_ARRAY, null, EMPTY_STRING_ARRAY, null, null, customizers, loader, null, null);
    MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(), EMPTY_STRING_ARRAY,
            EMPTY_CLASS_ARRAY, null, EMPTY_STRING_ARRAY, null, null, customizers, loader, null, null);
    assertThat(mergedConfig2).isEqualTo(mergedConfig1);
  }

  /**
   * @since 4.0
   */
  @Test
  void equalsWithDifferentContextCustomizers() {
    Set<ContextCustomizer> customizers1 = Collections.singleton(mock(ContextCustomizer.class));
    Set<ContextCustomizer> customizers2 = Collections.singleton(mock(ContextCustomizer.class));

    MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(), EMPTY_STRING_ARRAY,
            EMPTY_CLASS_ARRAY, null, EMPTY_STRING_ARRAY, null, null, customizers1, loader, null, null);
    MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(), EMPTY_STRING_ARRAY,
            EMPTY_CLASS_ARRAY, null, EMPTY_STRING_ARRAY, null, null, customizers2, loader, null, null);
    assertThat(mergedConfig2).isNotEqualTo(mergedConfig1);
    assertThat(mergedConfig1).isNotEqualTo(mergedConfig2);
  }

  /**
   * @since 4.0
   */
  @Test
  void equalsWithSameParent() {
    MergedContextConfiguration parent = new MergedContextConfiguration(getClass(), new String[] { "foo", "bar}" },
            EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, loader);

    MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(), EMPTY_STRING_ARRAY,
            EMPTY_CLASS_ARRAY, null, EMPTY_STRING_ARRAY, loader, null, parent);
    MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(), EMPTY_STRING_ARRAY,
            EMPTY_CLASS_ARRAY, null, EMPTY_STRING_ARRAY, loader, null, parent);
    assertThat(mergedConfig2).isEqualTo(mergedConfig1);
    assertThat(mergedConfig1).isEqualTo(mergedConfig2);
  }

  /**
   * @since 4.0
   */
  @Test
  void equalsWithDifferentParents() {
    MergedContextConfiguration parent1 = new MergedContextConfiguration(getClass(), new String[] { "foo", "bar}" },
            EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, loader);
    MergedContextConfiguration parent2 = new MergedContextConfiguration(getClass(), new String[] { "baz", "quux" },
            EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, loader);

    MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(), EMPTY_STRING_ARRAY,
            EMPTY_CLASS_ARRAY, null, EMPTY_STRING_ARRAY, loader, null, parent1);
    MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(), EMPTY_STRING_ARRAY,
            EMPTY_CLASS_ARRAY, null, EMPTY_STRING_ARRAY, loader, null, parent2);
    assertThat(mergedConfig2).isNotEqualTo(mergedConfig1);
    assertThat(mergedConfig1).isNotEqualTo(mergedConfig2);
  }

  private static class FooInitializer implements ApplicationContextInitializer {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {

    }
  }

  private static class BarInitializer implements ApplicationContextInitializer {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {

    }
  }

}
