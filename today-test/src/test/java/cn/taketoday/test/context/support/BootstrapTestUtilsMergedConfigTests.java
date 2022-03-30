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

import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Test;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.test.context.ActiveProfiles;
import cn.taketoday.test.context.BootstrapTestUtils;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.ContextLoader;
import cn.taketoday.test.context.MergedContextConfiguration;
import cn.taketoday.test.context.TestPropertySource;
import cn.taketoday.test.context.web.WebDelegatingSmartContextLoader;
import cn.taketoday.test.context.web.WebMergedContextConfiguration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link BootstrapTestUtils} involving {@link MergedContextConfiguration}.
 *
 * @author Sam Brannen
 * @since 3.1
 */
class BootstrapTestUtilsMergedConfigTests extends AbstractContextConfigurationUtilsTests {

	@Test
	void buildImplicitMergedConfigWithoutAnnotation() {
		Class<?> testClass = Enigma.class;
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);

		assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, DelegatingSmartContextLoader.class);
	}

	/**
	 * @since 4.3
	 */
	@Test
	void buildMergedConfigWithContextConfigurationWithoutLocationsClassesOrInitializers() {
		assertThatIllegalStateException().isThrownBy(() ->
				buildMergedContextConfiguration(MissingContextAttributesTestCase.class))
			.withMessageStartingWith("DelegatingSmartContextLoader was unable to detect defaults, "
					+ "and no ApplicationContextInitializers or ContextCustomizers were declared for context configuration attributes");
	}

	@Test
	void buildMergedConfigWithBareAnnotations() {
		Class<?> testClass = BareAnnotations.class;
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);

		assertMergedConfig(
			mergedConfig,
			testClass,
			array("classpath:org/springframework/test/context/support/AbstractContextConfigurationUtilsTests$BareAnnotations-context.xml"),
			EMPTY_CLASS_ARRAY, DelegatingSmartContextLoader.class);
	}

	@Test
	void buildMergedConfigWithLocalAnnotationAndLocations() {
		Class<?> testClass = LocationsFoo.class;
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);

		assertMergedConfig(mergedConfig, testClass, array("classpath:/foo.xml"), EMPTY_CLASS_ARRAY,
			DelegatingSmartContextLoader.class);
	}

	@Test
	void buildMergedConfigWithMetaAnnotationAndLocations() {
		Class<?> testClass = MetaLocationsFoo.class;
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);

		assertMergedConfig(mergedConfig, testClass, array("classpath:/foo.xml"), EMPTY_CLASS_ARRAY,
			DelegatingSmartContextLoader.class);
	}

	@Test
	void buildMergedConfigWithMetaAnnotationAndClasses() {
		buildMergedConfigWithMetaAnnotationAndClasses(Dog.class);
		buildMergedConfigWithMetaAnnotationAndClasses(WorkingDog.class);
		buildMergedConfigWithMetaAnnotationAndClasses(GermanShepherd.class);
	}

	private void buildMergedConfigWithMetaAnnotationAndClasses(Class<?> testClass) {
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);
		assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, array(FooConfig.class,
			BarConfig.class), DelegatingSmartContextLoader.class);
	}

	@Test
	void buildMergedConfigWithLocalAnnotationAndClasses() {
		Class<?> testClass = ClassesFoo.class;
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);

		assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, array(FooConfig.class),
			DelegatingSmartContextLoader.class);
	}

	/**
	 * Introduced to investigate claims made in a discussion on
	 * <a href="https://stackoverflow.com/questions/24725438/what-could-cause-a-class-implementing-applicationlistenercontextrefreshedevent">Stack Overflow</a>.
	 */
	@Test
	void buildMergedConfigWithAtWebAppConfigurationWithAnnotationAndClassesOnSuperclass() {
		Class<?> webTestClass = WebClassesFoo.class;
		Class<?> standardTestClass = ClassesFoo.class;
		WebMergedContextConfiguration webMergedConfig = (WebMergedContextConfiguration) buildMergedContextConfiguration(webTestClass);
		MergedContextConfiguration standardMergedConfig = buildMergedContextConfiguration(standardTestClass);

		assertThat(webMergedConfig).isEqualTo(webMergedConfig);
		assertThat(standardMergedConfig).isEqualTo(standardMergedConfig);
		assertThat(webMergedConfig).isNotEqualTo(standardMergedConfig);
		assertThat(standardMergedConfig).isNotEqualTo(webMergedConfig);

		assertMergedConfig(webMergedConfig, webTestClass, EMPTY_STRING_ARRAY, array(FooConfig.class),
			WebDelegatingSmartContextLoader.class);
		assertMergedConfig(standardMergedConfig, standardTestClass, EMPTY_STRING_ARRAY,
			array(FooConfig.class), DelegatingSmartContextLoader.class);
	}

	@Test
	@SuppressWarnings("deprecation")
	void buildMergedConfigWithLocalAnnotationAndOverriddenContextLoaderAndLocations() {
		Class<?> testClass = PropertiesLocationsFoo.class;
		Class<? extends ContextLoader> expectedContextLoaderClass = cn.taketoday.test.context.support.GenericPropertiesContextLoader.class;
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);

		assertMergedConfig(mergedConfig, testClass, array("classpath:/foo.properties"), EMPTY_CLASS_ARRAY,
			expectedContextLoaderClass);
	}

	@Test
	@SuppressWarnings("deprecation")
	void buildMergedConfigWithLocalAnnotationAndOverriddenContextLoaderAndClasses() {
		Class<?> testClass = PropertiesClassesFoo.class;
		Class<? extends ContextLoader> expectedContextLoaderClass = cn.taketoday.test.context.support.GenericPropertiesContextLoader.class;
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);

		assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, array(FooConfig.class),
			expectedContextLoaderClass);
	}

	@Test
	void buildMergedConfigWithLocalAndInheritedAnnotationsAndLocations() {
		Class<?> testClass = LocationsBar.class;
		String[] expectedLocations = array("/foo.xml", "/bar.xml");
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);

		assertMergedConfig(mergedConfig, testClass, expectedLocations, EMPTY_CLASS_ARRAY,
			AnnotationConfigContextLoader.class);
	}

	@Test
	void buildMergedConfigWithLocalAndInheritedAnnotationsAndClasses() {
		Class<?> testClass = ClassesBar.class;
		Class<?>[] expectedClasses = array(FooConfig.class, BarConfig.class);
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);

		assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, expectedClasses,
			AnnotationConfigContextLoader.class);
	}

	@Test
	void buildMergedConfigWithAnnotationsAndOverriddenLocations() {
		Class<?> testClass = OverriddenLocationsBar.class;
		String[] expectedLocations = array("/bar.xml");
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);

		assertMergedConfig(mergedConfig, testClass, expectedLocations, EMPTY_CLASS_ARRAY,
			AnnotationConfigContextLoader.class);
	}

	@Test
	void buildMergedConfigWithAnnotationsAndOverriddenClasses() {
		Class<?> testClass = OverriddenClassesBar.class;
		Class<?>[] expectedClasses = array(BarConfig.class);
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);

		assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, expectedClasses,
			AnnotationConfigContextLoader.class);
	}

	@Test
	void buildMergedConfigAndVerifyLocationPathsAreCleanedEquivalently() {
		assertMergedConfigForLocationPaths(AbsoluteFooXmlLocationWithoutClasspathPrefix.class);
		assertMergedConfigForLocationPaths(AbsoluteFooXmlLocationWithInnerRelativePathWithoutClasspathPrefix.class);
		assertMergedConfigForLocationPaths(AbsoluteFooXmlLocationWithClasspathPrefix.class);
		assertMergedConfigForLocationPaths(RelativeFooXmlLocation.class);
	}

	private void assertMergedConfigForLocationPaths(Class<?> testClass) {
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);

		assertThat(mergedConfig).isNotNull();
		assertThat(mergedConfig.getTestClass()).isEqualTo(testClass);
		assertThat(mergedConfig.getContextLoader()).isInstanceOf(DelegatingSmartContextLoader.class);
		assertThat(mergedConfig.getLocations()).containsExactly("classpath:/example/foo.xml");
		assertThat(mergedConfig.getPropertySourceLocations()).containsExactly("classpath:/example/foo.properties");

		assertThat(mergedConfig.getClasses()).isEmpty();
		assertThat(mergedConfig.getActiveProfiles()).isEmpty();
		assertThat(mergedConfig.getContextInitializerClasses()).isEmpty();
		assertThat(mergedConfig.getPropertySourceProperties()).isEmpty();
	}

	/**
	 * @since 5.3
	 */
	@Test
	public void buildMergedConfigForNestedTestClassWithInheritedConfig() {
		Class<?> testClass = OuterTestCase.NestedTestCaseWithInheritedConfig.class;
		Class<?>[] expectedClasses = array(FooConfig.class);
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);

		assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, expectedClasses,
			AnnotationConfigContextLoader.class);
	}

	/**
	 * @since 5.3
	 */
	@Test
	public void buildMergedConfigForNestedTestClassWithMergedInheritedConfig() {
		Class<?> testClass = OuterTestCase.NestedTestCaseWithMergedInheritedConfig.class;
		Class<?>[] expectedClasses = array(FooConfig.class, BarConfig.class);
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);

		assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, expectedClasses,
			AnnotationConfigContextLoader.class);
	}

	/**
	 * @since 5.3
	 */
	@Test
	public void buildMergedConfigForNestedTestClassWithOverriddenConfig() {
		Class<?> testClass = OuterTestCase.NestedTestCaseWithOverriddenConfig.class;
		Class<?>[] expectedClasses = array(BarConfig.class);
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);

		assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, expectedClasses,
			DelegatingSmartContextLoader.class);
	}

	/**
	 * @since 5.3
	 */
	@Test
	public void buildMergedConfigForDoubleNestedTestClassWithInheritedOverriddenConfig() {
		Class<?> testClass = OuterTestCase.NestedTestCaseWithOverriddenConfig.DoubleNestedTestCaseWithInheritedOverriddenConfig.class;
		Class<?>[] expectedClasses = array(BarConfig.class);
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);

		assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, expectedClasses,
			DelegatingSmartContextLoader.class);
	}

	/**
	 * @since 5.3
	 */
	@Test
	public void buildMergedConfigForContextHierarchy() {
		Class<?> testClass = ContextHierarchyOuterTestCase.class;
		Class<?>[] expectedClasses = array(BarConfig.class);

		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);
		assertThat(mergedConfig).as("merged config").isNotNull();

		MergedContextConfiguration parent = mergedConfig.getParent();
		assertThat(parent).as("parent config").isNotNull();
		// The following does not work -- at least not in Eclipse.
		// asssertThat(parent.getClasses())...
		// So we use AssertionsForClassTypes directly.
		AssertionsForClassTypes.assertThat(parent.getClasses()).containsExactly(FooConfig.class);

		assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, expectedClasses,
			AnnotationConfigContextLoader.class);
	}

	/**
	 * @since 5.3
	 */
	@Test
	public void buildMergedConfigForNestedTestClassWithInheritedConfigForContextHierarchy() {
		Class<?> enclosingTestClass = ContextHierarchyOuterTestCase.class;
		Class<?> testClass = ContextHierarchyOuterTestCase.NestedTestCaseWithInheritedConfig.class;
		Class<?>[] expectedClasses = array(BarConfig.class);

		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);
		assertThat(mergedConfig).as("merged config").isNotNull();

		MergedContextConfiguration parent = mergedConfig.getParent();
		assertThat(parent).as("parent config").isNotNull();
		AssertionsForClassTypes.assertThat(parent.getClasses()).containsExactly(FooConfig.class);

		assertMergedConfig(mergedConfig, enclosingTestClass, EMPTY_STRING_ARRAY, expectedClasses,
			AnnotationConfigContextLoader.class);
	}

	/**
	 * @since 5.3
	 */
	@Test
	public void buildMergedConfigForNestedTestClassWithMergedInheritedConfigForContextHierarchy() {
		Class<?> testClass = ContextHierarchyOuterTestCase.NestedTestCaseWithMergedInheritedConfig.class;
		Class<?>[] expectedClasses = array(BarConfig.class, BazConfig.class);

		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);
		assertThat(mergedConfig).as("merged config").isNotNull();

		MergedContextConfiguration parent = mergedConfig.getParent();
		assertThat(parent).as("parent config").isNotNull();
		AssertionsForClassTypes.assertThat(parent.getClasses()).containsExactly(FooConfig.class);

		assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, expectedClasses,
			AnnotationConfigContextLoader.class);
	}

	/**
	 * @since 5.3
	 */
	@Test
	public void buildMergedConfigForNestedTestClassWithOverriddenConfigForContextHierarchy() {
		Class<?> testClass = ContextHierarchyOuterTestCase.NestedTestCaseWithOverriddenConfig.class;
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);
		assertThat(mergedConfig).as("merged config").isNotNull();

		MergedContextConfiguration parent = mergedConfig.getParent();
		assertThat(parent).as("parent config").isNotNull();

		assertMergedConfig(parent, testClass, EMPTY_STRING_ARRAY, array(QuuxConfig.class),
				AnnotationConfigContextLoader.class);
		assertMergedConfig(mergedConfig, ContextHierarchyOuterTestCase.class, EMPTY_STRING_ARRAY,
				array(BarConfig.class), AnnotationConfigContextLoader.class);
	}

	/**
	 * @since 5.3
	 */
	@Test
	void buildMergedConfigWithDuplicateConfigurationOnSuperclassAndSubclass() {
		compareApplesToApples(AppleConfigTestCase.class, DuplicateConfigAppleConfigTestCase.class);
		compareApplesToApples(DuplicateConfigAppleConfigTestCase.class, SubDuplicateConfigAppleConfigTestCase.class);
		compareApplesToOranges(ApplesAndOrangesConfigTestCase.class, DuplicateConfigApplesAndOrangesConfigTestCase.class);
		compareApplesToOranges(DuplicateConfigApplesAndOrangesConfigTestCase.class, SubDuplicateConfigApplesAndOrangesConfigTestCase.class);
	}

	/**
	 * @since 5.3
	 */
	@Test
	void buildMergedConfigWithDuplicateConfigurationOnEnclosingClassAndNestedClass() {
		compareApplesToApples(AppleConfigTestCase.class, AppleConfigTestCase.Nested.class);
		compareApplesToApples(AppleConfigTestCase.Nested.class, AppleConfigTestCase.Nested.DoubleNested.class);
		compareApplesToOranges(ApplesAndOrangesConfigTestCase.class, ApplesAndOrangesConfigTestCase.Nested.class);
		compareApplesToOranges(ApplesAndOrangesConfigTestCase.Nested.class, ApplesAndOrangesConfigTestCase.Nested.DoubleNested.class);
	}

	private void compareApplesToApples(Class<?> parent, Class<?> child) {
		MergedContextConfiguration parentMergedConfig = buildMergedContextConfiguration(parent);
		assertMergedConfig(parentMergedConfig, parent, EMPTY_STRING_ARRAY, array(AppleConfig.class),
				DelegatingSmartContextLoader.class);

		MergedContextConfiguration childMergedConfig = buildMergedContextConfiguration(child);
		assertMergedConfig(childMergedConfig, child, EMPTY_STRING_ARRAY, array(AppleConfig.class),
				DelegatingSmartContextLoader.class);

		assertThat(parentMergedConfig.getActiveProfiles()).as("active profiles")
			.containsExactly("apples")
			.isEqualTo(childMergedConfig.getActiveProfiles());
		assertThat(parentMergedConfig).isEqualTo(childMergedConfig);
	}

	private void compareApplesToOranges(Class<?> parent, Class<?> child) {
		MergedContextConfiguration parentMergedConfig = buildMergedContextConfiguration(parent);
		assertMergedConfig(parentMergedConfig, parent, EMPTY_STRING_ARRAY, array(AppleConfig.class),
				DelegatingSmartContextLoader.class);

		MergedContextConfiguration childMergedConfig = buildMergedContextConfiguration(child);
		assertMergedConfig(childMergedConfig, child, EMPTY_STRING_ARRAY, array(AppleConfig.class),
				DelegatingSmartContextLoader.class);

		assertThat(parentMergedConfig.getActiveProfiles()).as("active profiles")
			.containsExactly("oranges", "apples")
			.isEqualTo(childMergedConfig.getActiveProfiles());
		assertThat(parentMergedConfig).isEqualTo(childMergedConfig);
	}

	/**
	 * @since 5.3
	 */
	@Test
	void buildMergedConfigWithEmptyConfigurationOnSuperclassAndSubclass() {
		// not equal because different defaults are detected for each class
		assertEmptyConfigsAreNotEqual(EmptyConfigTestCase.class, SubEmptyConfigTestCase.class, SubSubEmptyConfigTestCase.class);
	}

	private void assertEmptyConfigsAreNotEqual(Class<?> parent, Class<?> child, Class<?> grandchild) {
		MergedContextConfiguration parentMergedConfig = buildMergedContextConfiguration(parent);
		assertMergedConfig(parentMergedConfig, parent, EMPTY_STRING_ARRAY,
				array(EmptyConfigTestCase.Config.class), DelegatingSmartContextLoader.class);

		MergedContextConfiguration childMergedConfig = buildMergedContextConfiguration(child);
		assertMergedConfig(childMergedConfig, child, EMPTY_STRING_ARRAY,
				array(EmptyConfigTestCase.Config.class, SubEmptyConfigTestCase.Config.class), DelegatingSmartContextLoader.class);

		assertThat(parentMergedConfig.getActiveProfiles()).as("active profiles")
			.isEqualTo(childMergedConfig.getActiveProfiles());
		assertThat(parentMergedConfig).isNotEqualTo(childMergedConfig);

		MergedContextConfiguration grandchildMergedConfig = buildMergedContextConfiguration(grandchild);
		assertMergedConfig(grandchildMergedConfig, grandchild, EMPTY_STRING_ARRAY,
				array(EmptyConfigTestCase.Config.class, SubEmptyConfigTestCase.Config.class, SubSubEmptyConfigTestCase.Config.class),
				DelegatingSmartContextLoader.class);

		assertThat(childMergedConfig.getActiveProfiles()).as("active profiles")
			.isEqualTo(grandchildMergedConfig.getActiveProfiles());
		assertThat(childMergedConfig).isNotEqualTo(grandchildMergedConfig);
	}

	/**
	 * @since 5.3
	 */
	@Test
	void buildMergedConfigWithEmptyConfigurationOnEnclosingClassAndExplicitConfigOnNestedClass() {
		Class<EmptyConfigTestCase> enclosingClass = EmptyConfigTestCase.class;
		Class<EmptyConfigTestCase.Nested> nestedClass = EmptyConfigTestCase.Nested.class;

		MergedContextConfiguration enclosingMergedConfig = buildMergedContextConfiguration(enclosingClass);
		assertMergedConfig(enclosingMergedConfig, enclosingClass, EMPTY_STRING_ARRAY,
				array(EmptyConfigTestCase.Config.class), DelegatingSmartContextLoader.class);

		MergedContextConfiguration nestedMergedConfig = buildMergedContextConfiguration(nestedClass);
		assertMergedConfig(nestedMergedConfig, nestedClass, EMPTY_STRING_ARRAY,
				array(EmptyConfigTestCase.Config.class, AppleConfig.class), DelegatingSmartContextLoader.class);

		assertThat(enclosingMergedConfig.getActiveProfiles()).as("active profiles")
			.isEqualTo(nestedMergedConfig.getActiveProfiles());
		assertThat(enclosingMergedConfig).isNotEqualTo(nestedMergedConfig);
	}


	@ContextConfiguration
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public static @interface SpringAppConfig {

		Class<?>[] classes() default {};
	}

	@SpringAppConfig(classes = { FooConfig.class, BarConfig.class })
	public static abstract class Dog {
	}

	public static abstract class WorkingDog extends Dog {
	}

	public static class GermanShepherd extends WorkingDog {
	}

	@ContextConfiguration
	static class MissingContextAttributesTestCase {
	}

	@ContextConfiguration(locations = "/example/foo.xml")
	@TestPropertySource("/example/foo.properties")
	static class AbsoluteFooXmlLocationWithoutClasspathPrefix {
	}

	@ContextConfiguration(locations = "/example/../org/../example/foo.xml")
	@TestPropertySource("/example/../org/../example/foo.properties")
	static class AbsoluteFooXmlLocationWithInnerRelativePathWithoutClasspathPrefix {
	}

	@ContextConfiguration(locations = "classpath:/example/foo.xml")
	@TestPropertySource("classpath:/example/foo.properties")
	static class AbsoluteFooXmlLocationWithClasspathPrefix {
	}

	// cn.taketoday.test.context.support --> 5 levels up to the root of the classpath
	@ContextConfiguration(locations = "../../../../../example/foo.xml")
	@TestPropertySource("../../../../../example/foo.properties")
	static class RelativeFooXmlLocation {
	}

	static class AppleConfig {
	}

	@ContextConfiguration(classes = AppleConfig.class)
	@ActiveProfiles("apples")
	static class AppleConfigTestCase {

		@ContextConfiguration(classes = AppleConfig.class)
		@ActiveProfiles({"apples", "apples"})
		class Nested {

			@ContextConfiguration(classes = AppleConfig.class)
			@ActiveProfiles({"apples", "apples", "apples"})
			class DoubleNested {
			}
		}
	}

	@ContextConfiguration(classes = AppleConfig.class)
	@ActiveProfiles({"apples", "apples"})
	static class DuplicateConfigAppleConfigTestCase extends AppleConfigTestCase {
	}

	@ContextConfiguration(classes = AppleConfig.class)
	@ActiveProfiles({"apples", "apples", "apples"})
	static class SubDuplicateConfigAppleConfigTestCase extends DuplicateConfigAppleConfigTestCase {
	}

	@ContextConfiguration(classes = AppleConfig.class)
	@ActiveProfiles({"oranges", "apples"})
	static class ApplesAndOrangesConfigTestCase {

		@ContextConfiguration(classes = AppleConfig.class)
		@ActiveProfiles(profiles = {"oranges", "apples"}, inheritProfiles = false)
		class Nested {

			@ContextConfiguration(classes = AppleConfig.class)
			@ActiveProfiles(profiles = {"oranges", "apples", "oranges"}, inheritProfiles = false)
			class DoubleNested {
			}
		}
	}

	@ContextConfiguration(classes = AppleConfig.class)
	@ActiveProfiles(profiles = {"oranges", "apples", "oranges"}, inheritProfiles = false)
	static class DuplicateConfigApplesAndOrangesConfigTestCase extends ApplesAndOrangesConfigTestCase {
	}

	@ContextConfiguration(classes = AppleConfig.class)
	@ActiveProfiles(profiles = {"oranges", "apples", "oranges"}, inheritProfiles = false)
	static class SubDuplicateConfigApplesAndOrangesConfigTestCase extends DuplicateConfigApplesAndOrangesConfigTestCase {
	}

	@ContextConfiguration
	static class EmptyConfigTestCase {

		@ContextConfiguration(classes = AppleConfig.class)
		class Nested {
			// inner classes cannot have static nested @Configuration classes
		}

		@Configuration
		static class Config {
		}
	}

	@ContextConfiguration
	static class SubEmptyConfigTestCase extends EmptyConfigTestCase {

		@Configuration
		static class Config {
		}
	}

	@ContextConfiguration
	static class SubSubEmptyConfigTestCase extends SubEmptyConfigTestCase {

		@Configuration
		static class Config {
		}
	}

}
