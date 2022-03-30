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
import cn.taketoday.test.context.MergedContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link AnnotationConfigContextLoader}.
 *
 * @author Sam Brannen
 * @since 3.1
 */
class AnnotationConfigContextLoaderTests {

	private final AnnotationConfigContextLoader contextLoader = new AnnotationConfigContextLoader();

	private static final String[] EMPTY_STRING_ARRAY = new String[0];
	private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];


	/**
	 * @since 4.0.4
	 */
	@Test
	void configMustNotContainLocations() throws Exception {
		MergedContextConfiguration mergedConfig = new MergedContextConfiguration(getClass(),
			new String[] { "config.xml" }, EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, contextLoader);
		assertThatIllegalStateException().isThrownBy(() ->
				contextLoader.loadContext(mergedConfig))
			.withMessageContaining("does not support resource locations");
	}

	@Test
	void detectDefaultConfigurationClassesForAnnotatedInnerClass() {
		Class<?>[] configClasses = contextLoader.detectDefaultConfigurationClasses(ContextConfigurationInnerClassTestCase.class);
		assertThat(configClasses).isNotNull();
		assertThat(configClasses.length).as("annotated static ContextConfiguration should be considered.").isEqualTo(1);

		configClasses = contextLoader.detectDefaultConfigurationClasses(AnnotatedFooConfigInnerClassTestCase.class);
		assertThat(configClasses).isNotNull();
		assertThat(configClasses.length).as("annotated static FooConfig should be considered.").isEqualTo(1);
	}

	@Test
	void detectDefaultConfigurationClassesForMultipleAnnotatedInnerClasses() {
		Class<?>[] configClasses = contextLoader.detectDefaultConfigurationClasses(MultipleStaticConfigurationClassesTestCase.class);
		assertThat(configClasses).isNotNull();
		assertThat(configClasses.length).as("multiple annotated static classes should be considered.").isEqualTo(2);
	}

	@Test
	void detectDefaultConfigurationClassesForNonAnnotatedInnerClass() {
		Class<?>[] configClasses = contextLoader.detectDefaultConfigurationClasses(PlainVanillaFooConfigInnerClassTestCase.class);
		assertThat(configClasses).isNotNull();
		assertThat(configClasses.length).as("non-annotated static FooConfig should NOT be considered.").isEqualTo(0);
	}

	@Test
	void detectDefaultConfigurationClassesForFinalAnnotatedInnerClass() {
		Class<?>[] configClasses = contextLoader.detectDefaultConfigurationClasses(FinalConfigInnerClassTestCase.class);
		assertThat(configClasses).isNotNull();
		assertThat(configClasses.length).as("final annotated static Config should NOT be considered.").isEqualTo(0);
	}

	@Test
	void detectDefaultConfigurationClassesForPrivateAnnotatedInnerClass() {
		Class<?>[] configClasses = contextLoader.detectDefaultConfigurationClasses(PrivateConfigInnerClassTestCase.class);
		assertThat(configClasses).isNotNull();
		assertThat(configClasses.length).as("private annotated inner classes should NOT be considered.").isEqualTo(0);
	}

	@Test
	void detectDefaultConfigurationClassesForNonStaticAnnotatedInnerClass() {
		Class<?>[] configClasses = contextLoader.detectDefaultConfigurationClasses(NonStaticConfigInnerClassesTestCase.class);
		assertThat(configClasses).isNotNull();
		assertThat(configClasses.length).as("non-static annotated inner classes should NOT be considered.").isEqualTo(0);
	}

}
