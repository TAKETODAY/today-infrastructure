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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.core.SpringProperties;
import cn.taketoday.test.context.TestConstructor;
import cn.taketoday.test.context.support.TestConstructorUtils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;

import static org.assertj.core.api.Assertions.assertThat;
import static cn.taketoday.test.context.TestConstructor.AutowireMode.ALL;
import static cn.taketoday.test.context.TestConstructor.AutowireMode.ANNOTATED;

/**
 * Unit tests for {@link TestConstructorUtils}.
 *
 * @author Sam Brannen
 * @since 5.2
 */
class TestConstructorUtilsTests {

	@AfterEach
	void clearGlobalFlag() {
		setGlobalFlag(null);
	}

	@Test
	void notAutowirable() throws Exception {
		assertNotAutowirable(NotAutowirableTestCase.class);
	}

	@Test
	void autowiredAnnotation() throws Exception {
		assertAutowirable(AutowiredAnnotationTestCase.class);
	}

	@Test
	void testConstructorAnnotation() throws Exception {
		assertAutowirable(TestConstructorAnnotationTestCase.class);
	}

	@Test
	void testConstructorAsMetaAnnotation() throws Exception {
		assertAutowirable(TestConstructorAsMetaAnnotationTestCase.class);
	}

	@Test
	void automaticallyAutowired() throws Exception {
		setGlobalFlag();
		assertAutowirable(AutomaticallyAutowiredTestCase.class);
	}

	@Test
	void automaticallyAutowiredButOverriddenLocally() throws Exception {
		setGlobalFlag();
		assertNotAutowirable(TestConstructorAnnotationOverridesGlobalFlagTestCase.class);
	}

	@Test
	void globalFlagVariations() throws Exception {
		Class<?> testClass = AutomaticallyAutowiredTestCase.class;

		setGlobalFlag(ALL.name());
		assertAutowirable(testClass);

		setGlobalFlag(ALL.name().toLowerCase());
		assertAutowirable(testClass);

		setGlobalFlag("\t" + ALL.name().toLowerCase() + "   ");
		assertAutowirable(testClass);

		setGlobalFlag("bogus");
		assertNotAutowirable(testClass);

		setGlobalFlag("        ");
		assertNotAutowirable(testClass);
	}

	private void assertAutowirable(Class<?> testClass) throws NoSuchMethodException {
		Constructor<?> constructor = testClass.getDeclaredConstructor();
		assertThat(TestConstructorUtils.isAutowirableConstructor(constructor, testClass)).isTrue();
	}

	private void assertNotAutowirable(Class<?> testClass) throws NoSuchMethodException {
		Constructor<?> constructor = testClass.getDeclaredConstructor();
		assertThat(TestConstructorUtils.isAutowirableConstructor(constructor, testClass)).isFalse();
	}

	private void setGlobalFlag() {
		setGlobalFlag(ALL.name());
	}

	private void setGlobalFlag(String flag) {
		SpringProperties.setProperty(TestConstructor.TEST_CONSTRUCTOR_AUTOWIRE_MODE_PROPERTY_NAME, flag);
	}


	static class NotAutowirableTestCase {
	}

	// The following declaration simply verifies that @Autowired on the constructor takes
	// precedence.
	@TestConstructor(autowireMode = ANNOTATED)
	static class AutowiredAnnotationTestCase {

		@Autowired
		AutowiredAnnotationTestCase() {
		}
	}

	@TestConstructor(autowireMode = ALL)
	static class TestConstructorAnnotationTestCase {
	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@TestConstructor(autowireMode = ALL)
	@interface AutowireConstructor {
	}

	@AutowireConstructor
	static class TestConstructorAsMetaAnnotationTestCase {
	}

	static class AutomaticallyAutowiredTestCase {
	}

	@TestConstructor(autowireMode = ANNOTATED)
	static class TestConstructorAnnotationOverridesGlobalFlagTestCase {
	}

}
