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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import cn.taketoday.test.context.support.DefaultTestContextBootstrapper;
import cn.taketoday.test.context.web.WebTestContextBootstrapper;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static cn.taketoday.test.context.BootstrapUtils.resolveTestContextBootstrapper;
import static cn.taketoday.test.context.NestedTestConfiguration.EnclosingConfiguration.INHERIT;
import static cn.taketoday.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;

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
				resolveTestContextBootstrapper(bootstrapContext))
			.withMessageContaining("Specify @BootstrapWith's 'value' attribute");
	}

	@Test
	void resolveTestContextBootstrapperWithDoubleMetaBootstrapWithAnnotations() {
		BootstrapContext bootstrapContext = BootstrapTestUtils.buildBootstrapContext(
			DoubleMetaAnnotatedBootstrapWithAnnotationClass.class, delegate);
		assertThatIllegalStateException().isThrownBy(() ->
				resolveTestContextBootstrapper(bootstrapContext))
			.withMessageContaining("Configuration error: found multiple declarations of @BootstrapWith")
			.withMessageContaining(FooBootstrapper.class.getCanonicalName())
			.withMessageContaining(BarBootstrapper.class.getCanonicalName());
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
			args(WebAppConfigClass.NestedWithInheritedWebConfig.DoubleNestedWithOverriddenWebConfig.TripleNestedWithInheritedOverriddenWebConfigAndTestInterface.class, DefaultTestContextBootstrapper.class)//
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
		TestContextBootstrapper bootstrapper = resolveTestContextBootstrapper(bootstrapContext);
		assertThat(bootstrapper).isNotNull();
		assertThat(bootstrapper.getClass()).isEqualTo(expectedBootstrapper);
	}

	// -------------------------------------------------------------------

	static class FooBootstrapper extends DefaultTestContextBootstrapper {}

	static class BarBootstrapper extends DefaultTestContextBootstrapper {}

	static class EnigmaBootstrapper extends DefaultTestContextBootstrapper {}

	@BootstrapWith(FooBootstrapper.class)
	@Retention(RetentionPolicy.RUNTIME)
	@interface BootWithFoo {}

	@BootstrapWith(FooBootstrapper.class)
	@Retention(RetentionPolicy.RUNTIME)
	@interface BootWithFooAgain {}

	@BootstrapWith(BarBootstrapper.class)
	@Retention(RetentionPolicy.RUNTIME)
	@interface BootWithBar {}

	// Invalid
	@BootstrapWith
	static class EmptyBootstrapWithAnnotationClass {}

	// Invalid
	@BootWithBar
	@BootWithFoo
	static class DoubleMetaAnnotatedBootstrapWithAnnotationClass {}

	static class NonAnnotatedClass {}

	@BootstrapWith(FooBootstrapper.class)
	static class DirectBootstrapWithAnnotationClass {}

	static class InheritedBootstrapWithAnnotationClass extends DirectBootstrapWithAnnotationClass {}

	@BootWithBar
	static class MetaAnnotatedBootstrapWithAnnotationClass {}

	@BootWithFoo
	@BootWithFooAgain
	static class DuplicateMetaAnnotatedBootstrapWithAnnotationClass {}

	@BootWithFoo
	@BootWithBar
	@BootstrapWith(EnigmaBootstrapper.class)
	static class LocalDeclarationAndMetaAnnotatedBootstrapWithAnnotationClass {}

	@cn.taketoday.test.context.web.WebAppConfiguration
	static class WebAppConfigClass {

		@NestedTestConfiguration(INHERIT)
		class NestedWithInheritedWebConfig {

			class DoubleNestedWithImplicitlyInheritedWebConfig {
			}

			@NestedTestConfiguration(OVERRIDE)
			class DoubleNestedWithOverriddenWebConfig {

				@NestedTestConfiguration(INHERIT)
				@cn.taketoday.test.context.web.WebAppConfiguration
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
