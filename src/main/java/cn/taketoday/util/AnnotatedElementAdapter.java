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

package cn.taketoday.util;

import java.io.Serial;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;

import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.lang.Constant;

/**
 * Adapter class for exposing annotations as an {@link AnnotatedElement}
 *
 * @author TODAY 2021/3/26 13:42
 * @see AnnotationUtils#isPresent(AnnotatedElement, Class)
 * @see AnnotationUtils#getAnnotation(AnnotatedElement, Class)
 */
public class AnnotatedElementAdapter
        extends AbstractAnnotatedElement implements AnnotatedElement {
  @Serial
  private static final long serialVersionUID = 1L;
  private final Annotation[] annotations;

  public AnnotatedElementAdapter(Annotation[] annotations) {
    this.annotations = annotations;
  }

  @Override
  public Annotation[] getAnnotations() {
    return (this.annotations != null ? this.annotations.clone() : Constant.EMPTY_ANNOTATION_ARRAY);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof final AnnotatedElementAdapter that))
      return false;
    return Arrays.equals(annotations, that.annotations);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(annotations);
  }
}
