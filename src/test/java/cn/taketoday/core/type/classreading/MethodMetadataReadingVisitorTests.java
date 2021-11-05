/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.core.type.classreading;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import cn.taketoday.core.bytecode.ClassReader;
import cn.taketoday.core.io.DefaultResourceLoader;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.core.type.AbstractMethodMetadataTests;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.util.ClassUtils;

import java.io.BufferedInputStream;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link MethodMetadataReadingVisitor}.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 */
@SuppressWarnings("deprecation")
class MethodMetadataReadingVisitorTests extends AbstractMethodMetadataTests {

	@Override
	protected AnnotationMetadata get(Class<?> source) {
		try {
			ClassLoader classLoader = source.getClassLoader();
			String className = source.getName();
			String resourcePath = ResourceLoader.CLASSPATH_URL_PREFIX
					+ ClassUtils.convertClassNameToResourcePath(className)
					+ ClassUtils.CLASS_FILE_SUFFIX;
			Resource resource = new DefaultResourceLoader().getResource(resourcePath);
			try (InputStream inputStream = new BufferedInputStream(
					resource.getInputStream())) {
				ClassReader classReader = new ClassReader(inputStream);
				AnnotationMetadataReadingVisitor metadata = new AnnotationMetadataReadingVisitor(
						classLoader);
				classReader.accept(metadata, ClassReader.SKIP_DEBUG);
				return metadata;
			}
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	@Test
	@Disabled("equals() not implemented in deprecated MethodMetadataReadingVisitor")
	@Override
	public void verifyEquals() throws Exception {
	}

	@Test
	@Disabled("hashCode() not implemented in deprecated MethodMetadataReadingVisitor")
	@Override
	public void verifyHashCode() throws Exception {
	}

	@Test
	@Disabled("toString() not implemented in deprecated MethodMetadataReadingVisitor")
	@Override
	public void verifyToString() {
	}

	@Test
	@Override
	public void getAnnotationsReturnsDirectAnnotations() {
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(
				super::getAnnotationsReturnsDirectAnnotations);
	}

}
