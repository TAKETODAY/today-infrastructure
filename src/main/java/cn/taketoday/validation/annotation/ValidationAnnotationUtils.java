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

package cn.taketoday.validation.annotation;

import java.lang.annotation.Annotation;

import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.lang.Nullable;

/**
 * Utility class for handling validation annotations.
 * Mainly for internal use within the framework.
 *
 * @author Christoph Dreis
 * @since 4.0
 */
public abstract class ValidationAnnotationUtils {

  private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

  /**
   * Determine any validation hints by the given annotation.
   * <p>This implementation checks for {@code @jakarta.validation.Valid},
   * Spring's {@link cn.taketoday.validation.annotation.Validated},
   * and custom annotations whose name starts with "Valid".
   *
   * @param ann the annotation (potentially a validation annotation)
   * @return the validation hints to apply (possibly an empty array),
   * or {@code null} if this annotation does not trigger any validation
   */
  @Nullable
  public static Object[] determineValidationHints(Annotation ann) {
    Class<? extends Annotation> annotationType = ann.annotationType();
    String annotationName = annotationType.getName();
    if ("jakarta.validation.Valid".equals(annotationName)) {
      return EMPTY_OBJECT_ARRAY;
    }
    Validated validatedAnn = AnnotationUtils.getAnnotation(ann, Validated.class);
    if (validatedAnn != null) {
      Object hints = validatedAnn.value();
      return convertValidationHints(hints);
    }
    if (annotationType.getSimpleName().startsWith("Valid")) {
      Object hints = AnnotationUtils.getValue(ann);
      return convertValidationHints(hints);
    }
    return null;
  }

  private static Object[] convertValidationHints(@Nullable Object hints) {
    if (hints == null) {
      return EMPTY_OBJECT_ARRAY;
    }
    return (hints instanceof Object[] ? (Object[]) hints : new Object[] { hints });
  }

}
