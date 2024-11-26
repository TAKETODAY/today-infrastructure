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

package infra.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

import infra.core.GenericTypeResolver;
import infra.core.type.AnnotatedTypeMetadata;
import infra.lang.Assert;
import infra.lang.NonNull;

/**
 * @author TODAY 2021/3/8 16:51
 * @since 3.0
 */
public interface AnnotationProvider<A extends Annotation> {

  /**
   * Get Annotation on sub-class
   */
  default A getAnnotation() {
    return getAnnotation(getClass());
  }

  default A getAnnotation(AnnotatedElement annotated) {
    Assert.notNull(annotated, "annotated is required");
    return AnnotationUtils.getAnnotation(annotated, annotationType());
  }

  /**
   * @since 4.0
   */
  default MergedAnnotation<A> getMergedAnnotation(AnnotatedTypeMetadata metadata) {
    Assert.notNull(metadata, "AnnotatedTypeMetadata is required");
    return metadata.getAnnotation(annotationType());
  }

  /**
   * @since 4.0
   */
  default A getAnnotation(AnnotatedTypeMetadata metadata) {
    return getMergedAnnotation(metadata).synthesize();
  }

  /**
   * @since 4.0
   */
  default AnnotationAttributes getAttributes(AnnotatedTypeMetadata metadata) {
    return AnnotationAttributes.fromMetadata(metadata, annotationType());
  }

  /**
   * @since 4.0
   */
  @NonNull
  default Class<A> annotationType() {
    Class<A> generic = GenericTypeResolver.resolveTypeArgument(getClass(), AnnotationProvider.class);
    if (generic != null) {
      return generic;
    }
    throw new IllegalArgumentException("Cannot get annotation type on: " + this);
  }

}
