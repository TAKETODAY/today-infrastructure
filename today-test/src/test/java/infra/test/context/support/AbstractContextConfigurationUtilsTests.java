/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.test.context.support;

import org.mockito.Mockito;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.Set;

import infra.context.ApplicationContextInitializer;
import infra.context.annotation.Configuration;
import infra.core.annotation.AliasFor;
import infra.test.context.ActiveProfiles;
import infra.test.context.BootstrapContext;
import infra.test.context.BootstrapTestUtils;
import infra.test.context.CacheAwareContextLoaderDelegate;
import infra.test.context.ContextConfiguration;
import infra.test.context.ContextConfigurationAttributes;
import infra.test.context.ContextHierarchy;
import infra.test.context.ContextLoader;
import infra.test.context.MergedContextConfiguration;
import infra.test.context.NestedTestConfiguration;
import infra.test.context.TestContextBootstrapper;
import infra.test.context.web.WebAppConfiguration;

import static infra.test.context.NestedTestConfiguration.EnclosingConfiguration.INHERIT;
import static infra.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * Abstract base class for tests involving {@link ContextLoaderUtils},
 * {@link BootstrapTestUtils}, and {@link ActiveProfilesUtils}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
abstract class AbstractContextConfigurationUtilsTests {

  static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];

  static final String[] EMPTY_STRING_ARRAY = new String[0];

  static final Set<Class<? extends ApplicationContextInitializer>>
          EMPTY_INITIALIZER_CLASSES = Collections.emptySet();

  MergedContextConfiguration buildMergedContextConfiguration(Class<?> testClass) {
    CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate = Mockito.mock(CacheAwareContextLoaderDelegate.class);
    BootstrapContext bootstrapContext = BootstrapTestUtils.buildBootstrapContext(testClass, cacheAwareContextLoaderDelegate);
    TestContextBootstrapper bootstrapper = BootstrapTestUtils.resolveTestContextBootstrapper(bootstrapContext);
    return bootstrapper.buildMergedContextConfiguration();
  }

  void assertAttributes(ContextConfigurationAttributes attributes, Class<?> expectedDeclaringClass,
          String[] expectedLocations, Class<?>[] expectedClasses,
          Class<? extends ContextLoader> expectedContextLoaderClass, boolean expectedInheritLocations) {

    assertSoftly(softly -> {
      softly.assertThat(attributes.getDeclaringClass()).as("declaring class").isEqualTo(expectedDeclaringClass);
      softly.assertThat(attributes.getLocations()).as("locations").isEqualTo(expectedLocations);
      softly.assertThat(attributes.getClasses()).as("classes").isEqualTo(expectedClasses);
      softly.assertThat(attributes.isInheritLocations()).as("inherit locations").isEqualTo(expectedInheritLocations);
      softly.assertThat(attributes.getContextLoaderClass()).as("context loader").isEqualTo(expectedContextLoaderClass);
    });
  }

  void assertMergedConfig(MergedContextConfiguration mergedConfig, Class<?> expectedTestClass,
          String[] expectedLocations, Class<?>[] expectedClasses,
          Class<? extends ContextLoader> expectedContextLoaderClass) {

    assertMergedConfig(mergedConfig, expectedTestClass, expectedLocations, expectedClasses,
            EMPTY_INITIALIZER_CLASSES, expectedContextLoaderClass);
  }

  void assertMergedConfig(
          MergedContextConfiguration mergedConfig,
          Class<?> expectedTestClass,
          String[] expectedLocations,
          Class<?>[] expectedClasses,
          Set<Class<? extends ApplicationContextInitializer>> expectedInitializerClasses,
          Class<? extends ContextLoader> expectedContextLoaderClass) {

    assertSoftly(softly -> {
      softly.assertThat(mergedConfig).as("merged config").isNotNull();
      softly.assertThat(mergedConfig.getTestClass()).as("test class").isEqualTo(expectedTestClass);
      softly.assertThat(mergedConfig.getLocations()).as("locations").containsExactly(expectedLocations);
      softly.assertThat(mergedConfig.getClasses()).as("classes").containsExactly(expectedClasses);
      softly.assertThat(mergedConfig.getActiveProfiles()).as("active profiles").isNotNull();

      if (expectedContextLoaderClass == null) {
        softly.assertThat(mergedConfig.getContextLoader()).as("context loader").isNull();
      }
      else {
        softly.assertThat(mergedConfig.getContextLoader().getClass()).as("context loader").isEqualTo(expectedContextLoaderClass);
      }
      softly.assertThat(mergedConfig.getContextInitializerClasses()).as("context initializers").isNotNull();
      softly.assertThat(mergedConfig.getContextInitializerClasses()).as("context initializers").isEqualTo(expectedInitializerClasses);
    });
  }

  @SafeVarargs
  static <T> T[] array(T... objects) {
    return objects;
  }

  static class Enigma {
  }

  @ContextConfiguration
  @ActiveProfiles
  static class BareAnnotations {
  }

  @Configuration
  static class FooConfig {
  }

  @Configuration
  static class BarConfig {
  }

  @Configuration
  static class BazConfig {
  }

  @Configuration
  static class QuuxConfig {
  }

  @ContextConfiguration("/foo.xml")
  @ActiveProfiles(profiles = "foo")
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  public static @interface MetaLocationsFooConfig {
  }

  @ContextConfiguration
  @ActiveProfiles
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  public static @interface MetaLocationsFooConfigWithOverrides {

    @AliasFor(annotation = ContextConfiguration.class)
    String[] locations() default "/foo.xml";

    @AliasFor(annotation = ActiveProfiles.class)
    String[] profiles() default "foo";
  }

  @ContextConfiguration("/bar.xml")
  @ActiveProfiles(profiles = "bar")
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  public static @interface MetaLocationsBarConfig {
  }

  @MetaLocationsFooConfig
  static class MetaLocationsFoo {
  }

  @MetaLocationsBarConfig
  static class MetaLocationsBar extends MetaLocationsFoo {
  }

  @MetaLocationsFooConfigWithOverrides
  static class MetaLocationsFooWithOverrides {
  }

  @MetaLocationsFooConfigWithOverrides(locations = { "foo1.xml", "foo2.xml" }, profiles = { "foo1", "foo2" })
  static class MetaLocationsFooWithOverriddenAttributes {
  }

  @ContextConfiguration(locations = "/foo.xml", inheritLocations = false)
  @ActiveProfiles("foo")
  static class LocationsFoo {
  }

  @ContextConfiguration(classes = FooConfig.class, inheritLocations = false)
  @ActiveProfiles("foo")
  static class ClassesFoo {
  }

  @WebAppConfiguration
  static class WebClassesFoo extends ClassesFoo {
  }

  @ContextConfiguration(locations = "/bar.xml", inheritLocations = true, loader = AnnotationConfigContextLoader.class)
  @ActiveProfiles("bar")
  static class LocationsBar extends LocationsFoo {
  }

  @ContextConfiguration(locations = "/bar.xml", inheritLocations = false, loader = AnnotationConfigContextLoader.class)
  @ActiveProfiles("bar")
  static class OverriddenLocationsBar extends LocationsFoo {
  }

  @ContextConfiguration(classes = BarConfig.class, inheritLocations = true, loader = AnnotationConfigContextLoader.class)
  @ActiveProfiles("bar")
  static class ClassesBar extends ClassesFoo {
  }

  @ContextConfiguration(classes = BarConfig.class, inheritLocations = false, loader = AnnotationConfigContextLoader.class)
  @ActiveProfiles("bar")
  static class OverriddenClassesBar extends ClassesFoo {
  }

  @SuppressWarnings("deprecation")
  @ContextConfiguration(locations = "/foo.properties", loader = GenericPropertiesContextLoader.class)
  @ActiveProfiles("foo")
  static class PropertiesLocationsFoo {
  }

  // Combining @Configuration classes with a Properties based loader doesn't really make
  // sense, but that's OK for unit testing purposes.
  @SuppressWarnings("deprecation")
  @ContextConfiguration(classes = FooConfig.class, loader = GenericPropertiesContextLoader.class)
  @ActiveProfiles("foo")
  static class PropertiesClassesFoo {
  }

  @ContextConfiguration(classes = FooConfig.class, loader = AnnotationConfigContextLoader.class)
  @NestedTestConfiguration(INHERIT)
  static class OuterTestCase {

    class NestedTestCaseWithInheritedConfig {
    }

    @ContextConfiguration(classes = BarConfig.class)
    class NestedTestCaseWithMergedInheritedConfig {
    }

    @NestedTestConfiguration(OVERRIDE)
    @ContextConfiguration(classes = BarConfig.class)
    class NestedTestCaseWithOverriddenConfig {

      @NestedTestConfiguration(INHERIT)
      class DoubleNestedTestCaseWithInheritedOverriddenConfig {
      }
    }

  }

  @ContextHierarchy({ //
          @ContextConfiguration(classes = FooConfig.class, loader = AnnotationConfigContextLoader.class, name = "foo"), //
          @ContextConfiguration(classes = BarConfig.class, loader = AnnotationConfigContextLoader.class, name = "bar")//
  })
  @NestedTestConfiguration(INHERIT)
  static class ContextHierarchyOuterTestCase {

    class NestedTestCaseWithInheritedConfig {
    }

    @ContextConfiguration(classes = BazConfig.class, loader = AnnotationConfigContextLoader.class, name = "bar")
    class NestedTestCaseWithMergedInheritedConfig {
    }

    @ContextConfiguration(classes = QuuxConfig.class, loader = AnnotationConfigContextLoader.class, name = "foo", inheritLocations = false)
    class NestedTestCaseWithOverriddenConfig {
    }

  }

}
