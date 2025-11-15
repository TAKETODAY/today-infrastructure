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

package infra.core.type.classreading;

import com.github.benmanes.caffeine.cache.Caffeine;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import infra.core.type.AbstractAnnotationMetadataTests;
import infra.core.type.AnnotationMetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link SimpleAnnotationMetadata} and
 * {@link SimpleAnnotationMetadataReadingVisitor} on Java < 24,
 * and for the ClassFile API variant on Java >= 24.
 *
 * @author Phillip Webb
 */
class DefaultAnnotationMetadataTests extends AbstractAnnotationMetadataTests {

  @Override
  protected AnnotationMetadata get(Class<?> source) {
    try {
      return MetadataReaderFactory.create(source.getClassLoader())
              .getMetadataReader(source.getName()).getAnnotationMetadata();
    }
    catch (IOException ex) {
      throw new IllegalStateException(ex);
    }
  }

  @Test
  void getClassAttributeWhenUnknownClass() {
    var annotation = get(WithClassMissingFromClasspath.class).getAnnotations().get(ClassAttributes.class);
    assertThat(annotation.getStringArray("types")).contains("com.github.benmanes.caffeine.cache.Caffeine");
    assertThatIllegalArgumentException().isThrownBy(() -> annotation.getClassArray("types"));
  }

  @ClassAttributes(types = { Caffeine.class })
  public static class WithClassMissingFromClasspath {
  }

  @Retention(RetentionPolicy.RUNTIME)
  public @interface ClassAttributes {
    Class<?>[] types();
  }

}
