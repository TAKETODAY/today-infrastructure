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

package infra.test.context;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.stream.Stream;

import infra.test.context.support.DefaultTestContextBootstrapper;
import infra.test.context.web.WebAppConfiguration;
import infra.test.context.web.WebTestContextBootstrapper;

import static infra.test.context.NestedTestConfiguration.EnclosingConfiguration.INHERIT;
import static infra.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link BootstrapUtils}.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 4.0
 */
class BootstrapUtilsTests {

  private final CacheAwareContextLoaderDelegate delegate = mock(CacheAwareContextLoaderDelegate.class);

  @Test
  void resolveTestContextBootstrapperWithEmptyBootstrapWithAnnotation() {
    BootstrapContext bootstrapContext = BootstrapTestUtils.buildBootstrapContext(EmptyBootstrapWithAnnotationClass.class, delegate);
    assertThatIllegalStateException().isThrownBy(() ->
                    BootstrapUtils.resolveTestContextBootstrapper(bootstrapContext))
            .withMessageContaining("Specify @BootstrapWith's 'value' attribute");
  }

  @Test
  void resolveTestContextBootstrapperWithDoubleMetaBootstrapWithAnnotations() {
    BootstrapContext bootstrapContext = BootstrapTestUtils.buildBootstrapContext(
            DoubleMetaAnnotatedBootstrapWithAnnotationClass.class, delegate);
    assertThatIllegalStateException().isThrownBy(() ->
                    BootstrapUtils.resolveTestContextBootstrapper(bootstrapContext))
            .withMessageContaining("Configuration error: found multiple declarations of @BootstrapWith")
            .withMessageContaining(FooBootstrapper.class.getSimpleName())
            .withMessageContaining(BarBootstrapper.class.getSimpleName());
  }

  @Test
  void resolveTestContextBootstrapperForNonAnnotatedClass() {
    assertBootstrapper(NonAnnotatedClass.class, DefaultTestContextBootstrapper.class);
  }

  @Test
  void resolveTestContextBootstrapperWithDirectBootstrapWithAnnotation() {
    assertBootstrapper(DirectBootstrapWithAnnotationClass.class, FooBootstrapper.class);
  }

  @Test
  void resolveTestContextBootstrapperWithInheritedBootstrapWithAnnotation() {
    assertBootstrapper(InheritedBootstrapWithAnnotationClass.class, FooBootstrapper.class);
  }

  @Test
  void resolveTestContextBootstrapperWithMetaBootstrapWithAnnotation() {
    assertBootstrapper(MetaAnnotatedBootstrapWithAnnotationClass.class, BarBootstrapper.class);
  }

  @Test
  void resolveTestContextBootstrapperWithDuplicatingMetaBootstrapWithAnnotations() {
    assertBootstrapper(DuplicateMetaAnnotatedBootstrapWithAnnotationClass.class, FooBootstrapper.class);
  }

  /**
   * @since 4.0
   */
  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource
  void resolveTestContextBootstrapperInEnclosingClassHierarchy(Class<?> testClass, Class<?> expectedBootstrapper) {
    assertBootstrapper(testClass, expectedBootstrapper);
  }

  static Stream<Arguments> resolveTestContextBootstrapperInEnclosingClassHierarchy() {
    return Stream.of(//
            args(OuterClass.class, FooBootstrapper.class),//
            args(OuterClass.NestedWithInheritedBootstrapper.class, FooBootstrapper.class),//
            args(OuterClass.NestedWithInheritedBootstrapper.DoubleNestedWithInheritedButOverriddenBootstrapper.class, EnigmaBootstrapper.class),//
            args(OuterClass.NestedWithInheritedBootstrapper.DoubleNestedWithOverriddenBootstrapper.class, BarBootstrapper.class),//
            args(OuterClass.NestedWithInheritedBootstrapper.DoubleNestedWithOverriddenBootstrapper.TripleNestedWithInheritedBootstrapper.class, BarBootstrapper.class),//
            args(OuterClass.NestedWithInheritedBootstrapper.DoubleNestedWithOverriddenBootstrapper.TripleNestedWithInheritedBootstrapperButLocalOverride.class, EnigmaBootstrapper.class),//
            // @WebAppConfiguration and default bootstrapper
            args(WebAppConfigClass.class, WebTestContextBootstrapper.class),//
            args(WebAppConfigClass.NestedWithInheritedWebConfig.class, WebTestContextBootstrapper.class),//
            args(WebAppConfigClass.NestedWithInheritedWebConfig.DoubleNestedWithImplicitlyInheritedWebConfig.class, WebTestContextBootstrapper.class),//
            args(WebAppConfigClass.NestedWithInheritedWebConfig.DoubleNestedWithOverriddenWebConfig.class, DefaultTestContextBootstrapper.class),//
            args(WebAppConfigClass.NestedWithInheritedWebConfig.DoubleNestedWithOverriddenWebConfig.TripleNestedWithInheritedOverriddenWebConfig.class, WebTestContextBootstrapper.class),//
            args(WebAppConfigClass.NestedWithInheritedWebConfig.DoubleNestedWithOverriddenWebConfig.TripleNestedWithInheritedOverriddenWebConfigAndTestInterface.class,
                    DefaultTestContextBootstrapper.class)//
    );
  }

