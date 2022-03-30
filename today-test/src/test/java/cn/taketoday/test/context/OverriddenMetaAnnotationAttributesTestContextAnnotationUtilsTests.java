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
import cn.taketoday.core.annotation.AliasFor;
import cn.taketoday.test.context.TestContextAnnotationUtils.AnnotationDescriptor;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static cn.taketoday.test.context.TestContextAnnotationUtils.findAnnotationDescriptor;

/**
 * Unit tests for {@link TestContextAnnotationUtils} that verify support for
 * overridden meta-annotation attributes.
 *
 * <p>See <a href="https://jira.spring.io/browse/SPR-10181">SPR-10181</a>.
 *
 * @author Sam Brannen
 * @since 5.3, though originally since 4.0 for the deprecated
 * {@link cn.taketoday.test.util.MetaAnnotationUtils} support
 * @see TestContextAnnotationUtilsTests
 */
class OverriddenMetaAnnotationAttributesTestContextAnnotationUtilsTests {

	@Test
	void contextConfigurationValue() {
		Class<?> rootDeclaringClass = MetaValueConfigTestCase.class;
		AnnotationDescriptor<ContextConfiguration> descriptor = findAnnotationDescriptor(rootDeclaringClass,
			ContextConfiguration.class);
		assertThat(descriptor).isNotNull();
		assertThat(descriptor.getRootDeclaringClass()).isEqualTo(rootDeclaringClass);
		assertThat(descriptor.getDeclaringClass()).isEqualTo(MetaValueConfig.class);
		assertThat(descriptor.getAnnotationType()).isEqualTo(ContextConfiguration.class);
		assertThat(descriptor.getAnnotation().value()).containsExactly("foo.xml");
		assertThat(descriptor.getAnnotation().locations()).containsExactly("foo.xml");
	}

	@Test
	void overriddenContextConfigurationValue() {
		Class<?> rootDeclaringClass = OverriddenMetaValueConfigTestCase.class;
		AnnotationDescriptor<ContextConfiguration> descriptor = findAnnotationDescriptor(rootDeclaringClass,
			ContextConfiguration.class);
		assertThat(descriptor).isNotNull();
		assertThat(descriptor.getRootDeclaringClass()).isEqualTo(rootDeclaringClass);
		assertThat(descriptor.getDeclaringClass()).isEqualTo(MetaValueConfig.class);
		assertThat(descriptor.getAnnotationType()).isEqualTo(ContextConfiguration.class);
		assertThat(descriptor.getAnnotation().value()).containsExactly("bar.xml");
		assertThat(descriptor.getAnnotation().locations()).containsExactly("bar.xml");
	}

	@Test
	void contextConfigurationLocationsAndInheritLocations() {
		Class<?> rootDeclaringClass = MetaLocationsConfigTestCase.class;
		AnnotationDescriptor<ContextConfiguration> descriptor = findAnnotationDescriptor(rootDeclaringClass,
			ContextConfiguration.class);
		assertThat(descriptor).isNotNull();
		assertThat(descriptor.getRootDeclaringClass()).isEqualTo(rootDeclaringClass);
		assertThat(descriptor.getDeclaringClass()).isEqualTo(MetaLocationsConfig.class);
		assertThat(descriptor.getAnnotationType()).isEqualTo(ContextConfiguration.class);
		assertThat(descriptor.getAnnotation().value()).isEmpty();
		assertThat(descriptor.getAnnotation().locations()).isEmpty();
		assertThat(descriptor.getAnnotation().inheritLocations()).isTrue();
	}

	@Test
	void overriddenContextConfigurationLocationsAndInheritLocations() {
		Class<?> rootDeclaringClass = OverriddenMetaLocationsConfigTestCase.class;
		AnnotationDescriptor<ContextConfiguration> descriptor = findAnnotationDescriptor(rootDeclaringClass,
			ContextConfiguration.class);
		assertThat(descriptor).isNotNull();
		assertThat(descriptor.getRootDeclaringClass()).isEqualTo(rootDeclaringClass);
		assertThat(descriptor.getDeclaringClass()).isEqualTo(MetaLocationsConfig.class);
		assertThat(descriptor.getAnnotationType()).isEqualTo(ContextConfiguration.class);
		assertThat(descriptor.getAnnotation().value()).containsExactly("bar.xml");
		assertThat(descriptor.getAnnotation().locations()).containsExactly("bar.xml");
		assertThat(descriptor.getAnnotation().inheritLocations()).isTrue();
	}


	// -------------------------------------------------------------------------

	@ContextConfiguration
	@Retention(RetentionPolicy.RUNTIME)
	@interface MetaValueConfig {

		@AliasFor(annotation = ContextConfiguration.class)
		String[] value() default "foo.xml";
	}

	@MetaValueConfig
	static class MetaValueConfigTestCase {
	}

	@MetaValueConfig("bar.xml")
	static class OverriddenMetaValueConfigTestCase {
	}

	@ContextConfiguration(locations = "foo.xml", inheritLocations = false)
	@Retention(RetentionPolicy.RUNTIME)
	@interface MetaLocationsConfig {

		String[] locations() default {};

		boolean inheritLocations();
	}

	@MetaLocationsConfig(inheritLocations = true)
	static class MetaLocationsConfigTestCase {
	}

	@MetaLocationsConfig(locations = "bar.xml", inheritLocations = true)
	static class OverriddenMetaLocationsConfigTestCase {
	}

}
