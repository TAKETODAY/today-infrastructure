/*
 * Copyright 2017 - 2024 the original author or authors.
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

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import infra.context.ApplicationContextInitializer;
import infra.context.ConfigurableApplicationContext;
import infra.test.context.BootstrapTestUtils;
import infra.test.context.ContextConfiguration;
import infra.test.context.MergedContextConfiguration;

/**
 * Unit tests for {@link BootstrapTestUtils} involving {@link ApplicationContextInitializer}s.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@SuppressWarnings("unchecked")
class BootstrapTestUtilsContextInitializerTests extends AbstractContextConfigurationUtilsTests {

  @Test
  void buildMergedConfigWithSingleLocalInitializer() {
    Class<?> testClass = SingleInitializer.class;
    MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);

    assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY,
            initializers(FooInitializer.class), DelegatingSmartContextLoader.class);
  }

  @Test
  void buildMergedConfigWithLocalInitializerAndConfigClass() {
    Class<?> testClass = InitializersFoo.class;
    MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);

    assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, classes(FooConfig.class),
            initializers(FooInitializer.class), DelegatingSmartContextLoader.class);
  }

  @Test
  void buildMergedConfigWithLocalAndInheritedInitializer() {
    Class<?> testClass = InitializersBar.class;
    MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);

    assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, classes(FooConfig.class, BarConfig.class),
            initializers(FooInitializer.class, BarInitializer.class), DelegatingSmartContextLoader.class);
  }

  @Test
  void buildMergedConfigWithOverriddenInitializers() {
    Class<?> testClass = OverriddenInitializersBar.class;
    MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);

    assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, classes(FooConfig.class, BarConfig.class),
            initializers(BarInitializer.class), DelegatingSmartContextLoader.class);
  }

  @Test
  void buildMergedConfigWithOverriddenInitializersAndClasses() {
    Class<?> testClass = OverriddenInitializersAndClassesBar.class;
    MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);

    assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, classes(BarConfig.class),
            initializers(BarInitializer.class), DelegatingSmartContextLoader.class);
  }

  private Set<Class<? extends ApplicationContextInitializer>> initializers(
          Class<? extends ApplicationContextInitializer>... classes) {

    return new HashSet<>(Arrays.asList(classes));
  }

  private Class<?>[] classes(Class<?>... classes) {
    return classes;
  }

  private static class FooInitializer implements ApplicationContextInitializer {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
// GenericApplicationContext
    }
  }

  private static class BarInitializer implements ApplicationContextInitializer {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {

    }
  }

  @ContextConfiguration(initializers = FooInitializer.class)
  private static class SingleInitializer {
  }

  @ContextConfiguration(classes = FooConfig.class, initializers = FooInitializer.class)
  private static class InitializersFoo {
  }

  @ContextConfiguration(classes = BarConfig.class, initializers = BarInitializer.class)
  private static class InitializersBar extends InitializersFoo {
  }

  @ContextConfiguration(classes = BarConfig.class, initializers = BarInitializer.class, inheritInitializers = false)
  private static class OverriddenInitializersBar extends InitializersFoo {
  }

  @ContextConfiguration(classes = BarConfig.class, inheritLocations = false, initializers = BarInitializer.class, inheritInitializers = false)
  private static class OverriddenInitializersAndClassesBar extends InitializersFoo {
  }

}