  private static Arguments args(Class<?> testClass, Class<? extends TestContextBootstrapper> expectedBootstrapper) {
    return arguments(named(testClass.getSimpleName(), testClass), expectedBootstrapper);
  }

  /**
   * @since 4.0
   */
  @Test
  void resolveTestContextBootstrapperWithLocalDeclarationThatOverridesMetaBootstrapWithAnnotations() {
    assertBootstrapper(LocalDeclarationAndMetaAnnotatedBootstrapWithAnnotationClass.class, EnigmaBootstrapper.class);
  }

  private void assertBootstrapper(Class<?> testClass, Class<?> expectedBootstrapper) {
    BootstrapContext bootstrapContext = BootstrapTestUtils.buildBootstrapContext(testClass, delegate);
    TestContextBootstrapper bootstrapper = BootstrapUtils.resolveTestContextBootstrapper(bootstrapContext);
    assertThat(bootstrapper).isNotNull();
    assertThat(bootstrapper.getClass()).isEqualTo(expectedBootstrapper);
  }

  // -------------------------------------------------------------------

  static class FooBootstrapper extends DefaultTestContextBootstrapper { }

  static class BarBootstrapper extends DefaultTestContextBootstrapper { }

  static class EnigmaBootstrapper extends DefaultTestContextBootstrapper { }

  @BootstrapWith(FooBootstrapper.class)
  @Retention(RetentionPolicy.RUNTIME)
  @interface BootWithFoo { }

  @BootstrapWith(FooBootstrapper.class)
  @Retention(RetentionPolicy.RUNTIME)
  @interface BootWithFooAgain { }

  @BootstrapWith(BarBootstrapper.class)
  @Retention(RetentionPolicy.RUNTIME)
  @interface BootWithBar { }

  // Invalid
  @BootstrapWith
  static class EmptyBootstrapWithAnnotationClass { }

  // Invalid
  @BootWithBar
  @BootWithFoo
  static class DoubleMetaAnnotatedBootstrapWithAnnotationClass { }

  static class NonAnnotatedClass { }

  @BootstrapWith(FooBootstrapper.class)
  static class DirectBootstrapWithAnnotationClass { }

  static class InheritedBootstrapWithAnnotationClass extends DirectBootstrapWithAnnotationClass { }

  @BootWithBar
  static class MetaAnnotatedBootstrapWithAnnotationClass { }

  @BootWithFoo
  @BootWithFooAgain
  static class DuplicateMetaAnnotatedBootstrapWithAnnotationClass { }

  @BootWithFoo
  @BootWithBar
  @BootstrapWith(EnigmaBootstrapper.class)
  static class LocalDeclarationAndMetaAnnotatedBootstrapWithAnnotationClass { }

  @WebAppConfiguration
  static class WebAppConfigClass {

    @NestedTestConfiguration(INHERIT)
    class NestedWithInheritedWebConfig {

      class DoubleNestedWithImplicitlyInheritedWebConfig {
      }

      @NestedTestConfiguration(OVERRIDE)
      class DoubleNestedWithOverriddenWebConfig {

        @NestedTestConfiguration(INHERIT)
        @WebAppConfiguration
        class TripleNestedWithInheritedOverriddenWebConfig {
        }

        @NestedTestConfiguration(INHERIT)
        class TripleNestedWithInheritedOverriddenWebConfigAndTestInterface {
        }
      }
    }

    // Intentionally not annotated with @WebAppConfiguration to ensure that
    // TripleNestedWithInheritedOverriddenWebConfigAndTestInterface is not
    // considered to be annotated with @WebAppConfiguration even though the
    // enclosing class for TestInterface is annotated with @WebAppConfiguration.
    interface TestInterface {
    }
  }

  @BootWithFoo
  static class OuterClass {

    @NestedTestConfiguration(INHERIT)
    class NestedWithInheritedBootstrapper {

      @NestedTestConfiguration(INHERIT)
      @BootstrapWith(EnigmaBootstrapper.class)
      class DoubleNestedWithInheritedButOverriddenBootstrapper {
      }

      @NestedTestConfiguration(OVERRIDE)
      @BootWithBar
      class DoubleNestedWithOverriddenBootstrapper {

        @NestedTestConfiguration(INHERIT)
        class TripleNestedWithInheritedBootstrapper {
        }

        @NestedTestConfiguration(INHERIT)
        @BootstrapWith(EnigmaBootstrapper.class)
        class TripleNestedWithInheritedBootstrapperButLocalOverride {
        }
      }
    }
  }

}
