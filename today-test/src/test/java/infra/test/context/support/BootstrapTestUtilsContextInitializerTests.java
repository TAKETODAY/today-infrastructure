/*
 * Copyright 2002-present the original author or authors.
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
