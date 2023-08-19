/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.validation.annotation;

import java.lang.annotation.Annotation;

import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.lang.Nullable;

/**
 * Utility class for handling validation annotations.
 * Mainly for internal use within the framework.
 *
 * @author Christoph Dreis
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class ValidationAnnotationUtils {

  private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

  /**
   * Determine any validation hints by the given annotation.
   * <p>This implementation checks for Infra
   * {@link cn.taketoday.validation.annotation.Validated},
   * {@code @jakarta.validation.Valid}, and custom annotations whose
   * name starts with "Valid" which may optionally declare validation
   * hints through the "value" attribute.
   *
   * @param ann the annotation (potentially a validation annotation)
   * @return the validation hints to apply (possibly an empty array),
   * or {@code null} if this annotation does not trigger any validation
   */
  @Nullable
  public static Object[] determineValidationHints(Annotation ann) {
    // Direct presence of @Validated ?
    if (ann instanceof Validated validated) {
      return validated.value();
    }
    // Direct presence of @Valid ?
    Class<? extends Annotation> annotationType = ann.annotationType();
    if ("jakarta.validation.Valid".equals(annotationType.getName())) {
      return EMPTY_OBJECT_ARRAY;
    }
    // Meta presence of @Validated ?
    Validated validatedAnn = AnnotationUtils.getAnnotation(ann, Validated.class);
    if (validatedAnn != null) {
      return validatedAnn.value();
    }
    // Custom validation annotation ?
    if (annotationType.getSimpleName().startsWith("Valid")) {
      return convertValidationHints(AnnotationUtils.getValue(ann));
    }
    // No validation triggered
    return null;
  }

  private static Object[] convertValidationHints(@Nullable Object hints) {
    if (hints == null) {
      return EMPTY_OBJECT_ARRAY;
    }
    return (hints instanceof Object[] ? (Object[]) hints : new Object[] { hints });
  }

}
